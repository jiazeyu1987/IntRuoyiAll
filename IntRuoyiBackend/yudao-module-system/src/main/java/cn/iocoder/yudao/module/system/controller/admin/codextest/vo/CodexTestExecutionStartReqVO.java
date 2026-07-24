package cn.iocoder.yudao.module.system.controller.admin.codextest.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - Codex 测试执行启动 Request VO")
@Data
public class CodexTestExecutionStartReqVO {

    @Schema(description = "目标测试租户", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "目标测试租户不能为空")
    private Long targetTenantId;

    @Schema(description = "执行方式", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "执行方式不能为空")
    private String executionMode;

    @Schema(description = "测试项编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "测试项不能为空")
    private List<Long> caseIds;

}
