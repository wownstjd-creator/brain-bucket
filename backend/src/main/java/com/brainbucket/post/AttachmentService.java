package com.brainbucket.post;

import com.brainbucket.user.*;
import java.io.IOException;
import java.util.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class AttachmentService {
  private final PostRepository posts;
  private final UserRepository users;
  private final PostFileRepository files;
  private final PosterStorage storage;
  private final ApprovalLogRepository logs;

  public AttachmentService(
      PostRepository p,
      UserRepository u,
      PostFileRepository f,
      PosterStorage s,
      ApprovalLogRepository l) {
    posts = p;
    users = u;
    files = f;
    storage = s;
    logs = l;
  }

  private String kind(String kind) {
    String k = kind.toUpperCase(Locale.ROOT);
    if (!Set.of("EVIDENCE", "ORIGINAL", "POSTER").contains(k))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    return k;
  }

  private User user(String email) {
    return users
        .findByEmail(email)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
  }

  private Post authorize(String email, Long id, boolean lock, boolean ownerOnly) {
    return authorize(email, id, lock, ownerOnly, false);
  }

  private Post authorize(String email, Long id, boolean lock, boolean ownerOnly, boolean history) {
    var u = user(email);
    var p =
        (lock ? posts.findForReview(id) : history ? posts.findById(id) : posts.findByIdAndDeletedAtIsNull(id))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (!p.getAuthor().getId().equals(u.getId()) && (ownerOnly || !"ADMIN".equals(u.getRole())))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    return p;
  }

  public UploadPosterResponse save(
      String email, Long id, String k, MultipartFile file, Long version) {
    k = kind(k);
    var p = authorize(email, id, true, true);
    PostRules.version(p, version);
    if (p.getStatus() != PostStatus.DRAFT && p.getStatus() != PostStatus.REJECTED)
      throw new ResponseStatusException(HttpStatus.CONFLICT);
    String name =
        Optional.ofNullable(file.getOriginalFilename()).orElse("image").replace('\\', '/');
    name = name.substring(name.lastIndexOf('/') + 1);
    if (name.isBlank() || name.length() > 255)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    var previous = files.findByPost_IdAndFileKind(id, k);
    String oldKey = previous.map(PostFile::getStorageKey).orElse(null);
    boolean preserved = oldKey != null && logs.countFileReferences(oldKey) > 0;
    var stored = storage.store(file);
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          public void afterCompletion(int status) {
            String delete =
                status == STATUS_ROLLED_BACK
                    ? stored.storageKey()
                    : status == STATUS_COMMITTED && !preserved ? oldKey : null;
            if (delete != null)
              try {
                storage.deleteStored(delete);
              } catch (IOException e) {
                org.slf4j.LoggerFactory.getLogger(AttachmentService.class)
                    .error("첨부 정리 실패 {}", delete, e);
              }
          }
        });
    PostFile f = previous.orElse(null);
    if (f == null)
      f = new PostFile(p, k, stored.storageKey(), name, stored.contentType(), stored.sizeBytes());
    else f.replaceFile(stored.storageKey(), name, stored.contentType(), stored.sizeBytes());
    files.saveAndFlush(f);
    p.touchFiles();
    posts.flush();
    return new UploadPosterResponse(
        f.getId(),
        id,
        name,
        f.getContentType(),
        f.getSizeBytes(),
        "/api/posts/mine/" + id + "/attachments/" + k.toLowerCase(Locale.ROOT));
  }

  public void remove(String email, Long id, String k, Long version) {
    k=kind(k);
    var p=authorize(email,id,true,true);
    PostRules.version(p,version);
    if(p.getStatus()!=PostStatus.DRAFT && p.getStatus()!=PostStatus.REJECTED)
      throw new ResponseStatusException(HttpStatus.CONFLICT,"작성 중이거나 반려된 글의 첨부만 제거할 수 있습니다.");
    var file=files.findByPost_IdAndFileKind(id,k);
    if(file.isEmpty()) return;
    String key=file.get().getStorageKey();
    boolean preserved=logs.countFileReferences(key)>0;
    files.delete(file.get());
    files.flush();
    p.touchFiles();
    posts.flush();
    if(!preserved) TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          public void afterCommit() {
            try { storage.deleteStored(key); }
            catch(IOException e) { org.slf4j.LoggerFactory.getLogger(AttachmentService.class)
                .error("첨부 정리 실패 {}",key,e); }
          }
        });
  }

  @Transactional(readOnly = true)
  public PosterService.PosterDownload get(String email, Long id, String k) {
    authorize(email, id, false, false);
    var f =
        files
            .findByPost_IdAndFileKind(id, kind(k))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    return new PosterService.PosterDownload(
        new FileSystemResource(storage.loadStored(f.getStorageKey())), f.getContentType());
  }

  @Transactional(readOnly = true)
  public List<History> history(String email, Long id) {
    authorize(email, id, false, false, true);
    return logs.findByPost_IdOrderByCreatedAtDescIdDesc(id).stream()
        .map(
            l ->
                new History(
                    l.getId(),
                    l.getDecision(),
                    l.getReason(),
                    l.getCreatedAt(),
                    l.getReviewer().getDisplayName(),
                    l.getSnapshot(),
                    url(l, "POSTER"),
                    url(l, "EVIDENCE"),
                    url(l, "ORIGINAL")))
        .toList();
  }

  private String url(ApprovalLog l, String kind) {
    return l.key(kind) == null
        ? null
        : "/api/posts/mine/"
            + l.getPost().getId()
            + "/history/"
            + l.getId()
            + "/"
            + kind.toLowerCase(Locale.ROOT);
  }

  @Transactional(readOnly = true)
  public PosterService.PosterDownload historical(String email, Long id, Long logId, String kind) {
    authorize(email, id, false, false, true);
    var l =
        logs.findById(logId)
            .filter(v -> v.getPost().getId().equals(id))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    String k = kind(kind);
    if (l.key(k) == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    return new PosterService.PosterDownload(
        new FileSystemResource(storage.loadStored(l.key(k))), l.type(k));
  }

  public record History(
      Long id,
      String decision,
      String reason,
      java.time.LocalDateTime createdAt,
      String reviewerName,
      String snapshot,
      String imageUrl,
      String evidenceUrl,
      String originalUrl) {}
}
