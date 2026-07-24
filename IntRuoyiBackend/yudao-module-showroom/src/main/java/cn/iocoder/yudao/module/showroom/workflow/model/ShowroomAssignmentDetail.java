package cn.iocoder.yudao.module.showroom.workflow.model;

public record ShowroomAssignmentDetail(Long assignmentId, String targetType, Long targetId, String fieldCode,
                                       Long assigneeUserId, Long assignedBy, String status, Long notifyMessageId,
                                       String notifyTemplateCode, String notifyContent, String currentDraftValue,
                                       Long lastSavedRevisionId, Long lastChangeRequestId,
                                       String latestChangeRequestStatus) {
}
