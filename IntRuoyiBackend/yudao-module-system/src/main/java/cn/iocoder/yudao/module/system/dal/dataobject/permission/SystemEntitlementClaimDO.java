package cn.iocoder.yudao.module.system.dal.dataobject.permission;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@TableName("system_entitlement_claim")
@KeySequence("system_entitlement_claim_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class SystemEntitlementClaimDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String sourceType;

    private String sourceKey;

    private String sourceVersion;

    private String sourceDigest;

    private String policyCode;

    private String subjectType;

    private Long subjectId;

    private Long resolvedUserId;

    private String status;

    private LocalDateTime effectiveAt;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime revokedAt;

    private String lastSyncStatus;

    private String lastSyncMessage;

    private Long operatorUserId;

    private String operatorUsername;

}
