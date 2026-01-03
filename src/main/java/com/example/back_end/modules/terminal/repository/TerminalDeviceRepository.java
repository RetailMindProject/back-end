package com.example.back_end.modules.terminal.repository;

import com.example.back_end.modules.terminal.entity.TerminalDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for TerminalDevice entity
 */
@Repository
public interface TerminalDeviceRepository extends JpaRepository<TerminalDevice, Long> {

    /**
     * Find ACTIVE device by browser token hash
     */
    Optional<TerminalDevice> findByTokenHashAndRevokedAtIsNull(String tokenHash);

    /**
     * Find ACTIVE device by terminal ID
     * (Terminal must have at most one active device)
     */
    Optional<TerminalDevice> findByTerminalIdAndRevokedAtIsNull(Long terminalId);

    /**
     * Check if terminal has an active paired device
     */
    boolean existsByTerminalIdAndRevokedAtIsNull(Long terminalId);

    /**
     * Check if browser token is already paired
     */
    boolean existsByTokenHashAndRevokedAtIsNull(String tokenHash);

    /**
     * Find all devices for a terminal (admin use - includes revoked)
     */
    List<TerminalDevice> findByTerminalId(Long terminalId);

    List<TerminalDevice> findByTokenHash(String tokenHash);

}