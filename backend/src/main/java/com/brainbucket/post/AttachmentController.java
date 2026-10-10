package com.brainbucket.post;

import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/posts/mine/{id}")
public class AttachmentController {
  private final AttachmentService s;

  public AttachmentController(AttachmentService s) {
    this.s = s;
  }

  @PutMapping(value = "/attachments/{kind}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public UploadPosterResponse save(
      Authentication a,
      @PathVariable Long id,
      @PathVariable String kind,
      @RequestParam("file") MultipartFile file,
      @RequestParam(required = false) Long version) {
    return s.save(a.getName(), id, kind, file, version);
  }

  @GetMapping("/attachments/{kind}")
  public ResponseEntity<Resource> get(
      Authentication a, @PathVariable Long id, @PathVariable String kind) {
    return download(s.get(a.getName(), id, kind));
  }

  @GetMapping("/history")
  public List<AttachmentService.History> history(Authentication a, @PathVariable Long id) {
    return s.history(a.getName(), id);
  }

  @GetMapping("/history/{logId}/{kind}")
  public ResponseEntity<Resource> historical(
      Authentication a,
      @PathVariable Long id,
      @PathVariable Long logId,
      @PathVariable String kind) {
    return download(s.historical(a.getName(), id, logId, kind));
  }

  private ResponseEntity<Resource> download(PosterService.PosterDownload d) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .contentType(MediaType.parseMediaType(d.contentType()))
        .body(d.resource());
  }
}
