package com.example.back_end.modules.offer.service;

import com.example.back_end.modules.offer.dto.OfferRequestDTO;
import com.example.back_end.modules.offer.dto.OfferResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OfferService {

    /**
     * Create a new offer
     */
    OfferResponseDTO createOffer(OfferRequestDTO requestDTO);

    /**
     * Update an existing offer
     */
    OfferResponseDTO updateOffer(Long id, OfferRequestDTO requestDTO);

    /**
     * Get offer by ID
     */
    OfferResponseDTO getOfferById(Long id);

    /**
     * Get all offers with pagination
     */
    Page<OfferResponseDTO> getAllOffers(Pageable pageable);

    /**
     * Get all active offers
     */
    List<OfferResponseDTO> getActiveOffers();

    /**
     * Delete offer by ID
     */
    void deleteOffer(Long id);

    /**
     * Toggle offer active status
     */
    OfferResponseDTO toggleOfferStatus(Long id);

    /**
     * Check if offer code exists
     */
    boolean offerCodeExists(String code);
}