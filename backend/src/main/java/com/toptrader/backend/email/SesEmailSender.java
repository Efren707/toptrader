package com.toptrader.backend.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

@Profile("prod")
@Service
public class SesEmailSender implements EmailSender {

  private final SesClient sesClient = SesClient.builder().region(Region.US_EAST_2).build();
  private static final Logger log = LoggerFactory.getLogger(SesEmailSender.class);

  @Value("${toptrader.mail-from-address}")
  private String mailFromAddress;

  @Override
  public void send(String to, String subject, String body) {
    Destination destination = Destination.builder().toAddresses(to).build();

    Content subjectContent = Content.builder().data(subject).charset("UTF-8").build();

    Content textBodyContent = Content.builder().data(body).charset("UTF-8").build();

    Body emailBody = Body.builder().text(textBodyContent).build();

    Message emailMessage = Message.builder().subject(subjectContent).body(emailBody).build();

    SendEmailRequest emailRequest =
        SendEmailRequest.builder()
            .source(mailFromAddress)
            .destination(destination)
            .message(emailMessage)
            .build();

    try {
      sesClient.sendEmail(emailRequest);
    } catch (SdkException e) {
      log.atWarn().addKeyValue("recipient", to).setCause(e).log("Failed to send email via SES");
    }
  }
}
