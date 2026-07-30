import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { Quote, QuoteService } from '../../core/services/quote.service';
import { ApiError } from '../../core/interceptors/error.interceptor';
import { Card } from '../../shared/ui/card/card';
import { TradeForm } from '../../shared/trade-form/trade-form';
import { Holding, TradeService } from '../../core/services/trade.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-stock-details',
  imports: [CurrencyPipe, DatePipe, DecimalPipe, Card, TradeForm],
  templateUrl: './stock-details.html',
  styleUrl: './stock-details.css',
})
export class StockDetails implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly quoteService = inject(QuoteService);
  private readonly tradeService = inject(TradeService);
  private readonly authService = inject(AuthService);

  protected readonly quote = signal<Quote | null>(null);
  protected readonly holding = signal<Holding | null>(null);
  protected readonly holdings = signal<Holding[]>([]);
  protected readonly ticker = signal<string | null>(null);
  protected readonly loading = signal(true);
  protected readonly notFound = signal(false);
  protected readonly cashBalance = (this.authService.currentUser()?.cashBalance ?? 0);

  protected readonly todaysReturn = computed(() => {
    const holding = this.holding();
    if (!holding) {
      return null;
    }
    const previousClose = holding.currentPrice / (1 + holding.percentChange / 100);
    return (holding.currentPrice - previousClose) * holding.quantity;
  });

  protected readonly totalReturnPercent = computed(() => {
    const holding = this.holding();
    if (!holding) {
      return null;
    }
    return (holding.unrealizedGainLoss / (holding.averageCostBasis * holding.quantity)) * 100;
  });

  protected readonly portfolioBalance = computed(() =>
    (this.cashBalance ?? 0) + this.holdings().reduce((sum, h) => sum + h.marketValue, 0)
  );

  protected readonly portfolioDiversity = computed(() => {
    const holding = this.holding();
    const total = this.portfolioBalance();
    if (!holding || total === 0) {
      return null;
    }
    return (holding.marketValue / total) * 100;
  });

  ngOnInit(): void {
    this.fetchHoldingsData();
    this.route.paramMap.subscribe((params) => {
      this.ticker.set(params.get('ticker'));
      this.loading.set(true);
      this.notFound.set(false);
      this.quote.set(null);
      this.holding.set(null);
      this.fetchQuoteData();
      this.fetchHoldingData();
    });
  }

  fetchQuoteData() {
    this.quoteService.getQuote(this.ticker() ?? '').subscribe({
      next: (quote) => {
        this.loading.set(false);
        this.quote.set(quote);
      },
      error: (error: ApiError) => {
        this.loading.set(false);
        if (error.status === 404) {
          this.notFound.set(true);
        }
      },
    });
  }

  fetchHoldingData() {
    this.tradeService.getHolding(this.ticker() ?? '').subscribe({
      next: (holding) => {
        this.holding.set(holding);
      },
      error: (error: ApiError) => {
        if (error.status !== 404) {
          throw error;
        }
      },
    });
  }

  fetchHoldingsData() {
    this.tradeService.getHoldings().subscribe({
      next: (holdings) => {
        this.holdings.set(holdings);
      },
    });
  }

}
