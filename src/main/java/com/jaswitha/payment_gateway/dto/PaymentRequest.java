package com.jaswitha.payment_gateway.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {

    private Long fromAccountId;
    private Long toAccountId;
    private BigDecimal amount;
    private String idempotencyKey;
}