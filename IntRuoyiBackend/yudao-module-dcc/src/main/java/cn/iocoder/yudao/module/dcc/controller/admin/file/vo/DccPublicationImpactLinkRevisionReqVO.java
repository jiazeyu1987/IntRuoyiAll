package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DccPublicationImpactLinkRevisionReqVO {

    @NotNull(message = "expectedVersion is required")
    private Integer expectedVersion;
    @NotNull(message = "revisionControlledFileId is required")
    private Long revisionControlledFileId;
    @NotBlank(message = "reason is required")
    private String reason;
}
