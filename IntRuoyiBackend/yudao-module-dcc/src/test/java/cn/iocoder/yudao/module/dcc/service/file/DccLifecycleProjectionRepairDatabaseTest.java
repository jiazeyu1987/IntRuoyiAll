package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccSignatureVerifyRespVO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.permission.dto.RoleRespDTO;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditPolicyOperationDO;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import cn.iocoder.yudao.module.bpm.service.task.*;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Repair uses the real Gxp kernel and core/H2; source directory and formal signature verification are explicit ports. */
@Import(GxpAuditServiceImpl.class)
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(scripts="/sql/dcc_b_gxp_audit_tables.sql",executionPhase=Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts={"/sql/clean.sql","/sql/dcc_b_gxp_audit_clean.sql"},executionPhase=Sql.ExecutionPhase.AFTER_TEST_METHOD)
class DccLifecycleProjectionRepairDatabaseTest extends DccNativePlatformLifecycleTransactionTest {
    @Resource DccControlledFileMapper files;
    @Resource DccControlledFileMasterMapper masters;
    @Resource DccControlledFileRouteSnapshotMapper routes;
    @Resource DccControlledFileTaskAssigneeSnapshotMapper assignments;
    @Resource DccControlledFileSignatureMapper signatures;
    @Resource PlatformTransactionManager transactions;
    @Resource GxpAuditEventMapper events;
    @Resource GxpAuditPolicyOperationMapper policies;
    @MockitoBean AdminUserApi users;
    @MockitoSpyBean GxpAuditServiceImpl actualAudit;
    private DccLifecycleProjectionRepairService repair;
    private org.springframework.jdbc.core.JdbcTemplate repairJdbc;
    private DccElectronicSignatureManagementService verification;
    private PermissionApi permissions;

