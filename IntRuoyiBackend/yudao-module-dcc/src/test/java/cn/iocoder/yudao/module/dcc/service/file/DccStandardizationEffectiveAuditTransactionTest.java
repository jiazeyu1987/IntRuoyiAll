package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccSignatureVerifyRespVO;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditServiceImpl;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
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

/** Actual unified GxP kernel/policy/event/sequence Mapper and DCC state snapshot, on existing isolated H2 fixture. */
@Import(GxpAuditServiceImpl.class)
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(scripts="/sql/dcc_b_gxp_audit_tables.sql",executionPhase=Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts="/sql/dcc_b_gxp_audit_clean.sql",executionPhase=Sql.ExecutionPhase.AFTER_TEST_METHOD)
class DccStandardizationEffectiveAuditTransactionTest extends BaseDbUnitTest {
    @Resource private GxpAuditServiceImpl ledger;
    @Resource private DataSource dataSource;
    @Resource private PlatformTransactionManager transactionManager;
    @Resource(name="dccControlledFileMapper") private DccControlledFileMapper files;
    @Resource private DccControlledFileMasterMapper masters;
    @Resource private DccControlledFileSignatureMapper signatures;
    @Resource private DccControlledFileObsoleteAuditMapper obsoleteAudits;
    @MockitoBean private AdminUserApi accounts;
    private JdbcTemplate jdbc;private TransactionTemplate tx;private DccControlledFileLifecycleService lifecycle;
    private DccWorkflowFileStateAudit audit;
    private final LocalDateTime signedAt=LocalDateTime.of(2026,10,11,10,1,2);
    @BeforeEach void fixture(){
        jdbc=new JdbcTemplate(dataSource);assertNotNull(jdbc.queryForObject("SELECT H2VERSION()",String.class));
        tx=new TransactionTemplate(transactionManager);TenantContextHolder.setTenantId(1L);
        when(accounts.getUser(88L)).thenReturn(new AdminUserRespDTO().setId(88L).setTenantId(1L).setStatus(0).setUsername("real-doc").setNickname("真实文控"));
        SecurityFrameworkUtils.setLoginUser(new LoginUser().setId(88L).setTenantId(1L).setUserType(UserTypeEnum.ADMIN.getValue())
                .setInfo(Map.of("username","real-doc",LoginUser.INFO_KEY_NICKNAME,"真实文控")),new org.springframework.mock.web.MockHttpServletRequest());
        masters.insert(DccControlledFileMasterDO.builder().id(3L).tenantId(1L).categoryId(2L).fileName("SOP.pdf").fileNumber("SOP-01")
                .status("ACTIVE_CHAIN").currentActiveControlledFileId(10L).latestControlledFileId(10L).build());
        files.insert(DccControlledFileDO.builder().id(10L).tenantId(1L).masterId(3L).categoryId(2L).directoryId(4L).fileName("SOP.pdf")
                .sourceFileId(110L).originalFileId(110L).processType("CONTROLLED_FILE").changeType("NEW")
                .title("SOP").fileNumber("SOP-01").versionNo("A/1").status("ACTIVE").requesterId(99L).submitterId(99L)
                .processInstanceId("old-round").controlledTime(signedAt.minusDays(20)).activatedTime(signedAt.minusDays(20)).build());
        files.insert(DccControlledFileDO.builder().id(20L).tenantId(1L).masterId(3L).categoryId(2L).directoryId(4L).fileName("SOP.pdf")
                .sourceFileId(111L).originalFileId(111L).processType("CONTROLLED_FILE").changeType("REVISION")
                .title("SOP").fileNumber("SOP-01").versionNo("B/1").status("PENDING_DOC_CONTROL_REVIEW").requesterId(99L).submitterId(99L)
                .processInstanceId("round-1").processDefinitionKey(DccControlledFileProcessDefinitionKeys.UPLOAD)
                .effectiveMode(DccWorkflowDatePolicy.AFTER_STANDARDIZATION_SIGNATURE).build());
        signature();
        audit=new DccWorkflowFileStateAudit();wire(audit,"jdbcTemplate",jdbc,"auditService",ledger);
        lifecycle=new DccControlledFileLifecycleService();var dates=new DccWorkflowDatePolicy();dates.setZoneId("Asia/Shanghai");
        var verify=mock(DccElectronicSignatureManagementService.class);var result=new DccSignatureVerifyRespVO();result.setSignatureId(700L);result.setVerificationStatus("VALID");
        when(verify.verifySignatureEvidence(700L)).thenReturn(result);
        var tasks=mock(BpmTaskService.class);var task=mock(org.flowable.task.api.Task.class);
        when(task.getId()).thenReturn("doc-task");when(task.getTenantId()).thenReturn("1");when(task.getProcessInstanceId()).thenReturn("round-1");
        when(task.getTaskDefinitionKey()).thenReturn("DOC_CONTROL_REVIEW");when(task.getAssignee()).thenReturn("88");when(tasks.getTask("doc-task")).thenReturn(task);
        wire(lifecycle,"controlledFileMapper",files,"masterMapper",masters,"datePolicy",dates,"approvalSignatureMapper",signatures,
                "signatureManagementService",verify,"bpmTaskService",tasks,"jdbcTemplate",jdbc,"fileStateAudit",audit,
                "platformAdapter",mock(DccControlledContentAdapter.class),"obsoleteAuditMapper",obsoleteAudits,
                "obsoleteRetentionService",mock(DccObsoleteRetentionService.class),"versionPolicy",DccControlledFileVersionPolicy.defaultPolicy(),
                "eventPublisher",(org.springframework.context.ApplicationEventPublisher)event->{});
        var batchPort=mock(DccDistributionBatchService.class);when(batchPort.acceptActivatedEvent(any())).thenReturn(900L);
        when(batchPort.processAutomaticBatch(900L))
                .thenReturn(new DccDistributionBatchResult(900L,"FAILED","ROSTER_POLICY_UNDECIDED","isolated external batch port"));
        wire(lifecycle,"distributionBatchService",batchPort);
        var contractPort=mock(DccLifecycleEffectiveContractResolver.class);
        when(contractPort.resolve(any())).thenAnswer(call->{var current=call.getArgument(0,DccControlledFileDO.class);
            return new DccLifecycleEffectiveContract(current.getTenantId(),current.getId(),current.getProcessInstanceId(),"exact-public-id",
                    "exact-deployment",current.getProcessDefinitionKey(),"a".repeat(64),DccLifecycleEffectiveContract.Rule.NEW_EXPLICIT_MODE);});
        wire(lifecycle,"effectiveContractResolver",contractPort);
    }
    @AfterEach void clear(){SecurityContextHolder.clearContext();TenantContextHolder.clear();}
    @Test void realAuditCapturesActualSignedEffectAndNamespacedProjectionEvidence(){
        policy("dcc.controlled-file.standardization-effective","REQUIRED");effect();
        var events=jdbc.queryForList("SELECT * FROM gxp_audit_event WHERE operation_id='dcc.controlled-file.standardization-effective'");
        assertEquals(1,events.size());var event=events.get(0);
        assertEquals("DCC_CONTROLLED_FILE_SIGNATURE:700",event.get("signature_record_id"));assertEquals("actual-dcc-hmac",event.get("signature_content_hash"));
        var before=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(event.get("before_state_json").toString(),Map.class);
        var after=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(event.get("after_state_json").toString(),Map.class);
        assertEquals(DccWorkflowDatePolicy.AFTER_STANDARDIZATION_SIGNATURE,before.get("effectiveMode"));assertNull(before.get("effectiveTime"));
        assertEquals(signedAt.toString(),after.get("effectiveTime"));assertEquals(700,((Number)after.get("effectiveSignatureId")).intValue());
        assertEquals("round-1",after.get("effectiveProcessInstanceId"));assertNull(after.get("activatedTime"));assertEquals(88L,((Number)event.get("actor_id")).longValue());
        effect();assertEquals(1,count());
    }
    @Test void missingPolicyRollsBackNewSignatureEffectAndProcessWriteTogether(){
        jdbc.update("DELETE FROM dcc_controlled_file_signature WHERE id=700");
        var failure=assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->tx.executeWithoutResult(s->{signature();lifecycle.recordStandardizationSignatureEffect(88L,20L,700L);
            jdbc.update("UPDATE dcc_controlled_file SET status='PENDING_APPLICANT_TRAINING_RECORD' WHERE id=20");}));
        assertEquals(cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.GXP_AUDIT_POLICY_NOT_FOUND.getCode(),failure.getCode());
        assertNull(signatures.selectById(700L));assertNull(files.selectById(20L).getEffectiveTime());assertEquals(0,count());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_workflow_lifecycle_event",Integer.class));
    }
    @Test void lateAuditAppendFailureRollsBackAllSuccessFacts(){
        policy("dcc.controlled-file.standardization-effective","REQUIRED");
        jdbc.execute("ALTER TABLE gxp_audit_event ADD CONSTRAINT w06_effect_fail CHECK(operation_id<>'dcc.controlled-file.standardization-effective')");
        try{var failure=assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,this::effect);
            assertEquals(cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.GXP_AUDIT_APPEND_FAILED.getCode(),failure.getCode());
            assertNull(files.selectById(20L).getEffectiveTime());assertEquals(0,count());
            assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_workflow_lifecycle_event",Integer.class));}
        finally{jdbc.execute("ALTER TABLE gxp_audit_event DROP CONSTRAINT w06_effect_fail");}
    }
    @Test void failedLaterControlRetainsCommittedSignatureEffectAuditAndOldDailyVersion(){
        policy("dcc.controlled-file.standardization-effective","REQUIRED");effect();
        jdbc.update("UPDATE dcc_controlled_file SET status='READY_TO_PUBLISH',published_file_id=211,stamped_file_id=211 WHERE id=20");
        assertThrows(RuntimeException.class,()->tx.executeWithoutResult(s->lifecycle.completeControl(files.selectByIdAndTenantForUpdate(1L,20L),masters.selectByIdForUpdate(3L),88L)));
        assertEquals(signedAt,files.selectById(20L).getEffectiveTime());assertNull(files.selectById(20L).getControlledTime());
        assertEquals(10L,masters.selectById(3L).getCurrentActiveControlledFileId());assertEquals(1,count());assertNotNull(signatures.selectById(700L));
    }
    @Test void activationAuditIsSystemDailyUseWithSignedModeReason(){
        policy("dcc.controlled-file.standardization-effective","REQUIRED");policy("dcc.controlled-file.control","NOT_REQUIRED");
        policy("dcc.controlled-file.activate","NOT_REQUIRED");policy("dcc.controlled-file.auto-obsolete","NOT_REQUIRED");effect();
        jdbc.update("UPDATE dcc_controlled_file SET status='READY_TO_PUBLISH',published_file_id=211,stamped_file_id=211 WHERE id=20");
        tx.executeWithoutResult(s->lifecycle.completeControl(files.selectByIdAndTenantForUpdate(1L,20L),masters.selectByIdForUpdate(3L),88L));
        var event=jdbc.queryForMap("SELECT * FROM gxp_audit_event WHERE operation_id='dcc.controlled-file.activate'");
        assertEquals(-1L,((Number)event.get("actor_id")).longValue());assertNull(event.get("signature_record_id"));
        assertTrue(event.get("reason").toString().contains("文控签名"));assertTrue(event.get("reason").toString().contains("日常启用"));
        assertFalse(event.get("reason").toString().contains("预设"));assertNotNull(files.selectById(20L).getActivatedTime());
    }
    @Test void changedProjectionEvidenceCannotBePassedAsRealSignatureReference(){
        policy("dcc.controlled-file.standardization-effective","REQUIRED");
        assertThrows(IllegalStateException.class,()->tx.executeWithoutResult(s->{
            var before=audit.capture(files.selectById(20L),masters.selectById(3L));
            jdbc.update("UPDATE dcc_controlled_file SET effective_time=?,effective_signature_id=700,effective_process_instance_id='round-1' WHERE id=20",signedAt);
            var forged=signatures.selectById(700L);forged.setEvidenceHash("different-not-persisted-evidence");
            audit.recordSignatureEffect(before,"round-1",forged);
        }));
        assertNull(files.selectById(20L).getEffectiveTime());assertEquals(0,count());
    }
    @Test void signedEffectAuditRequiresExactAuthenticatedSigner(){
        policy("dcc.controlled-file.standardization-effective","REQUIRED");
        SecurityFrameworkUtils.setLoginUser(new LoginUser().setId(77L).setTenantId(1L).setUserType(UserTypeEnum.ADMIN.getValue())
                .setInfo(Map.of("username","other",LoginUser.INFO_KEY_NICKNAME,"其他人")),new org.springframework.mock.web.MockHttpServletRequest());
        assertThrows(IllegalStateException.class,this::effect);assertNull(files.selectById(20L).getEffectiveTime());assertEquals(0,count());
    }
    @Test void laterProcessFailureRollsBackSignatureEffectOutboxAndRealLedger(){
        policy("dcc.controlled-file.standardization-effective","REQUIRED");jdbc.update("DELETE FROM dcc_controlled_file_signature WHERE id=700");
        assertThrows(IllegalStateException.class,()->tx.executeWithoutResult(s->{
            signature();lifecycle.recordStandardizationSignatureEffect(88L,20L,700L);
            jdbc.update("UPDATE dcc_controlled_file SET status='PENDING_APPLICANT_TRAINING_RECORD' WHERE id=20");
            throw new IllegalStateException("actual task-progression late failure injection");
        }));
        assertNull(signatures.selectById(700L));assertNull(files.selectById(20L).getEffectiveTime());assertEquals(0,count());
        assertEquals("PENDING_DOC_CONTROL_REVIEW",files.selectById(20L).getStatus());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_workflow_lifecycle_event",Integer.class));
        assertEquals(10L,masters.selectById(3L).getCurrentActiveControlledFileId());
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans={false,true})
    void activationAuditPreservesPresetAndProvenLegacyDateContractMeaning(boolean legacy){
        policy("dcc.controlled-file.control","NOT_REQUIRED");policy("dcc.controlled-file.activate","NOT_REQUIRED");policy("dcc.controlled-file.auto-obsolete","NOT_REQUIRED");
        jdbc.update("UPDATE dcc_controlled_file SET effective_mode=?,effective_date=?,status='READY_TO_PUBLISH',published_file_id=211,stamped_file_id=211 WHERE id=20",
                legacy?null:DccWorkflowDatePolicy.PRESET_DATE,LocalDate.now(ZoneId.of("Asia/Shanghai")));
        if(legacy){var resolver=mock(DccLifecycleEffectiveContractResolver.class);when(resolver.resolve(any())).thenAnswer(call->{
            var file=call.getArgument(0,DccControlledFileDO.class);return new DccLifecycleEffectiveContract(1L,20L,"round-1","known-old-id","known-deployment",
                file.getProcessDefinitionKey(),"a8222aa8aa09dba8aba4bc2ff3e2b296b1b824849e48dca3f9a4d1a5809d99c6",DccLifecycleEffectiveContract.Rule.LEGACY_V4_PRESET_DATE);});
            wire(lifecycle,"effectiveContractResolver",resolver);}
        tx.executeWithoutResult(s->lifecycle.completeControl(files.selectByIdAndTenantForUpdate(1L,20L),masters.selectByIdForUpdate(3L),88L));
        var event=jdbc.queryForMap("SELECT * FROM gxp_audit_event WHERE operation_id='dcc.controlled-file.activate'");
        assertEquals(-1L,((Number)event.get("actor_id")).longValue());assertNull(event.get("signature_record_id"));
        assertTrue(event.get("reason").toString().contains("日常启用"));assertTrue(event.get("reason").toString().contains(legacy?"旧日期":"预设日期"));
        var after=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(event.get("after_state_json").toString(),Map.class);
        if(legacy){assertNull(after.get("effectiveMode"));assertNull(after.get("effectiveTime"));}
        else{assertEquals(DccWorkflowDatePolicy.PRESET_DATE,after.get("effectiveMode"));assertNotNull(after.get("effectiveTime"));}
    }
    @Test void newSnapshotVersionNeverRewritesSavedV1LedgerOrResignsStoredProjection(){
        Long oldEventId=priorSavedV1Event();
        var priorEvent=jdbc.queryForMap("SELECT * FROM gxp_audit_event WHERE id=?",oldEventId);
        var priorSignature=jdbc.queryForMap("SELECT * FROM dcc_controlled_file_signature WHERE id=700");
        policy("dcc.controlled-file.standardization-effective","REQUIRED");effect();effect();
        assertEquals(priorEvent,jdbc.queryForMap("SELECT * FROM gxp_audit_event WHERE id=?",oldEventId));
        assertEquals(priorSignature,jdbc.queryForMap("SELECT * FROM dcc_controlled_file_signature WHERE id=700"));
        var newEvent=jdbc.queryForMap("SELECT * FROM gxp_audit_event WHERE operation_id='dcc.controlled-file.standardization-effective'");
        assertEquals(priorEvent.get("event_hash"),newEvent.get("previous_event_hash"));
        assertEquals("dcc.file-state.v1",facts(priorEvent,"after_state_json").get("schemaVersion"));
        assertEquals("dcc.file-state.v2",facts(newEvent,"before_state_json").get("schemaVersion"));
        assertEquals("dcc.file-state.v2",facts(newEvent,"after_state_json").get("schemaVersion"));
        assertEquals(2,count());assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_signature",Integer.class));
    }
    @Test void lateAuditFailureDoesNotAdvanceExistingWatermarkOrRewritePriorEvent(){
        Long oldEventId=priorSavedV1Event();
        var priorEvent=jdbc.queryForMap("SELECT * FROM gxp_audit_event WHERE id=?",oldEventId);
        var priorSequence=jdbc.queryForMap("SELECT * FROM gxp_audit_ledger_sequence WHERE tenant_id=1");
        var priorSignature=jdbc.queryForMap("SELECT * FROM dcc_controlled_file_signature WHERE id=700");
        policy("dcc.controlled-file.standardization-effective","REQUIRED");
        jdbc.execute("ALTER TABLE gxp_audit_event ADD CONSTRAINT w06_effect_prior_fail CHECK(operation_id<>'dcc.controlled-file.standardization-effective')");
        try{var error=assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,this::effect);
            assertEquals(cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.GXP_AUDIT_APPEND_FAILED.getCode(),error.getCode());}
        finally{jdbc.execute("ALTER TABLE gxp_audit_event DROP CONSTRAINT w06_effect_prior_fail");}
        assertEquals(priorEvent,jdbc.queryForMap("SELECT * FROM gxp_audit_event WHERE id=?",oldEventId));
        assertEquals(priorSequence,jdbc.queryForMap("SELECT * FROM gxp_audit_ledger_sequence WHERE tenant_id=1"));
        assertEquals(priorSignature,jdbc.queryForMap("SELECT * FROM dcc_controlled_file_signature WHERE id=700"));
        assertNull(files.selectById(20L).getEffectiveTime());assertEquals(1,count());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_workflow_lifecycle_event",Integer.class));
    }
    private Long priorSavedV1Event(){
        policy("dcc.controlled-file.control","NOT_REQUIRED");
        String savedV1="{\"controlledFileId\":10,\"schemaVersion\":\"dcc.file-state.v1\",\"status\":\"ACTIVE\",\"versionNo\":\"A/1\"}";
        var state=cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditStateEnvelope.builder()
            .state("PRESENT").objectVersion("A/1").canonicalJson(savedV1).build();
        var command=cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditCommand.builder()
            .operationId("dcc.controlled-file.control").subjectId("CONTROLLED_FILE:10").subjectVersion("A/1")
            .reason("隔离fixture此前已保存的正式v1事件").beforeState(state).afterState(state)
            .idempotencyKey("IC09-W06-PRIOR-V1").requestId("old-round").source("IC09_W06_PRIVATE_FIXTURE").build();
        return tx.execute(s->ledger.append(command)).eventId();
    }
    private Map<?,?> facts(Map<String,Object> event,String column){return cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(event.get(column).toString(),Map.class);}
    private void policy(String operation,String signaturePolicy){jdbc.update("""
       INSERT INTO gxp_audit_policy_operation(tenant_id,policy_version,operation_id,source_type,source_locator,domain,subject_type,
        action_type,reason_policy,signature_policy,state_policy,retention_class,test_ids,owner,applicability,active)
       VALUES(1,'IC09-ISOLATED',?,'SERVICE_METHOD','DccWorkflowFileStateAudit','DCC','DCC_CONTROLLED_FILE','UPDATE','REQUIRED',?,
        'PRESENT_TO_PRESENT','GXP_CONTROLLED_DOCUMENT','IC09-W06','W06','GXP',TRUE)
       """,operation,signaturePolicy);}
    private void signature(){signatures.insert(DccControlledFileSignatureDO.builder().id(700L).controlledFileId(20L).revisionId(20L).versionNo("B/1")
        .processInstanceId("round-1").taskId("doc-task").actorId(88L).actionType("APPROVE").meaningCode("DOC_CONTROL_REVIEW_APPROVE")
        .signatureMode("PASSWORD").comment("正式文控审核通过意见")
        .passwordVerified(true).signedAt(signedAt).evidenceStatus("VALID").evidenceHash("actual-dcc-hmac").evidencePayloadVersion("v4-workflow").build());}
    private void effect(){tx.executeWithoutResult(s->lifecycle.recordStandardizationSignatureEffect(88L,20L,700L));}
    private int count(){return jdbc.queryForObject("SELECT COUNT(*) FROM gxp_audit_event",Integer.class);}
    private static void wire(Object bean,Object... fields){for(int i=0;i<fields.length;i+=2)ReflectionTestUtils.setField(bean,(String)fields[i],fields[i+1]);}
}
