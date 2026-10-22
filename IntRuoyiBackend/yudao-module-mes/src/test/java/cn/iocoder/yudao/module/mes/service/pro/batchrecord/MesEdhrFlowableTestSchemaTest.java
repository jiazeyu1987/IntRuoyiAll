package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesEdhrEngineOwnershipMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import jakarta.annotation.Resource;
import org.flowable.common.engine.api.Engine;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngines;
import org.flowable.eventregistry.impl.EventRegistryEngine;
import org.flowable.eventregistry.impl.EventRegistryEngines;
import org.flowable.idm.engine.IdmEngine;
import org.flowable.idm.engine.IdmEngines;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Real shared H2 resource: schema/lifecycle checks stay separate from MES fixture mutations. */
class MesEdhrFlowableTestSchemaTest extends BaseDbUnitTest {
    @Resource DataSource dataSource;
    @Resource MesEdhrEngineOwnershipMapper ownershipMapper;
    @Resource MesProBatchRecordExecutionMapper executionMapper;

    private static final String BPMN = """
            <?xml version="1.0" encoding="UTF-8"?>
            <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" targetNamespace="edhr">
              <process id="schema-lifecycle" isExecutable="true">
                <startEvent id="start"/><sequenceFlow id="s1" sourceRef="start" targetRef="task"/>
                <userTask id="task" name="Approval"/>
                <sequenceFlow id="s2" sourceRef="task" targetRef="end"/><endEvent id="end"/>
              </process>
            </definitions>
            """;

    @Test void completeOfficialSchemaVersionsAndBothIdentitySemantics() throws SQLException {
        try (var owned = open()) {
            var jdbc = new JdbcTemplate(dataSource);
            jdbc.update("INSERT INTO ACT_HI_TSK_LOG(TASK_ID_,TIME_STAMP_) VALUES('identity-test',CURRENT_TIMESTAMP)");
            jdbc.update("INSERT INTO ACT_EVT_LOG(TIME_STAMP_) VALUES(CURRENT_TIMESTAMP)");
            assertNotNull(jdbc.queryForObject("SELECT ID_ FROM ACT_HI_TSK_LOG WHERE TASK_ID_='identity-test'", Long.class));
            assertNotNull(jdbc.queryForObject("SELECT LOG_NR_ FROM ACT_EVT_LOG", Long.class));
            jdbc.update("INSERT INTO ACT_HI_TSK_LOG(ID_,TASK_ID_,TIME_STAMP_) VALUES(900001,'explicit-test',CURRENT_TIMESTAMP)");
            jdbc.update("INSERT INTO ACT_EVT_LOG(LOG_NR_,TIME_STAMP_) VALUES(900001,CURRENT_TIMESTAMP)");
            assertThrows(RuntimeException.class, () -> jdbc.update(
                    "INSERT INTO ACT_EVT_LOG(LOG_NR_,TIME_STAMP_) VALUES(900001,CURRENT_TIMESTAMP)"));
            assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM ACT_HI_TSK_LOG", Integer.class));
            assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM ACT_EVT_LOG", Integer.class));
            assertSame(owned.processConfig, owned.engine().getProcessEngineConfiguration());
            assertEquals("false", owned.processConfig.getDatabaseSchemaUpdate());
            assertEquals("h2", owned.processConfig.getDatabaseType());
        }
        assertNoSchema();
    }

    @Test void consecutiveRealBuildCloseLeavesRegistriesMesAndSharedResourceIntact() {
        Map<String, ProcessEngine> processes = new HashMap<>(ProcessEngines.getProcessEngines());
        Map<String, IdmEngine> identities = new HashMap<>(IdmEngines.getIdmEngines());
        Map<String, EventRegistryEngine> events = new HashMap<>(EventRegistryEngines.getEventRegistryEngines());
        Map<Object, Object> resources = TransactionSynchronizationManager.getResourceMap();
        var jdbc = new JdbcTemplate(dataSource);
        jdbc.update("INSERT INTO mes_pro_batch_record_execution(id,execution_code,work_order_id,work_order_code,batch_code,sheet_layout_json,status,revision_no,tenant_id,deleted) VALUES(930001,'schema-sentinel',1,'WO','B','{}',1,1,1,0)");
        for (int iteration = 0; iteration < 2; iteration++) {
            try (var owned = open()) {
                var deployment = owned.deploy(BPMN);
                var definition = owned.engine().getRepositoryService().createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
                var instance = owned.start(definition.getId());
                assertEquals(1, owned.engine().getTaskService().createTaskQuery().processInstanceId(instance.getId()).count());
                assertNotNull(IdmEngines.getIdmEngines().get(owned.idmConfig.getEngineName()));
                assertNotNull(EventRegistryEngines.getEventRegistryEngines().get(owned.eventConfig.getEngineName()));
                owned.close();
                owned.close();
            }
            assertNoSchema();
            assertEquals(processes, ProcessEngines.getProcessEngines());
            assertEquals(identities, IdmEngines.getIdmEngines());
            assertEquals(events, EventRegistryEngines.getEventRegistryEngines());
            assertEquals(resources, TransactionSynchronizationManager.getResourceMap());
            assertEquals("schema-sentinel", jdbc.queryForObject("SELECT execution_code FROM mes_pro_batch_record_execution WHERE id=930001", String.class));
        }
    }

    @Test void foreignObjectsAreRejectedWithoutDeletionOrSchemaAdoption() {
        var jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE ACT_FOREIGN_TEST (ID_ BIGINT PRIMARY KEY)");
        try {
            jdbc.update("INSERT INTO ACT_FOREIGN_TEST VALUES(9)");
            var failure = assertThrows(IllegalStateException.class, () -> { try (var unexpected = new NegativeLifetime(open())) { fail("owned open unexpectedly succeeded"); } });
            assertTrue(failure.getMessage().contains("foreign ACT_/FLW_"));
            assertEquals(9L, jdbc.queryForObject("SELECT ID_ FROM ACT_FOREIGN_TEST", Long.class));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME='ACT_GE_PROPERTY'", Integer.class));
        } finally {
            jdbc.execute("DROP TABLE ACT_FOREIGN_TEST");
        }
        try (var next = open()) { assertNotNull(next.engine()); }
        assertNoSchema();
    }

    @ParameterizedTest @ValueSource(strings = {"common", "engine"})
    void realMidScriptFailurePreservesOriginalCauseAndDropsOnlyOwnedObjects(String component) {
        var cause = new SQLException("injected real DDL execute failure", "TEST1", 930);
        List<Engine> built = new ArrayList<>();
        var hooks = new MesEdhrFlowableTestSchema.FailureHooks() {
            @Override public void beforeSql(String script, String sql) throws SQLException {
                if (script.endsWith("create." + component + ".sql") && sql.toLowerCase(java.util.Locale.ROOT).startsWith("create table "
                        + (component.equals("common") ? "act_ru_task" : "act_re_procdef"))) throw cause;
            }
            @Override public void engineBuilt(Engine engine) { built.add(engine); }
        };
        RuntimeException failure = assertThrows(RuntimeException.class, () -> { try (var unexpected = new NegativeLifetime(open(hooks))) { fail("owned open unexpectedly succeeded"); } });
        assertTrue(contains(failure, cause));
        assertTrue(built.isEmpty());
        assertNoSchema();
        try (var next = open()) { assertNotNull(next.engine()); }
    }

    @Test void childBuiltThenParentBuildFailureReleasesExactOwnedChildInstances() {
        var cause = new IllegalStateException("injected parent schema-build command failure");
        List<Engine> built = new ArrayList<>();
        Map<String, IdmEngine> previousIdm = new HashMap<>(IdmEngines.getIdmEngines());
        Map<String, EventRegistryEngine> previousEvent = new HashMap<>(EventRegistryEngines.getEventRegistryEngines());
        var hooks = new MesEdhrFlowableTestSchema.FailureHooks() {
            @Override public void beforeCommand(String stage) {
                if (stage.equals("SchemaOperationsEngineBuild")) {
                    // Official configurators defer child lifecycle callbacks until the parent is built.
                    // Observe actual registered children at this earlier failing stage; cleanup still
                    // uses the helper's exact pre-owned config/name identities, never registry deltas.
                    IdmEngines.getIdmEngines().forEach((name, engine) -> { if (!previousIdm.containsKey(name)) built.add(engine); });
                    EventRegistryEngines.getEventRegistryEngines().forEach((name, engine) -> { if (!previousEvent.containsKey(name)) built.add(engine); });
                    assertTrue(built.stream().anyMatch(IdmEngine.class::isInstance));
                    assertTrue(built.stream().anyMatch(EventRegistryEngine.class::isInstance));
                    throw cause;
                }
            }
        };
        assertSame(cause, assertThrows(IllegalStateException.class, () -> { try (var unexpected = new NegativeLifetime(open(hooks))) { fail("owned open unexpectedly succeeded"); } }));
        assertEquals(2, built.size());
        for (Engine engine : built) assertUnregistered(engine);
        assertNoSchema();
        try (var next = open()) { assertNotNull(next.engine()); }
    }

