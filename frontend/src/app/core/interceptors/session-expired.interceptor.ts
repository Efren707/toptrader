import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * /auth/login and /auth/register legitimately return 401 for a wrong password or an
 * unauthenticated state; /auth/session's own 401 is already handled by AuthService.checkSession.
 * None of those should redirect away from where the user already is.
 */
const EXEMPT_PATHS = ['/auth/login', '/auth/register', '/auth/session'];

/**
 * Must run closer to the backend than errorInterceptor in app.config.ts's withInterceptors array,
 * so it sees the raw HttpErrorResponse before errorInterceptor unwraps it into an ApiError.
 */
export const sessionExpiredInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: unknown) => {
      if (
        error instanceof HttpErrorResponse &&
        error.status === 401 &&
        !EXEMPT_PATHS.some((path) => req.url.includes(path))
      ) {
        authService.currentUser.set(null);
        router.navigate(['/login']);
      }
      return throwError(() => error);
    }),
  );
};
