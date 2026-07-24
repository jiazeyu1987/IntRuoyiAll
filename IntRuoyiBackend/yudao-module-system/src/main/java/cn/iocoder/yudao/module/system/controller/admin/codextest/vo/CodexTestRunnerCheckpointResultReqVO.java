package cn.iocoder.yudao.module.system.controller.admin.codextest.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CodexTestRunnerCheckpointResultReqVO {

    @NotNull(message = "执行测试项不能为空")
    private Long executionCaseId;

    @NotNull(message = "检查点排序不能为空")
    private Integer checkpointSort;

    @NotBlank(message = "检查点状态不能为空")
    private String status;

    private String expectedText;
    private String actualText;
    private String mismatchDescription;
    private Long screenshotArtifactId;

}
