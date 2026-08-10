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

@TableName("mes_pro_process_pool_report_allocation_adjustment_audit")
@KeySequence("mes_pro_process_pool_report_allocation_adjustment_audit_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProcessPoolReportAllocationAdjustmentAuditDO extends TenantBaseDO {

    public static final String SOURCE_INITIAL_BASELINE = "INITIAL_BASELINE";
    public static final String SOURCE_FIFO = "FIFO";
    public static final String SOURCE_MANUAL = "MANUAL";
    public static final String SOURCE_ORDER_CHANGE = "ORDER_CHANGE";

    @TableId
    private Long id;

    private Long eventId;
    private Integer allocationVersion;
    private Long sourceAllocationId;
    private Long activeOrderId;
    private Long workOrderId;
    private Long routeProcessId;
    private Long processId;
    private BigDecimal beforeQuantity;
    private BigDecimal afterQuantity;
    private BigDecimal deltaQuantity;
    private Long actorUserId;
    private String adjustmentReason;
    private String allocationMode;
    private String changeSource;
    private LocalDateTime occurredAt;
}
