package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES 临时工电子签名密码重置 Request VO")
@Data
@Accessors(chain = true)
public class MesTeamTemporarySignaturePasswordResetReqVO {

    @Schema(description = "生产人员档案编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "8801")
    @NotNull(message = "生产人员档案编号不能为空")
    private Long employeeProfileId;

    @Schema(description = "新的电子签名密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "******")
    @NotBlank(message = "新的电子签名密码不能为空")
    private String signaturePassword;
}
