package cn.iocoder.yudao.module.srm.controller.admin.purchaseorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - SRM 采购订单协同 Response VO")
@Data
public class SrmPurchaseOrderRespVO {

    private Long id;

    private String orderNo;

    private Long sourcePlanId;

    private String sourcePlanNo;

    private Long supplierId;

    private String supplierName;

    private String orderStatus;

    private String orderStatusLabel;

    private String orderRemark;

    private Long confirmedBy;

    private String confirmedName;

    private LocalDateTime confirmedTime;

    private String confirmRemark;

    private LocalDateTime createTime;

    private Change latestChange;

    private List<Line> lines;

    @Data
    public static class Change {

        private Long id;

        private String changeNo;

        private String changeStatus;

        private String changeStatusLabel;

        private String changeReason;

        private String changeRemark;

        private String confirmRemark;

        private String rejectRemark;

        private String withdrawRemark;

        private LocalDateTime submittedTime;

        private LocalDateTime confirmedTime;

        private LocalDateTime rejectedTime;

        private LocalDateTime withdrawnTime;
    }

    @Data
    public static class Line {

        private Long id;

        private String lineNo;

        private Long sourcePlanLineId;

        private Long materialId;

        private String materialCode;

        private String materialName;

        private BigDecimal requestedQuantity;

        private String unit;

        private LocalDate requestedDeliveryDate;

        private BigDecimal confirmedQuantity;

        private LocalDate confirmedDeliveryDate;

        private String supplierRemark;

        private BigDecimal pendingChangedQuantity;

        private LocalDate pendingChangedDeliveryDate;

        private String pendingChangedRemark;
    }
}
