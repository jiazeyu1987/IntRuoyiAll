package cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("gxp_audit_policy_operation")
@KeySequence("gxp_audit_policy_operation_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class GxpAuditPolicyOperationDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String policyVersion;
    private String operationId;
    private String sourceType;
    private String sourceLocator;
    private String domain;
    private String subjectType;
    private String actionType;
    private String reasonPolicy;
    private String signaturePolicy;
    private String statePolicy;
    private String retentionClass;
    private String testIds;
    private String owner;
    private String applicability;
    private Boolean active;

}
