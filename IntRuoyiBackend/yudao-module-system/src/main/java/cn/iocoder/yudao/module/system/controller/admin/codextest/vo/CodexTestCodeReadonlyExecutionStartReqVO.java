package cn.iocoder.yudao.module.system.controller.admin.codextest.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - Codex 只读代码测试启动 Request VO")
@Data
public class CodexTestCodeReadonlyExecutionStartReqVO {

    @Schema(description = "目标测试租户", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "目标测试租户不能为空")
    private Long targetTenantId;

    @Schema(description = "只读代码测试定义", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotNull(message = "只读代码测试定义不能为空")
    private CodexTestCodeReadonlyCaseReqVO caseDefinition;

}
