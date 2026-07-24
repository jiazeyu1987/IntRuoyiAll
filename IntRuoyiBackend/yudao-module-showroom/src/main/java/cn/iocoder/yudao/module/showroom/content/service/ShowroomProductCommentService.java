package cn.iocoder.yudao.module.showroom.content.service;

import cn.iocoder.yudao.module.showroom.content.model.ShowroomCommentAnchorType;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductComment;
import cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomProductCommentDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.workflow.ShowroomChangeRequestDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.workflow.ShowroomFieldAssignmentDO;
import cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomProductCommentMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.workflow.ShowroomChangeRequestMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.workflow.ShowroomFieldAssignmentMapper;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomChangeRequest;
import cn.iocoder.yudao.module.showroom.workflow.service.ShowroomPersistentWorkflowService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ShowroomProductCommentService {

    private static final String STATUS_OPEN = "OPEN";
    private static final String STATUS_RESOLVED = "RESOLVED";
    private static final String TARGET_PRODUCT = "PRODUCT";
    private static final String DISCUSSION_ACCESS_DENIED =
            "SHOWROOM_DISCUSSION_ACCESS_DENIED: product discussion is visible only to participating modifiers";

    private final ShowroomProductCommentMapper commentMapper;
    private final ShowroomChangeRequestMapper changeRequestMapper;
    private final ShowroomFieldAssignmentMapper assignmentMapper;
    private final ShowroomPersistentContentService contentService;
    private final ShowroomPersistentWorkflowService workflowService;

    public ShowroomProductCommentService(ShowroomProductCommentMapper commentMapper,
                                         ShowroomChangeRequestMapper changeRequestMapper,
                                         ShowroomFieldAssignmentMapper assignmentMapper,
                                         ShowroomPersistentContentService contentService,
                                         ShowroomPersistentWorkflowService workflowService) {
        this.commentMapper = commentMapper;
        this.changeRequestMapper = changeRequestMapper;
        this.assignmentMapper = assignmentMapper;
        this.contentService = contentService;
        this.workflowService = workflowService;
    }

    @Transactional(rollbackFor = Exception.class)
    public ShowroomProductComment createThread(Long productId, Long targetRevisionId, Long changeRequestId,
                                               ShowroomCommentAnchorType anchorType, String anchorKey,
                                               Long createdBy, String content) {
        requireNonNull(productId, "SHOWROOM_TARGET_NOT_FOUND: product id is required");
        requireNonNull(createdBy, "SHOWROOM_ROLE_BINDING_MISSING: comment author is required");
        requireText(content, "SHOWROOM_REQUIRED_FIELD_MISSING: comment content is required");
        contentService.getProduct(productId);
        AnchorResolution anchor = resolveAnchor(productId, targetRevisionId, changeRequestId, anchorType, anchorKey);
        ShowroomProductCommentDO comment = ShowroomProductCommentDO.builder()
                .productId(productId)
                .targetRevisionId(anchor.targetRevisionId())
                .changeRequestId(anchor.changeRequestId())
                .parentCommentId(null)
                .anchorType(anchor.anchorType().name())
                .anchorKey(anchor.anchorKey())
                .content(content)
                .status(STATUS_OPEN)
                .createdBy(createdBy)
                .createdAt(LocalDateTime.now())
                .build();
        commentMapper.insert(comment);
        return toComment(comment);
    }

    @Transactional(rollbackFor = Exception.class)
    public ShowroomProductComment createThreadVisible(Long productId, Long targetRevisionId, Long changeRequestId,
                                                      ShowroomCommentAnchorType anchorType, String anchorKey,
                                                      Long operatorUserId, String content) {
        requireDiscussionAccess(productId, operatorUserId);
        return createThread(productId, targetRevisionId, changeRequestId, anchorType, anchorKey,
                operatorUserId, content);
    }

    @Transactional(rollbackFor = Exception.class)
    public ShowroomProductComment reply(Long parentCommentId, Long createdBy, String content) {
        ShowroomProductCommentDO parent = requireComment(parentCommentId);
        if (STATUS_RESOLVED.equals(parent.getStatus())) {
            throw new IllegalStateException("SHOWROOM_DISCUSSION_TARGET_INVALID: resolved thread cannot be replied");
        }
        requireNonNull(createdBy, "SHOWROOM_ROLE_BINDING_MISSING: reply author is required");
        requireText(content, "SHOWROOM_REQUIRED_FIELD_MISSING: reply content is required");
        ShowroomProductCommentDO reply = ShowroomProductCommentDO.builder()
                .productId(parent.getProductId())
                .targetRevisionId(parent.getTargetRevisionId())
                .changeRequestId(parent.getChangeRequestId())
                .parentCommentId(parent.getId())
                .anchorType(parent.getAnchorType())
                .anchorKey(parent.getAnchorKey())
                .content(content)
                .status(STATUS_OPEN)
                .createdBy(createdBy)
                .createdAt(LocalDateTime.now())
                .build();
        commentMapper.insert(reply);
        return toComment(reply);
    }

    @Transactional(rollbackFor = Exception.class)
    public ShowroomProductComment replyVisible(Long parentCommentId, Long operatorUserId, String content) {
        ShowroomProductCommentDO parent = requireComment(parentCommentId);
        requireDiscussionAccess(parent.getProductId(), operatorUserId);
        return reply(parentCommentId, operatorUserId, content);
    }

    @Transactional(rollbackFor = Exception.class)
    public ShowroomProductComment resolve(Long commentId, Long resolvedBy) {
        ShowroomProductCommentDO root = requireComment(commentId);
        requireNonNull(resolvedBy, "SHOWROOM_ROLE_BINDING_MISSING: resolver is required");
        LocalDateTime now = LocalDateTime.now();
        Set<Long> threadCommentIds = collectThreadCommentIds(root.getProductId(), root.getId(),
                root.getAnchorType(), root.getAnchorKey(), root.getChangeRequestId());
        for (Long id : threadCommentIds) {
            ShowroomProductCommentDO comment = commentMapper.selectById(id);
            comment.setStatus(STATUS_RESOLVED);
            comment.setResolvedBy(resolvedBy);
            comment.setResolvedAt(now);
            commentMapper.updateById(comment);
        }
        return toComment(commentMapper.selectById(root.getId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public ShowroomProductComment resolveVisible(Long commentId, Long operatorUserId) {
        ShowroomProductCommentDO root = requireComment(commentId);
        requireDiscussionAccess(root.getProductId(), operatorUserId);
        return resolve(commentId, operatorUserId);
    }

    public List<ShowroomProductComment> pageByProduct(Long productId, ShowroomCommentAnchorType anchorType,
                                                      String anchorKey, Long changeRequestId) {
        return pageByProduct(productId, anchorType, anchorKey, changeRequestId, null);
    }

    public List<ShowroomProductComment> pageByProduct(Long productId, ShowroomCommentAnchorType anchorType,
                                                      String anchorKey, Long changeRequestId, String status) {
        requireNonNull(productId, "SHOWROOM_TARGET_NOT_FOUND: product id is required");
        List<ShowroomProductComment> comments = commentMapper.selectListByProductId(productId).stream()
                .filter(comment -> anchorType == null || anchorType.name().equals(comment.getAnchorType()))
                .filter(comment -> anchorKey == null || anchorKey.equals(comment.getAnchorKey()))
                .filter(comment -> changeRequestId == null || changeRequestId.equals(comment.getChangeRequestId()))
                .filter(comment -> status == null || status.equals(comment.getStatus()))
                .sorted(Comparator.comparing(ShowroomProductCommentDO::getCreatedAt)
                        .thenComparing(ShowroomProductCommentDO::getId))
                .map(this::toComment)
                .toList();
        return List.copyOf(comments);
    }

    public List<ShowroomProductComment> pageByProductVisible(Long productId, ShowroomCommentAnchorType anchorType,
                                                             String anchorKey, Long changeRequestId, String status,
                                                             Long operatorUserId) {
        requireDiscussionAccess(productId, operatorUserId);
        return pageByProduct(productId, anchorType, anchorKey, changeRequestId, status);
    }

    private AnchorResolution resolveAnchor(Long productId, Long targetRevisionId, Long changeRequestId,
                                           ShowroomCommentAnchorType anchorType, String anchorKey) {
        requireNonNull(anchorType, "SHOWROOM_DISCUSSION_TARGET_INVALID: anchor type is required");
        if (anchorType == ShowroomCommentAnchorType.CHANGE_REQUEST) {
            if (changeRequestId == null || hasText(anchorKey)) {
                throw new IllegalStateException(
                        "SHOWROOM_DISCUSSION_TARGET_INVALID: change request anchor requires only change_request_id");
            }
            ShowroomChangeRequest request = workflowService.getChangeRequest(changeRequestId);
            if (!TARGET_PRODUCT.equals(request.targetType()) || !productId.equals(request.targetId())) {
                throw new IllegalStateException(
                        "SHOWROOM_DISCUSSION_TARGET_INVALID: change request does not belong to product");
            }
            Long resolvedRevisionId = targetRevisionId == null ? request.targetRevisionId() : targetRevisionId;
            if (!request.targetRevisionId().equals(resolvedRevisionId)) {
                throw new IllegalStateException(
                        "SHOWROOM_DISCUSSION_TARGET_INVALID: change request revision mismatch");
            }
            return new AnchorResolution(anchorType, resolvedRevisionId, changeRequestId, null);
        }
        if (!hasText(anchorKey) || changeRequestId != null) {
            throw new IllegalStateException(
                    "SHOWROOM_DISCUSSION_TARGET_INVALID: field or module anchor requires only anchor_key");
        }
        requireNonNull(targetRevisionId, "SHOWROOM_TARGET_NOT_FOUND: target revision id is required");
        contentService.getProductRevision(targetRevisionId);
        return new AnchorResolution(anchorType, targetRevisionId, null, anchorKey);
    }

    private Set<Long> collectThreadCommentIds(Long productId, Long rootCommentId, String anchorType, String anchorKey,
                                              Long changeRequestId) {
        Map<Long, ShowroomProductCommentDO> commentMap = commentMapper.selectListByProductId(productId).stream()
                .filter(comment -> anchorType.equals(comment.getAnchorType()))
                .filter(comment -> anchorKey == null ? comment.getAnchorKey() == null
                        : anchorKey.equals(comment.getAnchorKey()))
                .filter(comment -> changeRequestId == null ? comment.getChangeRequestId() == null
                        : changeRequestId.equals(comment.getChangeRequestId()))
                .collect(Collectors.toMap(ShowroomProductCommentDO::getId, comment -> comment, (left, right) -> left,
                        LinkedHashMap::new));
        Set<Long> collected = new java.util.LinkedHashSet<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        queue.add(rootCommentId);
        while (!queue.isEmpty()) {
            Long currentId = queue.removeFirst();
            if (!collected.add(currentId)) {
                continue;
            }
            for (ShowroomProductCommentDO comment : commentMap.values()) {
                if (currentId.equals(comment.getParentCommentId()) && !collected.contains(comment.getId())) {
                    queue.add(comment.getId());
                }
            }
        }
        return collected;
    }

    private ShowroomProductCommentDO requireComment(Long commentId) {
        requireNonNull(commentId, "SHOWROOM_TARGET_NOT_FOUND: comment id is required");
        ShowroomProductCommentDO comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: product comment not found");
        }
        return comment;
    }

    private void requireDiscussionAccess(Long productId, Long operatorUserId) {
        requireNonNull(productId, "SHOWROOM_TARGET_NOT_FOUND: product id is required");
        requireNonNull(operatorUserId, "SHOWROOM_ROLE_BINDING_MISSING: discussion operator is required");
        contentService.getProduct(productId);
        if (!isDiscussionParticipant(productId, operatorUserId)) {
            throw new IllegalStateException(DISCUSSION_ACCESS_DENIED);
        }
    }

    private boolean isDiscussionParticipant(Long productId, Long operatorUserId) {
        return commentMapper.selectListByProductId(productId).stream()
                .anyMatch(comment -> operatorUserId.equals(comment.getCreatedBy())
                        || operatorUserId.equals(comment.getResolvedBy()))
                || changeRequestMapper.selectListByTarget(TARGET_PRODUCT, productId).stream()
                .anyMatch(request -> operatorUserId.equals(request.getSubmittedBy())
                        || operatorUserId.equals(request.getSupervisorUserId())
                        || operatorUserId.equals(request.getGaoxinUserId()))
                || assignmentMapper.selectListByTarget(TARGET_PRODUCT, productId).stream()
                .anyMatch(assignment -> operatorUserId.equals(assignment.getAssignedBy())
                        || operatorUserId.equals(assignment.getAssigneeUserId()));
    }

    private ShowroomProductComment toComment(ShowroomProductCommentDO comment) {
        return new ShowroomProductComment(comment.getId(), comment.getProductId(), comment.getTargetRevisionId(),
                comment.getChangeRequestId(), comment.getParentCommentId(),
                ShowroomCommentAnchorType.valueOf(comment.getAnchorType()), comment.getAnchorKey(),
                comment.getContent(), comment.getStatus(), comment.getCreatedBy(), comment.getResolvedBy());
    }

    private static void requireNonNull(Object value, String message) {
        if (value == null) {
            throw new IllegalStateException(message);
        }
    }

    private static String requireText(String value, String message) {
        if (!hasText(value)) {
            throw new IllegalStateException(message);
        }
        return value;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record AnchorResolution(ShowroomCommentAnchorType anchorType, Long targetRevisionId,
                                    Long changeRequestId, String anchorKey) {
    }

}
