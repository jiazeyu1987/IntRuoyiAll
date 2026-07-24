package cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - MES 创建金蝶生产订单 Response VO")
@Data
public class MesKingdeeProductionOrderCreateRespVO {

    @Schema(description = "生产工单 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Long workOrderId;

    @Schema(description = "金蝶生产订单 FID", requiredMode = Schema.RequiredMode.REQUIRED, example = "310119")
    private String erpFid;

    @Schema(description = "金蝶生产订单单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "WO-001")
    private String erpBillNo;

    @Schema(description = "是否保存成功", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean saved;

    @Schema(description = "是否提交成功", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean submitted;

}
