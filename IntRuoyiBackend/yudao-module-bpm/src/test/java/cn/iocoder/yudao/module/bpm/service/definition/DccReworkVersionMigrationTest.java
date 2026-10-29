package cn.iocoder.yudao.module.bpm.service.definition;

import org.flowable.engine.ProcessEngineConfiguration;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class DccReworkVersionMigrationTest {
    private byte[] source() {
        return ("""
            <?xml version="1.0" encoding="UTF-8"?>
            <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="test">
             <process id="dcc-controlled-file-approval" name="Existing approval" isExecutable="true">
              <startEvent id="start"/><sequenceFlow id="s" sourceRef="start" targetRef="DOC_CONTROL_REVIEW"/>
              <userTask id="DOC_CONTROL_REVIEW" name="Existing review"/>
              <sequenceFlow id="a" sourceRef="DOC_CONTROL_REVIEW" targetRef="MATRIX_REVIEW"/>
              <userTask id="MATRIX_REVIEW" name="Existing matrix review"><extensionElements><flowable:candidateStrategy>34</flowable:candidateStrategy></extensionElements></userTask>
              <sequenceFlow id="b" sourceRef="MATRIX_REVIEW" targetRef="MATRIX_APPROVAL"/>
              <userTask id="MATRIX_APPROVAL" name="Existing matrix approval"/>
              <sequenceFlow id="c" sourceRef="MATRIX_APPROVAL" targetRef="DOC_CONTROL_APPROVAL"/>
              <userTask id="DOC_CONTROL_APPROVAL" name="Existing control approval"/>
              <sequenceFlow id="d" sourceRef="DOC_CONTROL_APPROVAL" targetRef="end"/><endEvent id="end"/>
             </process>
            </definitions>
            """).stripLeading().getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void actualEngineKeepsOldResourcesAndRunningTasksOnOldVersion() throws Exception {
        var engine = ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration()
                .setJdbcUrl("jdbc:h2:mem:dcc-versioned;DB_CLOSE_DELAY=-1")
                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE)
                .setAsyncExecutorActivate(false).buildProcessEngine();
        try {
            var repository = engine.getRepositoryService();
            var oldDeployment = repository.createDeployment().tenantId("122").addBytes("dcc.bpmn20.xml", source()).disableSchemaValidation().deploy();
            var oldDefinition = repository.createProcessDefinitionQuery().deploymentId(oldDeployment.getId()).singleResult();
            var instance = engine.getRuntimeService().startProcessInstanceById(oldDefinition.getId());
            byte[] updated = DccReworkVersionMigration.addRework(source());
            repository.createDeployment().tenantId("122").addBytes("dcc.bpmn20.xml", updated).disableSchemaValidation().deploy();
            var latest = repository.createProcessDefinitionQuery().processDefinitionKey(DccReworkVersionMigration.KEY).processDefinitionTenantId("122").latestVersion().singleResult();
            assertEquals(oldDefinition.getVersion()+1, latest.getVersion());
            assertNotNull(repository.getBpmnModel(latest.getId()).getMainProcess().getFlowElement("APPLICANT_REWORK"));
            assertNull(repository.getBpmnModel(oldDefinition.getId()).getMainProcess().getFlowElement("APPLICANT_REWORK"));
            assertArrayEquals(source(), repository.getResourceAsStream(oldDeployment.getId(), "dcc.bpmn20.xml").readAllBytes());
            repository.suspendProcessDefinitionById(oldDefinition.getId(), false, null);
            var task = engine.getTaskService().createTaskQuery().processInstanceId(instance.getId()).singleResult();
            assertEquals(oldDefinition.getId(), task.getProcessDefinitionId());
            engine.getTaskService().complete(task.getId());
            assertEquals("MATRIX_REVIEW", engine.getTaskService().createTaskQuery().processInstanceId(instance.getId()).singleResult().getTaskDefinitionKey());
            var newInstance = engine.getRuntimeService().startProcessInstanceByKeyAndTenantId(DccReworkVersionMigration.KEY, "122");
            assertEquals(latest.getId(), newInstance.getProcessDefinitionId());
        } finally {
            engine.close();
        }
    }

    @Test
    void rejectsUnknownGraphAndSecondTransformation() {
        assertThrows(IllegalStateException.class, () -> DccReworkVersionMigration.addRework(
                new String(source(), StandardCharsets.UTF_8).replace("MATRIX_REVIEW", "UNKNOWN_REVIEW").getBytes(StandardCharsets.UTF_8)));
        assertThrows(IllegalStateException.class, () -> DccReworkVersionMigration.addRework(DccReworkVersionMigration.addRework(source())));
    }
}
