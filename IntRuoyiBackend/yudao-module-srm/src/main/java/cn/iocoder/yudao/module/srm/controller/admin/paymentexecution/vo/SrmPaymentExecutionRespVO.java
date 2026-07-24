package cn.iocoder.yudao.module.srm.controller.admin.paymentexecution.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - SRM 付款执行 Response VO")
@Data
public class SrmPaymentExecutionRespVO {

    private Long id;
    private String paymentNo;
    private Long reconciliationId;
    private String reconciliationNo;
    private Long executionId;
    private String executionNo;
    private Long contractId;
    private String contractNo;
    private Long supplierId;
    private String supplierName;
    private String paymentStatus;
    private String paymentStatusLabel;
    private String simulationSource;
    private String simulationLabel;
    private String paymentStage;
    private BigDecimal paymentRatio;
    private LocalDate dueDate;
    private String paymentTermSummary;
    private BigDecimal reconciliationAmount;
    private BigDecimal applyAmount;
    private String paymentRemark;
    private String rejectRemark;
    private String pushRemark;
    private LocalDateTime submittedTime;
    private LocalDateTime approvedTime;
    private LocalDateTime rejectedTime;
    private LocalDateTime pushedTime;
    private LocalDateTime createTime;
    private List<Event> events;

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
