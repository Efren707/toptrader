package com.toptrader.backend.friendship;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.toptrader.backend.user.User;
import com.toptrader.backend.user.UserRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

/** Covers Section 1 of the Friends milestone (docs/tasks/in-progress/friends.md) and ADR 0049. */
@ExtendWith(MockitoExtension.class)
class FriendshipServiceTest {

  private static final Long REQUESTER_ID = 1L;
  private static final Long ADDRESSEE_ID = 2L;
  private static final Long FRIENDSHIP_ID = 10L;

  @Mock private UserRepository userRepository;
  @Mock private FriendshipRepository friendshipRepository;

  private FriendshipService friendshipService;
  private User requester;
  private User addressee;

  @BeforeEach
  void setUp() {
    friendshipService = new FriendshipService(userRepository, friendshipRepository);
    requester = new User("requester@example.com", "requester", "hash", BigDecimal.ZERO);
    ReflectionTestUtils.setField(requester, "id", REQUESTER_ID);
    addressee = new User("addressee@example.com", "addressee", "hash", BigDecimal.ZERO);
    ReflectionTestUtils.setField(addressee, "id", ADDRESSEE_ID);
  }

  // sendFriendRequest

  @Test
  void sendFriendRequest_createsNewPendingRequest_whenNoneExists() {
    when(userRepository.findByIdForUpdate(REQUESTER_ID)).thenReturn(Optional.of(requester));
    when(userRepository.findByIdForUpdate(ADDRESSEE_ID)).thenReturn(Optional.of(addressee));
    when(friendshipRepository.findByUserPair(requester, addressee)).thenReturn(Optional.empty());

    FriendshipResponse response = friendshipService.sendFriendRequest(REQUESTER_ID, ADDRESSEE_ID);

    assertThat(response.status()).isEqualTo(Friendship.Status.PENDING);
    assertThat(response.requesterId()).isEqualTo(REQUESTER_ID);
    assertThat(response.addresseeId()).isEqualTo(ADDRESSEE_ID);
    verify(friendshipRepository).save(any(Friendship.class));
  }

  @Test
  void sendFriendRequest_toSelf_throwsBadRequest() {
    assertThatThrownBy(() -> friendshipService.sendFriendRequest(REQUESTER_ID, REQUESTER_ID))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST);
    verifyNoInteractions(userRepository, friendshipRepository);
  }

  @Test
  void sendFriendRequest_toNonexistentUser_throwsNotFound() {
    when(userRepository.findByIdForUpdate(REQUESTER_ID)).thenReturn(Optional.of(requester));
    when(userRepository.findByIdForUpdate(ADDRESSEE_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> friendshipService.sendFriendRequest(REQUESTER_ID, ADDRESSEE_ID))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
    verifyNoInteractions(friendshipRepository);
  }

  @Test
  void sendFriendRequest_asDemoAccount_throwsForbidden() {
    ReflectionTestUtils.setField(requester, "isDemo", true);
    when(userRepository.findByIdForUpdate(REQUESTER_ID)).thenReturn(Optional.of(requester));

    assertThatThrownBy(() -> friendshipService.sendFriendRequest(REQUESTER_ID, ADDRESSEE_ID))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);
    verifyNoInteractions(friendshipRepository);
  }

  @Test
  void sendFriendRequest_duplicateSameDirection_throwsConflict() {
    when(userRepository.findByIdForUpdate(REQUESTER_ID)).thenReturn(Optional.of(requester));
    when(userRepository.findByIdForUpdate(ADDRESSEE_ID)).thenReturn(Optional.of(addressee));
    Friendship existing = new Friendship(requester, addressee, Friendship.Status.PENDING);
    when(friendshipRepository.findByUserPair(requester, addressee))
        .thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> friendshipService.sendFriendRequest(REQUESTER_ID, ADDRESSEE_ID))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.CONFLICT);
    verify(friendshipRepository, never()).save(any());
  }

  @Test
  void sendFriendRequest_crossedRequest_autoAcceptsExistingRequest() {
    when(userRepository.findByIdForUpdate(REQUESTER_ID)).thenReturn(Optional.of(requester));
    when(userRepository.findByIdForUpdate(ADDRESSEE_ID)).thenReturn(Optional.of(addressee));
    // addressee already requested requester before this call - opposite direction.
    Friendship existing = new Friendship(addressee, requester, Friendship.Status.PENDING);
    when(friendshipRepository.findByUserPair(requester, addressee))
        .thenReturn(Optional.of(existing));

    FriendshipResponse response = friendshipService.sendFriendRequest(REQUESTER_ID, ADDRESSEE_ID);

    assertThat(response.status()).isEqualTo(Friendship.Status.ACCEPTED);
    assertThat(existing.getStatus()).isEqualTo(Friendship.Status.ACCEPTED);
    assertThat(existing.getRespondedAt()).isNotNull();
    verify(friendshipRepository).save(existing);
  }

  @Test
  void sendFriendRequest_alreadyFriends_throwsConflict() {
    when(userRepository.findByIdForUpdate(REQUESTER_ID)).thenReturn(Optional.of(requester));
    when(userRepository.findByIdForUpdate(ADDRESSEE_ID)).thenReturn(Optional.of(addressee));
    Friendship existing = new Friendship(requester, addressee, Friendship.Status.ACCEPTED);
    when(friendshipRepository.findByUserPair(requester, addressee))
        .thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> friendshipService.sendFriendRequest(REQUESTER_ID, ADDRESSEE_ID))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.CONFLICT);
    verify(friendshipRepository, never()).save(any());
  }

  // cancelFriendRequest

  @Test
  void cancelFriendRequest_deletesPendingFriendship() {
    Friendship friendship = new Friendship(requester, addressee, Friendship.Status.PENDING);
    when(friendshipRepository.findById(FRIENDSHIP_ID)).thenReturn(Optional.of(friendship));

    friendshipService.cancelFriendRequest(FRIENDSHIP_ID);

    verify(friendshipRepository).delete(friendship);
  }

  @Test
  void cancelFriendRequest_nonexistent_throwsNotFound() {
    when(friendshipRepository.findById(FRIENDSHIP_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> friendshipService.cancelFriendRequest(FRIENDSHIP_ID))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void cancelFriendRequest_notPending_throwsNotFound() {
    Friendship friendship = new Friendship(requester, addressee, Friendship.Status.ACCEPTED);
    when(friendshipRepository.findById(FRIENDSHIP_ID)).thenReturn(Optional.of(friendship));

    assertThatThrownBy(() -> friendshipService.cancelFriendRequest(FRIENDSHIP_ID))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
    verify(friendshipRepository, never()).delete(any());
  }

  @Test
  void cancelFriendRequest_asDemoRequester_throwsForbidden() {
    ReflectionTestUtils.setField(requester, "isDemo", true);
    Friendship friendship = new Friendship(requester, addressee, Friendship.Status.PENDING);
    when(friendshipRepository.findById(FRIENDSHIP_ID)).thenReturn(Optional.of(friendship));

    assertThatThrownBy(() -> friendshipService.cancelFriendRequest(FRIENDSHIP_ID))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);
    verify(friendshipRepository, never()).delete(any());
  }
}
