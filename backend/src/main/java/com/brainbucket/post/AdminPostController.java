package com.brainbucket.post;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/posts")
public class AdminPostController {

    private final AdminPostService adminPostService;

    public AdminPostController(AdminPostService adminPostService) {
        this.adminPostService = adminPostService;
    }

    @PostMapping("/{postId}/approve")
    public CreatePostResponse approve(
            Authentication authentication,
            @PathVariable Long postId
    ) {
        return adminPostService.approve(
                authentication.getName(),
                postId
        );
    }

    @PostMapping("/{postId}/reject")
    public CreatePostResponse reject(
            Authentication authentication,
            @PathVariable Long postId,
            @Valid @RequestBody RejectPostRequest request
    ) {
        return adminPostService.reject(
                authentication.getName(),
                postId,
                request.reason()
        );
    }
}