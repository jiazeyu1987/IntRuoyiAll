package cn.iocoder.yudao.module.system.controller.admin.permission.vo.temporaryrole;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 临时角色授权撤销 Request VO")
@Data
public class TemporaryRoleGrantRevokeReqVO {

    @Schema(description = "授权记录编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "授权记录编号不能为空")
    private Long id;

    @Schema(description = "撤销原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "撤销原因不能为空")
    private String reason;

}
