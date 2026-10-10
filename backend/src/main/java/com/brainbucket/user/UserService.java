package com.brainbucket.user;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  public SignupResponse signup(SignupRequest request) {

    String email = request.email().trim().toLowerCase(Locale.ROOT);
    String displayName = request.displayName().trim();
    String password = request.password();

    if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "비밀번호는 UTF-8 기준 72바이트 이하여야 합니다.");
    }

    if (userRepository.existsByEmail(email)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 가입된 이메일입니다.");
    }

    String passwordHash = passwordEncoder.encode(password);

    User user = new User(email, passwordHash, displayName);

    User savedUser;
    try {
      savedUser = userRepository.saveAndFlush(user);
    } catch (DataIntegrityViolationException exception) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "회원 정보를 저장할 수 없습니다. 이메일 중복 여부를 확인해주세요.");
    }

    return new SignupResponse(
        savedUser.getId(), savedUser.getEmail(), savedUser.getDisplayName(), savedUser.getRole());
  }
}
