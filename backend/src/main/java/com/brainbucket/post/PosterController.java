package com.brainbucket.post;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/posts/mine")
public class PosterController {

    private final PosterService posterService;

    public PosterController(PosterService posterService) {
        this.posterService = posterService;
    }

    @PostMapping(
            value = "/{postId}/poster",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public UploadPosterResponse upload(
            Authentication authentication,
            @PathVariable Long postId,
            @RequestParam("file") MultipartFile file
    ) {
        return posterService.upload(
                authentication.getName(),
                postId,
                file
        );
    }
    @GetMapping("/{postId}/poster")
    public ResponseEntity<Resource> getMyPoster(
            Authentication authentication,
            @PathVariable Long postId
    ) {
        var poster = posterService.getMyPoster(
                authentication.getName(),
                postId
        );

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(poster.contentType()))
                .cacheControl(CacheControl.noStore())
                .body(poster.resource());
    }
    @PutMapping(
            value = "/{postId}/poster",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public UploadPosterResponse replace(
            Authentication authentication,
            @PathVariable Long postId,
            @RequestParam("file") MultipartFile file
    ) {
        return posterService.replace(
                authentication.getName(),
                postId,
                file
        );
    }
}