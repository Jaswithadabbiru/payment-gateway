package com.jaswitha.payment_gateway.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.jaswitha.payment_gateway.entity.Account;

public interface AccountRepository extends JpaRepository<Account, Long> {
}