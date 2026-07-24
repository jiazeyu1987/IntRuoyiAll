package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DccControlledFileUploadTemporaryCleanupReqVO {

    @NotBlank(message = "sessionId is required")
    private String sessionId;

}
