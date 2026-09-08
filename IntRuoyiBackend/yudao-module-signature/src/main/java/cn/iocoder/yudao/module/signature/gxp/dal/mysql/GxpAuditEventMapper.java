package cn.iocoder.yudao.module.signature.gxp.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.signature.gxp.dal.dataobject.GxpAuditEventDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface GxpAuditEventMapper extends BaseMapperX<GxpAuditEventDO> {

    default GxpAuditEventDO selectByTenantIdAndIdempotencyKey(Long tenantId, String idempotencyKey) {
        return selectOne(new LambdaQueryWrapperX<GxpAuditEventDO>()
                .eq(GxpAuditEventDO::getTenantId, tenantId)
                .eq(GxpAuditEventDO::getIdempotencyKey, idempotencyKey));
    }

    @Select("SELECT COALESCE(MAX(ledger_sequence), 0) FROM gxp_audit_event WHERE tenant_id = #{tenantId}")
    Long selectMaxLedgerSequence(@Param("tenantId") Long tenantId);

}
