package com.brainbucket.post;

import jakarta.servlet.http.HttpSession;
import java.time.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ViewController {
  private final JdbcTemplate db;

  public ViewController(JdbcTemplate db) {
    this.db = db;
  }

  @PostMapping("/api/posts/public/{id}/view")
  @Transactional
  public Map<String, Object> view(@PathVariable Long id, HttpSession session) {
    synchronized (session) {
      @SuppressWarnings("unchecked")
      var seen = (Set<Long>) session.getAttribute("viewedPosts");
      if (seen == null) seen = new HashSet<>();
      LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
      if (!seen.contains(id)) {
        int changed =
            db.update(
                "update posts set view_count=view_count+1 where id=? and status='APPROVED' and"
                    + " deleted_at is null and display_until is not null and (display_from is null"
                    + " or display_from<=?)",
                id,
                today);
        if (changed == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if (seen.size() >= 1000) seen.clear();
        seen.add(id);
        session.setAttribute("viewedPosts", seen);
      }
      var rows =
          db.queryForList(
              "select view_count,show_view_count from posts where id=? and status='APPROVED' and"
                  + " deleted_at is null and display_until is not null and (display_from is null or"
                  + " display_from<=?)",
              id,
              today);
      if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
      var row = rows.getFirst();
      Object flag = row.get("show_view_count");
      boolean visible = flag instanceof Boolean b ? b : ((Number) flag).intValue() != 0;
      var result = new HashMap<String, Object>();
      result.put("viewCount", visible ? row.get("view_count") : null);
      return result;
    }
  }
}
