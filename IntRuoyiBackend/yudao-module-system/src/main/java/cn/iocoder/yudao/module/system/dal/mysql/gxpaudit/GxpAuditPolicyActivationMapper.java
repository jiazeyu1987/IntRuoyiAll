package cn.iocoder.yudao.module.system.dal.mysql.gxpaudit;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditPolicyActivationDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GxpAuditPolicyActivationMapper extends BaseMapperX<GxpAuditPolicyActivationDO> {

    /** Caller must explicitly scope cross-tenant access; normal tenant interception stays enabled. */
    @Select("SELECT activation.* FROM gxp_audit_policy_activation activation "
            + "WHERE activation.id IN (SELECT MAX(history.id) FROM gxp_audit_policy_activation history "
            + "GROUP BY history.tenant_id) ORDER BY activation.tenant_id")
    List<GxpAuditPolicyActivationDO> selectLatestAcrossTenants();

    default GxpAuditPolicyActivationDO selectByRequestIdForUpdate(Long tenantId, String requestId) {
        return selectOne(new LambdaQueryWrapperX<GxpAuditPolicyActivationDO>()
                .eq(GxpAuditPolicyActivationDO::getTenantId, tenantId)
                .eq(GxpAuditPolicyActivationDO::getRequestId, requestId).last("FOR UPDATE"));
    }

    default GxpAuditPolicyActivationDO selectLatestForUpdate(Long tenantId) {
        return selectOne(new LambdaQueryWrapperX<GxpAuditPolicyActivationDO>()
                .eq(GxpAuditPolicyActivationDO::getTenantId, tenantId)
                .orderByDesc(GxpAuditPolicyActivationDO::getId).last("LIMIT 1 FOR UPDATE"));
    }

    default GxpAuditPolicyActivationDO selectLatest(Long tenantId) {
        return selectOne(new LambdaQueryWrapperX<GxpAuditPolicyActivationDO>()
                .eq(GxpAuditPolicyActivationDO::getTenantId, tenantId)
                .orderByDesc(GxpAuditPolicyActivationDO::getId)
                .last("LIMIT 1"));
    }

    default GxpAuditPolicyActivationDO selectByRequestId(Long tenantId, String requestId) {
        return selectOne(new LambdaQueryWrapperX<GxpAuditPolicyActivationDO>()
                .eq(GxpAuditPolicyActivationDO::getTenantId, tenantId)
                .eq(GxpAuditPolicyActivationDO::getRequestId, requestId));
    }
}
