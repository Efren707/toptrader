package com.toptrader.backend.trading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.toptrader.backend.market.QuoteService;
import com.toptrader.backend.user.User;
import com.toptrader.backend.user.UserPrincipal;
import com.toptrader.backend.user.UserRepository;
import java.math.BigDecimal;
import java.util.List;
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
 * Verifies ADR 0035's {@code @PreAuthorize} guard on {@link TradeService}. No controller can
 * currently trigger the deny path (every controller already derives userId from the principal), so
 * this calls the real Spring-proxied service bean directly with a mismatched userId to prove the
 * guard itself is wired and evaluated correctly.
 */
@SpringBootTest
class TradeServiceAuthorizationTest {

  private static final long AUTHENTICATED_USER_ID = 1L;
  private static final long OTHER_USER_ID = 2L;

  @Autowired private TradeService tradeService;

  @MockitoBean private QuoteService quoteService;
  @MockitoBean private HoldingRepository holdingRepository;
  @MockitoBean private TransactionRepository transactionRepository;
  @MockitoBean private UserRepository userRepository;

  @BeforeEach
  void authenticateAsUser() {
    User user = new User("owner@example.com", "owner", "hash", BigDecimal.ZERO);
    ReflectionTestUtils.setField(user, "id", AUTHENTICATED_USER_ID);
    UserPrincipal principal = new UserPrincipal(user);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
  }

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void buyStock_deniesRequestForAnotherUsersId() {
    assertThatThrownBy(() -> tradeService.buyStock(OTHER_USER_ID, new TradeRequest("AAPL", 1)))
        .isInstanceOf(AccessDeniedException.class);
    verifyNoInteractions(userRepository, quoteService, holdingRepository, transactionRepository);
  }

  @Test
  void sellStock_deniesRequestForAnotherUsersId() {
    assertThatThrownBy(() -> tradeService.sellStock(OTHER_USER_ID, new TradeRequest("AAPL", 1)))
        .isInstanceOf(AccessDeniedException.class);
    verifyNoInteractions(userRepository, quoteService, holdingRepository, transactionRepository);
  }

  @Test
  void getHolding_deniesRequestForAnotherUsersId() {
    assertThatThrownBy(() -> tradeService.getHolding(OTHER_USER_ID, "AAPL"))
        .isInstanceOf(AccessDeniedException.class);
    verifyNoInteractions(userRepository, quoteService, holdingRepository);
  }

  @Test
  void getHoldings_deniesRequestForAnotherUsersId() {
    assertThatThrownBy(() -> tradeService.getHoldings(OTHER_USER_ID))
        .isInstanceOf(AccessDeniedException.class);
    verifyNoInteractions(userRepository, holdingRepository, quoteService);
  }

  @Test
  void getTransactions_deniesRequestForAnotherUsersId() {
    assertThatThrownBy(() -> tradeService.getTransactions(OTHER_USER_ID))
        .isInstanceOf(AccessDeniedException.class);
    verifyNoInteractions(userRepository, transactionRepository);
  }

  @Test
  void getHoldings_allowsRequestForOwnUserId() {
    User user = new User("owner@example.com", "owner", "hash", BigDecimal.ZERO);
    ReflectionTestUtils.setField(user, "id", AUTHENTICATED_USER_ID);
    when(userRepository.findById(AUTHENTICATED_USER_ID)).thenReturn(Optional.of(user));
    when(holdingRepository.findByUser(user)).thenReturn(List.of());

    List<HoldingResponse> result = tradeService.getHoldings(AUTHENTICATED_USER_ID);

    assertThat(result).isEmpty();
  }
}
