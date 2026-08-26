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

@TableName("mes_pro_process_pool_active_order_process_snapshot")
@KeySequence("mes_pro_process_pool_active_order_process_snapshot_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProcessPoolActiveOrderProcessSnapshotDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long activeOrderId;
    private Long workOrderId;
    private Long routeId;
    private Long routeVersionId;
    private Long routeProcessId;
    private Long processId;
    private String processCodeSnapshot;
    private String processNameSnapshot;
    private BigDecimal erpFixedQuantitySnapshot;
    private BigDecimal productionQuantityFactorSnapshot;
    private BigDecimal plannedQuantitySnapshot;
    private String parameterSnapshotJson;
    private String parameterSnapshotSha256;
    private String parameterSnapshotState;
    private Boolean simulated;
    private String simulationStage;
    private String simulationRunId;
}
