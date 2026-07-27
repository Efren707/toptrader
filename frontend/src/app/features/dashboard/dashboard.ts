import { Component, computed, ElementRef, HostListener, inject, OnInit, signal, viewChild } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { AuthService } from '../../core/services/auth.service';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Quote, QuoteService } from '../../core/services/quote.service';
import { ApiError } from '../../core/interceptors/error.interceptor';
import { Router } from '@angular/router';
import { Holding, TradeService } from '../../core/services/trade.service';
import { Card } from '../../shared/ui/card/card';

type SearchField = 'ticker';

@Component({
  selector: 'app-dashboard',
  imports: [ReactiveFormsModule, CurrencyPipe, Card],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly quoteService = inject(QuoteService);
  private readonly authService = inject(AuthService);
  private readonly tradeService = inject(TradeService);
  protected readonly username = this.authService.currentUser()?.username;
  protected readonly cashBalance = this.authService.currentUser()?.cashBalance;
  private readonly router = inject(Router);
  protected readonly searchForm = viewChild<ElementRef<HTMLElement>>('searchForm');

  protected readonly form = this.fb.nonNullable.group({
    ticker: ['', [Validators.required]],
  });

  protected readonly submitting = signal(false);
  protected readonly formError = signal<string | null>(null);
  protected readonly notFound = signal(false);
  protected readonly quote = signal<Quote | null>(null);
  protected readonly submitted = signal(false);
  protected readonly holdings = signal<Holding[]>([]);
  protected readonly loading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);
  
  protected readonly portfolioBalance = computed(() =>
    (this.cashBalance ?? 0) + this.holdings().reduce((sum, h) => sum + h.marketValue, 0)
  );
  
  ngOnInit(): void {
    this.fetchHoldingsData();
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.submitted.set(true);
      return;
    }

    this.submitted.set(false);
    this.formError.set(null);
    this.notFound.set(false);
    this.quote.set(null);
    this.submitting.set(true);
    const tickerValue = this.form.controls.ticker.value;
    this.form.controls.ticker.reset('');

    this.quoteService.getQuote(tickerValue).subscribe({
      next: (quote) => {
        this.submitting.set(false);
        this.quote.set(quote);
      },
      error: (error: ApiError) => {
        this.submitting.set(false);
        if (error.status === 404) {
          this.notFound.set(true);
        } else {
          this.formError.set(error.detail);
        }
      },
    });
  }

  protected errorFor(field: SearchField): string {
    const control = this.form.get(field);
    if (!control || !this.submitted() || !control.errors) {
      return '';
    }
    if (control.errors['server']) {
      return control.errors['server'];
    }
    if (control.errors['required']) {
      return 'Required';
    }
    return '';
  }

  @HostListener('document:click', ['$event'])
  protected onDocumentClick(event: MouseEvent): void {
    const formEl = this.searchForm()?.nativeElement;
    if (formEl && !formEl.contains(event.target as Node)) {
      this.quote.set(null);
      this.notFound.set(false);
      this.submitted.set(false);
    }
  }

  protected clearTicker(): void {
    this.form.controls.ticker.reset('');
    this.submitted.set(false);
  }

  protected onQuoteClick(): void {
    this.router.navigate([`/stocks/${this.quote()?.ticker}`]);
  }

  protected fetchHoldingsData(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.tradeService.getHoldings().subscribe({
      next: (data: Holding[]) => {
        this.loading.set(false);
        this.holdings.set(data);
      },
      error: (error: ApiError) => {
        this.loading.set(false);
        this.errorMessage.set(error.detail);
      },
    });
  }
}
