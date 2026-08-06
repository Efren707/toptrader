import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';

import { errorInterceptor } from '../../../core/interceptors/error.interceptor';
import { VerifyEmail } from './verify-email';

describe('VerifyEmail', () => {
  let component: VerifyEmail;
  let fixture: ComponentFixture<VerifyEmail>;
  let httpTesting: HttpTestingController;

  async function setUp(token: string | null): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [VerifyEmail],
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { queryParamMap: convertToParamMap(token !== null ? { token } : {}) },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(VerifyEmail);
    component = fixture.componentInstance;
    httpTesting = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  }

  afterEach(() => {
    httpTesting.verify();
  });

  function expectVerifyEmailRequest() {
    return httpTesting.expectOne((req) => req.url.endsWith('/auth/verify-email'));
  }

  function expectNoVerifyEmailRequest(): void {
    httpTesting.expectNone((req) => req.url.endsWith('/auth/verify-email'));
  }

  function expectResendVerificationRequest() {
    return httpTesting.expectOne((req) => req.url.endsWith('/auth/resend-verification'));
  }

  function expectNoResendVerificationRequest(): void {
    httpTesting.expectNone((req) => req.url.endsWith('/auth/resend-verification'));
  }

  function emailInput(): HTMLInputElement {
    return fixture.nativeElement.querySelector('input[type="email"]');
  }

  function resendButton(): HTMLButtonElement {
    return fixture.nativeElement.querySelector('button[type="submit"]');
  }

  function enterEmailAndSubmitResend(email: string): void {
    const input = emailInput();
    input.value = email;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    resendButton().click();
    fixture.detectChanges();
  }

  describe('with a valid token in the URL', () => {
    it('creates the component', async () => {
      await setUp('valid-raw-token');
      expect(component).toBeTruthy();
      expectVerifyEmailRequest().flush(null);
    });

    it('calls the verify-email endpoint with the token', async () => {
      await setUp('valid-raw-token');

      const req = expectVerifyEmailRequest();
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ rawToken: 'valid-raw-token' });
      req.flush(null);
    });

    it('shows a success message and a dashboard link when verification succeeds', async () => {
      await setUp('valid-raw-token');
      expectVerifyEmailRequest().flush(null);
      fixture.detectChanges();

      expect(fixture.nativeElement.textContent).toContain('Email verified');
      expect(fixture.nativeElement.querySelector('a[href="/dashboard"]')).not.toBeNull();
      expect(emailInput()).toBeNull();
    });

    it('shows the failed state with a resend form when verification fails', async () => {
      await setUp('expired-token');
      expectVerifyEmailRequest().flush(
        { detail: 'Invalid or expired email verification token.' },
        { status: 400, statusText: 'Bad Request' },
      );
      fixture.detectChanges();

      expect(fixture.nativeElement.textContent).toContain("Couldn't verify your email");
      expect(emailInput()).not.toBeNull();
    });
  });

  describe('without a token in the URL', () => {
    beforeEach(() => setUp(null));

    it('shows the failed state without calling the verify-email endpoint', () => {
      expect(fixture.nativeElement.textContent).toContain("Couldn't verify your email");
      expectNoVerifyEmailRequest();
    });

    it('shows a required error and does not call the API when the resend email is blank', () => {
      resendButton().click();
      fixture.detectChanges();

      expect(fixture.nativeElement.querySelector('.error')?.textContent).toContain('Required');
      expectNoResendVerificationRequest();
    });

    it('shows an email-format error and does not call the API for an invalid resend email', () => {
      enterEmailAndSubmitResend('not-an-email');

      expect(fixture.nativeElement.querySelector('.error')?.textContent).toContain(
        'Must be a valid email address',
      );
      expectNoResendVerificationRequest();
    });

    it('calls the resend-verification endpoint with the entered email on valid submit', () => {
      enterEmailAndSubmitResend('trader@example.com');

      const req = expectResendVerificationRequest();
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ email: 'trader@example.com' });
      req.flush(null);
    });

    it('shows the enumeration-safe confirmation message on successful resend', () => {
      enterEmailAndSubmitResend('trader@example.com');
      expectResendVerificationRequest().flush(null);
      fixture.detectChanges();

      expect(fixture.nativeElement.querySelector('.form-success')?.textContent).toContain(
        "we've sent a new link",
      );
    });

    it('shows the server error message when the resend request fails', () => {
      enterEmailAndSubmitResend('trader@example.com');
      expectResendVerificationRequest().flush(
        { detail: 'Rate limit exceeded. Try again later.' },
        { status: 429, statusText: 'Too Many Requests' },
      );
      fixture.detectChanges();

      expect(fixture.nativeElement.querySelector('.form-error')?.textContent).toContain(
        'Rate limit exceeded. Try again later.',
      );
    });
  });
});
