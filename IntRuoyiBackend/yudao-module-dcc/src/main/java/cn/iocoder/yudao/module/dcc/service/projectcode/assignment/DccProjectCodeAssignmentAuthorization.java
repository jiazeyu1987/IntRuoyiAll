package cn.iocoder.yudao.module.dcc.service.projectcode.assignment;

import static cn.iocoder.yudao.module.dcc.enums.DccProjectCodeAssignmentConstants.SOURCE_ASSIGNMENT_USER;
import static cn.iocoder.yudao.module.dcc.enums.DccProjectCodeAssignmentConstants.SOURCE_DOC_CONTROL_DIRECT;

public record DccProjectCodeAssignmentAuthorization(Long assignmentId, Long projectCodeId, String source) {

    public static DccProjectCodeAssignmentAuthorization docControlDirect(Long projectCodeId) {
        return new DccProjectCodeAssignmentAuthorization(null, projectCodeId, SOURCE_DOC_CONTROL_DIRECT);
    }

    public static DccProjectCodeAssignmentAuthorization assignedUser(Long assignmentId, Long projectCodeId) {
        return new DccProjectCodeAssignmentAuthorization(assignmentId, projectCodeId, SOURCE_ASSIGNMENT_USER);
    }

}
