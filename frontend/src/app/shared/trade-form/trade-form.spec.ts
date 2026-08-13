import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { errorInterceptor } from '../../core/interceptors/error.interceptor';
import { TradeResult, TradeSide } from '../../core/services/trade.service';
import { TradeForm } from './trade-form';

describe('TradeForm', () => {
  let component: TradeForm;
  let fixture: ComponentFixture<TradeForm>;
  let httpTesting: HttpTestingController;

  const tradeResult: TradeResult = {
    transaction: {
      id: 1,
      ticker: 'AAPL',
      side: TradeSide.BUY,
      quantity: 3,
      pricePerShare: 210.5,
      totalAmount: 631.5,
      executedAt: '2026-07-24T15:00:00Z',
    },
    cashBalance: 368.5,
    holding: {
      ticker: 'AAPL',
      quantity: 3,
      averageCostBasis: 210.5,
      currentPrice: 210.5,
      percentChange: 0,
      marketValue: 631.5,
      unrealizedGainLoss: 0,
    },
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TradeForm],
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TradeForm);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('ticker', 'AAPL');
    fixture.componentRef.setInput('price', 210.5);
    fixture.componentRef.setInput('cashBalance', 1000);
    fixture.componentRef.setInput('hasHolding', true);
    httpTesting = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTesting.verify();
  });

  function quantityInput(): HTMLInputElement {
    return fixture.nativeElement.querySelector('input');
  }

  function buttonByText(text: string): HTMLButtonElement {
    const buttons: HTMLButtonElement[] = Array.from(fixture.nativeElement.querySelectorAll('button'));
    const button = buttons.find((b) => b.textContent?.trim().includes(text));
    if (!button) {
      throw new Error(`No button found with text "${text}"`);
    }
    return button;
  }

  function expectNoBuyRequest(): void {
    httpTesting.expectNone((req) => req.url.endsWith('/trades/buy'));
  }

  function expectBuyRequest() {
    return httpTesting.expectOne((req) => req.url.endsWith('/trades/buy'));
  }

  function enterQuantityAndSubmit(quantity: string): void {
    const input = quantityInput();
    input.value = quantity;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    buttonByText('Review order').click();
    fixture.detectChanges();
  }

  it('creates the component', () => {
    expect(component).toBeTruthy();
  });

  it('shows a required error and does not move to the confirm step when quantity is left blank', () => {
    buttonByText('Review order').click();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.form-error')?.textContent).toContain('Required');
    expect(fixture.nativeElement.textContent).not.toContain('Order Summary');
    expectNoBuyRequest();
  });

  it('shows the order summary without calling the API when quantity is valid', () => {
    enterQuantityAndSubmit('3');

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Order Summary');
    expect(text).toContain('buy $631.50 of AAPL');
    expectNoBuyRequest();
  });

  it('calls the API with the confirmed quantity only after Confirm is clicked', () => {
    enterQuantityAndSubmit('3');

    buttonByText('Confirm').click();
    fixture.detectChanges();

    const req = expectBuyRequest();
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ ticker: 'AAPL', quantity: 3 });
    req.flush(tradeResult);
  });

  it('disables Cancel and Confirm while the confirm request is in flight', () => {
    enterQuantityAndSubmit('3');
    buttonByText('Confirm').click();
    fixture.detectChanges();

    expect(buttonByText('Cancel').disabled).toBe(true);
    expect(buttonByText('Confirm').disabled).toBe(true);

    expectBuyRequest().flush(tradeResult);
  });

  it('shows the order summary and emits traded on a successful confirm', () => {
    let emitted: TradeResult | undefined;
    component.traded.subscribe((result) => (emitted = result));

    enterQuantityAndSubmit('3');
    buttonByText('Confirm').click();
    fixture.detectChanges();

    expectBuyRequest().flush(tradeResult);
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Bought 3 AAPL');
    expect(text).toContain('$368.50');
    expect(emitted).toEqual(tradeResult);
  });

  it('returns to the quantity form without calling the API when Cancel is clicked', () => {
    enterQuantityAndSubmit('3');

    buttonByText('Cancel').click();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).not.toContain('Order Summary');
    expect(quantityInput()).toBeTruthy();
    expectNoBuyRequest();
  });

  it('shows the server error and stays on the confirm step when the trade fails', () => {
    enterQuantityAndSubmit('3');
    buttonByText('Confirm').click();
    fixture.detectChanges();

    expectBuyRequest().flush(
      { detail: 'Insufficient cash', errors: {} },
      { status: 422, statusText: 'Unprocessable Entity' },
    );
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.form-error')?.textContent).toContain('Insufficient cash');
    expect(fixture.nativeElement.textContent).toContain('Order Summary');
  });

  it('resets to a blank quantity form when Trade again is clicked after a successful trade', () => {
    enterQuantityAndSubmit('3');
    buttonByText('Confirm').click();
    fixture.detectChanges();
    expectBuyRequest().flush(tradeResult);
    fixture.detectChanges();

    buttonByText('Trade again').click();
    fixture.detectChanges();

    expect(quantityInput().value).toBe('');
    expect(fixture.nativeElement.textContent).not.toContain('Bought');
  });

  it('shows a read-only note instead of the submit button when readOnly is true', () => {
    fixture.componentRef.setInput('readOnly', true);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.read-only-note')?.textContent).toContain(
      'trading is disabled',
    );
    expect(fixture.nativeElement.querySelector('button[type="submit"]')).toBeNull();
    expect(quantityInput().disabled).toBe(true);
  });

  it('does not move to the confirm step when the form is submitted while readOnly', () => {
    fixture.componentRef.setInput('readOnly', true);
    fixture.detectChanges();

    const input = quantityInput();
    input.value = '3';
    input.dispatchEvent(new Event('input'));
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).not.toContain('Order Summary');
    expectNoBuyRequest();
  });

  it('shows the submit button and no read-only note when readOnly is false', () => {
    expect(fixture.nativeElement.querySelector('.read-only-note')).toBeNull();
    expect(buttonByText('Review order')).toBeTruthy();
  });
});
