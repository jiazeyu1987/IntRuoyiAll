package cn.iocoder.yudao.module.system.dal.mysql.gxpaudit;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditLedgerSequenceDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface GxpAuditLedgerSequenceMapper extends BaseMapperX<GxpAuditLedgerSequenceDO> {

    @Select("SELECT tenant_id, next_ledger_sequence, create_time, update_time FROM gxp_audit_ledger_sequence WHERE tenant_id = #{tenantId} FOR UPDATE")
    GxpAuditLedgerSequenceDO selectByTenantIdForUpdate(Long tenantId);

}
