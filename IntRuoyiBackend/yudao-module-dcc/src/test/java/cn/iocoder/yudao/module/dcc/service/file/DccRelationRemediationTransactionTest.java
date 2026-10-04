package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.service.file.relations.*;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.*;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccRelationRemediationTransactionTest {
    JdbcTemplate jdbc; TransactionTemplate tx; DccRelationRemediationService service;
    DccLatestControlledFileResolver files; DccRelationAccessPolicy access;
    DccRelationNotificationPostCommitScheduler scheduler;
    LocalDateTime time=LocalDateTime.of(2026,9,30,12,0);
    ControlledEvent event=new ControlledEvent(1L,10L,100L,"round-1","controlled-100-round-1",time);
    @BeforeEach void setup() throws Exception {
        JdbcDataSource ds=new JdbcDataSource(); ds.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000");
        jdbc=new JdbcTemplate(ds); tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        jdbc.execute("CREATE TABLE dcc_controlled_file_master (id BIGINT,tenant_id BIGINT,deleted INT)");
        jdbc.update("INSERT INTO dcc_controlled_file_master VALUES (10,1,0)");
        jdbc.execute("CREATE TABLE dcc_controlled_file_related_file (tenant_id BIGINT,controlled_file_id BIGINT,related_master_id BIGINT,deleted INT)");
        jdbc.update("INSERT INTO dcc_controlled_file_related_file VALUES (1,100,20,0),(1,100,30,0)");
        for(String sql:Files.readString(Path.of("../sql/mysql/20260930_dcc_d_relations.sql")).replaceAll("(?m)^--.*$","").split(";"))
            if(!sql.isBlank()) jdbc.execute(sql);
        files=mock(DccLatestControlledFileResolver.class); access=mock(DccRelationAccessPolicy.class);
        when(files.resolveSelected(100L)).thenReturn(new FileVersion(1L,100L,10L,9L,"N","源.pdf","A/1","ACTIVE",true,false,true));
        when(files.resolveLatestForUpdate(10L)).thenReturn(new FileVersion(1L,100L,10L,9L,"N","源.pdf","A/1","ACTIVE",true,false,true));
        scheduler=mock(DccRelationNotificationPostCommitScheduler.class);
        service=new DccRelationRemediationService(new DccRelationStore(jdbc, mock(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService.class)),files,access,scheduler);
        TenantContextHolder.setTenantId(1L);
    }
    @AfterEach void cleanup(){TenantContextHolder.clear();}
    void save(Arrangement... rows){tx.executeWithoutResult(s->service.saveArrangements(7L,100L,"round-1",List.of(rows),"会签整改安排原因"));}
    void record(){tx.executeWithoutResult(s->service.recordControlled(event));}
    int count(String table){return jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class);}
    @Test void onlySelectedPeopleGetTasksAndOutboxAtControlNotAtSave() {
        save(new Arrangement(20L,8L,time.plusDays(3)));
        assertEquals(0,count("dcc_relation_notification_outbox")); record();
        assertEquals(1,count("dcc_relation_remediation_task")); assertEquals(1,count("dcc_relation_notification_outbox"));
        assertEquals(8L,jdbc.queryForObject("SELECT recipient_user_id FROM dcc_relation_notification_outbox",Long.class));
        assertEquals(time.plusDays(3),jdbc.queryForObject("SELECT due_at FROM dcc_relation_remediation_task",LocalDateTime.class));
        assertEquals(1,service.listAssignedTasks(8L).size());assertEquals(0,service.listAssignedTasks(9L).size());
        assertEquals(2,count("dcc_current_file_relation"));assertEquals(1,count("dcc_current_file_relation_set"));
    }
    @Test void rejectedWithdrawnFailedControlAndRollbackProduceZeroTasksOrNotifications() {
        save(new Arrangement(20L,8L,time.plusDays(3)));
        doThrow(new IllegalStateException("CONTROL_NOT_SUCCESSFUL")).when(files).assertControlledEvent(event);
        assertThrows(IllegalStateException.class,this::record);
        assertEquals(0,count("dcc_relation_controlled_event")); assertEquals(0,count("dcc_relation_notification_outbox"));
        reset(files); when(files.resolveSelected(100L)).thenReturn(new FileVersion(1L,100L,10L,9L,"N","源.pdf","A/1","ACTIVE",true,false,true));
        when(files.resolveLatestForUpdate(10L)).thenReturn(new FileVersion(1L,100L,10L,9L,"N","源.pdf","A/1","ACTIVE",true,false,true));
        assertThrows(IllegalStateException.class,()->tx.executeWithoutResult(s->{service.recordControlled(event); throw new IllegalStateException("MAIN_TX_FAILED");}));
        assertEquals(0,count("dcc_relation_remediation_task")); assertEquals(0,count("dcc_relation_notification_outbox"));
    }
    @Test void sameEventReplayDoesNotDuplicateEvenWhenMutableStatusLaterChanges() {
        save(new Arrangement(20L,8L,time.plusDays(3))); record();
        doThrow(new IllegalStateException("NOW_OBSOLETE")).when(files).assertControlledEvent(event);
        record(); assertEquals(1,count("dcc_relation_remediation_task")); assertEquals(1,count("dcc_relation_notification_outbox"));
    }
    @Test void conflictingArrangementOrChangedReplayCannotOverwriteFrozenFacts() {
        save(new Arrangement(20L,8L,time.plusDays(3))); save(new Arrangement(20L,8L,time.plusDays(3)));
        assertThrows(IllegalStateException.class,()->save(new Arrangement(20L,9L,time.plusDays(4))));
        record();
        assertThrows(IllegalStateException.class,()->tx.executeWithoutResult(s->service.recordControlled(
            new ControlledEvent(1L,10L,100L,"round-1","different-key",time))));
        assertThrows(IllegalStateException.class,()->save(new Arrangement(30L,9L,time.plusDays(4))));
        assertEquals(1,count("dcc_relation_arrangement"));
    }
    @Test void unknownRelationMissingDeadlineWrongTenantAndUnauthorizedArrangementRejectAllWrites() {
        assertThrows(IllegalArgumentException.class,()->save(new Arrangement(20L,8L,null)));
        assertThrows(IllegalStateException.class,()->save(new Arrangement(99L,8L,time.plusDays(3))));
        doThrow(new IllegalStateException("NOT_COUNTERSIGN_TASK")).when(access).assertCanArrange(7L,100L,"round-1");
        assertThrows(IllegalStateException.class,()->save(new Arrangement(20L,8L,time.plusDays(3))));
        TenantContextHolder.setTenantId(2L); assertThrows(IllegalStateException.class,this::record);
        assertEquals(0,count("dcc_relation_arrangement")); assertEquals(0,count("dcc_relation_notification_outbox"));
    }
    @Test void concurrentEventConsumersMaterializeOnce() throws Exception {
        save(new Arrangement(20L,8L,time.plusDays(3)));
        ExecutorService pool=Executors.newFixedThreadPool(2);
        try {
            Callable<Void> call=()->{TenantContextHolder.setTenantId(1L);try{record();}finally{TenantContextHolder.clear();}return null;};
            Future<Void> a=pool.submit(call),b=pool.submit(call);a.get(15,TimeUnit.SECONDS);b.get(15,TimeUnit.SECONDS);
        } finally {pool.shutdownNow();}
        assertEquals(1,count("dcc_relation_controlled_event")); assertEquals(1,count("dcc_relation_notification_outbox"));
    }
    DccRelationNotificationDispatcher dispatcher(DccRelationNotificationSender sender) {
        return new DccRelationNotificationDispatcher(new DccRelationStore(jdbc,mock(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService.class)),
                sender,access,tx.getTransactionManager());
    }
    long deliveryId(){return jdbc.queryForObject("SELECT id FROM dcc_relation_notification_outbox",Long.class);}
    @Test void dispatcherReplaysOnceAndRequiresCommittedControl() {
        var sender=mock(DccRelationNotificationSender.class);when(sender.send(any())).thenReturn(800L);
        save(new Arrangement(20L,8L,time.plusDays(3))); record();
        var dispatcher=dispatcher(sender);dispatcher.dispatch(deliveryId());dispatcher.dispatch(deliveryId());
        verify(sender,times(1)).send(any());assertEquals("SENT",jdbc.queryForObject("SELECT status FROM dcc_relation_notification_outbox",String.class));
        assertThrows(IllegalStateException.class,()->tx.executeWithoutResult(s->dispatcher.dispatch(deliveryId())));
    }
    @Test void senderFailureIsPersistedAndRetryUsesSamePlatformBusinessKey() {
        var sender=mock(DccRelationNotificationSender.class);when(sender.send(any())).thenThrow(new IllegalStateException("SECRET_DETAILS")).thenReturn(800L);
        save(new Arrangement(20L,8L,time.plusDays(3)));record();var dispatcher=dispatcher(sender);
        assertThrows(IllegalStateException.class,()->dispatcher.dispatch(deliveryId()));
        assertEquals("FAILED",jdbc.queryForObject("SELECT status FROM dcc_relation_notification_outbox",String.class));
        assertEquals("DELIVERY_FAILED",jdbc.queryForObject("SELECT failure_code FROM dcc_relation_notification_outbox",String.class));
        dispatcher.dispatch(deliveryId());
        var args=org.mockito.ArgumentCaptor.forClass(Notification.class);verify(sender,times(2)).send(args.capture());
        assertEquals(args.getAllValues().get(0).businessKey(),args.getAllValues().get(1).businessKey());
    }
    @Test void unavailableRecipientAndMissingPlatformIdCannotBecomeSent() {
        var sender=mock(DccRelationNotificationSender.class);
        save(new Arrangement(20L,8L,time.plusDays(3)));record();var dispatcher=dispatcher(sender);
        doThrow(new IllegalStateException("RECIPIENT_DISABLED")).when(access).assertAssigneeAvailable(8L,20L);
        assertThrows(IllegalStateException.class,()->dispatcher.dispatch(deliveryId()));verifyNoInteractions(sender);
        reset(access);assertThrows(IllegalStateException.class,()->dispatcher.dispatch(deliveryId()));
        assertEquals("FAILED",jdbc.queryForObject("SELECT status FROM dcc_relation_notification_outbox",String.class));
    }
    @Test void firstLateHistoricalControlDoesNotRevertTheNewerCurrentRelationsOrRestartItsDeadline() {
        save(new Arrangement(20L,8L,time.plusDays(3)));
        var latest=new FileVersion(1L,101L,10L,9L,"N","源.pdf","B/1","ACTIVE",true,true,false);
        when(files.resolveLatestForUpdate(10L)).thenReturn(latest);
        when(files.resolveSelected(101L)).thenReturn(latest);
        jdbc.update("INSERT INTO dcc_controlled_file_related_file VALUES (1,101,40,0)");
        var newer=new ControlledEvent(1L,10L,101L,"round-2","controlled-101-round-2",time.plusHours(1));
        tx.executeWithoutResult(s->service.recordControlled(newer));
        record();record();
        assertEquals(101L,jdbc.queryForObject("SELECT controlled_file_id FROM dcc_current_file_relation_set",Long.class));
        assertEquals(List.of(40L),jdbc.queryForList("SELECT related_master_id FROM dcc_current_file_relation",Long.class));
        assertEquals(time.plusDays(3),jdbc.queryForObject("SELECT due_at FROM dcc_relation_remediation_task",LocalDateTime.class));
        assertEquals(1,count("dcc_relation_notification_outbox"));assertEquals(2,count("dcc_relation_controlled_event"));
    }
    @Test void successfulControlledEventSchedulesItsActualOutboxAndReplayRecoversTheSameEvent(){
        save(new Arrangement(20L,8L,time.plusDays(3)));record();
        Long eventId=jdbc.queryForObject("SELECT id FROM dcc_relation_controlled_event",Long.class);
        verify(scheduler).scheduleAfterCommit(1L,eventId);
        record();verify(scheduler,times(2)).scheduleAfterCommit(1L,eventId);
        assertEquals(1,count("dcc_relation_remediation_task"));assertEquals(1,count("dcc_relation_notification_outbox"));
    }
    @Test void failedControlDoesNotRegisterAnyNotificationCallback(){
        save(new Arrangement(20L,8L,time.plusDays(3)));
        doThrow(new DccRelationFailure("DCC_CONTROL_NOT_SUCCESSFUL")).when(files).assertControlledEvent(event);
        assertThrows(IllegalStateException.class,this::record);verifyNoInteractions(scheduler);
    }
    @Test void arrangementsReadReturnsOnlySavedVersionRoundAndRequiresApprovalContext(){
        save(new Arrangement(20L,8L,time.plusDays(3)));
        jdbc.update("INSERT INTO dcc_relation_arrangement (tenant_id,source_file_id,application_round,related_master_id,assignee_user_id,due_at,arranged_by) VALUES (1,100,'other-round',30,9,?,7),(2,100,'round-1',30,9,?,7)",time.plusDays(4),time.plusDays(5));
        var saved=service.listArrangements(7L,100L,"round-1");assertEquals(List.of(new Arrangement(20L,8L,time.plusDays(3))),saved);
        verify(access).assertCanReadArrangements(7L,100L,"round-1");
        doThrow(new DccRelationFailure("DCC_APPROVAL_CONTEXT_DENIED")).when(access).assertCanReadArrangements(9L,100L,"round-1");
        assertThrows(IllegalStateException.class,()->service.listArrangements(9L,100L,"round-1"));
        TenantContextHolder.setTenantId(2L);assertThrows(IllegalStateException.class,()->service.listArrangements(7L,100L,"round-1"));
    }
    @Test void removedFrozenRelationBeforeControlRejectsAllTasksAndNotifications(){
        save(new Arrangement(20L,8L,time.plusDays(3)));
        jdbc.update("DELETE FROM dcc_controlled_file_related_file WHERE related_master_id=20");
        assertThrows(IllegalStateException.class,this::record);
        assertEquals(0,count("dcc_relation_controlled_event"));assertEquals(0,count("dcc_relation_remediation_task"));assertEquals(0,count("dcc_relation_notification_outbox"));verifyNoInteractions(scheduler);
    }
    @Test void laterArrangementConflictRollsBackAllNewRowsFromTheSameSignedOperation(){
        save(new Arrangement(30L,9L,time.plusDays(4)));
        assertThrows(IllegalStateException.class,()->save(new Arrangement(20L,8L,time.plusDays(3)),new Arrangement(30L,8L,time.plusDays(5))));
        assertEquals(1,count("dcc_relation_arrangement"));assertEquals(9L,jdbc.queryForObject("SELECT assignee_user_id FROM dcc_relation_arrangement",Long.class));
        assertEquals(0,count("dcc_relation_remediation_task"));assertEquals(0,count("dcc_relation_notification_outbox"));verifyNoInteractions(scheduler);
    }
    @Test void savedDeadlineAndAssigneeAreUnchangedAcrossReadAndSameArrangeReplay(){
        var row=new Arrangement(20L,8L,time.plusDays(3));save(row);save(row);
        assertEquals(List.of(row),service.listArrangements(7L,100L,"round-1"));assertEquals(1,count("dcc_relation_arrangement"));
        record();assertEquals(List.of(row),service.listArrangements(7L,100L,"round-1"));
        assertEquals(row.dueAt(),jdbc.queryForObject("SELECT due_at FROM dcc_relation_remediation_task",LocalDateTime.class));
    }
}
