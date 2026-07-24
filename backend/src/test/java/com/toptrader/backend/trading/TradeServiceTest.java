package com.toptrader.backend.trading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.toptrader.backend.market.Quote;
import com.toptrader.backend.market.QuoteService;
import com.toptrader.backend.user.User;
import com.toptrader.backend.user.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Covers the US-5 acceptance criteria (docs/requirements/acceptance-criteria.md). */
@ExtendWith(MockitoExtension.class)
class TradeServiceTest {

  private static final long USER_ID = 1L;

  @Mock private QuoteService quoteService;
  @Mock private HoldingRepository holdingRepository;
  @Mock private TransactionRepository transactionRepository;
  @Mock private UserRepository userRepository;

  private TradeService tradeService;
  private User user;

  @BeforeEach
  void setUp() {
    tradeService =
        new TradeService(quoteService, holdingRepository, transactionRepository, userRepository);
    user = new User("trader@example.com", "trader", "hash", new BigDecimal("1000.00"));
  }

  @Test
  void buyStock_createsNewHolding_whenNoneExistsForTicker() {
    when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
    when(quoteService.getQuote("AAPL"))
        .thenReturn(new Quote("AAPL", "Apple Inc.", new BigDecimal("100.00"), Instant.now()));
    when(holdingRepository.findByUserAndTicker(user, "AAPL")).thenReturn(Optional.empty());

    TradeResult result = tradeService.buyStock(USER_ID, new TradeRequest("AAPL", 5));

    assertThat(result.cashBalance()).isEqualByComparingTo("500.00");
    assertThat(result.holding().quantity()).isEqualTo(5);
    assertThat(result.holding().averageCostBasis()).isEqualByComparingTo("100.00");
    assertThat(result.transaction().side()).isEqualTo(Transaction.Side.BUY);
    assertThat(result.transaction().quantity()).isEqualTo(5);
    assertThat(result.transaction().pricePerShare()).isEqualByComparingTo("100.00");
    assertThat(result.transaction().totalAmount()).isEqualByComparingTo("500.00");

    verify(userRepository).save(user);
    verify(holdingRepository).save(any(Holding.class));
    verify(transactionRepository).save(any(Transaction.class));
  }

  @Test
  void buyStock_increasesExistingHolding_withWeightedAverageCostBasis() {
    Holding existing = new Holding(user, "AAPL", 10, new BigDecimal("50.00"), null);
    when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
    when(quoteService.getQuote("AAPL"))
        .thenReturn(new Quote("AAPL", "Apple Inc.", new BigDecimal("100.00"), Instant.now()));
    when(holdingRepository.findByUserAndTicker(user, "AAPL")).thenReturn(Optional.of(existing));

    TradeResult result = tradeService.buyStock(USER_ID, new TradeRequest("AAPL", 10));

    assertThat(existing.getQuantity()).isEqualTo(20);
    // (10 * 50.00 + 10 * 100.00) / 20 = 75.00
    assertThat(existing.getAverageCostBasis()).isEqualByComparingTo("75.00");
    assertThat(result.holding().quantity()).isEqualTo(20);
    assertThat(result.holding().averageCostBasis()).isEqualByComparingTo("75.00");
  }

  @Test
  void buyStock_rejectsInsufficientCash_withNoStateChange() {
    user = new User("trader@example.com", "trader", "hash", new BigDecimal("100.00"));
    when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
    when(quoteService.getQuote("AAPL"))
        .thenReturn(new Quote("AAPL", "Apple Inc.", new BigDecimal("100.00"), Instant.now()));

    assertThatThrownBy(() -> tradeService.buyStock(USER_ID, new TradeRequest("AAPL", 5)))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Cash balance not enough");

    assertThat(user.getCashBalance()).isEqualByComparingTo("100.00");
    verifyNoInteractions(holdingRepository, transactionRepository);
    verify(userRepository, never()).save(any());
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1, -100})
  void buyStock_rejectsNonPositiveQuantity_withoutFetchingQuote(int badQuantity) {
    assertThatThrownBy(
            () -> tradeService.buyStock(USER_ID, new TradeRequest("AAPL", badQuantity)))
        .isInstanceOf(ResponseStatusException.class);

    verifyNoInteractions(quoteService, userRepository, holdingRepository, transactionRepository);
  }

  @Test
  void buyStock_rejectsNullQuantity() {
    assertThatThrownBy(() -> tradeService.buyStock(USER_ID, new TradeRequest("AAPL", null)))
        .isInstanceOf(ResponseStatusException.class);

    verifyNoInteractions(quoteService, userRepository, holdingRepository, transactionRepository);
  }

  @Test
  void buyStock_propagatesUnknownTicker_withoutTouchingUserOrPersistence() {
    when(quoteService.getQuote("ZZZZ"))
        .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown ticker: ZZZZ"));

    assertThatThrownBy(() -> tradeService.buyStock(USER_ID, new TradeRequest("ZZZZ", 5)))
        .isInstanceOf(ResponseStatusException.class);

    verifyNoInteractions(userRepository, holdingRepository, transactionRepository);
  }

  @Test
  void buyStock_throwsNotFound_whenUserDoesNotExist() {
    when(quoteService.getQuote("AAPL"))
        .thenReturn(new Quote("AAPL", "Apple Inc.", new BigDecimal("100.00"), Instant.now()));
    when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> tradeService.buyStock(USER_ID, new TradeRequest("AAPL", 5)))
        .isInstanceOf(ResponseStatusException.class);

    verifyNoInteractions(holdingRepository, transactionRepository);
  }

  @Test
  void buyStock_usesTheFetchedQuotePriceExactlyOnce_forExecution() {
    when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
    when(quoteService.getQuote("AAPL"))
        .thenReturn(new Quote("AAPL", "Apple Inc.", new BigDecimal("210.50"), Instant.now()));
    when(holdingRepository.findByUserAndTicker(user, "AAPL")).thenReturn(Optional.empty());

    TradeResult result = tradeService.buyStock(USER_ID, new TradeRequest("AAPL", 3));

    assertThat(result.transaction().pricePerShare()).isEqualByComparingTo("210.50");
    verify(quoteService, times(1)).getQuote("AAPL");
  }
}
