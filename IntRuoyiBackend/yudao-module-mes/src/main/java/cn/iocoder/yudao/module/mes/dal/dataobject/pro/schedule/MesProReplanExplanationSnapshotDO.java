package cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule;

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

import java.time.LocalDateTime;

@TableName("mes_pro_replan_explanation_snapshot")
@KeySequence("mes_pro_replan_explanation_snapshot_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProReplanExplanationSnapshotDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String requestId;

    private String triggerSource;

    private String capacityMode;

    private String reason;

    private Long operatorId;

    private String operatorName;

    private LocalDateTime requestStartTime;

    private LocalDateTime appliedAt;

    private String snapshotJson;
}
