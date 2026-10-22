package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - eDHR 不合格评审可选活跃订单")
@Data
public class MesProEdhrNonconformanceReviewActiveOrderRespVO {

    @Schema(description = "活跃订单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "生产工单ID")
    private Long workOrderId;

    @Schema(description = "生产工单号")
    private String workOrderCode;

    @Schema(description = "批号")
    private String batchCode;

    @Schema(description = "活跃订单状态")
    private String activeStatus;

    @Schema(description = "业务状态")
    private String businessStatus;
}
