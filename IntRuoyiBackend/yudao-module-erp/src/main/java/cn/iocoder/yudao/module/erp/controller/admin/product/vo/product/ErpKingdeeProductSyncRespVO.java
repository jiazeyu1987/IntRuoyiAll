package cn.iocoder.yudao.module.erp.controller.admin.product.vo.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - ERP 产品金蝶同步 Response VO")
@Data
public class ErpKingdeeProductSyncRespVO {

    @Schema(description = "新增数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer createdCount;

    @Schema(description = "更新数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "5")
    private Integer updatedCount;

    @Schema(description = "跳过数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
    private Integer skippedCount;

    @Schema(description = "新增产品编码列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> createdProductCodes;

    @Schema(description = "更新产品编码列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> updatedProductCodes;

    @Schema(description = "跳过产品编码列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> skippedProductCodes;

}
