package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.service.file.relations.*;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditServiceImpl;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.GXP_AUDIT_POLICY_NOT_FOUND;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.GXP_AUDIT_APPEND_FAILED;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Actual A lifecycle -> D resolver/store/consumer -> post-commit scheduler/dispatcher on isolated H2. */
@Import(GxpAuditServiceImpl.class)
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(scripts="/sql/dcc_b_gxp_audit_tables.sql",executionPhase=Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts="/sql/dcc_b_gxp_audit_clean.sql",executionPhase=Sql.ExecutionPhase.AFTER_TEST_METHOD)
class DccWorkflowControlledRemediationIntegrationTest extends BaseDbUnitTest {
    @jakarta.annotation.Resource private cn.iocoder.yudao.module.dcc.dal.mysql.file.DccSourceNameReservationMapper g25Reservations;

    @Resource private GxpAuditServiceImpl ledger;
    @Resource private DataSource dataSource;
    @Resource private PlatformTransactionManager transactionManager;
    @Resource private DccControlledFileMapper files;
    @Resource private DccControlledFileMasterMapper masters;
    @Resource private DccControlledFileObsoleteAuditMapper audits;
    @Resource private DccControlledFileNameClaimMapper claims;
    private JdbcTemplate jdbc;
    private DccControlledFileLifecycleService lifecycle;
    private DccRelationRemediationService remediation;
    private DccRelationAccessPolicy access;
    private DccRelationNotificationSender sender;
    private List<Runnable> dispatched;
    private final LocalDateTime deadline=LocalDateTime.of(2026,11,1,12,0);
    private static final List<String> D_TABLES=List.of("dcc_relation_notification_outbox","dcc_relation_remediation_task",
            "dcc_relation_controlled_event","dcc_relation_arrangement","dcc_current_file_relation_set","dcc_current_file_relation");

