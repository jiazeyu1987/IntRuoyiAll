package cn.iocoder.yudao.module.system.dal.mysql.gxpaudit;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface GxpAuditEventMapper extends BaseMapperX<GxpAuditEventDO> {

    @Select("SELECT COALESCE(MAX(ledger_sequence), 0) FROM gxp_audit_event WHERE tenant_id = #{tenantId}")
    Long selectMaxLedgerSequence(Long tenantId);

    default GxpAuditEventDO selectLatestByTenant(Long tenantId) {
        return selectOne(new LambdaQueryWrapperX<GxpAuditEventDO>()
                .eq(GxpAuditEventDO::getTenantId, tenantId)
                .orderByDesc(GxpAuditEventDO::getLedgerSequence)
                .last("LIMIT 1"));
    }

    default GxpAuditEventDO selectByIdempotencyKey(Long tenantId, String idempotencyKey) {
        return selectOne(new LambdaQueryWrapperX<GxpAuditEventDO>()
                .eq(GxpAuditEventDO::getTenantId, tenantId)
                .eq(GxpAuditEventDO::getIdempotencyKey, idempotencyKey));
    }

}
