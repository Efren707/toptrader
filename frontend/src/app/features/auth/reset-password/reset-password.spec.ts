import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';

import { errorInterceptor } from '../../../core/interceptors/error.interceptor';
import { ResetPassword } from './reset-password';

describe('ResetPassword', () => {
  let component: ResetPassword;
  let fixture: ComponentFixture<ResetPassword>;
  let httpTesting: HttpTestingController;

  async function setUp(token: string | null): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [ResetPassword],
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

    fixture = TestBed.createComponent(ResetPassword);
    component = fixture.componentInstance;
    httpTesting = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  }

  afterEach(() => {
    httpTesting.verify();
  });

  function passwordInput(): HTMLInputElement {
    return fixture.nativeElement.querySelector('input[type="password"]');
  }

  function submitButton(): HTMLButtonElement {
    return fixture.nativeElement.querySelector('button[type="submit"]');
  }

  function expectNoResetPasswordRequest(): void {
    httpTesting.expectNone((req) => req.url.endsWith('/auth/reset-password'));
  }

  function expectResetPasswordRequest() {
    return httpTesting.expectOne((req) => req.url.endsWith('/auth/reset-password'));
  }

  function enterPasswordAndSubmit(password: string): void {
    const input = passwordInput();
    input.value = password;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    submitButton().click();
    fixture.detectChanges();
  }

  describe('without a token in the URL', () => {
    beforeEach(() => setUp(null));

    it('shows an invalid-link message instead of the form', () => {
      expect(fixture.nativeElement.textContent).toContain('Invalid reset link');
      expect(passwordInput()).toBeNull();
      expectNoResetPasswordRequest();
    });
  });

  describe('with a valid token in the URL', () => {
    beforeEach(() => setUp('valid-raw-token'));

    it('creates the component', () => {
      expect(component).toBeTruthy();
    });

    it('shows a required error and does not call the API when password is left blank', () => {
      submitButton().click();
      fixture.detectChanges();

      expect(fixture.nativeElement.querySelector('.error')?.textContent).toContain('Required');
      expectNoResetPasswordRequest();
    });

    it('shows a length error and does not call the API for a short password', () => {
      enterPasswordAndSubmit('short');

      expect(fixture.nativeElement.querySelector('.error')?.textContent).toContain(
        'Must be at least 8 characters',
      );
      expectNoResetPasswordRequest();
    });

    it('calls the reset-password endpoint with the token and new password on valid submit', () => {
      enterPasswordAndSubmit('newPassword123');

      const req = expectResetPasswordRequest();
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({
        rawToken: 'valid-raw-token',
        password: 'newPassword123',
      });
    });

    it('disables the submit button while the request is in flight', () => {
      enterPasswordAndSubmit('newPassword123');

      expect(submitButton().disabled).toBe(true);

      expectResetPasswordRequest().flush(null);
    });

    it('shows a success message and re-enables the button on success', () => {
      enterPasswordAndSubmit('newPassword123');
      expectResetPasswordRequest().flush(null);
      fixture.detectChanges();

      expect(fixture.nativeElement.querySelector('.form-success')?.textContent).toContain(
        'Your password has been changed successfully.',
      );
      expect(submitButton().disabled).toBe(false);
    });

    it('shows the server error message and re-enables the button when the token is invalid or expired', () => {
      enterPasswordAndSubmit('newPassword123');
      expectResetPasswordRequest().flush(
        { detail: 'Invalid or expired reset token.' },
        { status: 400, statusText: 'Bad Request' },
      );
      fixture.detectChanges();

      expect(fixture.nativeElement.querySelector('.form-error')?.textContent).toContain(
        'Invalid or expired reset token.',
      );
      expect(submitButton().disabled).toBe(false);
    });
  });
});
