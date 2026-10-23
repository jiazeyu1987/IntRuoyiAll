package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectApplicationAttributesDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.service.file.relations.*;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Query wiring decisions; relation payload and authority are explicit ports, not a substitute for H2 reader verification. */
class DccW08ApplicationRelationEvidenceTest {
    DccControlledFileQueryServiceImpl query;
    DccControlledFileApplicationEvidenceMapper evidence;
    DccControlledFileDetailAuthorizationGuard detail;
    DccApplicationHistoryGuard history;
    DccRelationStageSnapshotService stages;
    DccControlledFileDO file;
    DccProjectApplicationAttributesDO attributes;

    @BeforeEach void setup() throws Exception {
        TenantContextHolder.setTenantId(1L);query=new DccControlledFileQueryServiceImpl();
        for(var field:query.getClass().getDeclaredFields())if(!Modifier.isStatic(field.getModifiers())&&field.getAnnotation(Resource.class)!=null)
            ReflectionTestUtils.setField(query,field.getName(),mock(field.getType()));
        file=DccControlledFileDO.builder().id(23L).tenantId(1L).masterId(12L).dccProjectCodeId(5L).categoryId(2L)
                .requesterId(88L).versionNo("A/1").status("ACTIVE").build();
        DccControlledFileMapper files=port("controlledFileMapper");when(files.selectById(23L)).thenReturn(file);
        detail=port("detailAuthorizationGuard");when(detail.isAllowed(eq(99L),eq(file),anyBoolean(),any())).thenReturn(true);
        evidence=port("applicationEvidenceMapper");history=port("applicationHistoryGuard");stages=port("relationStageSnapshotService");
        when(evidence.selectBoundRounds(1L,5L,"REVISION",23L,"actual-bpm")).thenReturn(List.of(7));
        attributes=new DccProjectApplicationAttributesDO();attributes.setTenantId(1L);attributes.setProjectCodeId(5L);
        attributes.setApplicationType("REVISION");attributes.setApplicationId(23L);attributes.setApplicationRound(7);attributes.setSubmitted(true);
        attributes.setDefaultSourceJson("{\"targetMarkets\":[\"CE\"],\"licenseHolder\":\"Y\",\"actualManufacturer\":\"N\",\"documentTransfer\":\"N\"}");
        attributes.setActualAttributesJson("{\"targetMarkets\":[\"FDA\"],\"licenseHolder\":\"N\",\"actualManufacturer\":\"Y\",\"documentTransfer\":\"N\"}");
        when(evidence.selectSnapshot(1L,5L,"REVISION",23L,7)).thenReturn(attributes);
    }
    @AfterEach void clear(){TenantContextHolder.clear();}
    @SuppressWarnings("unchecked")private <T>T port(String name){return (T)ReflectionTestUtils.getField(query,name);}
    private DccControlledFileApplicationEvidence read(){return query.getApplicationEvidence(99L,23L,"REVISION","actual-bpm");}

