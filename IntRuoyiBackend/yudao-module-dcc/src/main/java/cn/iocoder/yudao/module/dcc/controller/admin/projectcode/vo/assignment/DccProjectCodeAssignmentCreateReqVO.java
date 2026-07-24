package cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.assignment;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DccProjectCodeAssignmentCreateReqVO {

    @NotNull(message = "assigneeUserId is required")
    private Long assigneeUserId;

    private List<Long> fileIds;

    @NotNull(message = "scopeMode is required")
    private String scopeMode;

    private LocalDateTime expireTime;

    private String assignmentReason;

}
