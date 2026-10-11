package com.brainbucket.post;

import com.brainbucket.user.*;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class BookmarkService {
  private final JdbcTemplate db;
  private final UserRepository users;
  private final PostRepository posts;

  public BookmarkService(JdbcTemplate db, UserRepository u, PostRepository p) {
    this.db = db;
    users = u;
    posts = p;
  }

  private Long user(String email) {
    return users
        .findByEmail(email)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED))
        .getId();
  }

  public void save(String email, Long id) {
    Long uid = user(email);
    db.queryForObject("select id from users where id=? for update", Long.class, uid);
    var p =
        posts
            .findForReview(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (!p.isPublic(LocalDate.now(ZoneId.of("Asia/Seoul"))))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    if (db.queryForObject(
            "select count(*) from bookmarks where user_id=? and post_id=?", Long.class, uid, id)
        == 0)
      db.update(
          "insert into bookmarks(user_id,post_id,created_at) values(?,?,?)",
          uid,
          id,
          LocalDateTime.now(ZoneId.of("Asia/Seoul")));
  }

  public void remove(String email, Long id) {
    db.update("delete from bookmarks where user_id=? and post_id=?", user(email), id);
  }

  @Transactional(readOnly = true)
  public List<Long> ids(String email) {
    return db.queryForList(
        "select b.post_id from bookmarks b join posts p on p.id=b.post_id where b.user_id=? and p.deleted_at is null order by b.created_at desc,b.post_id desc",
        Long.class,
        user(email));
  }

  @Transactional(readOnly = true)
  public List<PublicPostResponse> list(String email, int page, int size) {
    if (page < 0 || size < 1 || size > 50)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    return list(email,page,size,null,null,null);
  }

  @Transactional(readOnly=true)
  public List<PublicPostResponse> list(String email,int page,int size,String search,String category,Integer year){
    if(page<0||size<1||size>50||search!=null&&search.length()>200||year!=null&&(year<1900||year>9998)
        ||category!=null&&!Set.of("CLUB","CONTEST","EVENT","NOTICE","OTHER").contains(category))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    var args=new ArrayList<Object>();args.add(user(email));args.add(LocalDate.now(ZoneId.of("Asia/Seoul")));
    String sql="select p.id from bookmarks b join posts p on p.id=b.post_id where b.user_id=? and p.status='APPROVED' and p.deleted_at is null and p.display_until is not null and (p.display_from is null or p.display_from<=?)";
    if(category!=null){sql+=" and p.category=?";args.add(category);}
    if(year!=null){sql+=" and p.display_until>=? and p.display_until<?";args.add(LocalDate.of(year,1,1));args.add(LocalDate.of(year+1,1,1));}
    if(search!=null&&!search.isBlank()){
      sql+=" and (lower(p.title) like ? escape '!' or lower(p.organizer) like ? escape '!' or lower(p.description) like ? escape '!')";
      String pattern="%"+search.strip().toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%";
      args.add(pattern);args.add(pattern);args.add(pattern);
    }
    sql+=" order by b.created_at desc,p.id desc limit ? offset ?";args.add(size);args.add((long)page*size);
    List<Long> selected=db.queryForList(sql,Long.class,args.toArray());
    Map<Long,Post> found=new HashMap<>();posts.findAllById(selected).forEach(p->found.put(p.getId(),p));
    return selected.stream().map(found::get).filter(Objects::nonNull).map(PublicPostResponse::from).toList();
  }
}
