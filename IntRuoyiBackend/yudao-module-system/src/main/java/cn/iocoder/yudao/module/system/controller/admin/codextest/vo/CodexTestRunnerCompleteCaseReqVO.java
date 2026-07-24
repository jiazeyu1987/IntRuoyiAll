package cn.iocoder.yudao.module.system.controller.admin.codextest.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CodexTestRunnerCompleteCaseReqVO {

    @NotNull(message = "执行测试项不能为空")
    private Long executionCaseId;

    @NotBlank(message = "执行状态不能为空")
    private String status;

    private String summary;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

}
