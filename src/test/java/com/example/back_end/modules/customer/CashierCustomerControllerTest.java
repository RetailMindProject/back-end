package com.example.back_end.modules.customer;

import com.example.back_end.exception.DuplicateResourceException;
import com.example.back_end.modules.customer.dto.CashierCustomerCreateRequest;
import com.example.back_end.modules.customer.service.CashierCustomerService;
import com.example.back_end.modules.register.entity.Customer;
import com.example.back_end.modules.register.entity.User;
import com.example.back_end.modules.register.repository.CustomerRepository;
import com.example.back_end.modules.register.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CashierCustomerControllerTest {

    @Mock
    CustomerRepository customerRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    CashierCustomerService service;

    @Test
    void searchPhone_notFound() {
        when(customerRepository.findByPhone("0770000000")).thenReturn(Optional.empty());
        assertThat(service.searchByPhone("0770000000")).isEmpty();
    }

    @Test
    void searchPhone_found() {
        Customer c = new Customer();
        c.setId(1);
        c.setPhone("0770000000");
        when(customerRepository.findByPhone("0770000000")).thenReturn(Optional.of(c));

        assertThat(service.searchByPhone("0770000000")).isPresent();
    }

    @Test
    void createCustomer_createsUserAndCustomer() {
        when(customerRepository.existsByPhone("0771111111")).thenReturn(false);
        when(customerRepository.existsByEmail("moath@example.com")).thenReturn(false);
        when(userRepository.existsByEmail("moath@example.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("ENC");

        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(99);
            return u;
        });

        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> {
            Customer c = inv.getArgument(0);
            c.setId(10);
            return c;
        });

        var created = service.createCustomer(CashierCustomerCreateRequest.builder()
                .firstName("Moath")
                .phone("0771111111")
                .email("moath@example.com")
                .gender("M")
                .build());

        assertThat(created.user().getId()).isEqualTo(99);
        assertThat(created.user().getRole()).isEqualTo(User.UserRole.CUSTOMER);
        assertThat(created.customer().getId()).isEqualTo(10);
        assertThat(created.customer().getUserId()).isEqualTo(99);

        verify(userRepository).save(any(User.class));
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void createCustomer_conflictPhone() {
        when(customerRepository.existsByPhone("0772222222")).thenReturn(true);

        assertThatThrownBy(() -> service.createCustomer(CashierCustomerCreateRequest.builder()
                .firstName("B")
                .phone("0772222222")
                .email("b@test.com")
                .build())).isInstanceOf(DuplicateResourceException.class);

        verifyNoInteractions(userRepository);
    }

    @Test
    void createCustomer_conflictEmail_customersTable() {
        when(customerRepository.existsByPhone("0774444444")).thenReturn(false);
        when(customerRepository.existsByEmail("dup@test.com")).thenReturn(true);

        assertThatThrownBy(() -> service.createCustomer(CashierCustomerCreateRequest.builder()
                .firstName("B")
                .phone("0774444444")
                .email("dup@test.com")
                .build())).isInstanceOf(DuplicateResourceException.class);

        verifyNoInteractions(userRepository);
    }

    @Test
    void createCustomer_conflictEmail_usersTable() {
        when(customerRepository.existsByPhone("0774444444")).thenReturn(false);
        when(customerRepository.existsByEmail("dup@test.com")).thenReturn(false);
        when(userRepository.existsByEmail("dup@test.com")).thenReturn(true);

        assertThatThrownBy(() -> service.createCustomer(CashierCustomerCreateRequest.builder()
                .firstName("B")
                .phone("0774444444")
                .email("dup@test.com")
                .build())).isInstanceOf(DuplicateResourceException.class);

        verifyNoMoreInteractions(userRepository);
    }
}
