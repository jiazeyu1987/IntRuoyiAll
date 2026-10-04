package cn.iocoder.yudao.module.dcc;

import cn.iocoder.yudao.module.bpm.framework.flowable.core.behavior.BpmActivityBehaviorFactory;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.candidate.BpmTaskCandidateInvoker;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class DccWorkflowV4BpmnTest {
    @Test
    void exactV4ModelsExecuteInMemoryWithTwoObligationsForOneUser() throws Exception {
        String sql = Files.readString(Path.of("../sql/mysql/20260930_dcc_a_workflow_bpmn_v4.sql"));
        var candidates = mock(BpmTaskCandidateInvoker.class);
        when(candidates.calculateUserListByTask(any())).thenReturn(List.of(99L,99L));
        when(candidates.calculateUsersByTask(any())).thenReturn(Set.of(100L));
        var factory = new BpmActivityBehaviorFactory(); factory.setTaskCandidateInvoker(candidates);
        var config = new StandaloneInMemProcessEngineConfiguration();
        config.setJdbcUrl("jdbc:h2:mem:dcc_a_v4;DB_CLOSE_DELAY=-1");
        config.setDatabaseSchemaUpdate("true");
        config.setAsyncExecutorActivate(false); config.setDisableIdmEngine(true);
        config.setActivityBehaviorFactory(factory);
        ProcessEngine engine = config.buildProcessEngine();
        try {
            for(String action:List.of("upload","revision","obsolete")) {
                String marker="SET @dcc_"+action+"_bpmn = '";
                int start=sql.indexOf(marker)+marker.length(); int end=sql.indexOf("';",start);
                String xml=sql.substring(start,end);
                assertFalse(xml.contains("id=\"DISTRIBUTION\""));
                if(action.equals("obsolete")) assertFalse(xml.contains("id=\"DOC_CONTROL_REVIEW\""));
                engine.getRepositoryService().createDeployment().addString(action+".bpmn20.xml",xml).deploy();
            }
            for(String action:List.of("upload","revision","obsolete")) {
                for(boolean training: action.equals("obsolete") ? List.of(false) : List.of(false,true)) {
                    var instance=engine.getRuntimeService().startProcessInstanceByKey("dcc-controlled-file-"+action,
                            Map.of("needTraining",training,
                                    BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_DCC_TASK_OBLIGATION_IDS,
                                    Map.of("MATRIX_REVIEW",List.of("department-51","department-52"))));
                    var tasks=engine.getTaskService().createTaskQuery().processInstanceId(instance.getId()).list();
                    assertEquals(2,tasks.size());
                    assertTrue(tasks.stream().allMatch(task -> "99".equals(task.getAssignee())));
                    assertEquals(Set.of("department-51","department-52"),tasks.stream().map(task ->
                            (String)engine.getTaskService().getVariableLocal(task.getId(),BpmnVariableConstants.TASK_VARIABLE_DCC_OBLIGATION_ID))
                            .collect(java.util.stream.Collectors.toSet()));
                    engine.getTaskService().complete(tasks.get(0).getId());
                    assertEquals("MATRIX_REVIEW",engine.getTaskService().createTaskQuery().processInstanceId(instance.getId()).singleResult().getTaskDefinitionKey());
                    engine.getTaskService().complete(tasks.get(1).getId());
                    complete(engine,instance.getId(),"MATRIX_APPROVAL");
                    if(action.equals("obsolete")) {
                        assertNull(engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(instance.getId()).singleResult());
                        continue;
                    }
                    if(training) {
                        assertEquals(0,engine.getTaskService().createTaskQuery().processInstanceId(instance.getId()).count());
                        var waiting=engine.getRuntimeService().createExecutionQuery().processInstanceId(instance.getId()).activityId("TRAINING").singleResult();
                        assertNotNull(waiting); engine.getRuntimeService().trigger(waiting.getId());
                    }
                    complete(engine,instance.getId(),"DOC_CONTROL_REVIEW");
                    assertNull(engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(instance.getId()).singleResult());
                }
            }
        } finally { engine.close(); }
    }
    private void complete(ProcessEngine engine,String instance,String key) {
        var task=engine.getTaskService().createTaskQuery().processInstanceId(instance).singleResult();
        assertNotNull(task); assertEquals(key,task.getTaskDefinitionKey());
        engine.getTaskService().complete(task.getId());
    }
}
