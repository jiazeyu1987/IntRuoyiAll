package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeOpsInspectionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 运行控制台巡检报告 Response VO")
@Data
public class RuntimeControlInspectionRunRespVO {

    @Schema(description = "巡检编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "NO_GO")
    private RuntimeOpsInspectionStatus status;

    @Schema(description = "摘要")
    private String summary;

    @Schema(description = "开始时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime startedAt;

    @Schema(description = "完成时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime completedAt;

    @Schema(description = "检查项", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<RuntimeControlInspectionCheckRespVO> checks;
}
