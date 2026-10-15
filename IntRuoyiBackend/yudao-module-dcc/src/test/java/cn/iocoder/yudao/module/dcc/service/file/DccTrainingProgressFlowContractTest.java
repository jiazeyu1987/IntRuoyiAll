package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileApplicationEvidenceMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Consumer tests only: the actual W05 leaf type is the declared external identity port.
 * These tests do not certify the leaf's historical provenance, BPM model or attribute implementation. */
class DccTrainingProgressFlowContractTest {
    @Mock private DccControlledFileMapper files;
    @Mock private DccOfflineTrainingRecordService training;
    @Mock private DccControlledFileApplicationEvidenceMapper mappings;
    @Mock private DccApplicationHistoryGuard history;
    @Mock private BpmTaskService tasks;
    @Mock private DccBoundWorkflowContractResolver boundContracts;
    @InjectMocks private DccTrainingApplicationProgressService service;
    private AutoCloseable mocks;
    private DccControlledFileDO file;
    private DccApplicationRoundSummary mapping;
    private HistoricTaskInstance task;

    @BeforeEach void setup() {
        mocks=MockitoAnnotations.openMocks(this);TenantContextHolder.setTenantId(1L);
        file=DccControlledFileDO.builder().id(10L).tenantId(1L).dccProjectCodeId(5L).requesterId(99L)
                .processInstanceId("training-round").processDefinitionKey(DccControlledFileProcessDefinitionKeys.REVISION)
                .status(DccOfflineTrainingRecordService.STATUS).needTraining(true).build();
        mapping=new DccApplicationRoundSummary();mapping.setControlledFileId(10L);mapping.setApplicationType("REVISION");
        mapping.setBpmRound("training-round");mapping.setAttributeRound(1);
        when(files.selectById(10L)).thenReturn(file);when(training.canReadProgress(99L,file)).thenReturn(true);
        when(mappings.selectApplicationRounds(1L,5L,10L)).thenReturn(List.of(mapping));
        task=mock(HistoricTaskInstance.class);when(task.getId()).thenReturn("actual-task");when(task.getProcessInstanceId()).thenReturn("training-round");
        when(task.getTenantId()).thenReturn("1");when(task.getTaskDefinitionKey()).thenReturn("MATRIX_REVIEW");
        when(task.getTaskLocalVariables()).thenReturn(Map.of("TASK_STATUS",2));
        when(tasks.getTaskListByProcessInstanceId("training-round",true)).thenReturn(List.of(task));
    }
    @AfterEach void cleanup()throws Exception{mocks.close();TenantContextHolder.clear();}

    @Test void authorizedExactMappingAndHistoryPrecedeLeafAndOriginalTaskRead() {
        var contract=ready(true,DccWorkflowDefinitionCatalog.FlowRule.PUBLIC_V5_TRAINING_AFTER_DOCUMENT_CONTROL);
        when(boundContracts.resolveRoundFlowContract(file,mapping)).thenReturn(contract);
        var result=service.read(99L,10L,"training-round");assertSame(contract,flow(result));
        assertEquals("actual-task",result.tasks().get(0).id());assertEquals(2,result.tasks().get(0).status());
        var ordered=inOrder(training,mappings,history,boundContracts,tasks);
        ordered.verify(training).canReadProgress(99L,file);
        ordered.verify(mappings).selectApplicationRounds(1L,5L,10L);
        ordered.verify(history).require(file,"REVISION","training-round");
        ordered.verify(boundContracts).resolveRoundFlowContract(file,mapping);
        ordered.verify(tasks).getTaskListByProcessInstanceId("training-round",true);
        verifyNoMoreInteractions(boundContracts);
    }

