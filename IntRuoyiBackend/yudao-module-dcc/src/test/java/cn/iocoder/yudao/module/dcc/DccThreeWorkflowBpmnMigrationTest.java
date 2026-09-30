package cn.iocoder.yudao.module.dcc;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DccThreeWorkflowBpmnMigrationTest {

    private static final String MIGRATION_FILE = "sql/mysql/20260922_dcc_three_workflow_bpmn_seed.sql";
    private static final String FIX_MIGRATION_FILE =
            "sql/mysql/20260923_dcc_three_workflow_candidate_strategy_fix.sql";

    @Test
    void threeWorkflowBpmnSeedShouldDeclareIndependentUploadRevisionAndObsoleteModels() throws Exception {
        Path projectDir = findProjectDir();
        Path migrationFile = projectDir.resolve(MIGRATION_FILE);

        assertTrue(Files.exists(migrationFile), "DCC three workflow BPMN seed migration must exist");

        String sql = Files.readString(migrationFile, StandardCharsets.UTF_8);
        assertTrue(sql.contains("-- release-migration:"), "migration must carry release metadata");
        assertTrue(sql.contains("dependsOn=20260921_dcc_category_approval_route_action_type"),
                "three workflow seed must depend on route action type schema");
        assertTrue(sql.contains("BINARY CONVERT(b.BYTES_ USING utf8mb4) <> BINARY defs.bpmn_xml"),
                "BPMN byte comparison must use binary semantics across MySQL collations");
        assertTrue(sql.contains("BINARY p.KEY_ = BINARY defs.process_key")
                && sql.contains("BINARY m.KEY_ = BINARY defs.process_key")
                && sql.contains("BINARY i.process_definition_id ="),
                "Flowable and BPM metadata key comparisons must use binary semantics across MySQL collations");

        assertActionWorkflow(sql, "dcc-controlled-file-upload");
        assertActionWorkflow(sql, "dcc-controlled-file-revision");
        assertObsoleteWorkflow(sql);
        assertTrue(sql.contains("process_definition_key = 'dcc-controlled-file-obsolete'"),
                "DCC obsolete FormCenter policy must be switched to the independent obsolete process key");
        assertFalse(sql.contains("process_definition_key = 'dcc-controlled-file-obsolete-approval'"),
                "new three workflow seed must not point DCC obsolete policy back to the legacy one-step process");
    }

    @Test
    void threeWorkflowBpmnSeedShouldDeployAndRunInIsolatedFlowableEngine() throws Exception {
        String sql = Files.readString(findProjectDir().resolve(MIGRATION_FILE), StandardCharsets.UTF_8);
        String uploadBpmn = sqlVariable(sql, "@dcc_upload_bpmn");
        String revisionBpmn = sqlVariable(sql, "@dcc_revision_bpmn");
        String obsoleteBpmn = sqlVariable(sql, "@dcc_obsolete_bpmn");

        StandaloneInMemProcessEngineConfiguration configuration = new StandaloneInMemProcessEngineConfiguration();
        configuration.setDatabaseSchemaUpdate("true");
        configuration.setAsyncExecutorActivate(false);
        configuration.setDisableIdmEngine(true);

        ProcessEngine engine = configuration.buildProcessEngine();
        try {
            deploy(engine, "dcc-controlled-file-upload.bpmn20.xml", uploadBpmn);
            deploy(engine, "dcc-controlled-file-revision.bpmn20.xml", revisionBpmn);
            deploy(engine, "dcc-controlled-file-obsolete.bpmn20.xml", obsoleteBpmn);

            assertEquals(1, engine.getRepositoryService().createProcessDefinitionQuery()
                    .processDefinitionKey("dcc-controlled-file-upload").count());
            assertEquals(1, engine.getRepositoryService().createProcessDefinitionQuery()
                    .processDefinitionKey("dcc-controlled-file-revision").count());
            assertEquals(1, engine.getRepositoryService().createProcessDefinitionQuery()
                    .processDefinitionKey("dcc-controlled-file-obsolete").count());

            assertUploadOrRevisionPath(engine, "dcc-controlled-file-upload", true);
            assertUploadOrRevisionPath(engine, "dcc-controlled-file-upload", false);
            assertUploadOrRevisionPath(engine, "dcc-controlled-file-revision", true);
            assertUploadOrRevisionPath(engine, "dcc-controlled-file-revision", false);
            assertObsoletePath(engine);
        } finally {
            engine.close();
        }
    }

    @Test
    void candidateStrategyFixMigrationShouldPublishVersionTwoIdempotently() throws Exception {
        Path migrationFile = findProjectDir().resolve(FIX_MIGRATION_FILE);
        assertTrue(Files.exists(migrationFile), "candidate strategy fix migration must exist");

        String sql = Files.readString(migrationFile, StandardCharsets.UTF_8);
        assertTrue(sql.startsWith("-- release-migration:"));
        assertTrue(sql.contains("dependsOn=20260922_dcc_three_workflow_bpmn_seed"));
        assertTrue(sql.contains("VERSION_,") && sql.contains("2,"));
        assertTrue(sql.contains("-deploy-v2-tenant-"));
        assertTrue(sql.contains("-bpmn-v2-tenant-"));
        assertTrue(sql.contains("-model-v2-tenant-"));
        assertTrue(sql.contains("SUSPENSION_STATE_ = 2"));
        assertTrue(sql.contains("REPLACE("));
        assertTrue(sql.contains("NOT EXISTS"));
    }

    private static void assertActionWorkflow(String sql, String processKey) {
        String process = processBlock(sql, processKey);
        assertTrue(process.contains("id=\"MATRIX_REVIEW\""), processKey + " must start with matrix review");
        assertTrue(process.contains("id=\"MATRIX_APPROVAL\""), processKey + " must include matrix approval");
        assertTrue(process.contains("id=\"NeedTrainingGateway\""), processKey + " must branch on needTraining");
        assertTrue(process.contains("${needTraining == true}"), processKey + " must have true training condition");
        assertTrue(process.contains("${needTraining != true}"), processKey + " must skip training only when false");
        assertTrue(process.contains("id=\"TRAINING\""), processKey + " must include training wait node");
        assertTrue(process.contains("id=\"DISTRIBUTION\""), processKey + " must include distribution wait node");
        assertTrue(process.contains("<receiveTask id=\"TRAINING\""), processKey + " training node must wait for DCC runtime trigger");
        assertTrue(process.contains("<receiveTask id=\"DISTRIBUTION\""), processKey + " distribution node must wait for DCC runtime trigger");
        assertFalse(process.contains("dccControlledFileTrainingWaitDelegate"), processKey + " must not use auto-completing training serviceTask");
        assertFalse(process.contains("dccControlledFileDistributionWaitDelegate"), processKey + " must not use auto-completing distribution serviceTask");
        assertTrue(process.contains("id=\"DOC_CONTROL_REVIEW\""), processKey + " must end with document-control review");
        assertCandidateStrategy(process, "MATRIX_REVIEW", 35, processKey);
        assertCandidateStrategy(process, "MATRIX_APPROVAL", 34, processKey);
        assertCandidateStrategy(process, "DOC_CONTROL_REVIEW", 34, processKey);
        assertAppearsBefore(process, "id=\"MATRIX_REVIEW\"", "id=\"MATRIX_APPROVAL\"", processKey);
        assertAppearsBefore(process, "id=\"MATRIX_APPROVAL\"", "id=\"NeedTrainingGateway\"", processKey);
        assertAppearsBefore(process, "id=\"NeedTrainingGateway\"", "id=\"DOC_CONTROL_REVIEW\"", processKey);
    }

    private static void assertObsoleteWorkflow(String sql) {
        String process = processBlock(sql, "dcc-controlled-file-obsolete");
        assertTrue(process.contains("id=\"MATRIX_REVIEW\""), "obsolete workflow must start with matrix review");
        assertTrue(process.contains("id=\"MATRIX_APPROVAL\""), "obsolete workflow must include matrix approval");
        assertTrue(process.contains("id=\"DOC_CONTROL_REVIEW\""), "obsolete workflow must end with document-control review");
        assertCandidateStrategy(process, "MATRIX_REVIEW", 35, "obsolete");
        assertCandidateStrategy(process, "MATRIX_APPROVAL", 34, "obsolete");
        assertCandidateStrategy(process, "DOC_CONTROL_REVIEW", 34, "obsolete");
        assertFalse(process.contains("TRAINING"), "obsolete workflow must not include training");
        assertFalse(process.contains("DISTRIBUTION"), "obsolete workflow must not include distribution");
        assertFalse(process.contains("NeedTrainingGateway"), "obsolete workflow must not branch on needTraining");
        assertAppearsBefore(process, "id=\"MATRIX_REVIEW\"", "id=\"MATRIX_APPROVAL\"", "obsolete");
        assertAppearsBefore(process, "id=\"MATRIX_APPROVAL\"", "id=\"DOC_CONTROL_REVIEW\"", "obsolete");
    }

    private static String processBlock(String sql, String processKey) {
        String startMarker = "<process id=\"" + processKey + "\"";
        int start = sql.indexOf(startMarker);
        assertTrue(start >= 0, "missing process " + processKey);
        int end = sql.indexOf("</process>", start);
        assertTrue(end > start, "missing end process for " + processKey);
        return sql.substring(start, end);
    }

    private static void deploy(ProcessEngine engine, String resourceName, String bpmnXml) {
        engine.getRepositoryService().createDeployment()
                .name(resourceName)
                .addInputStream(resourceName, new ByteArrayInputStream(bpmnXml.getBytes(StandardCharsets.UTF_8)))
                .deploy();
    }

    private static void assertUploadOrRevisionPath(ProcessEngine engine, String processKey, boolean needTraining) {
        ProcessInstance instance = engine.getRuntimeService().startProcessInstanceByKey(processKey,
                Map.of("needTraining", needTraining));

        assertSingleActiveTask(engine, instance, "MATRIX_REVIEW");
        completeTask(engine, instance, "MATRIX_REVIEW");
        assertSingleActiveTask(engine, instance, "MATRIX_APPROVAL");
        completeTask(engine, instance, "MATRIX_APPROVAL");
        if (needTraining) {
            assertWaitingAt(engine, instance, "TRAINING");
            assertNoActiveTask(engine, instance, "DOC_CONTROL_REVIEW");
            triggerReceiveTask(engine, instance, "TRAINING");
        } else {
            assertNotWaitingAt(engine, instance, "TRAINING", processKey + " without training must skip TRAINING");
        }
        assertWaitingAt(engine, instance, "DISTRIBUTION");
        assertNoActiveTask(engine, instance, "DOC_CONTROL_REVIEW");
        triggerReceiveTask(engine, instance, "DISTRIBUTION");
        assertSingleActiveTask(engine, instance, "DOC_CONTROL_REVIEW");
        completeTask(engine, instance, "DOC_CONTROL_REVIEW");

        assertNull(engine.getRuntimeService().createProcessInstanceQuery()
                .processInstanceId(instance.getProcessInstanceId())
                .singleResult(), processKey + " process instance should end after doc-control review");
    }

    private static void assertObsoletePath(ProcessEngine engine) {
        ProcessInstance instance = engine.getRuntimeService()
                .startProcessInstanceByKey("dcc-controlled-file-obsolete", Map.of("needTraining", true));

        assertSingleActiveTask(engine, instance, "MATRIX_REVIEW");
        completeTask(engine, instance, "MATRIX_REVIEW");
        assertSingleActiveTask(engine, instance, "MATRIX_APPROVAL");
        completeTask(engine, instance, "MATRIX_APPROVAL");
        assertNotWaitingAt(engine, instance, "TRAINING", "obsolete workflow must never wait at TRAINING");
        assertNotWaitingAt(engine, instance, "DISTRIBUTION", "obsolete workflow must never wait at DISTRIBUTION");
        assertSingleActiveTask(engine, instance, "DOC_CONTROL_REVIEW");
        completeTask(engine, instance, "DOC_CONTROL_REVIEW");

        assertNull(engine.getRuntimeService().createProcessInstanceQuery()
                .processInstanceId(instance.getProcessInstanceId())
                .singleResult(), "obsolete process instance should end after doc-control review");
    }

    private static void completeTask(ProcessEngine engine, ProcessInstance instance, String taskDefinitionKey) {
        Task task = engine.getTaskService().createTaskQuery()
                .processInstanceId(instance.getProcessInstanceId())
                .taskDefinitionKey(taskDefinitionKey)
                .singleResult();
        assertNotNull(task, instance.getProcessDefinitionKey() + " must create task " + taskDefinitionKey);
        engine.getTaskService().complete(task.getId());
    }

    private static void assertSingleActiveTask(ProcessEngine engine, ProcessInstance instance, String taskDefinitionKey) {
        assertEquals(1, engine.getTaskService().createTaskQuery()
                .processInstanceId(instance.getProcessInstanceId())
                .active()
                .count(), instance.getProcessDefinitionKey() + " must have one active user task");
        assertNotNull(engine.getTaskService().createTaskQuery()
                .processInstanceId(instance.getProcessInstanceId())
                .taskDefinitionKey(taskDefinitionKey)
                .singleResult(), instance.getProcessDefinitionKey() + " must stop at " + taskDefinitionKey);
    }

    private static void assertNoActiveTask(ProcessEngine engine, ProcessInstance instance, String taskDefinitionKey) {
        assertEquals(0, engine.getTaskService().createTaskQuery()
                .processInstanceId(instance.getProcessInstanceId())
                .taskDefinitionKey(taskDefinitionKey)
                .count(), instance.getProcessDefinitionKey() + " must not activate " + taskDefinitionKey + " yet");
    }

    private static void triggerReceiveTask(ProcessEngine engine, ProcessInstance instance, String activityId) {
        assertWaitingAt(engine, instance, activityId);
        var execution = engine.getRuntimeService().createExecutionQuery()
                .processInstanceId(instance.getProcessInstanceId())
                .activityId(activityId)
                .singleResult();
        assertNotNull(execution, instance.getProcessDefinitionKey() + " must wait at " + activityId);
        engine.getRuntimeService().trigger(execution.getId());
    }

    private static void assertWaitingAt(ProcessEngine engine, ProcessInstance instance, String activityId) {
        assertNotNull(engine.getRuntimeService().createExecutionQuery()
                .processInstanceId(instance.getProcessInstanceId())
                .activityId(activityId)
                .singleResult(), instance.getProcessDefinitionKey() + " must wait at " + activityId);
    }

    private static void assertNotWaitingAt(ProcessEngine engine, ProcessInstance instance, String activityId,
            String message) {
        assertNull(engine.getRuntimeService().createExecutionQuery()
                .processInstanceId(instance.getProcessInstanceId())
                .activityId(activityId)
                .singleResult(), message);
    }

    private static String sqlVariable(String sql, String variableName) {
        String startMarker = "SET " + variableName + " = '";
        int start = sql.indexOf(startMarker);
        assertTrue(start >= 0, "missing " + variableName);
        start += startMarker.length();
        int end = sql.indexOf("';", start);
        assertTrue(end > start, "missing end of " + variableName);
        return sql.substring(start, end);
    }

    private static void assertAppearsBefore(String text, String before, String after, String processKey) {
        int beforeIndex = text.indexOf(before);
        int afterIndex = text.indexOf(after);
        assertTrue(beforeIndex >= 0, processKey + " missing " + before);
        assertTrue(afterIndex >= 0, processKey + " missing " + after);
        assertTrue(beforeIndex < afterIndex, processKey + " must order " + before + " before " + after);
    }

    private static void assertCandidateStrategy(String process, String taskId, int expectedStrategy,
            String processKey) {
        String startMarker = "<userTask id=\"" + taskId + "\"";
        int start = process.indexOf(startMarker);
        assertTrue(start >= 0, processKey + " missing user task " + taskId);
        int end = process.indexOf("</userTask>", start);
        assertTrue(end > start, processKey + " missing user task end for " + taskId);
        String task = process.substring(start, end);
        assertTrue(task.contains("<flowable:candidateStrategy>" + expectedStrategy + "</flowable:candidateStrategy>"),
                processKey + " task " + taskId + " must use candidate strategy " + expectedStrategy);
    }

    private static Path findProjectDir() {
        Path current = Paths.get("").toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("pom.xml")) && Files.exists(current.resolve("sql/mysql"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate project root");
    }
}
