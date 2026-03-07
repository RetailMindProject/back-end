package com.example.back_end.modules.inventory.service;

import com.example.back_end.exception.CustomException;
import com.example.back_end.modules.inventory.dto.TransferRequestDTO;
import com.example.back_end.modules.messages.entity.Message;
import com.example.back_end.modules.messages.repository.MessageRepository;
import com.example.back_end.modules.register.entity.User;
import com.example.back_end.modules.register.entity.User.UserRole;
import com.example.back_end.modules.register.repository.UserRepository;
import com.example.back_end.modules.store_product.dto.StoreTransferRequestDTO;
import com.example.back_end.modules.store_product.service.StoreProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferRequestService {

    private static final String TITLE = "Transfer request";

    private static final Pattern STATUS_PATTERN =
            Pattern.compile("\\[TRANSFER_REQ_STATUS=(PENDING|APPROVED|REJECTED)]");
    private static final Pattern REQUESTED_BY_PATTERN =
            Pattern.compile("\\[TRANSFER_REQ_REQUESTED_BY=(\\d+)]");

    // New: decision parsing (from body)
    private static final Pattern DECIDED_BY_PATTERN =
            Pattern.compile("\\[TRANSFER_REQ_DECIDED_BY=(\\d+)]");
    private static final Pattern DECIDED_AT_PATTERN =
            Pattern.compile("\\[TRANSFER_REQ_DECIDED_AT=([^\\]]+)]");
    private static final Pattern REJECT_REASON_PATTERN =
            Pattern.compile("\\[TRANSFER_REQ_REJECT_REASON=([^\\]]+)]");

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final StoreProductService storeProductService;

    private User getCurrentUser() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new CustomException("Unauthorized");
        }
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException("User not found"));
    }

    /**
     * ✅ NEW:
     * Inventory Manager lists requests addressed to him (optionally filtered by status).
     * GET /api/inventory/transfer-requests?status=PENDING
     */
    @Transactional(readOnly = true)
    public List<TransferRequestDTO.Response> list(TransferRequestDTO.RequestStatus status) {
        User current = getCurrentUser();
        if (current.getRole() != UserRole.INVENTORY_MANAGER) {
            throw new CustomException("Only INVENTORY_MANAGER can view transfer requests");
        }

        return messageRepository.findAll().stream()
                // only messages that represent transfer requests
                .filter(m -> TITLE.equalsIgnoreCase(m.getTitle()))
                // only for this inventory manager inbox
                .filter(m -> m.getToUser() != null && Objects.equals(m.getToUser().getId(), current.getId()))
                // optional status filter
                .filter(m -> status == null || parseStatus(m.getBody()) == status)
                // newest first (optional but useful)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * ✅ NEW:
     * Inventory Manager gets one request by id (only if it belongs to his inbox).
     * GET /api/inventory/transfer-requests/{requestId}
     */
    @Transactional(readOnly = true)
    public TransferRequestDTO.Response getById(Long requestId) {
        User current = getCurrentUser();
        if (current.getRole() != UserRole.INVENTORY_MANAGER) {
            throw new CustomException("Only INVENTORY_MANAGER can view transfer requests");
        }

        Message msg = messageRepository.findById(requestId)
                .orElseThrow(() -> new CustomException("Transfer request not found"));

        if (!TITLE.equalsIgnoreCase(msg.getTitle())) {
            throw new CustomException("Message is not a transfer request");
        }

        if (msg.getToUser() == null || !Objects.equals(msg.getToUser().getId(), current.getId())) {
            throw new CustomException("You are not allowed to view this transfer request");
        }

        return mapToResponse(msg);
    }

    /**
     * Store Manager creates a new request (PENDING). No inventory changes here.
     * Sends notification message to all active INVENTORY_MANAGER users.
     */
    @Transactional
    public TransferRequestDTO.Response createRequest(TransferRequestDTO.CreateRequest request) {
        User requester = getCurrentUser();
        if (requester.getRole() != UserRole.STORE_MANAGER) {
            throw new CustomException("Only STORE_MANAGER can create transfer requests");
        }

        validateCreateRequest(request);

        List<User> inventoryManagers = userRepository.findByRoleAndIsActiveTrue(UserRole.INVENTORY_MANAGER);
        if (inventoryManagers.isEmpty()) {
            throw new CustomException("No active INVENTORY_MANAGER users found");
        }

        Message first = null;
        for (User inv : inventoryManagers) {
            Message msg = new Message();
            msg.setFromUser(requester);
            msg.setToUser(inv);
            msg.setTitle(TITLE);
            msg.setBody(buildPendingBody(requester, request));
            msg.setStatus(Message.MessageStatus.SENT);
            msg = messageRepository.save(msg);
            first = first == null ? msg : first;

            log.info("Transfer request created messageId={} from storeManagerId={} to inventoryManagerId={}",
                    msg.getId(), requester.getId(), inv.getId());
        }

        return TransferRequestDTO.Response.builder()
                .requestId(first != null ? first.getId() : null)
                .status(TransferRequestDTO.RequestStatus.PENDING)
                .requestedByUserId(requester.getId())
                .requestedAt(LocalDateTime.ofInstant(first.getCreatedAt().toInstant(), ZoneId.systemDefault()))
                .note(request.getNote())
                .build();
    }

    /**
     * Inventory Manager approves request: executes warehouse->store transfers transactionally.
     * Prevents double approval.
     */
    @Transactional
    public TransferRequestDTO.Response approve(Long messageId) {
        User approver = getCurrentUser();
        if (approver.getRole() != UserRole.INVENTORY_MANAGER) {
            throw new CustomException("Only INVENTORY_MANAGER can approve transfer requests");
        }

        Message reqMsg = messageRepository.findById(messageId)
                .orElseThrow(() -> new CustomException("Transfer request not found"));

        TransferRequestDTO.RequestStatus current = parseStatus(reqMsg.getBody());
        if (current != TransferRequestDTO.RequestStatus.PENDING) {
            throw new IllegalStateException("Request is not pending");
        }

        TransferRequestDTO.CreateRequest parsed = parseCreateRequest(reqMsg.getBody());

        for (TransferRequestDTO.Item item : parsed.getItems()) {
            StoreTransferRequestDTO dto = StoreTransferRequestDTO.builder()
                    .productId(item.getProductId())
                    .quantity(item.getQuantity())
                    .unitCost(null)
                    .expirationDate(null)
                    .note("Approved transfer request " + messageId + (parsed.getNote() != null ? (": " + parsed.getNote()) : ""))
                    .build();
            storeProductService.transferFromInventoryToStore(dto);
        }

        reqMsg.setBody(markDecision(reqMsg.getBody(), TransferRequestDTO.RequestStatus.APPROVED, approver.getId(), null));
        messageRepository.save(reqMsg);

        Integer requesterId = parseRequestedBy(reqMsg.getBody());
        if (requesterId != null) {
            userRepository.findById(requesterId).ifPresent(storeManager -> {
                Message notif = new Message();
                notif.setFromUser(approver);
                notif.setToUser(storeManager);
                notif.setTitle("Transfer request approved");
                notif.setBody("Your transfer request (" + messageId + ") was APPROVED and executed."
                        + "\n[TRANSFER_REQ_DECISION=APPROVED][requestId=" + messageId + "][approvedBy=" + approver.getId() + "]");
                notif.setStatus(Message.MessageStatus.SENT);
                messageRepository.save(notif);
            });
        }

        log.info("Transfer request approved and executed. requestMessageId={}, approvedBy={}", messageId, approver.getId());

        return TransferRequestDTO.Response.builder()
                .requestId(messageId)
                .status(TransferRequestDTO.RequestStatus.APPROVED)
                .requestedByUserId(requesterId)
                .decidedByUserId(approver.getId())
                .decidedAt(LocalDateTime.now())
                .note(parsed.getNote())
                .build();
    }

    /**
     * Inventory Manager rejects request (no inventory changes)
     */
    @Transactional
    public TransferRequestDTO.Response reject(Long messageId, TransferRequestDTO.RejectRequest rejectRequest) {
        User approver = getCurrentUser();
        if (approver.getRole() != UserRole.INVENTORY_MANAGER) {
            throw new CustomException("Only INVENTORY_MANAGER can reject transfer requests");
        }

        Message reqMsg = messageRepository.findById(messageId)
                .orElseThrow(() -> new CustomException("Transfer request not found"));

        TransferRequestDTO.RequestStatus current = parseStatus(reqMsg.getBody());
        if (current != TransferRequestDTO.RequestStatus.PENDING) {
            throw new IllegalStateException("Request is not pending");
        }

        reqMsg.setBody(markDecision(reqMsg.getBody(), TransferRequestDTO.RequestStatus.REJECTED, approver.getId(), rejectRequest.getReason()));
        messageRepository.save(reqMsg);

        Integer requesterId = parseRequestedBy(reqMsg.getBody());
        if (requesterId != null) {
            userRepository.findById(requesterId).ifPresent(storeManager -> {
                Message notif = new Message();
                notif.setFromUser(approver);
                notif.setToUser(storeManager);
                notif.setTitle("Transfer request rejected");
                notif.setBody("Your transfer request (" + messageId + ") was REJECTED. Reason: " + rejectRequest.getReason()
                        + "\n[TRANSFER_REQ_DECISION=REJECTED][requestId=" + messageId + "][rejectedBy=" + approver.getId() + "]");
                notif.setStatus(Message.MessageStatus.SENT);
                messageRepository.save(notif);
            });
        }

        log.info("Transfer request rejected. requestMessageId={}, rejectedBy={}, reason={}", messageId, approver.getId(), rejectRequest.getReason());

        return TransferRequestDTO.Response.builder()
                .requestId(messageId)
                .status(TransferRequestDTO.RequestStatus.REJECTED)
                .requestedByUserId(requesterId)
                .decidedByUserId(approver.getId())
                .decidedAt(LocalDateTime.now())
                .decisionReason(rejectRequest.getReason())
                .build();
    }

    private void validateCreateRequest(TransferRequestDTO.CreateRequest request) {
        if (!"WAREHOUSE".equalsIgnoreCase(request.getFromLocation())) {
            throw new IllegalArgumentException("fromLocation must be WAREHOUSE");
        }
        if (!"STORE".equalsIgnoreCase(request.getToLocation())) {
            throw new IllegalArgumentException("toLocation must be STORE");
        }
        long distinct = request.getItems().stream().map(TransferRequestDTO.Item::getProductId)
                .filter(Objects::nonNull).distinct().count();
        if (distinct != request.getItems().size()) {
            throw new IllegalArgumentException("Duplicate productId in items list");
        }
    }

    private String buildPendingBody(User requester, TransferRequestDTO.CreateRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("[TRANSFER_REQ_STATUS=PENDING]")
                .append("[TRANSFER_REQ_REQUESTED_BY=").append(requester.getId()).append("]")
                .append("[FROM=").append("WAREHOUSE").append("]")
                .append("[TO=").append("STORE").append("]");

        if (request.getNote() != null && !request.getNote().isBlank()) {
            sb.append("\nNote: ").append(request.getNote());
        }

        sb.append("\nItems:");
        for (TransferRequestDTO.Item item : request.getItems()) {
            sb.append("\n- productId=").append(item.getProductId())
                    .append(", qty=").append(item.getQuantity());
        }

        sb.append("\n\nAction: Inventory Manager can APPROVE or REJECT this transfer request.");
        return sb.toString();
    }

    private TransferRequestDTO.RequestStatus parseStatus(String body) {
        if (body == null) return TransferRequestDTO.RequestStatus.PENDING;
        Matcher m = STATUS_PATTERN.matcher(body);
        if (m.find()) return TransferRequestDTO.RequestStatus.valueOf(m.group(1));
        return TransferRequestDTO.RequestStatus.PENDING;
    }

    private Integer parseRequestedBy(String body) {
        if (body == null) return null;
        Matcher m = REQUESTED_BY_PATTERN.matcher(body);
        if (m.find()) return Integer.valueOf(m.group(1));
        return null;
    }

    private Integer parseDecidedBy(String body) {
        if (body == null) return null;
        Matcher m = DECIDED_BY_PATTERN.matcher(body);
        if (m.find()) return Integer.valueOf(m.group(1));
        return null;
    }

    private LocalDateTime parseDecidedAt(String body) {
        if (body == null) return null;
        Matcher m = DECIDED_AT_PATTERN.matcher(body);
        if (m.find()) {
            try {
                return LocalDateTime.parse(m.group(1));
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private String parseRejectReason(String body) {
        if (body == null) return null;
        Matcher m = REJECT_REASON_PATTERN.matcher(body);
        if (m.find()) return m.group(1);
        return null;
    }

    private TransferRequestDTO.CreateRequest parseCreateRequest(String body) {
        if (body == null) throw new IllegalArgumentException("Invalid request payload");

        String note = null;
        List<String> lines = body.lines().toList();
        int noteIdx = -1;
        int itemsIdx = -1;

        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith("Note:")) noteIdx = i;
            if (lines.get(i).equals("Items:")) itemsIdx = i;
        }

        if (noteIdx >= 0) {
            note = lines.get(noteIdx).substring("Note:".length()).trim();
        }

        if (itemsIdx < 0) {
            throw new IllegalArgumentException("Invalid request payload: missing items");
        }

        var items = new java.util.ArrayList<TransferRequestDTO.Item>();
        for (int i = itemsIdx + 1; i < lines.size(); i++) {
            String ln = lines.get(i).trim();
            if (!ln.startsWith("-")) break;

            String cleaned = ln.substring(1).trim();
            String[] parts = cleaned.split(",");

            Long productId = null;
            java.math.BigDecimal qty = null;

            for (String p : parts) {
                String[] kv = p.trim().split("=");
                if (kv.length != 2) continue;
                if (kv[0].trim().equals("productId")) productId = Long.valueOf(kv[1].trim());
                if (kv[0].trim().equals("qty")) qty = new java.math.BigDecimal(kv[1].trim());
            }

            if (productId == null || qty == null) {
                throw new IllegalArgumentException("Invalid item line: " + ln);
            }

            items.add(TransferRequestDTO.Item.builder().productId(productId).quantity(qty).build());
        }

        return TransferRequestDTO.CreateRequest.builder()
                .fromLocation("WAREHOUSE")
                .toLocation("STORE")
                .items(items)
                .note(note)
                .build();
    }

    private String markDecision(String body, TransferRequestDTO.RequestStatus newStatus, Integer decidedBy, String reason) {
        TransferRequestDTO.RequestStatus current = parseStatus(body);
        if (current != TransferRequestDTO.RequestStatus.PENDING) {
            throw new IllegalStateException("Request already decided");
        }

        String updated = body.replace("[TRANSFER_REQ_STATUS=PENDING]", "[TRANSFER_REQ_STATUS=" + newStatus.name() + "]");

        String decisionLine = "\n[TRANSFER_REQ_DECIDED_BY=" + decidedBy + "]" +
                "[TRANSFER_REQ_DECIDED_AT=" + LocalDateTime.now() + "]";
        if (newStatus == TransferRequestDTO.RequestStatus.REJECTED && reason != null) {
            decisionLine += "[TRANSFER_REQ_REJECT_REASON=" + reason.replace("\n", " ") + "]";
        }

        return updated + decisionLine;
    }

    private TransferRequestDTO.Response mapToResponse(Message msg) {
        TransferRequestDTO.RequestStatus status = parseStatus(msg.getBody());
        Integer requesterId = parseRequestedBy(msg.getBody());
        LocalDateTime requestedAt = LocalDateTime.ofInstant(msg.getCreatedAt().toInstant(), ZoneId.systemDefault());

        TransferRequestDTO.CreateRequest parsed = parseCreateRequest(msg.getBody());

        String requesterName = null;
        if (requesterId != null) {
            requesterName = userRepository.findById(requesterId)
                    .map(u -> (u.getFirstName() + " " + u.getLastName()).trim())
                    .orElse(null);
        }

        return TransferRequestDTO.Response.builder()
                .requestId(msg.getId())
                .status(status)
                .requestedByUserId(requesterId)
                .requestedAt(requestedAt)
                .note(parsed.getNote())
                // ✅ ADD
                .requesterName(requesterName)
                .items(parsed.getItems())
                .build();
    }

    private String extractNoteFromBody(String body) {
        if (body == null) return null;
        for (String line : body.lines().toList()) {
            if (line.startsWith("Note:")) {
                return line.substring("Note:".length()).trim();
            }
        }
        return null;
    }
}