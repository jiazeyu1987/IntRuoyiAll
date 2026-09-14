package cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@TableName("mes_pro_process_pool_team_process_overage_limit")
@KeySequence("mes_pro_process_pool_team_process_overage_limit_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProcessPoolTeamProcessOverageLimitDO extends TenantBaseDO {
    @TableId
    private Long id;
    private Long leaderUserId;
    private Long routeProcessId;
    private Long processId;
    private BigDecimal overagePercent;
    private Boolean enabled;
}
