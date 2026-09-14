package cn.iocoder.yudao.module.erp.service.production.sync;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ErpKingdeeProductionReplenishmentList {

    public static final String FORM_ID = "PRD_FeedMtrl";

    private String fid;
    private String billNo;
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
    private List<Line> lines = new ArrayList<>();

    @Data
    public static class Line {

        private String entryId;
        private String materialNumber;
        private String materialName;
        private String materialSpecification;
        private String unitName;
        private BigDecimal actualQuantity;
        private BigDecimal baseActualQuantity;
        private BigDecimal requestedQuantity;
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
