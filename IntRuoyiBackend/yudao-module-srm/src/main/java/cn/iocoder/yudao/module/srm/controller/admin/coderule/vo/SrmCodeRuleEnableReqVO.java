package cn.iocoder.yudao.module.srm.controller.admin.coderule.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - SRM 编码规则启停 Request VO")
@Data
public class SrmCodeRuleEnableReqVO {

    @Schema(description = "规则编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "规则编号不能为空")
    private Long id;

    @Schema(description = "是否启用", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @NotNull(message = "是否启用不能为空")
    private Boolean enabled;

}
