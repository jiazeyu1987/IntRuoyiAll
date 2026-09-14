package cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("mes_pro_process_pool_active_order_transfer_trace")
@KeySequence("mes_pro_process_pool_active_order_transfer_trace_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProcessPoolActiveOrderTransferTraceDO extends TenantBaseDO {

    public static final String SOURCE_TYPE_TRANSFER = "TRANSFER";
    public static final String SOURCE_TYPE_SHIPMENT = "SHIPMENT";
    public static final String SOURCE_TYPE_REPLENISHMENT = "REPLENISHMENT";
    public static final String SOURCE_TYPE_RETURN = "RETURN";
    public static final String SOURCE_TYPE_BATCH_TRACE = "BATCH_TRACE";
    public static final String SOURCE_TYPE_SCRAP = "SCRAP";
    public static final String SOURCE_TYPE_REWORK = "REWORK";

    @TableId
    private Long id;

    private Long activeOrderId;
    private Long workOrderId;
    private Long routeId;
    private Long routeVersionId;
    private String sourceType;
    private String direction;
    private Long transferId;
    private Long transferLineId;
    private Long transferDetailId;
    private Long materialStockId;
    private Long batchId;
    private Long itemId;
    private BigDecimal quantity;
    private String sourceObjectType;
    private String sourceObjectId;
    private String sourceObjectCode;
    private String sourceStatus;
    private LocalDateTime sourceOccurredAt;
    private String idempotencyKey;
    private String sourceSnapshotJson;
}
