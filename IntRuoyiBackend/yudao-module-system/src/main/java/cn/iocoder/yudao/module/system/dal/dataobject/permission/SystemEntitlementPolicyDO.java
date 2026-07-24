package cn.iocoder.yudao.module.system.dal.dataobject.permission;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("system_entitlement_policy")
@KeySequence("system_entitlement_policy_seq")
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
public class SystemEntitlementPolicyDO extends BaseDO {

    @TableId
    private Long id;

    private String policyCode;

    private String policyName;

    private String moduleCode;

    private Integer status;

    private String description;

    private String allowedPermissionCodesJson;

    private String allowedMenuRefsJson;

    private String forbiddenPermissionCodesJson;

}
