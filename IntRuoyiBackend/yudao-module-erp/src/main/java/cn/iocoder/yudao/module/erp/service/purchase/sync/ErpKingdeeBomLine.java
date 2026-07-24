package cn.iocoder.yudao.module.erp.service.purchase.sync;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ErpKingdeeBomLine {

    public static final String FORM_ID = "ENG_BOM";

    private String fid;
    private String bomVersion;
    private String parentMaterialNumber;
    private String parentMaterialName;
    private String parentMaterialSpecification;
    private String childMaterialNumber;
    private String childMaterialName;
    private String childMaterialSpecification;
    private String childUnitName;
    private BigDecimal numerator;
    private BigDecimal denominator;
    private LocalDateTime sourceModifyTime;

}
