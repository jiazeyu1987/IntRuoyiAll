package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 运行控制台事故动作 Response VO")
@Data
public class RuntimeControlIncidentActionRespVO {

    @Schema(description = "处置动作")
    private String action;

    @Schema(description = "操作者")
    private String operator;

    @Schema(description = "验证结果")
    private String verificationResult;

    @Schema(description = "验证证据")
    private String evidence;

    @Schema(description = "动作时间")
    private LocalDateTime actedAt;
}