    @BeforeEach void fixture() throws Exception {
        TenantContextHolder.setTenantId(1L);jdbc=new JdbcTemplate(dataSource);
        assertNotNull(jdbc.queryForObject("SELECT H2VERSION()",String.class));
        for(String sql:Files.readString(Path.of("../sql/mysql/20260930_dcc_d_relations.sql")).replaceAll("(?m)^--.*$","").split(";"))
            if(!sql.isBlank()) jdbc.execute(sql);
        for(String table:D_TABLES)jdbc.update("DELETE FROM "+table);
        jdbc.update("DELETE FROM dcc_controlled_file_related_file");
        for(String operation:List.of("dcc.relation.arrange","dcc.relation.controlled","dcc.controlled-file.control",
                "dcc.controlled-file.activate","dcc.controlled-file.auto-obsolete")) policy(operation);
        var store=new DccRelationStore(jdbc,ledger);
        var resolver=new DccLatestControlledFileResolverImpl();wire(resolver,"fileMapper",files,"masterMapper",masters,"jdbc",jdbc);
        // External authorization and notification port are explicit substitutions; resolver and business writes are real.
        access=mock(DccRelationAccessPolicy.class);sender=mock(DccRelationNotificationSender.class);
        when(sender.send(any())).thenReturn(800L);
        var dispatcher=new DccRelationNotificationDispatcher(store,sender,access,transactionManager);
        dispatched=Collections.synchronizedList(new ArrayList<>());
        var scheduler=new DccRelationNotificationPostCommitScheduler(dispatched::add,store,dispatcher);
        remediation=new DccRelationRemediationService(store,resolver,access,scheduler);
        lifecycle=new DccControlledFileLifecycleService();var dates=new DccWorkflowDatePolicy();dates.setZoneId("Asia/Singapore");
        var identities=new DccControlledFileNameClaimService();org.springframework.test.util.ReflectionTestUtils.setField(identities,"reservationMapper",g25Reservations);wire(identities,"masterMapper",masters,"claimMapper",claims);
        var retention=new DccObsoleteRetentionService();wire(retention,"identities",identities);
        wire(lifecycle,"controlledFileMapper",files,"masterMapper",masters,"obsoleteAuditMapper",audits,"jdbcTemplate",jdbc,
                "datePolicy",dates,"versionPolicy",DccControlledFileVersionPolicy.defaultPolicy(),
                "eventPublisher",(ApplicationEventPublisher)event->new DccRelationControlledEventConsumer(remediation)
                        .onLifecycleEvent((DccControlledFileLifecycleEvent)event),"obsoleteRetentionService",retention);
        var stateAudit=new DccWorkflowFileStateAudit();wire(stateAudit,"jdbcTemplate",jdbc,"auditService",ledger);
        wire(lifecycle,"fileStateAudit",stateAudit);
        jdbc.update("""
                INSERT INTO dcc_controlled_file_master(id,category_id,file_name,file_number,status,tenant_id,
                  current_active_controlled_file_id,latest_controlled_file_id,deleted)
                VALUES(10,10,'SOP.pdf','A-SOP','ACTIVE_CHAIN',1,1,1,0)
                """);
        for(long id:new long[]{1,2}) jdbc.update("""
                INSERT INTO dcc_controlled_file(id,master_id,category_id,directory_id,source_file_id,original_file_id,
                  file_name,title,file_number,version_no,status,submitter_id,requester_id,tenant_id,deleted,
                  published_file_id,stamped_file_id,effective_date,process_instance_id)
                VALUES(?,10,10,20,100,100,'SOP.pdf','SOP','A-SOP',?,?,99,99,1,0,100,100,?,?)
                """,id,id==1?"A/1":"B/1",id==1?"ACTIVE":"READY_TO_PUBLISH",LocalDate.now().plusDays(10),"approval-"+id);
        jdbc.update("UPDATE dcc_controlled_file SET controlled_time=CURRENT_TIMESTAMP,activated_time=CURRENT_TIMESTAMP WHERE id=1");
        jdbc.update("INSERT INTO dcc_controlled_file_name_claim(tenant_id,master_id,normalized_name,source_original_file_name,normalized_file_number,deleted) VALUES(1,10,'SOP.pdf','SOP.pdf','A-SOP',0)");
        for(long master:new long[]{20,30}) jdbc.update("INSERT INTO dcc_controlled_file_related_file(controlled_file_id,related_controlled_file_id,project_code_id,related_master_id,relation_source,tenant_id,deleted) VALUES(2,?,9,?,'SUBMIT',1,0)",master,master);
        tx().executeWithoutResult(s->remediation.saveArrangements(99L,2L,"approval-2",List.of(new Arrangement(20L,200L,deadline)),"选定整改"));
    }
    @AfterEach void cleanup() { for(String table:D_TABLES)jdbc.update("DELETE FROM "+table);
        jdbc.update("DELETE FROM dcc_controlled_file_related_file");SecurityContextHolder.clearContext();TenantContextHolder.clear(); }

