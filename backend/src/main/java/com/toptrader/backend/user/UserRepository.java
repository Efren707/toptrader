package com.toptrader.backend.user;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  boolean existsByUsername(String username);

  /** Locks the row for the life of the caller's transaction, so concurrent trades against the
   * same user serialize instead of racing on cash_balance. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from User u where u.id = :id")
  Optional<User> findByIdForUpdate(@Param("id") Long id);
}