    @Test void authorizedBoundFrozenReadReturnsExactPayloadAfterHistoryAndSnapshotValidation() {
        var relations=new DccRelationRoundEvidence(1L,23L,12L,5L,"REVISION",7,"actual-bpm",false,
                "本轮没有签名节点",List.of(),List.of(),List.of(),"a".repeat(64));
        when(stages.readRoundEvidence(99L,23L,"REVISION","actual-bpm")).thenReturn(relations);
        var result=read();assertTrue(result.recorded());assertSame(relations,result.relationEvidence());
        var order=inOrder(detail,evidence,history,stages);
        order.verify(detail).isAllowed(eq(99L),eq(file),anyBoolean(),any());
        order.verify(evidence).selectBoundRounds(1L,5L,"REVISION",23L,"actual-bpm");
        order.verify(history).require(file,"REVISION","actual-bpm");
        order.verify(evidence).selectSnapshot(1L,5L,"REVISION",23L,7);
        order.verify(stages).readRoundEvidence(99L,23L,"REVISION","actual-bpm");
    }
    @Test void absentMappingKeepsExplicitAbsenceWithoutRelationQuery() {
        when(evidence.selectBoundRounds(1L,5L,"REVISION",23L,"actual-bpm")).thenReturn(List.of());
        var result=read();assertEquals("NOT_RECORDED",result.unavailableReason());assertNull(result.relationEvidence());
        verifyNoInteractions(stages,history);verify(evidence,never()).selectSnapshot(anyLong(),anyLong(),anyString(),anyLong(),anyInt());
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"NO_ATTRIBUTES","DRAFT","BAD_SNAPSHOT","BAD_JSON","HISTORY_REFUSED","DENIED"})
    void incompleteOrUnauthorizedContextNeverInvokesStageReader(String invalid) {
        switch(invalid){
            case "NO_ATTRIBUTES" -> when(evidence.selectSnapshot(1L,5L,"REVISION",23L,7)).thenReturn(null);
            case "DRAFT" -> attributes.setSubmitted(false);
            case "BAD_SNAPSHOT" -> attributes.setApplicationId(24L);
            case "BAD_JSON" -> attributes.setActualAttributesJson("{}");
            case "HISTORY_REFUSED" -> doThrow(new IllegalStateException("actual BPM refused")).when(history).require(file,"REVISION","actual-bpm");
            case "DENIED" -> when(detail.isAllowed(eq(99L),eq(file),anyBoolean(),any())).thenReturn(false);
        }
        if("NO_ATTRIBUTES".equals(invalid)||"DRAFT".equals(invalid))assertNull(read().relationEvidence());
        else assertThrows(RuntimeException.class,this::read);
        verifyNoInteractions(stages);
    }
    @Test void relationPortSchemaFailurePropagatesAndNullIsNotSuccessfulEvidence() {
        when(stages.readRoundEvidence(99L,23L,"REVISION","actual-bpm")).thenThrow(new IllegalStateException("real stage table missing"));
        assertEquals("real stage table missing",assertThrows(IllegalStateException.class,this::read).getMessage());
        doReturn(null).when(stages).readRoundEvidence(99L,23L,"REVISION","actual-bpm");
        assertThrows(NullPointerException.class,this::read);
    }

    @Test void currentTrainingOwnerReadsApplicationEvidenceThroughRealMetadataNamePolicies() throws Exception {
        var training=installCurrentTrainingOwnerPolicies();
        assertTrue(training.canOrganizeTraining(99L,file));assertFalse(training.canUpload(99L,file));
        assertTrue(detail.isAllowed(99L,file,false,null));
        var result=read();assertTrue(result.recorded());assertEquals(23L,result.relationEvidence().fileId());
        assertEquals("actual-bpm",result.relationEvidence().bpmRound());
        assertFalse(query.canViewFileName(99L,file),"Duty source-name access must not grant global browse visibility");
        verify(stages).readRoundEvidence(99L,23L,"REVISION","actual-bpm");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"NOT_OWNER","OWNER_ROUND","CURRENT_ROUND","ENDED","NO_SIGNATURE","OUTSIDE_SCOPE","NON_NATIVE"})
    void trainingNameMetadataRejectsInvalidDutyWithoutBorrowingAnotherGrant(String invalid) throws Exception {
        var training=installCurrentTrainingOwnerPolicies();
        switch(invalid) {
            case "NOT_OWNER" -> file.setFileOwnerUserId(77L);
            case "OWNER_ROUND" -> file.setFileOwnerProcessInstanceId("other-round");
            case "CURRENT_ROUND" -> file.setProcessInstanceId("other-round");
            case "ENDED" -> file.setStatus("READY_TO_PUBLISH");
            case "NO_SIGNATURE" -> file.setFileOwnerSignatureId(null);
            case "OUTSIDE_SCOPE" -> {
                DccControlledFileAssignmentScopeService scope=port("assignmentScopeService");
                when(scope.isWithinAssignedFileScope(99L,23L)).thenReturn(false);
            }
            case "NON_NATIVE" -> file.setProcessDefinitionKey(DccControlledFileProcessDefinitionKeys.LEGACY_APPROVAL);
        }
        if(!"OUTSIDE_SCOPE".equals(invalid)) assertFalse(training.canOrganizeTraining(99L,file));
        var rejected=assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                ()->query.assertRelationNameVisible(99L,23L));
        assertEquals(1_080_000_012,rejected.getCode());
    }

    @Test void currentTrainingDutyNeverGrantsOtherRelatedFilesNameMetadata() throws Exception {
        installCurrentTrainingOwnerPolicies();
        var other=DccControlledFileDO.builder().id(24L).tenantId(1L).masterId(13L).dccProjectCodeId(5L)
                .categoryId(2L).directoryId(8L).requesterId(88L).versionNo("A/1")
                .status(DccOfflineTrainingRecordService.STATUS).processDefinitionKey(DccControlledFileProcessDefinitionKeys.REVISION)
                .processInstanceId("other-file-round").fileOwnerUserId(77L).build();
        DccControlledFileMapper files=port("controlledFileMapper");when(files.selectById(24L)).thenReturn(other);
        DccControlledFileAssignmentScopeService scope=port("assignmentScopeService");
        when(scope.isWithinAssignedFileScope(99L,24L)).thenReturn(true);
        query.assertRelationNameVisible(99L,23L);
        var rejected=assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                ()->query.assertRelationNameVisible(99L,24L));
        assertEquals(1_080_000_012,rejected.getCode());
        // A member rejected by the real bridge aborts the whole evidence read rather than disappearing.
        var bridge=new DccRelationAccessPolicyImpl();ReflectionTestUtils.setField(bridge,"query",query);
        when(stages.readRoundEvidence(99L,23L,"REVISION","actual-bpm")).thenAnswer(call->{
            bridge.assertNameVisible(99L,23L);bridge.assertNameVisible(99L,24L);
            throw new AssertionError("An unauthorized related file must stop evidence projection");
        });
        assertEquals(1_080_000_012,assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,this::read).getCode());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"NO_EXECUTION","WRONG_EXECUTION","SUSPENDED_EXECUTION","NO_RECEIVE_TASK","INACTIVE_BOUND","DOC_CONTROL_EVIDENCE"})
    void trainingNameMetadataPropagatesRealWaitingFailure(String invalid) throws Exception {
        var training=installCurrentTrainingOwnerPolicies();
        String expected;
        switch(invalid) {
            case "NO_EXECUTION" -> {
                org.flowable.engine.RuntimeService runtime=dependency(training,"runtime");
                when(runtime.createExecutionQuery().processInstanceId("actual-bpm").activityId("TRAINING").list()).thenReturn(List.of());
                expected="DCC_OFFLINE_TRAINING_EXECUTION_REQUIRED";
            }
            case "WRONG_EXECUTION","SUSPENDED_EXECUTION" -> {
                org.flowable.engine.RuntimeService runtime=dependency(training,"runtime");
                var execution=runtime.createExecutionQuery().list().get(0);
                if("WRONG_EXECUTION".equals(invalid)) when(execution.getProcessInstanceId()).thenReturn("other-round");
                else when(execution.isSuspended()).thenReturn(true);
                expected="DCC_OFFLINE_TRAINING_EXECUTION_INVALID";
            }
            case "NO_RECEIVE_TASK" -> {
                cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService definitions=dependency(training,"definitions");
                definitions.getProcessDefinitionBpmnModel("definition").getMainProcess().removeFlowElement("TRAINING");
                expected="DCC_OFFLINE_TRAINING_DEFINITION_INVALID";
            }
            case "INACTIVE_BOUND" -> {
                DccBoundWorkflowContractResolver contracts=dependency(training,"boundContracts");
                var bound=contracts.resolve(file);
                when(contracts.resolve(file)).thenReturn(new DccBoundWorkflowContractResolver.BoundExecution(bound.tenantId(),
                        bound.controlledFileId(),bound.processInstanceId(),bound.attributeRound(),bound.processKey(),
                        bound.processDefinitionId(),bound.deploymentId(),bound.definitionDigest(),bound.executionContract(),bound.flowRule(),false));
                expected="DCC_OFFLINE_TRAINING_BOUND_IDENTITY_INVALID";
            }
            case "DOC_CONTROL_EVIDENCE" -> {
                DccControlledFileLifecycleService lifecycle=dependency(training,"lifecycle");
                doThrow(new IllegalStateException("formal document-control evidence rejected")).when(lifecycle)
                        .requirePublicDocumentControlEvidence(eq(file),any());
                expected="formal document-control evidence rejected";
            }
            default -> throw new AssertionError(invalid);
        }
        assertEquals(expected,assertThrows(IllegalStateException.class,
                ()->query.assertRelationNameVisible(99L,23L)).getMessage());
    }

    private DccOfflineTrainingRecordService installCurrentTrainingOwnerPolicies() throws Exception {
        file.setStatus(DccOfflineTrainingRecordService.STATUS);
        file.setDirectoryId(8L);file.setProcessDefinitionKey(DccControlledFileProcessDefinitionKeys.REVISION);
        file.setProcessInstanceId("actual-bpm");file.setNeedTraining(true);file.setApprovedTime(LocalDateTime.of(2026,10,19,12,0));
        file.setFileOwnerUserId(99L);file.setFileOwnerSignatureId(860L);file.setFileOwnerApprovalTaskId("owner-approval");
        file.setFileOwnerProcessInstanceId("actual-bpm");file.setFileOwnerSelectedTime(file.getApprovedTime());

        var training=new DccOfflineTrainingRecordService();mockPorts(training);
        ReflectionTestUtils.setField(training,"versions",DccControlledFileVersionPolicy.defaultPolicy());
        var account=new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO();
        account.setId(99L);account.setTenantId(1L);account.setStatus(0);
        cn.iocoder.yudao.module.system.api.user.AdminUserApi users=dependency(training,"users");
        when(users.getUser(99L)).thenReturn(account);
        cn.iocoder.yudao.module.system.api.permission.PermissionApi permissions=dependency(training,"permissions");
        when(permissions.hasAnyPermissions(99L,"dcc:controlled-file:query")).thenReturn(true);
        var master=new cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO();
        master.setId(12L);master.setTenantId(1L);master.setDccProjectCodeId(5L);
        DccControlledFileMasterMapper masters=dependency(training,"masters");when(masters.selectById(12L)).thenReturn(master);
        var project=new cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO();
        project.setId(5L);project.setTenantId(1L);
        cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper projects=dependency(training,"projects");
        when(projects.selectById(5L)).thenReturn(project);
        DccBoundWorkflowContractResolver contracts=dependency(training,"boundContracts");
        when(contracts.resolve(file)).thenReturn(new DccBoundWorkflowContractResolver.BoundExecution(1L,23L,"actual-bpm",7,
                DccControlledFileProcessDefinitionKeys.REVISION,"definition","deployment","a".repeat(64),"PUBLIC_V6_ACTION",
                DccWorkflowDefinitionCatalog.FlowRule.PUBLIC_V6_DELAYED_EFFECTIVE_CONFIRM,true));
        var process=mock(org.flowable.engine.runtime.ProcessInstance.class);
        when(process.getId()).thenReturn("actual-bpm");when(process.getTenantId()).thenReturn("1");
        when(process.getProcessDefinitionKey()).thenReturn(file.getProcessDefinitionKey());
        when(process.getProcessDefinitionId()).thenReturn("definition");when(process.getBusinessKey()).thenReturn("23");
        when(process.getProcessVariables()).thenReturn(Map.of("controlledFileId","23"));
        cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService processes=dependency(training,"processes");
        when(processes.getProcessInstance("actual-bpm")).thenReturn(process);
        var definition=mock(org.flowable.engine.repository.ProcessDefinition.class);
        when(definition.getId()).thenReturn("definition");when(definition.getTenantId()).thenReturn("1");
        when(definition.getKey()).thenReturn(file.getProcessDefinitionKey());
        var model=new org.flowable.bpmn.model.BpmnModel();var main=new org.flowable.bpmn.model.Process();
        main.setId(file.getProcessDefinitionKey());var receive=new org.flowable.bpmn.model.ReceiveTask();receive.setId("TRAINING");
        main.addFlowElement(receive);model.addProcess(main);
        cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService definitions=dependency(training,"definitions");
        when(definitions.getProcessDefinition("definition")).thenReturn(definition);
        when(definitions.getProcessDefinitionBpmnModel("definition")).thenReturn(model);
        var executions=mock(org.flowable.engine.runtime.ExecutionQuery.class,RETURNS_SELF);
        org.flowable.engine.RuntimeService runtime=dependency(training,"runtime");when(runtime.createExecutionQuery()).thenReturn(executions);
        var execution=mock(org.flowable.engine.runtime.Execution.class);
        when(execution.getId()).thenReturn("training-execution");when(execution.getTenantId()).thenReturn("1");
        when(execution.getProcessInstanceId()).thenReturn("actual-bpm");when(execution.getActivityId()).thenReturn("TRAINING");
        when(executions.list()).thenReturn(List.of(execution));

        var realGuard=new DccControlledFileDetailAuthorizationGuard();mockPorts(realGuard);
        detail=realGuard;
        ReflectionTestUtils.setField(realGuard,"offlineTraining",training);
        ReflectionTestUtils.setField(query,"offlineTraining",training);ReflectionTestUtils.setField(query,"detailAuthorizationGuard",realGuard);
        DccControlledFileAssignmentScopeService scope=port("assignmentScopeService");
        when(scope.isWithinAssignedFileScope(99L,23L)).thenReturn(true);
        ReflectionTestUtils.setField(realGuard,"assignmentScopeService",scope);
        cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessPermissionService directories=port("directoryAccessPermissionService");
        when(directories.getAuthorizedDirectoryIds(eq(99L),any())).thenReturn(Set.of());
        var bridge=new DccRelationAccessPolicyImpl();ReflectionTestUtils.setField(bridge,"query",query);
        var relations=new DccRelationRoundEvidence(1L,23L,12L,5L,"REVISION",7,"actual-bpm",false,
                "本轮没有关联文件",List.of(),List.of(),List.of(),"a".repeat(64));
        when(stages.readRoundEvidence(99L,23L,"REVISION","actual-bpm")).thenAnswer(call->{
            bridge.assertNameVisible(99L,23L);return relations;
        });
        return training;
    }

    private static void mockPorts(Object service) throws Exception {
        for(var field:service.getClass().getDeclaredFields())
            if(!Modifier.isStatic(field.getModifiers()) && field.getAnnotation(Resource.class)!=null)
                ReflectionTestUtils.setField(service,field.getName(),mock(field.getType()));
    }
    @SuppressWarnings("unchecked") private static <T>T dependency(Object service,String name) {
        return (T)ReflectionTestUtils.getField(service,name);
    }
}
