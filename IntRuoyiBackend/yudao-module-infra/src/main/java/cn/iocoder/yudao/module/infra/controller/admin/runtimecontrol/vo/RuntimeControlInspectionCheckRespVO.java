package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeOpsInspectionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 运行控制台巡检检查项 Response VO")
@Data
public class RuntimeControlInspectionCheckRespVO {

    @Schema(description = "检查编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "probe")
    private String code;

    @Schema(description = "检查名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "NO_GO")
    private RuntimeOpsInspectionStatus status;

    @Schema(description = "是否关键证据", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean required;

    @Schema(description = "证据")
    private String evidence;

    @Schema(description = "失败或阻断原因")
    private String reason;

    @Schema(description = "采样时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime sampledAt;
}