    @BeforeEach void actualSavedControlDrift() {
        repairJdbc=(org.springframework.jdbc.core.JdbcTemplate)ReflectionTestUtils.getField(this,"actualJdbc");
        ReflectionTestUtils.invokeMethod(this,"seedBoth",LocalDate.now());
        ReflectionTestUtils.invokeMethod(this,"control");
        repairJdbc.update("UPDATE controlled_content_version_ref SET canonical_status='FINALIZING',domain_status='FINALIZING',open_candidate_unique_flag=1,active_unique_flag=NULL WHERE native_version_id=2");
        repairJdbc.update("UPDATE dcc_controlled_file_master SET dcc_project_code_id=5 WHERE id=10");
        repairJdbc.update("UPDATE dcc_controlled_file SET dcc_project_code_id=5,process_definition_key='dcc-controlled-file-upload',approved_time=controlled_time WHERE id=2");
        var account=new AdminUserRespDTO().setId(99L).setTenantId(1L).setStatus(0).setUsername("repair-user").setNickname("真实维护人");
        when(users.getUser(99L)).thenReturn(account);
        var login=new LoginUser().setId(99L).setTenantId(1L).setUserType(2).setInfo(Map.of("username","repair-user","nickname","真实维护人"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login,null,List.of()));
        permissions=mock(PermissionApi.class);when(permissions.hasAnyPermissions(eq(99L),any())).thenReturn(true);
        when(permissions.getUserRoleIdListByUserId(99L)).thenReturn(Set.of(7L));
        var roleApi=mock(RoleApi.class);var role=new RoleRespDTO();role.setId(7L);role.setCode("doc_control");role.setStatus(0);
        when(roleApi.getRoleByCode("doc_control")).thenReturn(role);
        var scope=mock(DccControlledFileAssignmentScopeService.class);when(scope.isWithinAssignedFileScope(99L,2L)).thenReturn(true);
        var processes=mock(BpmProcessInstanceService.class);var history=mock(org.flowable.engine.history.HistoricProcessInstance.class);
        when(history.getId()).thenReturn("approval-2");when(history.getBusinessKey()).thenReturn("2");when(history.getTenantId()).thenReturn("1");
        when(history.getStartUserId()).thenReturn("99");when(history.getProcessDefinitionKey()).thenReturn("dcc-controlled-file-upload");
        when(history.getEndTime()).thenReturn(new Date());when(history.getProcessDefinitionId()).thenReturn("isolated-approved-model");
        when(history.getProcessVariables()).thenReturn(Map.of("PROCESS_STATUS",2));when(processes.getHistoricProcessInstance("approval-2")).thenReturn(history);
        var taskApi=mock(BpmTaskService.class);verification=mock(DccElectronicSignatureManagementService.class);
        for(int index=0;index<3;index++) {
            String stage=List.of("MATRIX_REVIEW","MATRIX_APPROVAL","DOC_CONTROL_REVIEW").get(index);long id=100+index;
            routes.insert(cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRouteSnapshotDO.builder()
                    .controlledFileId(2L).routeVersionNo(1).stageName(stage).stageNo(index+1).stageOrder(index+1).stageCode(stage).resolvedUserIds("99")
                    .candidateSourceType(index==0?"DEPT":"USER").candidateSourceIds(index==0?"100":null).approveMethod("ALL").requireAllApprovals(true).build());
            repairJdbc.update("INSERT INTO dcc_controlled_file_signature(id,controlled_file_id,revision_id,version_no,task_id,process_instance_id,actor_id,action_type,meaning_code,signature_mode,password_verified,signed_at,evidence_hash,evidence_status,tenant_id) VALUES(?,2,2,'B/1',?,'approval-2',99,'APPROVE',?,'ISOLATED',1,CURRENT_TIMESTAMP,'isolated-port-evidence','VALID',1)",id,"signed-"+id,stage+"_APPROVE");
            var checked=new DccSignatureVerifyRespVO();checked.setSignatureId(id);checked.setVerificationStatus("VALID");
            when(verification.verifySignatureEvidence(id)).thenReturn(checked);
            var task=mock(org.flowable.task.api.history.HistoricTaskInstance.class);when(task.getEndTime()).thenReturn(new Date());
            when(task.getId()).thenReturn("signed-"+id);when(task.getTaskDefinitionKey()).thenReturn(stage);
            when(task.getProcessInstanceId()).thenReturn("approval-2");when(task.getTenantId()).thenReturn("1");when(task.getAssignee()).thenReturn("99");
            when(task.getTaskLocalVariables()).thenReturn(Map.of("TASK_STATUS",2));when(taskApi.getHistoricTask("signed-"+id)).thenReturn(task);
        }
        jdbcAssignmentFixture();
        repairJdbc.update("UPDATE dcc_controlled_file_signature SET evidence_payload_version='v4-workflow' WHERE controlled_file_id=2");
        var storage=mock(FileService.class);when(storage.getFile(100L)).thenReturn(FileDO.builder().id(100L).configId(1L).path("isolated/controlled").size(4L).build());
        try {when(storage.getFileContent(1L,"isolated/controlled")).thenReturn(new byte[]{1,2,3,4});}catch(Exception e){throw new IllegalStateException(e);}
        repair=new DccLifecycleProjectionRepairService();
        wire(repair,"files",files,"masters",masters,"routes",routes,"assignments",assignments,"signatures",signatures,
                "refs",refs,"core",ReflectionTestUtils.getField(this,"core"),"scope",scope,"versionPolicy",DccControlledFileVersionPolicy.defaultPolicy(),"users",users,"roles",roleApi,
                "permissions",permissions,"processes",processes,"tasks",taskApi,"verification",verification,"storage",storage,
                "jdbc",repairJdbc,"audit",actualAudit,"auditEvents",events);
        var policy=new GxpAuditPolicyOperationDO();policy.setTenantId(1L);policy.setOperationId(DccLifecycleProjectionRepairService.OPERATION);
        policy.setPolicyVersion("G59_ISOLATED");policy.setSourceType("SERVICE_METHOD");policy.setSourceLocator("G59_REPAIR");policy.setDomain("DCC");
        policy.setSubjectType("DCC_CONTROLLED_FILE");policy.setActionType("UPDATE");policy.setReasonPolicy("REQUIRED");policy.setSignaturePolicy("NOT_REQUIRED");
        policy.setStatePolicy("BEFORE_AFTER");policy.setRetentionClass("ISOLATED");policy.setTestIds("G59");policy.setOwner("TEST");policy.setApplicability("GXP");policy.setActive(true);policies.insert(policy);
    }

