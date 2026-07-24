package cn.iocoder.yudao.module.showroom.workflow.model;

public record ShowroomAssignmentCreate(String targetType, Long targetId, String fieldCode, Long assigneeUserId,
                                       Long assignedBy, Long notifyMessageId) {
}
