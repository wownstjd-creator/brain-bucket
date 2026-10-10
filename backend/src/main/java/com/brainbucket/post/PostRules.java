package com.brainbucket.post;

import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class PostRules {
  private PostRules() {}

  public static void window(LocalDate start, LocalDate end) {
    if (start != null && end != null && start.isAfter(end))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "게시 시작일은 종료일 이후일 수 없습니다.");
  }

  public static void version(Post p, Long expected) {
    if (expected != null && p.getVersion() != expected)
      throw new ResponseStatusException(HttpStatus.CONFLICT, "다른 변경이 있습니다. 새로고침해주세요.");
  }

  public static void draft(CreatePostRequest r) {
    window(r.displayFrom(), r.displayUntil());
    if (r.applicationUrl() != null && !r.applicationUrl().isBlank()) {
      try {
        var u = java.net.URI.create(r.applicationUrl());
        if (!java.util.Set.of("http", "https").contains(u.getScheme()) || u.getHost() == null)
          throw new IllegalArgumentException();
      } catch (Exception e) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "http/https 신청 주소를 입력해주세요.");
      }
    }
  }
}
