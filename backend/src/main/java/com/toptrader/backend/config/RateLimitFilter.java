package com.toptrader.backend.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.caffeine.Bucket4jCaffeine;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/** Enforces per-group rate limits (ADR 0034) via bucket4j, in-process, no Redis. */
public final class RateLimitFilter extends OncePerRequestFilter {

  private final ProxyManager<String> proxyManager =
      Bucket4jCaffeine.<String>builderFor(Caffeine.newBuilder())
          .expirationAfterWrite(
              ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(
                  Duration.ofHours(2)))
          .build();
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    RateLimitGroup group = matchingGroup(request);
    if (group == null) {
      chain.doFilter(request, response);
      return;
    }

    String extractedKey = group.extractKey(request);
    if (extractedKey == null) {
      chain.doFilter(request, response);
      return;
    }

    String key = group.name() + ":" + extractedKey;
    Bucket bucket =
        proxyManager.getProxy(
            key,
            () ->
                new BucketConfiguration(
                    List.of(
                        Bandwidth.builder()
                            .capacity(group.getCapacity())
                            .refillIntervally(group.getCapacity(), group.getRefillDuration())
                            .build())));
    ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

    if (probe.isConsumed()) {
      chain.doFilter(request, response);
      return;
    }

    long retryAfterSeconds = Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds() + 1;
    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded. Try again later.");
    objectMapper.writeValue(response.getWriter(), problemDetail);
  }

  private RateLimitGroup matchingGroup(HttpServletRequest request) {
    for (RateLimitGroup group : RateLimitGroup.values()) {
      if (group.matches(request)) {
        return group;
      }
    }
    return null;
  }
}
