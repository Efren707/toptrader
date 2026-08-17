package com.toptrader.backend.user;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  boolean existsByUsername(String username);

  boolean existsByEmailAndIdNot(String email, Long userId);

  boolean existsByUsernameAndIdNot(String username, Long userId);

  @Query(
      "select u from User u where LOWER(u.username) LIKE LOWER(:pattern) ESCAPE '\\' "
          + "AND u.id != :id AND u.isDemo = false ORDER BY u.username")
  List<User> searchByUsername(@Param("id") Long id, @Param("pattern") String pattern, Limit limit);

  /**
   * Locks the row for the life of the caller's transaction, so concurrent trades against the same
   * user serialize instead of racing on cash_balance.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from User u where u.id = :id")
  Optional<User> findByIdForUpdate(@Param("id") Long id);
}
