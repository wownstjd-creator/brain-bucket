package com.brainbucket.post;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "post_files",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_post_files_kind",
                columnNames = {"post_id", "file_kind"}
        )
)
public class PostFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "file_kind", nullable = false, length = 20)
    private String fileKind;

    @Column(name = "storage_key", nullable = false, length = 512)
    private String storageKey;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected PostFile() {
    }

    public PostFile(
            Post post,
            String fileKind,
            String storageKey,
            String originalName,
            String contentType,
            long sizeBytes
    ) {
        this.post = post;
        this.fileKind = fileKind;
        this.storageKey = storageKey;
        this.originalName = originalName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
    }

    @PrePersist
    private void beforeInsert() {
        this.createdAt = LocalDateTime.now();
    }
    public void replaceFile(
            String storageKey,
            String originalName,
            String contentType,
            long sizeBytes
    ) {
        this.storageKey = storageKey;
        this.originalName = originalName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
    }
    public Long getId() { return id; }
    public Post getPost() { return post; }
    public String getFileKind() { return fileKind; }
    public String getStorageKey() { return storageKey; }
    public String getOriginalName() { return originalName; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}