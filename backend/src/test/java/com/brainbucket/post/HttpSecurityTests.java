package com.brainbucket.post;

import static org.junit.jupiter.api.Assertions.*;

import com.brainbucket.user.*;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.regex.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class HttpSecurityTests {
  @LocalServerPort int port;
  @Autowired UserRepository users;
  @Autowired JdbcTemplate db;
  @Autowired PasswordEncoder encoder;
  @Autowired PostService drafts;
  @Autowired AttachmentService files;
  @Autowired PostApprovalService approval;
  @Autowired AdminPostService reviews;

  HttpClient client() {
    return HttpClient.newBuilder()
        .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
        .build();
  }

  HttpResponse<String> call(HttpClient c, String path, String method, String body, String token)
      throws Exception {
    var b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
    if (token != null) b.header("X-CSRF-TOKEN", token);
    if (body != null) b.header("Content-Type", "application/json");
    return c.send(
        b.method(
                method,
                body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body))
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  String csrf(HttpClient c) throws Exception {
    var r = call(c, "/api/auth/csrf", "GET", null, null);
    assertEquals(200, r.statusCode());
    var m = Pattern.compile("\\\"token\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(r.body());
    assertTrue(m.find());
    return m.group(1);
  }

  void login(HttpClient c, String email) throws Exception {
    assertEquals(
        200,
        call(
                c,
                "/api/auth/login",
                "POST",
                "{\"email\":\"" + email + "\",\"password\":\"TestPass123!\"}",
                csrf(c))
            .statusCode());
  }

  @Test
  void authenticationCsrfAndSessionViews() throws Exception {
    users.saveAndFlush(
        new User("http-owner@example.com", encoder.encode("TestPass123!"), "HTTP owner"));
    var admin =
        users.saveAndFlush(
            new User("http-admin@example.com", encoder.encode("TestPass123!"), "HTTP admin"));
    db.update("update users set role='ADMIN' where id=?", admin.getId());
    var anonymous = client();
    assertEquals(401, call(anonymous, "/api/admin/posts", "GET", null, null).statusCode());
    var owner = client();
    login(owner, "http-owner@example.com");
    assertEquals(403, call(owner, "/api/admin/posts", "GET", null, null).statusCode());
    assertEquals(403, call(owner, "/api/posts", "POST", "{}", null).statusCode());
    var reviewer = client();
    login(reviewer, "http-admin@example.com");
    assertEquals(200, call(reviewer, "/api/admin/posts/managed", "GET", null, null).statusCode());
    LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
    Long id =
        drafts
            .createDraft(
                "http-owner@example.com",
                new CreatePostRequest(
                    "HTTP",
                    "COMA",
                    "EVENT",
                    "",
                    "",
                    "",
                    null,
                    null,
                    today.plusDays(1),
                    today,
                    null,
                    true))
            .id();
    var img = new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_RGB);
    var out = new java.io.ByteArrayOutputStream();
    javax.imageio.ImageIO.write(img, "png", out);
    var file = new MockMultipartFile("file", "test.png", "image/png", out.toByteArray());
    files.save("http-owner@example.com", id, "POSTER", file, null);
    files.save("http-owner@example.com", id, "EVIDENCE", file, null);
    approval.requestApproval("http-owner@example.com", id);
    reviews.approve("http-admin@example.com", id);
    assertEquals(
        401,
        call(anonymous, "/api/posts/mine/" + id + "/attachments/evidence", "GET", null, null)
            .statusCode());
    assertEquals(200, call(anonymous, "/api/posts/public/" + id, "GET", null, null).statusCode());
    String token = csrf(anonymous);
    assertEquals(
        200,
        call(anonymous, "/api/posts/public/" + id + "/view", "POST", null, token).statusCode());
    assertEquals(
        200,
        call(anonymous, "/api/posts/public/" + id + "/view", "POST", null, token).statusCode());
    assertEquals(1L, db.queryForObject("select view_count from posts where id=?", Long.class, id));
    assertEquals(204, call(owner, "/api/bookmarks/" + id, "PUT", null, csrf(owner)).statusCode());
    assertEquals(200, call(owner, "/api/bookmarks", "GET", null, null).statusCode());
    assertEquals(
        204, call(owner, "/api/bookmarks/" + id, "DELETE", null, csrf(owner)).statusCode());
  }
}
