package com.toptrader.backend.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile("!prod")
@Service
public class LogEmailSender implements EmailSender {

  private static final Logger log = LoggerFactory.getLogger(LogEmailSender.class);

  @Override
  public void send(String to, String subject, String body) {
    log.info("Sending email to {} with subject {} body {}.", to, subject, body);
  }
}
