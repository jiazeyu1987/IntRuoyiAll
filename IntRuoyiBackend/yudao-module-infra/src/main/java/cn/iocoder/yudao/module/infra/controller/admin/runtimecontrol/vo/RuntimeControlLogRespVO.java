package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 运行控制台日志 Response VO")
@Data
public class RuntimeControlLogRespVO {

    @Schema(description = "操作编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String operationId;

    @Schema(description = "操作状态", requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;

    @Schema(description = "日志内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @Schema(description = "日志字节长度", requiredMode = Schema.RequiredMode.REQUIRED)
    private long length;

    @Schema(description = "是否已截断", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean truncated;

    @Schema(description = "日志路径")
    private String logPath;
}
