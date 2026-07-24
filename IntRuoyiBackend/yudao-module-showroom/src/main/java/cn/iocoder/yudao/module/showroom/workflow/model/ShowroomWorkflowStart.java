package cn.iocoder.yudao.module.showroom.workflow.model;

import java.util.List;

public record ShowroomWorkflowStart(String targetType, Long targetId, Long targetRevisionId, String moduleCode,
                                    String requestType, String submissionSource, String processInstanceId,
                                    Long submittedBy, Long submitterDeptId, Long supervisorUserId,
                                    Long gaoxinUserId, Long sourceAssignmentId, List<ShowroomChangeRequestItem> items) {

    public ShowroomWorkflowStart(String targetType, Long targetId, Long targetRevisionId, String moduleCode,
                                 String submissionSource, Long submittedBy, Long submitterDeptId,
                                 Long supervisorUserId, Long gaoxinUserId, Long sourceAssignmentId,
                                 List<ShowroomChangeRequestItem> items) {
        this(targetType, targetId, targetRevisionId, moduleCode, "CONTENT_UPDATE",
                submissionSource, null, submittedBy, submitterDeptId, supervisorUserId, gaoxinUserId,
                sourceAssignmentId, items);
    }
}
