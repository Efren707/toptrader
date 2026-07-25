import { HttpInterceptorFn, HttpXsrfTokenExtractor } from '@angular/common/http';
import { inject } from '@angular/core';

const UNSAFE_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE']);

/**
 * Angular's built-in XSRF interceptor skips any request whose URL starts with "http://" or
 * "https://", on the assumption that an absolute URL means a different, untrusted origin. This
 * app's API calls are always absolute (`environment.apiUrl`, a different port in dev and a
 * different subdomain in prod per ADR 0004), so that built-in interceptor never actually attaches
 * the header — this one reads the same XSRF-TOKEN cookie via Angular's own extractor and attaches
 * it regardless of URL shape.
 */
export const xsrfInterceptor: HttpInterceptorFn = (req, next) => {
  if (!UNSAFE_METHODS.has(req.method)) {
    return next(req);
  }

  const token = inject(HttpXsrfTokenExtractor).getToken();
  if (token === null) {
    return next(req);
  }

  return next(req.clone({ headers: req.headers.set('X-XSRF-TOKEN', token) }));
};
