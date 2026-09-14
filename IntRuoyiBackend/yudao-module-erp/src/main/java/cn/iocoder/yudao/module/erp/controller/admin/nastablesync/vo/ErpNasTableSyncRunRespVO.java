package cn.iocoder.yudao.module.erp.controller.admin.nastablesync.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - ERP NAS 表格同步运行 Response VO")
@Data
public class ErpNasTableSyncRunRespVO {

    @Schema(description = "运行编号")
    private Long id;

    @Schema(description = "计划编号")
    private Long planId;

    @Schema(description = "触发类型")
    private String triggerType;

    @Schema(description = "运行状态")
    private String status;

    @Schema(description = "开始时间")
    private LocalDateTime startedAt;

    @Schema(description = "结束时间")
    private LocalDateTime endedAt;

    @Schema(description = "输出路径")
    private String outputPath;

    @Schema(description = "总表数")
    private Integer totalTableCount;

    @Schema(description = "成功表数")
    private Integer successTableCount;

    @Schema(description = "失败表数")
    private Integer failedTableCount;

    @Schema(description = "失败信息")
    private String failureMessage;

    @Schema(description = "运行明细")
    private List<ErpNasTableSyncRunItemRespVO> items = new ArrayList<>();
}
