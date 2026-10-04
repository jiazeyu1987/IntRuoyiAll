package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileObsoleteReqVO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import jakarta.annotation.Resource;
import org.flowable.engine.history.HistoricProcessInstance;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditServiceImpl;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.*;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.GXP_AUDIT_POLICY_NOT_FOUND;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.GXP_AUDIT_APPEND_FAILED;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real H2 transactions and locks; external BPM/signature/notification facts have separate tests. */
@Import(GxpAuditServiceImpl.class)
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(scripts="/sql/dcc_b_gxp_audit_tables.sql",executionPhase=Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts="/sql/dcc_b_gxp_audit_clean.sql",executionPhase=Sql.ExecutionPhase.AFTER_TEST_METHOD)
class DccWorkflowObsoleteTransactionTest extends BaseDbUnitTest {
    @jakarta.annotation.Resource private cn.iocoder.yudao.module.dcc.dal.mysql.file.DccSourceNameReservationMapper g25Reservations;

    @Resource private GxpAuditServiceImpl ledger;
    @Resource private DataSource dataSource;
    @Resource private PlatformTransactionManager transactionManager;
    @Resource(name="dccControlledFileMapper") private DccControlledFileMapper files;
    @Resource private DccControlledFileMasterMapper masters;
    @Resource private DccControlledFileObsoleteAuditMapper audits;
    @Resource(name="dccControlledFileMessageJobMapper") private DccControlledFileMessageJobMapper jobs;
    @Resource private DccControlledFileDistributionMapper distributions;
    @Resource private DccControlledFileTrainingMapper training;
    @Resource private DccControlledFileNameClaimMapper claims;
    private JdbcTemplate jdbc;
    private DccControlledFileObsoleteServiceImpl service;
    private DccWorkflowObsoleteEvidenceGuard evidence;
    private DccControlledContentAdapter platform;

