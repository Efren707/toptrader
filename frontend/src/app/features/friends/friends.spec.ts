import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { errorInterceptor } from '../../core/interceptors/error.interceptor';
import {
  Friend,
  IncomingFriendRequest,
  OutgoingFriendRequest,
  RelationshipStatus,
  Status,
  UserSearchResult,
} from '../../core/services/friend.service';
import { Friends } from './friends';

describe('Friends', () => {
  let component: Friends;
  let fixture: ComponentFixture<Friends>;
  let httpTesting: HttpTestingController;

  const mockFriends: Friend[] = [
    { id: 2, username: 'trader2', avatarKey: 'nova', friendsSince: '2026-01-01T00:00:00' },
  ];

  const mockIncoming: IncomingFriendRequest[] = [
    { id: 10, requester: { id: 3, username: 'trader3', avatarKey: 'orbit' }, createdAt: '2026-08-19T00:00:00' },
  ];

  const mockOutgoing: OutgoingFriendRequest[] = [
    { id: 20, addressee: { id: 4, username: 'trader4', avatarKey: 'ember' }, createdAt: '2026-08-20T00:00:00' },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Friends],
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Friends);
    component = fixture.componentInstance;
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  function flushInitial(
    friends: Friend[] = [],
    incoming: IncomingFriendRequest[] = [],
    outgoing: OutgoingFriendRequest[] = [],
  ): void {
    fixture.detectChanges();
    httpTesting.expectOne((r) => r.url.endsWith('/friends/requests/incoming')).flush(incoming);
    httpTesting.expectOne((r) => r.url.endsWith('/friends/requests/outgoing')).flush(outgoing);
    httpTesting.expectOne((r) => r.url.endsWith('/friends')).flush(friends);
    fixture.detectChanges();
  }

  function searchInput(): HTMLInputElement {
    return fixture.nativeElement.querySelector('.search-input');
  }

  function clearButton(): HTMLButtonElement {
    return fixture.nativeElement.querySelector('.field-icon-clear');
  }

  function typeSearch(query: string): void {
    const input = searchInput();
    input.value = query;
    input.dispatchEvent(new Event('input'));
  }

  function friendsCard(): HTMLElement {
    return fixture.nativeElement.querySelector('.friends-list-card');
  }

  function friendRows(): HTMLElement[] {
    return Array.from(friendsCard().querySelectorAll('.friends-scroll .entity-row'));
  }

  function requestsCards(): HTMLElement[] {
    return Array.from(fixture.nativeElement.querySelectorAll('.requests-card'));
  }

  function incomingRows(): HTMLElement[] {
    return Array.from(requestsCards()[0].querySelectorAll('.entity-row'));
  }

  function outgoingRows(): HTMLElement[] {
    return Array.from(requestsCards()[1].querySelectorAll('.entity-row'));
  }

  function searchResultRows(): HTMLElement[] {
    return Array.from(fixture.nativeElement.querySelectorAll('.result-row'));
  }

  function removeCancelButton(): HTMLButtonElement {
    return fixture.nativeElement.querySelectorAll('.remove-friend-actions button')[0];
  }

  function removeConfirmButton(): HTMLButtonElement {
    return fixture.nativeElement.querySelectorAll('.remove-friend-actions button')[1];
  }

  it('should create', () => {
    flushInitial();
    expect(component).toBeTruthy();
  });

  describe('lists', () => {
    it('shows empty states when there is no data', () => {
      flushInitial([], [], []);

      expect(friendsCard().querySelector('.status')?.textContent).toContain('No friends yet');
      expect(requestsCards()[0].querySelector('.status')?.textContent).toContain(
        'No incoming requests',
      );
      expect(requestsCards()[1].querySelector('.status')?.textContent).toContain(
        'No pending outgoing requests',
      );
    });

    it('renders a friend row with avatar and username', () => {
      flushInitial(mockFriends);

      expect(friendRows().length).toBe(1);
      expect(friendRows()[0].querySelector('.row-username')?.textContent).toContain('trader2');
      expect(friendRows()[0].querySelector('.row-avatar')?.getAttribute('src')).toContain(
        '/avatars/nova.svg',
      );
    });

    it('renders incoming and outgoing request rows', () => {
      flushInitial([], mockIncoming, mockOutgoing);

      expect(incomingRows()[0].querySelector('.row-username')?.textContent).toContain('trader3');
      expect(outgoingRows()[0].querySelector('.row-username')?.textContent).toContain('trader4');
    });
  });

  describe('removing a friend', () => {
    it('opens the confirm modal with the friend\'s username when the remove icon is clicked', () => {
      flushInitial(mockFriends);

      friendRows()[0].querySelector<HTMLButtonElement>('.icon-btn-remove')!.click();
      fixture.detectChanges();

      expect(fixture.nativeElement.querySelector('.remove-friend-backdrop')).toBeTruthy();
      expect(fixture.nativeElement.querySelector('.remove-friend-description')?.textContent).toContain(
        'trader2',
      );
    });

    it('dismisses the modal without calling the API on cancel', () => {
      flushInitial(mockFriends);

      friendRows()[0].querySelector<HTMLButtonElement>('.icon-btn-remove')!.click();
      fixture.detectChanges();
      removeCancelButton().click();
      fixture.detectChanges();

      expect(fixture.nativeElement.querySelector('.remove-friend-backdrop')).toBeFalsy();
      httpTesting.expectNone((r) => r.url.endsWith('/friends/2'));
    });

    it('dismisses the modal on Escape', () => {
      flushInitial(mockFriends);

      friendRows()[0].querySelector<HTMLButtonElement>('.icon-btn-remove')!.click();
      fixture.detectChanges();
      fixture.nativeElement
        .querySelector('.remove-friend-backdrop')
        .dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
      fixture.detectChanges();

      expect(fixture.nativeElement.querySelector('.remove-friend-backdrop')).toBeFalsy();
    });

    it('removes the friend and refetches the friends list on confirm', () => {
      flushInitial(mockFriends);

      friendRows()[0].querySelector<HTMLButtonElement>('.icon-btn-remove')!.click();
      fixture.detectChanges();
      removeConfirmButton().click();

      httpTesting.expectOne((r) => r.url.endsWith('/friends/2') && r.method === 'DELETE').flush(null);
      httpTesting.expectOne((r) => r.url.endsWith('/friends') && r.method === 'GET').flush([]);
      fixture.detectChanges();

      expect(fixture.nativeElement.querySelector('.remove-friend-backdrop')).toBeFalsy();
      expect(friendRows().length).toBe(0);
    });
  });

  describe('incoming/outgoing request actions', () => {
    it('accepts an incoming request and refetches incoming + friends', () => {
      flushInitial([], mockIncoming);

      incomingRows()[0].querySelector<HTMLButtonElement>('.icon-btn-accept')!.click();

      httpTesting
        .expectOne((r) => r.url.endsWith('/friends/requests/10/accept'))
        .flush({ id: 10, requesterId: 3, addresseeId: 1, status: Status.ACCEPTED });
      httpTesting.expectOne((r) => r.url.endsWith('/friends/requests/incoming')).flush([]);
      httpTesting
        .expectOne((r) => r.url.endsWith('/friends') && r.method === 'GET')
        .flush([{ id: 3, username: 'trader3', avatarKey: 'orbit', friendsSince: '2026-08-22T00:00:00' }]);
      fixture.detectChanges();

      expect(incomingRows().length).toBe(0);
    });

    it('declines an incoming request and refetches incoming', () => {
      flushInitial([], mockIncoming);

      incomingRows()[0].querySelector<HTMLButtonElement>('.icon-btn-decline')!.click();

      httpTesting.expectOne((r) => r.url.endsWith('/friends/requests/10/decline')).flush(null);
      httpTesting.expectOne((r) => r.url.endsWith('/friends/requests/incoming')).flush([]);
      fixture.detectChanges();

      expect(incomingRows().length).toBe(0);
    });

    it('cancels an outgoing request and refetches outgoing', () => {
      flushInitial([], [], mockOutgoing);

      outgoingRows()[0].querySelector<HTMLButtonElement>('.icon-btn-cancel')!.click();

      httpTesting
        .expectOne((r) => r.url.endsWith('/friends/requests/20') && r.method === 'DELETE')
        .flush(null);
      httpTesting.expectOne((r) => r.url.endsWith('/friends/requests/outgoing')).flush([]);
      fixture.detectChanges();

      expect(outgoingRows().length).toBe(0);
    });
  });

  describe('search', () => {
    beforeEach(() => {
      vi.useFakeTimers();
    });

    afterEach(() => {
      vi.useRealTimers();
    });

    it('debounces the search request by 300ms', () => {
      flushInitial();

      typeSearch('trader');
      httpTesting.expectNone((r) => r.url.includes('/users/search'));

      vi.advanceTimersByTime(300);

      const req = httpTesting.expectOne((r) => r.url.endsWith('/users/search'));
      expect(req.request.params.get('q')).toBe('trader');
      req.flush([]);
    });

    it('shows "No users found" when the search returns no results', () => {
      flushInitial();

      typeSearch('nobody');
      vi.advanceTimersByTime(300);
      httpTesting.expectOne((r) => r.url.endsWith('/users/search')).flush([]);
      fixture.detectChanges();

      expect(fixture.nativeElement.querySelector('.search-empty')?.textContent).toContain(
        'No users found',
      );
    });

    it('closes the dropdown on outside click and reopens it on focus with an existing query', () => {
      flushInitial();

      typeSearch('trader');
      vi.advanceTimersByTime(300);
      httpTesting
        .expectOne((r) => r.url.endsWith('/users/search'))
        .flush([{ id: 5, username: 'trader5', avatarKey: null, relationshipStatus: RelationshipStatus.NONE }]);
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('.search-results')).toBeTruthy();

      document.body.click();
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('.search-results')).toBeFalsy();

      searchInput().dispatchEvent(new Event('focus'));
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('.search-results')).toBeTruthy();
    });

    it('clears the query, results, and closes the dropdown when the clear button is clicked', () => {
      flushInitial();

      typeSearch('trader');
      vi.advanceTimersByTime(300);
      httpTesting
        .expectOne((r) => r.url.endsWith('/users/search'))
        .flush([{ id: 5, username: 'trader5', avatarKey: null, relationshipStatus: RelationshipStatus.NONE }]);
      fixture.detectChanges();

      clearButton().click();
      fixture.detectChanges();

      expect(searchInput().value).toBe('');
      expect(fixture.nativeElement.querySelector('.search-results')).toBeFalsy();
    });

    function flushSearch(results: UserSearchResult[]): void {
      typeSearch('trader');
      vi.advanceTimersByTime(300);
      httpTesting.expectOne((r) => r.url.endsWith('/users/search')).flush(results);
      fixture.detectChanges();
    }

    it('sends a request and flips the row to "Requested" when adding a new user', () => {
      flushInitial();
      flushSearch([{ id: 5, username: 'trader5', avatarKey: null, relationshipStatus: RelationshipStatus.NONE }]);

      searchResultRows()[0].querySelector<HTMLButtonElement>('.add-btn')!.click();

      const req = httpTesting.expectOne((r) => r.url.endsWith('/friends/requests') && r.method === 'POST');
      expect(req.request.body).toEqual({ addresseeId: 5 });
      req.flush({ id: 30, requesterId: 1, addresseeId: 5, status: Status.PENDING }, { status: 201, statusText: 'Created' });
      httpTesting.expectOne((r) => r.url.endsWith('/friends/requests/outgoing')).flush([]);
      fixture.detectChanges();

      expect(searchResultRows()[0].querySelector('.status-label')?.textContent).toContain('Requested');
    });

    it('flips the row straight to "Friends" on a crossed-request auto-accept', () => {
      flushInitial();
      flushSearch([{ id: 6, username: 'trader6', avatarKey: null, relationshipStatus: RelationshipStatus.NONE }]);

      searchResultRows()[0].querySelector<HTMLButtonElement>('.add-btn')!.click();

      httpTesting
        .expectOne((r) => r.url.endsWith('/friends/requests') && r.method === 'POST')
        .flush({ id: 31, requesterId: 1, addresseeId: 6, status: Status.ACCEPTED });
      httpTesting.expectOne((r) => r.url.endsWith('/friends') && r.method === 'GET').flush([]);
      httpTesting.expectOne((r) => r.url.endsWith('/friends/requests/incoming')).flush([]);
      fixture.detectChanges();

      expect(searchResultRows()[0].querySelector('.status-label-friends')?.textContent).toContain('Friends');
    });

    it('shows a plain "Requested" label with no action buttons for outgoing-pending rows', () => {
      flushInitial();
      flushSearch([
        { id: 4, username: 'trader4', avatarKey: 'ember', relationshipStatus: RelationshipStatus.OUTGOING_PENDING },
      ]);

      expect(searchResultRows()[0].querySelector('.status-label')?.textContent).toContain('Requested');
      expect(searchResultRows()[0].querySelector('.icon-btn')).toBeFalsy();
      expect(searchResultRows()[0].querySelector('.add-btn')).toBeFalsy();
    });

    it('accepts an incoming-pending row by looking up its friendship id from the incoming list', () => {
      flushInitial([], mockIncoming);
      flushSearch([
        { id: 3, username: 'trader3', avatarKey: 'orbit', relationshipStatus: RelationshipStatus.INCOMING_PENDING },
      ]);

      searchResultRows()[0].querySelector<HTMLButtonElement>('.icon-btn-accept')!.click();

      httpTesting
        .expectOne((r) => r.url.endsWith('/friends/requests/10/accept'))
        .flush({ id: 10, requesterId: 3, addresseeId: 1, status: Status.ACCEPTED });
      httpTesting.expectOne((r) => r.url.endsWith('/friends/requests/incoming')).flush([]);
      httpTesting.expectOne((r) => r.url.endsWith('/friends') && r.method === 'GET').flush([]);
      fixture.detectChanges();

      expect(searchResultRows()[0].querySelector('.status-label-friends')?.textContent).toContain('Friends');
    });

    it('shows a plain "Friends" label with no action buttons for already-friended rows', () => {
      flushInitial();
      flushSearch([
        { id: 2, username: 'trader2', avatarKey: 'nova', relationshipStatus: RelationshipStatus.FRIENDS },
      ]);

      expect(searchResultRows()[0].querySelector('.status-label-friends')?.textContent).toContain('Friends');
      expect(searchResultRows()[0].querySelector('.icon-btn')).toBeFalsy();
      expect(searchResultRows()[0].querySelector('.add-btn')).toBeFalsy();
    });
  });
});
