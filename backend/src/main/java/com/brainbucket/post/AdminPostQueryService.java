package com.brainbucket.post;

import com.brainbucket.user.UserRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AdminPostQueryService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PostFileRepository postFileRepository;
    private final PosterStorage posterStorage;

    public AdminPostQueryService(
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

    public List<ReviewSummary> getPending(
            String email,
            int page,
            int size
    ) {
        requireAdmin(email);

        if (page < 0 || size < 1 || size > 50) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "page는 0 이상, size는 1~50이어야 합니다."
            );
        }

        var pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.asc("createdAt"),
                        Sort.Order.asc("id")
                )
        );

        return postRepository
                .findByStatusAndDeletedAtIsNull(
                        PostStatus.PENDING,
                        pageable
                )
                .getContent()
                .stream()
                .map(post -> new ReviewSummary(
                        post.getId(),
                        post.getAuthor().getId(),
                        post.getTitle(),
                        post.getOrganizer(),
                        post.getCategory(),
                        post.getStatus(),
                        post.getDisplayUntil(),
                        post.getCreatedAt(),
                        post.getUpdatedAt()
                ))
                .toList();
    }

    public ReviewDetail getDetail(String email, Long postId) {
        requireAdmin(email);
        Post post = requirePost(postId);

        String imageUrl = postFileRepository
                .existsByPost_IdAndFileKind(postId, "POSTER")
                ? "/api/admin/posts/" + postId + "/poster"
                : null;

        return new ReviewDetail(
                post.getAuthor().getId(),
                new MyPostDetailResponse(
                        post.getId(),
                        post.getTitle(),
                        post.getOrganizer(),
                        post.getCategory(),
                        post.getDescription(),
                        post.getLocation(),
                        post.getApplicationUrl(),
                        post.getEventAt(),
                        post.getDeadlineAt(),
                        post.getDisplayUntil(),
                        post.getStatus(),
                        post.isShowViewCount(),
                        post.getViewCount(),
                        post.getPublishedAt(),
                        post.getCreatedAt(),
                        post.getUpdatedAt()
                ),
                imageUrl
        );
    }

    public PosterService.PosterDownload getPoster(
            String email,
            Long postId
    ) {
        requireAdmin(email);
        requirePost(postId);

        PostFile file = postFileRepository
                .findByPost_IdAndFileKind(postId, "POSTER")
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "전단지 이미지가 없습니다."
                ));

        return new PosterService.PosterDownload(
                new FileSystemResource(
                        posterStorage.loadStored(file.getStorageKey())
                ),
                file.getContentType()
        );
    }

    private void requireAdmin(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED
                ));

        if (!"ADMIN".equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private Post requirePost(Long postId) {
        return postRepository.findByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "게시물을 찾을 수 없습니다."
                ));
    }

    public record ReviewSummary(
            Long id,
            Long authorId,
            String title,
            String organizer,
            String category,
            PostStatus status,
            LocalDate displayUntil,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record ReviewDetail(
            Long authorId,
            MyPostDetailResponse post,
            String imageUrl
    ) {
    }
}