import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type TradeSide = 'BUY' | 'SELL';

export interface Transaction {
  id: number;
  ticker: string;
  side: TradeSide;
  quantity: number;
  pricePerShare: number;
  totalAmount: number;
  executedAt: string;
}

export interface Holding {
  ticker: string;
  quantity: number;
  averageCostBasis: number;
  currentPrice: number;
  marketValue: number;
  unrealizedGainLoss: number;
}

export interface TradeResult {
  transaction: Transaction;
  cashBalance: number;
  holding: Holding | null;
}

export interface TradeRequest {
  ticker: string;
  quantity: number;
}

@Injectable({ providedIn: 'root' })
export class TradeService {
  private readonly http = inject(HttpClient);

  buyStock(request: TradeRequest): Observable<TradeResult> {
    return this.http.post<TradeResult>(`${environment.apiUrl}/trades/buy`, request)
  }

}