package com.toptrader.backend.user;

import com.toptrader.backend.friendship.Friendship;
import com.toptrader.backend.friendship.FriendshipRepository;
import com.toptrader.backend.friendship.RelationshipStatus;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserSearchService {

  private final UserRepository userRepository;
  private final FriendshipRepository friendshipRepository;

  public UserSearchService(
      UserRepository userRepository, FriendshipRepository friendshipRepository) {
    this.userRepository = userRepository;
    this.friendshipRepository = friendshipRepository;
  }

  @Transactional
  public List<UserSearchResult> searchUsers(Long callerId, String q) {
    User caller =
        this.userRepository
            .findById(callerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    String pattern = "%" + escapeLikePattern(q) + "%";
    List<User> matches = this.userRepository.searchByUsername(callerId, pattern, Limit.of(10));

    return matches.stream().map(user -> toResult(caller, user)).toList();
  }

  private UserSearchResult toResult(User caller, User user) {
    Optional<Friendship> friendship = this.friendshipRepository.findByUserPair(caller, user);
    RelationshipStatus relationshipStatus =
        friendship.map(f -> relationshipStatusOf(f, caller)).orElse(RelationshipStatus.NONE);
    return new UserSearchResult(
        user.getId(), user.getUsername(), user.getAvatarKey(), relationshipStatus);
  }

  private RelationshipStatus relationshipStatusOf(Friendship friendship, User caller) {
    if (friendship.getStatus() == Friendship.Status.ACCEPTED) {
      return RelationshipStatus.FRIENDS;
    }
    if (friendship.getRequester().getId().equals(caller.getId())) {
      return RelationshipStatus.OUTGOING_PENDING;
    }
    return RelationshipStatus.INCOMING_PENDING;
  }

  private String escapeLikePattern(String raw) {
    return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
