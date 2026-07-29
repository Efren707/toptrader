import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export enum TradeSide { BUY = 'BUY', SELL = 'SELL' }

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
  percentChange: number;
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

  sellStock(request: TradeRequest): Observable<TradeResult> {
    return this.http.post<TradeResult>(`${environment.apiUrl}/trades/sell`, request)
  }

  getHolding(ticker: string): Observable<Holding> {
    return this.http.get<Holding>(`${environment.apiUrl}/trades/holdings/${ticker}`)
  }

  getHoldings(): Observable<Holding[]> {
    return this.http.get<Holding[]>(`${environment.apiUrl}/trades/holdings`)
  }

  getTransactions(): Observable<Transaction[]> {
    return this.http.get<Transaction[]>(`${environment.apiUrl}/trades/transactions`)
  } 

}