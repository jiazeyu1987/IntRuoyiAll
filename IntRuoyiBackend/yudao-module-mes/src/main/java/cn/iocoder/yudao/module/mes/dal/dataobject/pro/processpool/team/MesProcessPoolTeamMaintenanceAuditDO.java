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

import java.time.LocalDateTime;

@TableName("mes_pro_process_pool_team_maintenance_audit")
@KeySequence("mes_pro_process_pool_team_maintenance_audit_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProcessPoolTeamMaintenanceAuditDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long leaderUserId;
    private Long operatorUserId;
    private String actionType;
    private String targetType;
    private Long targetId;
    private String resultStatus;
    private String changeSummary;
    private String beforeSnapshot;
    private String afterSnapshot;
    private LocalDateTime auditTime;
}
