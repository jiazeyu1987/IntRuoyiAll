package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.accessrequest.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DccRegistrationCertificateAccessReasonReqVO {

    @Schema(description = "撤回或撤销原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 512)
    private String reason;
}
