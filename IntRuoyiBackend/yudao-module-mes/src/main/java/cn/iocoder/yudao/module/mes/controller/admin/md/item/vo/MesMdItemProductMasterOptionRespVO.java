package cn.iocoder.yudao.module.mes.controller.admin.md.item.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - MES 物料可选 MDM 产品 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesMdItemProductMasterOptionRespVO {

    @Schema(description = "MDM 产品主档编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long productMasterId;

    @Schema(description = "MDM 产品编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String productCode;

    @Schema(description = "DCC 产品编号")
    private String dccProductCode;

    @Schema(description = "中文名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nameCn;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;

}
