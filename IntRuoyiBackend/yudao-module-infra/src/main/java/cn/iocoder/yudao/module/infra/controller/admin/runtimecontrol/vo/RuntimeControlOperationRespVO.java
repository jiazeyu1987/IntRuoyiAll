package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "管理后台 - 运行控制台操作记录 Response VO")
@Data
public class RuntimeControlOperationRespVO {

    @Schema(description = "操作编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String operationId;

    @Schema(description = "操作人", requiredMode = Schema.RequiredMode.REQUIRED)
    private String requestedBy;

    @Schema(description = "操作时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime requestedAt;

    @Schema(description = "环境", requiredMode = Schema.RequiredMode.REQUIRED, example = "test")
    private String environment;

    @Schema(description = "组件", requiredMode = Schema.RequiredMode.REQUIRED, example = "website-frontend")
    private String component;

    @Schema(description = "动作", example = "publish-test")
    private String action;

    @Schema(description = "动作名称", example = "发布测试服")
    private String actionLabel;

    @Schema(description = "安全操作参数")
    private Map<String, String> parameters;

    @Schema(description = "原因", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reason;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "running")
    private String status;

    @Schema(description = "摘要")
    private String summary;

    @Schema(description = "结果日志路径")
    private String resultLogPath;
}
