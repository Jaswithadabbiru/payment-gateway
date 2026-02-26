package com.jaswitha.payment_gateway.service;

import com.jaswitha.payment_gateway.entity.Account;
import com.jaswitha.payment_gateway.entity.Transaction;
import com.jaswitha.payment_gateway.exception.AccountNotFoundException;
import com.jaswitha.payment_gateway.exception.InsufficientBalanceException;
import com.jaswitha.payment_gateway.repository.AccountRepository;
import com.jaswitha.payment_gateway.repository.TransactionRepository;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final ExternalBankService externalBankService;

    @Transactional
    public Account createAccount(String name, BigDecimal balance, String currency) {
        Account account = Account.builder()
                .name(name)
                .balance(balance)
                .currency(currency)
                .build();

        return accountRepository.save(account);
    }

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    @Transactional
    public void deleteAccount(Long id) {
        accountRepository.deleteById(id);
    }

    // MAIN TRANSFER METHOD
    @Transactional
    public void transfer(Long fromId,
                         Long toId,
                         BigDecimal amount,
                         String idempotencyKey) {

        //1.Idempotency validation
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new RuntimeException("Idempotency key is required.");
        }

        if (transactionRepository.existsByIdempotencyKey(idempotencyKey)) {
            throw new RuntimeException("Duplicate transfer request detected.");
        }

        //2.Validate amount
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Transfer amount must be greater than zero.");
        }

        //3.Validate different accounts
        if (fromId.equals(toId)) {
            throw new RuntimeException("Sender and receiver cannot be the same account.");
        }

        //4.Fetch accounts
        Account sender = accountRepository.findById(fromId)
                .orElseThrow(() -> new AccountNotFoundException("Sender account not found"));

        Account receiver = accountRepository.findById(toId)
                .orElseThrow(() -> new AccountNotFoundException("Receiver account not found"));

        //5.Currency match validation
        if (!sender.getCurrency().equals(receiver.getCurrency())) {
            throw new RuntimeException("Currency mismatch between accounts.");
        }

        //6.Balance validation
        if (sender.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }

        //7.External rail call (protected by circuit breaker)
       callExternalRail();

        //8.Update balances
        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));

        accountRepository.save(sender);
        accountRepository.save(receiver);

        //9.Log DEBIT with idempotency key
        transactionRepository.save(Transaction.builder()
                .accountId(fromId)
                .amount(amount)
                .type("DEBIT")
                .timestamp(LocalDateTime.now())
                .idempotencyKey(idempotencyKey)
                .build());

        //10.Log CREDIT
        transactionRepository.save(Transaction.builder()
                .accountId(toId)
                .amount(amount)
                .type("CREDIT")
                .timestamp(LocalDateTime.now())
                .idempotencyKey(idempotencyKey + "-CREDIT")
                .build());
    }

    // Circuit breaker only for external rail
    @CircuitBreaker(name = "paymentService", fallbackMethod = "externalFallback")
    public void callExternalRail() {
        externalBankService.processPayment();
    }

    public void externalFallback(Throwable t) {
        throw new RuntimeException(
                "Payment temporarily unavailable due to external system failure. Please retry.");
    }
}