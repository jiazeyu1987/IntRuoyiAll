package cn.iocoder.yudao.module.dcc.service.file;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSignatureMapper;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccSignatureVerifyRespVO;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Actual projection validation, with explicit signature verifier and official historic-task read ports. */
class DccPublicDocumentControlEvidenceTest {
    private DccControlledFileLifecycleService service;
    private DccControlledFileSignatureMapper signatures;
    private BpmTaskService tasks;
    private DccElectronicSignatureManagementService verifier;
    private DccControlledFileDO file;
    private DccControlledFileSignatureDO doc,owner;
    private DccBoundWorkflowContractResolver.BoundExecution bound;
    @BeforeEach void setup(){
        TenantContextHolder.setTenantId(1L);service=new DccControlledFileLifecycleService();
        signatures=mock(DccControlledFileSignatureMapper.class);tasks=mock(BpmTaskService.class);verifier=mock(DccElectronicSignatureManagementService.class);
        ReflectionTestUtils.setField(service,"approvalSignatureMapper",signatures);ReflectionTestUtils.setField(service,"bpmTaskService",tasks);
        ReflectionTestUtils.setField(service,"signatureManagementService",verifier);
        file=DccControlledFileDO.builder().id(20L).tenantId(1L).versionNo("B/1").processInstanceId("real-round")
                .processDefinitionKey(DccControlledFileProcessDefinitionKeys.UPLOAD).fileOwnerUserId(77L)
                .fileOwnerUsernameSnapshot("owner").fileOwnerNicknameSnapshot("Owner").fileOwnerSignatureId(701L)
                .fileOwnerApprovalTaskId("approval-task").fileOwnerProcessInstanceId("real-round")
                .fileOwnerSelectedTime(LocalDateTime.of(2026,10,12,1,0)).build();
        doc=signature(700L,"doc-task","DOC_CONTROL_REVIEW");owner=signature(701L,"approval-task","MATRIX_APPROVAL");
        owner.setComment("approved；批准文件负责人事实：[77,\"owner\",\"Owner\"]");
        when(signatures.selectListByControlledFileId(20L)).thenReturn(List.of(doc,owner));
        when(signatures.selectById(700L)).thenReturn(doc);when(signatures.selectById(701L)).thenReturn(owner);
        for(var sign:List.of(doc,owner)){
            var historic=mock(org.flowable.task.api.history.HistoricTaskInstance.class);
            when(historic.getId()).thenReturn(sign.getTaskId());when(historic.getProcessInstanceId()).thenReturn("real-round");
            when(historic.getProcessDefinitionId()).thenReturn("exact-id");when(historic.getTenantId()).thenReturn("1");
            when(historic.getTaskDefinitionKey()).thenReturn(sign==doc?"DOC_CONTROL_REVIEW":"MATRIX_APPROVAL");
            when(historic.getAssignee()).thenReturn("88");when(historic.getEndTime()).thenReturn(new Date());
            when(historic.getTaskLocalVariables()).thenReturn(Map.of(cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants.TASK_VARIABLE_STATUS,2));
            when(tasks.getHistoricTask(sign.getTaskId())).thenReturn(historic);
            var verification=new DccSignatureVerifyRespVO();verification.setSignatureId(sign.getId());verification.setVerificationStatus("VALID");
            when(verifier.verifySignatureEvidence(sign.getId())).thenReturn(verification);
        }
        bound=new DccBoundWorkflowContractResolver.BoundExecution(1L,20L,"real-round",7,file.getProcessDefinitionKey(),
                "exact-id","deployment","a".repeat(64),"PUBLIC_MATRIX_NATIVE_V1",DccWorkflowDefinitionCatalog.FlowRule.PUBLIC_V5_TRAINING_AFTER_DOCUMENT_CONTROL,true);
    }
    @AfterEach void cleanup(){TenantContextHolder.clear();}
    @Test void realDocAndSignedOwnerAreRequiredEvenWhenPresetHasNoSignatureEffectFields(){assertSame(doc,evidence());assertNull(file.getEffectiveTime());}
    @Test void wrongDocRoundCannotAuthorizeTraining(){doc.setProcessInstanceId("old-round");assertThrows(RuntimeException.class,this::evidence);}
    @Test void alteredOwnerProjectionCannotReplaceTheSignedOwner(){file.setFileOwnerUserId(78L);assertThrows(RuntimeException.class,this::evidence);}
    @Test void unfinishedDocHistoricTaskCannotAuthorizeTraining(){when(tasks.getHistoricTask("doc-task").getEndTime()).thenReturn(null);assertThrows(RuntimeException.class,this::evidence);}
    @Test void invalidDomainEvidenceCannotAuthorizeTraining(){var invalid=new DccSignatureVerifyRespVO();invalid.setSignatureId(700L);invalid.setVerificationStatus("INVALID");
        when(verifier.verifySignatureEvidence(700L)).thenReturn(invalid);assertThrows(RuntimeException.class,this::evidence);}
    private DccControlledFileSignatureDO evidence(){return ReflectionTestUtils.invokeMethod(service,"requirePublicDocumentControlEvidence",file,bound);}
    private DccControlledFileSignatureDO signature(Long id,String task,String stage){return DccControlledFileSignatureDO.builder().id(id)
            .controlledFileId(20L).revisionId(20L).versionNo("B/1").processInstanceId("real-round").taskId(task).actorId(88L)
            .actionType("APPROVE").meaningCode(stage+"_APPROVE").passwordVerified(true).signedAt(file.getFileOwnerSelectedTime())
            .evidenceStatus("VALID").evidenceHash("actual-verifier-port-proof").evidencePayloadVersion("v4-workflow").build();}
}
