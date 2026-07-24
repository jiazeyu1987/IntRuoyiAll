package cn.iocoder.yudao.module.showroom.workflow.model;

public record ShowroomFieldAssignment(Long assignmentId, String targetType, Long targetId, String fieldCode,
                                      Long assigneeUserId, Long assignedBy, String status, Long notifyMessageId,
                                      Long lastSavedRevisionId, Long lastChangeRequestId) {

    public ShowroomFieldAssignment autoSubmitted(Long savedRevisionId, Long changeRequestId) {
        return new ShowroomFieldAssignment(assignmentId, targetType, targetId, fieldCode, assigneeUserId, assignedBy,
                "AUTO_SUBMITTED", notifyMessageId, savedRevisionId, changeRequestId);
    }

}