    @ParameterizedTest @ValueSource(strings={"QUALIFICATION","TENANT","ROUND","MISSING_MAPPING","DUPLICATE_MAPPING","HISTORY"})
    void existingReadGateFailureNeverQueriesLeafOrTasks(String broken) {
        switch(broken) {
            case "QUALIFICATION"->when(training.canReadProgress(99L,file)).thenReturn(false);
            case "TENANT"->file.setTenantId(2L);
            case "ROUND"->file.setProcessInstanceId("another-round");
            case "MISSING_MAPPING"->when(mappings.selectApplicationRounds(1L,5L,10L)).thenReturn(List.of());
            case "DUPLICATE_MAPPING"->when(mappings.selectApplicationRounds(1L,5L,10L)).thenReturn(List.of(mapping,mapping));
            case "HISTORY"->doThrow(new IllegalStateException("ACTUAL_HISTORY_IDENTITY_INVALID")).when(history).require(file,"REVISION","training-round");
        }
        assertThrows(RuntimeException.class,()->service.read(99L,10L,"training-round"));verifyNoInteractions(boundContracts,tasks);
    }

    @ParameterizedTest @ValueSource(booleans={true,false})
    void trainingSelectionComesOnlyFromTheExactLeafNotCurrentFile(boolean actualTraining) {
        file.setNeedTraining(!actualTraining);var contract=ready(actualTraining,DccWorkflowDefinitionCatalog.FlowRule.PUBLIC_V5_TRAINING_AFTER_DOCUMENT_CONTROL);
        when(boundContracts.resolveRoundFlowContract(file,mapping)).thenReturn(contract);
        assertEquals(actualTraining,flow(service.read(99L,10L,"training-round")).needTraining());
        assertEquals(!actualTraining,file.getNeedTraining());
    }

    @Test void readyCannotTreatAnUnrecordedTrainingSelectionAsFalse() {
        when(boundContracts.resolveRoundFlowContract(file,mapping)).thenReturn(ready(null,DccWorkflowDefinitionCatalog.FlowRule.PUBLIC_V5_TRAINING_AFTER_DOCUMENT_CONTROL));
        assertThrows(IllegalStateException.class,()->service.read(99L,10L,"training-round"));verifyNoInteractions(tasks);
    }

    @ParameterizedTest @ValueSource(strings={"NOT_RECORDED","UNSUPPORTED_DEFINITION"})
    void typedUnavailablePreservesOriginalTasksAndDoesNotInventAFlow(String status) {
        var unavailable=new DccBoundWorkflowContractResolver.RoundFlowContract(1L,10L,"REVISION",1,"training-round",file.getProcessDefinitionKey(),
                "actual-definition","actual-deployment","a".repeat(64),null,null,null,null,null,
                DccBoundWorkflowContractResolver.RoundContractStatus.valueOf(status),"ACTUAL_READ_SOURCE_UNAVAILABLE");
        when(boundContracts.resolveRoundFlowContract(file,mapping)).thenReturn(unavailable);
        var result=service.read(99L,10L,"training-round");assertSame(unavailable,flow(result));
        assertNull(flow(result).flowRule());assertNull(flow(result).executionContract());assertEquals(1,result.tasks().size());
    }

    @ParameterizedTest @ValueSource(strings={"TENANT","FILE","TYPE","ATTRIBUTE_ROUND","BPM_ROUND","KEY"})
    void aDifferentReturnedTupleNeverAuthorizesTaskProjection(String mismatch) {
        var contract=new DccBoundWorkflowContractResolver.RoundFlowContract("TENANT".equals(mismatch)?2L:1L,"FILE".equals(mismatch)?11L:10L,
                "TYPE".equals(mismatch)?"UPLOAD":"REVISION","ATTRIBUTE_ROUND".equals(mismatch)?2:1,"BPM_ROUND".equals(mismatch)?"other-round":"training-round",
                "KEY".equals(mismatch)?DccControlledFileProcessDefinitionKeys.UPLOAD:file.getProcessDefinitionKey(),"actual-definition","actual-deployment",
                "a".repeat(64),"PUBLIC_MATRIX_NATIVE_V1",DccWorkflowDefinitionCatalog.FlowRule.PUBLIC_V5_TRAINING_AFTER_DOCUMENT_CONTROL,
                true,null,null,DccBoundWorkflowContractResolver.RoundContractStatus.READY,null);
        when(boundContracts.resolveRoundFlowContract(file,mapping)).thenReturn(contract);
        assertThrows(IllegalStateException.class,()->service.read(99L,10L,"training-round"));verifyNoInteractions(tasks);
    }

