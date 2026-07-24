package cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - DCC 产品目录更新 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class DccProductCatalogUpdateReqVO extends DccProductCatalogSaveReqVO {

    @Schema(description = "原 sheet 行号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "原 sheet 行号不能为空")
    @Min(value = 2, message = "原 sheet 行号必须大于表头行")
    private Integer originalRowNo;
}
