package com.toptrader.backend.friendship;

import com.toptrader.backend.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "friendships")
public class Friendship {

  public enum Status {
    PENDING,
    ACCEPTED
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "requester_id", nullable = false)
  private User requester;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "addressee_id", nullable = false)
  private User addressee;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "responded_at")
  private LocalDateTime respondedAt;

  public Friendship() {}

  public Friendship(User requester, User addressee, Status status) {
    this.requester = requester;
    this.addressee = addressee;
    this.status = status;
  }

  public Long getId() {
    return this.id;
  }

  public User getRequester() {
    return this.requester;
  }

  public void setRequester(User requesterId) {
    this.requester = requesterId;
  }

  public User getAddressee() {
    return this.addressee;
  }

  public void setAddressee(User addresseeId) {
    this.addressee = addresseeId;
  }

  public Status getStatus() {
    return this.status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public LocalDateTime getRespondedAt() {
    return this.respondedAt;
  }

  public void setRespondedAt(LocalDateTime respondedAt) {
    this.respondedAt = respondedAt;
  }

  public LocalDateTime getCreatedAt() {
    return this.createdAt;
  }
}
