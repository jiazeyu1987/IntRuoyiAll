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
public class TenantNotificationConfigInitializer {
    private final JdbcTemplate jdbcTemplate;

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void initialize(TenantCreatedEvent event) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        if (!tenantId.equals(event.tenantId())) throw new IllegalStateException("TENANT_INITIALIZATION_CONTEXT_MISMATCH");
        validateSourceReferences();
        jdbcTemplate.update("""
INSERT INTO system_tenant_mail_account (tenant_id,source_id,mail,username,password,host,port,ssl_enable,starttls_enable,creator,create_time,updater,update_time,deleted)
SELECT t.id,s.id,s.mail,s.username,s.password,s.host,s.port,s.ssl_enable,s.starttls_enable,s.creator,s.create_time,s.updater,s.update_time,s.deleted FROM system_tenant t CROSS JOIN system_mail_account s
WHERE NOT EXISTS (SELECT 1 FROM system_tenant_mail_account x WHERE x.tenant_id=t.id AND x.source_id=s.id) AND t.id=?
                """, tenantId);
        jdbcTemplate.update("""
INSERT INTO system_tenant_sms_channel (tenant_id,source_id,signature,code,status,remark,api_key,api_secret,callback_url,creator,create_time,updater,update_time,deleted)
SELECT t.id,s.id,s.signature,s.code,s.status,s.remark,s.api_key,s.api_secret,s.callback_url,s.creator,s.create_time,s.updater,s.update_time,s.deleted FROM system_tenant t CROSS JOIN system_sms_channel s
WHERE NOT EXISTS (SELECT 1 FROM system_tenant_sms_channel x WHERE x.tenant_id=t.id AND x.source_id=s.id) AND t.id=?
                """, tenantId);
        jdbcTemplate.update("""
INSERT INTO system_tenant_mail_template (tenant_id,source_id,name,code,account_id,nickname,title,content,params,status,remark,creator,create_time,updater,update_time,deleted)
SELECT t.id,s.id,s.name,s.code,p.id,s.nickname,s.title,s.content,s.params,s.status,s.remark,s.creator,s.create_time,s.updater,s.update_time,s.deleted FROM system_tenant t CROSS JOIN system_mail_template s JOIN system_tenant_mail_account p ON p.tenant_id=t.id AND p.source_id=s.account_id
WHERE NOT EXISTS (SELECT 1 FROM system_tenant_mail_template x WHERE x.tenant_id=t.id AND x.source_id=s.id) AND t.id=?
                """, tenantId);
        jdbcTemplate.update("""
INSERT INTO system_tenant_sms_template (tenant_id,source_id,type,status,code,name,content,params,remark,api_template_id,channel_id,channel_code,creator,create_time,updater,update_time,deleted)
SELECT t.id,s.id,s.type,s.status,s.code,s.name,s.content,s.params,s.remark,s.api_template_id,p.id,s.channel_code,s.creator,s.create_time,s.updater,s.update_time,s.deleted FROM system_tenant t CROSS JOIN system_sms_template s JOIN system_tenant_sms_channel p ON p.tenant_id=t.id AND p.source_id=s.channel_id
WHERE NOT EXISTS (SELECT 1 FROM system_tenant_sms_template x WHERE x.tenant_id=t.id AND x.source_id=s.id) AND t.id=?
                """, tenantId);
        jdbcTemplate.update("""
INSERT INTO system_tenant_notify_template (tenant_id,source_id,name,code,nickname,content,type,params,status,remark,creator,create_time,updater,update_time,deleted)
SELECT t.id,s.id,s.name,s.code,s.nickname,s.content,s.type,s.params,s.status,s.remark,s.creator,s.create_time,s.updater,s.update_time,s.deleted FROM system_tenant t CROSS JOIN system_notify_template s
WHERE NOT EXISTS (SELECT 1 FROM system_tenant_notify_template x WHERE x.tenant_id=t.id AND x.source_id=s.id) AND t.id=?
                """, tenantId);
    }

    private void validateSourceReferences() {
        Long invalidMail = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM system_mail_template s LEFT JOIN system_mail_account p ON p.id=s.account_id WHERE p.id IS NULL", Long.class);
        Long invalidSms = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM system_sms_template s LEFT JOIN system_sms_channel p ON p.id=s.channel_id WHERE p.id IS NULL", Long.class);
        if (invalidMail != 0 || invalidSms != 0) throw new IllegalStateException("TENANT_NOTIFICATION_SOURCE_REFERENCE_MISSING");
    }
}
