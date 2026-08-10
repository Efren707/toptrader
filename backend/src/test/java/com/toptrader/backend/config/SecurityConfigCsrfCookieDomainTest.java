package com.toptrader.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Separate Spring context (needs its own {@code toptrader.csrf-cookie-domain} property) verifying
 * that value actually reaches the XSRF-TOKEN cookie's Domain attribute — the setting that lets the
 * cookie be read across the app./api. subdomain split in prod.
 */
@SpringBootTest(properties = "toptrader.csrf-cookie-domain=.toptrader.dev")
@AutoConfigureMockMvc
class SecurityConfigCsrfCookieDomainTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void csrfCookie_hasConfiguredDomainAttribute() throws Exception {
    MvcResult result =
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk()).andReturn();

    Cookie csrfCookie = result.getResponse().getCookie("XSRF-TOKEN");

    assertThat(csrfCookie).isNotNull();
    assertThat(csrfCookie.getDomain()).isEqualTo(".toptrader.dev");
  }
}
