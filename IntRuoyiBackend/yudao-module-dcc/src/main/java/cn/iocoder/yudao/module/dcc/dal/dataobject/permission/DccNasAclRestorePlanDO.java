package cn.iocoder.yudao.module.dcc.dal.dataobject.permission;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@TableName("dcc_nas_acl_restore_plan")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccNasAclRestorePlanDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long snapshotId;
    private Long transferTaskId;
    private String planKey;
    private String targetModel;
    private String status;
    private String semanticPolicyVersion;
    private String identityMappingVersion;
    private String validationSummaryJson;
    private Long createdByUserId;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String failureCode;
    private String failureMessage;

}
