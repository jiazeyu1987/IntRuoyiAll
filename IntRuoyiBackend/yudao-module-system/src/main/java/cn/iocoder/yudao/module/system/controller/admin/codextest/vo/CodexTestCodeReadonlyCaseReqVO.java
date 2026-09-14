package cn.iocoder.yudao.module.system.controller.admin.codextest.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - Codex 只读代码测试定义 Request VO")
@Data
public class CodexTestCodeReadonlyCaseReqVO {

    @Schema(description = "测试项名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "测试项名称不能为空")
    private String name;

    @Schema(description = "所属项目", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "所属项目不能为空")
    private String project;

    @Schema(description = "只读代码检查方法", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "只读代码检查方法不能为空")
    private String methodText;

    @Schema(description = "测试范围和上下文")
    private String testDataText;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "检查点", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotEmpty(message = "检查点不能为空")
    private List<CodexTestCheckpointSaveReqVO> checkpoints;

}
