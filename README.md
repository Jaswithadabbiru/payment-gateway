![Java CI with Maven]([![Java CI with Maven](https://github.com/Jaswithadabbiru/payment-gateway/actions/workflows/maven.yml/badge.svg)](https://github.com/Jaswithadabbiru/payment-gateway/actions/workflows/maven.yml))
# High-Availability Payment Gateway

A production-style backend payment gateway built using Spring Boot and PostgreSQL, designed with resiliency, transactional integrity, and CI/CD automation.

## Tech Stack

- Java 21
- Spring Boot 3
- PostgreSQL
- JPA / Hibernate
- Resilience4j (Circuit Breaker)
- JUnit 5 & Mockito
- H2 (Test Isolation)
- GitHub Actions (CI/CD)
- Docker-ready

## Key Engineering Features

- ACID-compliant transactional money transfer
- Idempotent transaction handling to prevent duplicate payments
- Optimistic locking for concurrent financial operations
- Circuit breaker pattern for external payment rail protection
- Full integration testing hitting real REST endpoints
- CI pipeline with automated test execution

## Architecture Highlights

- Controller → Service → Repository layered design
- ExternalBankService protected by Resilience4j
- Separate test profile using in-memory H2 database
- Clean separation of production and test environments

## Running Locally

1. Set environment variables:
   DB_USERNAME
   DB_PASSWORD

2. Run:
   ./mvnw spring-boot:run

## Run Tests

./mvnw clean test

All tests execute automatically via GitHub Actions on push.

---

Engineered for high-availability financial systems.