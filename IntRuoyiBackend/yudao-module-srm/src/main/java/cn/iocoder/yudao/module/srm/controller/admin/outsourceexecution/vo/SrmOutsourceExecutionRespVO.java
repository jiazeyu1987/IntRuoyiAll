package cn.iocoder.yudao.module.srm.controller.admin.outsourceexecution.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - SRM 委外执行 Response VO")
@Data
public class SrmOutsourceExecutionRespVO {

    private Long id;
    private String executionNo;
    private Long sourcePurchaseOrderId;
    private String sourcePurchaseOrderNo;
    private Long sourcePlanId;
    private String sourcePlanNo;
    private Long supplierId;
    private String supplierName;
    private String executionStatus;
    private String executionStatusLabel;
    private String simulationSource;
    private String simulationLabel;
    private String simulationRemark;
    private BigDecimal plannedQuantity;
    private String issueNoticeNo;
    private BigDecimal issueQuantity;
    private BigDecimal progressPercent;
    private String progressStage;
    private BigDecimal receivedQuantity;
    private BigDecimal qualifiedQuantity;
    private BigDecimal unitPrice;
    private LocalDateTime issuedTime;
    private LocalDateTime deliveredTime;
    private LocalDateTime inspectedTime;
    private LocalDateTime createTime;
    private Reconciliation reconciliation;
    private List<Event> events;

    @Data
    public static class Reconciliation {
        private Long id;
        private String reconciliationNo;
        private String reconciliationStatus;
        private String reconciliationStatusLabel;
        private BigDecimal unitPrice;
        private BigDecimal receivedQuantity;
        private BigDecimal qualifiedQuantity;
        private BigDecimal diffQuantity;
        private BigDecimal reconciliationAmount;
        private BigDecimal diffAmount;
        private String confirmRemark;
        private LocalDateTime confirmedTime;
    }

    @Data
    public static class Event {
        private Long id;
        private String eventNo;
        private String eventType;
        private String eventTypeLabel;
        private String beforeStatus;
        private String afterStatus;
        private String operatorName;
        private String eventRemark;
        private String eventPayload;
        private LocalDateTime eventTime;
    }
}
