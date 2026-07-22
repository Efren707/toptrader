package com.toptrader.backend.market;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/quotes")
public class QuoteController {
  private final QuoteService quoteService;

  public QuoteController(QuoteService quoteService) {
    this.quoteService = quoteService;
  }

  @GetMapping("/{ticker}")
  public ResponseEntity<Quote> getQuote(@PathVariable String ticker) {
    Quote quote = quoteService.getQuote(ticker);
    return new ResponseEntity<>(quote, HttpStatus.OK);
  }
}