    @BeforeEach void fixture() {
        TenantContextHolder.setTenantId(1L);
        jdbc=new JdbcTemplate(dataSource);
        assertNotNull(jdbc.queryForObject("SELECT H2VERSION()",String.class));
        for(String operation:java.util.List.of("dcc.controlled-file.obsolete","dcc.controlled-file.activate"))
            jdbc.update("INSERT INTO gxp_audit_policy_operation(tenant_id,policy_version,operation_id,source_type,source_locator,domain,subject_type,action_type,reason_policy,signature_policy,state_policy,retention_class,test_ids,owner,applicability,active) VALUES(1,'CC2-A-ISOLATED',?,'SERVICE_METHOD','A.TEST','DCC','CONTROLLED_FILE','UPDATE','REQUIRED','NOT_REQUIRED','PRESENT_TO_PRESENT','GXP_CONTROLLED_DOCUMENT','CC2-A-LEDGER','dcc-a','GXP',TRUE)",operation);
        service=new DccControlledFileObsoleteServiceImpl();
        evidence=mock(DccWorkflowObsoleteEvidenceGuard.class);
        when(evidence.require(any(),eq("obsolete-round"),any())).thenReturn(100L);
        var bpm=mock(BpmProcessInstanceService.class);
        var process=mock(HistoricProcessInstance.class);
        when(process.getId()).thenReturn("obsolete-round");
        when(process.getTenantId()).thenReturn("1");
        when(process.getStartUserId()).thenReturn("99");
        when(process.getProcessDefinitionKey()).thenReturn(DccControlledFileProcessDefinitionKeys.OBSOLETE);
        when(process.getProcessVariables()).thenReturn(Map.of("PROCESS_STATUS",2,"systemCode","DCC",
                "objectType","CONTROLLED_FILE","objectId","42","objectVersion","A/1","actionCode","OBSOLETE",
                "PROCESS_LAST_APPROVER_USER_ID",100L));
        when(bpm.getHistoricProcessInstance("obsolete-round")).thenReturn(process);
        var permissions=mock(DccControlledFileCategoryPermissionSupport.class);
        when(permissions.hasCategoryPermission(any(),eq(99L),any())).thenReturn(true);
        platform=mock(DccControlledContentAdapter.class);
        wire(service,"controlledFileMapper",files,"controlledFileMasterMapper",masters,"obsoleteAuditMapper",audits,
                "obsoleteArchiveRequestService",new DccWorkflowObsoleteArchiveRequestService(jdbc),
                "obsoleteProcessService",bpm,"obsoleteEvidenceGuard",evidence,"permissionSupport",permissions,
                "distributionMapper",distributions,"trainingMapper",training,"messageJobMapper",jobs,
                "messageDeliveryService",mock(DccControlledFileMessageDeliveryService.class),"platformAdapter",platform);
        var identities=new DccControlledFileNameClaimService();org.springframework.test.util.ReflectionTestUtils.setField(identities,"reservationMapper",g25Reservations);wire(identities,"masterMapper",masters,"claimMapper",claims);
        var retention=new DccObsoleteRetentionService();wire(retention,"identities",identities);
        wire(service,"obsoleteRetentionService",retention);
        wire(service,"fileStateAudit",stateAudit());
        // Notifications are deliberately not delivered. Only real persisted PENDING rows are verified.
        jdbc.update("""
                INSERT INTO dcc_controlled_file_master(id,category_id,file_name,file_number,status,tenant_id,
                  current_active_controlled_file_id,latest_controlled_file_id,deleted)
                VALUES(10,10,'SOP.pdf','A-SOP','ACTIVE_CHAIN',1,42,42,0)
                """);
        jdbc.update("""
                INSERT INTO dcc_controlled_file(id,master_id,category_id,directory_id,source_file_id,original_file_id,
                  file_name,title,file_number,version_no,status,submitter_id,requester_id,tenant_id,deleted,
                  published_file_id,stamped_file_id,effective_date,process_instance_id,controlled_time,activated_time)
                VALUES(42,10,10,20,100,100,'SOP.pdf','SOP','A-SOP','A/1','ACTIVE',99,99,1,0,
                  100,100,?,'upload-round',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                """,LocalDate.now().minusDays(1));
        jdbc.update("INSERT INTO dcc_controlled_file_name_claim(tenant_id,master_id,normalized_name,source_original_file_name,normalized_file_number,deleted) VALUES(1,10,'SOP.pdf','SOP.pdf','A-SOP',0)");
    }

    @AfterEach void clear() { TenantContextHolder.clear(); }

