import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { Quote, QuoteService } from '../../core/services/quote.service';
import { ApiError } from '../../core/interceptors/error.interceptor';
import { Card } from '../../shared/ui/card/card';
import { TradeForm } from '../../shared/trade-form/trade-form';

@Component({
  selector: 'app-stock-details',
  imports: [CurrencyPipe, DatePipe, RouterLink, Card, TradeForm],
  templateUrl: './stock-details.html',
  styleUrl: './stock-details.css',
})
export class StockDetails implements OnInit {
  

  private readonly route = inject(ActivatedRoute);
  private readonly quoteService = inject(QuoteService);

  protected readonly quote = signal<Quote | null>(null);
  protected readonly ticker = signal<string | null>(null);
  protected readonly loading = signal(true);
  protected readonly notFound = signal(false);

  ngOnInit(): void {
    this.ticker.set(this.route.snapshot.paramMap.get('ticker'));
    this.fetchQuoteData();
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

}
