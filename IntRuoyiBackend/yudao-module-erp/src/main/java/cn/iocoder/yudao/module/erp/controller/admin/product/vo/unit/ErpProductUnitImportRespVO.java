package cn.iocoder.yudao.module.erp.controller.admin.product.vo.unit;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Schema(description = "管理后台 - ERP 产品单位导入结果")
@Data
@AllArgsConstructor
public class ErpProductUnitImportRespVO {

    @Schema(description = "实际新增数量", requiredMode = Schema.RequiredMode.REQUIRED)
    private int importedCount;

    @Schema(description = "跳过的重复单位数量", requiredMode = Schema.RequiredMode.REQUIRED)
    private int skippedCount;
}
