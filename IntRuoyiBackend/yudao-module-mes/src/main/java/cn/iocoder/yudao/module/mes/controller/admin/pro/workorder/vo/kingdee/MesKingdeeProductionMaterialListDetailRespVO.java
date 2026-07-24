package cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - ERP 生产用料清单单据明细 Response VO")
@Data
public class MesKingdeeProductionMaterialListDetailRespVO {

    @Schema(description = "子项物料编码", example = "A001.02.014.300")
    private String childMaterialCode;

    @Schema(description = "子项物料名称", example = "造影导管软端")
    private String childMaterialName;

    @Schema(description = "规格型号")
    private String childMaterialSpecification;

    @Schema(description = "子项类型")
    private String childMaterialType;

    @Schema(description = "分子")
    private BigDecimal numerator;

    @Schema(description = "分母")
    private BigDecimal denominator;

    @Schema(description = "子项单位")
    private String childUnitName;

    @Schema(description = "生产订单编号", example = "WO-001")
    private String productionOrderNo;

    @Schema(description = "本地生产工单 ID", example = "903245")
    private Long workOrderId;

    @Schema(description = "本地生产工单编号", example = "WO-001")
    private String workOrderCode;

}
