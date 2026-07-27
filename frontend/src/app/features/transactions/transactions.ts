import { Component, inject, OnInit, signal } from '@angular/core';
import { TradeService, Transaction } from '../../core/services/trade.service';
import { ApiError } from '../../core/interceptors/error.interceptor';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { Card } from '../../shared/ui/card/card';
import { Navbar } from '../../shared/navbar/navbar';

@Component({
  selector: 'app-transactions',
  imports: [CurrencyPipe, DatePipe, Card, Navbar],
  templateUrl: './transactions.html',
  styleUrl: './transactions.css',
})
export class Transactions implements OnInit {
  private readonly tradeService = inject(TradeService);


  protected readonly transactions = signal<Transaction[]>([]);
  protected readonly loading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    this.fetchTransactionsData();
  }

  protected fetchTransactionsData(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.tradeService.getTransactions().subscribe({
      next: (data: Transaction[]) => {
        this.loading.set(false);
        this.transactions.set(data);
      },
      error: (error: ApiError) => {
        this.loading.set(false);
        this.errorMessage.set(error.detail);
      },
    });
  }
}
