import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { vi } from 'vitest';

import { errorInterceptor } from '../../../core/interceptors/error.interceptor';
import { NotificationService } from '../../../core/services/notification.service';
import { Register } from './register';

describe('Register', () => {
  let component: Register;
  let fixture: ComponentFixture<Register>;
  let httpTesting: HttpTestingController;
  let notificationService: NotificationService;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Register],
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Register);
    component = fixture.componentInstance;
    httpTesting = TestBed.inject(HttpTestingController);
    notificationService = TestBed.inject(NotificationService);
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTesting.verify();
  });

  function emailInput(): HTMLInputElement {
    return fixture.nativeElement.querySelector('input[type="email"]');
  }

  function usernameInput(): HTMLInputElement {
    return fixture.nativeElement.querySelector('input[type="text"]');
  }

  function passwordInput(): HTMLInputElement {
    return fixture.nativeElement.querySelector('input[type="password"]');
  }

  function submitButton(): HTMLButtonElement {
    return fixture.nativeElement.querySelector('button[type="submit"]');
  }

  function setValue(input: HTMLInputElement, value: string): void {
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  function fillFormAndSubmit(): void {
    setValue(emailInput(), 'trader@example.com');
    setValue(usernameInput(), 'trader');
    setValue(passwordInput(), 'password123');
    fixture.detectChanges();
    submitButton().click();
    fixture.detectChanges();
  }

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('shows a verification notice and navigates to the dashboard on successful registration', () => {
    const navigateSpy = vi.spyOn(router, 'navigate');
    fillFormAndSubmit();

    const req = httpTesting.expectOne((req) => req.url.endsWith('/auth/register'));
    req.flush({ id: 1, email: 'trader@example.com', username: 'trader', cashBalance: 500 });

    expect(notificationService.message()).toContain('verify your address');
    expect(navigateSpy).toHaveBeenCalledWith(['/dashboard']);
  });

  it('does not show a notice or navigate when registration fails', () => {
    const navigateSpy = vi.spyOn(router, 'navigate');
    fillFormAndSubmit();

    const req = httpTesting.expectOne((req) => req.url.endsWith('/auth/register'));
    req.flush(
      { detail: 'Email already in use' },
      { status: 409, statusText: 'Conflict' },
    );
    fixture.detectChanges();

    expect(notificationService.message()).toBeNull();
    expect(navigateSpy).not.toHaveBeenCalled();
  });
});
