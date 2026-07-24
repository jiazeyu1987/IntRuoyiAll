package cn.iocoder.yudao.module.erp.service.purchase.sync;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ErpKingdeeInventoryRow {

    public static final String FORM_ID = "STK_Inventory";

    private String materialNumber;
    private String materialName;
    private String materialSpecification;
    private BigDecimal quantity;
    private String warehouseNumber;
    private String warehouseName;
    private String stockOrgNumber;
    private String stockOrgName;
    private String unitName;
    private String lotNumber;
    private LocalDateTime sourceModifyTime;

}
