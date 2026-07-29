import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { Navbar } from './navbar';

describe('Navbar', () => {
  let component: Navbar;
  let fixture: ComponentFixture<Navbar>;
  let httpTesting: HttpTestingController;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Navbar],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(Navbar);
    component = fixture.componentInstance;
    httpTesting = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTesting.verify();
  });

  function accountButton(): HTMLButtonElement {
    return fixture.nativeElement.querySelector('.account-button');
  }

  function menuButtonByText(text: string): HTMLButtonElement {
    const buttons: HTMLButtonElement[] = Array.from(
      fixture.nativeElement.querySelectorAll('.menu-item button'),
    );
    const button = buttons.find((b) => b.textContent?.trim() === text);
    if (!button) {
      throw new Error(`No account-menu button found with text "${text}"`);
    }
    return button;
  }

  function searchAndSubmit(ticker: string): void {
    const input: HTMLInputElement = fixture.nativeElement.querySelector('.search-input');
    input.value = ticker;
    input.dispatchEvent(new Event('input'));
    fixture.nativeElement.querySelector('.search-form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('opens the account dropdown on click and closes it again on a second click', () => {
    expect(fixture.nativeElement.querySelector('.account-menu')).toBeFalsy();

    accountButton().click();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.account-menu')).toBeTruthy();

    accountButton().click();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.account-menu')).toBeFalsy();
  });

  it('shows the matching result when a ticker search succeeds', () => {
    searchAndSubmit('AAPL');

    const req = httpTesting.expectOne((r) => r.url.endsWith('/quotes/AAPL'));
    req.flush({ ticker: 'AAPL', companyName: 'Apple Inc.', price: 210.5, asOf: '2026-07-28T15:00:00Z' });
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.result-ticker')?.textContent).toContain('AAPL');
  });

  it('shows "No stocks found" when the ticker search 404s', () => {
    searchAndSubmit('ZZZZ');

    const req = httpTesting.expectOne((r) => r.url.endsWith('/quotes/ZZZZ'));
    req.flush({ detail: 'Not found', errors: {} }, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.search-empty')?.textContent).toContain('No stocks found');
  });

  it('navigates to the stock details page when a search result is clicked', () => {
    searchAndSubmit('AAPL');
    httpTesting
      .expectOne((r) => r.url.endsWith('/quotes/AAPL'))
      .flush({ ticker: 'AAPL', companyName: 'Apple Inc.', price: 210.5, asOf: '2026-07-28T15:00:00Z' });
    fixture.detectChanges();

    fixture.nativeElement.querySelector('.result-item').click();

    expect(router.navigate).toHaveBeenCalledWith(['/stocks/AAPL']);
  });

  it('navigates to transactions and closes the dropdown when "Transaction history" is clicked', () => {
    accountButton().click();
    fixture.detectChanges();

    menuButtonByText('Transaction history').click();
    fixture.detectChanges();

    expect(router.navigate).toHaveBeenCalledWith(['/transactions']);
    expect(fixture.nativeElement.querySelector('.account-menu')).toBeFalsy();
  });

  it('navigates to performance when "Performance" is clicked', () => {
    accountButton().click();
    fixture.detectChanges();

    menuButtonByText('Performance').click();
    fixture.detectChanges();

    expect(router.navigate).toHaveBeenCalledWith(['/performance']);
  });

  it('logs out and navigates to login when "Logout" succeeds', () => {
    accountButton().click();
    fixture.detectChanges();

    menuButtonByText('Logout').click();

    httpTesting.expectOne((r) => r.url.endsWith('/auth/logout')).flush(null);
    fixture.detectChanges();

    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });
});
