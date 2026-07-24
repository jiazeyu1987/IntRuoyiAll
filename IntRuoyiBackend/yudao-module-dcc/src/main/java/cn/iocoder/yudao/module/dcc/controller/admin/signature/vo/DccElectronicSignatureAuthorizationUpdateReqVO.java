package cn.iocoder.yudao.module.dcc.controller.admin.signature.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - DCC电子签名授权更新 Request VO")
@Data
public class DccElectronicSignatureAuthorizationUpdateReqVO {

    @Schema(description = "是否开通电子签名授权", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @NotNull(message = "电子签名授权状态不能为空")
    private Boolean electronicSignatureEnabled;

    @Schema(description = "授权变更原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "完成岗位电子签名授权")
    @NotBlank(message = "授权变更原因不能为空")
    private String reason;
}
