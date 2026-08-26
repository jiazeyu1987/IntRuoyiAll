package cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

@TableName("mes_pro_process_pool_active_order_pick_list_binding")
@KeySequence("mes_pro_process_pool_active_order_pick_list_binding_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProcessPoolActiveOrderPickListBindingDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long activeOrderId;
    private Long workOrderId;
    private Long pickListId;
    private String sourceFid;
    private String sourceBillNo;
    private String sourceDocumentStatus;
    private LocalDateTime sourceModifyTime;
    private String sourceSnapshotHash;
    private String bindingStatus;
    private Long boundBy;
    private LocalDateTime boundAt;
    private String idempotencyKey;
    private String requestPayloadHash;
    private Integer bindingVersion;
    private Boolean simulated;
    private String simulationStage;
    private String simulationRunId;
}
