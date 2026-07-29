import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { errorInterceptor } from '../../core/interceptors/error.interceptor';
import { AuthService, UserSummary } from '../../core/services/auth.service';
import { Holding } from '../../core/services/trade.service';
import { Performance } from './performance';

describe('Performance', () => {
  let component: Performance;
  let fixture: ComponentFixture<Performance>;
  let httpTesting: HttpTestingController;

  const mockUser: UserSummary = {
    id: 1,
    email: 'test@example.com',
    username: 'testuser',
    cashBalance: 200,
  };

  function setup(): void {
    const authService = TestBed.inject(AuthService);
    authService.currentUser.set(mockUser);

    fixture = TestBed.createComponent(Performance);
    component = fixture.componentInstance;
    httpTesting = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  }

  function flushHoldings(holdings: Holding[]): void {
    httpTesting.expectOne((req) => req.url.endsWith('/trades/holdings')).flush(holdings);
    fixture.detectChanges();
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Performance],
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    }).compileComponents();
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should create', () => {
    setup();
    flushHoldings([]);

    expect(component).toBeTruthy();
  });

  it('shows a positive P&L in green when the portfolio value has grown past the starting balance', () => {
    setup();
    const holdings: Holding[] = [
      { ticker: 'AAPL', quantity: 2, averageCostBasis: 100, currentPrice: 200, percentChange: 5, marketValue: 400, unrealizedGainLoss: 200 },
    ];
    flushHoldings(holdings);

    // cash 200 + holdings 400 = 600 portfolio value; 600 - 500 starting balance = +100 (+20%)
    const earnings = fixture.nativeElement.querySelector('.earnings-value');
    expect(earnings.textContent).toContain('$100.00');
    expect(earnings.textContent).toContain('20.00%');
    expect(earnings.classList.contains('positive')).toBe(true);
    expect(earnings.classList.contains('negative')).toBe(false);

    const statValues = fixture.nativeElement.querySelectorAll('.stat-value');
    expect(statValues[0].textContent).toContain('$600.00');
    expect(statValues[1].textContent).toContain('$200.00');
  });

  it('shows a negative P&L in red when the portfolio value has dropped below the starting balance', () => {
    setup();
    const holdings: Holding[] = [
      { ticker: 'AAPL', quantity: 1, averageCostBasis: 100, currentPrice: 50, percentChange: -3, marketValue: 50, unrealizedGainLoss: -50 },
    ];
    flushHoldings(holdings);

    // cash 200 + holdings 50 = 250 portfolio value; 250 - 500 starting balance = -250 (-50%)
    const earnings = fixture.nativeElement.querySelector('.earnings-value');
    expect(earnings.textContent).toContain('-$250.00');
    expect(earnings.textContent).toContain('-50.00%');
    expect(earnings.classList.contains('negative')).toBe(true);
    expect(earnings.classList.contains('positive')).toBe(false);
  });

  it('shows an error message and no P&L figure when the holdings request fails', () => {
    setup();
    httpTesting
      .expectOne((req) => req.url.endsWith('/trades/holdings'))
      .flush({ detail: 'Something went wrong', errors: {} }, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.status-error')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('.stats-card')).toBeFalsy();
  });
});
