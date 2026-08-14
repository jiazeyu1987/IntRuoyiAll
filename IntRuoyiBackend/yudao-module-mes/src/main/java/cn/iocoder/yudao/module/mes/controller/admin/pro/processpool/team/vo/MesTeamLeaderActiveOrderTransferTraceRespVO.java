package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES 班组长活跃订单调拨/库存追溯 Response VO")
@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderTransferTraceRespVO {

    @Schema(description = "追溯记录编号", example = "7201")
    private Long id;

    @Schema(description = "统一活跃订单编号", example = "8101")
    private Long activeOrderId;

    @Schema(description = "生产订单编号", example = "9001")
    private Long workOrderId;

    @Schema(description = "正式工艺路线编号", example = "922119")
    private Long routeId;

    @Schema(description = "正式工艺路线版本编号", example = "448")
    private Long routeVersionId;

    @Schema(description = "来源类型", example = "SHIPMENT")
    private String sourceType;

    @Schema(description = "出入方向", example = "OUT")
    private String direction;

    @Schema(description = "调拨单编号", example = "5001")
    private Long transferId;

    @Schema(description = "调拨行编号", example = "5002")
    private Long transferLineId;

    @Schema(description = "调拨明细编号", example = "5003")
    private Long transferDetailId;

    @Schema(description = "物料库存编号", example = "6001")
    private Long materialStockId;

    @Schema(description = "批次编号", example = "7001")
    private Long batchId;

    @Schema(description = "物料编号", example = "8001")
    private Long itemId;

    @Schema(description = "来源数量", example = "15.000000")
    private BigDecimal quantity;

    @Schema(description = "来源对象类型", example = "WM_TRANSFER_DETAIL")
    private String sourceObjectType;

    @Schema(description = "来源对象编号", example = "5003")
    private String sourceObjectId;

    @Schema(description = "来源对象编码", example = "TR-9001")
    private String sourceObjectCode;

    @Schema(description = "来源状态", example = "SHIPPED")
    private String sourceStatus;

    @Schema(description = "来源发生时间")
    private LocalDateTime sourceOccurredAt;

    @Schema(description = "幂等键", example = "transfer-9001-line-2-batch-3")
    private String idempotencyKey;

    @Schema(description = "来源快照 JSON")
    private String sourceSnapshotJson;
}