    @Test void actualDeploymentParseFailureRetainsCauseAndReleasesBuiltResources() {
        var owned = open();
        try (var lifetime = new NegativeLifetime(owned)) {
            RuntimeException failure = assertThrows(RuntimeException.class, () -> owned.deploy("<definitions><broken>"));
            Throwable originalCause = failure.getCause();
            owned.closeAfterFailure(failure);
            assertSame(originalCause, failure.getCause());
            assertEquals(0, failure.getSuppressed().length);
            assertUnregistered(owned.engine());
            assertNoSchema();
            try (var next = open()) { assertNotNull(next.engine()); }
        }
    }

    @Test void actualStartCommandFailureAfterDeploymentReleasesResourcesAndRetainsFirstCause() {
        var cause = new IllegalStateException("injected actual start command failure");
        boolean[] failCommand = {true};
        var hooks = new MesEdhrFlowableTestSchema.FailureHooks() {
            @Override public void beforeCommand(String stage) { if (failCommand[0] && stage.equals("StartProcessInstanceCmd")) throw cause; }
        };
        var owned = open(hooks);
        try (var lifetime = new NegativeLifetime(owned, () -> failCommand[0] = false)) {
            var deployment = owned.deploy(BPMN);
            var definition = owned.engine().getRepositoryService().createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
            assertSame(cause, assertThrows(IllegalStateException.class, () -> owned.start(definition.getId())));
            owned.closeAfterFailure(cause);
            assertEquals(0, cause.getSuppressed().length);
            assertUnregistered(owned.engine());
            assertNoSchema();
        }
    }

    @Test void actualDeleteDeploymentFailureStillClosesAllEnginesAndDropsSchema() {
        var cause = new IllegalStateException("injected actual delete command failure");
        boolean[] failCommand = {true};
        var hooks = new MesEdhrFlowableTestSchema.FailureHooks() {
            @Override public void beforeCommand(String stage) { if (failCommand[0] && stage.equals("DeleteDeploymentCmd")) throw cause; }
        };
        var owned = open(hooks);
        try (var lifetime = new NegativeLifetime(owned, () -> failCommand[0] = false)) {
            owned.deploy(BPMN);
            var failure = assertThrows(IllegalStateException.class, owned::close);
            assertTrue(contains(failure, cause));
            assertUnregistered(owned.engine());
            assertNoSchema();
            assertThrows(IllegalStateException.class, owned::close);
            try (var next = open()) { assertNotNull(next.engine()); }
        }
    }

