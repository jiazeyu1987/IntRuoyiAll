package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.importrecord;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - MES 第三方报工归属候选 Response VO")
@Data
public class MesProFeedbackImportCandidateRespVO {

    private String targetType;
    private Long scheduleOrderId;
    private String scheduleOrderCode;
    private Long scheduleOrderProcessId;
    private Long workOrderId;
    private String workOrderCode;
    private Long productId;
    private String itemCode;
    private String itemName;
    private String specification;
    private Long processId;
    private String processCode;
    private String processName;
    private BigDecimal plannedQuantity;
    private BigDecimal reportedQuantity;
    private BigDecimal remainingQuantity;
    private Long taskId;
    private String taskCode;
    private Boolean exactWorkOrderMatch;
    private Boolean externalOtherOrder;
    private String targetOrderLabel;
    private String targetProductLabel;
    private BigDecimal overproduceQuantity;
    private BigDecimal surplusPoolQuantity;
    private BigDecimal availableFeedbackQuantity;
    private BigDecimal selectedQuantity;
}
