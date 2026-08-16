package com.toptrader.backend.friendship;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.toptrader.backend.user.User;
import com.toptrader.backend.user.UserPrincipal;
import com.toptrader.backend.user.UserRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Verifies ADR 0049's two-party {@code @PreAuthorize} guards on {@link FriendshipService}: the
 * simple userId-binding on {@code sendFriendRequest} (ADR 0035's pattern), and the
 * resource-lookup-based {@code isRequester} check on {@code cancelFriendRequest} via {@link
 * FriendshipAuthorization} - the codebase's first two-party IDOR guard that's actually reachable
 * through normal use, unlike the userId-binding checks (see ADR 0035's consequences).
 */
@SpringBootTest
class FriendshipServiceAuthorizationTest {

  private static final long AUTHENTICATED_USER_ID = 1L;
  private static final long OTHER_USER_ID = 2L;
  private static final long FRIENDSHIP_ID = 10L;

  @Autowired private FriendshipService friendshipService;

  @MockitoBean private UserRepository userRepository;
  @MockitoBean private FriendshipRepository friendshipRepository;

  private User authenticatedUser;

  @BeforeEach
  void authenticateAsUser() {
    authenticatedUser = new User("owner@example.com", "owner", "hash", BigDecimal.ZERO);
    ReflectionTestUtils.setField(authenticatedUser, "id", AUTHENTICATED_USER_ID);
    UserPrincipal principal = new UserPrincipal(authenticatedUser);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
  }

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void sendFriendRequest_deniesRequestForAnotherUsersId() {
    assertThatThrownBy(() -> friendshipService.sendFriendRequest(OTHER_USER_ID, 3L))
        .isInstanceOf(AccessDeniedException.class);
    verifyNoInteractions(userRepository, friendshipRepository);
  }

  @Test
  void cancelFriendRequest_deniesCancelForNonRequester() {
    User actualRequester = new User("requester@example.com", "requester", "hash", BigDecimal.ZERO);
    ReflectionTestUtils.setField(actualRequester, "id", OTHER_USER_ID);
    Friendship friendship =
        new Friendship(actualRequester, authenticatedUser, Friendship.Status.PENDING);
    when(friendshipRepository.findById(FRIENDSHIP_ID)).thenReturn(Optional.of(friendship));

    assertThatThrownBy(() -> friendshipService.cancelFriendRequest(FRIENDSHIP_ID))
        .isInstanceOf(AccessDeniedException.class);
    verify(friendshipRepository, never()).delete(friendship);
  }

  @Test
  void cancelFriendRequest_allowsCancelForOwnRequest() {
    User addressee = new User("addressee@example.com", "addressee", "hash", BigDecimal.ZERO);
    ReflectionTestUtils.setField(addressee, "id", OTHER_USER_ID);
    Friendship friendship = new Friendship(authenticatedUser, addressee, Friendship.Status.PENDING);
    when(friendshipRepository.findById(FRIENDSHIP_ID)).thenReturn(Optional.of(friendship));

    friendshipService.cancelFriendRequest(FRIENDSHIP_ID);

    verify(friendshipRepository).delete(friendship);
  }
}
