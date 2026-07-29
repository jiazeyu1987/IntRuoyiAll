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
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@TableName("mes_pro_process_pool_quantity_fragment")
@KeySequence("mes_pro_process_pool_quantity_fragment_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProProcessPoolQuantityFragmentDO extends TenantBaseDO {

    public static final String ALLOCATION_STATUS_AVAILABLE = "AVAILABLE";

    @TableId
    private Long id;

    private Long poolId;
    private Long eventId;
    private Long workOrderId;
    private Long routeId;
    private Long routeProcessId;
    private Long processId;
    private String sourceQuantityType;
    private String qualityStatus;
    private BigDecimal totalQuantity;
    private BigDecimal allocatedQuantity;
    private BigDecimal availableQuantity;
    private String allocationStatus;
    private Boolean locked;
    private String rawPayload;
}
