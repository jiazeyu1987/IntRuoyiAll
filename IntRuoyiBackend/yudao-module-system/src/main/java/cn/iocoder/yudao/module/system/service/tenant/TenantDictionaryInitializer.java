package cn.iocoder.yudao.module.system.service.tenant;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.api.tenant.TenantCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TenantDictionaryInitializer {
    private final JdbcTemplate jdbcTemplate;

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void initialize(TenantCreatedEvent event) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        if (!tenantId.equals(event.tenantId())) throw new IllegalStateException("TENANT_INITIALIZATION_CONTEXT_MISMATCH");
        // Lock the tenant so concurrent initialization cannot copy the template twice.
        jdbcTemplate.queryForObject("SELECT id FROM system_tenant WHERE id=? FOR UPDATE", Long.class, tenantId);
        if (jdbcTemplate.queryForObject("SELECT COUNT(*) FROM system_tenant_dict_snapshot WHERE tenant_id=?", Long.class, tenantId) != 0) return;
        if (jdbcTemplate.queryForObject("""
SELECT COUNT(*) FROM system_dict_data d WHERE d.deleted=0 AND NOT EXISTS
    (SELECT 1 FROM system_dict_type t WHERE t.type=d.dict_type AND t.deleted=0)
                """, Long.class) != 0) throw new IllegalStateException("TENANT_DICTIONARY_SOURCE_TYPE_MISSING");
        jdbcTemplate.update("""
INSERT INTO system_tenant_dict_type (tenant_id,name,type,status,remark,creator,create_time,updater,update_time,deleted,deleted_time)
SELECT t.id,s.name,s.type,s.status,s.remark,s.creator,s.create_time,s.updater,s.update_time,s.deleted,s.deleted_time FROM system_tenant t CROSS JOIN system_dict_type s
WHERE NOT EXISTS (SELECT 1 FROM system_tenant_dict_snapshot x WHERE x.tenant_id=t.id) AND t.id=?
                """, tenantId);
        jdbcTemplate.update("""
INSERT INTO system_tenant_dict_data (tenant_id,sort,label,value,dict_type,status,color_type,css_class,remark,creator,create_time,updater,update_time,deleted)
SELECT t.id,s.sort,s.label,s.value,s.dict_type,s.status,s.color_type,s.css_class,s.remark,s.creator,s.create_time,s.updater,s.update_time,s.deleted FROM system_tenant t CROSS JOIN system_dict_data s
WHERE NOT EXISTS (SELECT 1 FROM system_tenant_dict_snapshot x WHERE x.tenant_id=t.id) AND t.id=?
                """, tenantId);
        jdbcTemplate.update("INSERT INTO system_tenant_dict_snapshot (tenant_id) VALUES (?)", tenantId);
    }
}
