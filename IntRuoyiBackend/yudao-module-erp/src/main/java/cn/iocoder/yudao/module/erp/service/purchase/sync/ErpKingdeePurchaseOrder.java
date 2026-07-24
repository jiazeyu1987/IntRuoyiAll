package cn.iocoder.yudao.module.erp.service.purchase.sync;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ErpKingdeePurchaseOrder {

    public static final String FORM_ID = "PUR_PurchaseOrder";

    private String fid;
    private String billNo;
    private LocalDateTime billDate;
    private LocalDateTime sourceModifyTime;
    private String documentStatus;
    private String closeStatus;
    private String cancelStatus;
    private String supplierNumber;
    private String supplierName;
    private List<Line> lines = new ArrayList<>();

    @Data
    public static class Line {

        private String materialNumber;
        private String materialName;
        private BigDecimal quantity;
        private BigDecimal price;
        private BigDecimal taxPercent;
        private String remark;

    }

}
