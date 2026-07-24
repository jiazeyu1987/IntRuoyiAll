package cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 生产用料清单单据汇总 Response VO")
@Data
public class MesKingdeeProductionMaterialListGroupRespVO {

    @Schema(description = "ERP 生产用料清单单据编号", example = "PPBOM003088")
    private String sourceBillNo;

    @Schema(description = "整单子项数量", example = "12")
    private Long lineCount;

    @Schema(description = "ERP 来源修改时间")
    private LocalDateTime sourceModifyTime;

    @Schema(description = "最后同步时间")
    private LocalDateTime lastSyncTime;

    @Schema(description = "关联生产订单数量", example = "2")
    private Long productionOrderCount;

    @Schema(description = "关联生产订单摘要", example = "WO-001、WO-002")
    private String productionOrderSummary;

}
