package com.toptrader.backend.friendship;

import com.toptrader.backend.user.User;
import com.toptrader.backend.user.UserRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FriendshipService {
  private static final Logger log = LoggerFactory.getLogger(FriendshipService.class);

  private final UserRepository userRepository;
  private final FriendshipRepository friendshipRepository;

  public FriendshipService(
      UserRepository userRepository, FriendshipRepository friendshipRepository) {
    this.userRepository = userRepository;
    this.friendshipRepository = friendshipRepository;
  }

  @PreAuthorize("#userId == authentication.principal.user.id")
  @Transactional
  public FriendshipResponse sendFriendRequest(Long userId, Long friendId) {
    if (userId.equals(friendId)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Cannot send a friend request to yourself");
    }

    User requester =
        this.userRepository
            .findByIdForUpdate(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    if (requester.isDemo()) {
      log.atWarn()
          .addKeyValue("userId", userId)
          .addKeyValue("reason", "Demo account is read-only")
          .log("Friend request failed");
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Demo accounts cannot send friend requests");
    }

    User addressee =
        this.userRepository
            .findByIdForUpdate(friendId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Friend not found"));

    Optional<Friendship> existing = this.friendshipRepository.findByUserPair(requester, addressee);

    if (existing.isPresent()) {
      Friendship friendship = existing.get();

      if (friendship.getStatus() == Friendship.Status.ACCEPTED) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Already friends");
      }

      if (friendship.getRequester().getId().equals(addressee.getId())) {
        friendship.setStatus(Friendship.Status.ACCEPTED);
        friendship.setRespondedAt(LocalDateTime.now());
        this.friendshipRepository.save(friendship);
        return toResponse(friendship);
      }

      throw new ResponseStatusException(HttpStatus.CONFLICT, "Friend request already pending");
    }

    Friendship friendship = new Friendship(requester, addressee, Friendship.Status.PENDING);
    this.friendshipRepository.save(friendship);
    return toResponse(friendship);
  }

  @PreAuthorize("@friendshipAuthorization.isRequester(#friendshipId, authentication.principal)")
  @Transactional
  public void cancelFriendRequest(Long friendshipId) {
    Friendship friendship =
        this.friendshipRepository
            .findById(friendshipId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Friendship not found"));

    if (friendship.getRequester().isDemo()) {
      log.atWarn()
          .addKeyValue("friendshipId", friendshipId)
          .addKeyValue("reason", "Demo account is read-only")
          .log("Friend request cancellation failed");
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Demo accounts cannot cancel friend requests");
    }

    if (friendship.getStatus() != Friendship.Status.PENDING) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Friendship cannot be cancelled");
    }

    this.friendshipRepository.delete(friendship);
  }

  private FriendshipResponse toResponse(Friendship friendship) {
    return new FriendshipResponse(
        friendship.getId(),
        friendship.getRequester().getId(),
        friendship.getAddressee().getId(),
        friendship.getStatus());
  }
}
