package com.jaswitha.payment_gateway.service;

import org.springframework.stereotype.Service;

@Service
public class ExternalBankService {

    public void processPayment() {

        // Simulate 50% external system failure
        if (Math.random() < 0.5) {
            throw new RuntimeException("External ledger system unavailable");
        }
    }
}