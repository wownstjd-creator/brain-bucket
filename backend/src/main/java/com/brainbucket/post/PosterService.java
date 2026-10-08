package com.brainbucket.post;

import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;
import com.brainbucket.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@Service
public class PosterService {

    private static final Logger log =
            LoggerFactory.getLogger(PosterService.class);

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PostFileRepository postFileRepository;
    private final PosterStorage posterStorage;

    public PosterService(
            UserRepository userRepository,
            PostRepository postRepository,
            PostFileRepository postFileRepository,
            PosterStorage posterStorage
    ) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.postFileRepository = postFileRepository;
        this.posterStorage = posterStorage;
    }

    @Transactional
    public UploadPosterResponse upload(
            String email,
            Long postId,
            MultipartFile file
    ) {
        var author = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED)
                );

        Post post = postRepository
                .findByIdAndAuthor_IdAndDeletedAtIsNull(postId, author.getId())
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND)
                );

        if (post.getStatus() != PostStatus.DRAFT
                && post.getStatus() != PostStatus.REJECTED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "작성 중이거나 반려된 게시물만 업로드할 수 있습니다."
            );
        }

        if (postFileRepository.existsByPost_IdAndFileKind(postId, "POSTER")) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 전단지 이미지가 등록되어 있습니다."
            );
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            originalName = "poster";
        }

        originalName = originalName.replace('\\', '/');
        originalName = originalName.substring(originalName.lastIndexOf('/') + 1);

        if (originalName.isBlank() || originalName.length() > 255) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "파일 이름은 1~255자여야 합니다."
            );
        }

        var stored = posterStorage.store(file);

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) {
                            try {
                                posterStorage.deleteStored(stored.storageKey());
                            } catch (IOException exception) {
                                log.error("저장 실패 후 이미지 정리 실패", exception);
                            }
                        }
                    }
                }
        );

        PostFile saved = postFileRepository.saveAndFlush(
                new PostFile(
                        post,
                        "POSTER",
                        stored.storageKey(),
                        originalName,
                        stored.contentType(),
                        stored.sizeBytes()
                )
        );

        return new UploadPosterResponse(
                saved.getId(),
                postId,
                saved.getOriginalName(),
                saved.getContentType(),
                saved.getSizeBytes(),
                "/api/posts/mine/" + postId + "/poster"
        );
    }
    @Transactional(readOnly = true)
    public PosterDownload getMyPoster(String email, Long postId) {
        var author = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED)
                );

        postRepository
                .findByIdAndAuthor_IdAndDeletedAtIsNull(postId, author.getId())
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND)
                );

        PostFile file = postFileRepository
                .findByPost_IdAndFileKind(postId, "POSTER")
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND)
                );

        Resource resource = new FileSystemResource(
                posterStorage.loadStored(file.getStorageKey())
        );

        return new PosterDownload(resource, file.getContentType());
    }

    public record PosterDownload(
            Resource resource,
            String contentType
    ) {
    }
    @Transactional
    public UploadPosterResponse replace(
            String email,
            Long postId,
            MultipartFile file
    ) {
        var author = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED)
                );

        // 같은 게시물의 이미지 교체와 승인 처리를 순서대로 진행
        Post post = postRepository.findForReview(postId)
                .filter(value ->
                        value.getAuthor().getId().equals(author.getId())
                )
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND)
                );

        if (post.getStatus() != PostStatus.DRAFT
                && post.getStatus() != PostStatus.REJECTED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "작성 중이거나 반려된 게시물만 이미지를 교체할 수 있습니다."
            );
        }

        PostFile existing = postFileRepository
                .findByPost_IdAndFileKind(postId, "POSTER")
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "교체할 이미지가 없습니다. 먼저 이미지를 업로드해주세요."
                        )
                );

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            originalName = "poster";
        }

        originalName = originalName.replace('\\', '/');
        originalName = originalName.substring(
                originalName.lastIndexOf('/') + 1
        );

        if (originalName.isBlank() || originalName.length() > 255) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "파일 이름은 1~255자여야 합니다."
            );
        }

        String oldStorageKey = existing.getStorageKey();

        // 이미지 검증과 새 파일 저장이 성공해야 DB 변경 진행
        var stored = posterStorage.store(file);

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status == STATUS_COMMITTED) {
                            // DB가 새 파일을 가리키게 된 후 기존 파일 정리
                            deleteQuietly(oldStorageKey);
                        } else if (status == STATUS_ROLLED_BACK) {
                            // DB 변경 실패 시 새 파일 정리, 기존 파일 유지
                            deleteQuietly(stored.storageKey());
                        } else {
                            // 결과가 불확실하면 파일을 유지하고 기록
                            log.error(
                                    "이미지 교체 트랜잭션 결과 불확실: postId={}, status={}",
                                    postId,
                                    status
                            );
                        }
                    }
                }
        );

        existing.replaceFile(
                stored.storageKey(),
                originalName,
                stored.contentType(),
                stored.sizeBytes()
        );

        postFileRepository.saveAndFlush(existing);

        return new UploadPosterResponse(
                existing.getId(),
                postId,
                existing.getOriginalName(),
                existing.getContentType(),
                existing.getSizeBytes(),
                "/api/posts/mine/" + postId + "/poster"
        );
    }

    private void deleteQuietly(String storageKey) {
        try {
            posterStorage.deleteStored(storageKey);
        } catch (IOException exception) {
            log.error(
                    "이미지 파일 정리 실패: storageKey={}",
                    storageKey,
                    exception
            );
        }
    }
}