package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.service.file.relations.*;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccRelationPostCommitSchedulerTest {
    JdbcTemplate jdbc;TransactionTemplate tx;DccRelationNotificationDispatcher dispatcher;
    DccRelationStore store;List<Runnable> queue;
    @BeforeEach void setup() throws Exception {
        var ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MYSQL;DB_CLOSE_DELAY=-1");
        jdbc=new JdbcTemplate(ds);tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        for(String sql:Files.readString(Path.of("../sql/mysql/20260930_dcc_d_relations.sql")).replaceAll("(?m)^--.*$","").split(";"))if(!sql.isBlank())jdbc.execute(sql);
        store=new DccRelationStore(jdbc,mock(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService.class));
        dispatcher=mock(DccRelationNotificationDispatcher.class);queue=new ArrayList<>();TenantContextHolder.setTenantId(1L);
    }
    @AfterEach void clear(){TenantContextHolder.clear();}
    void insert(long id,long event,String status,long tenant){
        jdbc.update("INSERT INTO dcc_relation_notification_outbox (id,tenant_id,event_id,related_master_id,recipient_user_id,source_file_id,due_at,business_key,status,file_number_snapshot,version_no_snapshot) VALUES (?,?,?,?,8,100,CURRENT_TIMESTAMP,?,?, 'N','B/1')",
                id,tenant,event,id,"KEY-"+id,status);
    }
    DccRelationNotificationPostCommitScheduler scheduler(Executor executor){return new DccRelationNotificationPostCommitScheduler(executor,store,dispatcher);}
    @Test void committedControlQueuesOnlyAfterCommitAndRollbackQueuesNothing(){
        var scheduler=scheduler(queue::add);
        tx.executeWithoutResult(s->{insert(1,10,"PENDING",1);scheduler.scheduleAfterCommit(1L,10L);assertTrue(queue.isEmpty());});
        assertEquals(1,queue.size());
        assertThrows(IllegalStateException.class,()->tx.executeWithoutResult(s->{insert(2,20,"PENDING",1);scheduler.scheduleAfterCommit(1L,20L);throw new IllegalStateException("ROLLBACK");}));
        assertEquals(1,queue.size());assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_relation_notification_outbox",Integer.class));
    }
    @Test void dispatchesOnlyEventTenantUnsentRowsAndRestoresWorkerTenantAfterOneFailure(){
        insert(1,10,"PENDING",1);insert(2,10,"FAILED",1);insert(3,10,"SENT",1);insert(4,10,"PENDING",2);insert(5,20,"PENDING",1);
        var scheduler=scheduler(queue::add);tx.executeWithoutResult(s->scheduler.scheduleAfterCommit(1L,10L));
        doThrow(new DccRelationFailure("DCC_NOTIFICATION_DELIVERY_FAILED_RETRYABLE")).when(dispatcher).dispatch(1L);
        TenantContextHolder.setTenantId(9L);queue.get(0).run();assertEquals(9L,TenantContextHolder.getTenantId());
        verify(dispatcher).dispatch(1L);verify(dispatcher).dispatch(2L);verify(dispatcher,never()).dispatch(3L);verify(dispatcher,never()).dispatch(4L);verify(dispatcher,never()).dispatch(5L);
    }
    @Test void executorRejectionDoesNotUndoCommittedControlAndKeepsOutboxPending(){
        var scheduler=scheduler(r->{throw new RejectedExecutionException("SECRET_DETAILS");});
        tx.executeWithoutResult(s->{insert(1,10,"PENDING",1);scheduler.scheduleAfterCommit(1L,10L);});
        assertEquals("PENDING",jdbc.queryForObject("SELECT status FROM dcc_relation_notification_outbox",String.class));verifyNoInteractions(dispatcher);
    }
    @Test void refusesScheduleOutsideTransactionOrForWrongTenant(){
        var scheduler=scheduler(queue::add);
        assertThrows(IllegalStateException.class,()->scheduler.scheduleAfterCommit(1L,10L));
        assertThrows(IllegalStateException.class,()->tx.executeWithoutResult(s->scheduler.scheduleAfterCommit(2L,10L)));
        assertTrue(queue.isEmpty());
    }
}
