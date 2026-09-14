package cn.iocoder.yudao.module.erp.service.stock.sync;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ErpKingdeeStockMove {

    public static final String FORM_ID = "STK_TransferDirect";

    private String fid;
    private String billNo;
    private LocalDateTime billDate;
    private String documentStatus;
    private String transferDirect;
    private String transferBizType;
    private String remark;
    private LocalDateTime sourceModifyTime;
    private List<Line> lines = new ArrayList<>();

    @Data
    public static class Line {

        private String entryId;
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
