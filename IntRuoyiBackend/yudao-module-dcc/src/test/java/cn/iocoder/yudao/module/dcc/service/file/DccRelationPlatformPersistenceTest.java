package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.service.file.relations.*;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.*;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApiImpl;
import cn.iocoder.yudao.module.system.dal.dataobject.notify.NotifyTemplateDO;
import cn.iocoder.yudao.module.system.service.notify.*;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.core.io.ClassPathResource;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Import({NotifySendServiceImpl.class,NotifyMessageServiceImpl.class,NotifyMessageSendApiImpl.class,DccRelationPlatformNotificationSender.class})
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:dcc_d_platform_test;MODE=MYSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1;NON_KEYWORDS=value;LOCK_TIMEOUT=10000")
class DccRelationPlatformPersistenceTest extends BaseDbUnitTest {
    @Resource javax.sql.DataSource dataSource;
    JdbcTemplate jdbc;
    @Resource DccRelationPlatformNotificationSender sender;
    @Resource PlatformTransactionManager manager;
    @MockitoBean DccLatestControlledFileResolver files;
    @MockitoBean NotifyTemplateService templates;
    private DccRelationNotificationDispatcher dispatcher;
    @BeforeEach void setup() throws Exception {
        jdbc=new JdbcTemplate(dataSource);
        try(var connection=dataSource.getConnection()) {
            assertTrue(connection.getMetaData().getURL().startsWith("jdbc:h2:mem:"));
        }
        String platformSql=new ClassPathResource("sql/dcc_d_platform_message_fixture.sql").getContentAsString(StandardCharsets.UTF_8);
        for(String sql:platformSql.split(";")) if(!sql.isBlank())jdbc.execute(sql);
        for(String sql:Files.readString(Path.of("../sql/mysql/20260930_dcc_d_relations.sql")).replaceAll("(?m)^--.*$","").split(";"))if(!sql.isBlank())jdbc.execute(sql);
        jdbc.update("DELETE FROM system_notify_message");jdbc.update("DELETE FROM dcc_relation_notification_outbox");
        var template=NotifyTemplateDO.builder().id(710L).code("dcc_relation_remediation").type(2).nickname("DCC系统")
                .content("受控文件 {fileNumber} {versionNo} 关联整改期限 {dueAt}").status(0)
                .params(List.of("fileNumber","versionNo","relatedMasterId","dueAt","sourceControlledFileId","detailUrl")).build();
        when(templates.getNotifyTemplateByCodeFromCache("dcc_relation_remediation")).thenReturn(template);
        when(templates.formatNotifyTemplateContent(anyString(),anyMap())).thenAnswer(invocation->{
            var config=new NotifyTemplateServiceImpl();return config.formatNotifyTemplateContent(invocation.getArgument(0),invocation.getArgument(1));
        });
        when(files.resolveSelected(100L)).thenReturn(new FileVersion(1L,100L,10L,9L,"N-100","源.pdf","B/1","ACTIVE",true,true,false));
        dispatcher=new DccRelationNotificationDispatcher(new DccRelationStore(jdbc,mock(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService.class)),sender,mock(DccRelationAccessPolicy.class),manager);
        jdbc.update("INSERT INTO dcc_relation_notification_outbox (tenant_id,event_id,related_master_id,recipient_user_id,source_file_id,due_at,business_key,status,file_number_snapshot,version_no_snapshot) VALUES (1,1,20,8,100,?,'dcc-remediation:1:100:round-1:20','PENDING','N-100','B/1')",LocalDateTime.of(2026,10,3,12,0));
    }
    long delivery(){return jdbc.queryForObject("SELECT id FROM dcc_relation_notification_outbox",Long.class);}
    long messages(){return jdbc.queryForObject("SELECT COUNT(*) FROM system_notify_message",Long.class);}
    @Test void realPlatformServicePersistsSingleMessageAcrossOutboxReplayAndResultRecovery() {
        dispatcher.dispatch(delivery());dispatcher.dispatch(delivery());
        assertEquals(1,messages());
        Long original=jdbc.queryForObject("SELECT id FROM system_notify_message",Long.class);
        // Reproduce a committed platform send with missing local result: retry the same immutable outbox identity.
        jdbc.update("UPDATE dcc_relation_notification_outbox SET status='FAILED',platform_message_id=NULL");
        when(files.resolveSelected(100L)).thenReturn(new FileVersion(1L,100L,10L,9L,"N-RENAMED","源.pdf","B/1","OBSOLETE",false,false,false));
        dispatcher.dispatch(delivery());assertEquals(1,messages());
        assertEquals(original,jdbc.queryForObject("SELECT platform_message_id FROM dcc_relation_notification_outbox",Long.class));
        assertEquals(8L,jdbc.queryForObject("SELECT user_id FROM system_notify_message",Long.class));
        assertTrue(jdbc.queryForObject("SELECT template_content FROM system_notify_message",String.class).contains("2026-10-03 12:00:00"));
    }
    @Test void simultaneousDispatchersPersistOnePlatformMessageAndOneSentResult() throws Exception {
        long id=delivery();ExecutorService pool=Executors.newFixedThreadPool(2);
        try {
            Callable<Void> dispatch=()->{TenantContextHolder.setTenantId(1L);try{dispatcher.dispatch(id);}finally{TenantContextHolder.clear();}return null;};
            var a=pool.submit(dispatch);var b=pool.submit(dispatch);a.get(15,TimeUnit.SECONDS);b.get(15,TimeUnit.SECONDS);
        } finally {pool.shutdownNow();}
        assertEquals(1,messages());assertEquals("SENT",jdbc.queryForObject("SELECT status FROM dcc_relation_notification_outbox",String.class));
    }
    @Test void missingTemplatePersistsFailureWithoutPlatformMessageAndBeforeCommitRefusesDelivery() {
        when(templates.getNotifyTemplateByCodeFromCache("dcc_relation_remediation")).thenReturn(null);
        assertThrows(IllegalStateException.class,()->dispatcher.dispatch(delivery()));assertEquals(0,messages());
        assertEquals("FAILED",jdbc.queryForObject("SELECT status FROM dcc_relation_notification_outbox",String.class));
        assertThrows(IllegalStateException.class,()->new TransactionTemplate(manager).executeWithoutResult(s->dispatcher.dispatch(delivery())));
        assertEquals(0,messages());
    }
    @Test void successfulControlAutomaticallyQueuesRealPlatformSendAndRollbackQueuesNothing(){
        jdbc.update("DELETE FROM dcc_relation_notification_outbox");
        jdbc.update("DELETE FROM dcc_relation_remediation_task");jdbc.update("DELETE FROM dcc_relation_arrangement");
        jdbc.update("DELETE FROM dcc_relation_controlled_event");jdbc.update("DELETE FROM dcc_current_file_relation");jdbc.update("DELETE FROM dcc_current_file_relation_set");
        jdbc.update("INSERT INTO dcc_controlled_file_master (id,category_id,file_name,file_number,status,tenant_id) VALUES (10,1,'源.pdf','N-100','ACTIVE',1)");
        jdbc.update("INSERT INTO dcc_controlled_file_related_file (controlled_file_id,related_controlled_file_id,project_code_id,related_master_id,relation_source,tenant_id) VALUES (100,200,9,20,'UPLOAD',1)");
        var source=new FileVersion(1L,100L,10L,9L,"N-100","源.pdf","B/1","ACTIVE",true,true,false);
        when(files.resolveLatestForUpdate(10L)).thenReturn(source);
        var event=new ControlledEvent(1L,10L,100L,"round-1","control-100",LocalDateTime.of(2026,10,1,12,0));
        var store=new DccRelationStore(jdbc,mock(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService.class));
        var queue=new ArrayList<Runnable>();var scheduler=new DccRelationNotificationPostCommitScheduler(queue::add,store,dispatcher);
        var service=new DccRelationRemediationService(store,files,mock(DccRelationAccessPolicy.class),scheduler);
        var tx=new TransactionTemplate(manager);
        tx.executeWithoutResult(s->service.saveArrangements(7L,100L,"round-1",List.of(new Arrangement(20L,8L,LocalDateTime.of(2026,10,3,12,0))),"会签安排"));
        assertThrows(IllegalStateException.class,()->tx.executeWithoutResult(s->{service.recordControlled(event);assertEquals(0,messages());throw new IllegalStateException("MAIN_ROLLBACK");}));
        assertTrue(queue.isEmpty());assertEquals(0,messages());
        tx.executeWithoutResult(s->{service.recordControlled(event);assertTrue(queue.isEmpty());assertEquals(0,messages());});
        assertEquals(1,queue.size());queue.remove(0).run();assertEquals(1,messages());
        assertEquals("SENT",jdbc.queryForObject("SELECT status FROM dcc_relation_notification_outbox",String.class));
        tx.executeWithoutResult(s->service.recordControlled(event));queue.remove(0).run();assertEquals(1,messages());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_relation_remediation_task",Integer.class));
        jdbc.update("DELETE FROM dcc_controlled_file_related_file WHERE controlled_file_id=100");
    }
}
