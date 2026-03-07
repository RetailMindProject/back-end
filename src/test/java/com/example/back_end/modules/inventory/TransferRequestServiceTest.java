package com.example.back_end.modules.inventory;

import com.example.back_end.exception.CustomException;
import com.example.back_end.modules.inventory.dto.TransferRequestDTO;
import com.example.back_end.modules.inventory.service.TransferRequestService;
import com.example.back_end.modules.messages.entity.Message;
import com.example.back_end.modules.messages.repository.MessageRepository;
import com.example.back_end.modules.register.entity.User;
import com.example.back_end.modules.register.entity.User.UserRole;
import com.example.back_end.modules.register.repository.UserRepository;
import com.example.back_end.modules.store_product.service.StoreProductService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TransferRequestServiceTest {

    @AfterEach
    void cleanup() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void createRequest_storeManager_createsPendingMessages_noInventoryChange() {
        UserRepository userRepo = mock(UserRepository.class);
        MessageRepository msgRepo = mock(MessageRepository.class);
        StoreProductService storeProductService = mock(StoreProductService.class);

        TransferRequestService service = new TransferRequestService(userRepo, msgRepo, storeProductService);

        User manager = new User();
        manager.setId(10);
        manager.setEmail("sm@test.com");
        manager.setRole(UserRole.STORE_MANAGER);
        manager.setIsActive(true);

        when(userRepo.findByEmail("sm@test.com")).thenReturn(Optional.of(manager));

        User inv1 = new User();
        inv1.setId(20);
        inv1.setRole(UserRole.INVENTORY_MANAGER);
        inv1.setIsActive(true);

        when(userRepo.findByRoleAndIsActiveTrue(UserRole.INVENTORY_MANAGER)).thenReturn(List.of(inv1));
        when(msgRepo.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            m.setId(1L);
            return m;
        });

        // auth context
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("sm@test.com", null)
        );

        TransferRequestDTO.CreateRequest req = TransferRequestDTO.CreateRequest.builder()
                .fromLocation("WAREHOUSE")
                .toLocation("STORE")
                .note("need items")
                .items(List.of(
                        TransferRequestDTO.Item.builder().productId(1L).quantity(new BigDecimal("2")).build()
                ))
                .build();

        TransferRequestDTO.Response resp = service.createRequest(req);

        assertThat(resp.getStatus()).isEqualTo(TransferRequestDTO.RequestStatus.PENDING);
        verify(storeProductService, never()).transferFromInventoryToStore(any());
    }

    @Test
    void approve_executesTransfers_andPreventsDoubleApproval() {
        UserRepository userRepo = mock(UserRepository.class);
        MessageRepository msgRepo = mock(MessageRepository.class);
        StoreProductService storeProductService = mock(StoreProductService.class);

        TransferRequestService service = new TransferRequestService(userRepo, msgRepo, storeProductService);

        User inv = new User();
        inv.setId(20);
        inv.setEmail("im@test.com");
        inv.setRole(UserRole.INVENTORY_MANAGER);
        inv.setIsActive(true);

        when(userRepo.findByEmail("im@test.com")).thenReturn(Optional.of(inv));

        Message reqMsg = new Message();
        reqMsg.setId(99L);
        reqMsg.setTitle("Transfer request");
        reqMsg.setBody("[TRANSFER_REQ_STATUS=PENDING][TRANSFER_REQ_REQUESTED_BY=10][FROM=WAREHOUSE][TO=STORE]\nItems:\n- productId=1, qty=2\n");

        when(msgRepo.findById(99L)).thenReturn(Optional.of(reqMsg));
        when(msgRepo.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("im@test.com", null)
        );

        service.approve(99L);
        verify(storeProductService, times(1)).transferFromInventoryToStore(any());

        // second approval should fail
        assertThatThrownBy(() -> service.approve(99L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void createRequest_nonStoreManager_forbidden() {
        UserRepository userRepo = mock(UserRepository.class);
        MessageRepository msgRepo = mock(MessageRepository.class);
        StoreProductService storeProductService = mock(StoreProductService.class);

        TransferRequestService service = new TransferRequestService(userRepo, msgRepo, storeProductService);

        User cashier = new User();
        cashier.setId(11);
        cashier.setEmail("c@test.com");
        cashier.setRole(UserRole.CASHIER);
        cashier.setIsActive(true);

        when(userRepo.findByEmail("c@test.com")).thenReturn(Optional.of(cashier));

        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("c@test.com", null)
        );

        TransferRequestDTO.CreateRequest req = TransferRequestDTO.CreateRequest.builder()
                .fromLocation("WAREHOUSE")
                .toLocation("STORE")
                .items(List.of(TransferRequestDTO.Item.builder().productId(1L).quantity(new BigDecimal("1")).build()))
                .build();

        assertThatThrownBy(() -> service.createRequest(req))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("STORE_MANAGER");
    }
}

