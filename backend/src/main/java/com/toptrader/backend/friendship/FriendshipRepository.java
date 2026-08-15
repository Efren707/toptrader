package com.toptrader.backend.friendship;

import com.toptrader.backend.user.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
  @Query(
      "SELECT f FROM Friendship f "
          + "WHERE (f.requester = :userA AND f.addressee = :userB) "
          + "OR (f.requester = :userB AND f.addressee = :userA)")
  Optional<Friendship> findByUserPair(@Param("userA") User userA, @Param("userB") User userB);
}