    @Test void cc2ApprovedObsoleteWritesTheTrueRoundReasonAndBeforeAfterOnce() {
        apply();apply();assertEquals(1,count("gxp_audit_event"));
        var row=jdbc.queryForMap("SELECT * FROM gxp_audit_event");assertEquals("dcc.controlled-file.obsolete",row.get("operation_id"));
        assertEquals("CONTROLLED_FILE:42",row.get("subject_id"));assertEquals("A/1",row.get("subject_version"));
        assertEquals("obsolete-round",row.get("request_id"));assertEquals("批准作废",row.get("reason"));
        assertEquals("PRESENT",row.get("before_state"));assertEquals("VOIDED",row.get("after_state"));
        String before=jdbc.queryForObject("SELECT before_state_json FROM gxp_audit_event",String.class);
        String after=jdbc.queryForObject("SELECT after_state_json FROM gxp_audit_event",String.class);
        assertTrue(before.contains("ACTIVE"));assertTrue(after.contains("OBSOLETE"));assertTrue(after.contains("100"));
        assertTrue(after.contains("retainUntil"));assertNotNull(row.get("event_hash"));
        var beforeFacts=JsonUtils.parseObject(before,Map.class);var afterFacts=JsonUtils.parseObject(after,Map.class);
        assertEquals(42,((Number)beforeFacts.get("currentActiveControlledFileId")).intValue());
        assertNull(afterFacts.get("currentActiveControlledFileId"));assertEquals(42,((Number)afterFacts.get("latestControlledFileId")).intValue());
        assertEquals(100,((Number)afterFacts.get("obsoletedBy")).intValue());
        assertEquals(files.selectById(42L).getObsoletedTime().plusYears(20).toString(),afterFacts.get("retainUntil"));
    }
    @Test void cc2MissingObsoletePolicyRollsBackAllBusinessFacts() {
        jdbc.update("DELETE FROM gxp_audit_policy_operation WHERE operation_id='dcc.controlled-file.obsolete'");
        assertServiceException(this::apply,GXP_AUDIT_POLICY_NOT_FOUND,"dcc.controlled-file.obsolete");assertUnchanged();assertEquals(0,count("gxp_audit_event"));
    }
    @Test void cc2RealObsoleteLedgerInsertFailureRollsBackAllBusinessFacts() {
        jdbc.execute("ALTER TABLE gxp_audit_event ADD CONSTRAINT cc2_obsolete_ledger_failure CHECK (operation_id <> 'dcc.controlled-file.obsolete')");
        try{assertServiceException(this::apply,GXP_AUDIT_APPEND_FAILED,"dcc.controlled-file.obsolete");assertUnchanged();assertEquals(0,count("gxp_audit_event"));}
        finally{jdbc.execute("ALTER TABLE gxp_audit_event DROP CONSTRAINT cc2_obsolete_ledger_failure");}
    }
    @Test void cc2OuterLateFailureCannotLeaveAUnifiedObsoleteLedgerEvent() {
        assertThrows(IllegalStateException.class,()->new TransactionTemplate(transactionManager).executeWithoutResult(s->{apply();throw new IllegalStateException("late outer failure");}));
        assertUnchanged();assertEquals(0,count("gxp_audit_event"));
    }

    @Test void obsoleteReplayWritesOneFactAndKeepsLatestIdentityOccupied() {
        apply();apply();
        assertEquals("OBSOLETE",files.selectById(42L).getStatus());
        assertEquals(100L,files.selectById(42L).getObsoletedBy());
        assertEquals(files.selectById(42L).getObsoletedTime().plusYears(20),
                jdbc.queryForObject("SELECT retain_until FROM dcc_controlled_file_name_claim",java.time.LocalDateTime.class));
        assertNull(masters.selectById(10L).getCurrentActiveControlledFileId());
        assertEquals(42L,masters.selectById(10L).getLatestControlledFileId());
        assertEquals(1,count("dcc_controlled_file_obsolete_audit"));
        assertEquals(100L,jdbc.queryForObject("SELECT operator_id FROM dcc_controlled_file_obsolete_audit",Long.class));
        assertEquals(1,count("dcc_workflow_obsolete_archive"));
        assertEquals(1,count("dcc_controlled_file_message_job"));
        assertEquals("PENDING",jdbc.queryForObject("SELECT status FROM dcc_controlled_file_message_job",String.class));
        verify(platform,times(1)).recordObsoleted(any(),eq(100L),eq("批准作废"),eq("dcc-obsolete:42"));
    }

    @Test void concurrentApprovedEffectsCommitOnlyOnce() throws Exception {
        var executor=Executors.newFixedThreadPool(2);
        var start=new CountDownLatch(1);
        Callable<Void> action=()-> {
            TenantContextHolder.setTenantId(1L);
            try { start.await();apply();return null; } finally { TenantContextHolder.clear(); }
        };
        try {
            var first=executor.submit(action);var second=executor.submit(action);start.countDown();
            first.get(15,TimeUnit.SECONDS);second.get(15,TimeUnit.SECONDS);
            assertEquals(1,count("dcc_controlled_file_obsolete_audit"));
            assertEquals(1,count("dcc_workflow_obsolete_archive"));
            assertEquals(1,count("dcc_controlled_file_message_job"));
            assertEquals(1,count("gxp_audit_event"));
            verify(platform,times(1)).recordObsoleted(any(),eq(100L),any(),any());
        } finally {
            executor.shutdownNow();assertTrue(executor.awaitTermination(10,TimeUnit.SECONDS));
        }
    }

