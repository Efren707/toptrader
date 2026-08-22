import { HttpClient } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';


export enum RelationshipStatus { 
  NONE = 'NONE',
  OUTGOING_PENDING = 'OUTGOING_PENDING',
  INCOMING_PENDING = 'INCOMING_PENDING',
  FRIENDS = 'FRIENDS'
}

export enum Status { 
  PENDING = 'PENDING',
  ACCEPTED = 'ACCEPTED'
}

export interface UserSearchResult { 
  id: number;
  username: string;
  avatarKey: string | null;
  relationshipStatus: RelationshipStatus;
}

export interface FriendshipResponse { 
  id: number;
  requesterId: number;
  addresseeId: number;
  status: Status;
}

export interface FriendSummary { 
  id: number;
  username: string;
  avatarKey: string;
}

export interface IncomingFriendRequest { 
  id: number, 
  requester: FriendSummary; 
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class FriendService {
  private readonly http = inject(HttpClient);

  search(q: string): Observable<UserSearchResult[]> {
    return this.http.get<UserSearchResult[]>(`${environment.apiUrl}/users/search`, {
      params: { q }
    });
  }

  sendFriendRequest(addresseeId: number): Observable<FriendshipResponse> {
    return this.http.post<FriendshipResponse>(`${environment.apiUrl}/friends/requests`, { addresseeId })
  }
  
  cancelFriendRequest(id: number) {
    return this.http.delete<void>(`${environment.apiUrl}/friends/requests/${id}`)
  }

  acceptFriendRequest(id: number): Observable<FriendshipResponse> {
    return this.http.post<FriendshipResponse>(`${environment.apiUrl}/friends/requests/${id}/accept`, null)
  }

  declineFriendRequest(id: number): Observable<void> {
    return this.http.post<void>(`${environment.apiUrl}/friends/requests/${id}/decline`, null)
  }

  getIncomingFriendRequests(): Observable<IncomingFriendRequest[]> {
    return this.http.get<IncomingFriendRequest[]>(`${environment.apiUrl}/friends/requests/incoming`)
  }

}