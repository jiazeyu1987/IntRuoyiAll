package cn.iocoder.yudao.module.showroom.content.model;

public record ShowroomProductComment(Long commentId, Long productId, Long targetRevisionId, Long changeRequestId,
                                     Long parentCommentId, ShowroomCommentAnchorType anchorType, String anchorKey,
                                     String content, String status, Long createdBy, Long resolvedBy) {
}
