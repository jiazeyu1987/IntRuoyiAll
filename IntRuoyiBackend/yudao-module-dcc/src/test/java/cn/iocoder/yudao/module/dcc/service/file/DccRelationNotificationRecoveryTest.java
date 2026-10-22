package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.service.file.relations.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.file.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccRelationNotificationRecoveryTest {
    JdbcTemplate jdbc;TransactionTemplate tx;DccRelationNotificationRecoveryService service;
    PermissionApi permission;DccRelationAccessPolicy access;DccRelationNotificationPostCommitScheduler scheduler;GxpAuditService audit;
    @BeforeEach void setup() throws Exception {
        var ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MYSQL;DB_CLOSE_DELAY=-1");
        jdbc=new JdbcTemplate(ds);tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        for(String sql:Files.readString(Path.of("../sql/mysql/20260930_dcc_d_relations.sql")).replaceAll("(?m)^--.*$","").split(";"))if(!sql.isBlank())jdbc.execute(sql);
        jdbc.update("INSERT INTO dcc_relation_controlled_event (id,tenant_id,master_id,controlled_file_id,application_round,event_key,controlled_at) VALUES (1,1,10,100,'r1','key1',CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO dcc_relation_notification_outbox (id,tenant_id,event_id,related_master_id,recipient_user_id,source_file_id,due_at,business_key,status,file_number_snapshot,version_no_snapshot) VALUES (1,1,1,20,8,100,CURRENT_TIMESTAMP,'key-outbox1','FAILED','N','B/1')");
        permission=mock(PermissionApi.class);access=mock(DccRelationAccessPolicy.class);scheduler=mock(DccRelationNotificationPostCommitScheduler.class);audit=mock(GxpAuditService.class);
        when(permission.hasAnyRoles(7L,"doc_control")).thenReturn(true);
        when(permission.hasAnyPermissions(7L,"dcc:controlled-file:approve")).thenReturn(true);
        when(permission.hasAnyPermissions(7L,"dcc:controlled-file:publication-followup:manage")).thenReturn(true);
        service=new DccRelationNotificationRecoveryService(new DccRelationStore(jdbc,audit),permission,access,scheduler);TenantContextHolder.setTenantId(1L);
    }
    @AfterEach void clear(){TenantContextHolder.clear();}
    @Test void genuineManagerRequestsRetryWithoutResettingFrozenOutbox(){
        assertEquals(Boolean.TRUE,tx.execute(s->service.requestRetry(7L,1L,"发送恢复原因")));
        verify(access).assertNameVisible(7L,100L);verify(scheduler).scheduleAfterCommit(1L,1L);verify(audit).append(any());
        assertEquals("FAILED",jdbc.queryForObject("SELECT status FROM dcc_relation_notification_outbox",String.class));
    }
    @Test void sentOnlyEventIsNoOpAndDeniedManagerWrongTenantOrBlankReasonNeverQueues(){
        jdbc.update("UPDATE dcc_relation_notification_outbox SET status='SENT'");
        assertEquals(Boolean.FALSE,tx.execute(s->service.requestRetry(7L,1L,"发送恢复原因")));verifyNoInteractions(scheduler,audit);
        assertThrows(IllegalArgumentException.class,()->tx.execute(s->service.requestRetry(7L,1L," ")));
        assertThrows(IllegalStateException.class,()->tx.execute(s->service.requestRetry(1L,1L,"admin不是文控")));
        TenantContextHolder.setTenantId(2L);assertThrows(IllegalStateException.class,()->tx.execute(s->service.requestRetry(7L,1L,"其他租户")));
        verifyNoInteractions(scheduler,audit);
    }
    @Test void failedRetryAuditDoesNotRegisterAnyCommitCallback(){
        doThrow(new DccRelationFailure("DCC_AUDIT_FAILED")).when(audit).append(any());
        assertThrows(IllegalStateException.class,()->tx.execute(s->service.requestRetry(7L,1L,"恢复原因")));
        verifyNoInteractions(scheduler);assertEquals("FAILED",jdbc.queryForObject("SELECT status FROM dcc_relation_notification_outbox",String.class));
    }
    @Test void notificationStatusIsEventAndTenantScopedAndNamePermissionIsRepeated(){
        assertEquals("FAILED",service.listStatus(7L,1L).get(0).status());
        doThrow(new DccRelationFailure("DCC_NAME_DENIED")).when(access).assertNameVisible(7L,100L);
        assertThrows(IllegalStateException.class,()->service.listStatus(7L,1L));
        TenantContextHolder.setTenantId(2L);assertThrows(IllegalStateException.class,()->service.listStatus(7L,1L));
        verifyNoInteractions(scheduler,audit);
    }
}
