package cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.assignment;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DccProjectCodeAssignmentRevokeReqVO {

    @NotBlank(message = "revokeReason is required")
    private String revokeReason;

}
