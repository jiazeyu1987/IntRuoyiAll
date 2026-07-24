package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 运行控制台事故 Response VO")
@Data
public class RuntimeControlIncidentRespVO {

    @Schema(description = "事故编号")
    private Long id;

    @Schema(description = "环境")
    private String environment;

    @Schema(description = "动作或异常类型")
    private String action;

    @Schema(description = "严重级别")
    private String severity;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "来源类型")
    private String sourceType;

    @Schema(description = "来源编号")
    private String sourceId;

    @Schema(description = "状态：OPEN/CLOSED")
    private String status;

    @Schema(description = "创建人")
    private String createdBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "事故动作")
    private List<RuntimeControlIncidentActionRespVO> actions;

    @Schema(description = "责任人门禁结果")
    private String ownerGateResult;

    @Schema(description = "验证结果")
    private String verificationResult;

    @Schema(description = "剩余风险")
    private String remainingRisk;

    @Schema(description = "复盘状态")
    private String postmortemStatus;

    @Schema(description = "关闭原因")
    private String closeReason;

    @Schema(description = "关闭人")
    private String closedBy;

    @Schema(description = "关闭时间")
    private LocalDateTime closedAt;
}
