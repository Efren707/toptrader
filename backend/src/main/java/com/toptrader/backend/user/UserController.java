package com.toptrader.backend.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@Validated
public class UserController {

  private final UserSearchService userSearchService;

  public UserController(UserSearchService userSearchService) {
    this.userSearchService = userSearchService;
  }

  @GetMapping("/search")
  public ResponseEntity<List<UserSearchResult>> search(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestParam @NotBlank @Size(max = 50) String q) {
    List<UserSearchResult> searchResult =
        this.userSearchService.searchUsers(userPrincipal.getUser().getId(), q);
    return new ResponseEntity<>(searchResult, HttpStatus.OK);
  }
}
