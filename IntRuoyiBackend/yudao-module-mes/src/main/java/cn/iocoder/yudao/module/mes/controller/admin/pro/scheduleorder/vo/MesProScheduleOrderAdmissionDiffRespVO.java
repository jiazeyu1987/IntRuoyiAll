package cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - MES 排产工单待同步差异 Response VO")
@Data
public class MesProScheduleOrderAdmissionDiffRespVO {

    @Schema(description = "生产工单编号", example = "100")
    private Long workOrderId;

    @Schema(description = "生产工单编码", example = "MO-001")
    private String workOrderCode;

    @Schema(description = "产品编号", example = "200")
    private Long productId;

    @Schema(description = "产品编码", example = "ITEM-001")
    private String productCode;

    @Schema(description = "产品名称", example = "产品A")
    private String productName;

    @Schema(description = "规格型号", example = "S1")
    private String productSpecification;

    @Schema(description = "生产数量", example = "100.000000")
    private BigDecimal quantity;

    @Schema(description = "需求日期")
    private LocalDateTime requestDate;

    @Schema(description = "生产工单状态", example = "1")
    private Integer workOrderStatus;

    @Schema(description = "是否临时冻结")
    private Boolean temporaryFrozen;

    @Schema(description = "排产工单编号", example = "900")
    private Long scheduleOrderId;

    @Schema(description = "入池状态", example = "READY_TO_ADMIT")
    private String admissionStatus;

    @Schema(description = "可排状态", example = "PASS")
    private String schedulableStatus;

    @Schema(description = "原因码", example = "BLOCKED_MISSING_ROUTE")
    private String reasonCode;

    @Schema(description = "严重度", example = "BLOCKED")
    private String severity;

    @Schema(description = "提示信息", example = "产品未绑定启用工艺流程排产配置")
    private String message;

    @Schema(description = "处理角色", example = "工艺维护")
    private String ownerRole;

    @Schema(description = "是否可勾选入池")
    private Boolean selectable;

    @Schema(description = "可操作动作")
    private List<MesProScheduleOrderIssueActionRespVO> actions;

}
