package cn.iocoder.yudao.module.erp.service.purchase.sync;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ErpKingdeeMaterial {

    public static final String FORM_ID = "BD_MATERIAL";

    private String materialNumber;
    private String materialName;
    private String specification;
    private String categoryCode;
    private String categoryName;
    private String unitName;
    private String forbidStatus;
    private String documentStatus;
    private LocalDateTime sourceModifyTime;

}
