package cn.iocoder.yudao.module.showroom.workflow.model;

import java.time.Instant;
import java.util.List;

public record ShowroomChangeRequest(Long changeRequestId, String targetType, Long targetId, Long targetRevisionId,
                                    String moduleCode, String requestType, String submissionSource, String status,
                                    String processInstanceId, Long submittedBy, Long submitterDeptId,
                                    Instant submittedAt, Long supervisorUserId, Long supervisorDeptId,
                                    Instant supervisorActionAt, Long gaoxinUserId, Instant gaoxinActionAt,
                                    String rejectionReason,
                                    Long sourceAssignmentId, List<ShowroomChangeRequestItem> items) {

    public ShowroomChangeRequest {
        items = List.copyOf(items);
    }

    public ShowroomChangeRequest(Long changeRequestId, String targetType, Long targetId, Long targetRevisionId,
                                 String moduleCode, String submissionSource, String status, Long submittedBy,
                                 Long submitterDeptId, Long supervisorUserId, Long gaoxinUserId,
                                 Long sourceAssignmentId, List<ShowroomChangeRequestItem> items) {
        this(changeRequestId, targetType, targetId, targetRevisionId, moduleCode, "CONTENT_UPDATE",
                submissionSource, status, null, submittedBy, submitterDeptId, null, supervisorUserId,
                submitterDeptId, null, gaoxinUserId, null, null, sourceAssignmentId, items);
    }

    public ShowroomChangeRequest withStatus(String nextStatus) {
        return new ShowroomChangeRequest(changeRequestId, targetType, targetId, targetRevisionId, moduleCode,
                requestType, submissionSource, nextStatus, processInstanceId, submittedBy, submitterDeptId,
                submittedAt, supervisorUserId, supervisorDeptId, supervisorActionAt, gaoxinUserId,
                gaoxinActionAt, rejectionReason, sourceAssignmentId, items);
    }

}
