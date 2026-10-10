package com.brainbucket.user;

import java.util.Map;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CsrfApiController {

  @GetMapping("/api/auth/csrf")
  public Map<String, String> csrf(CsrfToken csrfToken) {
    return Map.of(
        "headerName", csrfToken.getHeaderName(),
        "token", csrfToken.getToken());
  }
}
