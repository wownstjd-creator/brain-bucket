package com.brainbucket.user;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Service
public class PasswordChangeService {
 private final UserRepository users; private final PasswordEncoder encoder;
 public PasswordChangeService(UserRepository users,PasswordEncoder encoder){this.users=users;this.encoder=encoder;}
 @Transactional
 public void change(String email,String current,String next){
  if(current==null||next==null||next.length()<8||next.getBytes(StandardCharsets.UTF_8).length>72||current.getBytes(StandardCharsets.UTF_8).length>72)
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"새 비밀번호는 8자 이상, UTF-8 기준 72바이트 이하여야 합니다.");
  var user=users.findForPasswordChange(email).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));
  if(!encoder.matches(current,user.getPasswordHash()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"현재 비밀번호가 올바르지 않습니다.");
  if(current.equals(next))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"현재와 다른 비밀번호를 입력해주세요.");
  user.changePassword(encoder.encode(next));users.flush();
 }
}
