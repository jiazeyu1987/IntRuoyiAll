package cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - MES 排产工单排产前检查问题 Response VO")
@Data
public class MesProScheduleOrderPreflightIssueRespVO {

    @Schema(description = "原因码", example = "BLOCKED_MISSING_ROUTE")
    private String reasonCode;

    @Schema(description = "严重度", example = "BLOCKED")
    private String severity;

    @Schema(description = "对象类型", example = "SCHEDULE_ORDER")
    private String objectType;

    @Schema(description = "对象编号", example = "100")
    private Long objectId;

    @Schema(description = "生产工单编号", example = "100")
    private Long workOrderId;

    @Schema(description = "生产工单编码", example = "MO-001")
    private String workOrderCode;

    @Schema(description = "排产工单编号", example = "10")
    private Long scheduleOrderId;

    @Schema(description = "排产工单编码", example = "SCH-MO-001")
    private String scheduleOrderCode;

    @Schema(description = "产品编号", example = "200")
    private Long productId;

    @Schema(description = "产品编码", example = "ITEM-001")
    private String productCode;

    @Schema(description = "产品名称", example = "球囊导管")
    private String productName;

    @Schema(description = "工序编号", example = "300")
    private Long processId;

    @Schema(description = "工序名称", example = "吹球囊成型")
    private String processName;

    @Schema(description = "提示信息", example = "产品未绑定启用工艺流程排产配置")
    private String message;

    @Schema(description = "处理角色", example = "工艺维护")
    private String ownerRole;

    @Schema(description = "处理动作")
    private MesProScheduleOrderIssueActionRespVO action;

}
