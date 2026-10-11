package com.brainbucket.post;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookmarks")
public class BookmarkController {
  private final BookmarkService s;

  public BookmarkController(BookmarkService s) {
    this.s = s;
  }

  @GetMapping
  public List<PublicPostResponse> list(
      Authentication a,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required=false) String search,@RequestParam(required=false) String category,
      @RequestParam(required=false) Integer year) {
    return s.list(a.getName(), page, size,search,category,year);
  }

  @GetMapping("/ids")
  public List<Long> ids(Authentication a) {
    return s.ids(a.getName());
  }

  @PutMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void add(Authentication a, @PathVariable Long id) {
    s.save(a.getName(), id);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void remove(Authentication a, @PathVariable Long id) {
    s.remove(a.getName(), id);
  }
}
