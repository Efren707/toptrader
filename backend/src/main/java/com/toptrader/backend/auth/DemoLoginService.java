package com.toptrader.backend.auth;

import com.toptrader.backend.user.User;
import com.toptrader.backend.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DemoLoginService {

  private static final String DEMO_EMAIL = "demo@toptrader.dev";
  private static final Logger log = LoggerFactory.getLogger(DemoLoginService.class);

  private final UserRepository userRepository;
  private final SessionEstablisher sessionEstablisher;

  public DemoLoginService(UserRepository userRepository, SessionEstablisher sessionEstablisher) {
    this.userRepository = userRepository;
    this.sessionEstablisher = sessionEstablisher;
  }

  public UserSummary demoLogin(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
    Optional<User> user = userRepository.findByEmail(DEMO_EMAIL);

    if (user.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Demo user not found");
    }

    User demoUser = user.get();
    this.sessionEstablisher.establishSession(demoUser, httpRequest, httpResponse);
    log.atInfo().log("Demo login succeeded");
    return UserSummary.from(demoUser);
  }
}
