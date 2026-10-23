package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditServiceImpl;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.junit.jupiter.api.*;
import javax.sql.DataSource;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Actual Lifecycle -> persisted root -> batch -> GxP -> commit -> independent FAILED preparation. */
@Import(GxpAuditServiceImpl.class)
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(scripts="/sql/dcc_b_gxp_audit_tables.sql",executionPhase=Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts="/sql/dcc_b_gxp_audit_clean.sql",executionPhase=Sql.ExecutionPhase.AFTER_TEST_METHOD)
class DccLifecycleDistributionOutboxTransactionTest extends BaseDbUnitTest {
    @Test void connectorNativeDateTimeColumnMapDoesNotBreakActualActivation(){
        var nativeDates=new JdbcTemplate(dataSource){
            @Override public List<Map<String,Object>> queryForList(String sql,Object...args){
                var result=super.queryForList(sql,args);for(var row:result)for(var entry:new ArrayList<>(row.entrySet()))
                    if(entry.getValue() instanceof java.sql.Timestamp time)row.put(entry.getKey(),time.toLocalDateTime());
                return result;
            }
        };
        wire(batches,"jdbcTemplate",nativeDates);
        assertDoesNotThrow(this::control);
        assertEquals("ACTIVE",files.selectById(20L).getStatus());
        assertEquals(20L,masters.selectById(3L).getCurrentActiveControlledFileId());
        assertEquals(1,count("gxp_audit_event","operation_id='dcc.distribution.batch.accept'"));
    }
    @Resource protected GxpAuditServiceImpl ledger;
    @Resource protected DataSource dataSource;
    @Resource protected PlatformTransactionManager transactionManager;
    @Resource(name="dccControlledFileMapper") protected DccControlledFileMapper files;
    @Resource protected DccControlledFileMasterMapper masters;
    @Resource protected DccControlledFileObsoleteAuditMapper audits;
    @Resource protected DccDistributionBatchMapper batchRows;
    @Resource protected DccControlledFileDistributionMapper scopes;
    @Resource protected DccControlledFileDistributionRecipientMapper recipients;
    @Resource(name="dccControlledFileMessageJobMapper") protected DccControlledFileMessageJobMapper jobs;
    @MockitoBean private AdminUserApi accounts;
    protected JdbcTemplate jdbc;protected TransactionTemplate tx;
    protected DccControlledFileLifecycleService lifecycle;
    protected DccDistributionBatchService batches;
    protected final LocalDateTime now=LocalDateTime.of(2026,10,11,12,0);
    @BeforeEach void setup(){
        TenantContextHolder.setTenantId(1L);SecurityContextHolder.clearContext();jdbc=new JdbcTemplate(dataSource);
        assertNotNull(jdbc.queryForObject("SELECT H2VERSION()",String.class));tx=new TransactionTemplate(transactionManager);
        for(String table:List.of("dcc_distribution_dispatch_attempt","dcc_distribution_batch"))jdbc.update("DELETE FROM "+table);
        for(String operation:List.of("dcc.controlled-file.control","dcc.controlled-file.activate","dcc.controlled-file.auto-obsolete",
                "dcc.distribution.batch.accept","dcc.distribution.batch.fail"))policy(operation);
        masters.insert(DccControlledFileMasterDO.builder().id(3L).tenantId(1L).categoryId(2L).fileName("SOP.pdf").fileNumber("SOP-01")
                .status("ACTIVE_CHAIN").currentActiveControlledFileId(10L).latestControlledFileId(10L).build());
        files.insert(DccControlledFileDO.builder().id(10L).tenantId(1L).masterId(3L).categoryId(2L).directoryId(4L).sourceFileId(110L).originalFileId(110L)
                .fileName("SOP.pdf").title("SOP").fileNumber("SOP-01").versionNo("A/1").status("ACTIVE").requesterId(99L).submitterId(99L)
                .processInstanceId("old-round").controlledTime(now.minusDays(20)).activatedTime(now.minusDays(20)).build());
        files.insert(DccControlledFileDO.builder().id(20L).tenantId(1L).masterId(3L).categoryId(2L).directoryId(4L).sourceFileId(111L).originalFileId(111L)
                .fileName("SOP.pdf").title("SOP").fileNumber("SOP-01").versionNo("B/1").status("READY_TO_PUBLISH").requesterId(99L).submitterId(99L)
                .processInstanceId("round-1").processDefinitionKey(DccControlledFileProcessDefinitionKeys.UPLOAD)
                .effectiveMode(DccWorkflowDatePolicy.PRESET_DATE).effectiveDate(now.toLocalDate()).publishedFileId(211L).stampedFileId(211L)
                .distributionDepartmentIdsJson("[\"31\"]").build());
        batches=spy(new DccDistributionBatchService());wire(batches,"controlledFileMapper",files,"batchMapper",batchRows,"distributionMapper",scopes,
                "recipientMapper",recipients,"messageJobMapper",jobs,"jdbcTemplate",jdbc,"gxpAuditService",ledger,
                "transactionManager",transactionManager,"distributionService",mock(DccWorkflowDistributionService.class));
        // Exact NotReady is the declared server policy port, not an inferred production roster.
        var sourcePort=mock(org.springframework.beans.factory.ObjectProvider.class);
        when(sourcePort.orderedStream()).thenAnswer(ignored->java.util.stream.Stream.of((DccDistributionRosterSource)request->
                new DccDistributionRosterSource.NotReady("ROSTER_POLICY_UNDECIDED","任务隔离旧政策未决：ROSTER_POLICY_UNDECIDED")));
        wire(batches,"rosterSources",sourcePort);
        var stateAudit=new DccWorkflowFileStateAudit();wire(stateAudit,"jdbcTemplate",jdbc,"auditService",ledger);
        var dates=new DccWorkflowDatePolicy(){@Override public LocalDateTime now(){return now;}};
        lifecycle=new DccControlledFileLifecycleService();wire(lifecycle,"controlledFileMapper",files,"masterMapper",masters,"datePolicy",dates,
                "jdbcTemplate",jdbc,"fileStateAudit",stateAudit,"platformAdapter",mock(DccControlledContentAdapter.class),"obsoleteAuditMapper",audits,
                "obsoleteRetentionService",mock(DccObsoleteRetentionService.class),"versionPolicy",DccControlledFileVersionPolicy.defaultPolicy(),
                "eventPublisher",(org.springframework.context.ApplicationEventPublisher)event->{});
        wire(lifecycle,"distributionBatchService",batches);
        var contractPort=mock(DccLifecycleEffectiveContractResolver.class);
        when(contractPort.resolve(any())).thenAnswer(call->{var current=call.getArgument(0,DccControlledFileDO.class);
            return new DccLifecycleEffectiveContract(current.getTenantId(),current.getId(),current.getProcessInstanceId(),"exact-public-id",
                    "exact-deployment",current.getProcessDefinitionKey(),"a".repeat(64),DccLifecycleEffectiveContract.Rule.NEW_EXPLICIT_MODE);});
        wire(lifecycle,"effectiveContractResolver",contractPort);
    }
    @AfterEach void cleanup(){if(jdbc!=null)for(String table:List.of("dcc_distribution_dispatch_attempt","dcc_distribution_batch"))jdbc.update("DELETE FROM "+table);
        TenantContextHolder.clear();SecurityContextHolder.clearContext();}
    @Test void activatedEventReceivesPersistentBatchBeforeCommitThenWritesUndecidedFailureAfterCommit(){
        control();var batch=batchRows.selectList().get(0);
        assertEquals("FAILED",batch.getStatus());assertEquals("ROSTER_POLICY_UNDECIDED",batch.getErrorCode());
        assertEquals("[\"31\"]",batch.getDepartmentIdsJson());assertTrue(batch.getErrorMessage().contains("ROSTER_POLICY_UNDECIDED"));
        assertEquals("ACTIVE",files.selectById(20L).getStatus());assertEquals(now,files.selectById(20L).getEffectiveTime());
        assertEquals(now,files.selectById(20L).getActivatedTime());assertEquals(20L,masters.selectById(3L).getCurrentActiveControlledFileId());
        verify(batches).processAutomaticBatch(batch.getId());
        assertEquals("PENDING",jdbc.queryForObject("SELECT delivery_status FROM dcc_workflow_lifecycle_event WHERE event_type='ACTIVATED'",String.class));
        assertEquals(0,scopes.selectList().size());assertEquals(0,recipients.selectList().size());assertEquals(0,jobs.selectList().size());
        assertEquals(1,count("gxp_audit_event","operation_id='dcc.distribution.batch.accept'"));
        assertEquals(1,count("gxp_audit_event","operation_id='dcc.distribution.batch.fail'"));
        control();assertEquals(1,batchRows.selectList().size());
    }
    @Test void batchAcceptAuditFailureRollsBackActualActivationRootAndOldVersion(){
        jdbc.update("DELETE FROM gxp_audit_policy_operation WHERE operation_id='dcc.distribution.batch.accept'");
        var error=assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,this::control);
        assertEquals(cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.GXP_AUDIT_POLICY_NOT_FOUND.getCode(),error.getCode());
        assertRollback();verify(batches,never()).processAutomaticBatch(anyLong());
    }
    @Test void batchAcceptStorageFailureRollsBackActivationAndNoAfterCommitRuns(){
        jdbc.execute("ALTER TABLE dcc_distribution_batch ADD CONSTRAINT w06_accept_fail CHECK(controlled_file_id<>20)");
        try{assertThrows(org.springframework.dao.DataIntegrityViolationException.class,this::control);assertRollback();}
        finally{jdbc.execute("ALTER TABLE dcc_distribution_batch DROP CONSTRAINT w06_accept_fail");}
        verify(batches,never()).processAutomaticBatch(anyLong());
    }
    @Test void afterCommitPreparationFailureCannotReturnCommittedActivationAsFailure(){
        jdbc.update("DELETE FROM gxp_audit_policy_operation WHERE operation_id='dcc.distribution.batch.fail'");
        var laterCallback=new java.util.concurrent.atomic.AtomicBoolean();
        assertDoesNotThrow(()->tx.executeWithoutResult(s->{controlLocked();
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization(){@Override public void afterCommit(){laterCallback.set(true);}});
        }));assertTrue(laterCallback.get());assertEquals("ACTIVE",files.selectById(20L).getStatus());
        assertEquals(20L,masters.selectById(3L).getCurrentActiveControlledFileId());assertEquals("OBSOLETE",files.selectById(10L).getStatus());
        assertEquals("PENDING",batchRows.selectList().get(0).getStatus());
        assertEquals(1,count("gxp_audit_event","operation_id='dcc.distribution.batch.accept'"));
        assertEquals(0,count("gxp_audit_event","operation_id='dcc.distribution.batch.fail'"));
        assertEquals("PENDING",jdbc.queryForObject("SELECT delivery_status FROM dcc_workflow_lifecycle_event WHERE event_type='ACTIVATED'",String.class));
    }
    @Test void pendingControlDoesNotAcceptAndLateOuterRollbackCreatesNoBatch(){
        jdbc.update("UPDATE dcc_controlled_file SET effective_date=? WHERE id=20",now.toLocalDate().plusDays(2));control();
        assertTrue(batchRows.selectList().isEmpty());verify(batches,never()).acceptActivatedEvent(any());
        jdbc.update("UPDATE dcc_controlled_file SET effective_date=? WHERE id=20",now.toLocalDate());
        assertThrows(IllegalStateException.class,()->tx.executeWithoutResult(s->{lifecycle.activateDue(20L);throw new IllegalStateException("later business failure");}));
        assertTrue(batchRows.selectList().isEmpty());assertEquals("CONTROLLED_PENDING_EFFECTIVE",files.selectById(20L).getStatus());
        assertEquals(10L,masters.selectById(3L).getCurrentActiveControlledFileId());assertEquals("ACTIVE",files.selectById(10L).getStatus());
        verify(batches,never()).processAutomaticBatch(anyLong());
        assertEquals(0,count("dcc_workflow_lifecycle_event","event_type='ACTIVATED'"));
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans={false,true})
    void explicitEmptyAndMissingDepartmentsRemainAccurateFailuresAfterActivation(boolean missing){
        jdbc.update("UPDATE dcc_controlled_file SET distribution_department_ids_json=? WHERE id=20",missing?null:"[]");control();
        assertEquals(missing?"DISTRIBUTION_DEPARTMENTS_MISSING":"EMPTY_SCOPE_UNDECIDED",batchRows.selectList().get(0).getErrorCode());
        assertEquals(missing?null:"[]",batchRows.selectList().get(0).getDepartmentIdsJson());
        assertEquals("ACTIVE",files.selectById(20L).getStatus());assertTrue(jobs.selectList().isEmpty());
    }
    @Test void ordinaryPendingListNeedsOnlyActualFactsWithoutReminderConfigurationOrCalendarDate(){
        jdbc.update("UPDATE dcc_controlled_file SET status='ACTIVE',controlled_time=?,activated_time=?,effective_date=NULL,effective_mode='AFTER_STANDARDIZATION_SIGNATURE' WHERE id=20",
                now,now);
        var pending=lifecycle.pendingDistribution(false);
        assertEquals(List.of(10L,20L),pending.stream().map(DccControlledFileDO::getId).toList());
        jdbc.update("UPDATE dcc_controlled_file SET distributed_time=? WHERE id=10",now);
        assertEquals(List.of(20L),lifecycle.pendingDistribution(false,null).stream().map(DccControlledFileDO::getId).toList());
        assertEquals("文控提醒策略尚未批准",assertThrows(IllegalStateException.class,()->lifecycle.pendingDistribution(true)).getMessage());
        assertEquals("文控提醒策略尚未批准",assertThrows(IllegalStateException.class,()->lifecycle.pendingDistribution(true,null)).getMessage());
    }
    @Test void recoveryReplaysOnlyItsRealPersistedRootAndBatchWithoutAcknowledgingOtherConsumers(){
        control();var batch=batchRows.selectList().get(0);
        var event=new DccControlledFileLifecycleEvent(batch.getEventKey(),"ACTIVATED",1L,3L,20L,10L,"B/1","round-1",now);
        assertEquals(batch.getId(),tx.execute(s->batches.acceptActivatedEvent(event)));
        assertEquals(1,batchRows.selectList().size());
        assertEquals(List.of(batch.getId()),batchRows.recoverable(1L).stream().map(DccDistributionBatchDO::getId).toList());
        var recovered=batches.processAutomaticBatch(batch.getId());assertEquals("FAILED",recovered.status());
        assertEquals("ROSTER_POLICY_UNDECIDED",recovered.errorCode());assertTrue(jobs.selectList().isEmpty());
        assertEquals("ACTIVE",files.selectById(20L).getStatus());assertEquals("PENDING",jdbc.queryForObject(
                "SELECT delivery_status FROM dcc_workflow_lifecycle_event WHERE event_type='ACTIVATED'",String.class));
        var fabricated=new DccControlledFileLifecycleEvent("unknown-root","ACTIVATED",1L,3L,20L,10L,"B/1","round-1",now);
        assertThrows(IllegalStateException.class,()->tx.execute(s->batches.acceptActivatedEvent(fabricated)));
        assertEquals(1,batchRows.selectList().size());
    }
    protected void control(){tx.executeWithoutResult(s->controlLocked());}
    protected void controlLocked(){var master=masters.selectByIdForUpdate(3L);var file=files.selectByIdAndTenantForUpdate(1L,20L);
        lifecycle.completeControl(file,master,-1L);}
    protected void assertRollback(){assertEquals("ACTIVE",files.selectById(10L).getStatus());assertEquals(10L,masters.selectById(3L).getCurrentActiveControlledFileId());
        assertNull(files.selectById(20L).getControlledTime());assertNull(files.selectById(20L).getEffectiveTime());assertNull(files.selectById(20L).getActivatedTime());
        assertTrue(batchRows.selectList().isEmpty());assertEquals(0,count("dcc_workflow_lifecycle_event","1=1"));assertEquals(0,count("gxp_audit_event","1=1"));}
    protected int count(String table,String where){return jdbc.queryForObject("SELECT COUNT(*) FROM "+table+" WHERE "+where,Integer.class);}
    protected void policy(String operation){jdbc.update("""
       INSERT INTO gxp_audit_policy_operation(tenant_id,policy_version,operation_id,source_type,source_locator,domain,subject_type,
        action_type,reason_policy,signature_policy,state_policy,retention_class,test_ids,owner,applicability,active)
       VALUES(1,'IC10-W06-ISOLATED',?,'SERVICE_METHOD','IC10_W06_PRIVATE_FIXTURE','DCC','DCC_CONTROLLED_FILE','UPDATE','REQUIRED','NOT_REQUIRED',
        'PRESENT_TO_PRESENT','GXP_CONTROLLED_DOCUMENT','IC10-W06','W06','GXP',TRUE)
       """,operation);}
    protected static void wire(Object bean,Object... fields){for(int i=0;i<fields.length;i+=2)ReflectionTestUtils.setField(bean,(String)fields[i],fields[i+1]);}
}
