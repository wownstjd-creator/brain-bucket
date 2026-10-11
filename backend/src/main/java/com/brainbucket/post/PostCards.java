package com.brainbucket.post;
import java.util.*;
public final class PostCards {
 private PostCards(){}
 public static MyPostDetailResponse from(Post p,Set<String> kinds){return new MyPostDetailResponse(
  p.getId(),p.getTitle(),p.getOrganizer(),p.getCategory(),p.getDescription(),p.getLocation(),p.getApplicationUrl(),p.getEventAt(),p.getDeadlineAt(),p.getDisplayUntil(),p.getDisplayFrom(),p.getVersion(),kinds.contains("EVIDENCE"),kinds.contains("ORIGINAL"),p.getStatus(),p.isShowViewCount(),p.getViewCount(),p.getPublishedAt(),p.getCreatedAt(),p.getUpdatedAt());}
 public static List<MyPostDetailResponse> from(List<Post> posts,PostFileRepository files){
  if(posts.isEmpty())return List.of();
  Map<Long,Set<String>> kinds=new HashMap<>();
  for(var f:files.findByPost_IdIn(posts.stream().map(Post::getId).toList()))kinds.computeIfAbsent(f.getPost().getId(),id->new HashSet<>()).add(f.getFileKind());
  return posts.stream().map(p->from(p,kinds.getOrDefault(p.getId(),Set.of()))).toList();
 }
}
