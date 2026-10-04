package cn.iocoder.yudao.module.dcc.service.file;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.List;

public record DccSignoffAssignmentContext(
        @JsonFormat(shape=JsonFormat.Shape.STRING) Long controlledFileId,
        String processInstanceId, String taskId, String obligationId,
        @JsonFormat(shape=JsonFormat.Shape.STRING) Long departmentId, String departmentName,
        @JsonFormat(shape=JsonFormat.Shape.STRING) Long assigneeUserId,
        boolean assigned, boolean canAssign, List<AssigneeOption> assigneeOptions) {
    public record AssigneeOption(@JsonFormat(shape=JsonFormat.Shape.STRING) Long id,String name) {}
}
