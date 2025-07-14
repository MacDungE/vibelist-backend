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
import org.example.vibelist.global.user.entity.User;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

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
                .user(dto.getUser())
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

    public void update(Long id, CommentUpdateDto dto, Long userId) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.COMMENT_FORBIDDEN);
        }

        comment.setContent(dto.getContent());
    }

    public void delete(Long id, Long userId) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.COMMENT_FORBIDDEN);
        }

        commentRepository.delete(comment);
    }

    public List<CommentResponseDto> getSortedComments(Long postId, String sort) {
        List<Comment> allComments = commentRepository.findByPostId(postId);

        List<Comment> parents = allComments.stream()
                .filter(c -> c.getParent() == null)
                .collect(Collectors.toList());

        Comparator<Comment> comparator = switch (sort.toLowerCase()) {
            case "oldest" -> Comparator.comparing(Comment::getCreatedAt);
            case "likes" -> Comparator.comparing(Comment::getLikeCount).reversed();
            case "latest" -> Comparator.comparing(Comment::getCreatedAt).reversed();
            default ->  throw new CustomException(ErrorCode.BAD_REQUEST);
        };
        parents.sort(comparator);

        Map<Long, List<Comment>> childMap = allComments.stream()
                .filter(c -> c.getParent() != null)
                .collect(Collectors.groupingBy(c -> c.getParent().getId()));

        List<CommentResponseDto> result = new ArrayList<>();
        for (Comment parent : parents) {
            CommentResponseDto dto = toDto(parent);
            List<Comment> children = childMap.getOrDefault(parent.getId(), List.of());
            for (Comment child : children) {
                dto.getChildren().add(toDto(child));
            }
            result.add(dto);
        }
        return result;
    }

    // @todo 댓글 좋아요 w/ Redis
    public void likeComment(Long commentId, User user) {

    }

    // @todo 댓글 좋아요 취소 w/ Redis
    public void unlikeComment(Long commentId, User user) {

    }

    private CommentResponseDto toDto(Comment comment) {
        return CommentResponseDto.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .userId(comment.getUser().getId())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .createdAt(comment.getCreatedAt())
                .children(new ArrayList<>())
                .build();
    }
}
