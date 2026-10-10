package com.brainbucket.user;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 255)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @Column(name = "display_name", nullable = false, length = 50)
  private String displayName;

  @Column(nullable = false, length = 20)
  private String role;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  protected User() {}

  public User(String email, String passwordHash, String displayName) {
    this.email = email;
    this.passwordHash = passwordHash;
    this.displayName = displayName;
    this.role = "USER";
  }

  @PrePersist
  private void beforeInsert() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
    }
  }

  public Long getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getRole() {
    return role;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }
}
