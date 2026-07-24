package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES eDHR 记录本事件 Response VO")
@Data
public class MesProEdhrRecordbookEventRespVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "记录本 ID")
    private Long recordbookId;

    @Schema(description = "条目 ID")
    private Long entryId;

    @Schema(description = "事件类型")
    private String eventType;

    @Schema(description = "原状态")
    private String fromStatus;

    @Schema(description = "目标状态")
    private String toStatus;

    @Schema(description = "事件结果")
    private String resultStatus;

    @Schema(description = "失败原因")
    private String failureReason;

    @Schema(description = "操作人")
    private Long operatorUserId;

    @Schema(description = "操作人名称")
    private String operatorUsername;

    @Schema(description = "发生时间")
    private LocalDateTime occurredAt;

    @Schema(description = "事件快照 JSON")
    private String eventSnapshotJson;
}
