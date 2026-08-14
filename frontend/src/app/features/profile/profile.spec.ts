import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { errorInterceptor } from '../../core/interceptors/error.interceptor';
import { AuthService, UserSummary } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { Profile } from './profile';

describe('Profile', () => {
  let component: Profile;
  let fixture: ComponentFixture<Profile>;
  let httpTesting: HttpTestingController;
  let notificationService: NotificationService;
  let router: Router;

  const mockUser: UserSummary = {
    id: 1,
    email: 'trader@example.com',
    username: 'trader',
    cashBalance: 500,
    isDemo: false,
    avatarKey: null,
  };

  function setup(user: UserSummary = mockUser): void {
    const authService = TestBed.inject(AuthService);
    authService.currentUser.set(user);

    fixture = TestBed.createComponent(Profile);
    component = fixture.componentInstance;
    httpTesting = TestBed.inject(HttpTestingController);
    notificationService = TestBed.inject(NotificationService);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Profile],
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

  function usernameInput(): HTMLInputElement {
    return fixture.nativeElement.querySelector('input[type="text"]');
  }

  function emailInput(): HTMLInputElement {
    return fixture.nativeElement.querySelector('input[type="email"]');
  }

  function passwordInput(): HTMLInputElement {
    return fixture.nativeElement.querySelector('input[type="password"]');
  }

  function submitButton(): HTMLButtonElement {
    return fixture.nativeElement.querySelector('button[type="submit"]');
  }

  function avatarTrigger(): HTMLButtonElement {
    return fixture.nativeElement.querySelector('.avatar-trigger');
  }

  function avatarImage(): HTMLImageElement {
    return fixture.nativeElement.querySelector('.avatar-image');
  }

  function avatarOption(key: string): HTMLButtonElement {
    return fixture.nativeElement.querySelector(`.avatar-option[aria-label="${key}"]`);
  }

  function pickerCancelButton(): HTMLButtonElement {
    return fixture.nativeElement.querySelectorAll('.avatar-picker-actions button')[0];
  }

  function pickerConfirmButton(): HTMLButtonElement {
    return fixture.nativeElement.querySelectorAll('.avatar-picker-actions button')[1];
  }

  function deleteAccountButton(): HTMLButtonElement {
    return fixture.nativeElement.querySelector('.delete-trigger button');
  }

  function deleteCancelButton(): HTMLButtonElement {
    return fixture.nativeElement.querySelectorAll('.delete-account-actions button')[0];
  }

  function deleteConfirmButton(): HTMLButtonElement {
    return fixture.nativeElement.querySelectorAll('.delete-account-actions button')[1];
  }

  function expectDeleteRequest() {
    return httpTesting.expectOne((req) => req.url.endsWith('/auth/me') && req.method === 'DELETE');
  }

  function setValue(input: HTMLInputElement, value: string): void {
    input.value = value;
    input.dispatchEvent(new Event('input'));
    input.dispatchEvent(new Event('blur'));
  }

  function submit(): void {
    fixture.detectChanges();
    submitButton().click();
    fixture.detectChanges();
  }

  function expectNoUpdateRequest(): void {
    httpTesting.expectNone((req) => req.url.endsWith('/auth/me'));
  }

  function expectUpdateRequest() {
    return httpTesting.expectOne((req) => req.url.endsWith('/auth/me'));
  }

  it('should create', () => {
    setup();
    expect(component).toBeTruthy();
  });

  it('prefills the form with the current user\'s username and email, leaving password blank', () => {
    setup();

    expect(usernameInput().value).toBe('trader');
    expect(emailInput().value).toBe('trader@example.com');
    expect(passwordInput().value).toBe('');
  });

  it('falls back to the "nova" preset avatar when the user has none selected', () => {
    setup();

    expect(avatarImage().src).toContain('/avatars/nova.svg');
  });

  it('shows the user\'s selected avatar when one is already set', () => {
    setup({ ...mockUser, avatarKey: 'ember' });

    expect(avatarImage().src).toContain('/avatars/ember.svg');
  });

  it('does not call the API when submitting with no changes', () => {
    setup();
    submit();

    expectNoUpdateRequest();
  });

  it('sends only the changed field in the payload', () => {
    setup();
    setValue(usernameInput(), 'newname');
    submit();

    const req = expectUpdateRequest();
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ username: 'newname' });
    req.flush({ ...mockUser, username: 'newname' });
  });

  it('includes the password only when a new one is entered', () => {
    setup();
    setValue(passwordInput(), 'newPassword123');
    submit();

    const req = expectUpdateRequest();
    expect(req.request.body).toEqual({ password: 'newPassword123' });
    req.flush(mockUser);
  });

  it('shows a validation error and does not call the API for an invalid email', () => {
    setup();
    setValue(emailInput(), 'not-an-email');
    submit();

    expect(fixture.nativeElement.querySelector('.error')?.textContent).toContain(
      'valid email',
    );
    expectNoUpdateRequest();
  });

  it('disables the submit button while the request is in flight', () => {
    setup();
    setValue(usernameInput(), 'newname');
    submit();

    expect(submitButton().disabled).toBe(true);
    expectUpdateRequest().flush({ ...mockUser, username: 'newname' });
  });

  it('shows a success notification and re-enables the button on success', () => {
    setup();
    setValue(usernameInput(), 'newname');
    setValue(passwordInput(), 'newPassword123');
    submit();

    expectUpdateRequest().flush({ ...mockUser, username: 'newname' });
    fixture.detectChanges();

    expect(notificationService.message()).toContain('updated successfully');
    expect(submitButton().disabled).toBe(false);
  });

  it('clears the password field after a successful save', () => {
    setup();
    setValue(passwordInput(), 'newPassword123');
    submit();

    expectUpdateRequest().flush(mockUser);
    fixture.detectChanges();

    expect(passwordInput().value).toBe('');
  });

  it('applies field errors returned by the server to the matching control', () => {
    setup();
    setValue(usernameInput(), 'taken');
    submit();

    expectUpdateRequest().flush(
      { detail: 'Username already in use', errors: { username: 'Username already in use' } },
      { status: 409, statusText: 'Conflict' },
    );
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.error')?.textContent).toContain(
      'Username already in use',
    );
  });

  it('shows the server error message when the response has no field errors', () => {
    setup();
    setValue(usernameInput(), 'newname');
    submit();

    expectUpdateRequest().flush(
      { detail: 'Something went wrong', errors: {} },
      { status: 500, statusText: 'Server Error' },
    );
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.form-error')?.textContent).toContain(
      'Something went wrong',
    );
    expect(submitButton().disabled).toBe(false);
  });

  it('opens the avatar picker with all preset options when the avatar is clicked', () => {
    setup();
    expect(fixture.nativeElement.querySelector('.avatar-picker-backdrop')).toBeFalsy();

    avatarTrigger().click();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelectorAll('.avatar-option').length).toBe(16);
  });

  it('discards the selection and closes the picker on cancel', () => {
    setup();
    avatarTrigger().click();
    fixture.detectChanges();

    avatarOption('ember').click();
    fixture.detectChanges();
    pickerCancelButton().click();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.avatar-picker-backdrop')).toBeFalsy();
    expect(avatarImage().src).toContain('/avatars/nova.svg');
  });

  it('updates the displayed avatar and closes the picker on confirm, without calling the API', () => {
    setup();
    avatarTrigger().click();
    fixture.detectChanges();

    avatarOption('ember').click();
    fixture.detectChanges();
    pickerConfirmButton().click();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.avatar-picker-backdrop')).toBeFalsy();
    expect(avatarImage().src).toContain('/avatars/ember.svg');
    expectNoUpdateRequest();
  });

  it('includes the confirmed avatar in the payload when Save changes is clicked', () => {
    setup();
    avatarTrigger().click();
    fixture.detectChanges();
    avatarOption('ember').click();
    fixture.detectChanges();
    pickerConfirmButton().click();
    fixture.detectChanges();

    submit();

    const req = expectUpdateRequest();
    expect(req.request.body).toEqual({ avatarKey: 'ember' });
    req.flush({ ...mockUser, avatarKey: 'ember' });
  });

  it('opens the confirm modal when "Delete account" is clicked', () => {
    setup();
    expect(fixture.nativeElement.querySelector('.delete-account-backdrop')).toBeFalsy();

    deleteAccountButton().click();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.delete-account-backdrop')).toBeTruthy();
  });

  it('dismisses the confirm modal without calling the API on cancel', () => {
    setup();
    deleteAccountButton().click();
    fixture.detectChanges();

    deleteCancelButton().click();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.delete-account-backdrop')).toBeFalsy();
    httpTesting.expectNone((req) => req.url.endsWith('/auth/me'));
  });

  it('deletes the account and redirects to login on confirm', () => {
    setup();
    deleteAccountButton().click();
    fixture.detectChanges();

    deleteConfirmButton().click();
    fixture.detectChanges();

    expect(deleteConfirmButton().disabled).toBe(true);

    expectDeleteRequest().flush(null);
    fixture.detectChanges();

    expect(notificationService.message()).toContain('deleted successfully');
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('shows an error and re-enables the buttons when delete fails', () => {
    setup();
    deleteAccountButton().click();
    fixture.detectChanges();

    deleteConfirmButton().click();
    fixture.detectChanges();

    expectDeleteRequest().flush(
      { detail: 'Demo accounts cannot be deleted', errors: {} },
      { status: 403, statusText: 'Forbidden' },
    );
    fixture.detectChanges();

    expect(
      fixture.nativeElement.querySelector('.delete-account-panel .form-error')?.textContent,
    ).toContain('Demo accounts cannot be deleted');
    expect(deleteConfirmButton().disabled).toBe(false);
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
