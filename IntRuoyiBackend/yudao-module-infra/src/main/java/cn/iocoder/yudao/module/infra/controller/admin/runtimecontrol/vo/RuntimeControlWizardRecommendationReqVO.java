package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 运行控制台决策向导推荐 Request VO")
@Data
public class RuntimeControlWizardRecommendationReqVO {

    @Schema(description = "场景编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "app-exception")
    @NotBlank(message = "场景编码不能为空")
    private String scenario;
}
