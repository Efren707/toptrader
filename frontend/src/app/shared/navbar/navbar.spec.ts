import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { AuthService, UserSummary } from '../../core/services/auth.service';
import { IncomingFriendRequest } from '../../core/services/friend.service';
import { Navbar } from './navbar';

describe('Navbar', () => {
  let component: Navbar;
  let fixture: ComponentFixture<Navbar>;
  let httpTesting: HttpTestingController;
  let router: Router;
  let authService: AuthService;

  const mockUser: UserSummary = {
    id: 1,
    email: 'trader@example.com',
    username: 'trader',
    cashBalance: 500,
    isDemo: false,
    avatarKey: 'comet',
  };

  const mockIncomingRequests: IncomingFriendRequest[] = [
    { id: 1, requester: { id: 2, username: 'trader2', avatarKey: 'nova' }, createdAt: '2026-08-18T00:00:00' },
    { id: 2, requester: { id: 3, username: 'trader3', avatarKey: 'orbit' }, createdAt: '2026-08-19T00:00:00' },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Navbar],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(Navbar);
    component = fixture.componentInstance;
    httpTesting = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    authService = TestBed.inject(AuthService);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    authService.currentUser.set(mockUser);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  function flushIncomingRequests(requests: IncomingFriendRequest[] = []): void {
    fixture.detectChanges();
    httpTesting.expectOne((r) => r.url.endsWith('/friends/requests/incoming')).flush(requests);
    fixture.detectChanges();
  }

  function accountButton(): HTMLButtonElement {
    return fixture.nativeElement.querySelector('.account-button');
  }

  function accountAvatar(): HTMLImageElement {
    return fixture.nativeElement.querySelector('.account-button img');
  }

  function accountUsername(): string {
    return fixture.nativeElement.querySelector('.account-button span')?.textContent?.trim() ?? '';
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
    flushIncomingRequests();
    expect(component).toBeTruthy();
  });

  it('shows the current user\'s avatar and username in the account trigger', () => {
    flushIncomingRequests();

    expect(accountAvatar().src).toContain('/avatars/comet.svg');
    expect(accountUsername()).toBe('trader');
  });

  it('falls back to the default avatar when avatarKey is null', () => {
    flushIncomingRequests();

    authService.currentUser.set({ ...mockUser, avatarKey: null });
    fixture.detectChanges();

    expect(accountAvatar().src).toContain('/avatars/nova.svg');
  });

  it('updates the displayed username when the current user changes, without recreating the component', () => {
    flushIncomingRequests();
    expect(accountUsername()).toBe('trader');

    authService.currentUser.set({ ...mockUser, username: 'newname' });
    fixture.detectChanges();

    expect(accountUsername()).toBe('newname');
  });

  it('navigates to profile and closes the dropdown when "Profile" is clicked', () => {
    flushIncomingRequests();

    accountButton().click();
    fixture.detectChanges();

    menuButtonByText('Profile').click();
    fixture.detectChanges();

    expect(router.navigate).toHaveBeenCalledWith(['/profile']);
    expect(fixture.nativeElement.querySelector('.account-menu')).toBeFalsy();
  });

  it('navigates to friends and closes the dropdown when "Friends" is clicked', () => {
    flushIncomingRequests();

    accountButton().click();
    fixture.detectChanges();

    menuButtonByText('Friends').click();
    fixture.detectChanges();

    expect(router.navigate).toHaveBeenCalledWith(['/friends']);
    expect(fixture.nativeElement.querySelector('.account-menu')).toBeFalsy();
  });

  it('opens the account dropdown on click and closes it again on a second click', () => {
    flushIncomingRequests();
    expect(fixture.nativeElement.querySelector('.account-menu')).toBeFalsy();

    accountButton().click();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.account-menu')).toBeTruthy();

    accountButton().click();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.account-menu')).toBeFalsy();
  });

  it('shows the matching result when a ticker search succeeds', () => {
    flushIncomingRequests();
    searchAndSubmit('AAPL');

    const req = httpTesting.expectOne((r) => r.url.endsWith('/quotes/AAPL'));
    req.flush({ ticker: 'AAPL', companyName: 'Apple Inc.', price: 210.5, asOf: '2026-07-28T15:00:00Z' });
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.result-ticker')?.textContent).toContain('AAPL');
  });

  it('shows "No stocks found" when the ticker search 404s', () => {
    flushIncomingRequests();
    searchAndSubmit('ZZZZ');

    const req = httpTesting.expectOne((r) => r.url.endsWith('/quotes/ZZZZ'));
    req.flush({ detail: 'Not found', errors: {} }, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.search-empty')?.textContent).toContain('No stocks found');
  });

  it('navigates to the stock details page when a search result is clicked', () => {
    flushIncomingRequests();
    searchAndSubmit('AAPL');
    httpTesting
      .expectOne((r) => r.url.endsWith('/quotes/AAPL'))
      .flush({ ticker: 'AAPL', companyName: 'Apple Inc.', price: 210.5, asOf: '2026-07-28T15:00:00Z' });
    fixture.detectChanges();

    fixture.nativeElement.querySelector('.result-item').click();

    expect(router.navigate).toHaveBeenCalledWith(['/stocks/AAPL']);
  });

  it('navigates to transactions and closes the dropdown when "Transaction history" is clicked', () => {
    flushIncomingRequests();

    accountButton().click();
    fixture.detectChanges();

    menuButtonByText('Transaction history').click();
    fixture.detectChanges();

    expect(router.navigate).toHaveBeenCalledWith(['/transactions']);
    expect(fixture.nativeElement.querySelector('.account-menu')).toBeFalsy();
  });

  it('navigates to performance when "Performance" is clicked', () => {
    flushIncomingRequests();

    accountButton().click();
    fixture.detectChanges();

    menuButtonByText('Performance').click();
    fixture.detectChanges();

    expect(router.navigate).toHaveBeenCalledWith(['/performance']);
  });

  it('logs out and navigates to login when "Logout" succeeds', () => {
    flushIncomingRequests();

    accountButton().click();
    fixture.detectChanges();

    menuButtonByText('Logout').click();

    httpTesting.expectOne((r) => r.url.endsWith('/auth/logout')).flush(null);
    fixture.detectChanges();

    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('shows an orange dot on the account button when there are incoming requests and the dropdown is closed', () => {
    flushIncomingRequests(mockIncomingRequests);

    expect(accountButton().querySelector('.account-badge-dot')).toBeTruthy();
  });

  it('hides the account-button dot when there are no incoming requests', () => {
    flushIncomingRequests([]);

    expect(accountButton().querySelector('.account-badge-dot')).toBeFalsy();
  });

  it('hides the account-button dot once the dropdown is open', () => {
    flushIncomingRequests(mockIncomingRequests);
    expect(accountButton().querySelector('.account-badge-dot')).toBeTruthy();

    accountButton().click();
    fixture.detectChanges();

    expect(accountButton().querySelector('.account-badge-dot')).toBeFalsy();
  });

  it('shows the incoming-request count next to the Friends item in the account menu', () => {
    flushIncomingRequests(mockIncomingRequests);

    accountButton().click();
    fixture.detectChanges();

    const badge = fixture.nativeElement.querySelector('.menu-item-friends .menu-badge');
    expect(badge?.textContent?.trim()).toBe('2');
  });

  it('hides the Friends count badge when there are no incoming requests', () => {
    flushIncomingRequests([]);

    accountButton().click();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.menu-item-friends .menu-badge')).toBeFalsy();
  });
});
