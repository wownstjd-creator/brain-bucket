package com.brainbucket.post;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
public class PostController {

  private final PostService postService;

  public PostController(PostService postService) {
    this.postService = postService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CreatePostResponse createDraft(
      Authentication authentication, @Valid @RequestBody CreatePostRequest request) {
    return postService.createDraft(authentication.getName(), request);
  }

  @GetMapping("/mine")
  public List<MyPostResponse> getMyPosts(
      Authentication authentication,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return postService.getMyPosts(authentication.getName(), page, size);
  }

  @GetMapping("/mine/cards")
  public List<MyPostDetailResponse> cards(Authentication a,@RequestParam(defaultValue="0") int page,
      @RequestParam(defaultValue="20") int size){return postService.getMyCards(a.getName(),page,size);}

  @GetMapping("/mine/{postId}")
  public MyPostDetailResponse getMyPost(Authentication authentication, @PathVariable Long postId) {
    return postService.getMyPost(authentication.getName(), postId);
  }

  @PutMapping("/mine/{postId}")
  public CreatePostResponse updateMyDraft(
      Authentication authentication,
      @PathVariable Long postId,
      @Valid @RequestBody CreatePostRequest request) {
    return postService.updateMyDraft(authentication.getName(), postId, request);
  }
}
