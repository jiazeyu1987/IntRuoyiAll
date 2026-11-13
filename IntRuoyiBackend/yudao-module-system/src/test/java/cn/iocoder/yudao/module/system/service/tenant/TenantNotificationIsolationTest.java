package cn.iocoder.yudao.module.system.service.tenant;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.config.TenantProperties;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.db.TenantDatabaseInterceptor;
import cn.iocoder.yudao.framework.mybatis.core.util.MyBatisUtils;
import cn.iocoder.yudao.module.system.controller.admin.mail.vo.account.MailAccountSaveReqVO;
import cn.iocoder.yudao.module.system.controller.admin.mail.vo.template.MailTemplateSaveReqVO;
import cn.iocoder.yudao.module.system.controller.admin.notify.vo.template.NotifyTemplateSaveReqVO;
import cn.iocoder.yudao.module.system.service.mail.*;
import cn.iocoder.yudao.module.system.service.notify.NotifyTemplateServiceImpl;
import cn.iocoder.yudao.module.system.service.sms.SmsReceiptTenantRouter;
import cn.iocoder.yudao.module.system.framework.sms.core.client.dto.SmsReceiveRespDTO;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@Import({MailAccountServiceImpl.class, MailTemplateServiceImpl.class, NotifyTemplateServiceImpl.class,
        SmsReceiptTenantRouter.class, TenantNotificationConfigInitializer.class, TenantNotificationIsolationTest.SqlConfiguration.class})
class TenantNotificationIsolationTest extends BaseDbUnitTest {
    @Resource private MailAccountService accountService;
    @Resource private MailTemplateService templateService;
    @Resource private NotifyTemplateServiceImpl notifyService;
    @Resource private SmsReceiptTenantRouter router;
    @Resource private JdbcTemplate jdbcTemplate;
    @Resource private TenantNotificationConfigInitializer initializer;
    @Resource private org.springframework.transaction.PlatformTransactionManager transactions;

    @Test
    void accountAndTemplateChangesAndMixedDeletesCannotReachAnotherTenant() {
        TenantContextHolder.setTenantId(11L);
        Long accountA = accountService.createMailAccount(account("a"));
        Long templateA = templateService.createMailTemplate(template(accountA, "A"));
        TenantContextHolder.setTenantId(22L);
        Long accountB = accountService.createMailAccount(account("b"));
        Long templateB = templateService.createMailTemplate(template(accountB, "B"));
        TenantContextHolder.setIgnore(true);
        assertNull(accountService.getMailAccountFromCache(accountA));
        assertNull(templateService.getMailTemplate(templateA));
        assertEquals("B", templateService.getMailTemplateByCodeFromCache("same-code").getContent());
        assertThrows(RuntimeException.class, () -> templateService.createMailTemplate(template(accountA, "illegal").setCode("other")));
        assertThrows(RuntimeException.class, () -> accountService.updateMailAccount(account("changed").setId(accountA)));
        assertThrows(RuntimeException.class, () -> templateService.deleteMailTemplateList(List.of(templateB, templateA)));
        assertNotNull(templateService.getMailTemplate(templateB));
        templateService.updateMailTemplate(template(accountB, "B changed").setId(templateB));
        TenantContextHolder.setTenantId(11L);
        assertEquals("A", templateService.getMailTemplateByCodeFromCache("same-code").getContent());
        assertEquals("a.invalid", accountService.getMailAccount(accountA).getHost());
    }

    @Test
    void notifyTemplateCodeAndMissingTenantAreIsolated() {
        var request = new NotifyTemplateSaveReqVO().setCode("notice").setName("Notice").setNickname("Sender")
                .setType(1).setStatus(0).setContent("A");
        TenantContextHolder.setTenantId(11L);
        Long first = notifyService.createNotifyTemplate(request);
        TenantContextHolder.setTenantId(22L);
        notifyService.createNotifyTemplate(request.setContent("B"));
        assertEquals("B", notifyService.getNotifyTemplateByCodeFromCache("notice").getContent());
        assertNull(notifyService.getNotifyTemplate(first));
        TenantContextHolder.clear();
        assertThrows(RuntimeException.class, () -> notifyService.getNotifyTemplateByCodeFromCache("notice"));
        assertThrows(RuntimeException.class, () -> accountService.getMailAccountList());
    }

