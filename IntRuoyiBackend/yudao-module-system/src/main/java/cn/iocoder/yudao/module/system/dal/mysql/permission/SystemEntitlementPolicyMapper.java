package cn.iocoder.yudao.module.system.dal.mysql.permission;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.SystemEntitlementPolicyDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SystemEntitlementPolicyMapper extends BaseMapperX<SystemEntitlementPolicyDO> {

    default SystemEntitlementPolicyDO selectByPolicyCode(String policyCode) {
        return selectOne(SystemEntitlementPolicyDO::getPolicyCode, policyCode);
    }

}
