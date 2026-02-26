package com.jaswitha.payment_gateway.service;

import com.jaswitha.payment_gateway.entity.Account;
import com.jaswitha.payment_gateway.exception.AccountNotFoundException;
import com.jaswitha.payment_gateway.exception.InsufficientBalanceException;
import com.jaswitha.payment_gateway.repository.AccountRepository;
import com.jaswitha.payment_gateway.repository.TransactionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ExternalBankService externalBankService;

    @InjectMocks
    private AccountService accountService;

    private Account sender;
    private Account receiver;

    @BeforeEach
    void setup() {
        sender = Account.builder()
                .id(1L)
                .name("Alice")
                .balance(new BigDecimal("1000"))
                .currency("INR")
                .build();

        receiver = Account.builder()
                .id(2L)
                .name("Bob")
                .balance(new BigDecimal("500"))
                .currency("INR")
                .build();
    }

    @Test
    void shouldTransferSuccessfully() {

        when(transactionRepository.existsByIdempotencyKey(anyString()))
                .thenReturn(false);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(receiver));

        doNothing().when(externalBankService).processPayment();

        accountService.transfer(1L, 2L,
                new BigDecimal("100"),
                "key-success");

        assertEquals(new BigDecimal("900"), sender.getBalance());
        assertEquals(new BigDecimal("600"), receiver.getBalance());

        verify(accountRepository, times(2)).save(any(Account.class));
        verify(transactionRepository, times(2)).save(any());
    }

    @Test
    void shouldThrowInsufficientBalance() {

        when(transactionRepository.existsByIdempotencyKey(anyString()))
                .thenReturn(false);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(receiver));

        assertThrows(InsufficientBalanceException.class, () ->
                accountService.transfer(1L, 2L,
                        new BigDecimal("2000"),
                        "key-balance"));
    }

    @Test
    void shouldThrowAccountNotFound() {

        when(transactionRepository.existsByIdempotencyKey(anyString()))
                .thenReturn(false);

        when(accountRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () ->
                accountService.transfer(1L, 2L,
                        new BigDecimal("100"),
                        "key-notfound"));
    }

    @Test
    void shouldThrowCurrencyMismatch() {

        when(transactionRepository.existsByIdempotencyKey(anyString()))
                .thenReturn(false);

        receiver.setCurrency("USD");

        when(accountRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(receiver));

        assertThrows(RuntimeException.class, () ->
                accountService.transfer(1L, 2L,
                        new BigDecimal("100"),
                        "key-currency"));
    }

    @Test
    void shouldThrowWhenExternalFails() {

        when(transactionRepository.existsByIdempotencyKey(anyString()))
                .thenReturn(false);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(receiver));

        doThrow(new RuntimeException("External down"))
                .when(externalBankService).processPayment();

        assertThrows(RuntimeException.class, () ->
                accountService.transfer(1L, 2L,
                        new BigDecimal("100"),
                        "key-external"));
    }
}