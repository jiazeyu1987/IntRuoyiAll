package cn.iocoder.yudao.module.dcc.service.file;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import org.flowable.engine.RuntimeService;
import org.flowable.bpmn.model.*;
import org.junit.jupiter.api.*;
import org.mockito.*;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Waiting consumer scope; exact-bound and document-evidence services are declared external verified ports. */
class DccPublicTrainingWaitingTest {
    @Mock private DccBoundWorkflowContractResolver boundContracts;
    @Mock private DccControlledFileLifecycleService lifecycle;
    @Mock private DccControlledFileMasterMapper masters;
    @Mock private DccProjectCodeMapper projects;
    @Mock private BpmProcessInstanceService processes;
    @Mock private BpmProcessDefinitionService definitions;
    @Mock private RuntimeService runtime;
    @InjectMocks private DccOfflineTrainingRecordService service;
    private AutoCloseable mocks;
    private DccControlledFileDO file;
    @BeforeEach void setup(){
        mocks=MockitoAnnotations.openMocks(this);TenantContextHolder.setTenantId(1L);
        org.springframework.test.util.ReflectionTestUtils.setField(service,"versions",DccControlledFileVersionPolicy.defaultPolicy());
        file=DccControlledFileDO.builder().id(20L).tenantId(1L).masterId(3L).dccProjectCodeId(4L).categoryId(2L)
                .versionNo("B/1").status(DccOfflineTrainingRecordService.STATUS).processDefinitionKey(DccControlledFileProcessDefinitionKeys.UPLOAD)
                .processInstanceId("real-round").approvedTime(LocalDateTime.now()).needTraining(true).build();
        when(masters.selectById(3L)).thenReturn(DccControlledFileMasterDO.builder().id(3L).tenantId(1L).dccProjectCodeId(4L).build());
        var project=new DccProjectCodeDO();project.setId(4L);project.setTenantId(1L);when(projects.selectById(4L)).thenReturn(project);
        var process=mock(org.flowable.engine.runtime.ProcessInstance.class);when(process.getId()).thenReturn("real-round");when(process.getTenantId()).thenReturn("1");
        when(process.getBusinessKey()).thenReturn("20");when(process.getProcessDefinitionKey()).thenReturn(file.getProcessDefinitionKey());
        when(process.getProcessDefinitionId()).thenReturn("exact-id");when(process.getProcessVariables()).thenReturn(Map.of("controlledFileId",20L));
        when(processes.getProcessInstance("real-round")).thenReturn(process);
        var definition=mock(org.flowable.engine.repository.ProcessDefinition.class);when(definition.getId()).thenReturn("exact-id");
        when(definition.getTenantId()).thenReturn("1");when(definition.getKey()).thenReturn(file.getProcessDefinitionKey());when(definitions.getProcessDefinition("exact-id")).thenReturn(definition);
        var model=new BpmnModel();var body=new org.flowable.bpmn.model.Process();body.setId(file.getProcessDefinitionKey());
        var receive=new ReceiveTask();receive.setId("TRAINING");body.addFlowElement(receive);model.addProcess(body);when(definitions.getProcessDefinitionBpmnModel("exact-id")).thenReturn(model);
        var query=mock(org.flowable.engine.runtime.ExecutionQuery.class,Answers.RETURNS_SELF);when(runtime.createExecutionQuery()).thenReturn(query);
        var execution=mock(org.flowable.engine.runtime.Execution.class);when(execution.getId()).thenReturn("actual-receive");when(execution.getTenantId()).thenReturn("1");
        when(execution.getProcessInstanceId()).thenReturn("real-round");when(execution.getActivityId()).thenReturn("TRAINING");when(query.list()).thenReturn(List.of(execution));
    }
    @AfterEach void cleanup()throws Exception{mocks.close();TenantContextHolder.clear();}
    @Test void publicWaitingCallsCompletedDocumentAndOwnerEvidenceBeforeGrantingTask(){var bound=contract(true);when(boundContracts.resolve(file)).thenReturn(bound);
        assertEquals("actual-receive",service.requireWaiting(file).executionId());verify(lifecycle).requirePublicDocumentControlEvidence(file,bound);}
    @Test void publicCannotWaitWhenDocumentSignatureIsMissing(){var bound=contract(true);when(boundContracts.resolve(file)).thenReturn(bound);
        var failure=new IllegalStateException("missing actual document signature");doThrow(failure).when(lifecycle).requirePublicDocumentControlEvidence(file,bound);
        assertSame(failure,assertThrows(IllegalStateException.class,()->service.requireWaiting(file)));}
    @Test void exactLegacyWaitingDoesNotRequireAFutureDocumentSignature(){when(boundContracts.resolve(file)).thenReturn(contract(false));
        assertEquals("actual-receive",service.requireWaiting(file).executionId());verifyNoInteractions(lifecycle);}
    private DccBoundWorkflowContractResolver.BoundExecution contract(boolean isPublic){return new DccBoundWorkflowContractResolver.BoundExecution(1L,20L,"real-round",isPublic?7:null,
            file.getProcessDefinitionKey(),"exact-id","deployment","a".repeat(64),isPublic?"PUBLIC_MATRIX_NATIVE_V1":"LEGACY_V4_ACTION",
            isPublic?DccWorkflowDefinitionCatalog.FlowRule.PUBLIC_V5_TRAINING_AFTER_DOCUMENT_CONTROL:DccWorkflowDefinitionCatalog.FlowRule.LEGACY_V4_TRAINING_BEFORE_DOCUMENT_CONTROL,true);}
}
