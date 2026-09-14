package cn.iocoder.yudao.module.system.controller.admin.codextest.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CodexTestRunnerProgressReqVO {

    @NotNull(message = "执行测试项不能为空")
    private Long executionCaseId;

    @NotBlank(message = "执行阶段不能为空")
    private String phase;

    private Integer currentMethodSort;
    private Integer currentCheckpointSort;
    private String progressMessage;

}
