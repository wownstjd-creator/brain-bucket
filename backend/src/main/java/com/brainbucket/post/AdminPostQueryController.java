package com.brainbucket.post;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/posts")
public class AdminPostQueryController {

    private final AdminPostQueryService service;

    public AdminPostQueryController(AdminPostQueryService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<AdminPostQueryService.ReviewSummary>> pending(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.getPending(
                        authentication.getName(),
                        page,
                        size
                ));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<AdminPostQueryService.ReviewDetail> detail(
            Authentication authentication,
            @PathVariable Long postId
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.getDetail(
                        authentication.getName(),
                        postId
                ));
    }

    @GetMapping("/{postId}/poster")
    public ResponseEntity<Resource> poster(
            Authentication authentication,
            @PathVariable Long postId
    ) {
        var download = service.getPoster(
                authentication.getName(),
                postId
        );

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(
                        download.contentType()
                ))
                .body(download.resource());
    }
}