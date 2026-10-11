package com.brainbucket.post;

import com.brainbucket.user.*;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class BookmarkService {
  private final JdbcTemplate db;
  private final UserRepository users;
  private final PostRepository posts;

  public BookmarkService(JdbcTemplate db, UserRepository u, PostRepository p) {
    this.db = db;
    users = u;
    posts = p;
  }

  private Long user(String email) {
    return users
        .findByEmail(email)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED))
        .getId();
  }

  public void save(String email, Long id) {
    Long uid = user(email);
    db.queryForObject("select id from users where id=? for update", Long.class, uid);
    var p =
        posts
            .findForReview(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (!p.isPublic(LocalDate.now(ZoneId.of("Asia/Seoul"))))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    if (db.queryForObject(
            "select count(*) from bookmarks where user_id=? and post_id=?", Long.class, uid, id)
        == 0)
      db.update(
          "insert into bookmarks(user_id,post_id,created_at) values(?,?,?)",
          uid,
          id,
          LocalDateTime.now(ZoneId.of("Asia/Seoul")));
  }

  public void remove(String email, Long id) {
    db.update("delete from bookmarks where user_id=? and post_id=?", user(email), id);
  }

  @Transactional(readOnly = true)
  public List<Long> ids(String email) {
    return db.queryForList(
        "select b.post_id from bookmarks b join posts p on p.id=b.post_id where b.user_id=? and p.deleted_at is null order by b.created_at desc,b.post_id desc",
        Long.class,
        user(email));
  }

  @Transactional(readOnly = true)
  public List<PublicPostResponse> list(String email, int page, int size) {
    if (page < 0 || size < 1 || size > 50)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    List<Long> ids = ids(email);
    if (ids.isEmpty()) return List.of();
    LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
    return posts
        .findAll(
            (r, q, b) ->
                b.and(
                    r.get("id").in(ids),
                    b.equal(r.get("status"), PostStatus.APPROVED),
                    b.isNull(r.get("deletedAt")),
                    b.isNotNull(r.get("displayUntil")),
                    b.or(
                        b.isNull(r.get("displayFrom")),
                        b.lessThanOrEqualTo(r.get("displayFrom"), today))),
            PageRequest.of(page, size, Sort.by("id").descending()))
        .map(PublicPostResponse::from)
        .getContent();
  }
}
