package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DccPublicationImpactCreateRevisionReqVO {

    @NotNull(message = "expectedVersion is required")
    private Integer expectedVersion;
    @NotNull(message = "sourceControlledFileId is required")
    private Long sourceControlledFileId;
    @NotBlank(message = "reason is required")
    private String reason;
}
