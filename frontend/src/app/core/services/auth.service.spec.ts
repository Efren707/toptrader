import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('posts the raw token to /auth/verify-email', () => {
    service.verifyEmail({ rawToken: 'raw-token' }).subscribe();

    const req = httpTesting.expectOne((req) => req.url.endsWith('/auth/verify-email'));
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ rawToken: 'raw-token' });
    req.flush(null);
  });

  it('posts the email to /auth/resend-verification', () => {
    service.resendVerification({ email: 'trader@example.com' }).subscribe();

    const req = httpTesting.expectOne((req) => req.url.endsWith('/auth/resend-verification'));
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'trader@example.com' });
    req.flush(null);
  });
});
