import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { AuthService, UserSummary } from '../../core/services/auth.service';
import { Holding } from '../../core/services/trade.service';
import { Dashboard } from './dashboard';

describe('Dashboard', () => {
  let component: Dashboard;
  let fixture: ComponentFixture<Dashboard>;
  let httpTesting: HttpTestingController;
  let router: Router;

  const mockUser: UserSummary = {
    id: 1,
    email: 'trader@example.com',
    username: 'trader',
    cashBalance: 500,
    isDemo: false,
    avatarKey: null,
  };

  const mockHoldings: Holding[] = [
    {
      ticker: 'AAPL',
      quantity: 10,
      averageCostBasis: 150,
      currentPrice: 210.5,
      percentChange: 6.25,
      marketValue: 2105,
      unrealizedGainLoss: 605,
    },
    {
      ticker: 'TSLA',
      quantity: 5,
      averageCostBasis: 300,
      currentPrice: 250,
      percentChange: -16.67,
      marketValue: 1250,
      unrealizedGainLoss: -250,
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    TestBed.inject(AuthService).currentUser.set(mockUser);

    fixture = TestBed.createComponent(Dashboard);
    component = fixture.componentInstance;
    httpTesting = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  function flushHoldings(holdings: Holding[] = mockHoldings): void {
    fixture.detectChanges();
    httpTesting.expectOne((r) => r.url.endsWith('/trades/holdings')).flush(holdings);
    fixture.detectChanges();
  }

  function headerLabels(): (string | undefined)[] {
    const spans: HTMLElement[] = Array.from(
      fixture.nativeElement.querySelectorAll('.holdings-header span'),
    );
    return spans.map((el) => el.textContent?.trim());
  }

  function holdingRows(): HTMLButtonElement[] {
    return Array.from(fixture.nativeElement.querySelectorAll('.holding-row'));
  }

  it('should create', () => {
    flushHoldings([]);
    expect(component).toBeTruthy();
  });

  it('renders a header row labeling all 7 columns', () => {
    flushHoldings();

    expect(headerLabels()).toEqual([
      'Ticker',
      'Shares',
      'Avg. Cost',
      'Market Value',
      'Unrealized P/L',
      'Current Price',
      'Percent Change',
    ]);
  });

  it('renders a profitable holding with all 7 columns and positive coloring', () => {
    flushHoldings();

    const row = holdingRows()[0];
    expect(row.querySelector('.col-ticker')?.textContent).toContain('AAPL');
    expect(row.querySelector('.col-shares')?.textContent?.trim()).toBe('10');
    expect(row.querySelector('.col-avg-cost')?.textContent).toContain('$150.00');
    expect(row.querySelector('.col-market-value')?.textContent).toContain('$2,105.00');
    expect(row.querySelector('.col-pl')?.textContent).toContain('+$605.00');
    expect(row.querySelector('.col-pl')?.classList).toContain('positive');
    expect(row.querySelector('.col-price')?.textContent).toContain('$210.50');
    expect(row.querySelector('.col-change')?.textContent).toContain('+6.25%');
    expect(row.querySelector('.col-change')?.classList).toContain('positive');
  });

  it('renders a losing holding with negative coloring and no plus sign', () => {
    flushHoldings();

    const row = holdingRows()[1];
    expect(row.querySelector('.col-pl')?.textContent).toContain('-$250.00');
    expect(row.querySelector('.col-pl')?.classList).toContain('negative');
    expect(row.querySelector('.col-change')?.textContent).toContain('-16.67%');
    expect(row.querySelector('.col-change')?.classList).toContain('negative');
  });

  it('navigates to the stock details page when a holding row is clicked', () => {
    flushHoldings();

    holdingRows()[0].click();

    expect(router.navigate).toHaveBeenCalledWith(['/stocks/AAPL']);
  });
});
