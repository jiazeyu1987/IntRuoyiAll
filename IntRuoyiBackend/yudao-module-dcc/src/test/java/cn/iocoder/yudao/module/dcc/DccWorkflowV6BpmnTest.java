package cn.iocoder.yudao.module.dcc;

import cn.iocoder.yudao.module.bpm.framework.flowable.core.behavior.BpmActivityBehaviorFactory;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.candidate.BpmTaskCandidateInvoker;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.dcc.service.file.DccLifecycleEffectiveContractResolver;
import org.flowable.engine.*;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class DccWorkflowV6BpmnTest {
    @Test void actualUploadAndRevisionModelsWaitForOriginalCompilerOnlyAfterDocumentAndNecessaryTraining() throws Exception {
        Path root=requireRepositoryRoot();
        var candidates=mock(BpmTaskCandidateInvoker.class);
        when(candidates.calculateUserListByTask(any())).thenReturn(List.of(99L,99L));
        when(candidates.calculateUsersByTask(any())).thenAnswer(call -> {
            var execution=call.getArgument(0,org.flowable.engine.delegate.DelegateExecution.class);
            return Set.of("DELAYED_EFFECTIVE_CONFIRM".equals(execution.getCurrentActivityId())?101L:100L);
        });
        var factory=new BpmActivityBehaviorFactory();factory.setTaskCandidateInvoker(candidates);
        var config=new StandaloneInMemProcessEngineConfiguration();config.setJdbcUrl("jdbc:h2:mem:g01_v6_"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1");
        config.setDatabaseSchemaUpdate("true");config.setAsyncExecutorActivate(false);config.setDisableIdmEngine(true);config.setActivityBehaviorFactory(factory);
        var engine=config.buildProcessEngine();try{
            for(String action:List.of("upload","revision")){
                String xml=Files.readString(root.resolve("IntRuoyiBackend/config/dcc-workflow-v6/"+action+".bpmn20.xml"));
                var deployment=engine.getRepositoryService().createDeployment().addString(action+".bpmn20.xml",xml).deploy();
                var definition=engine.getRepositoryService().createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
                System.out.println("G01_V6_CANONICAL_DIGEST "+action+"="+DccLifecycleEffectiveContractResolver.definitionDigest(engine.getRepositoryService().getBpmnModel(definition.getId())));
                for(boolean training:List.of(false,true))for(String mode:List.of("DELAYED","AFTER_STANDARDIZATION_SIGNATURE")){
                    var process=engine.getRuntimeService().startProcessInstanceById(definition.getId(),Map.of("needTraining",training,"effectiveMode",mode,
                            BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_DCC_TASK_OBLIGATION_IDS,Map.of("MATRIX_REVIEW",List.of("A","B"))));
                    complete(engine,process.getId(),"SUPERVISOR_REVIEW");
                    var matrix=engine.getTaskService().createTaskQuery().processInstanceId(process.getId()).list();assertEquals(2,matrix.size());
                    for(var task:matrix)engine.getTaskService().complete(task.getId());
                    complete(engine,process.getId(),"MATRIX_APPROVAL");complete(engine,process.getId(),"DOC_CONTROL_REVIEW");
                    if(training){assertEquals(0,engine.getTaskService().createTaskQuery().processInstanceId(process.getId()).count());
                        var wait=engine.getRuntimeService().createExecutionQuery().processInstanceId(process.getId()).activityId("TRAINING").singleResult();assertNotNull(wait);engine.getRuntimeService().trigger(wait.getId());}
                    if(mode.equals("DELAYED")){
                        assertNotNull(engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(process.getId()).singleResult());
                        var confirm=engine.getTaskService().createTaskQuery().processInstanceId(process.getId()).singleResult();assertEquals("DELAYED_EFFECTIVE_CONFIRM",confirm.getTaskDefinitionKey());assertEquals("101",confirm.getAssignee());
                        engine.getTaskService().complete(confirm.getId());
                    }
                    assertNull(engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(process.getId()).singleResult());
                }
            }
        } finally {engine.close();}
    }
    private static Path requireRepositoryRoot() {
        String explicit=System.getProperty("dcc.repository.root");
        Path root;
        if(explicit!=null){
            if(explicit.isBlank())throw new IllegalStateException("dcc.repository.root must be an explicit absolute repository path");
            root=Path.of(explicit).normalize();
            if(!root.isAbsolute())throw new IllegalStateException("dcc.repository.root must be absolute");
        } else {
            String basedir=System.getProperty("basedir");
            if(basedir==null || basedir.isBlank())throw new IllegalStateException("Maven basedir is required; standalone runners must set dcc.repository.root");
            Path module=Path.of(basedir).normalize();
            if(!module.isAbsolute() || module.getFileName()==null || !"yudao-module-dcc".equals(module.getFileName().toString())
                    || module.getParent()==null || module.getParent().getFileName()==null || !"IntRuoyiBackend".equals(module.getParent().getFileName().toString())
                    || module.getParent().getParent()==null || !Files.isRegularFile(module.resolve("pom.xml")))
                throw new IllegalStateException("Maven basedir must identify the real IntRuoyiBackend/yudao-module-dcc module");
            root=module.getParent().getParent();
        }
        if(!Files.isRegularFile(root.resolve("AGENTS.md")) || !Files.isRegularFile(root.resolve("IntRuoyiBackend/pom.xml"))
                || !Files.isRegularFile(root.resolve("IntRuoyiBackend/yudao-module-dcc/pom.xml")))
            throw new IllegalStateException("DCC test repository root is missing its formal repository/module markers: "+root);
        for(String action:List.of("upload","revision"))
            if(!Files.isRegularFile(root.resolve("IntRuoyiBackend/config/dcc-workflow-v6/"+action+".bpmn20.xml")))
                throw new IllegalStateException("DCC production V6 model is missing: "+root.resolve("IntRuoyiBackend/config/dcc-workflow-v6/"+action+".bpmn20.xml"));
        return root;
    }
    private void complete(ProcessEngine e,String id,String stage){var task=e.getTaskService().createTaskQuery().processInstanceId(id).singleResult();assertNotNull(task);assertEquals(stage,task.getTaskDefinitionKey());e.getTaskService().complete(task.getId());}
}