    @Test void latePlatformFailureRollsBackObsoleteAuditPointersArchiveAndNotification() {
        doThrow(new IllegalStateException("test-owned platform persistence failure"))
                .when(platform).recordObsoleted(any(),any(),any(),any());
        assertThrows(IllegalStateException.class,this::apply);
        assertUnchanged();
    }

    @Test void archivePersistenceFailureCannotLeaveAnObsoleteFact() {
        var failingArchive=mock(DccWorkflowObsoleteArchiveRequestService.class);
        doThrow(new IllegalStateException("test-owned archive persistence failure")).when(failingArchive).request(any(),any());
        ReflectionTestUtils.setField(service,"obsoleteArchiveRequestService",failingArchive);
        assertThrows(IllegalStateException.class,this::apply);
        assertUnchanged();verifyNoInteractions(platform);
    }

    @Test void signatureFailureLeavesAllPersistentFactsUnchanged() {
        when(evidence.require(any(),eq("obsolete-round"),any())).thenThrow(new IllegalStateException("invalid evidence"));
        assertThrows(IllegalStateException.class,this::apply);
        assertUnchanged();verifyNoInteractions(platform);
    }
    @Test void anAlreadySelectedDueVersionCanceledByApprovedObsoleteCannotBeReactivatedOrReportedAsABrokenJob() {
        pending();var lifecycle=lifecycle();
        var selected=lifecycle.dueVersions();assertEquals(1,selected.size());assertEquals(42L,selected.get(0).getId());
        apply();var obsoleteAt=files.selectById(42L).getObsoletedTime();
        assertEquals(Boolean.FALSE,new TransactionTemplate(transactionManager).execute(s->lifecycle.activateDue(selected.get(0).getId())));
        assertEquals("OBSOLETE",files.selectById(42L).getStatus());assertNull(files.selectById(42L).getActivatedTime());
        assertNull(masters.selectById(10L).getCurrentActiveControlledFileId());
        assertEquals(obsoleteAt.plusYears(20),jdbc.queryForObject("SELECT retain_until FROM dcc_controlled_file_name_claim",java.time.LocalDateTime.class));
        assertEquals(0,count("dcc_workflow_lifecycle_event"));assertEquals(1,count("dcc_controlled_file_obsolete_audit"));
    }
    @Test void aMalformedObsoleteStatusWithoutRealTimestampsIsNotSilentlySkipped() {
        pending();jdbc.update("UPDATE dcc_controlled_file SET status='OBSOLETE',obsoleted_time=NULL WHERE id=42");
        assertThrows(IllegalStateException.class,()->new TransactionTemplate(transactionManager).execute(s->lifecycle().activateDue(42L)));
        assertEquals(0,count("dcc_workflow_lifecycle_event"));
    }
    private void pending() {
        jdbc.update("UPDATE dcc_controlled_file SET activated_time=NULL,status='CONTROLLED_PENDING_EFFECTIVE',effective_date=? WHERE id=42",LocalDate.now());
        jdbc.update("UPDATE dcc_controlled_file_master SET current_active_controlled_file_id=NULL WHERE id=10");
    }
    @Test void activationThenIndependentObsoleteRetainsTheAuthenticActivationHistoryButClearsExecution() {
        pending();var lifecycle=lifecycle();
        assertEquals(Boolean.TRUE,new TransactionTemplate(transactionManager).execute(s->lifecycle.activateDue(42L)));
        assertEquals("ACTIVE",files.selectById(42L).getStatus());assertEquals(42L,masters.selectById(10L).getCurrentActiveControlledFileId());
        apply();assertEquals("OBSOLETE",files.selectById(42L).getStatus());assertNotNull(files.selectById(42L).getActivatedTime());
        assertNull(masters.selectById(10L).getCurrentActiveControlledFileId());
        assertEquals(1,count("dcc_workflow_lifecycle_event"));assertEquals(1,count("dcc_controlled_file_obsolete_audit"));
        assertEquals(Boolean.FALSE,new TransactionTemplate(transactionManager).execute(s->lifecycle.activateDue(42L)));
    }
    @Test void independentlyObsoleteAndDueActivationCompeteWithoutResurrectionOrDanglingExecution() throws Exception {
        pending();var lifecycle=lifecycle();var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        Callable<Boolean> activate=()->{TenantContextHolder.setTenantId(1L);try{start.await();return new TransactionTemplate(transactionManager).execute(s->lifecycle.activateDue(42L));}finally{TenantContextHolder.clear();}};
        Callable<Void> obsolete=()->{TenantContextHolder.setTenantId(1L);try{start.await();apply();return null;}finally{TenantContextHolder.clear();}};
        try{var active=pool.submit(activate);var canceled=pool.submit(obsolete);start.countDown();boolean activated=active.get(20,TimeUnit.SECONDS);canceled.get(20,TimeUnit.SECONDS);
            assertEquals(activated?1:0,count("dcc_workflow_lifecycle_event"));
            assertEquals(activated,files.selectById(42L).getActivatedTime()!=null);}
        finally{pool.shutdownNow();assertTrue(pool.awaitTermination(10,TimeUnit.SECONDS));}
        assertEquals("OBSOLETE",files.selectById(42L).getStatus());assertNull(masters.selectById(10L).getCurrentActiveControlledFileId());
        assertEquals(1,count("dcc_controlled_file_obsolete_audit"));assertEquals(1,count("dcc_workflow_obsolete_archive"));
        assertEquals(files.selectById(42L).getObsoletedTime().plusYears(20),jdbc.queryForObject("SELECT retain_until FROM dcc_controlled_file_name_claim",java.time.LocalDateTime.class));
    }
    private DccControlledFileLifecycleService lifecycle() {
        var result=new DccControlledFileLifecycleService();var dates=new DccWorkflowDatePolicy();dates.setZoneId("Asia/Singapore");
        wire(result,"controlledFileMapper",files,"masterMapper",masters,"obsoleteAuditMapper",audits,"datePolicy",dates,
                "versionPolicy",DccControlledFileVersionPolicy.defaultPolicy(),"jdbcTemplate",jdbc,
                "eventPublisher",(org.springframework.context.ApplicationEventPublisher)event->{});
        wire(result,"fileStateAudit",stateAudit());
        return result;
    }

