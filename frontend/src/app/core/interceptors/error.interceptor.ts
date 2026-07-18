import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';

/** Unwraps the RFC 7807 ProblemDetail body (ADR 0012) into a shape components can render. */
export interface ApiError {
  status: number;
  detail: string;
  fieldErrors: Record<string, string>;
}

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse) {
        const body = error.error ?? {};
        const apiError: ApiError = {
          status: error.status,
          detail: body.detail ?? 'Something went wrong. Please try again.',
          fieldErrors: body.errors ?? {},
        };
        return throwError(() => apiError);
      }
      return throwError(() => error);
    }),
  );
};
