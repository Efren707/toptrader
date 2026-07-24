package com.toptrader.backend.trading;

import com.toptrader.backend.market.Quote;
import com.toptrader.backend.market.QuoteService;
import com.toptrader.backend.user.User;
import com.toptrader.backend.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class TradeService {
    private final HoldingRepository holdingRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final QuoteService quoteService;

    public TradeService(QuoteService quoteService, HoldingRepository holdingRepository, TransactionRepository transactionRepository,  UserRepository userRepository) {
        this.quoteService = quoteService;
        this.holdingRepository = holdingRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TradeResult buyStock(long userId, TradeRequest tradeRequest) {
        String ticker = tradeRequest.ticker();
        Integer quantity = tradeRequest.quantity();

        if (quantity == null || quantity < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be greater than zero");
        }

        Quote quote = this.quoteService.getQuote(ticker);

        User user = this.userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        BigDecimal total = quote.price().multiply(BigDecimal.valueOf(quantity));

        if (total.compareTo(user.getCashBalance()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cash balance not enough");
        }

        user.setCashBalance(user.getCashBalance().subtract(total));
        this.userRepository.save(user);

        Holding holding = this.holdingRepository.findByUserAndTicker(user, quote.ticker()).orElse(null);
        if (holding == null) {
            holding = new Holding(user, quote.ticker(), quantity, quote.price(), null);
        } else {
            BigDecimal existingCost = holding.getAverageCostBasis().multiply(BigDecimal.valueOf(holding.getQuantity()));
            BigDecimal newCost = quote.price().multiply(BigDecimal.valueOf(quantity));
            int newQuantity = holding.getQuantity() + quantity;
            BigDecimal newAverageCostBasis = existingCost.add(newCost)
                    .divide(BigDecimal.valueOf(newQuantity), 2, RoundingMode.HALF_UP);
            holding.setQuantity(newQuantity);
            holding.setAverageCostBasis(newAverageCostBasis);
        }
        this.holdingRepository.save(holding);

        BigDecimal currentPrice = quote.price();
        BigDecimal marketValue = currentPrice.multiply(BigDecimal.valueOf(holding.getQuantity()));
        BigDecimal costBasisTotal = holding.getAverageCostBasis().multiply(BigDecimal.valueOf(holding.getQuantity()));
        BigDecimal unrealizedGainLoss = marketValue.subtract(costBasisTotal);

        HoldingResponse holdingResponse = new HoldingResponse(holding.getTicker(), holding.getQuantity(),
                holding.getAverageCostBasis(), currentPrice, marketValue, unrealizedGainLoss);

        Transaction transaction = new Transaction(user, quote.ticker(), Transaction.Side.BUY, quantity, quote.price(), total);
        this.transactionRepository.save(transaction);

        TransactionResponse transactionResponse =
                new TransactionResponse(transaction.getId(), transaction.getTicker(), transaction.getSide(),
                        transaction.getQuantity(), transaction.getPricePerShare(), transaction.getTotalAmount(),
                        transaction.getExecutedAt());

        return new TradeResult(transactionResponse, user.getCashBalance(), holdingResponse);
    }

}
