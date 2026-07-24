package cn.iocoder.yudao.module.erp.controller.admin.sync.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - ERP 金蝶生产工单新增 Response VO")
@Data
public class ErpKingdeeProductionOrderCreateRespVO {

    @Schema(description = "金蝶生产订单 FID", example = "310119")
    private String erpFid;

    @Schema(description = "金蝶生产订单单号", example = "SMOKE-MO-001")
    private String erpBillNo;

    @Schema(description = "是否保存成功", example = "true")
    private Boolean saved;

    @Schema(description = "是否提交成功", example = "true")
    private Boolean submitted;

}
