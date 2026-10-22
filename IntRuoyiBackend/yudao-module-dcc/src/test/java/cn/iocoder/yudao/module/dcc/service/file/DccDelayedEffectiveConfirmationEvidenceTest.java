package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSignatureMapper;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccSignatureVerifyRespVO;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Calls the sole production verifier. External bound graph and cryptographic verification are explicit ports. */
class DccDelayedEffectiveConfirmationEvidenceTest {
    private Object service;
    private DccControlledFileDO file;
    private DccControlledFileSignatureDO signature;
    private DccControlledFileSignatureMapper signatures;
    private DccElectronicSignatureManagementService cryptographicVerification;
    private BpmTaskService tasks;
    private DccBoundWorkflowContractResolver bound;
    private final LocalDateTime selected = LocalDateTime.of(2026,10,16,12,0);
    private final LocalDateTime signed = LocalDateTime.of(2026,10,15,12,0,0,123456789);
    @BeforeEach void setup() throws Exception {
        TenantContextHolder.setTenantId(1L);
        service = Class.forName("cn.iocoder.yudao.module.dcc.service.file.DccDelayedEffectiveConfirmationEvidence").getConstructor().newInstance();
        file = DccControlledFileDO.builder().id(20L).tenantId(1L).masterId(3L).versionNo("B/1").requesterId(99L)
                .processInstanceId("round-2").processDefinitionKey(DccControlledFileProcessDefinitionKeys.REVISION).effectiveMode("DELAYED")
                .delayedEffectiveTime(selected).delayedEffectiveConfirmedAt(signed).delayedEffectiveSignatureId(700L).delayedEffectiveProcessInstanceId("round-2").build();
        signature = DccControlledFileSignatureDO.builder().id(700L).controlledFileId(20L).revisionId(20L).versionNo("B/1")
                .actorId(99L).taskId("confirm-task").processInstanceId("round-2").actionType("APPROVE").meaningCode("DELAYED_EFFECTIVE_CONFIRM_APPROVE")
                .passwordVerified(true).signedAt(signed).selectedEffectiveTime(selected).attributeRound(2).evidenceStatus("VALID")
                .evidenceHash("a".repeat(64)).evidencePayloadVersion("v5-delayed-effective").comment("真实衔接确认原因").build();
        signatures=mock(DccControlledFileSignatureMapper.class); when(signatures.selectById(700L)).thenAnswer(c->signature);
        cryptographicVerification=mock(DccElectronicSignatureManagementService.class);
        var response=new DccSignatureVerifyRespVO();response.setSignatureId(700L);response.setVerificationStatus("VALID");
        when(cryptographicVerification.verifySignatureEvidence(700L)).thenReturn(response);
        bound=mock(DccBoundWorkflowContractResolver.class);when(bound.resolve(file)).thenReturn(new DccBoundWorkflowContractResolver.BoundExecution(
                1L,20L,"round-2",2,DccControlledFileProcessDefinitionKeys.REVISION,"revision:6:exact","deployment","b".repeat(64),
                "PUBLIC_MATRIX_NATIVE_V2",DccWorkflowDefinitionCatalog.FlowRule.valueOf("PUBLIC_V6_DELAYED_EFFECTIVE_CONFIRM"),false));
        tasks=mock(BpmTaskService.class);var task=mock(HistoricTaskInstance.class);
        when(task.getId()).thenReturn("confirm-task");when(task.getProcessInstanceId()).thenReturn("round-2");when(task.getProcessDefinitionId()).thenReturn("revision:6:exact");
        when(task.getTenantId()).thenReturn("1");when(task.getTaskDefinitionKey()).thenReturn("DELAYED_EFFECTIVE_CONFIRM");when(task.getAssignee()).thenReturn("99");
        when(task.getEndTime()).thenReturn(new Date());when(task.getTaskLocalVariables()).thenReturn(Map.of("TASK_STATUS",2));
        when(tasks.getHistoricTask("confirm-task")).thenReturn(task);
        ReflectionTestUtils.setField(service,"signatureMapper",signatures);ReflectionTestUtils.setField(service,"signatureManagement",cryptographicVerification);
        ReflectionTestUtils.setField(service,"boundContracts",bound);ReflectionTestUtils.setField(service,"tasks",tasks);
    }
    @AfterEach void clear(){TenantContextHolder.clear();}
    private Object verify(){return ReflectionTestUtils.invokeMethod(service,"requireConfirmed",file);}
    @Test void realConfirmationReturnsOriginalFractionalSignedTimeWithoutUsingLaterSystemTime(){
        var result=verify();assertEquals(signed,ReflectionTestUtils.invokeMethod(result,"signedAt"));assertEquals(selected,ReflectionTestUtils.invokeMethod(result,"selectedEffectiveTime"));
        assertEquals(Long.valueOf(700L),(Long)ReflectionTestUtils.invokeMethod(result,"signatureId"));org.mockito.Mockito.verify(cryptographicVerification).verifySignatureEvidence(700L);
    }
    @Test void earlierChoiceIsRejectedWithoutRoundingTheSignature(){signature.setSelectedEffectiveTime(signed.withNano(0));file.setDelayedEffectiveTime(signed.withNano(0));assertThrows(RuntimeException.class,this::verify);}
    @Test void absentSignatureIsRejected(){file.setDelayedEffectiveSignatureId(null);assertThrows(RuntimeException.class,this::verify);}
    @Test void foreignActorCannotConfirmAsOriginalCompiler(){signature.setActorId(88L);assertThrows(RuntimeException.class,this::verify);}
    @Test void wrongRoundOrVersionCannotBorrowSignedConfirmation(){signature.setProcessInstanceId("old-round");assertThrows(RuntimeException.class,this::verify);signature.setProcessInstanceId("round-2");signature.setVersionNo("A/1");assertThrows(RuntimeException.class,this::verify);}
    @Test void storedTimeTamperingCannotBeAccepted(){file.setDelayedEffectiveTime(selected.plusSeconds(1));assertThrows(RuntimeException.class,this::verify);}
    @Test void invalidCryptographicEvidenceFailsClosed(){var invalid=new DccSignatureVerifyRespVO();invalid.setSignatureId(700L);invalid.setVerificationStatus("INVALID");when(cryptographicVerification.verifySignatureEvidence(700L)).thenReturn(invalid);assertThrows(RuntimeException.class,this::verify);}
    @Test void foreignTenantOrUnfinishedTaskCannotPass(){TenantContextHolder.setTenantId(2L);assertThrows(RuntimeException.class,this::verify);TenantContextHolder.setTenantId(1L);when(tasks.getHistoricTask("confirm-task")).thenReturn(null);assertThrows(RuntimeException.class,this::verify);}
    @Test void currentApprovedCommandRequiresExactSignedAuthorization(){
        when(tasks.getHistoricTask("confirm-task")).thenReturn(null);
        var task=mock(org.flowable.task.api.Task.class);when(tasks.getTask("confirm-task")).thenReturn(task);
        when(task.getId()).thenReturn("confirm-task");when(task.getProcessInstanceId()).thenReturn("round-2");when(task.getProcessDefinitionId()).thenReturn("revision:6:exact");
        when(task.getTenantId()).thenReturn("1");when(task.getTaskDefinitionKey()).thenReturn("DELAYED_EFFECTIVE_CONFIRM");when(task.getAssignee()).thenReturn("99");
        when(task.getTaskLocalVariables()).thenReturn(Map.of("TASK_STATUS",2));assertThrows(RuntimeException.class,this::verify);
        for(Object wrong:List.of(701L,"700",0L)){
            when(task.getTaskLocalVariables()).thenReturn(Map.of("TASK_STATUS",2,"dccVerifiedSignatureId",wrong,"dccVerifiedSignatureActorId",99L,"dccVerifiedSignatureAction","APPROVE"));
            assertThrows(RuntimeException.class,this::verify);
        }
        when(task.getTaskLocalVariables()).thenReturn(Map.of("TASK_STATUS",2,"dccVerifiedSignatureId",700L,"dccVerifiedSignatureActorId",88L,"dccVerifiedSignatureAction","APPROVE"));assertThrows(RuntimeException.class,this::verify);
        when(task.getTaskLocalVariables()).thenReturn(Map.of("TASK_STATUS",2,"dccVerifiedSignatureId",700L,"dccVerifiedSignatureActorId",99L,"dccVerifiedSignatureAction","REJECT"));assertThrows(RuntimeException.class,this::verify);
        when(task.getTaskLocalVariables()).thenReturn(Map.of("TASK_STATUS",1,"dccVerifiedSignatureId",700L,"dccVerifiedSignatureActorId",99L,"dccVerifiedSignatureAction","APPROVE"));assertThrows(RuntimeException.class,this::verify);
        assertNotNull(ReflectionTestUtils.invokeMethod(service,"requireSignedForActiveTask",file,bound.resolve(file),700L));
        when(task.getTaskLocalVariables()).thenReturn(Map.of("TASK_STATUS",2,"dccVerifiedSignatureId",700L,"dccVerifiedSignatureActorId",99L,"dccVerifiedSignatureAction","APPROVE"));assertNotNull(verify());
    }
}
