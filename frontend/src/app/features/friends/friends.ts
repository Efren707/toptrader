import { Component, ElementRef, HostListener, inject, OnInit, signal, viewChild } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { catchError, debounceTime, distinctUntilChanged, of, switchMap, tap } from 'rxjs';
import {
  Friend,
  FriendService,
  IncomingFriendRequest,
  OutgoingFriendRequest,
  RelationshipStatus,
  Status,
  UserSearchResult,
} from '../../core/services/friend.service';
import { ApiError } from '../../core/interceptors/error.interceptor';
import { Card } from '../../shared/ui/card/card';
import { Button } from '../../shared/ui/button/button';

@Component({
  selector: 'app-friends',
  imports: [Card, Button, ReactiveFormsModule],
  templateUrl: './friends.html',
  styleUrl: './friends.css',
})
export class Friends implements OnInit {
  private readonly friendService = inject(FriendService);

  protected readonly incomingFriendRequests = signal<IncomingFriendRequest[]>([]);
  protected readonly outgoingFriendRequests = signal<OutgoingFriendRequest[]>([]);
  protected readonly friends = signal<Friend[]>([]);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly searchControl = new FormControl('', { nonNullable: true });
  protected readonly searchResults = signal<UserSearchResult[]>([]);
  protected readonly searchOpen = signal(false);
  protected readonly searchLoading = signal(false);
  protected readonly RelationshipStatus = RelationshipStatus;
  protected readonly searchWrap = viewChild<ElementRef<HTMLElement>>('searchWrap');

  protected readonly pendingRemoveFriend = signal<Friend | null>(null);

  ngOnInit(): void {
    this.fetchIncomingFriendRequests();
    this.fetchOutgoingFriendRequests();
    this.fetchFriendsRequests();

    this.searchControl.valueChanges
      .pipe(
        distinctUntilChanged(),
        debounceTime(100),
        tap((query) => {
          const hasQuery = query.trim().length > 0;
          this.searchOpen.set(hasQuery);
          this.searchLoading.set(hasQuery);
        }),
        switchMap((query) =>
          query.trim()
            ? this.friendService.search(query).pipe(
                catchError((error: ApiError) => {
                  this.errorMessage.set(error.detail);
                  this.searchLoading.set(false);
                  return of([]);
                }),
              )
            : of([]),
        ),
      )
      .subscribe({
        next: (results) => {
          this.searchResults.set(results);
          this.searchLoading.set(false);
        },
        error: (error: ApiError) => {
          this.errorMessage.set(error.detail);
          this.searchLoading.set(false);
        },
      });
  }

  @HostListener('document:click', ['$event'])
  protected onDocumentClick(event: MouseEvent): void {
    const wrapEl = this.searchWrap()?.nativeElement;
    if (wrapEl && !wrapEl.contains(event.target as Node)) {
      this.searchOpen.set(false);
    }
  }

  protected onSearchFocus(): void {
    if (this.searchControl.value.trim()) {
      this.searchOpen.set(true);
    }
  }

  protected onSearchClear(): void {
    this.searchControl.setValue('');
    this.searchResults.set([]);
    this.searchOpen.set(false);
  }

  protected fetchIncomingFriendRequests(): void {
    this.friendService.getIncomingFriendRequests().subscribe({
      next: (data) => this.incomingFriendRequests.set(data),
      error: (error: ApiError) => this.errorMessage.set(error.detail),
    });
  }

  protected fetchOutgoingFriendRequests(): void {
    this.friendService.getOutgoingFriendRequests().subscribe({
      next: (data) => this.outgoingFriendRequests.set(data),
      error: (error: ApiError) => this.errorMessage.set(error.detail),
    });
  }

  protected fetchFriendsRequests(): void {
    this.friendService.getFriends().subscribe({
      next: (data) => this.friends.set(data),
      error: (error: ApiError) => this.errorMessage.set(error.detail),
    });
  }

