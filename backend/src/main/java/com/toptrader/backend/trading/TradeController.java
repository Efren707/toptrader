package com.toptrader.backend.trading;

import com.toptrader.backend.user.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
