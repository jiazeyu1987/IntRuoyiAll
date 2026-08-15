package cn.iocoder.yudao.module.erp.controller.admin.production.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ErpProductionPickListRespVO {

    private Long id;
    private String sourceFormId;
    private String sourceFid;
    private String sourceBillNo;
    private LocalDateTime billDate;
    private String documentStatus;
    private String stockOrgNumber;
    private String stockOrgName;
    private String productionOrgNumber;
    private String productionOrgName;
    private String ownerNumber;
    private String ownerName;
    private String departmentNumber;
    private String departmentName;
    private String description;
    private LocalDateTime sourceModifyTime;
    private LocalDateTime lastSyncTime;
    private LocalDateTime createTime;
    private String productionOrderNos;
    private String materialNames;
    private List<Item> items;

    @Data
    public static class Item {

        private Long id;
        private String sourceEntryId;
        private String materialNumber;
        private String materialName;
        private String materialSpecification;
        private String unitName;
        private BigDecimal requestedQuantity;
        private BigDecimal actualQuantity;
        private BigDecimal baseActualQuantity;
        private String warehouseNumber;
        private String warehouseName;
        private String stockLocationNumber;
        private String stockLocationName;
        private String lotNumber;
        private String productionOrderNo;
        private Integer productionOrderLineNo;
        private String productionMaterialListNo;
        private Integer productionMaterialListLineNo;
        private String workshopNumber;
        private String workshopName;
        private String stockStatusNumber;
        private String stockStatusName;

    }

}
