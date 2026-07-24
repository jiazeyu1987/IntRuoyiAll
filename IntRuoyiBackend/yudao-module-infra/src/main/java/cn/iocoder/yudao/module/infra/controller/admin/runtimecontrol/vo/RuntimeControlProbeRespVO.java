package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeOpsInspectionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 运行控制台探针 Response VO")
@Data
public class RuntimeControlProbeRespVO {

    @Schema(description = "环境", requiredMode = Schema.RequiredMode.REQUIRED, example = "local")
    private String environment;

    @Schema(description = "组件", requiredMode = Schema.RequiredMode.REQUIRED, example = "intruoyi-backend")
    private String component;

    @Schema(description = "组件类型", example = "backend")
    private String probeType;

    @Schema(description = "探针地址")
    private String url;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "NO_GO")
    private RuntimeOpsInspectionStatus status;

    @Schema(description = "HTTP 状态码")
    private Integer httpStatusCode;

    @Schema(description = "耗时毫秒")
    private Long durationMillis;

    @Schema(description = "错误")
    private String error;

    @Schema(description = "采样时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime sampledAt;
}
