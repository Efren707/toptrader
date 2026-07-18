import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface UserSummary {
  id: number;
  email: string;
  username: string;
}

export interface RegisterRequest {
  email: string;
  username: string;
  password: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  readonly currentUser = signal<UserSummary | null>(null);

  register(request: RegisterRequest): Observable<UserSummary> {
    return this.http
      .post<UserSummary>(`${environment.apiUrl}/auth/register`, request)
      .pipe(tap((user) => this.currentUser.set(user)));
  }
}
