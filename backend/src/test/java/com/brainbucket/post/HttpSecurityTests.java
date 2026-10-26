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
  @Autowired PostRepository posts;
  @Autowired PostLifecycleService lifecycle;

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
    var signupClient = client();
    assertEquals(201, call(signupClient, "/api/auth/signup", "POST",
        "{\"email\":\"http-signup@example.com\",\"password\":\"TestPass123!\",\"displayName\":\"Signup\"}",
        csrf(signupClient)).statusCode());
    login(signupClient, "http-signup@example.com");
    var badSignup = call(signupClient, "/api/auth/signup", "POST",
        "{\"email\":\"invalid\",\"password\":\"TestPass123!\",\"displayName\":\"Signup\"}", csrf(signupClient));
    assertEquals(400, badSignup.statusCode());
    assertTrue(badSignup.body().contains("message"));
    assertTrue(badSignup.body().contains("이메일 형식"));
    var badLink = call(signupClient, "/api/posts", "POST",
        "{\"applicationUrl\":\"invalid\"}", csrf(signupClient));
    assertEquals(400, badLink.statusCode());
    assertTrue(badLink.body().contains("http/https"));
    assertEquals(201, call(signupClient, "/api/posts", "POST",
        "{\"applicationUrl\":null}", csrf(signupClient)).statusCode());
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
    reviews.approve("http-admin@example.com", id, posts.findById(id).orElseThrow().getVersion());
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
  @Test
  void passwordChangeValidatesCredentialsAndExpiresCurrentSession() throws Exception {
    String email="password-http@example.com";
    users.saveAndFlush(new User(email,encoder.encode("TestPass123!"),"Password"));
    var c=client();login(c,email);
    String path="/api/auth/password";
    String body="{\"currentPassword\":\"TestPass123!\",\"newPassword\":\"ChangedPass456!\"}";
    assertEquals(403,call(c,path,"POST",body,null).statusCode());
    assertEquals(400,call(c,path,"POST",body.replace("TestPass123!","wrong"),csrf(c)).statusCode());
    assertTrue(encoder.matches("TestPass123!",users.findByEmail(email).orElseThrow().getPasswordHash()));
    assertEquals(400,call(c,path,"POST",body.replace("ChangedPass456!","tiny"),csrf(c)).statusCode());
    assertEquals(400,call(c,path,"POST",body.replace("ChangedPass456!","TestPass123!"),csrf(c)).statusCode());
    assertEquals(204,call(c,path,"POST",body,csrf(c)).statusCode());
    assertEquals(401,call(c,"/api/auth/me","GET",null,null).statusCode());
    assertEquals(401,call(c,"/api/auth/login","POST","{\"email\":\""+email+"\",\"password\":\"TestPass123!\"}",csrf(c)).statusCode());
    assertEquals(200,call(c,"/api/auth/login","POST","{\"email\":\""+email+"\",\"password\":\"ChangedPass456!\"}",csrf(c)).statusCode());
    assertTrue(encoder.matches("ChangedPass456!",users.findByEmail(email).orElseThrow().getPasswordHash()));
  }


  @Test
  void staleReviewCannotApproveOrRejectResubmittedContent() throws Exception {
    String email="revision-owner@example.com", adminEmail="revision-admin@example.com";
    users.saveAndFlush(new User(email,encoder.encode("TestPass123!"),"Owner"));
    var admin=users.saveAndFlush(new User(adminEmail,encoder.encode("TestPass123!"),"Admin"));
    db.update("UPDATE users SET role='ADMIN' WHERE id=?",admin.getId());
    var reviewer=client(); login(reviewer,adminEmail);
    var owner=client(); login(owner,email);
    var today=LocalDate.now(ZoneId.of("Asia/Seoul"));
    var draft=new CreatePostRequest("Before","Team","EVENT","before",null,null,null,null,today.plusDays(30),today,null,true);
    Long id=drafts.createDraft(email,draft).id();
    var image=new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB);
    var out=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",out);
    var file=new MockMultipartFile("file","test.png","image/png",out.toByteArray());
    files.save(email,id,"POSTER",file,null);files.save(email,id,"EVIDENCE",file,null);
    long draftVersion=posts.findById(id).orElseThrow().getVersion();
    String submit="/api/posts/mine/"+id+"/approval-request?version=";
    assertEquals(409,call(owner,submit+(draftVersion-1),"POST",null,csrf(owner)).statusCode());
    assertEquals(200,call(owner,submit+draftVersion,"POST",null,csrf(owner)).statusCode());
    long reviewed=posts.findById(id).orElseThrow().getVersion();
    lifecycle.change(email,id,reviewed,false);
    long editable=posts.findById(id).orElseThrow().getVersion();
    drafts.updateMyDraft(email,id,new CreatePostRequest("Changed","Team","EVENT","new content",null,null,null,null,today.plusDays(30),today,editable,true));
    approval.requestApproval(email,id,posts.findById(id).orElseThrow().getVersion());
    long current=posts.findById(id).orElseThrow().getVersion();
    assertTrue(current>reviewed);
    String path="/api/admin/posts/"+id;
    assertEquals(400,call(reviewer,path+"/approve","POST","{}",csrf(reviewer)).statusCode());
    assertEquals(400,call(reviewer,path+"/approve","POST",null,csrf(reviewer)).statusCode());
    assertEquals(400,call(reviewer,path+"/reject","POST","{\"reason\":\"fix\"}",csrf(reviewer)).statusCode());
    assertEquals(409,call(reviewer,path+"/approve","POST","{\"version\":"+reviewed+"}",csrf(reviewer)).statusCode());
    assertEquals(409,call(reviewer,path+"/reject","POST","{\"reason\":\"fix\",\"version\":"+reviewed+"}",csrf(reviewer)).statusCode());
    assertEquals(PostStatus.PENDING,posts.findById(id).orElseThrow().getStatus());
    assertEquals(200,call(reviewer,path+"/reject","POST","{\"reason\":\"reviewed changes\",\"version\":"+current+"}",csrf(reviewer)).statusCode());
    approval.requestApproval(email,id,posts.findById(id).orElseThrow().getVersion());
    long latest=posts.findById(id).orElseThrow().getVersion();
    assertEquals(200,call(reviewer,path+"/approve","POST","{\"version\":"+latest+"}",csrf(reviewer)).statusCode());
    assertEquals(PostStatus.APPROVED,posts.findById(id).orElseThrow().getStatus());
    assertEquals(404,call(reviewer,"/api/dev/users/count","GET",null,null).statusCode());
  }
}
