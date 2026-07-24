package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeOpsInspectionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 运行控制台探针最新结果 Response VO")
@Data
public class RuntimeControlProbeLatestRespVO {

    @Schema(description = "汇总状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "NO_GO")
    private RuntimeOpsInspectionStatus status;

    @Schema(description = "采样时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime sampledAt;

    @Schema(description = "探针明细", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<RuntimeControlProbeRespVO> probes;

    @Schema(description = "失败阈值触发的告警")
    private RuntimeControlAlertRespVO alert;
}
