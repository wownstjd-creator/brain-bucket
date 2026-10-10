package com.brainbucket.post;

import static org.junit.jupiter.api.Assertions.*;

import com.brainbucket.user.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
class IntegrationWorkflowTests {
  @Autowired PostService drafts;
  @Autowired UserRepository users;
  @Autowired PostRepository posts;
  @Autowired AttachmentService files;
  @Autowired PostApprovalService approval;
  @Autowired AdminPostService reviews;
  @Autowired ManagementService management;
  @Autowired PublicPostService published;
  @Autowired JdbcTemplate db;
  @Autowired BookmarkService bookmarks;
  @Autowired PosterService posters;
  private LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

  private CreatePostRequest draft(LocalDate start, LocalDate end, Long version) {
    return new CreatePostRequest(
        "Recruit",
        "COMA",
        "CLUB",
        "Details",
        "N111",
        "https://example.com",
        null,
        null,
        end,
        start,
        version,
        true);
  }

  private MockMultipartFile image(int color) throws IOException {
    var b = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
    b.setRGB(0, 0, color);
    var out = new ByteArrayOutputStream();
    ImageIO.write(b, "png", out);
    return new MockMultipartFile("file", "sample.png", "image/png", out.toByteArray());
  }

  private void status(int expected, Runnable action) {
    var ex = assertThrows(ResponseStatusException.class, action::run);
    assertEquals(expected, ex.getStatusCode().value());
  }

  @Test
  void fullWorkflowAndHistoricalFiles() throws Exception {
    users.saveAndFlush(new User("owner@example.com", "hash", "owner"));
    users.saveAndFlush(new User("other@example.com", "hash", "other"));
    var admin = users.saveAndFlush(new User("reviewer@example.com", "hash", "reviewer"));
    db.update("update users set role='ADMIN' where id=?", admin.getId());
    Long id =
        drafts
            .createDraft("owner@example.com", draft(today.plusDays(2), today.plusDays(30), null))
            .id();
    status(400, () -> approval.requestApproval("owner@example.com", id));
    files.save("owner@example.com", id, "POSTER", image(0xff0000), null);
    files.save("owner@example.com", id, "EVIDENCE", image(0x00ff00), null);
    files.save("owner@example.com", id, "ORIGINAL", image(0x0000ff), null);
    status(404, () -> files.get("other@example.com", id, "EVIDENCE"));
    status(403, () -> management.list("owner@example.com", "ALL", 0, 20));
    approval.requestApproval("owner@example.com", id);
    status(
        409,
        () ->
            files.save(
                "owner@example.com",
                id,
                "POSTER",
                new MockMultipartFile("file", new byte[] {1}),
                null));
    reviews.approve("reviewer@example.com", id);
    assertTrue(published.getPosts(false, 0, 20, null, null, null).isEmpty());
    status(404, () -> published.getDetail(id));
    status(404, () -> published.getPoster(id));
    management.schedule(
        "reviewer@example.com",
        id,
        new ScheduleRequest(today, today.plusDays(30), null, null, "start now", null));
    assertEquals(id, published.getDetail(id).id());
    assertEquals(
        1,
        published.getPosts(false, 0, 20, "Recruit", "CLUB", today.plusDays(30).getYear()).size());
    assertTrue(published.getPosts(false, 0, 20, "absent", null, null).isEmpty());
    var first =
        files.history("owner@example.com", id).stream()
            .filter(l -> l.decision().equals("APPROVED"))
            .findFirst()
            .orElseThrow();
    byte[] original =
        files
            .historical("reviewer@example.com", id, first.id(), "POSTER")
            .resource()
            .getInputStream()
            .readAllBytes();
    var pub = published.getDetail(id).publishedAt();
    management.visibility(
        "reviewer@example.com", id, true, new ManagementService.ChangeRequest("hide", null));
    status(404, () -> published.getPoster(id));
    status(
        409,
        () ->
            drafts.updateMyDraft("owner@example.com", id, draft(today, today.plusDays(30), null)));
    management.visibility(
        "reviewer@example.com", id, false, new ManagementService.ChangeRequest("restore", null));
    assertEquals(pub, published.getDetail(id).publishedAt());
    long version = drafts.getMyPost("owner@example.com", id).version();
    drafts.updateMyDraft("owner@example.com", id, draft(today, today.plusDays(30), version));
    status(
        409,
        () ->
            drafts.updateMyDraft(
                "owner@example.com", id, draft(today, today.plusDays(30), version)));
    status(404, () -> published.getDetail(id));
    files.save("owner@example.com", id, "POSTER", image(0xffffff), null);
    assertArrayEquals(
        original,
        files
            .historical("owner@example.com", id, first.id(), "POSTER")
            .resource()
            .getInputStream()
            .readAllBytes());
    byte[] before =
        posters.getMyPoster("owner@example.com", id).resource().getInputStream().readAllBytes();
    status(
        400,
        () ->
            files.save(
                "owner@example.com",
                id,
                "POSTER",
                new MockMultipartFile("file", "bad.txt", "text/plain", "bad".getBytes()),
                null));
    assertArrayEquals(
        before,
        posters.getMyPoster("owner@example.com", id).resource().getInputStream().readAllBytes());
    status(
        400,
        () ->
            management.schedule(
                "reviewer@example.com",
                id,
                new ScheduleRequest(today.plusDays(10), today, null, null, "invalid", null)));
    approval.requestApproval("owner@example.com", id);
    reviews.reject("reviewer@example.com", id, "fix");
    approval.requestApproval("owner@example.com", id);
    reviews.approve("reviewer@example.com", id);
    db.execute(
        "create table if not exists bookmarks(user_id bigint not null,post_id bigint not"
            + " null,created_at timestamp not null,primary key(user_id,post_id))");
    bookmarks.save("owner@example.com", id);
    bookmarks.save("owner@example.com", id);
    assertEquals(1, bookmarks.ids("owner@example.com").size());
    assertTrue(bookmarks.ids("other@example.com").isEmpty());
    management.visibility(
        "reviewer@example.com", id, true, new ManagementService.ChangeRequest("hidden", null));
    assertTrue(bookmarks.list("owner@example.com", 0, 20).isEmpty());
    bookmarks.remove("owner@example.com", id);
    assertTrue(bookmarks.ids("owner@example.com").isEmpty());
  }
}
