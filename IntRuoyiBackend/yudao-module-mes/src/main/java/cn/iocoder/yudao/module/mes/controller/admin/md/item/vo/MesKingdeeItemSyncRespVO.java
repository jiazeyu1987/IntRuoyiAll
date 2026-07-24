package cn.iocoder.yudao.module.mes.controller.admin.md.item.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - MES 物料产品金蝶同步 Response VO")
@Data
public class MesKingdeeItemSyncRespVO {

    @Schema(description = "新增数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer createdCount;

    @Schema(description = "更新数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "20")
    private Integer updatedCount;

    @Schema(description = "停用数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "5")
    private Integer disabledCount;

    @Schema(description = "跳过数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
    private Integer skippedCount;

}
