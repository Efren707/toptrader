import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { AuthService } from '../../core/services/auth.service';
import { ApiError } from '../../core/interceptors/error.interceptor';
import { Holding, TradeService } from '../../core/services/trade.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-dashboard',
  imports: [ CurrencyPipe, DecimalPipe ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly tradeService = inject(TradeService);
  protected readonly username = this.authService.currentUser()?.username;
  protected readonly cashBalance = this.authService.currentUser()?.cashBalance;
  private readonly router = inject(Router);

  protected readonly holdings = signal<Holding[]>([]);
  protected readonly loading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);
  
  protected readonly portfolioBalance = computed(() =>
    (this.cashBalance ?? 0) + this.holdings().reduce((sum, h) => sum + h.marketValue, 0)
  );
  
  ngOnInit(): void {
    this.fetchHoldingsData();
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

  protected onHoldingClick(ticker: string): void {
    this.router.navigate([`/stocks/${ticker}`]);
  }
}
