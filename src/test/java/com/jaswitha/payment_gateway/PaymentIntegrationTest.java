package com.jaswitha.payment_gateway;

import com.jaswitha.payment_gateway.dto.PaymentRequest;
import com.jaswitha.payment_gateway.entity.Account;
import com.jaswitha.payment_gateway.repository.AccountRepository;
import com.jaswitha.payment_gateway.service.ExternalBankService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PaymentIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AccountRepository accountRepository;

    // 🔥 THIS is the fix
    @MockBean
    private ExternalBankService externalBankService;

    @Test
    void shouldTransferMoneySuccessfully() {

        // Mock external rail so it does NOT throw exception
        doNothing().when(externalBankService).processPayment();

        // Clean DB before test
        accountRepository.deleteAll();

        // Create accounts
        Account sender = accountRepository.save(
                Account.builder()
                        .name("Alice")
                        .balance(new BigDecimal("1000"))
                        .currency("INR")
                        .build()
        );

        Account receiver = accountRepository.save(
                Account.builder()
                        .name("Bob")
                        .balance(new BigDecimal("500"))
                        .currency("INR")
                        .build()
        );

        // Prepare request
        PaymentRequest request = PaymentRequest.builder()
                .fromAccountId(sender.getId())
                .toAccountId(receiver.getId())
                .amount(new BigDecimal("100"))
                .idempotencyKey("test-key-123")
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<PaymentRequest> entity =
                new HttpEntity<>(request, headers);

        // Call API
        ResponseEntity<String> response =
                restTemplate.postForEntity(
                        "http://localhost:" + port + "/accounts/transfer",
                        entity,
                        String.class
                );

        // Verify HTTP response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Reload from DB
        Account updatedSender =
                accountRepository.findById(sender.getId()).orElseThrow();

        Account updatedReceiver =
                accountRepository.findById(receiver.getId()).orElseThrow();

        // Verify balances changed
        assertThat(updatedSender.getBalance())
                .isEqualByComparingTo("900");

        assertThat(updatedReceiver.getBalance())
                .isEqualByComparingTo("600");
    }
}