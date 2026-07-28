import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CurrencyPipe, PercentPipe } from '@angular/common';
import { AuthService } from '../../core/services/auth.service';
import { Holding, TradeService } from '../../core/services/trade.service';
import { ApiError } from '../../core/interceptors/error.interceptor';
import { Navbar } from '../../shared/navbar/navbar';
import { Card } from '../../shared/ui/card/card';

@Component({
  selector: 'app-performance',
  imports: [ Navbar, Card, CurrencyPipe, PercentPipe ],
  templateUrl: './performance.html',
  styleUrl: './performance.css',
})
export class Performance implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly tradeService = inject(TradeService);
  protected readonly cashBalance = this.authService.currentUser()?.cashBalance;

  protected readonly holdings = signal<Holding[]>([]);
  protected readonly loading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly startingCashBalance = 500;
  
  protected readonly portfolioBalance = computed(() =>
    (this.cashBalance ?? 0) + this.holdings().reduce((sum, h) => sum + h.marketValue, 0)
  );

  protected readonly performanceBalance = computed(() => this.portfolioBalance() - this.startingCashBalance);
  protected readonly performancePercentage = computed(() => this.performanceBalance() / this.startingCashBalance);

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

}
