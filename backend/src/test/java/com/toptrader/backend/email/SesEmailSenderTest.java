package com.toptrader.backend.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

@ExtendWith(MockitoExtension.class)
class SesEmailSenderTest {

  private static final String FROM_ADDRESS = "noreply@toptrader.dev";

  @Mock private SesClient sesClient;

  private SesEmailSender sender;

  @BeforeEach
  void setUp() {
    sender = new SesEmailSender();
    ReflectionTestUtils.setField(sender, "sesClient", sesClient);
    ReflectionTestUtils.setField(sender, "mailFromAddress", FROM_ADDRESS);
  }

  @Test
  void sendBuildsAndSendsRequestWithGivenContent() {
    ArgumentCaptor<SendEmailRequest> requestCaptor =
        ArgumentCaptor.forClass(SendEmailRequest.class);

    sender.send(
        "trader@example.com",
        "Reset your TopTrader password",
        "Use this link: http://example.com/reset");

    verify(sesClient).sendEmail(requestCaptor.capture());
    SendEmailRequest request = requestCaptor.getValue();
    assertThat(request.source()).isEqualTo(FROM_ADDRESS);
    assertThat(request.destination().toAddresses()).containsExactly("trader@example.com");
    assertThat(request.message().subject().data()).isEqualTo("Reset your TopTrader password");
    assertThat(request.message().body().text().data())
        .isEqualTo("Use this link: http://example.com/reset");
  }

  @Test
  void sendSwallowsSesFailureInsteadOfThrowing() {
    when(sesClient.sendEmail(any(SendEmailRequest.class)))
        .thenThrow(SdkException.builder().message("boom").build());

    assertThatCode(() -> sender.send("trader@example.com", "subject", "body"))
        .doesNotThrowAnyException();
  }
}
