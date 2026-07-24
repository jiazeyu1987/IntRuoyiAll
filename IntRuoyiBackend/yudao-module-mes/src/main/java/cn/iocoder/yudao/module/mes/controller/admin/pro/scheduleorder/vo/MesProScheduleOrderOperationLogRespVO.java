package cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES 排产工单操作追溯 Response VO")
@Data
public class MesProScheduleOrderOperationLogRespVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "排产工单编号", example = "1024")
    private Long scheduleOrderId;

    @Schema(description = "排产工单编码", example = "SCH-20260624-0001")
    private String scheduleOrderCode;

    @Schema(description = "操作类型", example = "FREEZE")
    private String operationType;

    @Schema(description = "操作前快照 JSON")
    private String beforeSnapshotJson;

    @Schema(description = "操作后快照 JSON")
    private String afterSnapshotJson;

    @Schema(description = "原因")
    private String reason;

    @Schema(description = "操作人编号")
    private Long operatorId;

    @Schema(description = "操作人名称")
    private String operatorName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
