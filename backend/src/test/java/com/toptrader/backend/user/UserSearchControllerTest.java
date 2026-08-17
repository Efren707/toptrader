package com.toptrader.backend.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.toptrader.backend.friendship.Friendship;
import com.toptrader.backend.friendship.FriendshipRepository;
import com.toptrader.backend.friendship.RelationshipStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/** Covers Section 3's GET /users/search HTTP behavior (docs/tasks/in-progress/friends.md). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserSearchControllerTest {

  private static final String PASSWORD = "password123";

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private FriendshipRepository friendshipRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void search_withoutSession_returns401() throws Exception {
    mockMvc
        .perform(get("/users/search").param("q", "anything"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void search_blankQuery_returns400() throws Exception {
    MockHttpSession session = seedUserAndLogin("blankquerycaller");

    mockMvc
        .perform(get("/users/search").param("q", "").session(session))
        .andExpect(status().isBadRequest());
  }

  @Test
  void search_oversizedQuery_returns400() throws Exception {
    MockHttpSession session = seedUserAndLogin("oversizedcaller");

    mockMvc
        .perform(get("/users/search").param("q", "a".repeat(51)).session(session))
        .andExpect(status().isBadRequest());
  }

  @Test
  void search_excludesCallerFromResults() throws Exception {
    MockHttpSession session = seedUserAndLogin("selfexcludeuser");

    UserSearchResult[] results = search("selfexclude", session);

    assertThat(results).isEmpty();
  }

  @Test
  void search_excludesDemoAccount() throws Exception {
    // V6__seed_demo_account.sql permanently seeds this user (username "demo_trader").
    MockHttpSession session = seedUserAndLogin("demosearchcaller");

    UserSearchResult[] results = search("demo_trad", session);

    assertThat(results).isEmpty();
  }

  @Test
  void search_isCaseInsensitiveAndPartialMatch() throws Exception {
    MockHttpSession session = seedUserAndLogin("partialmatchcaller");
    seedUser("partialmatchtarget");

    UserSearchResult[] results = search("PARTIALMATCHTAR", session);

    assertThat(results).extracting(UserSearchResult::username).contains("partialmatchtarget");
  }

  @Test
  void search_treatsPercentCharacterLiterally() throws Exception {
    MockHttpSession session = seedUserAndLogin("percentcaller");
    seedUser("has%percent");
    seedUser("nopercentchar");

    UserSearchResult[] results = search("%", session);

    assertThat(results)
        .extracting(UserSearchResult::username)
        .contains("has%percent")
        .doesNotContain("nopercentchar");
  }

  @Test
  void search_treatsUnderscoreCharacterLiterally() throws Exception {
    MockHttpSession session = seedUserAndLogin("underscorecaller");
    seedUser("has_underscore");
    seedUser("nounderscorechar");

    UserSearchResult[] results = search("_", session);

    assertThat(results)
        .extracting(UserSearchResult::username)
        .contains("has_underscore")
        .doesNotContain("nounderscorechar");
  }

  @Test
  void search_capsResultsAtTen() throws Exception {
    MockHttpSession session = seedUserAndLogin("capcaller");
    for (int i = 0; i < 11; i++) {
      seedUser("capuser" + i);
    }

    UserSearchResult[] results = search("capuser", session);

    assertThat(results).hasSize(10);
  }

  @Test
  void search_reflectsFriendsRelationshipStatus() throws Exception {
    User caller = seedUser("friendscaller");
    MockHttpSession session = loginAndGetSession(caller.getEmail(), PASSWORD);
    User target = seedUser("friendstarget");
    friendshipRepository.save(new Friendship(caller, target, Friendship.Status.ACCEPTED));

    UserSearchResult[] results = search("friendstarget", session);

    assertThat(results)
        .filteredOn(r -> r.username().equals("friendstarget"))
        .extracting(UserSearchResult::relationshipStatus)
        .containsExactly(RelationshipStatus.FRIENDS);
  }

  @Test
  void search_reflectsOutgoingPendingRelationshipStatus() throws Exception {
    User caller = seedUser("outgoingcaller");
    MockHttpSession session = loginAndGetSession(caller.getEmail(), PASSWORD);
    User target = seedUser("outgoingtarget");
    friendshipRepository.save(new Friendship(caller, target, Friendship.Status.PENDING));

    UserSearchResult[] results = search("outgoingtarget", session);

    assertThat(results)
        .filteredOn(r -> r.username().equals("outgoingtarget"))
        .extracting(UserSearchResult::relationshipStatus)
        .containsExactly(RelationshipStatus.OUTGOING_PENDING);
  }

  @Test
  void search_reflectsIncomingPendingRelationshipStatus() throws Exception {
    User caller = seedUser("incomingcaller");
    MockHttpSession session = loginAndGetSession(caller.getEmail(), PASSWORD);
    User target = seedUser("incomingtarget");
    friendshipRepository.save(new Friendship(target, caller, Friendship.Status.PENDING));

    UserSearchResult[] results = search("incomingtarget", session);

    assertThat(results)
        .filteredOn(r -> r.username().equals("incomingtarget"))
        .extracting(UserSearchResult::relationshipStatus)
        .containsExactly(RelationshipStatus.INCOMING_PENDING);
  }

  private UserSearchResult[] search(String q, MockHttpSession session) throws Exception {
    MvcResult result =
        mockMvc
            .perform(get("/users/search").param("q", q).session(session))
            .andExpect(status().isOk())
            .andReturn();
    return objectMapper.readValue(
        result.getResponse().getContentAsString(), UserSearchResult[].class);
  }

  private User seedUser(String username) {
    return userRepository.save(
        new User(
            username + "@example.com",
            username,
            passwordEncoder.encode(PASSWORD),
            new BigDecimal("500.00")));
  }

  private MockHttpSession seedUserAndLogin(String username) throws Exception {
    User user = seedUser(username);
    return loginAndGetSession(user.getEmail(), PASSWORD);
  }

  private MockHttpSession loginAndGetSession(String email, String password) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginRequestBody(email, password)))
            .andExpect(status().isOk())
            .andReturn();
    return (MockHttpSession) result.getRequest().getSession(false);
  }

  private String loginRequestBody(String email, String password) {
    return """
        {"email":"%s","password":"%s"}
        """
        .formatted(email, password);
  }
}
