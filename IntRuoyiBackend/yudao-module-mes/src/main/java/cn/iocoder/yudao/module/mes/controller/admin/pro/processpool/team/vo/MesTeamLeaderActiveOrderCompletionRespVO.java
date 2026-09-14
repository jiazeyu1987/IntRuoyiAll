package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Schema(description = "管理后台 - MES 生产组长活跃订单完成 Response VO")
@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderCompletionRespVO {

    @Schema(description = "活跃订单记录编号", example = "8101")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long activeOrderId;

    @Schema(description = "不可变完成/回填回执编号", example = "9101")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long completionReceiptId;

    @Schema(description = "正式生产批次编码")
    private String batchCode;

    @Schema(description = "冻结工艺路线编号")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long routeId;

    @Schema(description = "冻结工艺路线版本编号")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long routeVersionId;

    @Schema(description = "不可变回执哈希")
    private String receiptHash;

    @Schema(description = "交给流程6的回执状态", example = "BACKFILL_SUCCEEDED")
    private String flow6ReceiptStatus;

    @Schema(description = "完成后活跃订单版本", example = "4")
    private Integer activeOrderVersion;

    @Schema(description = "批记录回填状态", example = "SUCCESS")
    private String batchRecordStatus;

    @Schema(description = "过程检验单回填状态", example = "SUCCESS")
    private String processInspectionStatus;

    @Schema(description = "损耗回填状态", example = "NOT_REQUIRED")
    private String lossReportStatus;

    @Schema(description = "是否存在实际损耗", example = "false")
    private Boolean hasActualLoss;

    @Schema(description = "订单实际损耗数量", example = "0")
    private BigDecimal lossQuantity;

    @Schema(description = "流程6后继建批交接结果", example = "PENDING_FLOW6")
    private String provisionHandoff;
}
