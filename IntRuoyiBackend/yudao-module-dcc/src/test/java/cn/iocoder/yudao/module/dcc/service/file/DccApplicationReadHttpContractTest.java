package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.DccControlledFileController;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real Controller route/serialization contract; Query authorization/persistence has separate H2 coverage. */
class DccApplicationReadHttpContractTest extends BaseMockitoUnitTest {
    @Test void completeReworkSourceQueryUsesTheSameReadResourceAndForwardsOnlyExactOldTuple()throws Exception {
        var response=new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileObsoleteApplicationRespVO();
        response.setApplicationEntryMode("REWORK_SOURCE");response.setId(9223372036854775000L);response.setReworkSourceFormInstanceId(9223372036854775000L);
        var calls=new java.util.concurrent.atomic.AtomicInteger();
        var source=mock(DccControlledFileObsoleteService.class,invocation->{
            if(invocation.getMethod().getName().equals("getObsoleteReworkSource")) {
                org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(99L),invocation.getArgument(0,Long.class));
                org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(900L),invocation.getArgument(1,Long.class));
                var expected=invocation.getArgument(2);
                org.junit.jupiter.api.Assertions.assertEquals(9223372036854775000L,org.springframework.test.util.ReflectionTestUtils.getField(expected,"reworkSourceFormInstanceId"));
                org.junit.jupiter.api.Assertions.assertEquals("OLD-FORM",org.springframework.test.util.ReflectionTestUtils.getField(expected,"reworkSourceFormInstanceCode"));
                org.junit.jupiter.api.Assertions.assertEquals("old-bpm",org.springframework.test.util.ReflectionTestUtils.getField(expected,"reworkSourceBpmProcessInstanceId"));
                org.junit.jupiter.api.Assertions.assertEquals(1,org.springframework.test.util.ReflectionTestUtils.getField(expected,"reworkSourceAttributeRound"));calls.incrementAndGet();return response;
            }
            return org.mockito.Answers.RETURNS_DEFAULTS.answer(invocation);
        });
        org.springframework.test.util.ReflectionTestUtils.setField(controller,"obsoleteService",source);
        try(MockedStatic<SecurityFrameworkUtils> auth=mockStatic(SecurityFrameworkUtils.class)){
            auth.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            MockMvcBuilders.standaloneSetup(controller).build().perform(get("/dcc/controlled-files/900/obsolete-application-draft")
                .param("reworkSourceFormInstanceId","9223372036854775000").param("reworkSourceFormInstanceCode","OLD-FORM")
                .param("reworkSourceBpmProcessInstanceId","old-bpm").param("reworkSourceAttributeRound","1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.applicationEntryMode").value("REWORK_SOURCE"))
                .andExpect(jsonPath("$.data.reworkSourceFormInstanceId").value("9223372036854775000"));
        }
        org.junit.jupiter.api.Assertions.assertEquals(1,calls.get());verify(source,never()).getObsoleteApplicationDraft(any(),any());
    }
    @Test void incompleteReworkSourceReadQueryFailsBeforeEitherServiceBranch()throws Exception {
        MockMvcBuilders.standaloneSetup(controller).build().perform(get("/dcc/controlled-files/900/obsolete-application-draft")
            .param("reworkSourceFormInstanceId","5")).andExpect(status().isBadRequest());
        verifyNoInteractions(obsoleteService);
    }
    @Mock DccControlledFileQueryService queryService;
    @Mock DccTrainingApplicationProgressService trainingApplicationProgress;
    @Mock DccControlledFileObsoleteService obsoleteService;
    @InjectMocks DccControlledFileController controller;

    @Test void revisionOptionsUsesCurrentActorAndExactSelectedBaselineWithSafeLongResponse() throws Exception {
        var options=new DccControlledFileRevisionOptions(900L,"B/1",800L,"SOP.pdf",
                new DccControlledFileRevisionOptions.Target("B/2",null),
                new DccControlledFileRevisionOptions.Target("C/1",null),null,null,null,null,
                List.of(new DccControlledFileRevisionOptions.Iteration(9223372036854775000L,"B/1-2",true,false,false,null,null)));
        when(queryService.getRevisionOptions(99L,900L)).thenReturn(options);
        try(MockedStatic<SecurityFrameworkUtils> auth=mockStatic(SecurityFrameworkUtils.class)) {
            auth.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            MockMvcBuilders.standaloneSetup(controller).build().perform(get("/dcc/controlled-files/900/revision-options"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.controlledBaselineId").value("900"))
                    .andExpect(jsonPath("$.data.iterations[0].id").value("9223372036854775000"));
            verify(queryService).getRevisionOptions(99L,900L);
        }
    }

    @Test void frozenEvidenceReadsExplicitTypeAndOpaqueRoundWithoutBorrowingApplicant() throws Exception {
        var evidence=new DccControlledFileApplicationEvidence(900L,"B/1","OBSOLETE","real-obsolete-round",2,
                false,"NOT_RECORDED",null,null,List.of());
        when(queryService.getApplicationEvidence(99L,900L,"OBSOLETE","real-obsolete-round")).thenReturn(evidence);
        try(MockedStatic<SecurityFrameworkUtils> auth=mockStatic(SecurityFrameworkUtils.class)) {
            auth.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            MockMvcBuilders.standaloneSetup(controller).build().perform(get("/dcc/controlled-files/900/application-evidence")
                    .param("applicationType","OBSOLETE").param("bpmRound","real-obsolete-round"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.controlledFileId").value("900"))
                    .andExpect(jsonPath("$.data.bpmRound").value("real-obsolete-round"))
                    .andExpect(jsonPath("$.data.unavailableReason").value("NOT_RECORDED"))
                    .andExpect(jsonPath("$.data.recorded").value(false));
            verify(queryService).getApplicationEvidence(99L,900L,"OBSOLETE","real-obsolete-round");
        }
    }

    @Test void evidenceCannotCallQueryWhenTheFormalRoundIsOmitted() throws Exception {
        MockMvcBuilders.standaloneSetup(controller).build().perform(get("/dcc/controlled-files/900/application-evidence")
                .param("applicationType","REVISION"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(queryService);
    }

    @Test void roundListKeepsExactFlowIdentitiesAndUnavailableHistoryWithoutGuessingRules() throws Exception {
        var ready = round("UPLOAD", "current-public", 2);
        var old = round("UPLOAD", "older-public", 1);
        org.springframework.test.util.ReflectionTestUtils.setField(ready, "flowContract",
                new DccBoundWorkflowContractResolver.RoundFlowContract(1L,900L,"UPLOAD",2,"current-public",
                        "dcc-controlled-file-upload","upload:5:exact","deployment-5","a".repeat(64),
                        "PUBLIC_MATRIX_NATIVE_V1", DccWorkflowDefinitionCatalog.FlowRule.PUBLIC_V5_TRAINING_AFTER_DOCUMENT_CONTROL,
                        false,null,null,DccBoundWorkflowContractResolver.RoundContractStatus.READY,null));
        org.springframework.test.util.ReflectionTestUtils.setField(old, "flowContract",
                new DccBoundWorkflowContractResolver.RoundFlowContract(1L,900L,"UPLOAD",1,"older-public",
                        "dcc-controlled-file-upload","upload:5:older","deployment-older","b".repeat(64),
                        null,null,true,null,null,DccBoundWorkflowContractResolver.RoundContractStatus.NOT_RECORDED,
                        "HISTORICAL_CONTRACT_NOT_RECORDED"));
        when(queryService.listApplicationRounds(99L,900L)).thenReturn(List.of(ready,old));
        try (MockedStatic<SecurityFrameworkUtils> auth=mockStatic(SecurityFrameworkUtils.class)) {
            auth.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            MockMvcBuilders.standaloneSetup(controller).build().perform(get("/dcc/controlled-files/900/application-rounds"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].controlledFileId").value("900"))
                    .andExpect(jsonPath("$.data[0].bpmRound").value("current-public"))
                    .andExpect(jsonPath("$.data[0].flowContract.bpmRound").value("current-public"))
                    .andExpect(jsonPath("$.data[0].flowContract.processDefinitionId").value("upload:5:exact"))
                    .andExpect(jsonPath("$.data[0].flowContract.needTraining").value(false))
                    .andExpect(jsonPath("$.data[1].bpmRound").value("older-public"))
                    .andExpect(jsonPath("$.data[1].attributeRound").value(1))
                    .andExpect(jsonPath("$.data[1].flowContract.status").value("NOT_RECORDED"))
                    .andExpect(jsonPath("$.data[1].flowContract.unavailableReason").value("HISTORICAL_CONTRACT_NOT_RECORDED"))
                    .andExpect(jsonPath("$.data[1].flowContract.flowRule").value(org.hamcrest.Matchers.nullValue()))
                    .andExpect(jsonPath("$.data[1].flowContract.executionContract").value(org.hamcrest.Matchers.nullValue()));
            verify(queryService).listApplicationRounds(99L,900L);
            verifyNoInteractions(trainingApplicationProgress);
        }
    }

    @Test void exactObsoleteFlowMetadataSerializesLongIdentitiesAsStringsAndNoTraining() throws Exception {
        long precise=9223372036854775000L;
        var row=round("OBSOLETE","obsolete-exact",3);
        org.springframework.test.util.ReflectionTestUtils.setField(row,"flowContract",
                new DccBoundWorkflowContractResolver.RoundFlowContract(precise,900L,"OBSOLETE",3,"obsolete-exact",
                        "dcc-controlled-file-obsolete","obsolete:5:exact","deployment-obsolete","c".repeat(64),
                        "PUBLIC_MATRIX_NATIVE_V1",DccWorkflowDefinitionCatalog.FlowRule.PUBLIC_V5_OBSOLETE_APPROVAL_END,
                        null,precise,"FORM-EXACT",DccBoundWorkflowContractResolver.RoundContractStatus.READY,null));
        when(queryService.listApplicationRounds(99L,900L)).thenReturn(List.of(row));
        try (MockedStatic<SecurityFrameworkUtils> auth=mockStatic(SecurityFrameworkUtils.class)) {
            auth.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            MockMvcBuilders.standaloneSetup(controller).build().perform(get("/dcc/controlled-files/900/application-rounds"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].flowContract.tenantId").value(Long.toString(precise)))
                    .andExpect(jsonPath("$.data[0].flowContract.controlledFileId").value("900"))
                    .andExpect(jsonPath("$.data[0].flowContract.formInstanceId").value(Long.toString(precise)))
                    .andExpect(jsonPath("$.data[0].flowContract.formInstanceCode").value("FORM-EXACT"))
                    .andExpect(jsonPath("$.data[0].flowContract.needTraining").value(org.hamcrest.Matchers.nullValue()));
            verify(queryService).listApplicationRounds(99L,900L);
        }
    }

    private DccApplicationRoundSummary round(String type,String bpm,int number) {
        var result=new DccApplicationRoundSummary();
        result.setControlledFileId(900L);result.setApplicationType(type);
        result.setBpmRound(bpm);result.setAttributeRound(number);
        return result;
    }

    @Test void scopedTrainingProgressUsesLoginActorAndExactRoundWithoutBpmActionPayload() throws Exception {
        var flow=new DccBoundWorkflowContractResolver.RoundFlowContract(1L,900L,"REVISION",1,"training-T",
                "dcc-controlled-file-revision","revision:5:exact","deployment-training","d".repeat(64),
                "PUBLIC_MATRIX_NATIVE_V1",DccWorkflowDefinitionCatalog.FlowRule.PUBLIC_V5_TRAINING_AFTER_DOCUMENT_CONTROL,
                true,null,null,DccBoundWorkflowContractResolver.RoundContractStatus.READY,null);
        var result = new DccTrainingApplicationProgressService.Progress("900","REVISION","training-T",1,
                List.of(new DccTrainingApplicationProgressService.ProgressTask("review","training-T","MATRIX_REVIEW",2,"60","通过",null,null)),flow);
        when(trainingApplicationProgress.read(99L,900L,"training-T")).thenReturn(result);
        try (MockedStatic<SecurityFrameworkUtils> auth=mockStatic(SecurityFrameworkUtils.class)) {
            auth.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            MockMvcBuilders.standaloneSetup(controller).build().perform(get("/dcc/controlled-files/900/training-application-progress")
                            .param("bpmRound","training-T"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.controlledFileId").value("900"))
                    .andExpect(jsonPath("$.data.tasks[0].status").value(2))
                    .andExpect(jsonPath("$.data.flowContract.controlledFileId").value("900"))
                    .andExpect(jsonPath("$.data.flowContract.bpmRound").value("training-T"))
                    .andExpect(jsonPath("$.data.flowContract.processDefinitionId").value("revision:5:exact"))
                    .andExpect(jsonPath("$.data.flowContract.flowRule").value("PUBLIC_V5_TRAINING_AFTER_DOCUMENT_CONTROL"))
                    .andExpect(jsonPath("$.data.flowContract.needTraining").value(true));
            verify(trainingApplicationProgress).read(99L,900L,"training-T");
            verifyNoInteractions(queryService);
        }
    }
    @Test void obsoleteDraftReadUsesTheSameDccActorAndFileWithoutCallingAFormWrite()throws Exception{
        var draft=new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileObsoleteApplicationRespVO();
        draft.setId(9223372036854775000L);draft.setControlledFileId(900L);draft.setInstanceCode("ACTUAL-DRAFT-CODE");
        draft.setDccProjectCodeId(5L);draft.setVersionNo("A/1");draft.setAttributeRound(2);draft.setStatus("DRAFT");
        draft.setReason("本轮保存原因");draft.setIdempotencyKey("actual-key");draft.setSelectedSignoffDepartmentIds(List.of(51L));
        when(obsoleteService.getObsoleteApplicationDraft(99L,900L)).thenReturn(draft);
        try(MockedStatic<SecurityFrameworkUtils> auth=mockStatic(SecurityFrameworkUtils.class)){
            auth.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            MockMvcBuilders.standaloneSetup(controller).build().perform(get("/dcc/controlled-files/900/obsolete-application-draft"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value("9223372036854775000"))
                    .andExpect(jsonPath("$.data.controlledFileId").value("900"))
                    .andExpect(jsonPath("$.data.reason").value("本轮保存原因"))
                    .andExpect(jsonPath("$.data.selectedSignoffDepartmentIds[0]").value("51"));
            verify(obsoleteService).getObsoleteApplicationDraft(99L,900L);
            verify(obsoleteService,never()).obsoleteControlledFile(any(),any(),any());
            verifyNoInteractions(queryService,trainingApplicationProgress);
        }
    }
}
