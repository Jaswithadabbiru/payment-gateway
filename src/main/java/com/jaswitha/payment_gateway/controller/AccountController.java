package com.jaswitha.payment_gateway.controller;

import com.jaswitha.payment_gateway.dto.PaymentRequest;
import com.jaswitha.payment_gateway.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/transfer")
    public ResponseEntity<String> transfer(@RequestBody PaymentRequest request) {

        accountService.transfer(
                request.getFromAccountId(),
                request.getToAccountId(),
                request.getAmount(),
                request.getIdempotencyKey()
        );

        return ResponseEntity.ok("Transfer successful");
    }
}