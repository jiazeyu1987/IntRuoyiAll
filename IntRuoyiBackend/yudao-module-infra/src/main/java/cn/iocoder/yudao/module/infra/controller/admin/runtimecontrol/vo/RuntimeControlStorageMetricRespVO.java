package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeOpsInspectionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 运行控制台存储指标 Response VO")
@Data
public class RuntimeControlStorageMetricRespVO {

    @Schema(description = "路径")
    private String path;

    @Schema(description = "指标状态")
    private RuntimeOpsInspectionStatus status;

    @Schema(description = "总容量字节数")
    private Long totalBytes;

    @Schema(description = "可用容量字节数")
    private Long usableBytes;

    @Schema(description = "已用容量字节数")
    private Long usedBytes;

    @Schema(description = "已用百分比")
    private Double usagePercent;

    @Schema(description = "目录大小字节数")
    private Long sizeBytes;

    @Schema(description = "较上次采样增长字节数")
    private Long growthBytes;

    @Schema(description = "原因")
    private String reason;
}
