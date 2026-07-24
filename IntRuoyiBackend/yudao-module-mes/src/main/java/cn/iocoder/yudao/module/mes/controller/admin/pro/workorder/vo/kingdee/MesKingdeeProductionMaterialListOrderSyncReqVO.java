package cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - ERP 生产用料清单按生产订单号同步 Request VO")
@Data
public class MesKingdeeProductionMaterialListOrderSyncReqVO {

    @Schema(description = "生产订单号列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "生产订单号列表不能为空")
    private List<String> productionOrderNos;

}
