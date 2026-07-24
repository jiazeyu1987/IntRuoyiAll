package cn.iocoder.yudao.module.mes.controller.admin.md.item.vo.kingdee;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 物料清单 Response VO")
@Data
public class MesKingdeeBomListRespVO {

    private Long id;
    private String sourceFormId;
    private String sourceFid;
    private String bomNumber;
    private String bomType;
    private String documentStatus;
    private String parentMaterialCode;
    private String parentMaterialName;
    private String parentMaterialSpecification;
    private BigDecimal parentQuantity;
    private Integer lineNo;
    private String childMaterialCode;
    private String childMaterialName;
    private String childMaterialSpecification;
    private String childUnitName;
    private BigDecimal numerator;
    private BigDecimal denominator;
    private LocalDateTime sourceModifyTime;
    private LocalDateTime lastSyncTime;
    private LocalDateTime createTime;

}
