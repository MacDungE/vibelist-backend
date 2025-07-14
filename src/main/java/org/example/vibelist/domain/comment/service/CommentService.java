package org.example.vibelist.domain.comment.service;

import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.comment.dto.CommentCreateDto;
import org.example.vibelist.domain.comment.dto.CommentResponseDto;
import org.example.vibelist.domain.comment.dto.CommentUpdateDto;
import org.example.vibelist.domain.comment.entity.Comment;
import org.example.vibelist.domain.comment.repository.CommentRepository;
import org.example.vibelist.domain.post.entity.Post;
import org.example.vibelist.domain.post.repository.PostRepository;
import org.example.vibelist.global.exception.CustomException;
import org.example.vibelist.global.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
//    private final RedisTemplate<String, String> redisTemplate;


    public void create(CommentCreateDto dto) {
        Post post = postRepository.findById(dto.getPostId())
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        Comment parent = null;
        if (dto.getParentId() != null) {
            parent = commentRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
        }

        Comment comment = Comment.builder()
                .content(dto.getContent())
                .username(dto.getUsername())
                .post(post)
                .parent(parent)
                .build();

        commentRepository.save(comment);
    }

    public List<CommentResponseDto> getByPostId(Long postId) {
        List<Comment> comments = commentRepository.findByPostId(postId);
        Map<Long, CommentResponseDto> map = new LinkedHashMap<>();

        // 먼저 부모 댓글만 등록
        comments.stream().filter(c -> c.getParent() == null).forEach(c -> {
            map.put(c.getId(), toDto(c));
        });

        // 자식 댓글을 부모에 추가
        comments.stream().filter(c -> c.getParent() != null).forEach(c -> {
            CommentResponseDto parentDto = map.get(c.getParent().getId());
            if (parentDto != null) {
                parentDto.getChildren().add(toDto(c));
            }
        });

        return new ArrayList<>(map.values());
    }

    public void update(Long id, CommentUpdateDto dto, String username) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getUsername().equals(username)) {
            throw new CustomException(ErrorCode.COMMENT_FORBIDDEN);
        }

        comment.setContent(dto.getContent());
    }

    public void delete(Long id, String username) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getUsername().equals(username)) {
            throw new CustomException(ErrorCode.COMMENT_NOT_FOUND);
        }

        commentRepository.delete(comment);
    }

    public List<CommentResponseDto> getSortedComments(Long postId, String sort) {
        List<Comment> allComments = commentRepository.findByPostId(postId);

    }

    // @todo 댓글 좋아요 w/ Redis
    public void likeComment(Long commentId, String username) {

    }

    // @todo 댓글 좋아요 취소 w/ Redis
    public void unlikeComment(Long commentId, String username) {

    }

    private CommentResponseDto toDto(Comment comment) {
        return CommentResponseDto.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .username(comment.getUsername())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .createdAt(comment.getCreatedAt())
                .children(new ArrayList<>())
                .build();
    }
}
