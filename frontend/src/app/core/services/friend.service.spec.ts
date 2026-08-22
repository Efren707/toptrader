import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { FriendService, FriendshipResponse, IncomingFriendRequest, RelationshipStatus, Status, UserSearchResult } from './friend.service';

describe('FriendService', () => {
  let service: FriendService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(FriendService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('sends q as a query param to /users/search', () => {
    const results: UserSearchResult[] = [
      { id: 2, username: 'trader2', avatarKey: 'nova', relationshipStatus: RelationshipStatus.NONE },
    ];

    service.search('trader').subscribe();

    const req = httpTesting.expectOne((req) => req.url.endsWith('/users/search'));
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('q')).toBe('trader');
    req.flush(results);
  });

  it('posts addresseeId to /friends/requests', () => {
    const response: FriendshipResponse = {
      id: 1,
      requesterId: 1,
      addresseeId: 2,
      status: Status.PENDING,
    };

    service.sendFriendRequest(2).subscribe();

    const req = httpTesting.expectOne((req) => req.url.endsWith('/friends/requests'));
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ addresseeId: 2 });
    req.flush(response);
  });

  it('deletes /friends/requests/{id} to cancel a request', () => {
    service.cancelFriendRequest(1).subscribe();

    const req = httpTesting.expectOne((req) => req.url.endsWith('/friends/requests/1'));
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('posts to /friends/requests/{id}/accept with no body', () => {
    const response: FriendshipResponse = {
      id: 1,
      requesterId: 2,
      addresseeId: 1,
      status: Status.ACCEPTED,
    };

    service.acceptFriendRequest(1).subscribe();

    const req = httpTesting.expectOne((req) => req.url.endsWith('/friends/requests/1/accept'));
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toBeNull();
    req.flush(response);
  });

  it('posts to /friends/requests/{id}/decline with no body', () => {
    service.declineFriendRequest(1).subscribe();

    const req = httpTesting.expectOne((req) => req.url.endsWith('/friends/requests/1/decline'));
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toBeNull();
    req.flush(null);
  });

  it('gets /friends/requests/incoming', () => {
    const incoming: IncomingFriendRequest[] = [
      {
        id: 1,
        requester: { id: 2, username: 'trader2', avatarKey: 'nova' },
        createdAt: '2026-08-21T00:00:00',
      },
    ];

    service.getIncomingFriendRequests().subscribe();

    const req = httpTesting.expectOne((req) => req.url.endsWith('/friends/requests/incoming'));
    expect(req.request.method).toBe('GET');
    req.flush(incoming);
  });
});
