import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { vi } from 'vitest';

import { errorInterceptor } from '../../../core/interceptors/error.interceptor';
import { Login } from './login';

describe('Login', () => {
  let component: Login;
  let fixture: ComponentFixture<Login>;
  let httpTesting: HttpTestingController;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Login);
    component = fixture.componentInstance;
    httpTesting = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTesting.verify();
  });

  function demoButton(): HTMLButtonElement {
    return fixture.nativeElement.querySelector('button[type="button"]');
  }

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('navigates to the dashboard on successful demo login', () => {
    const navigateSpy = vi.spyOn(router, 'navigate');
    demoButton().click();
    fixture.detectChanges();

    const req = httpTesting.expectOne((req) => req.url.endsWith('/auth/demo-login'));
    req.flush({
      id: 1,
      email: 'demo@toptrader.dev',
      username: 'demo',
      cashBalance: 420,
      isDemo: true,
    });

    expect(navigateSpy).toHaveBeenCalledWith(['/dashboard']);
  });

  it('surfaces an error and does not navigate when demo login fails', () => {
    const navigateSpy = vi.spyOn(router, 'navigate');
    demoButton().click();
    fixture.detectChanges();

    const req = httpTesting.expectOne((req) => req.url.endsWith('/auth/demo-login'));
    req.flush(
      { detail: 'Demo account unavailable' },
      { status: 500, statusText: 'Internal Server Error' },
    );
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.form-error').textContent).toContain(
      'Demo account unavailable',
    );
    expect(navigateSpy).not.toHaveBeenCalled();
  });
});
