package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DccPublicationNotificationRetryReqVO {
    @NotNull(message = "expectedVersion is required")
    private Integer expectedVersion;
    @NotBlank(message = "retry reason is required")
    private String reason;
}
