package cn.iocoder.yudao.module.system.controller.admin.codextest.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CodexTestExecutionCancelReqVO {

    @NotNull(message = "执行编号不能为空")
    private Long executionId;

}