    @Test void actualDropFailureIsVisibleAndExplicitRecoveryOnlyTouchesRemainingOwnedSchema() {
        var cause = new SQLException("injected actual drop failure", "TEST2", 931);
        boolean[] failDrop = {true};
        var hooks = new MesEdhrFlowableTestSchema.FailureHooks() {
            @Override public void beforeSql(String script, String sql) throws SQLException {
                if (failDrop[0] && script.endsWith("drop.common.sql") && sql.toUpperCase(java.util.Locale.ROOT).contains("ACT_GE_PROPERTY")) throw cause;
            }
        };
        var owned = open(hooks);
        try (var lifetime = new NegativeLifetime(owned, () -> failDrop[0] = false)) {
            var failure = assertThrows(IllegalStateException.class, owned::close);
            assertTrue(contains(failure, cause));
            assertUnregistered(owned.engine());
            assertEquals(1, new JdbcTemplate(dataSource).queryForObject(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME='ACT_GE_PROPERTY'", Integer.class));
            assertThrows(IllegalStateException.class, () -> { try (var unexpected = new NegativeLifetime(open())) { fail("owned open unexpectedly succeeded"); } });
            failDrop[0] = false;
            owned.retryOwnedSchemaCleanup();
            assertNoSchema();
            assertThrows(IllegalStateException.class, owned::close); // Historical close failure must remain visible.
            try (var next = open()) { assertNotNull(next.engine()); }
        }
    }

    @Test void actualEngineCloseCommandFailureStillAttemptsConfigurationAndChildRelease() {
        var cause = new IllegalStateException("injected process close command failure");
        boolean[] failCommand = {true};
        var hooks = new MesEdhrFlowableTestSchema.FailureHooks() {
            @Override public void beforeCommand(String stage) { if (failCommand[0] && stage.equals("SchemaOperationProcessEngineClose")) throw cause; }
        };
        var owned = open(hooks);
        try (var lifetime = new NegativeLifetime(owned, () -> failCommand[0] = false)) {
            RuntimeException failure = assertThrows(RuntimeException.class, owned::close);
            assertTrue(contains(failure, cause));
            assertUnregistered(owned.engine());
            assertNull(IdmEngines.getIdmEngines().get(owned.idmConfig.getEngineName()));
            assertNull(EventRegistryEngines.getEventRegistryEngines().get(owned.eventConfig.getEngineName()));
            assertTrue(owned.configurationsClosed());
            assertNoSchema();
            assertThrows(IllegalStateException.class, owned::close);
            try (var next = open()) { assertNotNull(next.engine()); }
        }
    }

    @Test void startCauseSurvivesWithActualCleanupFailureSuppressed() {
        var firstCause = new IllegalStateException("injected start failure with failed cleanup");
        var cleanupCause = new SQLException("injected cleanup SQL failure", "TEST3", 932);
        boolean[] failDrop = {true};
        boolean[] failCommand = {true};
        var hooks = new MesEdhrFlowableTestSchema.FailureHooks() {
            @Override public void beforeCommand(String stage) { if (failCommand[0] && stage.equals("StartProcessInstanceCmd")) throw firstCause; }
            @Override public void beforeSql(String script, String sql) throws SQLException {
                if (failDrop[0] && script.endsWith("drop.common.sql") && sql.toUpperCase(java.util.Locale.ROOT).contains("ACT_GE_PROPERTY")) throw cleanupCause;
            }
        };
        var owned = open(hooks);
        try (var lifetime = new NegativeLifetime(owned, () -> failCommand[0] = false, () -> failDrop[0] = false)) {
            var deployment = owned.deploy(BPMN);
            var definition = owned.engine().getRepositoryService().createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
            assertSame(firstCause, assertThrows(IllegalStateException.class, () -> owned.start(definition.getId())));
            owned.closeAfterFailure(firstCause);
            assertEquals(1, firstCause.getSuppressed().length);
            assertTrue(contains(firstCause.getSuppressed()[0], cleanupCause));
            assertUnregistered(owned.engine());
            failDrop[0] = false;
            owned.retryOwnedSchemaCleanup();
            assertNoSchema();
        }
    }

    @Test void mesMapperAndFlowableCommandUseOneSpringConnectionAndRollbackTogether() {
        try (var harness = new MesEdhrDomainAssignmentFlowableTest.Harness(dataSource, ownershipMapper, executionMapper)) {
            harness.insertDomainFacts("APPROVE", "99,100");
            harness.tx.executeWithoutResult(status -> {
                assertNotNull(executionMapper.selectById(301L));
                var holder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
                assertNotNull(holder);
                Connection connection = holder.getConnection();
                harness.engine.getManagementService().executeCommand(context -> {
                    var sql = context.getSession(org.flowable.common.engine.impl.db.DbSqlSession.class);
                    assertSame(connection, DataSourceUtils.getTargetConnection(sql.getSqlSession().getConnection()));
                    assertSame(holder, TransactionSynchronizationManager.getResource(dataSource));
                    return null;
                });
                harness.jdbc.update("UPDATE mes_pro_batch_record_execution SET execution_code='must-rollback' WHERE id=301");
                harness.engine.getTaskService().setAssignee(harness.taskId, "99");
                status.setRollbackOnly();
            });
            assertEquals("flowable", harness.jdbc.queryForObject("SELECT execution_code FROM mes_pro_batch_record_execution WHERE id=301", String.class));
            assertEquals("88", harness.engine.getTaskService().createTaskQuery().taskId(harness.taskId).singleResult().getAssignee());
            assertFalse(TransactionSynchronizationManager.hasResource(dataSource));
        }
        assertNoSchema();
    }

    @Test void originalFullDropsRetainLaterBytearrayStatementAfterOneOwnedPrelude() throws SQLException {
        List<String> executed = new ArrayList<>();
        var hooks = new MesEdhrFlowableTestSchema.FailureHooks() {
            @Override public void afterSql(String script, String sql) { if (isDrop(script)) executed.add(script + ":" + sql); }
        };
        Map<String, ProcessEngine> processes = new HashMap<>(ProcessEngines.getProcessEngines());
        Map<String, IdmEngine> identities = new HashMap<>(IdmEngines.getIdmEngines());
        Map<String, EventRegistryEngine> events = new HashMap<>(EventRegistryEngines.getEventRegistryEngines());
        Map<Object, Object> resources = TransactionSynchronizationManager.getResourceMap();
        var jdbc = new JdbcTemplate(dataSource);
        jdbc.update("INSERT INTO mes_pro_batch_record_execution(id,execution_code,work_order_id,work_order_code,batch_code,sheet_layout_json,status,revision_no,tenant_id,deleted) VALUES(930002,'prelude-sentinel',1,'WO','B','{}',1,1,1,0)");
        try (var owned = open(hooks)) {
            owned.deploy(BPMN);
        }
        int prelude = -1, commonFirst = -1, originalBytearray = -1;
        int preludeCount = 0;
        for (int i = 0; i < executed.size(); i++) {
            String sql = executed.get(i);
            if (sql.startsWith(MesEdhrFlowableTestSchema.OWNED_PRELUDE + ":")) { prelude = i; preludeCount++; }
            if (sql.contains("drop.common.sql:")) {
                if (commonFirst < 0) commonFirst = i;
                if (sql.endsWith("drop table if exists ACT_GE_BYTEARRAY cascade constraints")) originalBytearray = i;
            }
        }
        assertEquals(1, preludeCount);
        assertTrue(prelude > 0 && prelude < commonFirst && originalBytearray > commonFirst);
        for (String component : List.of("eventregistry", "engine", "history", "identity")) {
            assertTrue(executed.subList(0, prelude).stream().anyMatch(sql -> sql.contains("drop." + component + ".sql:")));
        }
        assertNoSchema();
        assertEquals(processes, ProcessEngines.getProcessEngines());
        assertEquals(identities, IdmEngines.getIdmEngines());
        assertEquals(events, EventRegistryEngines.getEventRegistryEngines());
        assertEquals(resources, TransactionSynchronizationManager.getResourceMap());
        assertEquals("prelude-sentinel", jdbc.queryForObject("SELECT execution_code FROM mes_pro_batch_record_execution WHERE id=930002", String.class));
        try (var next = open()) { assertNotNull(next.engine()); }
        assertNoSchema();
    }

    @Test void actualPreludeSqlFailurePreservesCauseAndFullCommonAttemptThenExplicitRecovery() {
        var cause = new SQLException("injected prelude SQL failure", "TEST4", 933);
        boolean[] failPrelude = {true};
        int[] reached = {0}, commonStatements = {0};
        var hooks = new MesEdhrFlowableTestSchema.FailureHooks() {
            @Override public void beforeSql(String script, String sql) throws SQLException {
                if (script.equals(MesEdhrFlowableTestSchema.OWNED_PRELUDE)) { reached[0]++; if (failPrelude[0]) throw cause; }
                if (script.endsWith("drop.common.sql")) commonStatements[0]++;
            }
        };
        var owned = open(hooks);
        try (var lifetime = new NegativeLifetime(owned, () -> failPrelude[0] = false)) {
            var failure = assertThrows(IllegalStateException.class, owned::close);
            assertTrue(contains(failure, cause));
            assertTrue(containsStage(failure, "owned bytearray prelude"));
            assertEquals(1, reached[0]);
            assertTrue(commonStatements[0] >= 30); // Full original resource reaches its actual FK-owned index fault.
            assertTrue(owned.configurationsClosed());
            assertUnregistered(owned.engine());
            assertThrows(IllegalStateException.class, owned::close);
            failPrelude[0] = false;
            owned.retryOwnedSchemaCleanup();
            assertEquals(2, reached[0]);
            assertNoSchema();
            assertThrows(IllegalStateException.class, owned::close);
            try (var next = open()) { assertNotNull(next.engine()); }
        }
    }

    @Test void actualPreludePostExecuteFailureRemainsFailedEvenWhenFullDropCompletes() {
        var cause = new SQLException("injected post-execute prelude failure", "TEST5", 934);
        int[] reached = {0}, originalBytearray = {0};
        boolean[] failPrelude = {true};
        var hooks = new MesEdhrFlowableTestSchema.FailureHooks() {
            @Override public void afterSql(String script, String sql) throws SQLException {
                if (script.equals(MesEdhrFlowableTestSchema.OWNED_PRELUDE)) { reached[0]++; if (failPrelude[0]) throw cause; }
                if (script.endsWith("drop.common.sql") && sql.endsWith("ACT_GE_BYTEARRAY cascade constraints")) originalBytearray[0]++;
            }
        };
        var owned = open(hooks);
        try (var lifetime = new NegativeLifetime(owned, () -> failPrelude[0] = false)) {
            var failure = assertThrows(IllegalStateException.class, owned::close);
            assertTrue(contains(failure, cause));
            assertTrue(containsStage(failure, "owned bytearray prelude"));
            assertEquals(1, reached[0]);
            assertEquals(1, originalBytearray[0]);
            assertNoSchema();
            assertThrows(IllegalStateException.class, () -> { try (var unexpected = new NegativeLifetime(open())) { fail("owned open unexpectedly succeeded"); } }); // Failed cleanup history holds reservation until explicit recovery.
            owned.retryOwnedSchemaCleanup();
            assertThrows(IllegalStateException.class, owned::close);
            try (var next = open()) { assertNotNull(next.engine()); }
        }
    }

    @Test void originalBusinessStartCauseKeepsPreludeFailureSuppressedThroughRecovery() {
        var first = new IllegalStateException("injected original business start failure");
        var cleanup = new SQLException("injected business cleanup prelude failure", "TEST6", 935);
        boolean[] failPrelude = {true};
        int[] reached = {0};
        boolean[] failCommand = {true};
        var hooks = new MesEdhrFlowableTestSchema.FailureHooks() {
            @Override public void beforeCommand(String stage) { if (failCommand[0] && stage.equals("StartProcessInstanceCmd")) throw first; }
            @Override public void beforeSql(String script, String sql) throws SQLException {
                if (script.equals(MesEdhrFlowableTestSchema.OWNED_PRELUDE)) { reached[0]++; if (failPrelude[0]) throw cleanup; }
            }
        };
        var owned = open(hooks);
        try (var lifetime = new NegativeLifetime(owned, () -> failCommand[0] = false, () -> failPrelude[0] = false)) {
            var deployment = owned.deploy(BPMN);
            var definition = owned.engine().getRepositoryService().createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
            assertSame(first, assertThrows(IllegalStateException.class, () -> owned.start(definition.getId())));
            owned.closeAfterFailure(first);
            assertEquals(1, reached[0]);
            assertEquals(1, first.getSuppressed().length);
            assertTrue(contains(first.getSuppressed()[0], cleanup));
            assertTrue(containsStage(first.getSuppressed()[0], "owned bytearray prelude"));
            failPrelude[0] = false;
            owned.retryOwnedSchemaCleanup();
            assertNoSchema();
            assertThrows(IllegalStateException.class, owned::close);
            try (var next = open()) { assertNotNull(next.engine()); }
        }
    }

    @ParameterizedTest
    @CsvSource({"ACT_GE_BYTEARRAY,fk", "ACT_RE_DEPLOYMENT,fk", "FLW_EVENT_DEPLOYMENT,fk", "ACT_GE_BYTEARRAY,view", "ACT_RE_DEPLOYMENT,synonym"})
    void foreignDependencyRefusalPreventsEveryDatabaseDeletionAndRecoveryUntilOwnerRemovesIt(String target, String kind) throws SQLException {
        List<String> deletionHooks = new ArrayList<>();
        var hooks = new MesEdhrFlowableTestSchema.FailureHooks() {
            @Override public void beforeSql(String script, String sql) { if (isDrop(script)) deletionHooks.add(script); }
            @Override public void beforeCommand(String stage) { if (stage.equals("DeleteDeploymentCmd") || stage.equals("SchemaOperationProcessEngineClose")) deletionHooks.add(stage); }
        };
        var owned = open(hooks);
        try (var lifetime = new NegativeLifetime(owned)) {
            var deployment = owned.deploy(BPMN);
            var jdbc = new JdbcTemplate(dataSource);
            jdbc.update("INSERT INTO " + target + "(ID_) VALUES('foreign-target')");
            String schema;
            try (Connection connection = dataSource.getConnection()) { schema = connection.getSchema(); }
            String qualifiedTarget = '"' + schema.replace("\"", "\"\"") + "\".\"" + target + '"';
            createForeignSchema(lifetime, jdbc);
            String definition;
            if (kind.equals("fk")) {
                createForeignDependent(lifetime, jdbc, kind, "CREATE TABLE R2_FOREIGN.FOREIGN_DEPENDENT(ID_ BIGINT PRIMARY KEY, REF_ VARCHAR(255), CONSTRAINT R2_FOREIGN_FK FOREIGN KEY(REF_) REFERENCES " + qualifiedTarget + "(ID_))", () -> {});
                jdbc.update("INSERT INTO R2_FOREIGN.FOREIGN_DEPENDENT VALUES(7,'foreign-target')");
                definition = jdbc.queryForObject("SELECT CONSTRAINT_TYPE FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA='R2_FOREIGN' AND CONSTRAINT_NAME='R2_FOREIGN_FK'", String.class);
            } else if (kind.equals("view")) {
                createForeignDependent(lifetime, jdbc, kind, "CREATE VIEW R2_FOREIGN.FOREIGN_DEPENDENT AS SELECT ID_ FROM " + qualifiedTarget, () -> {});
                definition = jdbc.queryForObject("SELECT VIEW_DEFINITION FROM INFORMATION_SCHEMA.VIEWS WHERE TABLE_SCHEMA='R2_FOREIGN' AND TABLE_NAME='FOREIGN_DEPENDENT'", String.class);
            } else {
                createForeignDependent(lifetime, jdbc, kind, "CREATE SYNONYM R2_FOREIGN.FOREIGN_DEPENDENT FOR " + qualifiedTarget, () -> {});
                definition = jdbc.queryForObject("SELECT SYNONYM_FOR_SCHEMA || '.' || SYNONYM_FOR FROM INFORMATION_SCHEMA.SYNONYMS WHERE SYNONYM_SCHEMA='R2_FOREIGN' AND SYNONYM_NAME='FOREIGN_DEPENDENT'", String.class);
        }
            List<String> dependencyDefinition = foreignDefinition();
            long rows = jdbc.queryForObject("SELECT COUNT(*) FROM R2_FOREIGN.FOREIGN_DEPENDENT", Long.class);
            var failure = assertThrows(IllegalStateException.class, owned::close);
            assertTrue(containsStage(failure, "ownership/dependency check before database deletion"));
            assertTrue(deletionHooks.isEmpty());
            assertTrue(owned.configurationsClosed());
            assertUnregistered(owned.engine());
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM ACT_RE_DEPLOYMENT WHERE ID_=?", Integer.class, deployment.getId()));
            assertEquals(rows, jdbc.queryForObject("SELECT COUNT(*) FROM R2_FOREIGN.FOREIGN_DEPENDENT", Long.class));
            assertEquals(dependencyDefinition, foreignDefinition());
            if (kind.equals("fk")) {
                assertEquals(definition, jdbc.queryForObject("SELECT CONSTRAINT_TYPE FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA='R2_FOREIGN' AND CONSTRAINT_NAME='R2_FOREIGN_FK'", String.class));
                assertEquals("foreign-target", jdbc.queryForObject("SELECT REF_ FROM R2_FOREIGN.FOREIGN_DEPENDENT WHERE ID_=7", String.class));
                assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM INFORMATION_SCHEMA.INDEXES WHERE TABLE_SCHEMA='R2_FOREIGN' AND TABLE_NAME='FOREIGN_DEPENDENT' AND INDEX_TYPE_NAME='INDEX'", Integer.class));
            } else if (kind.equals("view")) {
                assertEquals(definition, jdbc.queryForObject("SELECT VIEW_DEFINITION FROM INFORMATION_SCHEMA.VIEWS WHERE TABLE_SCHEMA='R2_FOREIGN' AND TABLE_NAME='FOREIGN_DEPENDENT'", String.class));
            } else {
                assertEquals(definition, jdbc.queryForObject("SELECT SYNONYM_FOR_SCHEMA || '.' || SYNONYM_FOR FROM INFORMATION_SCHEMA.SYNONYMS WHERE SYNONYM_SCHEMA='R2_FOREIGN' AND SYNONYM_NAME='FOREIGN_DEPENDENT'", String.class));
            }
            var retry = assertThrows(IllegalStateException.class, owned::retryOwnedSchemaCleanup);
            assertTrue(containsStage(retry, "ownership/dependency check before explicit recovery"));
            assertTrue(deletionHooks.isEmpty());
            assertEquals(rows, jdbc.queryForObject("SELECT COUNT(*) FROM R2_FOREIGN.FOREIGN_DEPENDENT", Long.class));
            assertEquals(dependencyDefinition, foreignDefinition());
            assertThrows(IllegalStateException.class, owned::close);
        }
        assertNoSchema();
        assertThrows(IllegalStateException.class, owned::close);
        try (var next = open()) { assertNotNull(next.engine()); }
    }

    @Test void intentionalAssertionFailureStillReleasesOwnedLifetimeAndPreservesFirstFailure() {
        var jdbc = new JdbcTemplate(dataSource);
        jdbc.update("INSERT INTO mes_pro_batch_record_execution(id,execution_code,work_order_id,work_order_code,batch_code,sheet_layout_json,status,revision_no,tenant_id,deleted) VALUES(930003,'assertion-sentinel',1,'WO','B','{}',1,1,1,0)");
        Map<String, ProcessEngine> processes = new HashMap<>(ProcessEngines.getProcessEngines());
        Map<String, IdmEngine> identities = new HashMap<>(IdmEngines.getIdmEngines());
        Map<String, EventRegistryEngine> events = new HashMap<>(EventRegistryEngines.getEventRegistryEngines());
        Map<Object, Object> resources = TransactionSynchronizationManager.getResourceMap();
        var first = new AssertionError("intentional assertion after real deployment");
        var owned = open();
        try (var recovery = new NegativeLifetime(owned)) {
            assertSame(first, assertThrows(AssertionError.class, () -> {
                try (var lifetime = new NegativeLifetime(owned)) {
                    owned.deploy(BPMN);
                    throw first;
                }
            }));
            assertEquals(0, first.getSuppressed().length);
            assertTrue(owned.configurationsClosed());
            assertUnregistered(owned.engine());
            assertNoSchema();
        }

        var cleanup = new SQLException("injected assertion cleanup failure", "TEST7", 936);
        boolean[] failDrop = {true};
        var hooks = new MesEdhrFlowableTestSchema.FailureHooks() {
            @Override public void beforeSql(String script, String sql) throws SQLException {
                if (failDrop[0] && script.endsWith("drop.common.sql")
                        && sql.toUpperCase(java.util.Locale.ROOT).contains("ACT_GE_PROPERTY")) throw cleanup;
            }
        };
        var failed = open(hooks);
        try (var recovery = new NegativeLifetime(failed, () -> failDrop[0] = false)) {
            var original = new AssertionError("intentional assertion with real cleanup failure");
            assertSame(original, assertThrows(AssertionError.class, () -> {
                try (var lifetime = new NegativeLifetime(failed)) {
                    failed.deploy(BPMN);
                    throw original;
                }
            }));
            assertEquals(1, original.getSuppressed().length);
            assertTrue(contains(original.getSuppressed()[0], cleanup));
            assertEquals(1, original.getSuppressed()[0].getSuppressed().length); // Explicit recovery also failed with fault active.
            assertTrue(contains(original.getSuppressed()[0].getSuppressed()[0], cleanup));
            assertTrue(failed.configurationsClosed());
            assertUnregistered(failed.engine());
            assertTrue(failed.hasReservedCleanupFailure());
            assertThrows(IllegalStateException.class, () -> { try (var unexpected = new NegativeLifetime(open())) { fail("owned open unexpectedly succeeded"); } });
            failDrop[0] = false;
            failed.retryOwnedSchemaCleanup();
            assertNoSchema();
            assertThrows(IllegalStateException.class, failed::close); // Recovery cannot erase the failed close.
        }
        assertEquals(processes, ProcessEngines.getProcessEngines());
        assertEquals(identities, IdmEngines.getIdmEngines());
        assertEquals(events, EventRegistryEngines.getEventRegistryEngines());
        assertEquals(resources, TransactionSynchronizationManager.getResourceMap());
        assertEquals("assertion-sentinel", jdbc.queryForObject("SELECT execution_code FROM mes_pro_batch_record_execution WHERE id=930003", String.class));
        try (var next = open()) { assertNotNull(next.engine()); }
        assertNoSchema();
    }

    @Test void partialForeignSetupFailureCleansOnlyCreatedSentinelsBeforeOwnedRecovery() {
        var jdbc = new JdbcTemplate(dataSource);
        jdbc.update("INSERT INTO mes_pro_batch_record_execution(id,execution_code,work_order_id,work_order_code,batch_code,sheet_layout_json,status,revision_no,tenant_id,deleted) VALUES(930004,'partial-setup-sentinel',1,'WO','B','{}',1,1,1,0)");
        Map<String, ProcessEngine> processes = new HashMap<>(ProcessEngines.getProcessEngines());
        Map<String, IdmEngine> identities = new HashMap<>(IdmEngines.getIdmEngines());
        Map<String, EventRegistryEngine> events = new HashMap<>(EventRegistryEngines.getEventRegistryEngines());
        Map<Object, Object> resources = TransactionSynchronizationManager.getResourceMap();
        var first = new IllegalStateException("intentional partial foreign setup failure");
        var owned = open();
        try (var recovery = new NegativeLifetime(owned)) {
            assertSame(first, assertThrows(IllegalStateException.class, () -> {
                try (var lifetime = new NegativeLifetime(owned)) {
                    owned.deploy(BPMN);
                    createForeignSchema(lifetime, jdbc);
                    String target = qualifiedTable("ACT_GE_BYTEARRAY");
                    createForeignDependent(lifetime, jdbc, "view",
                            "CREATE VIEW R2_FOREIGN.FOREIGN_DEPENDENT AS SELECT ID_ FROM " + target, () -> {
                                assertTrue(owned.needsInitialClose());
                                assertFalse(owned.hasReservedCleanupFailure());
                                throw first; // Real CREATE succeeded; caller setup has not resumed.
                            });
                }
            }));
            assertEquals(0, first.getSuppressed().length);
            assertNull(h2(session -> session.getDatabase().findSchema("R2_FOREIGN")));
            assertTrue(owned.configurationsClosed());
            assertUnregistered(owned.engine());
            assertNoSchema();
        }
        assertEquals(processes, ProcessEngines.getProcessEngines());
        assertEquals(identities, IdmEngines.getIdmEngines());
        assertEquals(events, EventRegistryEngines.getEventRegistryEngines());
        assertEquals(resources, TransactionSynchronizationManager.getResourceMap());
        assertEquals("partial-setup-sentinel", jdbc.queryForObject("SELECT execution_code FROM mes_pro_batch_record_execution WHERE id=930004", String.class));
        try (var next = open()) { assertNotNull(next.engine()); }
        assertNoSchema();
    }

    @ParameterizedTest @ValueSource(strings = {"role", "user"})
    void foreignTableRightsRefuseEveryDeletionUntilGrantOwnerRevokes(String kind) {
        List<String> deletionHooks = new ArrayList<>();
        var hooks = new MesEdhrFlowableTestSchema.FailureHooks() {
            @Override public void beforeSql(String script, String sql) { if (isDrop(script)) deletionHooks.add(script); }
            @Override public void beforeCommand(String stage) {
                if (stage.equals("DeleteDeploymentCmd") || stage.equals("SchemaOperationProcessEngineClose")) deletionHooks.add(stage);
            }
        };
        var owned = open(hooks);
        try (var lifetime = new NegativeLifetime(owned)) {
            var jdbc = new JdbcTemplate(dataSource);
            Map<Object, Object> resources = TransactionSynchronizationManager.getResourceMap();
            var deployment = owned.deploy(BPMN);
            jdbc.update("INSERT INTO mes_pro_batch_record_execution(id,execution_code,work_order_id,work_order_code,batch_code,sheet_layout_json,status,revision_no,tenant_id,deleted) VALUES(930005,'right-sentinel',1,'WO','B','{}',1,1,1,0)");
            String target = qualifiedTable("ACT_GE_BYTEARRAY");
            String principalName = kind.equals("role") ? "R4_FOREIGN_ROLE" : "R4_FOREIGN_USER";
            assertNull(h2(session -> session.getDatabase().findUserOrRole(principalName)));
            org.h2.engine.RightOwner[] principalIdentity = {null};
            boolean[] principalAttempted = {false};
            lifetime.track(() -> {
                if (!principalAttempted[0]) return;
                assertNotNull(principalIdentity[0], "principal ownership capture required; do not adopt by name");
                assertSame(principalIdentity[0], h2(session -> session.getDatabase().findUserOrRole(principalName)));
                assertTrue(this.<Boolean>h2(session -> session.getDatabase().getAllRights().stream()
                        .noneMatch(candidate -> candidate.getGrantee() == principalIdentity[0])));
                jdbc.execute("DROP " + (kind.equals("role") ? "ROLE " : "USER ") + principalName);
            });
            jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) connection -> {
                principalAttempted[0] = true;
                var session = (org.h2.engine.SessionLocal) connection.unwrap(org.h2.jdbc.JdbcConnection.class).getSession();
                try (var statement = connection.createStatement()) {
                    statement.execute(kind.equals("role") ? "CREATE ROLE " + principalName : "CREATE USER " + principalName + " PASSWORD ''");
                    principalIdentity[0] = session.getDatabase().findUserOrRole(principalName);
                }
                return null;
            });
            var principal = principalIdentity[0];
            var table = h2(session -> session.getDatabase().getSchema(session.getCurrentSchemaName()).findTableOrView(session, "ACT_GE_BYTEARRAY"));
            org.h2.engine.Right[] rightIdentity = {null};
            boolean[] grantAttempted = {false}, revoked = {false};
            lifetime.track(() -> {
                if (grantAttempted[0] && !revoked[0]) {
                    assertNotNull(rightIdentity[0], "grant ownership capture required; do not adopt by name");
                    assertRightIntact(rightIdentity[0], table, principal, rightIdentity[0].getCreateSQL());
                    jdbc.execute("REVOKE SELECT ON " + target + " FROM " + principalName);
                    revoked[0] = true;
                }
            });
            jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) connection -> {
                grantAttempted[0] = true;
                var session = (org.h2.engine.SessionLocal) connection.unwrap(org.h2.jdbc.JdbcConnection.class).getSession();
                try (var statement = connection.createStatement()) {
                    statement.execute("GRANT SELECT ON " + target + " TO " + principalName);
                    rightIdentity[0] = session.getDatabase().getAllRights().stream()
                            .filter(candidate -> candidate.getGrantedObject() == table && candidate.getGrantee() == principal)
                            .findFirst().orElseThrow();
                }
                return null;
            });
            var right = rightIdentity[0];
            assertEquals(org.h2.engine.Right.SELECT, right.getRightMask());
            long rows = jdbc.queryForObject("SELECT COUNT(*) FROM ACT_GE_BYTEARRAY", Long.class);
            String definition = table.getCreateSQL();
            String rightDefinition = right.getCreateSQL();
            var failure = assertThrows(IllegalStateException.class, owned::close);
            assertTrue(containsStage(failure, "ownership/dependency check before database deletion"));
            assertTrue(containsStage(failure, "foreign table right on owned target"));
            assertTrue(deletionHooks.isEmpty());
            assertTrue(owned.configurationsClosed());
            assertUnregistered(owned.engine());
            assertNull(IdmEngines.getIdmEngines().get(owned.idmConfig.getEngineName()));
            assertNull(EventRegistryEngines.getEventRegistryEngines().get(owned.eventConfig.getEngineName()));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM ACT_RE_DEPLOYMENT WHERE ID_=?", Integer.class, deployment.getId()));
            assertEquals(rows, jdbc.queryForObject("SELECT COUNT(*) FROM ACT_GE_BYTEARRAY", Long.class));
            assertSame(table, h2(session -> session.getDatabase().getSchema(session.getCurrentSchemaName()).findTableOrView(session, "ACT_GE_BYTEARRAY")));
            assertEquals(definition, table.getCreateSQL());
            assertRightIntact(right, table, principal, rightDefinition);
            var retry = assertThrows(IllegalStateException.class, owned::retryOwnedSchemaCleanup);
            assertTrue(containsStage(retry, "ownership/dependency check before explicit recovery"));
            assertTrue(deletionHooks.isEmpty());
            assertEquals(rows, jdbc.queryForObject("SELECT COUNT(*) FROM ACT_GE_BYTEARRAY", Long.class));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM ACT_RE_DEPLOYMENT WHERE ID_=?", Integer.class, deployment.getId()));
            assertRightIntact(right, table, principal, rightDefinition);
            assertThrows(IllegalStateException.class, owned::close);
            assertThrows(IllegalStateException.class, () -> { try (var unexpected = new NegativeLifetime(open())) { fail("owned open unexpectedly succeeded"); } });
            assertEquals(resources, TransactionSynchronizationManager.getResourceMap());
            assertEquals("right-sentinel", jdbc.queryForObject("SELECT execution_code FROM mes_pro_batch_record_execution WHERE id=930005", String.class));
            // Only the sentinel owner revokes its exact grant; the fixture must never revoke it.
            assertSame(principal, h2(session -> session.getDatabase().findUserOrRole(principalName)));
            jdbc.execute("REVOKE SELECT ON " + target + " FROM " + principalName);
            revoked[0] = true;
            owned.retryOwnedSchemaCleanup();
            assertNoSchema();
            assertSame(principal, h2(session -> session.getDatabase().findUserOrRole(principalName)));
            assertThrows(IllegalStateException.class, owned::close);
            assertEquals("right-sentinel", jdbc.queryForObject("SELECT execution_code FROM mes_pro_batch_record_execution WHERE id=930005", String.class));
            assertEquals(resources, TransactionSynchronizationManager.getResourceMap());
            try (var next = open()) { assertNotNull(next.engine()); }
            assertNoSchema();
        }
    }

    @ParameterizedTest
    @CsvSource({"ACT_EVT_LOG,LOG_NR_,DEFAULT,EXTERNAL", "ACT_EVT_LOG,LOG_NR_,ON_UPDATE,EXTERNAL",
            "ACT_HI_TSK_LOG,ID_,DEFAULT,EXTERNAL", "ACT_HI_TSK_LOG,ID_,ON_UPDATE,EXTERNAL",
            "ACT_EVT_LOG,LOG_NR_,DEFAULT,MES", "ACT_EVT_LOG,LOG_NR_,ON_UPDATE,MES",
            "ACT_HI_TSK_LOG,ID_,DEFAULT,MES", "ACT_HI_TSK_LOG,ID_,ON_UPDATE,MES"})
    void foreignSequenceExpressionsRefuseEveryDeletionUntilOwnerRemovesDependency(
            String table, String column, String expression, String location) {
        List<String> deletionHooks = new ArrayList<>();
        Map<String, ProcessEngine> processes = new HashMap<>(ProcessEngines.getProcessEngines());
        Map<String, IdmEngine> identities = new HashMap<>(IdmEngines.getIdmEngines());
        Map<String, EventRegistryEngine> events = new HashMap<>(EventRegistryEngines.getEventRegistryEngines());
        Map<Object, Object> resources = TransactionSynchronizationManager.getResourceMap();
        var owned = open(sequenceDeletionHooks(deletionHooks));
        try (var lifetime = new NegativeLifetime(owned)) {
            var jdbc = new JdbcTemplate(dataSource);
            insertSequenceMesSentinel(jdbc);
            var deployment = owned.deploy(BPMN);
            var definition = owned.engine().getRepositoryService().createProcessDefinitionQuery()
                    .deploymentId(deployment.getId()).singleResult();
            var process = owned.start(definition.getId());
            assertEquals(1, owned.engine().getTaskService().createTaskQuery().processInstanceId(process.getId()).count());
            var sequence = actualIdentitySequence(owned, table, column);
            var sentinel = createSequenceSentinel(lifetime, jdbc, sequence, expression, location, () -> {});
            sentinel.assertExpressionDependsOn(sequence);
            var sequenceState = sequenceState(sequence);
            Set<org.h2.schema.SchemaObject> recorded = owned.recordedOwnedObjects();
            assertTrue(recorded.contains(sequence));
            assertThrows(UnsupportedOperationException.class, recorded::clear);
            List<Map<String, Object>> mesRows = sequenceMesRows(jdbc);
            Map<String, List<Map<String, Object>>> businessRows = sequenceBusinessRows(jdbc);
            var failure = assertThrows(IllegalStateException.class, owned::close);
            assertTrue(containsStage(failure, "ownership/dependency check before database deletion"));
            assertTrue(containsStage(failure, "foreign table expression dependency on owned sequence"));
            Throwable firstCause = failure.getCause();
            List<Throwable> historicalSuppressed = List.of(firstCause.getSuppressed());
            assertTrue(owned.configurationsClosed());
            assertEquals(processes, ProcessEngines.getProcessEngines());
            assertEquals(identities, IdmEngines.getIdmEngines());
            assertEquals(events, EventRegistryEngines.getEventRegistryEngines());
            for (int attempt = 0; attempt < 3; attempt++) {
                if (attempt > 0) {
                    var recoveryFailure = assertThrows(IllegalStateException.class, owned::retryOwnedSchemaCleanup);
                    assertTrue(containsStage(recoveryFailure, "ownership/dependency check before explicit recovery"));
                    assertTrue(containsStage(recoveryFailure, "foreign table expression dependency on owned sequence"));
                }
                assertTrue(deletionHooks.isEmpty());
                assertRecordedPresence(recorded, true);
                sentinel.assertIntact();
                assertSame(sequence, h2(session -> sequence.getSchema().findSequence(sequence.getName())));
                assertEquals(sequenceState, sequenceState(sequence));
                assertEquals(businessRows, sequenceBusinessRows(jdbc));
                assertEquals(mesRows, sequenceMesRows(jdbc));
                assertEquals(resources, TransactionSynchronizationManager.getResourceMap());
                assertTrue(owned.hasReservedCleanupFailure());
                var reservedFailure = assertThrows(IllegalStateException.class,
                        () -> { try (var unexpected = new NegativeLifetime(open())) { fail("reservation was released"); } });
                assertTrue(containsStage(reservedFailure, "MES datasource already has an owned Flowable harness"));
                assertTrue(contains(assertThrows(IllegalStateException.class, owned::close), firstCause));
            }
            sentinel.removeExpressionDependency(); // Only the exact sentinel owner may alter its table.
            sentinel.assertNoExpressionDependency(sequence);
            assertEquals(sequenceState, sequenceState(sequence));
            assertEquals(mesRows, sequenceMesRows(jdbc));
            owned.retryOwnedSchemaCleanup();
            assertRecordedPresence(recorded, false);
            assertFalse(liveCatalog().contains(sequence));
            assertNoSchema();
            sentinel.assertIntact();
            assertFalse(owned.hasReservedCleanupFailure());
            assertEquals(mesRows, sequenceMesRows(jdbc));
            assertEquals(resources, TransactionSynchronizationManager.getResourceMap());
            assertSame(firstCause, assertThrows(IllegalStateException.class, owned::close).getCause());
            assertEquals(historicalSuppressed, List.of(firstCause.getSuppressed()));
            try (var next = open()) { assertNotNull(next.engine()); }
            assertNoSchema();
            sentinel.assertIntact();
        }
        assertEquals(processes, ProcessEngines.getProcessEngines());
        assertEquals(identities, IdmEngines.getIdmEngines());
        assertEquals(events, EventRegistryEngines.getEventRegistryEngines());
        assertEquals(resources, TransactionSynchronizationManager.getResourceMap());
        assertNull(h2(session -> session.getDatabase().findSchema("R2_FOREIGN")));
        assertNull(h2(session -> session.getDatabase().getSchema(session.getCurrentSchemaName())
                .findTableOrView(session, "MES_R5_SEQUENCE_DEPENDENT")));
    }

    @ParameterizedTest @ValueSource(strings = {"DEFAULT", "ON_UPDATE"})
    void sequenceSentinelSetupAssertionRetainsHistoryAndReleasesOnlyOwnedLifetime(String expression) {
        List<String> deletionHooks = new ArrayList<>();
        Map<String, ProcessEngine> processes = new HashMap<>(ProcessEngines.getProcessEngines());
        Map<String, IdmEngine> identities = new HashMap<>(IdmEngines.getIdmEngines());
        Map<String, EventRegistryEngine> events = new HashMap<>(EventRegistryEngines.getEventRegistryEngines());
        Map<Object, Object> resources = TransactionSynchronizationManager.getResourceMap();
        var owned = open(sequenceDeletionHooks(deletionHooks));
        try (var recovery = new NegativeLifetime(owned)) {
            var first = new AssertionError("intentional sequence sentinel setup assertion");
            var jdbc = new JdbcTemplate(dataSource);
            insertSequenceMesSentinel(jdbc);
            List<Map<String, Object>> mesRows = sequenceMesRows(jdbc);
            owned.deploy(BPMN);
            var sequence = actualIdentitySequence(owned, "ACT_EVT_LOG", "LOG_NR_");
            Set<org.h2.schema.SchemaObject> recorded = owned.recordedOwnedObjects();
            assertSame(first, assertThrows(AssertionError.class, () -> {
                try (var lifetime = new NegativeLifetime(owned)) {
                    createSequenceSentinel(lifetime, jdbc, sequence, expression, "EXTERNAL", () -> {
                        owned.closeAfterFailure(first);
                        assertEquals(1, first.getSuppressed().length);
                        assertTrue(containsStage(first.getSuppressed()[0], "foreign table expression dependency on owned sequence"));
                        var retry = assertThrows(IllegalStateException.class, owned::retryOwnedSchemaCleanup);
                        first.getSuppressed()[0].addSuppressed(retry);
                        assertTrue(containsStage(retry, "ownership/dependency check before explicit recovery"));
                        assertTrue(deletionHooks.isEmpty());
                        assertRecordedPresence(recorded, true);
                        assertTrue(owned.hasReservedCleanupFailure());
                        throw first; // CREATE identity was captured, but caller setup never resumed.
                    });
                }
            }));
            assertEquals(1, first.getSuppressed().length);
            assertEquals(1, first.getSuppressed()[0].getSuppressed().length);
            assertTrue(containsStage(first.getSuppressed()[0].getSuppressed()[0], "foreign table expression dependency on owned sequence"));
            assertTrue(contains(assertThrows(IllegalStateException.class, owned::close), first.getSuppressed()[0].getCause()));
            assertRecordedPresence(recorded, false);
            assertFalse(liveCatalog().contains(sequence));
            assertNull(h2(session -> session.getDatabase().findSchema("R2_FOREIGN")));
            assertFalse(owned.hasReservedCleanupFailure());
            assertEquals(mesRows, sequenceMesRows(jdbc));
            assertTrue(owned.configurationsClosed());
            assertEquals(processes, ProcessEngines.getProcessEngines());
            assertEquals(identities, IdmEngines.getIdmEngines());
            assertEquals(events, EventRegistryEngines.getEventRegistryEngines());
            assertEquals(resources, TransactionSynchronizationManager.getResourceMap());
            try (var next = open()) { assertNotNull(next.engine()); }
            assertNoSchema();
        }
    }

    private MesEdhrFlowableTestSchema.FailureHooks sequenceDeletionHooks(List<String> deletionHooks) {
        return new MesEdhrFlowableTestSchema.FailureHooks() {
            @Override public void beforeSql(String script, String sql) { if (isDrop(script)) deletionHooks.add(script); }
            @Override public void beforeCommand(String stage) {
                if (stage.equals("DeleteDeploymentCmd") || stage.equals("SchemaOperationProcessEngineClose")) deletionHooks.add(stage);
            }
        };
    }

    private void insertSequenceMesSentinel(JdbcTemplate jdbc) {
        jdbc.update("INSERT INTO mes_pro_batch_record_execution(id,execution_code,work_order_id,work_order_code,batch_code,sheet_layout_json,status,revision_no,tenant_id,deleted) VALUES(930006,'sequence-sentinel',1,'WO','B','{}',1,1,1,0)");
    }

    private List<Map<String, Object>> sequenceMesRows(JdbcTemplate jdbc) {
        return jdbc.queryForList("SELECT * FROM mes_pro_batch_record_execution WHERE id=930006");
    }

    private Map<String, List<Map<String, Object>>> sequenceBusinessRows(JdbcTemplate jdbc) {
        return Map.of("deployment", jdbc.queryForList("SELECT * FROM ACT_RE_DEPLOYMENT ORDER BY ID_"),
                "execution", jdbc.queryForList("SELECT * FROM ACT_RU_EXECUTION ORDER BY ID_"),
                "task", jdbc.queryForList("SELECT * FROM ACT_RU_TASK ORDER BY ID_"));
    }

    private org.h2.schema.Sequence actualIdentitySequence(MesEdhrFlowableTestSchema owned, String table, String column) {
        var sequence = h2(session -> session.getDatabase().getSchema(session.getCurrentSchemaName())
                .findTableOrView(session, table).getColumn(column).getSequence());
        assertNotNull(sequence);
        assertTrue(sequence.getBelongsToTable());
        assertSame(sequence, h2(session -> sequence.getSchema().findSequence(sequence.getName())));
        assertTrue(owned.recordedOwnedObjects().contains(sequence));
        return sequence;
    }

    private record SequenceState(String definition, long base, long current, long cache, long increment,
                                 long start, long min, long max, org.h2.schema.Sequence.Cycle cycle) { }

    private SequenceState sequenceState(org.h2.schema.Sequence sequence) {
        return new SequenceState(sequence.getCreateSQL(), sequence.getBaseValue(), sequence.getCurrentValue(),
                sequence.getCacheSize(), sequence.getIncrement(), sequence.getStartValue(), sequence.getMinValue(),
                sequence.getMaxValue(), sequence.getCycle());
    }

    private Set<org.h2.schema.SchemaObject> liveCatalog() {
        return h2(session -> {
            Set<org.h2.schema.SchemaObject> objects = Collections.newSetFromMap(new IdentityHashMap<>());
            for (var schema : session.getDatabase().getAllSchemasNoMeta()) {
                for (var table : schema.getAllTablesAndViews(session)) {
                    objects.add(table);
                    for (var column : table.getColumns()) if (column.getSequence() != null) objects.add(column.getSequence());
                }
                objects.addAll(schema.getAllSequences());
                objects.addAll(schema.getAllIndexes());
                objects.addAll(schema.getAllConstraints());
                objects.addAll(schema.getAllSynonyms());
                objects.addAll(schema.getAllTriggers());
            }
            return objects;
        });
    }

    private void assertRecordedPresence(Set<org.h2.schema.SchemaObject> recorded, boolean present) {
        assertFalse(recorded.isEmpty());
        var current = liveCatalog();
        for (var identity : recorded) assertEquals(present, current.contains(identity), "recorded owned identity presence");
    }

    private SequenceSentinel createSequenceSentinel(NegativeLifetime lifetime, JdbcTemplate jdbc,
            org.h2.schema.Sequence sequence, String expression, String location, Runnable afterCreate) {
        String schema = location.equals("EXTERNAL") ? "R2_FOREIGN" : ownedSchema();
        String name = location.equals("EXTERNAL") ? "FOREIGN_DEPENDENT" : "MES_R5_SEQUENCE_DEPENDENT";
        if (location.equals("EXTERNAL")) createForeignSchema(lifetime, jdbc);
        var sentinel = new SequenceSentinel(jdbc, schema, name, expression);
        assertNull(h2(session -> session.getDatabase().getSchema(schema).findTableOrView(session, name)));
        lifetime.track(sentinel::dropOwnedTable);
        if (location.equals("MES")) {
            // Keep the MES snapshot table through recovery, even if a caller assertion interrupted setup.
            lifetime.track(() -> {
                if (lifetime.owned.hasReservedCleanupFailure()) lifetime.owned.retryOwnedSchemaCleanup();
            });
            lifetime.track(() -> {
                if (lifetime.owned.hasReservedCleanupFailure() && sentinel.attempted) {
                    assertNotNull(sentinel.table, "table ownership capture required before dependency removal");
                    if (sentinel.defaultSql != null || sentinel.onUpdateSql != null) {
                        if (sentinel.rows == null) sentinel.rows = sentinel.readRows();
                        sentinel.removeExpressionDependency();
                    }
                }
            });
        }
        String sequenceName = quoteIdentifier(sequence.getSchema().getName()) + "." + quoteIdentifier(sequence.getName());
        String clause = expression.equals("DEFAULT") ? "DEFAULT NEXT VALUE FOR " : "ON UPDATE NEXT VALUE FOR ";
        jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) connection -> {
            sentinel.attempted = true;
            var session = (org.h2.engine.SessionLocal) connection.unwrap(org.h2.jdbc.JdbcConnection.class).getSession();
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE TABLE " + sentinel.qualified + "(ID_ BIGINT PRIMARY KEY, SEQ_ BIGINT "
                        + clause + sequenceName + ", PAYLOAD_ VARCHAR(32))");
                sentinel.capture(session); // Capture the exact CREATE identity before any caller assertion.
                afterCreate.run();
            }
            return null;
        });
        if (expression.equals("DEFAULT")) jdbc.update("INSERT INTO " + sentinel.qualified + "(ID_,PAYLOAD_) VALUES(7,'created')");
        else {
            jdbc.update("INSERT INTO " + sentinel.qualified + " VALUES(7,0,'created')");
            jdbc.update("UPDATE " + sentinel.qualified + " SET PAYLOAD_='updated' WHERE ID_=7");
        }
        assertNotEquals(0L, jdbc.queryForObject("SELECT SEQ_ FROM " + sentinel.qualified + " WHERE ID_=7", Long.class));
        sentinel.rows = sentinel.readRows();
        return sentinel;
    }

    private static String quoteIdentifier(String name) { return '"' + name.replace("\"", "\"\"") + '"'; }

    private final class SequenceSentinel {
        final JdbcTemplate jdbc;
        final String schema, name, expression, qualified;
        org.h2.table.Table table;
        org.h2.table.Column column;
        List<org.h2.index.Index> indexes;
        List<String> indexDefinitions;
        String definition, defaultSql, onUpdateSql;
        List<Map<String, Object>> rows;
        boolean attempted;
        SequenceSentinel(JdbcTemplate jdbc, String schema, String name, String expression) {
            this.jdbc = jdbc; this.schema = schema; this.name = name; this.expression = expression;
            qualified = quoteIdentifier(schema) + "." + quoteIdentifier(name);
        }
        void capture(org.h2.engine.SessionLocal session) {
            table = session.getDatabase().getSchema(schema).findTableOrView(session, name);
            assertNotNull(table, "successful table ownership capture required");
            column = table.getColumn("SEQ_");
            indexes = new ArrayList<>(table.getIndexes());
            indexDefinitions = indexes.stream().map(index -> String.valueOf(index.getCreateSQL())).toList();
            definition = table.getCreateSQL();
            defaultSql = column.getDefaultSQL();
            onUpdateSql = column.getOnUpdateSQL();
        }
        List<Map<String, Object>> readRows() { return jdbc.queryForList("SELECT * FROM " + qualified + " ORDER BY ID_"); }
        void assertIntact() {
            assertSame(table, h2(session -> session.getDatabase().getSchema(schema).findTableOrView(session, name)));
            assertSame(column, table.getColumn("SEQ_"));
            assertEquals(definition, table.getCreateSQL());
            assertEquals(defaultSql, column.getDefaultSQL());
            assertEquals(onUpdateSql, column.getOnUpdateSQL());
            assertEquals(indexes.size(), table.getIndexes().size());
            for (int i = 0; i < indexes.size(); i++) assertSame(indexes.get(i), table.getIndexes().get(i));
            assertEquals(indexDefinitions, table.getIndexes().stream().map(index -> String.valueOf(index.getCreateSQL())).toList());
            assertEquals(rows, readRows());
        }
        void assertExpressionDependsOn(org.h2.schema.Sequence sequence) {
            if (expression.equals("DEFAULT")) { assertNotNull(defaultSql); assertNull(onUpdateSql); }
            else { assertNull(defaultSql); assertNotNull(onUpdateSql); }
            assertNull(column.getSequence()); // This is an expression reference, not an identity column.
            var dependencies = new HashSet<org.h2.engine.DbObject>();
            table.addDependencies(dependencies);
            assertTrue(dependencies.stream().anyMatch(dependency -> dependency == sequence));
        }
        void assertNoExpressionDependency(org.h2.schema.Sequence sequence) {
            assertNull(defaultSql);
            assertNull(onUpdateSql);
            var dependencies = new HashSet<org.h2.engine.DbObject>();
            table.addDependencies(dependencies);
            assertFalse(dependencies.stream().anyMatch(dependency -> dependency == sequence));
        }
        void removeExpressionDependency() {
            assertIntact();
            assertTrue(table.getDependentViews().isEmpty());
            assertTrue(table.getDependentMaterializedViews().isEmpty());
            assertTrue(table.getTriggers() == null || table.getTriggers().isEmpty());
            assertTrue(table.getConstraints().stream().allMatch(constraint -> constraint.getTable() == table
                    && (constraint.getRefTable() == null || constraint.getRefTable() == table)));
            jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) connection -> {
                var session = (org.h2.engine.SessionLocal) connection.unwrap(org.h2.jdbc.JdbcConnection.class).getSession();
                assertSame(table, session.getDatabase().getSchema(schema).findTableOrView(session, name));
                try (var statement = connection.createStatement()) {
                    statement.execute("ALTER TABLE " + qualified + " ALTER COLUMN SEQ_ "
                            + (expression.equals("DEFAULT") ? "DROP DEFAULT" : "DROP ON UPDATE"));
                    capture(session); // ALTER may replace table/column/index identities; undo uses only these.
                }
                return null;
            });
            assertIntact();
        }
        void dropOwnedTable() {
            if (!attempted) return;
            assertNotNull(table, "table ownership capture required; do not adopt by name");
            assertSame(table, h2(session -> session.getDatabase().getSchema(schema).findTableOrView(session, name)));
            jdbc.execute("DROP TABLE " + qualified); // Owner only, no CASCADE and no helper deletion.
        }
    }

    private void assertRightIntact(org.h2.engine.Right right, org.h2.table.Table table,
                                   org.h2.engine.RightOwner principal, String definition) {
        assertSame(right, h2(session -> session.getDatabase().getAllRights().stream()
                .filter(candidate -> candidate == right).findFirst().orElseThrow()));
        assertSame(table, right.getGrantedObject());
        assertSame(principal, right.getGrantee());
        assertEquals(org.h2.engine.Right.SELECT, right.getRightMask());
        assertEquals(definition, right.getCreateSQL());
        assertSame(principal, h2(session -> session.getDatabase().findUserOrRole(principal.getName())));
    }

    private String ownedSchema() { return h2(session -> session.getCurrentSchemaName()); }
    private String qualifiedTable(String table) {
        return '"' + ownedSchema().replace("\"", "\"\"") + "\".\"" + table + '"';
    }

    /** Local negative-case lifetime: unexpected body failures retain cleanup failures as suppressed. */
    private static final class NegativeLifetime implements AutoCloseable {
        private final MesEdhrFlowableTestSchema owned;
        private final Runnable[] resetFaults;
        private final List<Runnable> sentinels = new ArrayList<>();
        NegativeLifetime(MesEdhrFlowableTestSchema owned, Runnable... resetFaults) {
            this.owned = owned;
            this.resetFaults = resetFaults;
        }
        void track(Runnable undoOwnedSentinel) { sentinels.add(undoOwnedSentinel); }
        @Override public void close() {
            List<Throwable> failures = new ArrayList<>();
            for (Runnable reset : resetFaults) attempt(failures, reset);
            for (int i = sentinels.size() - 1; i >= 0; i--) attempt(failures, sentinels.get(i));
            if (owned.needsInitialClose()) attempt(failures, owned::close);
            if (owned.hasReservedCleanupFailure()) attempt(failures, owned::retryOwnedSchemaCleanup);
            if (!failures.isEmpty()) {
                Throwable first = failures.get(0);
                for (int i = 1; i < failures.size(); i++) if (failures.get(i) != first) first.addSuppressed(failures.get(i));
                if (first instanceof Error error) throw error;
                throw (RuntimeException) first;
            }
        }
        private static void attempt(List<Throwable> failures, Runnable action) {
            try { action.run(); } catch (RuntimeException | Error failure) { failures.add(failure); }
        }
    }

    private <T> T h2(java.util.function.Function<org.h2.engine.SessionLocal, T> read) {
        try (Connection connection = dataSource.getConnection()) {
            return read.apply((org.h2.engine.SessionLocal) connection.unwrap(org.h2.jdbc.JdbcConnection.class).getSession());
        } catch (SQLException failure) { throw new AssertionError("test-owned H2 identity inspection failed", failure); }
    }

    private void createForeignSchema(NegativeLifetime lifetime, JdbcTemplate jdbc) {
        assertNull(h2(session -> session.getDatabase().findSchema("R2_FOREIGN")));
        org.h2.schema.Schema[] identity = {null};
        boolean[] attempted = {false};
        lifetime.track(() -> {
            if (!attempted[0]) return;
            assertNotNull(identity[0], "successful schema ownership capture is required; do not adopt by name");
            assertSame(identity[0], h2(session -> session.getDatabase().findSchema("R2_FOREIGN")));
            jdbc.execute("DROP SCHEMA R2_FOREIGN"); // No CASCADE; remaining dependencies must refuse.
        });
        jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) connection -> {
            attempted[0] = true;
            var session = (org.h2.engine.SessionLocal) connection.unwrap(org.h2.jdbc.JdbcConnection.class).getSession();
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE SCHEMA R2_FOREIGN");
                identity[0] = session.getDatabase().getSchema("R2_FOREIGN");
            }
            return null;
        });
    }

    private void createForeignDependent(NegativeLifetime lifetime, JdbcTemplate jdbc, String kind,
                                         String sql, Runnable afterCreate) {
        org.h2.schema.SchemaObject[] identity = {null};
        boolean[] attempted = {false};
        lifetime.track(() -> {
            if (!attempted[0]) return;
            assertNotNull(identity[0], "successful dependent ownership capture is required; do not adopt by name");
            assertSame(identity[0], h2(session -> kind.equals("synonym")
                    ? session.getDatabase().getSchema("R2_FOREIGN").getSynonym("FOREIGN_DEPENDENT")
                    : session.getDatabase().getSchema("R2_FOREIGN").findTableOrView(session, "FOREIGN_DEPENDENT")));
            jdbc.execute("DROP " + (kind.equals("fk") ? "TABLE" : kind.equals("view") ? "VIEW" : "SYNONYM")
                    + " R2_FOREIGN.FOREIGN_DEPENDENT");
        });
        jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) connection -> {
            attempted[0] = true;
            var session = (org.h2.engine.SessionLocal) connection.unwrap(org.h2.jdbc.JdbcConnection.class).getSession();
            try (var statement = connection.createStatement()) {
                statement.execute(sql);
                identity[0] = kind.equals("synonym") ? session.getDatabase().getSchema("R2_FOREIGN").getSynonym("FOREIGN_DEPENDENT")
                        : session.getDatabase().getSchema("R2_FOREIGN").findTableOrView(session, "FOREIGN_DEPENDENT");
                afterCreate.run(); // Real CREATE succeeded; identity recorded before statement/connection close.
            }
            return null;
        });
    }

    private List<String> foreignDefinition() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            var session = (org.h2.engine.SessionLocal) connection.unwrap(org.h2.jdbc.JdbcConnection.class).getSession();
            var schema = session.getDatabase().getSchema("R2_FOREIGN");
            List<String> definition = new ArrayList<>();
            schema.getAllTablesAndViews(session).forEach(table -> definition.add(table.getCreateSQL()));
            schema.getAllConstraints().forEach(constraint -> definition.add(constraint.getCreateSQL()));
            schema.getAllIndexes().forEach(index -> { if (index.getCreateSQL() != null) definition.add(index.getCreateSQL()); });
            schema.getAllSynonyms().forEach(synonym -> definition.add(synonym.getCreateSQL()));
            return definition.stream().sorted().toList();
        }
    }

    private static boolean isDrop(String script) {
        return script.contains("/drop/") || script.equals(MesEdhrFlowableTestSchema.OWNED_PRELUDE);
    }

    private static boolean containsStage(Throwable error, String stage) {
        if (stage.equals(error.getMessage())) return true;
        if (error.getCause() != null && containsStage(error.getCause(), stage)) return true;
        for (Throwable suppressed : error.getSuppressed()) if (containsStage(suppressed, stage)) return true;
        return false;
    }

    private MesEdhrFlowableTestSchema open() { return MesEdhrFlowableTestSchema.open(dataSource, new DataSourceTransactionManager(dataSource)); }
    private MesEdhrFlowableTestSchema open(MesEdhrFlowableTestSchema.FailureHooks hooks) {
        return MesEdhrFlowableTestSchema.open(dataSource, new DataSourceTransactionManager(dataSource), hooks);
    }
    private void assertNoSchema() {
        try (Connection connection = dataSource.getConnection()) { assertTrue(MesEdhrFlowableTestSchema.flowableObjects(connection).isEmpty()); }
        catch (SQLException exception) { throw new AssertionError("shared datasource/schema absence check failed", exception); }
    }
    private static void assertUnregistered(Engine engine) {
        if (engine instanceof ProcessEngine) assertNull(ProcessEngines.getProcessEngines().get(engine.getName()));
        else if (engine instanceof IdmEngine) assertNull(IdmEngines.getIdmEngines().get(engine.getName()));
        else if (engine instanceof EventRegistryEngine) assertNull(EventRegistryEngines.getEventRegistryEngines().get(engine.getName()));
        else fail("unexpected engine type");
    }
    private static boolean contains(Throwable outer, Throwable expected) {
        if (outer == expected) return true;
        if (outer.getCause() != null && contains(outer.getCause(), expected)) return true;
        for (Throwable suppressed : outer.getSuppressed()) if (contains(suppressed, expected)) return true;
        return false;
    }
}
