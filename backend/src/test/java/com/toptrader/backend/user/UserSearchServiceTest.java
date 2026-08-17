package com.toptrader.backend.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.toptrader.backend.friendship.Friendship;
import com.toptrader.backend.friendship.FriendshipRepository;
import com.toptrader.backend.friendship.RelationshipStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

/** Covers Section 3's GET /users/search logic (docs/tasks/in-progress/friends.md). */
@ExtendWith(MockitoExtension.class)
class UserSearchServiceTest {

  private static final Long CALLER_ID = 1L;
  private static final Long MATCH_ID = 2L;

  @Mock private UserRepository userRepository;
  @Mock private FriendshipRepository friendshipRepository;

  private UserSearchService userSearchService;
  private User caller;
  private User match;

  @BeforeEach
  void setUp() {
    userSearchService = new UserSearchService(userRepository, friendshipRepository);
    caller = new User("caller@example.com", "caller", "hash", BigDecimal.ZERO);
    ReflectionTestUtils.setField(caller, "id", CALLER_ID);
    match = new User("match@example.com", "match", "hash", BigDecimal.ZERO);
    ReflectionTestUtils.setField(match, "id", MATCH_ID);
  }

  @Test
  void searchUsers_callerNotFound_throwsNotFound() {
    when(userRepository.findById(CALLER_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userSearchService.searchUsers(CALLER_ID, "query"))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
    verifyNoInteractions(friendshipRepository);
  }

  @Test
  void searchUsers_escapesPercentInQuery() {
    when(userRepository.findById(CALLER_ID)).thenReturn(Optional.of(caller));
    when(userRepository.searchByUsername(eq(CALLER_ID), anyString(), any(Limit.class)))
        .thenReturn(List.of());

    userSearchService.searchUsers(CALLER_ID, "abc%def");

    ArgumentCaptor<String> patternCaptor = ArgumentCaptor.forClass(String.class);
    verify(userRepository)
        .searchByUsername(eq(CALLER_ID), patternCaptor.capture(), any(Limit.class));
    assertThat(patternCaptor.getValue()).isEqualTo("%abc\\%def%");
  }

  @Test
  void searchUsers_escapesUnderscoreInQuery() {
    when(userRepository.findById(CALLER_ID)).thenReturn(Optional.of(caller));
    when(userRepository.searchByUsername(eq(CALLER_ID), anyString(), any(Limit.class)))
        .thenReturn(List.of());

    userSearchService.searchUsers(CALLER_ID, "abc_def");

    ArgumentCaptor<String> patternCaptor = ArgumentCaptor.forClass(String.class);
    verify(userRepository)
        .searchByUsername(eq(CALLER_ID), patternCaptor.capture(), any(Limit.class));
    assertThat(patternCaptor.getValue()).isEqualTo("%abc\\_def%");
  }

  @Test
  void searchUsers_escapesBackslashInQuery() {
    when(userRepository.findById(CALLER_ID)).thenReturn(Optional.of(caller));
    when(userRepository.searchByUsername(eq(CALLER_ID), anyString(), any(Limit.class)))
        .thenReturn(List.of());

    userSearchService.searchUsers(CALLER_ID, "a\\b");

    ArgumentCaptor<String> patternCaptor = ArgumentCaptor.forClass(String.class);
    verify(userRepository)
        .searchByUsername(eq(CALLER_ID), patternCaptor.capture(), any(Limit.class));
    assertThat(patternCaptor.getValue()).isEqualTo("%a\\\\b%");
  }

  @Test
  void searchUsers_noFriendshipRow_mapsToNone() {
    when(userRepository.findById(CALLER_ID)).thenReturn(Optional.of(caller));
    when(userRepository.searchByUsername(eq(CALLER_ID), anyString(), any(Limit.class)))
        .thenReturn(List.of(match));
    when(friendshipRepository.findByUserPair(caller, match)).thenReturn(Optional.empty());

    List<UserSearchResult> results = userSearchService.searchUsers(CALLER_ID, "match");

    assertThat(results).hasSize(1);
    assertThat(results.get(0).relationshipStatus()).isEqualTo(RelationshipStatus.NONE);
  }

  @Test
  void searchUsers_acceptedFriendship_mapsToFriends() {
    when(userRepository.findById(CALLER_ID)).thenReturn(Optional.of(caller));
    when(userRepository.searchByUsername(eq(CALLER_ID), anyString(), any(Limit.class)))
        .thenReturn(List.of(match));
    Friendship friendship = new Friendship(caller, match, Friendship.Status.ACCEPTED);
    when(friendshipRepository.findByUserPair(caller, match)).thenReturn(Optional.of(friendship));

    List<UserSearchResult> results = userSearchService.searchUsers(CALLER_ID, "match");

    assertThat(results.get(0).relationshipStatus()).isEqualTo(RelationshipStatus.FRIENDS);
  }

  @Test
  void searchUsers_callerIsRequesterOfPending_mapsToOutgoingPending() {
    when(userRepository.findById(CALLER_ID)).thenReturn(Optional.of(caller));
    when(userRepository.searchByUsername(eq(CALLER_ID), anyString(), any(Limit.class)))
        .thenReturn(List.of(match));
    Friendship friendship = new Friendship(caller, match, Friendship.Status.PENDING);
    when(friendshipRepository.findByUserPair(caller, match)).thenReturn(Optional.of(friendship));

    List<UserSearchResult> results = userSearchService.searchUsers(CALLER_ID, "match");

    assertThat(results.get(0).relationshipStatus()).isEqualTo(RelationshipStatus.OUTGOING_PENDING);
  }

  @Test
  void searchUsers_callerIsAddresseeOfPending_mapsToIncomingPending() {
    when(userRepository.findById(CALLER_ID)).thenReturn(Optional.of(caller));
    when(userRepository.searchByUsername(eq(CALLER_ID), anyString(), any(Limit.class)))
        .thenReturn(List.of(match));
    Friendship friendship = new Friendship(match, caller, Friendship.Status.PENDING);
    when(friendshipRepository.findByUserPair(caller, match)).thenReturn(Optional.of(friendship));

    List<UserSearchResult> results = userSearchService.searchUsers(CALLER_ID, "match");

    assertThat(results.get(0).relationshipStatus()).isEqualTo(RelationshipStatus.INCOMING_PENDING);
  }
}
