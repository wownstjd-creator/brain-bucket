package com.brainbucket.post;

import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts/public")
public class PublicPostController {

  private final PublicPostService publicPostService;

  public PublicPostController(PublicPostService publicPostService) {
    this.publicPostService = publicPostService;
  }

  @GetMapping("/{postId}")
  public PublicPostDetailResponse getDetail(@PathVariable Long postId) {
    return publicPostService.getDetail(postId);
  }

  @GetMapping("/{postId}/poster")
  public ResponseEntity<Resource> getPoster(@PathVariable Long postId) {
    var poster = publicPostService.getPoster(postId);

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(poster.contentType()))
        .cacheControl(CacheControl.noStore())
        .body(poster.resource());
  }

  @GetMapping
  public List<PublicPostResponse> getPosts(
      @RequestParam(defaultValue = "false") boolean archived,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String search,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) Integer year) {
    return publicPostService.getPosts(archived, page, size, search, category, year);
  }
}
