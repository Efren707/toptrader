import { HttpInterceptorFn } from '@angular/common/http';

/** Ensures the SESSION cookie (ADR 0004) rides on every request. */
export const credentialsInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req.clone({ withCredentials: true }));
};