  private updateSearchResultStatus(userId: number, status: RelationshipStatus): void {
    this.searchResults.update((results) =>
      results.map((result) =>
        result.id === userId ? { ...result, relationshipStatus: status } : result,
      ),
    );
  }

  protected onSearchAddClick(result: UserSearchResult): void {
    this.friendService.sendFriendRequest(result.id).subscribe({
      next: (response) => {
        if (response.status === Status.ACCEPTED) {
          this.updateSearchResultStatus(result.id, RelationshipStatus.FRIENDS);
          this.fetchFriendsRequests();
          this.fetchIncomingFriendRequests();
        } else {
          this.updateSearchResultStatus(result.id, RelationshipStatus.OUTGOING_PENDING);
          this.fetchOutgoingFriendRequests();
        }
      },
      error: (error: ApiError) => this.errorMessage.set(error.detail),
    });
  }

  protected onSearchAcceptClick(result: UserSearchResult): void {
    const incoming = this.incomingFriendRequests().find(
      (request) => request.requester.id === result.id,
    );
    if (!incoming) {
      return;
    }
    this.friendService.acceptFriendRequest(incoming.id).subscribe({
      next: () => {
        this.updateSearchResultStatus(result.id, RelationshipStatus.FRIENDS);
        this.fetchIncomingFriendRequests();
        this.fetchFriendsRequests();
      },
      error: (error: ApiError) => this.errorMessage.set(error.detail),
    });
  }

  protected onSearchDeclineClick(result: UserSearchResult): void {
    const incoming = this.incomingFriendRequests().find(
      (request) => request.requester.id === result.id,
    );
    if (!incoming) {
      return;
    }
    this.friendService.declineFriendRequest(incoming.id).subscribe({
      next: () => {
        this.updateSearchResultStatus(result.id, RelationshipStatus.NONE);
        this.fetchIncomingFriendRequests();
      },
      error: (error: ApiError) => this.errorMessage.set(error.detail),
    });
  }

  protected onAcceptClick(request: IncomingFriendRequest): void {
    this.friendService.acceptFriendRequest(request.id).subscribe({
      next: () => {
        this.fetchIncomingFriendRequests();
        this.fetchFriendsRequests();
        this.updateSearchResultStatus(request.requester.id, RelationshipStatus.FRIENDS);
      },
      error: (error: ApiError) => this.errorMessage.set(error.detail),
    });
  }

  protected onDeclineClick(request: IncomingFriendRequest): void {
    this.friendService.declineFriendRequest(request.id).subscribe({
      next: () => {
        this.fetchIncomingFriendRequests();
        this.updateSearchResultStatus(request.requester.id, RelationshipStatus.NONE);
      },
      error: (error: ApiError) => this.errorMessage.set(error.detail),
    });
  }

  protected onCancelClick(request: OutgoingFriendRequest): void {
    this.friendService.cancelFriendRequest(request.id).subscribe({
      next: () => {
        this.fetchOutgoingFriendRequests();
        this.updateSearchResultStatus(request.addressee.id, RelationshipStatus.NONE);
      },
      error: (error: ApiError) => this.errorMessage.set(error.detail),
    });
  }

  protected onRemoveClick(friend: Friend): void {
    this.pendingRemoveFriend.set(friend);
  }

  protected onRemoveBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.cancelRemove();
    }
  }

  protected cancelRemove(): void {
    this.pendingRemoveFriend.set(null);
  }

  protected confirmRemove(): void {
    const friend = this.pendingRemoveFriend();
    if (!friend) {
      return;
    }
    this.friendService.removeFriend(friend.id).subscribe({
      next: () => {
        this.pendingRemoveFriend.set(null);
        this.fetchFriendsRequests();
        this.updateSearchResultStatus(friend.id, RelationshipStatus.NONE);
      },
      error: (error: ApiError) => {
        this.pendingRemoveFriend.set(null);
        this.errorMessage.set(error.detail);
      },
    });
  }

  protected avatarSrcFor(key: string | null): string {
    return `/avatars/${key || 'nova'}.svg`;
  }
}