    private void jdbcAssignmentFixture() {
        repairJdbc.update("INSERT INTO dcc_controlled_file_signature(id,controlled_file_id,revision_id,version_no,task_id,process_instance_id,actor_id,action_type,meaning_code,signature_mode,password_verified,signed_at,evidence_hash,evidence_status,tenant_id) VALUES(103,2,2,'B/1','signed-100','approval-2',99,'ASSIGN','MATRIX_REVIEW_ASSIGN','ISOLATED',1,CURRENT_TIMESTAMP,'isolated-assignment-port-evidence','VALID',1)");
        assignments.insert(cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTaskAssigneeSnapshotDO.builder()
                .controlledFileId(2L).stageCode("MATRIX_REVIEW").stageNo(1).assigneeUserId(99L).leaderUserId(99L)
                .departmentId(100L).processInstanceId("approval-2").obligationId("isolated-obligation-100")
                .assignmentSignatureId(103L).assignedTime(LocalDateTime.now()).bpmTaskId("signed-100").tenantId(1L).build());
        var checked=new DccSignatureVerifyRespVO();checked.setSignatureId(103L);checked.setVerificationStatus("VALID");
        when(verification.verifySignatureEvidence(103L)).thenReturn(checked);
    }

    @AfterEach void clearLogin(){SecurityContextHolder.clearContext();}

    private static void wire(Object target,Object... fields){for(int i=0;i<fields.length;i+=2)ReflectionTestUtils.setField(target,(String)fields[i],fields[i+1]);}
    private DccLifecycleProjectionRepairReqVO request() {
        var preview=repair.preview(99L,2L);assertTrue(preview.canRepair());var request=new DccLifecycleProjectionRepairReqVO();
        request.setMasterId(Long.valueOf(preview.masterId()));request.setVersionRefId(Long.valueOf(preview.versionRefId()));
        request.setProcessInstanceId(preview.processInstanceId());request.setExpectedCanonicalStatus("FINALIZING");request.setSourceFactsHash(preview.sourceFactsHash());
        request.setPreimageHash(preview.preimageHash());request.setReason("正式核验既有受控事件后修复平台投影");request.setIdempotencyKey("g59-task-owned-repair");return request;
    }

    @Test void explicitRepairCommitsOfficialAuditAndExactReplayWritesNothingElse() {
        var source=repairJdbc.queryForMap("SELECT * FROM dcc_controlled_file WHERE id=2");var request=request();
        var receipt=new TransactionTemplate(transactions).execute(s->repair.repair(99L,2L,request));assertEquals("REPAIRED",receipt.status());assertEquals("ACTIVE",receipt.canonicalStatus());
        assertNotNull(receipt.auditEventId());assertNotNull(receipt.repairedAt());assertEquals(source,repairJdbc.queryForMap("SELECT * FROM dcc_controlled_file WHERE id=2"));
        int count=repairJdbc.queryForObject("SELECT COUNT(*) FROM controlled_content_transition_audit",Integer.class);
        var replay=new TransactionTemplate(transactions).execute(s->repair.repair(99L,2L,request));assertEquals("REPLAY",replay.status());
        assertEquals(receipt.auditEventId(),replay.auditEventId());assertEquals(count,repairJdbc.queryForObject("SELECT COUNT(*) FROM controlled_content_transition_audit",Integer.class));
        assertEquals(1,events.selectList().size());assertFalse(repair.preview(99L,2L).canRepair());
    }

