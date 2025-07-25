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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

//    public RsData<List<CommentResponseDto>> getByPostId(Long postId) {
//        try {
//            List<Comment> comments = commentRepository.findByPostId(postId);
//            Map<Long, CommentResponseDto> map = new LinkedHashMap<>();
//
//            // 먼저 부모 댓글만 등록
//            comments.stream().filter(c -> c.getParent() == null).forEach(c -> {
//                map.put(c.getId(), toDto(c));
//            });
//
//            // 자식 댓글을 부모에 추가
//            comments.stream().filter(c -> c.getParent() != null).forEach(c -> {
//                CommentResponseDto parentDto = map.get(c.getParent().getId());
//                if (parentDto != null) {
//                    parentDto.getChildren().add(toDto(c));
//                }
//            });
//
//            return RsData.success(ResponseCode.COMMENT_UPDATED, new ArrayList<>(map.values()));
//        } catch (Exception e) {
//            log.info("[COMMENT_500] 댓글 목록 조회 실패 - postId: {}, error: {}", postId, e.getMessage());
//            throw new GlobalException(ResponseCode.INTERNAL_SERVER_ERROR, "댓글 목록 조회 중 오류 - postId=" + postId + ", error=" + e.getMessage());
//        }
//    }

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

    @Transactional
    public RsData<Void> deleteAllCommentsByUserId(Long userId) {
        try {
            // 1. 해당 사용자의 모든 댓글 조회
            List<Comment> userComments = commentRepository.findByUserId(userId);

            if (userComments.isEmpty()) {
                log.info("[COMMENT_501] 삭제할 댓글이 없음 - userId: {}", userId);
                return RsData.success(ResponseCode.COMMENT_DELETED, null);
            }

            log.info("[COMMENT_502] 사용자 댓글 일괄 삭제 시작 - userId: {}, commentCount: {}", userId, userComments.size());

            // 2. 각 댓글 삭제
            int successCount = 0;
            int failCount = 0;

            for (Comment comment : userComments) {
                try {
                    commentRepository.delete(comment);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    log.error("[COMMENT_503] 댓글 삭제 실패 - commentId: {}, userId: {}, error: {}",
                            comment.getId(), userId, e.getMessage());
                }
            }

            log.info("[COMMENT_504] 사용자 댓글 일괄 삭제 완료 - userId: {}, 성공: {}, 실패: {}",
                    userId, successCount, failCount);

            return RsData.success(ResponseCode.COMMENT_DELETED, null);

        } catch (Exception e) {
            log.error("[COMMENT_500] 사용자 댓글 일괄 삭제 오류 - userId: {}, error: {}", userId, e.getMessage());
            throw new GlobalException(ResponseCode.INTERNAL_SERVER_ERROR,
                    "사용자 댓글 삭제 중 오류 - userId=" + userId + ", error=" + e.getMessage());
        }
    }

    public RsData<List<CommentResponseDto>> getSortedComments(Long postId, String sort) {
        try {
            // 1. 정렬 옵션 준비
            Sort dbSort = switch (sort.toLowerCase()) {
                case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
                case "likes"  -> Sort.by(Sort.Direction.DESC, "likeCount");
                case "latest" -> Sort.by(Sort.Direction.DESC, "createdAt");
                default -> throw new GlobalException(ResponseCode.BAD_REQUEST, "지원하지 않는 정렬 방식: " + sort);
            };

            // 2. 부모 댓글만 정렬·페이징해서 가져옴
            List<Comment> parents = commentRepository.findParentByPostId(postId, dbSort);
            if (parents.isEmpty()) {
                return RsData.success(ResponseCode.COMMENT_UPDATED, List.of());
            }

            List<Long> parentIds = parents.stream().map(Comment::getId).toList();

            // 3. 자식 댓글 한 번에 IN 쿼리로
            List<Comment> children = commentRepository.findChildByParentIds(parentIds);

            // 4. 자식 댓글 그룹핑
            Map<Long, List<Comment>> childMap = children.stream()
                    .collect(Collectors.groupingBy(c -> c.getParent().getId()));

            // 5. 조립
            List<CommentResponseDto> result = new ArrayList<>();
            for (Comment parent : parents) {
                CommentResponseDto dto = toDto(parent);
                List<Comment> childList = childMap.getOrDefault(parent.getId(), List.of());
                // 자식 정렬(필요하면)
                childList = childList.stream()
                        .sorted(Comparator.comparing(Comment::getCreatedAt))
                        .toList();
                for (Comment child : childList) {
                    dto.getChildren().add(toDto(child));
                }
                result.add(dto);
            }

            return RsData.success(ResponseCode.COMMENT_UPDATED, result);
        } catch (Exception e) {
            log.info("[COMMENT_500] 댓글 정렬 목록 조회 실패 - postId: {}, sort: {}, error: {}", postId, sort, e.getMessage());
            throw new GlobalException(ResponseCode.INTERNAL_SERVER_ERROR, "댓글 정렬 목록 조회 중 오류 - postId=" + postId + ", sort=" + sort + ", error=" + e.getMessage());
        }
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
