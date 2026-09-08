package cn.iocoder.yudao.module.system.dal.dataobject.permission;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("system_temporary_role_grant_audit")
@KeySequence("system_temporary_role_grant_audit_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class TemporaryRoleGrantAuditDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long grantId;

    private String eventType;

    private Long userId;

    private Long roleId;

    private String permissionCode;

    private Long operatorUserId;

    private String operatorUsername;

    private String message;

}
