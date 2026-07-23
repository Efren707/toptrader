import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Quote {
  ticker: string;
  companyName: string;
  price: number;
  asOf: string;
}

@Injectable({ providedIn: 'root' })
export class QuoteService {
  private readonly http = inject(HttpClient);

  getQuote(ticker: string): Observable<Quote> {
    return this.http.get<Quote>(`${environment.apiUrl}/quotes/${ticker}`)
  }

}