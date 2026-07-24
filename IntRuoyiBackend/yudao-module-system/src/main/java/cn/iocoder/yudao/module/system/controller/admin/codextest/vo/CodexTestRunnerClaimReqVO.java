package cn.iocoder.yudao.module.system.controller.admin.codextest.vo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CodexTestRunnerClaimReqVO {

    @NotNull(message = "Runner 会话不能为空")
    private Long runnerSessionId;

    @Min(value = 1, message = "领取容量必须大于 0")
    private Integer capacity;

}
