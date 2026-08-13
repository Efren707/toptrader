import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';

import { errorInterceptor } from '../../core/interceptors/error.interceptor';
import { AuthService, UserSummary } from '../../core/services/auth.service';
import { Quote } from '../../core/services/quote.service';
import { Holding } from '../../core/services/trade.service';
import { StockDetails } from './stock-details';

describe('StockDetails', () => {
  let component: StockDetails;
  let fixture: ComponentFixture<StockDetails>;
  let httpTesting: HttpTestingController;

  const mockUser: UserSummary = {
    id: 1,
    email: 'test@example.com',
    username: 'testuser',
    cashBalance: 1000,
    isDemo: false,
  };

  const mockQuote: Quote = {
    ticker: 'AAPL',
    companyName: 'Apple Inc.',
    price: 200,
    percentChange: 2,
    asOf: new Date().toISOString(),
  };

  const mockHolding: Holding = {
    ticker: 'AAPL',
    quantity: 2,
    averageCostBasis: 100,
    currentPrice: 200,
    percentChange: 2,
    marketValue: 400,
    unrealizedGainLoss: 200,
  };

  function setup(): void {
    const authService = TestBed.inject(AuthService);
    authService.currentUser.set(mockUser);

    fixture = TestBed.createComponent(StockDetails);
    component = fixture.componentInstance;
    httpTesting = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  }

  function flushHoldingsList(holdings: Holding[]): void {
    httpTesting.expectOne((req) => req.url.endsWith('/trades/holdings')).flush(holdings);
  }

  function flushHoldingsListError(status: number): void {
    httpTesting
      .expectOne((req) => req.url.endsWith('/trades/holdings'))
      .flush({ detail: 'Could not load holdings.', errors: {} }, { status, statusText: 'Error' });
  }

  function flushQuote(quote: Quote): void {
    httpTesting.expectOne((req) => req.url.endsWith('/quotes/AAPL')).flush(quote);
  }

  function flushQuoteError(status: number): void {
    httpTesting
      .expectOne((req) => req.url.endsWith('/quotes/AAPL'))
      .flush({ detail: 'Could not load quote.', errors: {} }, { status, statusText: 'Error' });
  }

  function flushNoHolding(): void {
    httpTesting
      .expectOne((req) => req.url.endsWith('/trades/holdings/AAPL'))
      .flush({ detail: 'Not Found', errors: {} }, { status: 404, statusText: 'Not Found' });
  }

  function flushHolding(holding: Holding): void {
    httpTesting.expectOne((req) => req.url.endsWith('/trades/holdings/AAPL')).flush(holding);
  }

  function flushHoldingError(status: number): void {
    httpTesting
      .expectOne((req) => req.url.endsWith('/trades/holdings/AAPL'))
      .flush({ detail: 'Could not load your position.', errors: {} }, { status, statusText: 'Error' });
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StockDetails],
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { paramMap: convertToParamMap({ ticker: 'AAPL' }) },
            paramMap: of(convertToParamMap({ ticker: 'AAPL' })),
          },
        },
      ],
    }).compileComponents();
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should create', () => {
    setup();
    flushHoldingsList([]);
    flushQuote(mockQuote);
    flushNoHolding();

    expect(component).toBeTruthy();
  });

  it('shows a not-found message when the quote request 404s, and no error banner', () => {
    setup();
    flushHoldingsList([]);
    flushQuoteError(404);
    flushNoHolding();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.status')?.textContent).toContain('No stock found for "AAPL"');
    expect(fixture.nativeElement.querySelector('.status-error')).toBeFalsy();
  });

  it('shows an error message and no layout when the quote request fails for a non-404 reason', () => {
    setup();
    flushHoldingsList([]);
    flushQuoteError(500);
    flushNoHolding();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.status-error')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('.layout')).toBeFalsy();
  });

  it('renders the quote and trade form with no position-stats block when the user holds no position (404)', () => {
    setup();
    flushHoldingsList([]);
    flushQuote(mockQuote);
    flushNoHolding();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.position-stats')).toBeFalsy();
    expect(fixture.nativeElement.querySelector('.status-error')).toBeFalsy();
    expect(fixture.nativeElement.querySelector('app-trade-form')).toBeTruthy();
  });

  it('renders position stats when the user holds a position', () => {
    setup();
    flushHoldingsList([mockHolding]);
    flushQuote(mockQuote);
    flushHolding(mockHolding);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.position-stats')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('.status-error')).toBeFalsy();
  });

  it('shows an error message in place of position stats, but still renders the trade form, when the holding request fails for a non-404 reason', () => {
    setup();
    flushHoldingsList([]);
    flushQuote(mockQuote);
    flushHoldingError(500);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.position-stats')).toBeFalsy();
    expect(fixture.nativeElement.querySelector('.status-error')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('app-trade-form')).toBeTruthy();
  });

  it('shows "Unavailable" for portfolio diversity, without hiding the rest of position stats, when the holdings list request fails', () => {
    setup();
    flushHoldingsListError(500);
    flushQuote(mockQuote);
    flushHolding(mockHolding);
    fixture.detectChanges();

    const diversityValue = fixture.nativeElement.querySelector('.stat-row-value.status-error');
    expect(diversityValue?.textContent).toContain('Unavailable');
    expect(fixture.nativeElement.querySelectorAll('.stat-headline').length).toBe(2);
  });
});
