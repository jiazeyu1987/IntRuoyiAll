package cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 生产用料清单 Response VO")
@Data
public class MesKingdeeProductionMaterialListRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "ERP 来源表单标识", example = "PRD_PPBOM")
    private String sourceFormId;

    @Schema(description = "ERP 生产用料清单单据编号", example = "PPBOM003088")
    private String sourceBillNo;

    @Schema(description = "ERP 来源分录 ID")
    private String sourceEntryId;

    @Schema(description = "产品编码", example = "AW.106.03.08.10")
    private String productCode;

    @Schema(description = "生产订单编号", example = "CODXMO20260")
    private String productionOrderNo;

    @Schema(description = "生产订单行号", example = "1")
    private Integer productionOrderLineNo;

    @Schema(description = "生产订单状态", example = "计划")
    private String productionOrderStatus;

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

    @Schema(description = "应发数量")
    private BigDecimal requiredQuantity;

    @Schema(description = "发料方式")
    private String issueMethod;

    @Schema(description = "需求日期")
    private LocalDateTime demandTime;

    @Schema(description = "本地生产工单 ID")
    private Long workOrderId;

    @Schema(description = "本地生产工单编号")
    private String workOrderCode;

    @Schema(description = "本地生产工单 BOM 明细 ID")
    private Long workOrderBomId;

    @Schema(description = "本地产品物料 ID")
    private Long productId;

    @Schema(description = "本地子项物料 ID")
    private Long childMaterialId;

    @Schema(description = "ERP 来源修改时间")
    private LocalDateTime sourceModifyTime;

    @Schema(description = "最后同步时间")
    private LocalDateTime lastSyncTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
