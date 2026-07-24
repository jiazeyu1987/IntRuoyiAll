package cn.iocoder.yudao.module.erp.service.purchase.sync;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ErpKingdeeProductionOrder {

    public static final String FORM_ID = "PRD_MO";

    private String fid;
    private String billNo;
    private LocalDateTime billDate;
    private String materialNumber;
    private String materialName;
    private String materialSpecification;
    private String unitCode;
    private String unitName;
    private BigDecimal quantity;
    private LocalDateTime plannedStartDate;
    private LocalDateTime plannedEndDate;
    private String sourceBillNo;
    private String batchNumber;
    private String documentStatus;
    private String status;
    private String workshopName;
    private String bomVersion;
    private String pickMode;
    private String auxiliaryCode;
    private String businessStatus;
    private String drawingNumber;
    private String scheduleStatus;
    private LocalDateTime sourceModifyTime;

}
