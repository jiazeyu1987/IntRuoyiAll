package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@MockitoSettings(strictness = Strictness.LENIENT)
class DccWorkflowObsoleteEvidenceGuardTest extends BaseMockitoUnitTest {
    @Mock private DccControlledFileTaskAssigneeSnapshotMapper taskSnapshotMapper;
    @Mock private DccControlledFileSignatureMapper signatureMapper;
    @Mock private DccElectronicSignatureManagementService signatureManagementService;
    @Mock private BpmTaskService bpmTaskService;
    @InjectMocks private DccWorkflowObsoleteEvidenceGuard guard;
    private DccControlledFileDO file;
    private List<DccControlledFileTaskAssigneeSnapshotDO> roster;
    private List<DccControlledFileSignatureDO> signatures;
    private Map<String,Object> variables;
    @BeforeEach void fixture() {
        TenantContextHolder.setTenantId(1L);
        file=DccControlledFileDO.builder().id(42L).versionNo("A/1").build();
        roster=new ArrayList<>();signatures=new ArrayList<>();
        for(int index=1;index<=2;index++) {
            roster.add(DccControlledFileTaskAssigneeSnapshotDO.builder().controlledFileId(42L).tenantId(1L)
                    .processInstanceId("obsolete-round").stageCode("MATRIX_REVIEW").obligationId("department-"+index)
                    .departmentId(50L+index).leaderUserId(99L).assigneeUserId(99L).bpmTaskId("matrix-task-"+index)
                    .assignmentSignatureId(100L+index).assignedTime(LocalDateTime.now()).build());
            signatures.add(signature(100L+index,"matrix-task-"+index,99L,"ASSIGN","MATRIX_REVIEW_ASSIGN"));
            signatures.add(signature(200L+index,"matrix-task-"+index,99L,"APPROVE","MATRIX_REVIEW_APPROVE"));
            var finished=task("matrix-task-"+index,"MATRIX_REVIEW",99L);
            when(bpmTaskService.getHistoricTask("matrix-task-"+index)).thenReturn(finished);
        }
        signatures.add(signature(300L,"approval-task",100L,"APPROVE","MATRIX_APPROVAL_APPROVE"));
        var approved=task("approval-task","MATRIX_APPROVAL",100L);
        when(bpmTaskService.getHistoricTask("approval-task")).thenReturn(approved);
        when(taskSnapshotMapper.selectListByControlledFileId(42L)).thenAnswer(ignored->roster);
        when(signatureMapper.selectListByControlledFileId(42L)).thenAnswer(ignored->signatures);
        when(signatureManagementService.verifySignatureEvidence(anyLong())).thenAnswer(call->{
            var verified=new cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccSignatureVerifyRespVO();
            verified.setSignatureId(call.getArgument(0));verified.setVerificationStatus("VALID");return verified;
        });
        variables=Map.of(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_DCC_TASK_OBLIGATION_IDS,
                Map.of("MATRIX_REVIEW",List.of("department-1","department-2")),
                BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_START_USER_SELECT_ASSIGNEES,Map.of("MATRIX_REVIEW",List.of(99L,99L)),
                BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_APPROVE_USER_SELECT_ASSIGNEES,Map.of("MATRIX_APPROVAL",List.of(100L)),
                BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_APPROVER_USER_ID,100L);
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }
    @Test void sameLeaderMustHaveTwoAssignmentsTwoSignoffsAndOneConfiguredApproval() {
        assertEquals(100L,guard.require(file,"obsolete-round",variables));
        verify(signatureManagementService,times(5)).verifySignatureEvidence(anyLong());
        signatures.removeIf(sig->sig.getId().equals(202L));
        assertThrows(RuntimeException.class,()->guard.require(file,"obsolete-round",variables));
    }
    @Test void missingAssignmentEvidenceCannotBeReplacedByACompletedSignoff() {
        signatures.removeIf(sig->sig.getActionType().equals("ASSIGN"));
        assertThrows(RuntimeException.class,()->guard.require(file,"obsolete-round",variables));
    }
    @Test void wrongRoundVersionSignerAndRejectedResultCannotCount() {
        var signed=signatures.get(1);
        signed.setProcessInstanceId("previous-round");assertThrows(RuntimeException.class,()->guard.require(file,"obsolete-round",variables));
        signed.setProcessInstanceId("obsolete-round");signed.setVersionNo("B/1");assertThrows(RuntimeException.class,()->guard.require(file,"obsolete-round",variables));
        signed.setVersionNo("A/1");signed.setActorId(101L);assertThrows(RuntimeException.class,()->guard.require(file,"obsolete-round",variables));
        signed.setActorId(99L);signed.setActionType("REJECT");assertThrows(RuntimeException.class,()->guard.require(file,"obsolete-round",variables));
    }
    @Test void cryptographicVerificationFailureIsNotReplacedByTheStoredValidFlag() {
        var invalid=new cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccSignatureVerifyRespVO();
        invalid.setSignatureId(101L);invalid.setVerificationStatus("INVALID");
        when(signatureManagementService.verifySignatureEvidence(101L)).thenReturn(invalid);
        assertThrows(RuntimeException.class,()->guard.require(file,"obsolete-round",variables));
    }
    @Test void completedTaskFromAnotherTenantOrRoundCannotAuthorizeAnObsoleteFact() {
        var task=task("matrix-task-1","MATRIX_REVIEW",99L);when(task.getTenantId()).thenReturn("122");
        when(bpmTaskService.getHistoricTask("matrix-task-1")).thenReturn(task);
        assertThrows(RuntimeException.class,()->guard.require(file,"obsolete-round",variables));
        when(task.getTenantId()).thenReturn("1");when(task.getProcessInstanceId()).thenReturn("another-round");
        assertThrows(RuntimeException.class,()->guard.require(file,"obsolete-round",variables));
    }
    @Test void oneDepartmentCannotReuseTheOtherDepartmentsActualTask() {
        roster.get(1).setBpmTaskId("matrix-task-1");
        assertThrows(RuntimeException.class,()->guard.require(file,"obsolete-round",variables));
    }
    @Test void approvalEvidenceMustBelongToTheActualFinalApprover() {
        var inconsistent=new HashMap<>(variables);
        inconsistent.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_APPROVER_USER_ID,99L);
        assertThrows(RuntimeException.class,()->guard.require(file,"obsolete-round",inconsistent));
    }
    @Test void missingFinalApproverCannotBeInferredFromConfiguredCandidates() {
        var incomplete=new HashMap<>(variables);
        incomplete.remove(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_APPROVER_USER_ID);
        assertThrows(RuntimeException.class,()->guard.require(file,"obsolete-round",incomplete));
    }
    private DccControlledFileSignatureDO signature(long id,String task,long actor,String action,String meaning) {
        return DccControlledFileSignatureDO.builder().id(id).controlledFileId(42L).revisionId(42L).versionNo("A/1")
                .processInstanceId("obsolete-round").taskId(task).actorId(actor).actionType(action).meaningCode(meaning)
                .evidencePayloadVersion("v4-workflow").evidenceHash("domain-hmac").evidenceStatus("VALID")
                .passwordVerified(true).signedAt(LocalDateTime.now()).build();
    }
    private HistoricTaskInstance task(String id,String stage,long actor) {
        var task=mock(HistoricTaskInstance.class);when(task.getId()).thenReturn(id);when(task.getProcessInstanceId()).thenReturn("obsolete-round");
        when(task.getTenantId()).thenReturn("1");when(task.getAssignee()).thenReturn(String.valueOf(actor));
        when(task.getTaskDefinitionKey()).thenReturn(stage);when(task.getEndTime()).thenReturn(new Date());
        when(task.getTaskLocalVariables()).thenReturn(Map.of(BpmnVariableConstants.TASK_VARIABLE_STATUS,2));return task;
    }
}
