package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccSignatureVerifyRespVO;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.context.ApplicationEventPublisher;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Actual DCC core Mapper + task-owned H2 + Spring transactions; external signature verification/BPM are ports. */
class DccControlledFileLifecycleEffectiveModeTest {
    private final LocalDateTime now=LocalDateTime.of(2026,10,11,12,0);
    private final LocalDateTime signedAt=now.minusHours(2);
    private JdbcTemplate jdbc; private TransactionTemplate tx;
    private DccControlledFileLifecycleService service;
    private DccControlledFileMapper files; private DccControlledFileMasterMapper masters;
    private DccControlledFileSignatureMapper signatures;
    private BpmTaskService tasks;
    private DccLifecycleEffectiveContractResolver effectiveContractResolver;
    @BeforeEach void setup() throws Exception {
        TenantContextHolder.setTenantId(1L);
        var ds=new DriverManagerDataSource("jdbc:h2:mem:w06_effective_"+UUID.randomUUID()+";MODE=MYSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1","sa","");
        jdbc=new JdbcTemplate(ds);assertNotNull(jdbc.queryForObject("SELECT H2VERSION()",String.class));
        table(DccControlledFileDO.class,"dcc_controlled_file");table(DccControlledFileMasterDO.class,"dcc_controlled_file_master");
        table(DccControlledFileSignatureDO.class,"dcc_controlled_file_signature");table(DccControlledFileObsoleteAuditDO.class,"dcc_controlled_file_obsolete_audit");
        jdbc.execute("""
          CREATE TABLE dcc_workflow_lifecycle_event(id BIGINT AUTO_INCREMENT PRIMARY KEY,tenant_id BIGINT,event_key VARCHAR(255),
          event_type VARCHAR(32),master_id BIGINT,controlled_file_id BIGINT,previous_active_file_id BIGINT,version_no VARCHAR(64),
          approval_process_instance_id VARCHAR(128),occurred_at DATETIME,delivery_status VARCHAR(32),UNIQUE(tenant_id,event_key))
          """);
        var factory=new MybatisSqlSessionFactoryBean();factory.setDataSource(ds);
        var configuration=new MybatisConfiguration();configuration.setMapUnderscoreToCamelCase(true);
        factory.setConfiguration(configuration);factory.setGlobalConfig(new GlobalConfig().setDbConfig(new GlobalConfig.DbConfig().setIdType(IdType.AUTO)));
        var sessions=factory.getObject();for(var mapper:List.of(DccControlledFileMapper.class,DccControlledFileMasterMapper.class,
                DccControlledFileSignatureMapper.class,DccControlledFileObsoleteAuditMapper.class))sessions.getConfiguration().addMapper(mapper);
        var template=new SqlSessionTemplate(sessions);files=template.getMapper(DccControlledFileMapper.class);
        masters=template.getMapper(DccControlledFileMasterMapper.class);signatures=template.getMapper(DccControlledFileSignatureMapper.class);
        tx=new TransactionTemplate(new DataSourceTransactionManager(ds));service=new DccControlledFileLifecycleService();
        var dates=new DccWorkflowDatePolicy(){@Override public LocalDateTime now(){return now;}};
        var verification=mock(DccElectronicSignatureManagementService.class);
        when(verification.verifySignatureEvidence(anyLong())).thenAnswer(call->{var result=new DccSignatureVerifyRespVO();
            result.setSignatureId(call.getArgument(0));result.setVerificationStatus("VALID");return result;});
        tasks=mock(BpmTaskService.class);var task=mock(org.flowable.task.api.Task.class);
        when(task.getId()).thenReturn("doc-task-1");when(task.getProcessInstanceId()).thenReturn("round-1");when(task.getTenantId()).thenReturn("1");
        when(task.getTaskDefinitionKey()).thenReturn("DOC_CONTROL_REVIEW");when(task.getAssignee()).thenReturn("88");
        when(tasks.getTask("doc-task-1")).thenReturn(task);
        wire(service,"controlledFileMapper",files,"masterMapper",masters,"datePolicy",dates,"jdbcTemplate",jdbc,
                "versionPolicy",DccControlledFileVersionPolicy.defaultPolicy(),"obsoleteAuditMapper",template.getMapper(DccControlledFileObsoleteAuditMapper.class),
                "eventPublisher",(ApplicationEventPublisher)event->{},"fileStateAudit",mock(DccWorkflowFileStateAudit.class),
                "platformAdapter",mock(DccControlledContentAdapter.class),"obsoleteRetentionService",mock(DccObsoleteRetentionService.class));
        wire(service,"approvalSignatureMapper",signatures,"signatureManagementService",verification,"bpmTaskService",tasks);
        var batchPort=mock(DccDistributionBatchService.class);when(batchPort.acceptActivatedEvent(any())).thenReturn(900L);
        when(batchPort.processAutomaticBatch(900L))
                .thenReturn(new DccDistributionBatchResult(900L,"FAILED","ROSTER_POLICY_UNDECIDED","isolated external batch port"));
        wire(service,"distributionBatchService",batchPort);
        effectiveContractResolver=mock(DccLifecycleEffectiveContractResolver.class);
        // Exact workflow identity is the declared external port; actual leaf/BPM identity has its own resolver suite.
        when(effectiveContractResolver.resolve(any())).thenAnswer(call->{var current=call.getArgument(0,DccControlledFileDO.class);
            if(current.getEffectiveMode()==null)return null;
            return new DccLifecycleEffectiveContract(current.getTenantId(),current.getId(),current.getProcessInstanceId(),"exact-public-definition",
                    "exact-deployment",current.getProcessDefinitionKey(),"a".repeat(64),DccLifecycleEffectiveContract.Rule.NEW_EXPLICIT_MODE);});
        wire(service,"effectiveContractResolver",effectiveContractResolver);
        masters.insert(DccControlledFileMasterDO.builder().id(3L).tenantId(1L).currentActiveControlledFileId(10L).latestControlledFileId(10L).status("ACTIVE_CHAIN").build());
        var old=DccControlledFileDO.builder().id(10L).tenantId(1L).masterId(3L).versionNo("A/1").status("ACTIVE")
                .controlledTime(now.minusDays(20)).activatedTime(now.minusDays(20)).processInstanceId("old-round").publishedFileId(100L).stampedFileId(100L).build();
        files.insert(old);
        var file=DccControlledFileDO.builder().id(20L).tenantId(1L).masterId(3L).versionNo("B/1").status("PENDING_DOC_CONTROL_REVIEW")
                .processInstanceId("round-1").processDefinitionKey(DccControlledFileProcessDefinitionKeys.UPLOAD).requesterId(99L)
                .effectiveMode(DccWorkflowDatePolicy.AFTER_STANDARDIZATION_SIGNATURE).sourceFileId(111L).build();
        files.insert(file);
        signatures.insert(DccControlledFileSignatureDO.builder().id(700L).controlledFileId(20L).revisionId(20L).versionNo("B/1")
                .processInstanceId("round-1").taskId("doc-task-1").actorId(88L).actionType("APPROVE").meaningCode("DOC_CONTROL_REVIEW_APPROVE")
                .passwordVerified(true).signedAt(signedAt).evidenceStatus("VALID").evidenceHash("actual-domain-evidence")
                .evidencePayloadVersion("v4-workflow").build());
    }
    @AfterEach void clear(){if(jdbc!=null)jdbc.execute("SHUTDOWN");TenantContextHolder.clear();}
    @Test void realSignatureTimeIsSavedBeforeControlAndReplayDoesNotSwitchOldDailyVersion() {
        effect();var file=files.selectById(20L);assertEquals(signedAt,file.getEffectiveTime());assertEquals(700L,file.getEffectiveSignatureId());
        assertEquals("round-1",file.getEffectiveProcessInstanceId());assertNull(file.getEffectiveDate());assertNull(file.getControlledTime());assertNull(file.getActivatedTime());
        assertEquals(10L,masters.selectById(3L).getCurrentActiveControlledFileId());assertEquals(1,eventCount("EFFECTIVE"));
        assertEquals(signedAt,jdbc.queryForObject("SELECT occurred_at FROM dcc_workflow_lifecycle_event WHERE event_type='EFFECTIVE'",LocalDateTime.class));
        var original=jdbc.queryForMap("SELECT * FROM dcc_controlled_file WHERE id=20");effect();
        assertEquals(original,jdbc.queryForMap("SELECT * FROM dcc_controlled_file WHERE id=20"));assertEquals(1,eventCount("EFFECTIVE"));
    }
    @Test void signatureEffectiveVersionActivatesOnlyAfterControlAndObsoletesOldAtomically() {
        effect();artifacts();control();var file=files.selectById(20L);assertEquals(signedAt,file.getEffectiveTime());
        assertEquals(now,file.getControlledTime());assertEquals(now,file.getActivatedTime());assertEquals("ACTIVE",file.getStatus());
        assertEquals(20L,masters.selectById(3L).getCurrentActiveControlledFileId());assertEquals("OBSOLETE",files.selectById(10L).getStatus());
        assertEquals(1,eventCount("CONTROLLED"));assertEquals(1,eventCount("ACTIVATED"));assertEquals(1,eventCount("EFFECTIVE"));
    }
    @Test void presetFutureRetainsOldUntilDueThenWritesSystemEffectiveFactWithNoFakeSignature() {
        jdbc.update("UPDATE dcc_controlled_file SET effective_mode='PRESET_DATE',effective_date=? WHERE id=20",now.toLocalDate().plusDays(2));
        artifacts();control();var pending=files.selectById(20L);assertNull(pending.getEffectiveTime());assertNull(pending.getActivatedTime());
        assertEquals(10L,masters.selectById(3L).getCurrentActiveControlledFileId());
        jdbc.update("UPDATE dcc_controlled_file SET effective_date=? WHERE id=20",now.toLocalDate());
        assertEquals(true,tx.execute(s->service.activateDue(20L)));var active=files.selectById(20L);
        assertEquals(now,active.getEffectiveTime());assertEquals(now,active.getActivatedTime());assertNull(active.getEffectiveSignatureId());
        assertEquals("round-1",active.getEffectiveProcessInstanceId());assertEquals(1,eventCount("EFFECTIVE"));
    }
    @Test void unknownHistoricalModeIsNotInferredOrWrittenByNewControl() {
        jdbc.update("UPDATE dcc_controlled_file SET effective_mode=NULL,effective_date=? WHERE id=20",now.toLocalDate());artifacts();
        var original=jdbc.queryForMap("SELECT * FROM dcc_controlled_file WHERE id=20");assertThrows(IllegalArgumentException.class,this::control);
        assertEquals(original,jdbc.queryForMap("SELECT * FROM dcc_controlled_file WHERE id=20"));assertEquals(10L,masters.selectById(3L).getCurrentActiveControlledFileId());
    }
    @Test void signatureIdentityConflictAndMissingTransactionNeverWriteEffectiveFact() {
        jdbc.update("UPDATE dcc_controlled_file_signature SET process_instance_id='other-round' WHERE id=700");
        assertThrows(RuntimeException.class,this::effect);assertNull(files.selectById(20L).getEffectiveTime());assertEquals(0,eventCount("EFFECTIVE"));
        assertThrows(RuntimeException.class,()->recordEffect());
    }
    @Test void eventFailureRollsBackEffectiveFactAndSignatureInSameTransaction() {
        wire(service,"eventPublisher",(ApplicationEventPublisher)event->{throw new IllegalStateException("event failure");});
        assertThrows(IllegalStateException.class,()->tx.executeWithoutResult(s->{
            jdbc.update("UPDATE dcc_controlled_file_signature SET comment='same transaction signed' WHERE id=700");recordEffect();}));
        assertNull(files.selectById(20L).getEffectiveTime());assertNull(signatures.selectById(700L).getComment());assertEquals(0,eventCount("EFFECTIVE"));
    }
    @Test void failedControlPreservesCommittedSignedEffectiveFactButNeverSwitchesOldVersion() {
        effect();artifacts();wire(service,"eventPublisher",(ApplicationEventPublisher)event->{
            if("CONTROLLED".equals(((DccControlledFileLifecycleEvent)event).eventType()))throw new IllegalStateException("control failure");});
        assertThrows(IllegalStateException.class,this::control);var file=files.selectById(20L);
        assertEquals(signedAt,file.getEffectiveTime());assertNull(file.getControlledTime());assertNull(file.getActivatedTime());
        assertEquals(10L,masters.selectById(3L).getCurrentActiveControlledFileId());assertEquals("ACTIVE",files.selectById(10L).getStatus());
        assertEquals(1,eventCount("EFFECTIVE"));assertEquals(0,eventCount("CONTROLLED"));assertEquals(0,eventCount("ACTIVATED"));
    }
    @Test void presetDateLegalAtSubmissionActivatesAtActualTimeAfterApprovalCrossesTheDate() {
        var submittedDate=now.toLocalDate().minusDays(1);
        var submissionPolicy=new DccWorkflowDatePolicy(){@Override public LocalDateTime now(){return submittedDate.atTime(9,0);}};
        submissionPolicy.requireEffectiveConfiguration(DccWorkflowDatePolicy.PRESET_DATE,submittedDate);
        jdbc.update("UPDATE dcc_controlled_file SET effective_mode='PRESET_DATE',effective_date=? WHERE id=20",submittedDate);
        artifacts();control();var active=files.selectById(20L);
        assertEquals("ACTIVE",active.getStatus());assertEquals(now,active.getControlledTime());assertEquals(now,active.getEffectiveTime());
        assertEquals(now,active.getActivatedTime());assertEquals(submittedDate,active.getEffectiveDate());assertNull(active.getEffectiveSignatureId());
        assertEquals(20L,masters.selectById(3L).getCurrentActiveControlledFileId());assertEquals("OBSOLETE",files.selectById(10L).getStatus());
        assertEquals(1,eventCount("CONTROLLED"));assertEquals(1,eventCount("EFFECTIVE"));assertEquals(1,eventCount("ACTIVATED"));
    }
    @Test void alreadySignedFactRejectsDifferentSignatureAndEndedTaskRejectsFirstWrite() {
        when(tasks.getTask("doc-task-1")).thenReturn(null);assertThrows(IllegalStateException.class,this::effect);
        assertNull(files.selectById(20L).getEffectiveTime());
        jdbc.update("UPDATE dcc_controlled_file SET effective_time=?,effective_signature_id=701,effective_process_instance_id='round-1' WHERE id=20",signedAt);
        assertThrows(IllegalArgumentException.class,this::effect);assertEquals(0,eventCount("EFFECTIVE"));
    }
    @Test void concurrentSignatureEffectReplayKeepsOneFactAndOneEvent() throws Exception {
        var executor=java.util.concurrent.Executors.newFixedThreadPool(2);var start=new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.Callable<Void> write=()->{TenantContextHolder.setTenantId(1L);
            try {start.await();effect();return null;}finally {TenantContextHolder.clear();}};
        try {var a=executor.submit(write);var b=executor.submit(write);start.countDown();
            a.get(10,java.util.concurrent.TimeUnit.SECONDS);b.get(10,java.util.concurrent.TimeUnit.SECONDS);
            assertEquals(signedAt,files.selectById(20L).getEffectiveTime());assertEquals(1,eventCount("EFFECTIVE"));
            assertEquals(10L,masters.selectById(3L).getCurrentActiveControlledFileId());
        } finally {executor.shutdownNow();assertTrue(executor.awaitTermination(10,java.util.concurrent.TimeUnit.SECONDS));}
    }
    @Test void dueQueryPreservesHistoricalCandidatesButDoesNotInferTheirExecutionRule() {
        jdbc.update("UPDATE dcc_controlled_file SET effective_mode='PRESET_DATE',effective_date=? WHERE id=20",now.toLocalDate().plusDays(1));
        artifacts();control();jdbc.update("UPDATE dcc_controlled_file SET effective_date=? WHERE id=20",now.toLocalDate());
        assertEquals(List.of(20L),service.dueVersions().stream().map(DccControlledFileDO::getId).toList());
        jdbc.update("UPDATE dcc_controlled_file SET effective_mode=NULL WHERE id=20");
        assertEquals(List.of(20L),service.dueVersions().stream().map(DccControlledFileDO::getId).toList());
        assertThrows(IllegalArgumentException.class,()->tx.execute(s->service.activateDue(20L)));
        assertNull(files.selectById(20L).getEffectiveMode());assertNull(files.selectById(20L).getEffectiveTime());
    }
    @Test void presetModeRejectsPrematureEffectiveFactsBeforeFirstControl() {
        jdbc.update("UPDATE dcc_controlled_file SET effective_mode='PRESET_DATE',effective_date=?,effective_time=?,effective_process_instance_id='round-1' WHERE id=20",
                now.toLocalDate(),now.minusHours(1));artifacts();
        assertThrows(IllegalStateException.class,this::control);assertNull(files.selectById(20L).getControlledTime());
        assertEquals(10L,masters.selectById(3L).getCurrentActiveControlledFileId());assertEquals(0,eventCount("CONTROLLED"));
    }
    @Test void provenFrozenLegacyDateContractPreservesNullFactsAndOldPendingDateSemantics() {
        legacyContract();jdbc.update("UPDATE dcc_controlled_file SET effective_mode=NULL,effective_date=? WHERE id=20",now.toLocalDate().plusDays(2));
        artifacts();control();assertEquals("CONTROLLED_PENDING_EFFECTIVE",files.selectById(20L).getStatus());
        assertEquals(10L,masters.selectById(3L).getCurrentActiveControlledFileId());
        jdbc.update("UPDATE dcc_controlled_file SET effective_date=? WHERE id=20",now.toLocalDate());
        assertEquals(true,tx.execute(s->service.activateDue(20L)));var active=files.selectById(20L);
        assertEquals("ACTIVE",active.getStatus());assertNull(active.getEffectiveMode());assertNull(active.getEffectiveTime());
        assertNull(active.getEffectiveSignatureId());assertNull(active.getEffectiveProcessInstanceId());assertEquals(0,eventCount("EFFECTIVE"));
        assertEquals(1,eventCount("ACTIVATED"));assertEquals("OBSOLETE",files.selectById(10L).getStatus());
    }
    @Test void provenLegacyContractStillRejectsPastDateAtControlRatherThanSilentlyCorrectingIt() {
        legacyContract();jdbc.update("UPDATE dcc_controlled_file SET effective_mode=NULL,effective_date=? WHERE id=20",now.toLocalDate().minusDays(1));artifacts();
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,this::control);
        assertNull(files.selectById(20L).getControlledTime());assertNull(files.selectById(20L).getEffectiveMode());assertEquals(0,eventCount("CONTROLLED"));
    }
    @Test void provenLegacyDocControlSignatureDoesNotFabricateNewEffectivenessFacts() {
        legacyContract();jdbc.update("UPDATE dcc_controlled_file SET effective_mode=NULL,effective_date=? WHERE id=20",now.toLocalDate());
        effect();assertNull(files.selectById(20L).getEffectiveTime());assertNull(files.selectById(20L).getEffectiveSignatureId());
        assertEquals(0,eventCount("EFFECTIVE"));assertNull(files.selectById(20L).getEffectiveMode());
    }
    private void legacyContract(){doAnswer(call->{var file=call.getArgument(0,DccControlledFileDO.class);
        return new DccLifecycleEffectiveContract(file.getTenantId(),file.getId(),file.getProcessInstanceId(),"frozen-real-v4-id","frozen-deployment",
                file.getProcessDefinitionKey(),"a8222aa8aa09dba8aba4bc2ff3e2b296b1b824849e48dca3f9a4d1a5809d99c6",DccLifecycleEffectiveContract.Rule.LEGACY_V4_PRESET_DATE);}).when(effectiveContractResolver).resolve(any());}
    private void effect(){tx.executeWithoutResult(s->recordEffect());}
    private void recordEffect(){service.recordStandardizationSignatureEffect(88L,20L,700L);}
    private void artifacts(){jdbc.update("UPDATE dcc_controlled_file SET status='READY_TO_PUBLISH',published_file_id=211,stamped_file_id=211 WHERE id=20");}
    private void control(){tx.executeWithoutResult(s->{var master=masters.selectByIdForUpdate(3L);var file=files.selectByIdAndTenantForUpdate(1L,20L);service.completeControl(file,master,88L);});}
    private int eventCount(String type){return jdbc.queryForObject("SELECT COUNT(*) FROM dcc_workflow_lifecycle_event WHERE event_type=?",Integer.class,type);}
    private void table(Class<?> type,String name){var cols=new LinkedHashMap<String,String>();
        for(var cls=type;cls!=Object.class;cls=cls.getSuperclass())for(var field:cls.getDeclaredFields()){
            if(java.lang.reflect.Modifier.isStatic(field.getModifiers()))continue;
            String column=field.getName().replaceAll("([a-z0-9])([A-Z])","$1_$2").toLowerCase();var t=field.getType();
            String sql=t==Long.class?"BIGINT":t==Integer.class?"INT":t==Boolean.class?"TINYINT DEFAULT 0":t==LocalDateTime.class?"DATETIME":t==LocalDate.class?"DATE":"VARCHAR(4096)";
            cols.put(column,"id".equals(column)?"id BIGINT AUTO_INCREMENT PRIMARY KEY":column+" "+sql);}
        jdbc.execute("CREATE TABLE "+name+"("+String.join(",",cols.values())+")");}
    private static void wire(Object bean,Object... fields){for(int i=0;i<fields.length;i+=2)ReflectionTestUtils.setField(bean,(String)fields[i],fields[i+1]);}
}
