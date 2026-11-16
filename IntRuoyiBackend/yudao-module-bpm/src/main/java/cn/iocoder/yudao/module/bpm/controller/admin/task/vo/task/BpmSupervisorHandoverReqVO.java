package cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class BpmSupervisorHandoverReqVO {
    @NotBlank private String id;
    @NotNull @Positive private Long previousAssigneeUserId;
    @NotNull @Positive private Long assigneeUserId;
    /** 直属主管变更时限定为该员工发起的主管审批。 */
    @Positive private Long applicantUserId;
    @NotBlank @Size(max = 500) private String reason;
}
