package cn.iocoder.yudao.module.system.dal.dataobject.permission;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("system_entitlement_grant")
@KeySequence("system_entitlement_grant_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class SystemEntitlementGrantDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String subjectType;

    private Long subjectId;

    private Long resolvedUserId;

    private String permissionCode;

    private Long menuId;

    private String policyCode;

    private Integer activeClaimCount;

    private String status;

}
