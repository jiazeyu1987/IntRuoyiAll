package cn.iocoder.yudao.module.erp.service.purchase.sync;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpKingdeeProductionOrderCreateRequest {

    private String billNo;

    private String templateBillNo;

    private String materialNumber;

    private String productionOrgNumber;

    private String unitNumber;

    /**
     * Explicitly read the unit from the selected template entry when unitNumber is blank.
     */
    private Boolean useTemplateEntryUnit;

    private BigDecimal quantity;

    private LocalDateTime plannedStartDate;

    private LocalDateTime plannedFinishDate;

    private String sourceBillNo;

    private String batchNumber;

}
