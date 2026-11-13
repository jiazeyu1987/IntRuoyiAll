package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.service.file.relations.*;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApiImpl;
import cn.iocoder.yudao.module.system.dal.dataobject.notify.NotifyTemplateDO;
import cn.iocoder.yudao.module.system.service.notify.*;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Import({NotifySendServiceImpl.class, NotifyMessageServiceImpl.class, NotifyMessageSendApiImpl.class,
        DccRelatedFileRevisionNotifier.class, DccRelatedFileRevisionNotifierTest.Config.class})
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:dcc_reverse_revision;MODE=MYSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1;NON_KEYWORDS=value")
class DccRelatedFileRevisionNotifierTest extends BaseDbUnitTest {
    @Configuration static class Config {
        @Bean DccRelationStore relationStore(javax.sql.DataSource ds) {
            return new DccRelationStore(new JdbcTemplate(ds), mock(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService.class));
        }
    }
    @Resource javax.sql.DataSource dataSource;
    @Resource PlatformTransactionManager manager;
    @Resource DccRelatedFileRevisionNotifier notifier;
    @MockitoBean DccControlledFileMapper files;
    @MockitoBean NotifyTemplateService templates;
    JdbcTemplate jdbc;
    DccControlledFileDO revised, source;
    DccControlledFileLifecycleEvent event;

    @BeforeEach void setup() throws Exception {
        TenantContextHolder.setTenantId(1L); jdbc = new JdbcTemplate(dataSource);
        String sql = new ClassPathResource("sql/dcc_d_platform_message_fixture.sql").getContentAsString(StandardCharsets.UTF_8);
        for (String statement : sql.split(";")) if (!statement.isBlank()) jdbc.execute(statement);
        jdbc.execute("CREATE TABLE IF NOT EXISTS dcc_current_file_relation (tenant_id BIGINT,source_master_id BIGINT,related_master_id BIGINT)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS dcc_current_file_relation_set (tenant_id BIGINT,source_master_id BIGINT,controlled_file_id BIGINT)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS dcc_controlled_file (id BIGINT,tenant_id BIGINT,master_id BIGINT,deleted INT,controlled_time DATETIME,status VARCHAR(40))");
        jdbc.update("DELETE FROM system_tenant_notify_message");
        jdbc.update("DELETE FROM dcc_current_file_relation"); jdbc.update("DELETE FROM dcc_current_file_relation_set");
        jdbc.update("DELETE FROM dcc_controlled_file");
        jdbc.update("INSERT INTO dcc_current_file_relation VALUES (1,10,20),(2,30,20)");
        jdbc.update("INSERT INTO dcc_current_file_relation_set VALUES (1,10,100),(2,30,300)");
        jdbc.update("INSERT INTO dcc_controlled_file (id,tenant_id,master_id,deleted,controlled_time,status,category_id,directory_id,source_file_id,original_file_id,file_name,title,file_number,version_no,submitter_id,requester_id) "
                + "VALUES (100,1,10,0,NOW(),'ACTIVE',1,1,1,1,'A.docx','A','A','A/1',7,7),"
                + "(300,2,30,0,NOW(),'ACTIVE',1,1,1,1,'C.docx','C','C','A/1',9,9)");
        revised = DccControlledFileDO.builder().id(200L).masterId(20L).fileNumber("B").versionNo("A/2")
                .changeType("REVISION").revisionSourceControlledFileId(199L).controlledTime(LocalDateTime.now()).build();
        revised.setTenantId(1L);
        source = DccControlledFileDO.builder().id(100L).masterId(10L).fileNumber("A").requesterId(7L)
                .fileOwnerUserId(8L).fileOwnerSignatureId(80L).build(); source.setTenantId(1L);
        when(files.selectById(200L)).thenReturn(revised); when(files.selectById(100L)).thenReturn(source);
        var template = NotifyTemplateDO.builder().id(711L).code(DccRelatedFileRevisionNotifier.TEMPLATE_CODE)
                .type(2).nickname("DCC系统").status(0).content("{relatedFileNumber} 关联的 {fileNumber} 升版 {versionNo}")
                .params(List.of("fileNumber","versionNo","relatedFileNumber","sourceControlledFileId","revisedControlledFileId","detailUrl")).build();
        when(templates.getNotifyTemplateByCodeFromCache(template.getCode())).thenReturn(template);
        when(templates.formatNotifyTemplateContent(anyString(), anyMap())).thenAnswer(i ->
                new NotifyTemplateServiceImpl().formatNotifyTemplateContent(i.getArgument(0), i.getArgument(1)));
        event = new DccControlledFileLifecycleEvent("controlled-200", "CONTROLLED",1L,20L,200L,199L,"A/2","round",revised.getControlledTime());
    }
    @AfterEach void cleanup() { TenantContextHolder.clear(); }
    void notifyRevision() { new TransactionTemplate(manager).executeWithoutResult(s -> notifier.onLifecycleEvent(event)); }
    int count() { return jdbc.queryForObject("SELECT COUNT(*) FROM system_tenant_notify_message", Integer.class); }

    @Test void signedCompilerReceivesOneRealPlatformMessageAcrossReplay() {
        notifyRevision(); notifyRevision(); assertEquals(1, count());
        assertEquals(8L, jdbc.queryForObject("SELECT user_id FROM system_tenant_notify_message", Long.class));
        assertTrue(jdbc.queryForObject("SELECT template_content FROM system_tenant_notify_message", String.class).contains("A 关联的 B"));
        verify(files, never()).selectById(300L);
    }
    @Test void outerControlRollbackAlsoRollsBackRealPlatformMessage() {
        assertThrows(IllegalStateException.class, () -> new TransactionTemplate(manager).executeWithoutResult(s -> {
            notifier.onLifecycleEvent(event); assertEquals(1, count()); throw new IllegalStateException("control failed");
        }));
        assertEquals(0, count());
    }
    @Test void removedRelationsAndInitialUploadsDoNotNotify() {
        jdbc.update("DELETE FROM dcc_current_file_relation WHERE tenant_id=1"); notifyRevision(); assertEquals(0,count());
        jdbc.update("INSERT INTO dcc_current_file_relation VALUES (1,10,20)");
        revised.setChangeType("NEW"); notifyRevision(); assertEquals(0,count());
    }
    @Test void laterActivationDoesNotRepeatControlledRevisionNotification() {
        notifyRevision(); assertEquals(1, count());
        for (String type : List.of("EFFECTIVE", "ACTIVATED")) {
            var activation = new DccControlledFileLifecycleEvent(type + "-200", type,1L,20L,200L,199L,
                    "A/2","round",revised.getControlledTime().plusDays(1));
            new TransactionTemplate(manager).executeWithoutResult(s -> notifier.onLifecycleEvent(activation));
        }
        assertEquals(1, count());
    }
    @Test void unavailableTemplateFailsControlRatherThanPretendingToSend() {
        when(templates.getNotifyTemplateByCodeFromCache(DccRelatedFileRevisionNotifier.TEMPLATE_CODE)).thenReturn(null);
        assertThrows(RuntimeException.class, this::notifyRevision); assertEquals(0,count());
    }
    @Test void missingSignedCompilerFailsInsteadOfNotifyingRequester() {
        source.setFileOwnerUserId(null);
        assertThrows(DccRelationFailure.class, this::notifyRevision); assertEquals(0,count());
    }
}