    @Test
    void receiptUsesPersistedOwnerAndRestoresCallerContextAndRejectsAmbiguousSerials() {
        // The receipt route deliberately queries only routing metadata, never message content or credentials.
        insertReceipt(11L, 110L, "serial-a");
        insertReceipt(22L, 220L, "serial-b");
        TenantContextHolder.setTenantId(99L);
        TenantContextHolder.setIgnore(true);
        router.route("ALIYUN", new SmsReceiveRespDTO().setLogId(110L), logId -> {
            assertEquals(110L, logId);
            assertEquals(11L, TenantContextHolder.getRequiredTenantId());
            assertFalse(TenantContextHolder.isIgnore());
        });
        assertEquals(99L, TenantContextHolder.getRequiredTenantId());
        assertTrue(TenantContextHolder.isIgnore());
        assertThrows(RuntimeException.class, () -> router.route("TENCENT", new SmsReceiveRespDTO().setLogId(110L), id -> fail("wrong channel")));
        insertReceipt(22L, 221L, "serial-a");
        assertThrows(RuntimeException.class, () -> router.route("ALIYUN", new SmsReceiveRespDTO().setSerialNo("serial-a"), id -> fail("ambiguous owner")));
    }

    @Test
    void creationSnapshotsOnlyNewTenantAndRollsBackWithTheCreationTransaction() {
        jdbcTemplate.update("INSERT INTO system_mail_account (id,mail,username,password,host,port,ssl_enable,starttls_enable) VALUES (10,'a@example.invalid','fixture','fixture','source.invalid',25,false,false)");
        jdbcTemplate.update("INSERT INTO system_mail_template (id,name,code,account_id,title,content,params,status) VALUES (30,'Notice','seed',10,'Title','Original','[]',0)");
        var tx = new org.springframework.transaction.support.TransactionTemplate(transactions);
        for (long tenant : new long[]{11,22}) {
            TenantContextHolder.setTenantId(tenant);
            tx.executeWithoutResult(status -> {
                insertTenant(tenant);
                initializer.initialize(new cn.iocoder.yudao.module.system.api.tenant.TenantCreatedEvent(tenant));
            });
            assertEquals("Original", templateService.getMailTemplateByCodeFromCache("seed").getContent());
        }
        jdbcTemplate.update("UPDATE system_tenant_mail_account SET host='changed.invalid' WHERE tenant_id=11");
        assertEquals("source.invalid", accountService.getMailAccountList().get(0).getHost());
        assertEquals(2L, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM system_tenant_mail_template t JOIN system_tenant_mail_account a ON a.id=t.account_id AND a.tenant_id=t.tenant_id", Long.class));
        TenantContextHolder.setTenantId(33L);
        assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(status -> {
            insertTenant(33L);
            initializer.initialize(new cn.iocoder.yudao.module.system.api.tenant.TenantCreatedEvent(33L));
            throw new IllegalStateException("LATER_CREATION_STEP_FAILED");
        }));
        assertEquals(0L, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM system_tenant WHERE id=33", Long.class));
        assertEquals(0L, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM system_tenant_mail_account WHERE tenant_id=33", Long.class));
        assertThrows(RuntimeException.class, () -> initializer.initialize(new cn.iocoder.yudao.module.system.api.tenant.TenantCreatedEvent(33L)));
    }

    private void insertTenant(long tenant) {
        jdbcTemplate.update("INSERT INTO system_tenant (id,name,contact_name,status,package_id,expire_time,account_count) VALUES (?,?,'Contact',0,1,'2099-01-01 00:00:00',10)", tenant, "Tenant " + tenant);
    }

    private void insertReceipt(long tenant, long id, String serial) {
        jdbcTemplate.update("INSERT INTO system_tenant_sms_log (id,tenant_id,channel_id,channel_code,template_id,template_code,template_type,template_content,template_params,api_template_id,mobile,send_status,receive_status,api_serial_no) VALUES (?,?,1,'ALIYUN',1,'notice',1,'content','{}','external','13800000000',1,0,?)", id, tenant, serial);
    }

    private MailAccountSaveReqVO account(String value) {
        return new MailAccountSaveReqVO().setMail(value+"@example.invalid").setUsername(value).setPassword("fixture")
                .setHost(value+".invalid").setPort(25).setSslEnable(false).setStarttlsEnable(false);
    }
    private MailTemplateSaveReqVO template(Long account, String content) {
        return new MailTemplateSaveReqVO().setAccountId(account).setCode("same-code").setName("Template")
                .setTitle("Title").setContent(content).setStatus(0);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SqlConfiguration {
        @Bean JdbcTemplate jdbcTemplate(javax.sql.DataSource ds) { return new JdbcTemplate(ds); }
        @Bean @Lazy(false) InitializingBean tenantInterceptor(MybatisPlusInterceptor interceptor) {
            return () -> MyBatisUtils.addInterceptor(interceptor,
                    new TenantLineInnerInterceptor(new TenantDatabaseInterceptor(new TenantProperties())), 0);
        }
    }
}
