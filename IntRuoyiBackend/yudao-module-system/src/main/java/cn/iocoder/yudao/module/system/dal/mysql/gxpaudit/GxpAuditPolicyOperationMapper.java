package cn.iocoder.yudao.module.system.dal.mysql.gxpaudit;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditPolicyOperationDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface GxpAuditPolicyOperationMapper extends BaseMapperX<GxpAuditPolicyOperationDO> {

    @Select("SELECT op.* FROM gxp_audit_policy_operation op "
            + "WHERE op.tenant_id = #{tenantId} "
            + "AND op.operation_id = #{operationId} "
            + "AND op.active = 1 "
            + "AND op.applicability = 'GXP' "
            + "AND op.policy_version = (SELECT activation.policy_version "
            + "FROM gxp_audit_policy_activation activation "
            + "WHERE activation.tenant_id = #{tenantId} "
            + "ORDER BY activation.id DESC LIMIT 1) "
            + "LIMIT 1")
    GxpAuditPolicyOperationDO selectActive(Long tenantId, String operationId);

    default GxpAuditPolicyOperationDO selectByPolicyVersionForUpdate(Long tenantId, String policyVersion,
                                                                      String operationId) {
        return selectOne(new LambdaQueryWrapperX<GxpAuditPolicyOperationDO>()
                .eq(GxpAuditPolicyOperationDO::getTenantId, tenantId)
                .eq(GxpAuditPolicyOperationDO::getPolicyVersion, policyVersion)
                .eq(GxpAuditPolicyOperationDO::getOperationId, operationId)
                .last("FOR UPDATE"));
    }

    default GxpAuditPolicyOperationDO selectByPolicyVersion(Long tenantId, String policyVersion,
                                                             String operationId) {
        return selectOne(new LambdaQueryWrapperX<GxpAuditPolicyOperationDO>()
                .eq(GxpAuditPolicyOperationDO::getTenantId, tenantId)
                .eq(GxpAuditPolicyOperationDO::getPolicyVersion, policyVersion)
                .eq(GxpAuditPolicyOperationDO::getOperationId, operationId));
    }

}
