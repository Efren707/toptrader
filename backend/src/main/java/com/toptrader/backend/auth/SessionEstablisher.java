package com.toptrader.backend.auth;

import com.toptrader.backend.user.User;
import com.toptrader.backend.user.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
public class SessionEstablisher {
  private final SecurityContextRepository securityContextRepository =
      new HttpSessionSecurityContextRepository();

  public void establishSession(
      User user, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
    UserPrincipal principal = new UserPrincipal(user);
    Authentication authentication =
        UsernamePasswordAuthenticationToken.authenticated(
            principal, principal.getPassword(), principal.getAuthorities());

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    securityContextRepository.saveContext(context, httpRequest, httpResponse);
  }
}
