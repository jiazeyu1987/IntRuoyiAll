package cn.iocoder.yudao.module.system.controller.admin.codextest.vo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CodexTestRunnerRegisterReqVO {

    @NotBlank(message = "Runner 名称不能为空")
    private String runnerName;

    @NotBlank(message = "Runner 能力不能为空")
    private String capabilities;

    @Min(value = 1, message = "Runner 并行数必须大于 0")
    private Integer maxParallelism;

    private String playwrightVersion;
    private String codexVersion;

}