    @Test void leafDependencyFailureIsPropagatedWithoutMarkingTypedUnavailable() {
        var failure=new IllegalStateException("REAL_MODEL_READ_FAILED");when(boundContracts.resolveRoundFlowContract(file,mapping)).thenThrow(failure);
        assertSame(failure,assertThrows(IllegalStateException.class,()->service.read(99L,10L,"training-round")));verifyNoInteractions(tasks);
    }
    @Test void missingLeafResultCannotBeAValidNullableContract() {
        when(boundContracts.resolveRoundFlowContract(file,mapping)).thenReturn(null);
        assertThrows(IllegalStateException.class,()->service.read(99L,10L,"training-round"));verifyNoInteractions(tasks);
    }

    @ParameterizedTest @ValueSource(strings={"FOREIGN_TASK","MISSING_STATUS","EMPTY_TASKS"})
    void readyFlowNeverConcealsDamagedOriginalTaskHistory(String damaged) {
        when(boundContracts.resolveRoundFlowContract(file,mapping)).thenReturn(ready(true,DccWorkflowDefinitionCatalog.FlowRule.PUBLIC_V5_TRAINING_AFTER_DOCUMENT_CONTROL));
        switch(damaged){case "FOREIGN_TASK"->when(task.getTenantId()).thenReturn("2");case "MISSING_STATUS"->when(task.getTaskLocalVariables()).thenReturn(Map.of());
            case "EMPTY_TASKS"->when(tasks.getTaskListByProcessInstanceId("training-round",true)).thenReturn(List.of());}
        assertThrows(IllegalStateException.class,()->service.read(99L,10L,"training-round"));verify(boundContracts).resolveRoundFlowContract(file,mapping);
    }

    @ParameterizedTest @ValueSource(strings={"LEGACY_V4_TRAINING_BEFORE_DOCUMENT_CONTROL","PUBLIC_V5_TRAINING_AFTER_DOCUMENT_CONTROL"})
    void exactOldAndNewRulesAreReturnedWithoutRewritingTheOriginalTaskNodes(String rule) {
        var contract=ready(true,DccWorkflowDefinitionCatalog.FlowRule.valueOf(rule));when(boundContracts.resolveRoundFlowContract(file,mapping)).thenReturn(contract);
        var result=service.read(99L,10L,"training-round");assertSame(contract,flow(result));assertEquals("MATRIX_REVIEW",result.tasks().get(0).taskDefinitionKey());
    }

    private DccBoundWorkflowContractResolver.RoundFlowContract ready(Boolean trainingChoice,DccWorkflowDefinitionCatalog.FlowRule rule) {
        return new DccBoundWorkflowContractResolver.RoundFlowContract(1L,10L,"REVISION",1,"training-round",file.getProcessDefinitionKey(),
                "actual-definition","actual-deployment","a".repeat(64),rule==DccWorkflowDefinitionCatalog.FlowRule.LEGACY_V4_TRAINING_BEFORE_DOCUMENT_CONTROL
                    ?"LEGACY_V4_ACTION":"PUBLIC_MATRIX_NATIVE_V1",rule,trainingChoice,null,null,DccBoundWorkflowContractResolver.RoundContractStatus.READY,null);
    }
    private DccBoundWorkflowContractResolver.RoundFlowContract flow(DccTrainingApplicationProgressService.Progress result) {
        return ReflectionTestUtils.invokeMethod(result,"flowContract");
    }
}
