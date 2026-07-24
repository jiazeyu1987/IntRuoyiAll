package cn.iocoder.yudao.module.erp.controller.admin.product.vo.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - ERP 产品金蝶按编码同步 Request VO")
@Data
public class ErpKingdeeProductCodeSyncReqVO {

    @Schema(description = "产品/物料编码列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "产品/物料编码列表不能为空")
    private List<String> productCodes;

}
