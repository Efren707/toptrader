package com.toptrader.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Forces the deferred CSRF token to resolve on every request, which is what actually causes {@link
 * CookieCsrfTokenRepository} to write the {@code XSRF-TOKEN} cookie. Spring Security 5.8+ wraps the
 * token in a supplier (a BREACH-attack mitigation) that only resolves when something reads it —
 * with no server-rendered view to do that read implicitly, the cookie would otherwise never be set,
 * and every CSRF-protected request from the Angular SPA would be rejected. See ADR 0022.
 */
public final class CsrfCookieFilter extends OncePerRequestFilter {
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
    csrfToken.getToken(); // return value unused — call is only for the side effect above
    chain.doFilter(request, response);
  }
}
