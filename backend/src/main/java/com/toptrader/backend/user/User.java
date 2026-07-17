package com.toptrader.backend.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "users")
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false, unique = true)
  private String username;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(name = "cash_balance", nullable = false)
  private BigDecimal cashBalance;

  @Column(name = "failed_attempts", nullable = false)
  private int failedAttempts;

  @Column(name = "locked_until")
  private LocalDateTime lockedUntil;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  protected User() {}

  public User(String email, String username, String passwordHash, BigDecimal cashBalance) {
    this.email = email;
    this.username = username;
    this.passwordHash = passwordHash;
    this.cashBalance = cashBalance;
    this.failedAttempts = 0;
  }

  public Long getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getUsername() {
    return username;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public BigDecimal getCashBalance() {
    return cashBalance;
  }

  public void setCashBalance(BigDecimal cashBalance) {
    this.cashBalance = cashBalance;
  }

  public int getFailedAttempts() {
    return failedAttempts;
  }

  public void setFailedAttempts(int failedAttempts) {
    this.failedAttempts = failedAttempts;
  }

  public LocalDateTime getLockedUntil() {
    return lockedUntil;
  }

  public void setLockedUntil(LocalDateTime lockedUntil) {
    this.lockedUntil = lockedUntil;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }
}
