package com.toptrader.backend.config;

import com.toptrader.backend.user.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/** Rate-limit groups, matchers, key strategy, and thresholds per ADR 0034. */
public enum RateLimitGroup {
  REGISTER(
      List.of(PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/auth/register")),
      RateLimitGroup::clientIp,
      5,
      Duration.ofHours(1)),
  DEMO_LOGIN(
      List.of(PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/auth/demo-login")),
      RateLimitGroup::clientIp,
      5,
      Duration.ofHours(1)),
  FORGOT_PASSWORD(
      List.of(
          PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/auth/forgot-password"),
          PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/auth/reset-password")),
      RateLimitGroup::clientIp,
      5,
      Duration.ofHours(1)),
  EMAIL_VERIFICATION(
      List.of(
          PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/auth/verify-email"),
          PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/auth/resend-verification")),
      RateLimitGroup::clientIp,
      5,
      Duration.ofHours(1)),
  QUOTE(
      List.of(PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/quotes/{ticker}")),
      RateLimitGroup::authenticatedUserId,
      20,
      Duration.ofMinutes(1)),
  TRADE(
      List.of(
          PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/trades/buy"),
          PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/trades/sell")),
      RateLimitGroup::authenticatedUserId,
      10,
      Duration.ofMinutes(1));

  private final List<PathPatternRequestMatcher> matchers;
  private final Function<HttpServletRequest, String> keyExtractor;
  private final long capacity;
  private final Duration refillDuration;

  RateLimitGroup(
      List<PathPatternRequestMatcher> matchers,
      Function<HttpServletRequest, String> keyExtractor,
      long capacity,
      Duration refillDuration) {
    this.matchers = matchers;
    this.keyExtractor = keyExtractor;
    this.capacity = capacity;
    this.refillDuration = refillDuration;
  }

  public boolean matches(HttpServletRequest request) {
    return matchers.stream().anyMatch(matcher -> matcher.matches(request));
  }

  public String extractKey(HttpServletRequest request) {
    return keyExtractor.apply(request);
  }

  public long getCapacity() {
    return capacity;
  }

  public Duration getRefillDuration() {
    return refillDuration;
  }

  private static String clientIp(HttpServletRequest request) {
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor == null || forwardedFor.isBlank()) {
      return request.getRemoteAddr();
    }
    return forwardedFor.split(",")[0].trim();
  }

  // Only safe to call once Spring Security's authentication filters have run and populated the
  // SecurityContext - this filter must be positioned after them in the chain (see SecurityConfig).
  // Returns null for anonymous/non-UserPrincipal requests (this filter runs before the
  // authorization check, so an unauthenticated request reaching here is expected, not an error -
  // it gets rejected downstream instead of rate-limited).
  private static String authenticatedUserId(HttpServletRequest request) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
      return null;
    }
    return String.valueOf(principal.getUser().getId());
  }
}
