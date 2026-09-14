package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DccPublicationImpactReassignReqVO {

    @NotNull(message = "expectedVersion is required")
    private Integer expectedVersion;
    @NotNull(message = "newAssigneeUserId is required")
    private Long newAssigneeUserId;
    @NotBlank(message = "reason is required")
    private String reason;
}
