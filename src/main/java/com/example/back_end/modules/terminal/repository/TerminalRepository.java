package com.example.back_end.modules.terminal.repository;

import com.example.back_end.modules.terminal.entity.Terminal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TerminalRepository extends JpaRepository<Terminal, Long> {

    Optional<Terminal> findByCode(String code);

    List<Terminal> findByIsActiveTrue();

    boolean existsByCode(String code);
}