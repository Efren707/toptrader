package com.toptrader.backend.friendship;

import com.toptrader.backend.user.UserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/friends")
public class FriendshipController {

  private final FriendshipService friendshipService;

  public FriendshipController(FriendshipService friendshipService) {
    this.friendshipService = friendshipService;
  }

  @PostMapping("/requests")
  public ResponseEntity<FriendshipResponse> sendFriendRequest(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @Valid @RequestBody FriendshipRequest request) {
    FriendshipResponse friendRequest =
        this.friendshipService.sendFriendRequest(
            userPrincipal.getUser().getId(), request.addresseeId());

    if (friendRequest.status() == Friendship.Status.PENDING) {
      return ResponseEntity.status(HttpStatus.CREATED).body(friendRequest);
    }

    return ResponseEntity.status(HttpStatus.OK).body(friendRequest);
  }

  @DeleteMapping("/requests/{friendshipId}")
  public ResponseEntity<Void> deleteFriendRequest(@PathVariable Long friendshipId) {
    this.friendshipService.cancelFriendRequest(friendshipId);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @PostMapping("/requests/{friendshipId}/accept")
  public ResponseEntity<FriendshipResponse> acceptFriendRequest(@PathVariable Long friendshipId) {
    FriendshipResponse friendshipResponse =
        this.friendshipService.acceptFriendRequest(friendshipId);
    return ResponseEntity.status(HttpStatus.OK).body(friendshipResponse);
  }

  @PostMapping("/requests/{friendshipId}/decline")
  public ResponseEntity<Void> declineFriendRequest(@PathVariable Long friendshipId) {
    this.friendshipService.declineFriendRequest(friendshipId);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @DeleteMapping("/{userId}")
  public ResponseEntity<Void> deleteFriend(
      @AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable Long userId) {
    this.friendshipService.removeFriend(userPrincipal.getUser().getId(), userId);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @GetMapping("/requests/incoming")
  public ResponseEntity<List<IncomingFriendRequest>> getIncomingFriendRequests(
      @AuthenticationPrincipal UserPrincipal userPrincipal) {
    List<IncomingFriendRequest> response =
        this.friendshipService.getIncomingFriendRequests(userPrincipal.getUser());
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @GetMapping("/requests/outgoing")
  public ResponseEntity<List<OutgoingFriendRequest>> getOutgoingFriendRequests(
      @AuthenticationPrincipal UserPrincipal userPrincipal) {
    List<OutgoingFriendRequest> response =
        this.friendshipService.getOutgoingFriendRequests(userPrincipal.getUser());
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @GetMapping
  public ResponseEntity<List<FriendResponse>> getFriends(
      @AuthenticationPrincipal UserPrincipal userPrincipal) {
    List<FriendResponse> response = this.friendshipService.getFriends(userPrincipal.getUser());
    return new ResponseEntity<>(response, HttpStatus.OK);
  }
}
