package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.kingdee;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ErpKingdeeInventoryListRespVO {

    private Long id;
    private String materialNumber;
    private String materialName;
    private String materialSpecification;
    private String warehouseNumber;
    private String warehouseName;
    private String lotNumber;
    private String unitName;
    private BigDecimal quantity;
    private String stockOrgNumber;
    private String stockOrgName;
    private LocalDateTime sourceModifyTime;
    private LocalDateTime lastSyncTime;
    private LocalDateTime createTime;

}
