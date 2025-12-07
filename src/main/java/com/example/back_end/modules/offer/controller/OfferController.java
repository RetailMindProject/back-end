package com.example.back_end.modules.offer.controller;

import com.example.back_end.modules.offer.dto.OfferRequestDTO;
import com.example.back_end.modules.offer.dto.OfferResponseDTO;
import com.example.back_end.modules.offer.service.OfferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/offers")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE})
public class OfferController {

    private final OfferService offerService;

    /**
     * Create a new offer
     * POST /api/offers
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createOffer(@Valid @RequestBody OfferRequestDTO requestDTO) {
        log.info("REST request to create offer: {}", requestDTO.getTitle());

        try {
            OfferResponseDTO createdOffer = offerService.createOffer(requestDTO);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Offer created successfully");
            response.put("data", createdOffer);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("Error creating offer: {}", e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            log.error("Unexpected error creating offer", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "An unexpected error occurred: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Update an existing offer
     * PUT /api/offers/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateOffer(
            @PathVariable Long id,
            @Valid @RequestBody OfferRequestDTO requestDTO) {
        log.info("REST request to update offer with ID: {}", id);

        try {
            OfferResponseDTO updatedOffer = offerService.updateOffer(id, requestDTO);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Offer updated successfully");
            response.put("data", updatedOffer);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Error updating offer: {}", e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            log.error("Unexpected error updating offer", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "An unexpected error occurred");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get offer by ID
     * GET /api/offers/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getOfferById(@PathVariable Long id) {
        log.info("REST request to get offer with ID: {}", id);

        try {
            OfferResponseDTO offer = offerService.getOfferById(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", offer);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Offer not found: {}", e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    /**
     * Get all offers with pagination
     * GET /api/offers
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllOffers(
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("REST request to get all offers");

        Page<OfferResponseDTO> offers = offerService.getAllOffers(pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", offers.getContent());
        response.put("currentPage", offers.getNumber());
        response.put("totalItems", offers.getTotalElements());
        response.put("totalPages", offers.getTotalPages());

        return ResponseEntity.ok(response);
    }

    /**
     * Get all active offers
     * GET /api/offers/active
     */
    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActiveOffers() {
        log.info("REST request to get all active offers");

        List<OfferResponseDTO> activeOffers = offerService.getActiveOffers();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", activeOffers);
        response.put("count", activeOffers.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Delete offer by ID
     * DELETE /api/offers/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteOffer(@PathVariable Long id) {
        log.info("REST request to delete offer with ID: {}", id);

        try {
            offerService.deleteOffer(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Offer deleted successfully");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Error deleting offer: {}", e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    /**
     * Toggle offer active status
     * PATCH /api/offers/{id}/toggle-status
     */
    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<Map<String, Object>> toggleOfferStatus(@PathVariable Long id) {
        log.info("REST request to toggle status for offer with ID: {}", id);

        try {
            OfferResponseDTO updatedOffer = offerService.toggleOfferStatus(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Offer status toggled successfully");
            response.put("data", updatedOffer);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Error toggling offer status: {}", e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    /**
     * Check if offer code exists
     * GET /api/offers/check-code/{code}
     */
    @GetMapping("/check-code/{code}")
    public ResponseEntity<Map<String, Object>> checkOfferCode(@PathVariable String code) {
        log.info("REST request to check if offer code exists: {}", code);

        boolean exists = offerService.offerCodeExists(code);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("exists", exists);

        return ResponseEntity.ok(response);
    }
}