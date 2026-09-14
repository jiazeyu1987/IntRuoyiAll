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

@TableName("mes_pro_process_pool_team_leader_scope")
@KeySequence("mes_pro_process_pool_team_leader_scope_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProcessPoolTeamLeaderScopeDO extends TenantBaseDO {

    public static final String LEADER_TYPE_PRODUCTION = "PRODUCTION";
    public static final String LEADER_TYPE_PQC = "PQC";
    public static final String SCOPE_TYPE_EMPLOYEE = "EMPLOYEE";
    public static final String SCOPE_TYPE_PROCESS = "PROCESS";
    public static final String SCOPE_TYPE_WORKSTATION = "WORKSTATION";
    public static final String SCOPE_TYPE_PRODUCTION_LINE = "PRODUCTION_LINE";
    public static final String SCOPE_TYPE_EQUIPMENT = "EQUIPMENT";
    public static final String SCOPE_TYPE_ORDER = "ORDER";

    @TableId
    private Long id;

    private Long leaderUserId;
    private String leaderType;
    private String scopeType;
    private Long employeeUserId;
    private Long processId;
    private Long workstationId;
    private Long productionLineId;
    private Long equipmentId;
    private Long workOrderId;
    private Boolean enabled;
}
