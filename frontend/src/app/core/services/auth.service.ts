import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { catchError, Observable, of, tap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiError } from '../interceptors/error.interceptor';

export interface UserSummary {
  id: number;
  email: string;
  username: string;
  cashBalance: number;
}

export interface RegisterRequest {
  email: string;
  username: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  rawToken: string;
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

  login(request: LoginRequest): Observable<UserSummary> {
    return this.http
      .post<UserSummary>(`${environment.apiUrl}/auth/login`, request)
      .pipe(tap((user) => this.currentUser.set(user)));
  }

  logout(): Observable<void> {
    return this.http
      .post<void>(`${environment.apiUrl}/auth/logout`, null)
      .pipe(tap(() => this.currentUser.set(null)));
  }

  checkSession(): Observable<UserSummary | null> {
    return this.http.get<UserSummary>(`${environment.apiUrl}/auth/session`).pipe(
      tap((user) => this.currentUser.set(user)),
      catchError((error: ApiError) => {
        if (error.status === 401) {
          return of(null);
        }
        return throwError(() => error);
      }),
    );
  }

  forgotPassword(request: ForgotPasswordRequest): Observable<void> {
    return this.http.post<void>(`${environment.apiUrl}/auth/forgot-password`, request)
  }

  resetPassword(request: ResetPasswordRequest): Observable<void> {
    return this.http.post<void>(`${environment.apiUrl}/auth/reset-password`, request)
  }
}
