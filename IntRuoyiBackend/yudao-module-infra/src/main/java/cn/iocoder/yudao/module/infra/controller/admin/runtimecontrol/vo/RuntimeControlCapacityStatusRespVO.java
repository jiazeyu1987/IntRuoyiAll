package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeOpsInspectionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 运行控制台容量状态 Response VO")
@Data
public class RuntimeControlCapacityStatusRespVO {

    @Schema(description = "汇总状态")
    private RuntimeOpsInspectionStatus status;

    @Schema(description = "采样时间")
    private LocalDateTime sampledAt;

    @Schema(description = "磁盘指标")
    private RuntimeControlStorageMetricRespVO disk;

    @Schema(description = "日志目录指标")
    private RuntimeControlStorageMetricRespVO logDirectory;

    @Schema(description = "原因列表")
    private List<String> reasons;

    @Schema(description = "超阈值告警")
    private RuntimeControlAlertRespVO alert;
}
