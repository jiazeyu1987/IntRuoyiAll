package cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@TableName("mes_kingdee_production_order_sync_record")
@KeySequence("mes_kingdee_production_order_sync_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesKingdeeProductionOrderSyncRecordDO extends BaseDO {

    @TableId
    private Long id;

    private String sourceFid;

    private String sourceBillNo;

    private String sourceMaterialNumber;

    private Long workOrderId;

}
