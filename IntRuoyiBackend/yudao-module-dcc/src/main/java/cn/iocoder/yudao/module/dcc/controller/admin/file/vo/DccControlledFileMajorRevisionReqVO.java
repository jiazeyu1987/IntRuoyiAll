package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Request to create the next major revision from a selected iteration. */
@Data
public class DccControlledFileMajorRevisionReqVO {

    @NotNull(message = "sourceControlledFileId is required")
    private Long sourceControlledFileId;

    @NotBlank(message = "reason is required")
    private String reason;
}
