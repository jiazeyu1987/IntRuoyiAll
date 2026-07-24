package cn.iocoder.yudao.module.dcc.controller.admin.signature.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - DCC电子签名授权解锁 Request VO")
@Data
public class DccElectronicSignatureAuthorizationUnlockReqVO {

    @Schema(description = "解锁原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "签名人完成身份复核")
    @NotBlank(message = "解锁原因不能为空")
    private String reason;
}
