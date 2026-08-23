package com.toptrader.backend.friendship;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.toptrader.backend.user.User;
import com.toptrader.backend.user.UserPrincipal;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** Covers ADR 0049's {@code isRequester} check backing {@code cancelFriendRequest}'s guard. */
@ExtendWith(MockitoExtension.class)
class FriendshipAuthorizationTest {

  private static final Long FRIENDSHIP_ID = 1L;

  @Mock private FriendshipRepository friendshipRepository;

  private FriendshipAuthorization authorization;

  @BeforeEach
  void setUp() {
    authorization = new FriendshipAuthorization(friendshipRepository);
  }

  private User userWithId(long id) {
    User user = new User("user" + id + "@example.com", "user" + id, "hash", BigDecimal.ZERO);
    ReflectionTestUtils.setField(user, "id", id);
    return user;
  }

  @Test
  void isRequester_returnsTrue_whenFriendshipDoesNotExist() {
    when(friendshipRepository.findById(FRIENDSHIP_ID)).thenReturn(Optional.empty());

    boolean result = authorization.isRequester(FRIENDSHIP_ID, new UserPrincipal(userWithId(1L)));

    assertThat(result).isTrue();
  }

  @Test
  void isRequester_returnsTrue_whenPrincipalIsTheRequester() {
    User requester = userWithId(1L);
    User addressee = userWithId(2L);
    Friendship friendship = new Friendship(requester, addressee, Friendship.Status.PENDING);
    when(friendshipRepository.findById(FRIENDSHIP_ID)).thenReturn(Optional.of(friendship));

    boolean result = authorization.isRequester(FRIENDSHIP_ID, new UserPrincipal(requester));

    assertThat(result).isTrue();
  }

  @Test
  void isRequester_returnsFalse_whenPrincipalIsNotTheRequester() {
    User requester = userWithId(1L);
    User addressee = userWithId(2L);
    Friendship friendship = new Friendship(requester, addressee, Friendship.Status.PENDING);
    when(friendshipRepository.findById(FRIENDSHIP_ID)).thenReturn(Optional.of(friendship));

    boolean result = authorization.isRequester(FRIENDSHIP_ID, new UserPrincipal(addressee));

    assertThat(result).isFalse();
  }

  @Test
  void isAddressee_returnsTrue_whenFriendshipDoesNotExist() {
    when(friendshipRepository.findById(FRIENDSHIP_ID)).thenReturn(Optional.empty());

    boolean result = authorization.isAddressee(FRIENDSHIP_ID, new UserPrincipal(userWithId(1L)));

    assertThat(result).isTrue();
  }

  @Test
  void isAddressee_returnsTrue_whenPrincipalIsTheAddressee() {
    User requester = userWithId(1L);
    User addressee = userWithId(2L);
    Friendship friendship = new Friendship(requester, addressee, Friendship.Status.PENDING);
    when(friendshipRepository.findById(FRIENDSHIP_ID)).thenReturn(Optional.of(friendship));

    boolean result = authorization.isAddressee(FRIENDSHIP_ID, new UserPrincipal(addressee));

    assertThat(result).isTrue();
  }

  @Test
  void isAddressee_returnsFalse_whenPrincipalIsNotTheAddressee() {
    User requester = userWithId(1L);
    User addressee = userWithId(2L);
    Friendship friendship = new Friendship(requester, addressee, Friendship.Status.PENDING);
    when(friendshipRepository.findById(FRIENDSHIP_ID)).thenReturn(Optional.of(friendship));

    boolean result = authorization.isAddressee(FRIENDSHIP_ID, new UserPrincipal(requester));

    assertThat(result).isFalse();
  }
}