    private DccWorkflowFileStateAudit stateAudit(){var audit=new DccWorkflowFileStateAudit();
        wire(audit,"jdbcTemplate",jdbc,"auditService",ledger);return audit;}

    private void apply() {
        var request=new DccControlledFileObsoleteReqVO();request.setReason("批准作废");
        request.setApprovalProcessInstanceId("obsolete-round");request.setApprovedVersionNo("A/1");
        new TransactionTemplate(transactionManager).executeWithoutResult(ignored->
                service.applyApprovedObsoleteControlledFile(99L,42L,request));
    }
    private void assertUnchanged() {
        assertEquals(0,count("gxp_audit_event"));
        assertEquals("ACTIVE",files.selectById(42L).getStatus());assertNull(files.selectById(42L).getObsoletedTime());
        assertEquals(42L,masters.selectById(10L).getCurrentActiveControlledFileId());
        assertEquals(42L,masters.selectById(10L).getLatestControlledFileId());
        for(String table:new String[]{"dcc_controlled_file_obsolete_audit","dcc_workflow_obsolete_archive","dcc_controlled_file_message_job"})
            assertEquals(0,count(table));
        assertNull(jdbc.queryForObject("SELECT retain_until FROM dcc_controlled_file_name_claim",java.time.LocalDateTime.class));
    }
    private int count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class); }
    private void wire(Object target,Object... fields) {
        for(int i=0;i<fields.length;i+=2)ReflectionTestUtils.setField(target,(String)fields[i],fields[i+1]);
    }
}
