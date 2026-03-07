package com.example.back_end.modules.inventory.service;

import com.example.back_end.modules.catalog.product.entity.Product;
import com.example.back_end.modules.messages.entity.Message;
import com.example.back_end.modules.messages.repository.MessageRepository;
import com.example.back_end.modules.register.entity.User;
import com.example.back_end.modules.register.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LowStockAlertService {

    /**
     * Default must be 5.
     */
    @Value("${app.inventory.low-stock-threshold:5}")
    private int lowStockThreshold;

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;

    /**
     * Called after decrement under the same stock_snapshot lock/transaction.
     * Creates unread notifications to active STORE_MANAGER users only when crossing the threshold.
     */
    @Transactional
    public void handleLowStock(Product product, BigDecimal prevQty, BigDecimal newQty, Long orderId) {
        if (product == null || product.getId() == null) {
            return;
        }
        if (prevQty == null || newQty == null) {
            return;
        }

        BigDecimal threshold = BigDecimal.valueOf(lowStockThreshold);

        // Trigger only on crossing: prev > threshold && new <= threshold
        if (prevQty.compareTo(threshold) <= 0 || newQty.compareTo(threshold) > 0) {
            return;
        }

        List<User> storeManagers = userRepository.findByRoleAndIsActiveTrue(User.UserRole.STORE_MANAGER);
        if (storeManagers.isEmpty()) {
            log.debug("Low stock crossed for productId={}, but no active STORE_MANAGER users found", product.getId());
            return;
        }

        for (User manager : storeManagers) {
            boolean alreadyUnread = messageRepository.existsUnreadLowStockAlert(manager.getId(), product.getId());
            if (alreadyUnread) {
                log.debug("Skip low stock alert (already unread) for productId={}, toUserId={}", product.getId(), manager.getId());
                continue;
            }

            Message msg = new Message();
            msg.setFromUser(null); // system
            msg.setToUser(manager);
            msg.setTitle("Low stock alert");

            // Stable signature to support duplicate check without DB schema changes.
            String signature = "[LOW_STOCK][productId=" + product.getId() + "][threshold=" + lowStockThreshold + "][orderId=" + orderId + "]";

            msg.setBody("Product " + product.getName() + " (SKU " + product.getSku() + ") is low in store: "
                    + newQty + " left. Threshold is " + lowStockThreshold + ".\n" + signature);

            msg.setStatus(Message.MessageStatus.SENT);
            msg.setReadAt(null);

            messageRepository.save(msg);
            log.info("Low stock alert created for productId={}, newQty={}, toUserId={}", product.getId(), newQty, manager.getId());
        }
    }
}
