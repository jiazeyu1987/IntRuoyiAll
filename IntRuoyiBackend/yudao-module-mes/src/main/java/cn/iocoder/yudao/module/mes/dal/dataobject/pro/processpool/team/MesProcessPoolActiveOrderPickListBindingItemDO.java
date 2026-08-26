package cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("mes_pro_process_pool_active_order_pick_list_binding_item")
@KeySequence("mes_pro_process_pool_active_order_pick_list_binding_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProcessPoolActiveOrderPickListBindingItemDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long bindingId;
    private Long pickListItemId;
    private String sourceEntryId;
    private String sourceLineKey;
    private String materialNumber;
    private String materialName;
    private String materialSpecification;
    private String unitName;
    private BigDecimal requestedQuantity;
    private BigDecimal actualQuantity;
    private BigDecimal baseActualQuantity;
    private String lotNumber;
    private String productionOrderNo;
    private Integer productionOrderLineNo;
    private LocalDateTime sourceModifyTime;
    private String itemSnapshotHash;
    private Boolean simulated;
    private String simulationStage;
    private String simulationRunId;
}
