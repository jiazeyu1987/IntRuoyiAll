package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DccControlledFileSignatureReissueReqVO {

    @NotBlank(message = "reason is required")
    private String reason;

}
