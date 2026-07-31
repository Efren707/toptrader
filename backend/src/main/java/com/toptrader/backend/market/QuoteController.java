package com.toptrader.backend.market;

import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/quotes")
@Validated
public class QuoteController {
  private final QuoteService quoteService;

  public QuoteController(QuoteService quoteService) {
    this.quoteService = quoteService;
  }

  @GetMapping("/{ticker}")
  public ResponseEntity<Quote> getQuote(
      @PathVariable @Pattern(regexp = "^[A-Za-z]{1,5}$", message = "Invalid stock ticker")
          String ticker) {
    Quote quote = quoteService.getQuote(ticker);
    return new ResponseEntity<>(quote, HttpStatus.OK);
  }
}
