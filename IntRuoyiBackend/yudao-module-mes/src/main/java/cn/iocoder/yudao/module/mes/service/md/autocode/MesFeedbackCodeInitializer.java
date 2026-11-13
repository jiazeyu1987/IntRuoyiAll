package cn.iocoder.yudao.module.mes.service.md.autocode;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.api.tenant.TenantCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 新租户正式报工所需的规则；已有配置不覆盖，提交时不自动补规则。 */
@Component
@RequiredArgsConstructor
public class MesFeedbackCodeInitializer {
    private final JdbcTemplate jdbc;

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void initialize(TenantCreatedEvent event) {
        Long tenant = TenantContextHolder.getRequiredTenantId();
        if (!tenant.equals(event.tenantId())) {
            throw new IllegalStateException("TENANT_INITIALIZATION_CONTEXT_MISMATCH");
        }
        jdbc.queryForObject("SELECT id FROM system_tenant WHERE id=? FOR UPDATE", Long.class, tenant);
        Long count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM mes_md_auto_code_rule
                WHERE tenant_id=? AND code='PRO_FEEDBACK_CODE' AND project_id IS NULL AND deleted=0
                """, Long.class, tenant);
        if (count == null || count > 1) throw new IllegalStateException("FEEDBACK_CODE_RULE_COUNT_INVALID");
        if (count == 1) return;
        // 新租户创建事务中还没有报工历史。
        jdbc.update("""
                INSERT INTO mes_md_auto_code_rule (tenant_id,code,name,max_length,padded,status)
                VALUES (?,'PRO_FEEDBACK_CODE','生产报工单编码',64,0,0)
                """, tenant);
        Long rule = jdbc.queryForObject("""
                SELECT id FROM mes_md_auto_code_rule
                WHERE tenant_id=? AND code='PRO_FEEDBACK_CODE' AND project_id IS NULL AND deleted=0
                """, Long.class, tenant);
        jdbc.update("""
                INSERT INTO mes_md_auto_code_part (tenant_id,rule_id,sort,type,length,fix_character)
                VALUES (?,?,1,3,3,'FB-')
                """, tenant, rule);
        jdbc.update("""
                INSERT INTO mes_md_auto_code_part
                (tenant_id,rule_id,sort,type,length,serial_start_no,serial_step,cycle_flag)
                VALUES (?,?,2,4,6,1,1,0)
                """, tenant, rule);
    }
}
