package org.example.vibelist.domain.post.comment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.post.comment.dto.CommentCreateDto;
import org.example.vibelist.domain.post.comment.dto.CommentResponseDto;
import org.example.vibelist.domain.post.comment.dto.CommentUpdateDto;
import org.example.vibelist.domain.post.comment.entity.Comment;
import org.example.vibelist.domain.post.comment.repository.CommentRepository;
import org.example.vibelist.domain.post.entity.Post;
import org.example.vibelist.domain.post.repository.PostRepository;
import org.example.vibelist.domain.user.entity.User;
import org.example.vibelist.domain.user.repository.UserRepository;
import org.example.vibelist.global.response.ResponseCode;
import org.example.vibelist.global.response.RsData;
import org.example.vibelist.global.response.GlobalException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
//    private final RedisTemplate<String, String> redisTemplate;


    public RsData<Void> create(CommentCreateDto dto, Long userId) {
        try {
            User user = userRepository.findById(userId).orElseThrow(() -> new GlobalException(ResponseCode.USER_NOT_FOUND, "userId=" + userId + "인 사용자를 찾을 수 없습니다."));
            Post post = postRepository.findById(dto.getPostId())
                    .orElseThrow(() -> new GlobalException(ResponseCode.POST_NOT_FOUND, "postId=" + dto.getPostId() + "인 게시글을 찾을 수 없습니다."));
            Comment parent = null;
            if (dto.getParentId() != null) {
                parent = commentRepository.findById(dto.getParentId())
                        .orElseThrow(() -> new GlobalException(ResponseCode.COMMENT_NOT_FOUND, "parentId=" + dto.getParentId() + "인 부모 댓글을 찾을 수 없습니다."));
            }
            Comment comment = Comment.builder()
                    .content(dto.getContent())
                    .user(user)
                    .post(post)
                    .parent(parent)
                    .build();
            commentRepository.save(comment);
            return RsData.success(ResponseCode.COMMENT_CREATED, null);
        } catch (GlobalException ce) {
            throw ce;
        } catch (Exception e) {
            log.info("[COMMENT_500] 댓글 생성 실패 - userId: {}, dto: {}, error: {}", userId, dto, e.getMessage());
            throw new GlobalException(ResponseCode.INTERNAL_SERVER_ERROR, "댓글 생성 중 오류 - userId=" + userId + ", error=" + e.getMessage());
        }
    }

    public RsData<List<CommentResponseDto>> getByPostId(Long postId) {
        try {
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

            return RsData.success(ResponseCode.COMMENT_UPDATED, new ArrayList<>(map.values()));
        } catch (Exception e) {
            log.info("[COMMENT_500] 댓글 목록 조회 실패 - postId: {}, error: {}", postId, e.getMessage());
            throw new GlobalException(ResponseCode.INTERNAL_SERVER_ERROR, "댓글 목록 조회 중 오류 - postId=" + postId + ", error=" + e.getMessage());
        }
    }

    public RsData<Void> update(Long id, CommentUpdateDto dto, Long userId) {
        try {
            Comment comment = commentRepository.findById(id)
                    .orElseThrow(() -> new GlobalException(ResponseCode.COMMENT_NOT_FOUND, "commentId=" + id + "인 댓글을 찾을 수 없습니다."));
            if (!comment.getUser().getId().equals(userId)) {
                throw new GlobalException(ResponseCode.COMMENT_FORBIDDEN, "댓글 수정 권한 없음 - userId=" + userId + ", commentId=" + id);
            }
            comment.setContent(dto.getContent());
            return RsData.success(ResponseCode.COMMENT_UPDATED, null);
        } catch (GlobalException ce) {
            throw ce;
        } catch (Exception e) {
            log.info("[COMMENT_500] 댓글 수정 실패 - commentId: {}, userId: {}, dto: {}, error: {}", id, userId, dto, e.getMessage());
            throw new GlobalException(ResponseCode.INTERNAL_SERVER_ERROR, "댓글 수정 중 오류 - commentId=" + id + ", userId=" + userId + ", error=" + e.getMessage());
        }
    }

    public RsData<Void> delete(Long id, Long userId) {
        try {
            Comment comment = commentRepository.findById(id)
                    .orElseThrow(() -> new GlobalException(ResponseCode.COMMENT_NOT_FOUND, "commentId=" + id + "인 댓글을 찾을 수 없습니다."));
            if (!comment.getUser().getId().equals(userId)) {
                throw new GlobalException(ResponseCode.COMMENT_FORBIDDEN, "댓글 삭제 권한 없음 - userId=" + userId + ", commentId=" + id);
            }
            commentRepository.delete(comment);
            return RsData.success(ResponseCode.COMMENT_DELETED, null);
        } catch (GlobalException ce) {
            throw ce;
        } catch (Exception e) {
            log.info("[COMMENT_500] 댓글 삭제 실패 - commentId: {}, userId: {}, error: {}", id, userId, e.getMessage());
            throw new GlobalException(ResponseCode.INTERNAL_SERVER_ERROR, "댓글 삭제 중 오류 - commentId=" + id + ", userId=" + userId + ", error=" + e.getMessage());
        }
    }

    public List<CommentResponseDto> getSortedComments(Long postId, String sort) {
        List<Comment> allComments = commentRepository.findAllByPostIdWithUser(postId);

        List<Comment> parents = allComments.stream()
                .filter(c -> c.getParent() == null)
                .collect(Collectors.toList());

        Comparator<Comment> comparator = switch (sort.toLowerCase()) {
            case "oldest" -> Comparator.comparing(Comment::getCreatedAt);
            case "likes" -> Comparator.comparing(Comment::getLikeCount).reversed();
            case "latest" -> Comparator.comparing(Comment::getCreatedAt).reversed();
            default ->  throw new GlobalException(ResponseCode.BAD_REQUEST, "지원하지 않는 정렬 방식: " + sort);
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
                .username(comment.getUser().getUsername())
                .userProfileName(comment.getUser().getUserProfile().getName())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .createdAt(comment.getCreatedAt())
                .children(new ArrayList<>())
                .build();
    }
}
