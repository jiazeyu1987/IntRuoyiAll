package cn.iocoder.yudao.module.erp.service.purchase.sync;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ErpKingdeeProductionMaterialList {

    public static final String FORM_ID = "PRD_PPBOM";

    private String formId;
    private String entryId;
    private String billNo;
    private String productCode;
    private String productionOrderNo;
    private Integer productionOrderLineNo;
    private String productionOrderStatus;
    private String childMaterialCode;
    private String childMaterialName;
    private String childMaterialSpecification;
    private String childMaterialType;
    private BigDecimal numerator;
    private BigDecimal denominator;
    private String childUnitName;
    private BigDecimal requiredQuantity;
    private String issueMethod;
    private LocalDateTime demandTime;
    private LocalDateTime sourceModifyTime;
    private String rawPayload;

}

