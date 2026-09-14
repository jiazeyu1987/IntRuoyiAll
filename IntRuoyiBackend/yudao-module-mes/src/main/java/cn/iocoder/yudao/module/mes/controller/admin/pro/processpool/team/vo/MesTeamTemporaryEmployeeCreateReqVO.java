package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES 生产人员临时工新增 Request VO")
@Data
@Accessors(chain = true)
public class MesTeamTemporaryEmployeeCreateReqVO {

    @Schema(description = "生产人员显示名", requiredMode = Schema.RequiredMode.REQUIRED, example = "临时工甲-A")
    @NotBlank(message = "生产人员显示名不能为空")
    private String displayName;

    @Schema(description = "临时工电子签名密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "******")
    @NotBlank(message = "临时工电子签名密码不能为空")
    private String signaturePassword;
}
