package com.brainbucket.post;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts/mine")
public class PostApprovalController {

  private final PostApprovalService postApprovalService;

  public PostApprovalController(PostApprovalService postApprovalService) {
    this.postApprovalService = postApprovalService;
  }

  @PostMapping("/{postId}/approval-request")
  public CreatePostResponse requestApproval(
      Authentication authentication, @PathVariable Long postId, @RequestParam(required = false) Long version) {
    return postApprovalService.requestApproval(authentication.getName(), postId, version);
  }

  @GetMapping("/{postId}/approval-logs")
  public List<ApprovalLogResponse> getMyApprovalLogs(
      Authentication authentication, @PathVariable Long postId) {
    return postApprovalService.getMyApprovalLogs(authentication.getName(), postId);
  }
}
