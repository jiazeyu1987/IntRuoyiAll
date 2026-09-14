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

@TableName("mes_pro_process_pool_defect_reason")
@KeySequence("mes_pro_process_pool_defect_reason_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProcessPoolDefectReasonDO extends TenantBaseDO {

    public static final String REASON_TYPE_LOSS = "LOSS";
    public static final String REASON_TYPE_UNQUALIFIED = "UNQUALIFIED";
    public static final String REASON_TYPE_PQC_FAILURE = "PQC_FAILURE";

    @TableId
    private Long id;

    private Long leaderUserId;
    private String reasonType;
    private String reasonCode;
    private String reasonName;
    private Long routeProcessId;
    private Long processId;
    private Boolean enabled;
    private String remark;
}
