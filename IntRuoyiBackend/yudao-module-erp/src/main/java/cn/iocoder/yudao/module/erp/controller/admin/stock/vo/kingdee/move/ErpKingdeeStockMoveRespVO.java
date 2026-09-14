package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.kingdee.move;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ErpKingdeeStockMoveRespVO {

    private Long id;
    private String sourceFormId;
    private String sourceFid;
    private String sourceBillNo;
    private LocalDateTime billDate;
    private String documentStatus;
    private String transferDirect;
    private String transferBizType;
    private String remark;
    private LocalDateTime sourceModifyTime;
    private LocalDateTime lastSyncTime;
    private LocalDateTime createTime;
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
        private BigDecimal quantity;
        private String fromWarehouseNumber;
        private String fromWarehouseName;
        private String toWarehouseNumber;
        private String toWarehouseName;
        private String fromStockLocation;
        private String toStockLocation;
        private String lotNumber;

    }

}
