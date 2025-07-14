package org.example.vibelist.domain.comment.controller;

import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.comment.dto.CommentCreateDto;
import org.example.vibelist.domain.comment.dto.CommentResponseDto;
import org.example.vibelist.domain.comment.dto.CommentUpdateDto;
import org.example.vibelist.domain.comment.service.CommentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<Void> create(@RequestBody CommentCreateDto dto) {
        commentService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<List<CommentResponseDto>> getAll(
            @RequestParam Long postId,
            @RequestParam(defaultValue = "latest") String sort
    ) {
        List<CommentResponseDto> comments = commentService.getSortedComments(postId, sort);
        return ResponseEntity.ok(commentService.getByPostId(postId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @RequestBody CommentUpdateDto dto) {
        String username = getCurrentUsername();
        commentService.update(id, dto, username);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        String username = getCurrentUsername();
        commentService.delete(id, username);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<Void> like(@PathVariable Long id) {
        String username = getCurrentUsername();
        commentService.likeComment(id, username);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/like")
    public ResponseEntity<Void> unlike(@PathVariable Long id) {
        String username = getCurrentUsername();
        commentService.unlikeComment(id, username);
        return ResponseEntity.ok().build();
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}
