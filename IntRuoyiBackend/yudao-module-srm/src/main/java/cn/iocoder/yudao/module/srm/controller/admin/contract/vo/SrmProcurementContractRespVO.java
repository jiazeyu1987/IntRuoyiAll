package cn.iocoder.yudao.module.srm.controller.admin.contract.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SrmProcurementContractRespVO {

    private Long id;
    private String contractNo;
    private String contractTitle;
    private String sourceType;
    private String sourceTypeLabel;
    private Long sourceId;
    private String sourceNo;
    private Long supplierId;
    private String supplierName;
    private BigDecimal contractAmount;
    private String currency;
    private LocalDate effectiveDate;
    private LocalDate expireDate;
    private String contractStatus;
    private String contractStatusLabel;
    private String createdName;
    private LocalDateTime createdTime;
    private String cancelledName;
    private LocalDateTime cancelledTime;
    private String cancelReason;
    private List<Payment> payments;
    private List<Signing> signings;
    private List<Attachment> attachments;

    @Data
    public static class Payment {
        private Long id;
        private String paymentStage;
        private BigDecimal paymentRatio;
        private BigDecimal paymentAmount;
        private LocalDate dueDate;
        private String paymentRemark;
    }

    @Data
    public static class Signing {
        private Long id;
        private String signingParty;
        private String signerName;
        private LocalDate signingDate;
        private String signingRemark;
    }

    @Data
    public static class Attachment {
        private Long id;
        private String attachmentName;
        private String attachmentUrl;
        private String attachmentType;
    }
}
