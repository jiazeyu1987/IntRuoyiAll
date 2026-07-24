package cn.iocoder.yudao.module.system.controller.admin.codextest.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - Codex 测试检查点保存 Request VO")
@Data
public class CodexTestCheckpointSaveReqVO {

    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "检查点排序不能为空")
    private Integer sort;

    @Schema(description = "检查点名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "检查点名称不能为空")
    private String name;

    @Schema(description = "期待结果", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "期待结果不能为空")
    private String expectedText;

    @Schema(description = "严重级别")
    private String severity;

    @Schema(description = "备注")
    private String remark;

}
