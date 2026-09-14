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

@TableName("mes_pro_process_pool_report_allocation")
@KeySequence("mes_pro_process_pool_report_allocation_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProcessPoolReportAllocationDO extends TenantBaseDO {

    public static final String MODE_FIFO = "FIFO";
    public static final String MODE_MANUAL = "MANUAL";
    public static final String MODE_SYSTEM = "SYSTEM";
    public static final String MODE_FRONTLINE_SELECTED = "FRONTLINE_SELECTED";
    public static final String LIFECYCLE_CURRENT = "CURRENT";
    public static final String LIFECYCLE_SUPERSEDED = "SUPERSEDED";

    @TableId
    private Long id;

    private Long eventId;
    private Long reviewId;
    private Long leaderUserId;
    private Long activeOrderId;
    private Long workOrderId;
    private Long routeProcessId;
    private Long processId;
    private BigDecimal allocatedQuantity;
    private String allocationMode;
    private String lifecycleStatus;
    private Integer createdVersion;
    private Integer supersededVersion;
    private LocalDateTime confirmedAt;
    private Boolean simulated;
    private String simulationStage;
    private String simulationRunId;
}
