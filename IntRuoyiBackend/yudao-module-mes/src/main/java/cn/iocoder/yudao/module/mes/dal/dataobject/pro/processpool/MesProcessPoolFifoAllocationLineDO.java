package cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool;

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

import java.math.BigDecimal;

@TableName("mes_pro_process_pool_fifo_allocation_line")
@KeySequence("mes_pro_process_pool_fifo_allocation_line_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProcessPoolFifoAllocationLineDO extends TenantBaseDO {

    public static final String STATUS_ALLOCATED = "ALLOCATED";

    @TableId
    private Long id;

    private String allocationBatchNo;
    private Long processPoolId;
    private Long sourceEventId;
    private Long sourceQuantityFragmentId;
    private Long sourceRouteProcessId;
    private Long sourceProcessId;
    private BigDecimal sourceFragmentQuantity;
    private Long targetWorkOrderId;
    private String targetWorkOrderCode;
    private Long targetRouteProcessId;
    private Long targetProcessId;
    private BigDecimal allocatedQuantity;
    private String allocationStatus;
    private String remark;

}
