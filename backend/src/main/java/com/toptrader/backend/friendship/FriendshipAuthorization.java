package com.toptrader.backend.friendship;

import com.toptrader.backend.user.UserPrincipal;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class FriendshipAuthorization {

  private final FriendshipRepository friendshipRepository;

  public FriendshipAuthorization(FriendshipRepository friendshipRepository) {
    this.friendshipRepository = friendshipRepository;
  }

  public boolean isRequester(Long friendshipId, UserPrincipal userPrincipal) {
    Optional<Friendship> friendship = friendshipRepository.findById(friendshipId);
    return friendship.isEmpty()
        || friendship.get().getRequester().getId().equals(userPrincipal.getUser().getId());
  }

  public boolean isAddressee(Long friendshipId, UserPrincipal userPrincipal) {
    Optional<Friendship> friendship = friendshipRepository.findById(friendshipId);
    return friendship.isEmpty()
        || friendship.get().getAddressee().getId().equals(userPrincipal.getUser().getId());
  }
}
