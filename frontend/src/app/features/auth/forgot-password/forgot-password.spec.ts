import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { errorInterceptor } from '../../../core/interceptors/error.interceptor';
import { ForgotPassword } from './forgot-password';

describe('ForgotPassword', () => {
  let component: ForgotPassword;
  let fixture: ComponentFixture<ForgotPassword>;
  let httpTesting: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ForgotPassword],
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ForgotPassword);
    component = fixture.componentInstance;
    httpTesting = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTesting.verify();
  });

  function emailInput(): HTMLInputElement {
    return fixture.nativeElement.querySelector('input[type="email"]');
  }

  function submitButton(): HTMLButtonElement {
    return fixture.nativeElement.querySelector('button[type="submit"]');
  }

  function expectNoForgotPasswordRequest(): void {
    httpTesting.expectNone((req) => req.url.endsWith('/auth/forgot-password'));
  }

  function expectForgotPasswordRequest() {
    return httpTesting.expectOne((req) => req.url.endsWith('/auth/forgot-password'));
  }

  function enterEmailAndSubmit(email: string): void {
    const input = emailInput();
    input.value = email;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    submitButton().click();
    fixture.detectChanges();
  }

  it('creates the component', () => {
    expect(component).toBeTruthy();
  });

  it('shows a required error and does not call the API when email is left blank', () => {
    submitButton().click();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.error')?.textContent).toContain('Required');
    expectNoForgotPasswordRequest();
  });

  it('shows an email-format error and does not call the API for an invalid email', () => {
    enterEmailAndSubmit('not-an-email');

    expect(fixture.nativeElement.querySelector('.error')?.textContent).toContain(
      'Must be a valid email address',
    );
    expectNoForgotPasswordRequest();
  });

  it('calls the forgot-password endpoint with the entered email on valid submit', () => {
    enterEmailAndSubmit('trader@example.com');

    const req = expectForgotPasswordRequest();
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'trader@example.com' });
  });

  it('disables the submit button while the request is in flight', () => {
    enterEmailAndSubmit('trader@example.com');

    expect(submitButton().disabled).toBe(true);

    expectForgotPasswordRequest().flush(null);
  });

  it('shows the enumeration-safe confirmation message and re-enables the button on success', () => {
    enterEmailAndSubmit('trader@example.com');
    expectForgotPasswordRequest().flush(null);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.form-success')?.textContent).toContain(
      'If that email is registered',
    );
    expect(submitButton().disabled).toBe(false);
  });

  it('shows the server error message and re-enables the button when the request fails', () => {
    enterEmailAndSubmit('trader@example.com');
    expectForgotPasswordRequest().flush(
      { detail: 'Rate limit exceeded. Try again later.' },
      { status: 429, statusText: 'Too Many Requests' },
    );
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.form-error')?.textContent).toContain(
      'Rate limit exceeded. Try again later.',
    );
    expect(submitButton().disabled).toBe(false);
  });
});
