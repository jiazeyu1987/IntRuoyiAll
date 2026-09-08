package cn.iocoder.yudao.module.system.dal.mysql.gxpaudit;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditPolicyOperationDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GxpAuditPolicyOperationMapper extends BaseMapperX<GxpAuditPolicyOperationDO> {

    default GxpAuditPolicyOperationDO selectActive(Long tenantId, String operationId) {
        return selectOne(new LambdaQueryWrapperX<GxpAuditPolicyOperationDO>()
                .eq(GxpAuditPolicyOperationDO::getTenantId, tenantId)
                .eq(GxpAuditPolicyOperationDO::getOperationId, operationId)
                .eq(GxpAuditPolicyOperationDO::getActive, true)
                .eq(GxpAuditPolicyOperationDO::getApplicability, "GXP")
                .orderByDesc(GxpAuditPolicyOperationDO::getId)
                .last("LIMIT 1"));
    }

}
