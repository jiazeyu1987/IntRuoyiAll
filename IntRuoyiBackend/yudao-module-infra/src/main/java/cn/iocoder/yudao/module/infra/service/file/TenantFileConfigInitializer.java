package cn.iocoder.yudao.module.infra.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantFileConfigInitializer {
    private final JdbcTemplate jdbc;

    @Transactional(propagation = Propagation.MANDATORY)
    public void initialize() {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        jdbc.queryForObject("SELECT id FROM system_tenant WHERE id=? FOR UPDATE", Long.class, tenantId);
        if (jdbc.queryForObject("SELECT COUNT(*) FROM infra_tenant_file_config_snapshot WHERE tenant_id=?", Long.class, tenantId) != 0) {
            throw new IllegalStateException("TENANT_FILE_CONFIGURATION_ALREADY_INITIALIZED");
        }
        if (jdbc.queryForObject("SELECT COUNT(*) FROM infra_file_config WHERE master=TRUE AND deleted=FALSE", Long.class) != 1) {
            throw new IllegalStateException("TENANT_FILE_DEFAULT_CONFIG_REQUIRED");
        }
        jdbc.update("""
INSERT INTO infra_tenant_file_config (tenant_id,id,name,storage,remark,master,config,creator,create_time,updater,update_time,deleted)
SELECT t.id,s.`id`,s.`name`,s.`storage`,s.`remark`,s.`master`,s.`config`,s.`creator`,s.`create_time`,s.`updater`,s.`update_time`,s.`deleted` FROM system_tenant t CROSS JOIN infra_file_config s
WHERE NOT EXISTS (SELECT 1 FROM infra_tenant_file_config_snapshot x WHERE x.tenant_id=t.id) AND t.id=?
                """, tenantId);
        jdbc.update("INSERT INTO infra_tenant_file_config_snapshot (tenant_id) VALUES (?)", tenantId);
    }
}
