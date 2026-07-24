package cn.iocoder.yudao.module.system.controller.admin.codextest.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - Codex 测试项保存 Request VO")
@Data
public class CodexTestCaseSaveReqVO {

    @Schema(description = "测试项编号")
    private Long id;

    @Schema(description = "测试项名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "测试项名称不能为空")
    private String name;

    @Schema(description = "自然语言测试方法", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "自然语言测试方法不能为空")
    private String methodText;

    @Schema(description = "用户手写测试数据")
    private String testDataText;

    @Schema(description = "默认执行方式")
    private String defaultExecutionMode;

    @Schema(description = "是否并行安全")
    private Boolean parallelSafe;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "检查点")
    @Valid
    @NotEmpty(message = "检查点不能为空")
    private List<CodexTestCheckpointSaveReqVO> checkpoints;

}
