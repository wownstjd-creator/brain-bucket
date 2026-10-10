package com.brainbucket.user;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {
  private final UserService userService;

  public UserController(UserService userService) { this.userService = userService; }

  @PostMapping("/api/auth/signup")
  @ResponseStatus(HttpStatus.CREATED)
  public SignupResponse signup(@Valid @RequestBody SignupRequest request) {
    return userService.signup(request);
  }
}
