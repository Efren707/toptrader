package com.toptrader.backend.trading;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.toptrader.backend.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "holdings")
public class Holding {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false)
  private String ticker;

  @Column(nullable = false)
  private Integer quantity;

  @Column(name = "average_cost_basis", nullable = false)
  private BigDecimal averageCostBasis;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  protected Holding() {}

  public Holding(
      User user,
      String ticker,
      Integer quantity,
      BigDecimal averageCostBasis,
      LocalDateTime updatedAt) {
    this.user = user;
    this.ticker = ticker;
    this.quantity = quantity;
    this.averageCostBasis = averageCostBasis;
    this.updatedAt = updatedAt;
  }

  public Long getId() {
    return id;
  }

  @JsonIgnore
  public User getUser() {
    return this.user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public String getTicker() {
    return ticker;
  }

  public void setTicker(String ticker) {
    this.ticker = ticker;
  }

  public Integer getQuantity() {
    return quantity;
  }

  public void setQuantity(Integer quantity) {
    this.quantity = quantity;
  }

  public BigDecimal getAverageCostBasis() {
    return averageCostBasis;
  }

  public void setAverageCostBasis(BigDecimal averageCostBasis) {
    this.averageCostBasis = averageCostBasis;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
