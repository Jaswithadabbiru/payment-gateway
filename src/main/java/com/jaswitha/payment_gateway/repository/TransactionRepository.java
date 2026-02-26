package com.jaswitha.payment_gateway.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.jaswitha.payment_gateway.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    boolean existsByIdempotencyKey(String idempotencyKey);
}