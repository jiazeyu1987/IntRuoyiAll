package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - ERP Kingdee K3Cloud 采购订单同步 Response VO")
@Data
public class ErpKingdeePurchaseOrderSyncRespVO {

    @Schema(description = "本次创建的采购订单数量", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer createdCount;

    @Schema(description = "本次跳过的 Kingdee 来源单数量", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer skippedCount;

    @Schema(description = "本次创建的 IntRuoyi 采购订单编号")
    private List<Long> createdPurchaseOrderIds;

    @Schema(description = "本次跳过的 Kingdee FID")
    private List<String> skippedSourceFids;

}
