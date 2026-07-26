package com.toptrader.backend.trading;

import com.toptrader.backend.user.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/trades")
public class TradeController {
  private final TradeService tradeService;

  public TradeController(TradeService tradeService) {
    this.tradeService = tradeService;
  }

  @PostMapping("/buy")
  public ResponseEntity<TradeResult> buy(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestBody TradeRequest tradeRequest) {
    TradeResult tradeResult = tradeService.buyStock(principal.getUser().getId(), tradeRequest);
    return new ResponseEntity<>(tradeResult, HttpStatus.CREATED);
  }

  @PostMapping("/sell")
  public ResponseEntity<TradeResult> sell(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestBody TradeRequest tradeRequest) {
    TradeResult tradeResult = tradeService.sellStock(principal.getUser().getId(), tradeRequest);
    return new ResponseEntity<>(tradeResult, HttpStatus.CREATED);
  }

  @GetMapping("/holdings/{ticker}")
  public ResponseEntity<HoldingResponse> holdings(
      @AuthenticationPrincipal UserPrincipal principal, @PathVariable String ticker) {
    return tradeService
        .getHolding(principal.getUser().getId(), ticker)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }
}
