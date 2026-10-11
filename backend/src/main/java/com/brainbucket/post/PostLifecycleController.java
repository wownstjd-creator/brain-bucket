package com.brainbucket.post;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts/mine/{id}")
public class PostLifecycleController {
  private final PostLifecycleService service;
  public PostLifecycleController(PostLifecycleService service) { this.service=service; }

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(Authentication auth, @PathVariable Long id, @RequestParam Long version) {
    service.change(auth.getName(), id, version, true);
  }

  @PostMapping("/approval-cancel")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void cancel(Authentication auth, @PathVariable Long id, @RequestParam Long version) {
    service.change(auth.getName(), id, version, false);
  }
}
