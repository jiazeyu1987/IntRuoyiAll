package cn.iocoder.yudao.module.system.controller.admin.codextest.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CodexTestRunnerHeartbeatReqVO {

    @NotNull(message = "Runner 会话不能为空")
    private Long runnerSessionId;

    private List<Long> runningExecutionCaseIds;

}
