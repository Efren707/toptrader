package com.toptrader.backend.trading;

import com.toptrader.backend.market.Quote;
import com.toptrader.backend.market.QuoteService;
import com.toptrader.backend.user.User;
import com.toptrader.backend.user.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TradeService {
  private static final Logger log = LoggerFactory.getLogger(TradeService.class);

  private final HoldingRepository holdingRepository;
  private final TransactionRepository transactionRepository;
  private final UserRepository userRepository;
  private final QuoteService quoteService;

  public TradeService(
      QuoteService quoteService,
      HoldingRepository holdingRepository,
      TransactionRepository transactionRepository,
      UserRepository userRepository) {
    this.quoteService = quoteService;
    this.holdingRepository = holdingRepository;
    this.transactionRepository = transactionRepository;
    this.userRepository = userRepository;
  }

  @PreAuthorize("#userId == authentication.principal.user.id")
  @Transactional
  public TradeResult buyStock(long userId, TradeRequest tradeRequest) {
    String ticker = tradeRequest.ticker();
    Integer quantity = tradeRequest.quantity();

    if (quantity == null || quantity < 1) {
      log.atWarn()
          .addKeyValue("userId", userId)
          .addKeyValue("ticker", ticker)
          .addKeyValue("reason", "Invalid quantity amount")
          .log("Buy stock failed");
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Quantity must be greater than zero");
    }

    Quote quote = this.quoteService.getQuote(ticker);

    User user =
        this.userRepository
            .findByIdForUpdate(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    BigDecimal total = quote.price().multiply(BigDecimal.valueOf(quantity));

    if (total.compareTo(user.getCashBalance()) > 0) {
      log.atWarn()
          .addKeyValue("userId", userId)
          .addKeyValue("ticker", ticker)
          .addKeyValue("reason", "Insufficient funds")
          .log("Buy stock failed");
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cash balance not enough");
    }

    user.setCashBalance(user.getCashBalance().subtract(total));
    this.userRepository.save(user);

    Holding holding = this.holdingRepository.findByUserAndTicker(user, quote.ticker()).orElse(null);
    if (holding == null) {
      holding = new Holding(user, quote.ticker(), quantity, quote.price(), null);
    } else {
      BigDecimal existingCost =
          holding.getAverageCostBasis().multiply(BigDecimal.valueOf(holding.getQuantity()));
      BigDecimal newCost = quote.price().multiply(BigDecimal.valueOf(quantity));
      int newQuantity = holding.getQuantity() + quantity;
      BigDecimal newAverageCostBasis =
          existingCost
              .add(newCost)
              .divide(BigDecimal.valueOf(newQuantity), 2, RoundingMode.HALF_UP);
      holding.setQuantity(newQuantity);
      holding.setAverageCostBasis(newAverageCostBasis);
    }
    this.holdingRepository.save(holding);

    HoldingResponse holdingResponse = toHoldingResponse(holding, quote);

    Transaction transaction =
        new Transaction(user, quote.ticker(), Transaction.Side.BUY, quantity, quote.price(), total);
    this.transactionRepository.save(transaction);
    TransactionResponse transactionResponse = toTransactionResponse(transaction);

    log.atInfo()
        .addKeyValue("userId", userId)
        .addKeyValue("ticker", quote.ticker())
        .addKeyValue("tradeId", transaction.getId())
        .log("Buy stock succeeded");

    return new TradeResult(transactionResponse, user.getCashBalance(), holdingResponse);
  }

  @PreAuthorize("#userId == authentication.principal.user.id")
  @Transactional
  public TradeResult sellStock(long userId, TradeRequest tradeRequest) {
    String ticker = tradeRequest.ticker();
    Integer quantity = tradeRequest.quantity();

    if (quantity == null || quantity < 1) {
      log.atWarn()
          .addKeyValue("userId", userId)
          .addKeyValue("ticker", ticker)
          .addKeyValue("reason", "Invalid quantity amount")
          .log("Sell stock failed");
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Quantity must be greater than zero");
    }

    Quote quote = this.quoteService.getQuote(ticker);

    User user =
        this.userRepository
            .findByIdForUpdate(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    Holding holding = this.holdingRepository.findByUserAndTicker(user, quote.ticker()).orElse(null);

    if (holding == null) {
      log.atWarn()
          .addKeyValue("userId", userId)
          .addKeyValue("ticker", ticker)
          .addKeyValue("reason", "Holding not found")
          .log("Sell stock failed");
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Holding not found");
    }

    if (holding.getQuantity() < quantity) {
      log.atWarn()
          .addKeyValue("userId", userId)
          .addKeyValue("ticker", ticker)
          .addKeyValue("reason", "Insufficient holding quantity")
          .log("Sell stock failed");
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Quantity must be less than current holding quantity");
    }

    int newQuantity = holding.getQuantity() - quantity;
    holding.setQuantity(newQuantity);

    if (newQuantity == 0) {
      this.holdingRepository.delete(holding);
    } else {
      this.holdingRepository.save(holding);
    }

    BigDecimal total = quote.price().multiply(BigDecimal.valueOf(quantity));

    user.setCashBalance(user.getCashBalance().add(total));
    this.userRepository.save(user);

    HoldingResponse holdingResponse = toHoldingResponse(holding, quote);

    Transaction transaction =
        new Transaction(
            user, quote.ticker(), Transaction.Side.SELL, quantity, quote.price(), total);
    this.transactionRepository.save(transaction);
    TransactionResponse transactionResponse = toTransactionResponse(transaction);

    log.atInfo()
        .addKeyValue("userId", userId)
        .addKeyValue("ticker", quote.ticker())
        .addKeyValue("tradeId", transaction.getId())
        .log("Sell stock succeeded");

    return new TradeResult(transactionResponse, user.getCashBalance(), holdingResponse);
  }

  @PreAuthorize("#userId == authentication.principal.user.id")
  public Optional<HoldingResponse> getHolding(Long userId, String ticker) {
    Quote quote = this.quoteService.getQuote(ticker);

    User user =
        this.userRepository
            .findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    Holding holding = this.holdingRepository.findByUserAndTicker(user, quote.ticker()).orElse(null);

    if (holding == null) {
      return Optional.empty();
    }

    HoldingResponse holdingResponse = toHoldingResponse(holding, quote);

    return Optional.of(holdingResponse);
  }

  @PreAuthorize("#userId == authentication.principal.user.id")
  public List<HoldingResponse> getHoldings(Long userId) {
    User user =
        this.userRepository
            .findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    List<Holding> holdings = this.holdingRepository.findByUser(user);
    List<HoldingResponse> holdingResponses = new ArrayList<>();

    for (Holding holding : holdings) {
      Quote quote = this.quoteService.getQuote(holding.getTicker());
      holdingResponses.add(toHoldingResponse(holding, quote));
    }

    return holdingResponses;
  }

  private HoldingResponse toHoldingResponse(Holding holding, Quote quote) {
    BigDecimal marketValue = quote.price().multiply(BigDecimal.valueOf(holding.getQuantity()));
    BigDecimal costBasisTotal =
        holding.getAverageCostBasis().multiply(BigDecimal.valueOf(holding.getQuantity()));
    BigDecimal unrealizedGainLoss = marketValue.subtract(costBasisTotal);

    return new HoldingResponse(
        holding.getTicker(),
        holding.getQuantity(),
        holding.getAverageCostBasis(),
        quote.price(),
        quote.percentChange(),
        marketValue,
        unrealizedGainLoss);
  }

  @PreAuthorize("#userId == authentication.principal.user.id")
  public List<TransactionResponse> getTransactions(Long userId) {
    User user =
        this.userRepository
            .findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    List<Transaction> transactions =
        this.transactionRepository.findByUserOrderByExecutedAtDesc(user);
    List<TransactionResponse> transactionResponses = new ArrayList<>();

    for (Transaction transaction : transactions) {
      transactionResponses.add(toTransactionResponse(transaction));
    }

    return transactionResponses;
  }

  private TransactionResponse toTransactionResponse(Transaction transaction) {
    return new TransactionResponse(
        transaction.getId(),
        transaction.getTicker(),
        transaction.getSide(),
        transaction.getQuantity(),
        transaction.getPricePerShare(),
        transaction.getTotalAmount(),
        transaction.getExecutedAt());
  }
}
