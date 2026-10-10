package com.brainbucket.post;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/posts")
public class ManagementController {
  private final ManagementService s;

  public ManagementController(ManagementService s) {
    this.s = s;
  }

  @GetMapping("/managed")
  public List<ManagementService.ManagedPost> list(
      Authentication a,
      @RequestParam(defaultValue = "ALL") String status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return s.list(a.getName(), status, page, size);
  }

  @PostMapping("/{id}/hide")
  public CreatePostResponse hide(
      Authentication a, @PathVariable Long id, @RequestBody ManagementService.ChangeRequest r) {
    return s.visibility(a.getName(), id, true, r);
  }

  @PostMapping("/{id}/restore")
  public CreatePostResponse restore(
      Authentication a, @PathVariable Long id, @RequestBody ManagementService.ChangeRequest r) {
    return s.visibility(a.getName(), id, false, r);
  }

  @PutMapping("/{id}/schedule")
  public CreatePostResponse schedule(
      Authentication a, @PathVariable Long id, @Valid @RequestBody ScheduleRequest r) {
    return s.schedule(a.getName(), id, r);
  }
}
