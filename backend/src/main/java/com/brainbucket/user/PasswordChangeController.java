package com.brainbucket.user;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
@RestController
@RequestMapping("/api/auth")
public class PasswordChangeController {
 private final PasswordChangeService service;
 public PasswordChangeController(PasswordChangeService service){this.service=service;}
 public record Input(String currentPassword,String newPassword) {}
 @PostMapping("/password") @ResponseStatus(HttpStatus.NO_CONTENT)
 public void change(Authentication auth,@RequestBody Input input,HttpServletRequest request,HttpServletResponse response){
  service.change(auth.getName(),input.currentPassword(),input.newPassword());
  new SecurityContextLogoutHandler().logout(request,response,auth);
 }
}
