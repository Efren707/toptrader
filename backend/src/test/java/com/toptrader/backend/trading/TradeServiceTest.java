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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
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
        .thenReturn(
            new Quote(
                "AAPL",
                "Apple Inc.",
                new BigDecimal("100.00"),
                new BigDecimal("5.19"),
                Instant.now()));
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
        .thenReturn(
            new Quote(
                "AAPL",
                "Apple Inc.",
                new BigDecimal("100.00"),
                new BigDecimal("5.19"),
                Instant.now()));
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
        .thenReturn(
            new Quote(
                "AAPL",
                "Apple Inc.",
                new BigDecimal("100.00"),
                new BigDecimal("5.19"),
                Instant.now()));

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
    assertThatThrownBy(() -> tradeService.buyStock(USER_ID, new TradeRequest("AAPL", badQuantity)))
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
        .thenReturn(
            new Quote(
                "AAPL",
                "Apple Inc.",
                new BigDecimal("100.00"),
                new BigDecimal("5.19"),
                Instant.now()));
    when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> tradeService.buyStock(USER_ID, new TradeRequest("AAPL", 5)))
        .isInstanceOf(ResponseStatusException.class);

    verifyNoInteractions(holdingRepository, transactionRepository);
  }

  @Test
  void buyStock_usesTheFetchedQuotePriceExactlyOnce_forExecution() {
    when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
    when(quoteService.getQuote("AAPL"))
        .thenReturn(
            new Quote(
                "AAPL",
                "Apple Inc.",
                new BigDecimal("210.50"),
                new BigDecimal("5.19"),
                Instant.now()));
    when(holdingRepository.findByUserAndTicker(user, "AAPL")).thenReturn(Optional.empty());

    TradeResult result = tradeService.buyStock(USER_ID, new TradeRequest("AAPL", 3));

    assertThat(result.transaction().pricePerShare()).isEqualByComparingTo("210.50");
    verify(quoteService, times(1)).getQuote("AAPL");
  }

  @Test
  void buyStock_rejectsDemoAccount_withNoStateChange() {
    ReflectionTestUtils.setField(user, "isDemo", true);
    when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
    when(quoteService.getQuote("AAPL"))
        .thenReturn(
            new Quote(
                "AAPL",
                "Apple Inc.",
                new BigDecimal("100.00"),
                new BigDecimal("5.19"),
                Instant.now()));

    assertThatThrownBy(() -> tradeService.buyStock(USER_ID, new TradeRequest("AAPL", 5)))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);

    assertThat(user.getCashBalance()).isEqualByComparingTo("1000.00");
    verifyNoInteractions(holdingRepository, transactionRepository);
    verify(userRepository, never()).save(any());
  }

  @Test
  void sellStock_decreasesHolding_withoutRemovingIt_onPartialSell() {
    Holding existing = new Holding(user, "AAPL", 10, new BigDecimal("50.00"), null);
    when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
    when(quoteService.getQuote("AAPL"))
        .thenReturn(
            new Quote(
                "AAPL",
                "Apple Inc.",
                new BigDecimal("100.00"),
                new BigDecimal("5.19"),
                Instant.now()));
    when(holdingRepository.findByUserAndTicker(user, "AAPL")).thenReturn(Optional.of(existing));

    TradeResult result = tradeService.sellStock(USER_ID, new TradeRequest("AAPL", 4));

    assertThat(result.cashBalance()).isEqualByComparingTo("1400.00"); // 1000.00 + 4 * 100.00
    assertThat(result.holding().quantity()).isEqualTo(6);
    assertThat(result.holding().averageCostBasis()).isEqualByComparingTo("50.00");
    assertThat(result.transaction().side()).isEqualTo(Transaction.Side.SELL);
    assertThat(result.transaction().quantity()).isEqualTo(4);
    assertThat(result.transaction().totalAmount()).isEqualByComparingTo("400.00");

    verify(holdingRepository).save(existing);
    verify(holdingRepository, never()).delete(any());
    verify(userRepository).save(user);
    verify(transactionRepository).save(any(Transaction.class));
  }

  @Test
  void sellStock_removesHolding_whenFullyLiquidated() {
    Holding existing = new Holding(user, "AAPL", 10, new BigDecimal("50.00"), null);
    when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
    when(quoteService.getQuote("AAPL"))
        .thenReturn(
            new Quote(
                "AAPL",
                "Apple Inc.",
                new BigDecimal("100.00"),
                new BigDecimal("5.19"),
                Instant.now()));
    when(holdingRepository.findByUserAndTicker(user, "AAPL")).thenReturn(Optional.of(existing));

    TradeResult result = tradeService.sellStock(USER_ID, new TradeRequest("AAPL", 10));

    assertThat(result.holding().quantity()).isEqualTo(0);
    assertThat(result.holding().marketValue()).isEqualByComparingTo("0.00");
    assertThat(result.cashBalance()).isEqualByComparingTo("2000.00"); // 1000.00 + 10 * 100.00

    verify(holdingRepository).delete(existing);
    verify(holdingRepository, never()).save(any());
  }

  @Test
  void sellStock_rejectsDemoAccount_withNoStateChange() {
    ReflectionTestUtils.setField(user, "isDemo", true);
    when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
    when(quoteService.getQuote("AAPL"))
        .thenReturn(
            new Quote(
                "AAPL",
                "Apple Inc.",
                new BigDecimal("100.00"),
                new BigDecimal("5.19"),
                Instant.now()));

    assertThatThrownBy(() -> tradeService.sellStock(USER_ID, new TradeRequest("AAPL", 5)))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);

    assertThat(user.getCashBalance()).isEqualByComparingTo("1000.00");
    verifyNoInteractions(holdingRepository, transactionRepository);
    verify(userRepository, never()).save(any());
  }

  @Test
  void sellStock_rejectsQuantityExceedingHolding_withNoStateChange() {
    Holding existing = new Holding(user, "AAPL", 5, new BigDecimal("50.00"), null);
    when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
    when(quoteService.getQuote("AAPL"))
        .thenReturn(
            new Quote(
                "AAPL",
                "Apple Inc.",
                new BigDecimal("100.00"),
                new BigDecimal("5.19"),
                Instant.now()));
    when(holdingRepository.findByUserAndTicker(user, "AAPL")).thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> tradeService.sellStock(USER_ID, new TradeRequest("AAPL", 6)))
        .isInstanceOf(ResponseStatusException.class);

    assertThat(existing.getQuantity()).isEqualTo(5);
    assertThat(user.getCashBalance()).isEqualByComparingTo("1000.00");
    verify(holdingRepository, never()).save(any());
    verify(holdingRepository, never()).delete(any());
    verifyNoInteractions(transactionRepository);
    verify(userRepository, never()).save(any());
  }

  @Test
  void sellStock_rejectsWhenNoHoldingExists() {
    when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
    when(quoteService.getQuote("AAPL"))
        .thenReturn(
            new Quote(
                "AAPL",
                "Apple Inc.",
                new BigDecimal("100.00"),
                new BigDecimal("5.19"),
                Instant.now()));
    when(holdingRepository.findByUserAndTicker(user, "AAPL")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> tradeService.sellStock(USER_ID, new TradeRequest("AAPL", 1)))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Holding not found");

    verifyNoInteractions(transactionRepository);
    verify(userRepository, never()).save(any());
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1, -100})
  void sellStock_rejectsNonPositiveQuantity_withoutFetchingQuote(int badQuantity) {
    assertThatThrownBy(() -> tradeService.sellStock(USER_ID, new TradeRequest("AAPL", badQuantity)))
        .isInstanceOf(ResponseStatusException.class);

    verifyNoInteractions(quoteService, userRepository, holdingRepository, transactionRepository);
  }

  @Test
  void sellStock_rejectsNullQuantity() {
    assertThatThrownBy(() -> tradeService.sellStock(USER_ID, new TradeRequest("AAPL", null)))
        .isInstanceOf(ResponseStatusException.class);

    verifyNoInteractions(quoteService, userRepository, holdingRepository, transactionRepository);
  }

  @Test
  void sellStock_propagatesUnknownTicker_withoutTouchingUserOrPersistence() {
    when(quoteService.getQuote("ZZZZ"))
        .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown ticker: ZZZZ"));

    assertThatThrownBy(() -> tradeService.sellStock(USER_ID, new TradeRequest("ZZZZ", 1)))
        .isInstanceOf(ResponseStatusException.class);

    verifyNoInteractions(userRepository, holdingRepository, transactionRepository);
  }

  @Test
  void sellStock_throwsNotFound_whenUserDoesNotExist() {
    when(quoteService.getQuote("AAPL"))
        .thenReturn(
            new Quote(
                "AAPL",
                "Apple Inc.",
                new BigDecimal("100.00"),
                new BigDecimal("5.19"),
                Instant.now()));
    when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> tradeService.sellStock(USER_ID, new TradeRequest("AAPL", 1)))
        .isInstanceOf(ResponseStatusException.class);

    verifyNoInteractions(holdingRepository, transactionRepository);
  }

  @Test
  void sellStock_usesTheFetchedQuotePriceExactlyOnce_forExecution() {
    Holding existing = new Holding(user, "AAPL", 10, new BigDecimal("50.00"), null);
    when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
    when(quoteService.getQuote("AAPL"))
        .thenReturn(
            new Quote(
                "AAPL",
                "Apple Inc.",
                new BigDecimal("210.50"),
                new BigDecimal("5.19"),
                Instant.now()));
    when(holdingRepository.findByUserAndTicker(user, "AAPL")).thenReturn(Optional.of(existing));

    TradeResult result = tradeService.sellStock(USER_ID, new TradeRequest("AAPL", 3));

    assertThat(result.transaction().pricePerShare()).isEqualByComparingTo("210.50");
    verify(quoteService, times(1)).getQuote("AAPL");
  }

  @Test
  void getHolding_returnsHolding_whenOneExistsForTicker() {
    Holding existing = new Holding(user, "AAPL", 10, new BigDecimal("50.00"), null);
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(quoteService.getQuote("AAPL"))
        .thenReturn(
            new Quote(
                "AAPL",
                "Apple Inc.",
                new BigDecimal("100.00"),
                new BigDecimal("5.19"),
                Instant.now()));
    when(holdingRepository.findByUserAndTicker(user, "AAPL")).thenReturn(Optional.of(existing));

    Optional<HoldingResponse> result = tradeService.getHolding(USER_ID, "AAPL");

    assertThat(result).isPresent();
    assertThat(result.get().ticker()).isEqualTo("AAPL");
    assertThat(result.get().quantity()).isEqualTo(10);
    assertThat(result.get().averageCostBasis()).isEqualByComparingTo("50.00");
    assertThat(result.get().currentPrice()).isEqualByComparingTo("100.00");
    assertThat(result.get().marketValue()).isEqualByComparingTo("1000.00");
    assertThat(result.get().unrealizedGainLoss()).isEqualByComparingTo("500.00");
  }

  @Test
  void getHolding_returnsEmpty_whenNoHoldingExistsForTicker() {
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(quoteService.getQuote("AAPL"))
        .thenReturn(
            new Quote(
                "AAPL",
                "Apple Inc.",
                new BigDecimal("100.00"),
                new BigDecimal("5.19"),
                Instant.now()));
    when(holdingRepository.findByUserAndTicker(user, "AAPL")).thenReturn(Optional.empty());

    Optional<HoldingResponse> result = tradeService.getHolding(USER_ID, "AAPL");

    assertThat(result).isEmpty();
  }

  @Test
  void getHolding_propagatesUnknownTicker() {
    when(quoteService.getQuote("ZZZZ"))
        .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown ticker: ZZZZ"));

    assertThatThrownBy(() -> tradeService.getHolding(USER_ID, "ZZZZ"))
        .isInstanceOf(ResponseStatusException.class);

    verifyNoInteractions(userRepository, holdingRepository);
  }

  @Test
  void getHolding_throwsNotFound_whenUserDoesNotExist() {
    when(quoteService.getQuote("AAPL"))
        .thenReturn(
            new Quote(
                "AAPL",
                "Apple Inc.",
                new BigDecimal("100.00"),
                new BigDecimal("5.19"),
                Instant.now()));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> tradeService.getHolding(USER_ID, "AAPL"))
        .isInstanceOf(ResponseStatusException.class);

    verifyNoInteractions(holdingRepository);
  }

  @Test
  void getHoldings_returnsAllHoldingsForUser_withComputedValues() {
    Holding aapl = new Holding(user, "AAPL", 10, new BigDecimal("50.00"), null);
    Holding msft = new Holding(user, "MSFT", 5, new BigDecimal("200.00"), null);
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(holdingRepository.findByUser(user)).thenReturn(List.of(aapl, msft));
    when(quoteService.getQuote("AAPL"))
        .thenReturn(
            new Quote(
                "AAPL",
                "Apple Inc.",
                new BigDecimal("100.00"),
                new BigDecimal("5.19"),
                Instant.now()));
    when(quoteService.getQuote("MSFT"))
        .thenReturn(
            new Quote(
                "MSFT",
                "Microsoft Corp.",
                new BigDecimal("180.00"),
                new BigDecimal("5.19"),
                Instant.now()));

    List<HoldingResponse> result = tradeService.getHoldings(USER_ID);

    assertThat(result).hasSize(2);

    HoldingResponse aaplResponse =
        result.stream().filter(h -> h.ticker().equals("AAPL")).findFirst().orElseThrow();
    assertThat(aaplResponse.quantity()).isEqualTo(10);
    assertThat(aaplResponse.currentPrice()).isEqualByComparingTo("100.00");
    assertThat(aaplResponse.marketValue()).isEqualByComparingTo("1000.00");
    assertThat(aaplResponse.unrealizedGainLoss()).isEqualByComparingTo("500.00");

    HoldingResponse msftResponse =
        result.stream().filter(h -> h.ticker().equals("MSFT")).findFirst().orElseThrow();
    assertThat(msftResponse.quantity()).isEqualTo(5);
    assertThat(msftResponse.currentPrice()).isEqualByComparingTo("180.00");
    assertThat(msftResponse.marketValue()).isEqualByComparingTo("900.00");
    // cost basis 5 * 200.00 = 1000.00, market value 900.00 -> -100.00
    assertThat(msftResponse.unrealizedGainLoss()).isEqualByComparingTo("-100.00");
  }

  @Test
  void getHoldings_returnsEmptyList_whenUserHasNoHoldings() {
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(holdingRepository.findByUser(user)).thenReturn(List.of());

    List<HoldingResponse> result = tradeService.getHoldings(USER_ID);

    assertThat(result).isEmpty();
    verifyNoInteractions(quoteService);
  }

  @Test
  void getHoldings_throwsNotFound_whenUserDoesNotExist() {
    when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> tradeService.getHoldings(USER_ID))
        .isInstanceOf(ResponseStatusException.class);

    verifyNoInteractions(holdingRepository, quoteService);
  }

  @Test
  void getHoldings_propagatesQuoteServiceFailure_forAnyHolding() {
    Holding aapl = new Holding(user, "AAPL", 10, new BigDecimal("50.00"), null);
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(holdingRepository.findByUser(user)).thenReturn(List.of(aapl));
    when(quoteService.getQuote("AAPL"))
        .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown ticker: AAPL"));

    assertThatThrownBy(() -> tradeService.getHoldings(USER_ID))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void getTransactions_returnsAllTransactionsForUser_mostRecentFirst() {
    Transaction buy =
        new Transaction(
            user,
            "AAPL",
            Transaction.Side.BUY,
            10,
            new BigDecimal("100.00"),
            new BigDecimal("1000.00"));
    Transaction sell =
        new Transaction(
            user,
            "AAPL",
            Transaction.Side.SELL,
            4,
            new BigDecimal("120.00"),
            new BigDecimal("480.00"));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(transactionRepository.findByUserOrderByExecutedAtDesc(user))
        .thenReturn(List.of(sell, buy));

    List<TransactionResponse> result = tradeService.getTransactions(USER_ID);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).side()).isEqualTo(Transaction.Side.SELL);
    assertThat(result.get(0).quantity()).isEqualTo(4);
    assertThat(result.get(0).pricePerShare()).isEqualByComparingTo("120.00");
    assertThat(result.get(0).totalAmount()).isEqualByComparingTo("480.00");
    assertThat(result.get(1).side()).isEqualTo(Transaction.Side.BUY);
    assertThat(result.get(1).quantity()).isEqualTo(10);
  }

  @Test
  void getTransactions_returnsEmptyList_whenUserHasNoTransactions() {
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(transactionRepository.findByUserOrderByExecutedAtDesc(user)).thenReturn(List.of());

    List<TransactionResponse> result = tradeService.getTransactions(USER_ID);

    assertThat(result).isEmpty();
  }

  @Test
  void getTransactions_throwsNotFound_whenUserDoesNotExist() {
    when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> tradeService.getTransactions(USER_ID))
        .isInstanceOf(ResponseStatusException.class);

    verifyNoInteractions(transactionRepository);
  }
}
