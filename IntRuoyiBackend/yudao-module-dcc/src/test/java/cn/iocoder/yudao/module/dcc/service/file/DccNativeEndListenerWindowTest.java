package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.event.BpmProcessInstanceEventPublisher;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.listener.BpmProcessInstanceEventListener;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.iocoder.yudao.module.bpm.service.message.BpmMessageService;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceServiceImpl;
import cn.iocoder.yudao.module.dcc.service.file.listener.DccControlledFileStatusListener;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.junit.jupiter.api.*;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Whole candidate graph and official engine listener/service/publisher/DCC listener; observes pre-flush identity only. */
class DccNativeEndListenerWindowTest {
    private ProcessEngine engine; private GenericApplicationContext context;
    private BpmProcessInstanceServiceImpl processes;
    private final List<Map<String,Object>> observed=new ArrayList<>();
    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);context=new GenericApplicationContext();context.refresh();
        processes=new BpmProcessInstanceServiceImpl();
        ReflectionTestUtils.setField(processes,"processDefinitionService",mock(BpmProcessDefinitionService.class));
        ReflectionTestUtils.setField(processes,"messageService",mock(BpmMessageService.class));
        ReflectionTestUtils.setField(processes,"processInstanceEventPublisher",new BpmProcessInstanceEventPublisher(context));
        var listener=new BpmProcessInstanceEventListener();ReflectionTestUtils.setField(listener,"processInstanceService",processes);
        var config=new StandaloneInMemProcessEngineConfiguration();
        config.setJdbcUrl("jdbc:h2:mem:w06_end_window_"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1");
        config.setDatabaseSchemaUpdate("true");config.setAsyncExecutorActivate(false);config.setDisableIdmEngine(true);
        config.setEventListeners(List.of(listener));engine=config.buildProcessEngine();
        ReflectionTestUtils.setField(processes,"runtimeService",engine.getRuntimeService());
        ReflectionTestUtils.setField(processes,"historyService",engine.getHistoryService());
        var target=mock(DccControlledFileFinalizationService.class);
        doAnswer(call->{observe(call.getArgument(0));return null;}).when(target).handleProcessInstanceStatusChanged(any());
        var dccListener=new DccControlledFileStatusListener();ReflectionTestUtils.setField(dccListener,"finalizationService",target);
        context.addApplicationListener(dccListener);
    }
    @AfterEach void cleanup(){if(engine!=null)engine.close();if(context!=null)context.close();TenantContextHolder.clear();}
    @Test void trainingReceiveEndCarriesLastDocumentApproverDuringActualCommand()throws Exception{complete(true);}
    @Test void noTrainingDocumentApproveEndHasTheSameOfficialCommandWindow()throws Exception{complete(false);}
    private void complete(boolean training)throws Exception {
        var root=Path.of("").toAbsolutePath();while(root!=null&&!Files.isRegularFile(root.resolve("AGENTS.md")))root=root.getParent();
        assertNotNull(root);
        var xml=Files.readString(root.resolve("doc/tasks/20261011-dcc-w05-approval-signatures/bpmn-v5/upload.bpmn20.xml"));
        var deployment=engine.getRepositoryService().createDeployment().tenantId("1").addString("candidate.bpmn20.xml",xml).deploy();
        var definition=engine.getRepositoryService().createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
        var variables=new HashMap<String,Object>();variables.put("controlledFileId",20L);variables.put("needTraining",training);
        variables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_STATUS,BpmProcessInstanceStatusEnum.RUNNING.getStatus());
        variables.put("PROCESS_DCC_TASK_OBLIGATION_IDS",Map.of("MATRIX_REVIEW",List.of("department-1")));
        var process=engine.getRuntimeService().startProcessInstanceById(definition.getId(),"20",variables);
        for(String stage:List.of("SUPERVISOR_REVIEW","MATRIX_REVIEW","MATRIX_APPROVAL","DOC_CONTROL_REVIEW")){
            var task=engine.getTaskService().createTaskQuery().processInstanceId(process.getId()).singleResult();assertEquals(stage,task.getTaskDefinitionKey());
            engine.getTaskService().setAssignee(task.getId(),"88");
            engine.getRuntimeService().setVariable(process.getId(),BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_APPROVER_USER_ID,88L);
            engine.getTaskService().complete(task.getId());
        }
        if(training){assertTrue(observed.isEmpty());var execution=engine.getRuntimeService().createExecutionQuery().processInstanceId(process.getId()).activityId("TRAINING").singleResult();
            assertNotNull(execution);engine.getRuntimeService().trigger(execution.getId());}
        assertEquals(1,observed.size());var facts=observed.get(0);
        assertEquals(88L,facts.get("eventActor"));assertEquals(BpmProcessInstanceStatusEnum.APPROVE.getStatus(),facts.get("eventStatus"));
        System.out.println("OFFICIAL_END_WINDOW training="+training+" facts="+facts);
        assertEquals(true,facts.get("runtimeEnded"));
        assertNull(engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(process.getId()).singleResult());
        assertNotNull(engine.getHistoryService().createHistoricProcessInstanceQuery().processInstanceId(process.getId()).singleResult().getEndTime());
    }
    private void observe(BpmProcessInstanceStatusEvent event){
        var runtime=engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(event.getId()).includeProcessVariables().singleResult();
        var history=engine.getHistoryService().createHistoricProcessInstanceQuery().processInstanceId(event.getId()).includeProcessVariables().singleResult();
        var facts=new LinkedHashMap<String,Object>();facts.put("eventActor",event.getActorUserId());facts.put("eventStatus",event.getStatus());
        facts.put("runtimePresent",runtime!=null);facts.put("runtimeEnded",runtime!=null&&runtime.isEnded());
        facts.put("historyPresent",history!=null);facts.put("historyEnded",history!=null&&history.getEndTime()!=null);
        facts.put("liveTasks",engine.getTaskService().createTaskQuery().processInstanceId(event.getId()).count());
        facts.put("liveActivities",engine.getRuntimeService().getActiveActivityIds(event.getId()));
        facts.put("tasks",engine.getTaskService().createTaskQuery().processInstanceId(event.getId()).list().stream()
                .map(task->task.getTaskDefinitionKey()+":"+task.getClass().getSimpleName()).toList());
        facts.put("historyTasks",engine.getHistoryService().createHistoricTaskInstanceQuery().processInstanceId(event.getId()).list().stream()
                .map(task->task.getTaskDefinitionKey()+":"+(task.getEndTime()!=null)).toList());
        observed.add(facts);
    }
}