    @Test void missingPolicyAndLateOfficialAuditFailureRollbackProjectionAndTransitions() {
        var request=request();int count=repairJdbc.queryForObject("SELECT COUNT(*) FROM controlled_content_transition_audit",Integer.class);
        policies.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GxpAuditPolicyOperationDO>().eq(GxpAuditPolicyOperationDO::getOperationId,DccLifecycleProjectionRepairService.OPERATION));
        assertThrows(RuntimeException.class,()->new TransactionTemplate(transactions).execute(s->repair.repair(99L,2L,request)));
        assertEquals("FINALIZING",refs.selectByNativeVersion(1L,"DCC_CONTROLLED_FILE","10",2L).getCanonicalStatus());
        assertEquals(count,repairJdbc.queryForObject("SELECT COUNT(*) FROM controlled_content_transition_audit",Integer.class));assertTrue(events.selectList().isEmpty());
    }

    @Test void changedPreimageInvalidSignatureAndWrongActorCannotRepair() {
        var request=request();request.setPreimageHash("0".repeat(64));
        assertEquals("DCC_REPAIR_PREIMAGE_CHANGED",assertThrows(IllegalStateException.class,()->new TransactionTemplate(transactions).execute(s->repair.repair(99L,2L,request))).getMessage());
        assertThrows(IllegalStateException.class,()->repair.preview(88L,2L));
        var invalid=new DccSignatureVerifyRespVO();invalid.setSignatureId(100L);invalid.setVerificationStatus("INVALID");when(verification.verifySignatureEvidence(100L)).thenReturn(invalid);
        assertEquals("DCC_REPAIR_SIGNATURE_INVALID",assertThrows(IllegalStateException.class,()->repair.preview(99L,2L)).getMessage());
        assertEquals("FINALIZING",refs.selectByNativeVersion(1L,"DCC_CONTROLLED_FILE","10",2L).getCanonicalStatus());assertTrue(events.selectList().isEmpty());
    }

    @Test void failureAfterActualGxpInsertRollsBackProjectionAndLedgerTogether() {
        var request=request();int before=repairJdbc.queryForObject("SELECT COUNT(*) FROM controlled_content_transition_audit",Integer.class);
        doAnswer(call->{call.callRealMethod();throw new IllegalStateException("ISOLATED_LATE_REAL_GXP_FAILURE");}).when(actualAudit).append(any());
        assertThrows(IllegalStateException.class,()->new TransactionTemplate(transactions).execute(s->repair.repair(99L,2L,request)));
        assertEquals("FINALIZING",refs.selectByNativeVersion(1L,"DCC_CONTROLLED_FILE","10",2L).getCanonicalStatus());
        assertEquals(before,repairJdbc.queryForObject("SELECT COUNT(*) FROM controlled_content_transition_audit",Integer.class));assertTrue(events.selectList().isEmpty());
    }

    @Test void foreignTenantStageSignatureCannotCompleteTheExactTenantRepairRoster() {
        repairJdbc.update("UPDATE dcc_controlled_file_signature SET tenant_id=2 WHERE id=101");
        assertThrows(RuntimeException.class,()->repair.preview(99L,2L));
        assertTrue(events.selectList().isEmpty());
        assertEquals("FINALIZING",refs.selectByNativeVersion(1L,"DCC_CONTROLLED_FILE","10",2L).getCanonicalStatus());
    }

    @Test void corruptedLifecycleEventIdentityCannotAuthorizeRepairOfTheSameFileAndRound() {
        repairJdbc.update("UPDATE dcc_workflow_lifecycle_event SET master_id=999,version_no='Z/9' WHERE controlled_file_id=2 AND event_type='CONTROLLED'");
        assertThrows(IllegalStateException.class,()->repair.preview(99L,2L));
        assertTrue(events.selectList().isEmpty());
        assertEquals("FINALIZING",refs.selectByNativeVersion(1L,"DCC_CONTROLLED_FILE","10",2L).getCanonicalStatus());
    }
}