    @Test void cc2FutureControlWritesRealUnifiedBeforeAfterOnce() {
        control();control();
        assertEquals(1,ledgerCount("dcc.controlled-file.control"));
        var row=jdbc.queryForMap("SELECT * FROM gxp_audit_event WHERE operation_id='dcc.controlled-file.control'");
        assertEquals("CONTROLLED_FILE:2",row.get("subject_id"));assertEquals("B/1",row.get("subject_version"));
        assertEquals("approval-2",row.get("request_id"));assertEquals("PRESENT",row.get("before_state"));
        assertEquals("PRESENT",row.get("after_state"));
        var before=facts("dcc.controlled-file.control","before_state_json");
        var after=facts("dcc.controlled-file.control","after_state_json");
        assertEquals("READY_TO_PUBLISH",before.get("status"));assertEquals("CONTROLLED_PENDING_EFFECTIVE",after.get("status"));
        assertEquals(1,((Number)before.get("latestControlledFileId")).intValue());
        assertEquals(2,((Number)after.get("latestControlledFileId")).intValue());
        assertEquals(1,((Number)after.get("currentActiveControlledFileId")).intValue());
        assertNull(before.get("controlledTime"));assertNotNull(after.get("controlledTime"));assertNull(after.get("activatedTime"));
        assertEquals(1,dispatched.size());assertEquals(1L,masters.selectById(10L).getCurrentActiveControlledFileId());
    }
    @Test void cc2ControlBeforeStateReadsPersistedArtifactsRatherThanMutatedFinalizationObject() {
        jdbc.update("UPDATE dcc_controlled_file SET published_file_id=NULL,stamped_file_id=NULL WHERE id=2");
        tx().executeWithoutResult(s->{var master=masters.selectByIdForUpdate(10L);var file=files.selectByIdAndTenantForUpdate(1L,2L);
            file.setPublishedFileId(100L);file.setStampedFileId(100L);lifecycle.completeControl(file,master,99L);});
        assertNull(facts("dcc.controlled-file.control","before_state_json").get("publishedFileId"));
        assertNull(facts("dcc.controlled-file.control","before_state_json").get("stampedFileId"));
        assertEquals(100,((Number)facts("dcc.controlled-file.control","after_state_json").get("publishedFileId")).intValue());
    }
    @Test void cc2AutomaticActivationAndObsoleteAreSystemFactsEvenInsideAHumanRequest() {
        control();jdbc.update("UPDATE dcc_controlled_file SET effective_date=? WHERE id=2",LocalDate.now());
        var login=new LoginUser();login.setId(99L);login.setTenantId(1L);login.setUserType(2);
        login.setInfo(Map.of("username","actual-doc-control",LoginUser.INFO_KEY_NICKNAME,"文控"));
        var authentication=new UsernamePasswordAuthenticationToken(login,null,List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        tx().executeWithoutResult(s->assertTrue(lifecycle.activateDue(2L)));
        assertSame(authentication,SecurityContextHolder.getContext().getAuthentication());
        tx().executeWithoutResult(s->assertFalse(lifecycle.activateDue(2L)));
        for(String operation:List.of("dcc.controlled-file.activate","dcc.controlled-file.auto-obsolete")) {
            assertEquals(1,ledgerCount(operation));
            var row=jdbc.queryForMap("SELECT * FROM gxp_audit_event WHERE operation_id=?",operation);
            assertEquals(-1L,((Number)row.get("actor_id")).longValue());assertEquals("SYSTEM_ACTOR",row.get("actor_username"));
            assertNull(row.get("signature_record_id"));assertNotNull(row.get("event_hash"));
        }
        assertEquals("VOIDED",jdbc.queryForObject("SELECT after_state FROM gxp_audit_event WHERE operation_id='dcc.controlled-file.auto-obsolete'",String.class));
        var before=facts("dcc.controlled-file.auto-obsolete","before_state_json");
        var after=facts("dcc.controlled-file.auto-obsolete","after_state_json");
        assertEquals("ACTIVE",before.get("status"));assertEquals("OBSOLETE",after.get("status"));
        assertEquals(2,((Number)after.get("currentActiveControlledFileId")).intValue());assertNotNull(after.get("retainUntil"));
        assertEquals("CONTROLLED_PENDING_EFFECTIVE",facts("dcc.controlled-file.activate","before_state_json").get("status"));
        assertEquals("ACTIVE",facts("dcc.controlled-file.activate","after_state_json").get("status"));
        assertEquals(1,dispatched.size());assertEquals("OBSOLETE",files.selectById(1L).getStatus());
    }
    @Test void cc2MissingControlPolicyRollsBackStatePointersDAndSchedulesNothing() {
        jdbc.update("DELETE FROM gxp_audit_policy_operation WHERE operation_id='dcc.controlled-file.control'");
        assertServiceException(this::control,GXP_AUDIT_POLICY_NOT_FOUND,"dcc.controlled-file.control");assertControlUnchanged();
    }
    @Test void cc2RealControlLedgerInsertFailureRollsBackItsWholeTransaction() {
        failLedger("dcc.controlled-file.control");
        try {assertServiceException(this::control,GXP_AUDIT_APPEND_FAILED,"dcc.controlled-file.control");assertControlUnchanged();}
        finally {jdbc.execute("ALTER TABLE gxp_audit_event DROP CONSTRAINT cc2_fail_ledger");}
    }
    @ParameterizedTest @ValueSource(strings={"dcc.controlled-file.activate","dcc.controlled-file.auto-obsolete"})
    void cc2MissingActivationPolicyRollsBackPointersOldObsoleteRetentionAndLedger(String operation) {
        control();jdbc.update("UPDATE dcc_controlled_file SET effective_date=? WHERE id=2",LocalDate.now());
        jdbc.update("DELETE FROM gxp_audit_policy_operation WHERE operation_id=?",operation);
        assertServiceException(()->tx().executeWithoutResult(s->lifecycle.activateDue(2L)),GXP_AUDIT_POLICY_NOT_FOUND,operation);
        assertActivationUnchanged();
    }
    @ParameterizedTest @ValueSource(strings={"dcc.controlled-file.activate","dcc.controlled-file.auto-obsolete"})
    void cc2RealActivationLedgerInsertFailureRollsBackPointersOldObsoleteRetentionAndLedger(String operation) {
        control();jdbc.update("UPDATE dcc_controlled_file SET effective_date=? WHERE id=2",LocalDate.now());failLedger(operation);
        try {assertServiceException(()->tx().executeWithoutResult(s->lifecycle.activateDue(2L)),GXP_AUDIT_APPEND_FAILED,operation);assertActivationUnchanged();}
        finally {jdbc.execute("ALTER TABLE gxp_audit_event DROP CONSTRAINT cc2_fail_ledger");}
    }
    @Test void cc2ImmediateActivationLedgerFailureAlsoRollsBackControlledDOutboxAndScheduling() {
        jdbc.update("UPDATE dcc_controlled_file SET effective_date=? WHERE id=2",LocalDate.now());failLedger("dcc.controlled-file.activate");
        try {assertServiceException(this::control,GXP_AUDIT_APPEND_FAILED,"dcc.controlled-file.activate");assertControlUnchanged();
            assertEquals("ACTIVE",files.selectById(1L).getStatus());assertEquals(0,count("dcc_controlled_file_obsolete_audit"));
            assertNull(jdbc.queryForObject("SELECT retain_until FROM dcc_controlled_file_name_claim",LocalDateTime.class));}
        finally {jdbc.execute("ALTER TABLE gxp_audit_event DROP CONSTRAINT cc2_fail_ledger");}
    }
    @Test void cc2ConcurrentDueActivationAppendsOnePairOfRealLedgerEvents() throws Exception {
        control();jdbc.update("UPDATE dcc_controlled_file SET effective_date=? WHERE id=2",LocalDate.now());
        var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        Callable<Boolean> action=()->{TenantContextHolder.setTenantId(1L);try{start.await();return tx().execute(s->lifecycle.activateDue(2L));}
            finally{TenantContextHolder.clear();SecurityContextHolder.clearContext();}};
        try {var first=pool.submit(action);var second=pool.submit(action);start.countDown();
            assertNotEquals(first.get(20,TimeUnit.SECONDS),second.get(20,TimeUnit.SECONDS));}
        finally {pool.shutdownNow();assertTrue(pool.awaitTermination(10,TimeUnit.SECONDS));}
        assertEquals(1,ledgerCount("dcc.controlled-file.activate"));assertEquals(1,ledgerCount("dcc.controlled-file.auto-obsolete"));
        assertEquals(2L,masters.selectById(10L).getCurrentActiveControlledFileId());assertEquals(1,dispatched.size());
    }
    @Test void cc2HumanControlAndAutomaticEffectsRetainDistinctAuthenticActors() {
        jdbc.update("UPDATE dcc_controlled_file SET effective_date=? WHERE id=2",LocalDate.now());
        var login=new LoginUser();login.setId(99L);login.setTenantId(1L);login.setUserType(2);
        login.setInfo(Map.of("username","actual-doc-control",LoginUser.INFO_KEY_NICKNAME,"文控"));
        var authentication=new UsernamePasswordAuthenticationToken(login,null,List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);control();
        assertSame(authentication,SecurityContextHolder.getContext().getAuthentication());
        assertEquals(99L,jdbc.queryForObject("SELECT actor_id FROM gxp_audit_event WHERE operation_id='dcc.controlled-file.control'",Long.class));
        for(String operation:List.of("dcc.controlled-file.activate","dcc.controlled-file.auto-obsolete"))
            assertEquals(-1L,jdbc.queryForObject("SELECT actor_id FROM gxp_audit_event WHERE operation_id=?",Long.class,operation));
        assertEquals(-1L,files.selectById(1L).getObsoletedBy());
        assertEquals(-1L,jdbc.queryForObject("SELECT operator_id FROM dcc_controlled_file_obsolete_audit WHERE controlled_file_id=1",Long.class));
    }
    @Test void cc2AutomaticLedgerFailureRestoresTheInvokingHumanAuthenticationAndRollsBack() {
        control();jdbc.update("UPDATE dcc_controlled_file SET effective_date=? WHERE id=2",LocalDate.now());
        var login=new LoginUser();login.setId(99L);login.setTenantId(1L);login.setUserType(2);
        var authentication=new UsernamePasswordAuthenticationToken(login,null,List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);failLedger("dcc.controlled-file.activate");
        try {assertServiceException(()->tx().executeWithoutResult(s->lifecycle.activateDue(2L)),GXP_AUDIT_APPEND_FAILED,"dcc.controlled-file.activate");
            assertSame(authentication,SecurityContextHolder.getContext().getAuthentication());assertActivationUnchanged();}
        finally {jdbc.execute("ALTER TABLE gxp_audit_event DROP CONSTRAINT cc2_fail_ledger");}
    }
    private int ledgerCount(String operation){return jdbc.queryForObject("SELECT COUNT(*) FROM gxp_audit_event WHERE operation_id=?",Integer.class,operation);}
    private Map<?,?> facts(String operation,String column){return JsonUtils.parseObject(jdbc.queryForObject("SELECT "+column+" FROM gxp_audit_event WHERE operation_id=?",String.class,operation),Map.class);}
    private void policy(String operation){jdbc.update("INSERT INTO gxp_audit_policy_operation(tenant_id,policy_version,operation_id,source_type,source_locator,domain,subject_type,action_type,reason_policy,signature_policy,state_policy,retention_class,test_ids,owner,applicability,active) VALUES(1,'CC2-A-ISOLATED',?,'SERVICE_METHOD','A.TEST','DCC','CONTROLLED_FILE','UPDATE','REQUIRED','NOT_REQUIRED','PRESENT_TO_PRESENT','GXP_CONTROLLED_DOCUMENT','CC2-A-LEDGER','dcc-a','GXP',TRUE)",operation);}
    private void failLedger(String operation){jdbc.execute("ALTER TABLE gxp_audit_event ADD CONSTRAINT cc2_fail_ledger CHECK (operation_id <> '"+operation+"')");}
    private void assertControlUnchanged(){assertNull(files.selectById(2L).getControlledTime());assertEquals("READY_TO_PUBLISH",files.selectById(2L).getStatus());
        assertEquals(1L,masters.selectById(10L).getLatestControlledFileId());assertEquals(1L,masters.selectById(10L).getCurrentActiveControlledFileId());
        for(String table:List.of("dcc_workflow_lifecycle_event","dcc_relation_controlled_event","dcc_relation_remediation_task","dcc_relation_notification_outbox","dcc_current_file_relation")) assertEquals(0,count(table));
        assertEquals(0,ledgerCount("dcc.controlled-file.control"));assertEquals(0,dispatched.size());verifyNoInteractions(sender);}
    private void assertActivationUnchanged(){assertEquals("ACTIVE",files.selectById(1L).getStatus());assertEquals("CONTROLLED_PENDING_EFFECTIVE",files.selectById(2L).getStatus());
        assertNull(files.selectById(2L).getActivatedTime());assertEquals(1L,masters.selectById(10L).getCurrentActiveControlledFileId());assertEquals(2L,masters.selectById(10L).getLatestControlledFileId());
        assertNull(jdbc.queryForObject("SELECT retain_until FROM dcc_controlled_file_name_claim",LocalDateTime.class));assertEquals(0,count("dcc_controlled_file_obsolete_audit"));
        assertEquals(1,count("dcc_workflow_lifecycle_event"));assertEquals(1,count("dcc_relation_notification_outbox"));assertEquals(1,dispatched.size());
        assertEquals(0,ledgerCount("dcc.controlled-file.activate"));assertEquals(0,ledgerCount("dcc.controlled-file.auto-obsolete"));}

    @Test void futureControlPersistsOnlySelectedRemediationAndSchedulesAfterCommit() {
        assertEquals(0,count("dcc_relation_notification_outbox"));
        tx().executeWithoutResult(s->{controlInside();assertEquals(1,count("dcc_relation_notification_outbox"));
            assertEquals(0,dispatched.size());verifyNoInteractions(sender);});
        assertEquals(1,ledgerCount("dcc.controlled-file.control"));
        assertEquals(1,dispatched.size());assertEquals("ACTIVE",files.selectById(1L).getStatus());
        assertEquals("CONTROLLED_PENDING_EFFECTIVE",files.selectById(2L).getStatus());
        assertEquals(1,count("dcc_relation_remediation_task"));assertEquals(2,count("dcc_current_file_relation"));
        assertEquals(200L,jdbc.queryForObject("SELECT recipient_user_id FROM dcc_relation_notification_outbox",Long.class));
        assertEquals(deadline,jdbc.queryForObject("SELECT due_at FROM dcc_relation_remediation_task",LocalDateTime.class));
        dispatched.forEach(Runnable::run);
        verify(sender,times(1)).send(argThat(message->message.sourceControlledFileId().equals(2L)
                && message.relatedMasterId().equals(20L) && message.recipientUserId().equals(200L)));
        assertEquals("SENT",jdbc.queryForObject("SELECT status FROM dcc_relation_notification_outbox",String.class));
    }
    @Test void failedOuterControlRollsBackDTasksCurrentRelationAndScheduledNotification() {
        assertThrows(IllegalStateException.class,()->tx().executeWithoutResult(s->{controlInside();throw new IllegalStateException("control failed");}));
        assertNull(files.selectById(2L).getControlledTime());assertEquals(1L,masters.selectById(10L).getLatestControlledFileId());
        for(String table:List.of("dcc_relation_controlled_event","dcc_relation_remediation_task","dcc_relation_notification_outbox","dcc_current_file_relation"))
            assertEquals(0,count(table));
        assertEquals(0,dispatched.size());verifyNoInteractions(sender);
        assertEquals(0,ledgerCount("dcc.controlled-file.control"));
    }
    @Test void unavailableSelectedAssigneeFailsTheActualControlTransaction() {
        doThrow(new DccRelationFailure("ASSIGNEE_DISABLED")).when(access).assertAssigneeAvailable(200L,20L);
        assertThrows(DccRelationFailure.class,this::control);
        assertNull(files.selectById(2L).getControlledTime());assertEquals(1L,masters.selectById(10L).getLatestControlledFileId());
        assertEquals(0,count("dcc_workflow_lifecycle_event"));assertEquals(0,count("dcc_relation_notification_outbox"));
        assertEquals(0,dispatched.size());
    }
    @Test void controlReplayAndLaterActivationNeverRepeatTheControlledRemediation() {
        control();control();
        assertEquals(1,count("dcc_relation_controlled_event"));assertEquals(1,count("dcc_relation_notification_outbox"));
        jdbc.update("UPDATE dcc_controlled_file SET effective_date=? WHERE id=2",LocalDate.now());
        tx().executeWithoutResult(s->assertTrue(lifecycle.activateDue(2L)));
        assertEquals(1,count("dcc_relation_controlled_event"));assertEquals(1,dispatched.size());
        assertEquals(1,count("dcc_relation_notification_outbox"));assertEquals("OBSOLETE",files.selectById(1L).getStatus());
    }
    @Test void concurrentControlsMaterializeOneConsumerEventAndOneAfterCommitDispatch() throws Exception {
        var pool=Executors.newFixedThreadPool(2);
        Callable<Void> action=()->{TenantContextHolder.setTenantId(1L);try{control();return null;}finally{TenantContextHolder.clear();}};
        try {var first=pool.submit(action);var second=pool.submit(action);first.get(15,TimeUnit.SECONDS);second.get(15,TimeUnit.SECONDS);}
        finally {pool.shutdownNow();assertTrue(pool.awaitTermination(10,TimeUnit.SECONDS));}
        assertEquals(1,count("dcc_relation_controlled_event"));assertEquals(1,count("dcc_relation_notification_outbox"));
        assertEquals(1,dispatched.size());
        assertEquals(1,ledgerCount("dcc.controlled-file.control"));
    }
    private void control(){tx().executeWithoutResult(s->controlInside());}
    private void controlInside(){var master=masters.selectByIdForUpdate(10L);
        lifecycle.completeControl(files.selectByIdAndTenantForUpdate(1L,2L),master,99L);}
    private TransactionTemplate tx(){return new TransactionTemplate(transactionManager);}
    private int count(String table){return jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class);}
    private void wire(Object target,Object... pairs){for(int i=0;i<pairs.length;i+=2)ReflectionTestUtils.setField(target,(String)pairs[i],pairs[i+1]);}
}
