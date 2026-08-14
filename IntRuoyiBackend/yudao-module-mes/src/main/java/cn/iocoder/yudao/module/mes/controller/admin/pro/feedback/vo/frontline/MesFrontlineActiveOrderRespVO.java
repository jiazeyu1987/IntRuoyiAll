package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES PQC 当前活跃订单 Response VO")
@Data
public class MesFrontlineActiveOrderRespVO {

    @Schema(description = "活跃订单编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long activeOrderId;
    @Schema(description = "生产工单编号")
    private Long workOrderId;
    @Schema(description = "生产工单编码")
    private String workOrderCode;
    @Schema(description = "生产工单名称")
    private String workOrderName;
    @Schema(description = "产品编号")
    private Long productId;
    @Schema(description = "产品编码")
    private String productCode;
    @Schema(description = "产品名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String productName;
    @Schema(description = "生产批次号")
    private String batchCode;
    @Schema(description = "生产数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "125.5")
    private BigDecimal quantity;
    @Schema(description = "工艺路线编号")
    private Long routeId;
    @Schema(description = "工艺路线编码")
    private String routeCode;
    @Schema(description = "工艺路线名称")
    private String routeName;
    @Schema(description = "最近提交时间")
    private LocalDateTime latestSubmitTime;
}
