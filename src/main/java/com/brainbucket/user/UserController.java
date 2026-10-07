package com.brainbucket.user;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;

    public UserController(
            UserRepository userRepository,
            UserService userService
    ) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @GetMapping("/api/dev/users/count")
    public Map<String, Long> countUsers() {
        return Map.of("count", userRepository.count());
    }

    @PostMapping("/api/auth/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public SignupResponse signup(
            @Valid @RequestBody SignupRequest request
    ) {
        return userService.signup(request);
    }
}