package cn.iocoder.yudao.module.system.dal.dataobject.permission;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("system_entitlement_audit_event")
@KeySequence("system_entitlement_audit_event_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class SystemEntitlementAuditEventDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String eventType;

    private String sourceType;

    private String sourceKey;

    private String policyCode;

    private String subjectType;

    private Long subjectId;

    private String beforeDigest;

    private String afterDigest;

    private String resultStatus;

    private String message;

    private Long operatorUserId;

    private String operatorUsername;

}
