package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES eDHR 独立表单事件 Response VO")
@Data
public class MesProEdhrFormEventRespVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "实例 ID")
    private Long instanceId;

    @Schema(description = "模板 ID")
    private Long templateId;

    @Schema(description = "实例编码")
    private String instanceCode;

    @Schema(description = "事件类型")
    private String eventType;

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

    @Schema(description = "事件元数据")
    private String metadataJson;
}
