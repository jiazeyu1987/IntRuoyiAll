package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import org.flowable.common.engine.api.Engine;
import org.flowable.common.engine.api.engine.EngineLifecycleListener;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.interceptor.AbstractCommandInterceptor;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandConfig;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngines;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.eventregistry.impl.EventRegistryEngine;
import org.flowable.eventregistry.impl.EventRegistryEngines;
import org.flowable.eventregistry.spring.SpringEventRegistryEngineConfiguration;
import org.flowable.eventregistry.spring.configurator.SpringEventRegistryConfigurator;
import org.flowable.idm.engine.IdmEngine;
import org.flowable.idm.engine.IdmEngines;
import org.flowable.idm.engine.configurator.IdmEngineConfigurator;
import org.flowable.idm.engine.impl.cfg.StandaloneIdmEngineConfiguration;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.h2.constraint.Constraint;
import org.h2.engine.Database;
import org.h2.engine.DbObject;
import org.h2.engine.SessionLocal;
import org.h2.index.Index;
import org.h2.jdbc.JdbcConnection;
import org.h2.schema.SchemaObject;
import org.h2.schema.Sequence;
import org.h2.table.Table;
import org.h2.table.TableSynonym;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

/** Only this harness's complete, fingerprinted Flowable 7.2.0 H2 schema and engines. */
final class MesEdhrFlowableTestSchema implements AutoCloseable {
    private record Script(String component, String root, String createHash, String dropHash) {
        String path(boolean drop) {
            String operation = drop ? "drop" : "create";
            return root + "/" + operation + "/flowable.h2." + operation + "." + component + ".sql";
        }
    }

    private static final List<Script> CREATE = List.of(
            new Script("common", "org/flowable/common/db", "6907b019fca825f8c459a8a1f88b4d89065913a84d322ff41ea16ce79558d19e", "af54962c518fc391c22c1670e7ca5e559c3855c55caccc004c8231e2811eea97"),
            new Script("identity", "org/flowable/idm/db", "e30eedcbd33923b2c46ffa79ba8de09d5f4dccb71a11e213bd27fa464a136a6a", "8fa91818b9df54d540313c0c23133a87e8ad563b2ba57afa317e648931e60fa3"),
            new Script("eventregistry", "org/flowable/eventregistry/db", "977c5734db21b7be8ae178172a99cd227020383ba1cca2b806fe066068b7fde2", "5448198b4a45492f4225033f71d7cf36104577eb4ce881910641217afb90417a"),
            new Script("engine", "org/flowable/db", "fedf8c054ef9dad14f94e6f1cf6b565fd79813b78dff3d588c4345f423556023", "5f8c473d9a8ee6ae21fb47e7632b2d8a2b6ec2b0c383b937cc52db646cf72c73"),
            new Script("history", "org/flowable/db", "b9a0a20560cbc58567275494586bc4e69750b9e938bc94ab61ed86fe67058704", "352ac224af5bc4ed63854c32e097a5fbc09553d7ebf52966031834e32a0b2bc9"));
    private static final List<Script> DROP = List.of(CREATE.get(2), CREATE.get(3), CREATE.get(4), CREATE.get(1), CREATE.get(0));
    static final String OWNED_PRELUDE = "owned-prelude/common-bytearray";
    private static final String BYTEARRAY_DROP = "drop table if exists ACT_GE_BYTEARRAY cascade constraints;";
    static final Set<String> TABLES = names("""
            ACT_GE_PROPERTY ACT_GE_BYTEARRAY FLW_RU_BATCH FLW_RU_BATCH_PART ACT_RU_ENTITYLINK ACT_HI_ENTITYLINK
            ACT_RU_EVENT_SUBSCR ACT_RU_IDENTITYLINK ACT_HI_IDENTITYLINK ACT_RU_JOB ACT_RU_TIMER_JOB ACT_RU_SUSPENDED_JOB
            ACT_RU_DEADLETTER_JOB ACT_RU_HISTORY_JOB ACT_RU_EXTERNAL_JOB ACT_RU_TASK ACT_HI_TASKINST ACT_HI_TSK_LOG
            ACT_RU_VARIABLE ACT_HI_VARINST ACT_ID_PROPERTY ACT_ID_BYTEARRAY ACT_ID_GROUP ACT_ID_MEMBERSHIP ACT_ID_USER
            ACT_ID_INFO ACT_ID_TOKEN ACT_ID_PRIV ACT_ID_PRIV_MAPPING FLW_EVENT_DEPLOYMENT FLW_EVENT_RESOURCE
            FLW_EVENT_DEFINITION FLW_CHANNEL_DEFINITION ACT_RE_DEPLOYMENT ACT_RE_MODEL ACT_RU_EXECUTION ACT_RE_PROCDEF
            ACT_EVT_LOG ACT_PROCDEF_INFO ACT_RU_ACTINST ACT_HI_PROCINST ACT_HI_ACTINST ACT_HI_DETAIL ACT_HI_COMMENT ACT_HI_ATTACHMENT
            """);
    private static final Set<String> INDEXES = names("""
            FLW_IDX_BATCH_PART ACT_IDX_ENT_LNK_SCOPE ACT_IDX_ENT_LNK_REF_SCOPE ACT_IDX_ENT_LNK_ROOT_SCOPE ACT_IDX_ENT_LNK_SCOPE_DEF
            ACT_IDX_HI_ENT_LNK_SCOPE ACT_IDX_HI_ENT_LNK_REF_SCOPE ACT_IDX_HI_ENT_LNK_ROOT_SCOPE ACT_IDX_HI_ENT_LNK_SCOPE_DEF
            ACT_IDX_EVENT_SUBSCR_CONFIG_ ACT_IDX_EVENT_SUBSCR_EXEC_ID ACT_IDX_EVENT_SUBSCR_PROC_ID ACT_IDX_EVENT_SUBSCR_SCOPEREF_
            ACT_IDX_IDENT_LNK_USER ACT_IDX_IDENT_LNK_GROUP ACT_IDX_IDENT_LNK_SCOPE ACT_IDX_IDENT_LNK_SUB_SCOPE ACT_IDX_IDENT_LNK_SCOPE_DEF
            ACT_IDX_HI_IDENT_LNK_USER ACT_IDX_HI_IDENT_LNK_SCOPE ACT_IDX_HI_IDENT_LNK_SUB_SCOPE ACT_IDX_HI_IDENT_LNK_SCOPE_DEF
            ACT_IDX_JOB_EXCEPTION_STACK_ID ACT_IDX_JOB_CUSTOM_VALUES_ID ACT_IDX_JOB_CORRELATION_ID
            ACT_IDX_TIMER_JOB_EXCEPTION_STACK_ID ACT_IDX_TIMER_JOB_CUSTOM_VALUES_ID ACT_IDX_TIMER_JOB_CORRELATION_ID ACT_IDX_TIMER_JOB_DUEDATE
            ACT_IDX_SUSPENDED_JOB_EXCEPTION_STACK_ID ACT_IDX_SUSPENDED_JOB_CUSTOM_VALUES_ID ACT_IDX_SUSPENDED_JOB_CORRELATION_ID
            ACT_IDX_DEADLETTER_JOB_EXCEPTION_STACK_ID ACT_IDX_DEADLETTER_JOB_CUSTOM_VALUES_ID ACT_IDX_DEADLETTER_JOB_CORRELATION_ID
            ACT_IDX_EXTERNAL_JOB_EXCEPTION_STACK_ID ACT_IDX_EXTERNAL_JOB_CUSTOM_VALUES_ID ACT_IDX_EXTERNAL_JOB_CORRELATION_ID
            ACT_IDX_JOB_SCOPE ACT_IDX_JOB_SUB_SCOPE ACT_IDX_JOB_SCOPE_DEF ACT_IDX_TJOB_SCOPE ACT_IDX_TJOB_SUB_SCOPE ACT_IDX_TJOB_SCOPE_DEF
            ACT_IDX_SJOB_SCOPE ACT_IDX_SJOB_SUB_SCOPE ACT_IDX_SJOB_SCOPE_DEF ACT_IDX_DJOB_SCOPE ACT_IDX_DJOB_SUB_SCOPE ACT_IDX_DJOB_SCOPE_DEF
            ACT_IDX_EJOB_SCOPE ACT_IDX_EJOB_SUB_SCOPE ACT_IDX_EJOB_SCOPE_DEF ACT_IDX_TASK_CREATE ACT_IDX_TASK_SCOPE ACT_IDX_TASK_SUB_SCOPE
            ACT_IDX_TASK_SCOPE_DEF ACT_IDX_HI_TASK_SCOPE ACT_IDX_HI_TASK_SUB_SCOPE ACT_IDX_HI_TASK_SCOPE_DEF ACT_IDX_ACT_HI_TSK_LOG_TASK
            ACT_IDX_RU_VAR_SCOPE_ID_TYPE ACT_IDX_RU_VAR_SUB_ID_TYPE ACT_IDX_HI_PROCVAR_NAME_TYPE ACT_IDX_HI_VAR_SCOPE_ID_TYPE ACT_IDX_HI_VAR_SUB_ID_TYPE
            ACT_IDX_PRIV_USER ACT_IDX_PRIV_GROUP FLW_IDX_EVENT_RSRC_DPL ACT_IDX_EVENT_DEF_UNIQ ACT_IDX_CHANNEL_DEF_UNIQ
            ACT_IDX_EXEC_BUSKEY ACT_IDC_EXEC_ROOT ACT_IDX_EXEC_REF_ID_ ACT_IDX_VARIABLE_TASK_ID ACT_IDX_ATHRZ_PROCEDEF ACT_IDX_INFO_PROCDEF
            ACT_IDX_BYTEAR_DEPL ACT_IDX_RU_ACTI_START ACT_IDX_RU_ACTI_END ACT_IDX_RU_ACTI_PROC ACT_IDX_RU_ACTI_PROC_ACT ACT_IDX_RU_ACTI_EXEC
            ACT_IDX_RU_ACTI_EXEC_ACT ACT_IDX_RU_ACTI_TASK ACT_IDX_HI_PRO_INST_END ACT_IDX_HI_PRO_I_BUSKEY ACT_IDX_HI_PRO_SUPER_PROCINST
            ACT_IDX_HI_ACT_INST_START ACT_IDX_HI_ACT_INST_END ACT_IDX_HI_DETAIL_PROC_INST ACT_IDX_HI_DETAIL_ACT_INST ACT_IDX_HI_DETAIL_TIME
            ACT_IDX_HI_DETAIL_NAME ACT_IDX_HI_DETAIL_TASK_ID ACT_IDX_HI_PROCVAR_PROC_INST ACT_IDX_HI_PROCVAR_TASK_ID ACT_IDX_HI_PROCVAR_EXE
            ACT_IDX_HI_ACT_INST_PROCINST ACT_IDX_HI_IDENT_LNK_TASK ACT_IDX_HI_IDENT_LNK_PROCINST ACT_IDX_HI_ACT_INST_EXEC ACT_IDX_HI_TASK_INST_PROCINST
            """);
    private static final Set<String> FOREIGN_KEYS = names("""
            FLW_FK_BATCH_PART_PARENT ACT_FK_JOB_EXCEPTION ACT_FK_JOB_CUSTOM_VALUES ACT_FK_TIMER_JOB_EXCEPTION ACT_FK_TIMER_JOB_CUSTOM_VALUES
            ACT_FK_SUSPENDED_JOB_EXCEPTION ACT_FK_SUSPENDED_JOB_CUSTOM_VALUES ACT_FK_DEADLETTER_JOB_EXCEPTION ACT_FK_DEADLETTER_JOB_CUSTOM_VALUES
            ACT_FK_EXTERNAL_JOB_EXCEPTION ACT_FK_EXTERNAL_JOB_CUSTOM_VALUES ACT_FK_VAR_BYTEARRAY ACT_FK_MEMB_GROUP ACT_FK_MEMB_USER ACT_FK_PRIV_MAPPING
            FLW_FK_EVENT_RSRC_DPL ACT_FK_BYTEARR_DEPL ACT_FK_EXE_PROCINST ACT_FK_EXE_PARENT ACT_FK_EXE_SUPER ACT_FK_EXE_PROCDEF
            ACT_FK_TSKASS_TASK ACT_FK_ATHRZ_PROCEDEF ACT_FK_IDL_PROCINST ACT_FK_TASK_EXE ACT_FK_TASK_PROCINST ACT_FK_TASK_PROCDEF
            ACT_FK_VAR_EXE ACT_FK_VAR_PROCINST ACT_FK_JOB_EXECUTION ACT_FK_JOB_PROCESS_INSTANCE ACT_FK_JOB_PROC_DEF
            ACT_FK_TIMER_JOB_EXECUTION ACT_FK_TIMER_JOB_PROCESS_INSTANCE ACT_FK_TIMER_JOB_PROC_DEF ACT_FK_SUSPENDED_JOB_EXECUTION
            ACT_FK_SUSPENDED_JOB_PROCESS_INSTANCE ACT_FK_SUSPENDED_JOB_PROC_DEF ACT_FK_DEADLETTER_JOB_EXECUTION
            ACT_FK_DEADLETTER_JOB_PROCESS_INSTANCE ACT_FK_DEADLETTER_JOB_PROC_DEF ACT_FK_EVENT_EXEC ACT_FK_MODEL_SOURCE
            ACT_FK_MODEL_SOURCE_EXTRA ACT_FK_MODEL_DEPLOYMENT ACT_FK_INFO_JSON_BA ACT_FK_INFO_PROCDEF
            """);

    /** Hooks run at actual SQL execute / engine built / command boundaries, only for failure regression. */
    interface FailureHooks {
        default void beforeSql(String script, String sql) throws SQLException { }
        default void afterSql(String script, String sql) throws SQLException { }
        default void engineBuilt(Engine engine) { }
        default void beforeCommand(String stage) { }
    }
    private static final FailureHooks NO_FAILURES = new FailureHooks() { };
    private static final Set<DataSource> OCCUPIED = Collections.newSetFromMap(new IdentityHashMap<>());
    private final DataSource ds;
    private final DataSourceTransactionManager tm;
    private final FailureHooks hooks;
    private final Thread ownerThread = Thread.currentThread();
    private final Map<SchemaObject, String> createdObjects = new IdentityHashMap<>();
    private final Map<String, Table> ownedTables = new HashMap<>();
    private final Set<EngineLifecycleListener> ownedListeners = Collections.newSetFromMap(new IdentityHashMap<>());
    private Database ownedDatabase;
    private String ownedSchema;
    private byte[] bytearrayPrelude;
    private boolean databaseDeletionRefused;
    private boolean destructiveCleanupStarted;
    private Set<String> originalMesObjects;
    private boolean processConfigurationCloseAttempted;
    private boolean idmConfigurationCloseAttempted;
    private boolean eventConfigurationCloseAttempted;
    private boolean processConfigurationClosed;
    private boolean idmConfigurationClosed;
    private boolean eventConfigurationClosed;
    final SpringProcessEngineConfiguration processConfig = new SpringProcessEngineConfiguration() {
        @Override public void close() { processConfigurationCloseAttempted = true; super.close(); processConfigurationClosed = true; }
        // Refusal releases only resources: the official close runnable also issues a database lock update.
        @Override public Runnable getProcessEngineCloseRunnable() {
            return databaseDeletionRefused ? null : super.getProcessEngineCloseRunnable();
        }
    };
    final StandaloneIdmEngineConfiguration idmConfig = new StandaloneIdmEngineConfiguration() {
        @Override public void close() { idmConfigurationCloseAttempted = true; super.close(); idmConfigurationClosed = true; }
    };
    final SpringEventRegistryEngineConfiguration eventConfig = new SpringEventRegistryEngineConfiguration() {
        @Override public void close() { eventConfigurationCloseAttempted = true; super.close(); eventConfigurationClosed = true; }
    };
    private final Set<Engine> closeAttempted = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<String, byte[]> createScripts = new HashMap<>();
    private final Map<String, byte[]> dropScripts = new HashMap<>();
    private final Map<Object, Object> springResources = TransactionSynchronizationManager.getResourceMap();
    private ProcessEngine engine;
    private IdmEngine idm;
    private EventRegistryEngine event;
    private String deploymentId;
    private boolean reserved;
    private boolean schemaOwned;
    private boolean buildStarted;
    private boolean closed;
    private Throwable closeFailure;
    private Snapshot cleanupBaseline;

    private MesEdhrFlowableTestSchema(DataSource ds, DataSourceTransactionManager tm, FailureHooks hooks) {
        this.ds = ds; this.tm = tm; this.hooks = hooks;
        String suffix = UUID.randomUUID().toString();
        processConfig.setEngineName("mes-edhr-test-process-" + suffix);
        idmConfig.setEngineName("mes-edhr-test-idm-" + suffix);
        eventConfig.setEngineName("mes-edhr-test-event-" + suffix);
        for (AbstractEngineConfiguration config : List.of(processConfig, idmConfig, eventConfig)) {
            config.setForceCloseMybatisConnectionPool(false);
            config.setDataSource(ds);
            var listener = new EngineLifecycleListener() {
                @Override public void onEngineBuilt(Engine built) {
                    capture(built, config);
                    MesEdhrFlowableTestSchema.this.hooks.engineBuilt(built);
                }
                @Override public void onEngineClosed(Engine ignored) { }
            };
            ownedListeners.add(listener);
            config.addEngineLifecycleListener(listener);
        }
        processConfig.setTransactionManager(tm);
        processConfig.setDatabaseSchemaUpdate("false");
        processConfig.setAsyncExecutorActivate(false);
        processConfig.setCustomPreCommandInterceptors(List.of(new AbstractCommandInterceptor() {
            @Override public <T> T execute(CommandConfig config, Command<T> command, CommandExecutor executor) {
                MesEdhrFlowableTestSchema.this.hooks.beforeCommand(command.getClass().getSimpleName());
                return next.execute(config, command, executor);
            }
        }));
        processConfig.setIdmEngineConfigurator(new IdmEngineConfigurator().setIdmEngineConfiguration(idmConfig));
        processConfig.setEventRegistryConfigurator(new SpringEventRegistryConfigurator().setEventEngineConfiguration(eventConfig));
    }

    static MesEdhrFlowableTestSchema open(DataSource ds, DataSourceTransactionManager tm) {
        return open(ds, tm, NO_FAILURES);
    }

    static MesEdhrFlowableTestSchema open(DataSource ds, DataSourceTransactionManager tm, FailureHooks hooks) {
        require(tm.getDataSource() == ds, "transaction manager must own the same MES datasource");
        require(!TransactionSynchronizationManager.isActualTransactionActive()
                && !TransactionSynchronizationManager.isSynchronizationActive()
                && !TransactionSynchronizationManager.hasResource(ds), "schema lifecycle requires no active datasource transaction");
        var owned = new MesEdhrFlowableTestSchema(ds, tm, hooks);
        try {
            owned.initialize();
            return owned;
        } catch (RuntimeException | Error failure) {
            owned.closeAfterFailure(failure);
            throw failure;
        }
    }

    private void initialize() {
        synchronized (OCCUPIED) {
            require(OCCUPIED.add(ds), "MES datasource already has an owned Flowable harness");
            reserved = true;
        }
        require(ProcessEngines.getProcessEngines().get(processConfig.getEngineName()) == null
                && IdmEngines.getIdmEngines().get(idmConfig.getEngineName()) == null
                && EventRegistryEngines.getEventRegistryEngines().get(eventConfig.getEngineName()) == null, "engine name is already registered");
        try (Connection connection = ds.getConnection()) {
            require("H2".equals(connection.getMetaData().getDatabaseProductName()), "resolved H2 is required");
            require("2.3.232 (2024-08-11)".equals(connection.getMetaData().getDatabaseProductVersion()), "resolved H2 2.3.232 is required");
            require("MySQL".equals(singleValue(connection, "SELECT SETTING_VALUE FROM INFORMATION_SCHEMA.SETTINGS WHERE SETTING_NAME='MODE'")),
                    "original H2 MYSQL mode is required");
            require(flowableObjects(connection).isEmpty(), "foreign ACT_/FLW_ objects exist; ownership is not transferable");
            Snapshot original = snapshot(connection);
            originalMesObjects = original.objects;
            SessionLocal session = localSession(connection);
            ownedDatabase = session.getDatabase();
            ownedSchema = connection.getSchema();
            for (Script script : CREATE) {
                createScripts.put(script.component, originalScript(script, false));
                dropScripts.put(script.component, originalScript(script, true));
            }
            String commonDrop = new String(dropScripts.get("common"), StandardCharsets.UTF_8);
            int position = commonDrop.indexOf(BYTEARRAY_DROP);
            require(position >= 0 && commonDrop.indexOf(BYTEARRAY_DROP, position + BYTEARRAY_DROP.length()) < 0,
                    "audited official bytearray DROP occurrence differs");
            bytearrayPrelude = commonDrop.substring(position, position + BYTEARRAY_DROP.length()).getBytes(StandardCharsets.UTF_8);
            schemaOwned = true; // H2 DDL commits; activate only after foreign-state rejection and hash checks.
            for (Script script : CREATE) execute(connection, script.path(false), adapted(script), hooks);
            verifySchema(connection);
            require(original.equals(snapshot(connection)), "schema creation changed original MES objects/data");
        } catch (SQLException exception) {
            throw new IllegalStateException("Flowable schema initialize failed SQLSTATE=" + exception.getSQLState()
                    + " vendor=" + exception.getErrorCode(), exception);
        }
        buildStarted = true;
        engine = processConfig.buildProcessEngine();
        require(engine.getProcessEngineConfiguration() == processConfig, "process engine configuration identity changed");
        require(idm != null && event != null, "real IDM and eventregistry engines must remain enabled");
        require(processConfig.getDataSource() == idmConfig.getDataSource()
                && processConfig.getDataSource() == eventConfig.getDataSource(), "child datasource identities differ");
        require(target(processConfig.getDataSource()) == ds && eventConfig.getTransactionManager() == tm
                && processConfig.getTransactionManager() == tm, "engines must use the MES datasource and transaction manager");
        require("h2".equals(processConfig.getDatabaseType()) && "false".equals(processConfig.getDatabaseSchemaUpdate()),
                "real H2 schema version checking is required");
        require(processConfig.getAdditionalSchemaManagers().get(idmConfig.getSchemaManager().getContext())
                    instanceof org.flowable.idm.engine.impl.db.IdmDbSchemaManager
                && processConfig.getAdditionalSchemaManagers().get(eventConfig.getSchemaManager().getContext())
                    instanceof org.flowable.eventregistry.impl.db.EventDbSchemaManager,
                "child schema version managers must be checked by the parent");
        require(springResources.equals(TransactionSynchronizationManager.getResourceMap()), "engine build leaked Spring resources");
    }

    ProcessEngine engine() { return engine; }

    Set<SchemaObject> recordedOwnedObjects() {
        Set<SchemaObject> identities = Collections.newSetFromMap(new IdentityHashMap<>());
        identities.addAll(createdObjects.keySet());
        return Collections.unmodifiableSet(identities);
    }

    boolean configurationsClosed() {
        return processConfigurationClosed && idmConfigurationClosed && eventConfigurationClosed;
    }

    Deployment deploy(String bpmn) {
        require(deploymentId == null, "only one owned deployment is allowed");
        Deployment deployment = engine.getRepositoryService().createDeployment().tenantId("1")
                .addString("edhr.bpmn20.xml", bpmn).deploy();
        deploymentId = deployment.getId();
        return deployment;
    }

    ProcessInstance start(String definitionId) {
        engine.getIdentityService().setAuthenticatedUserId("88");
        try {
            return engine.getRuntimeService().startProcessInstanceById(definitionId, "BUSINESS_APPROVAL:9401", Map.of());
        } finally {
            engine.getIdentityService().setAuthenticatedUserId(null);
        }
    }

    boolean needsInitialClose() { return !closed; }
    boolean hasReservedCleanupFailure() { return closed && closeFailure != null && reserved; }

    void closeAfterFailure(Throwable original) {
        try { close(); } catch (RuntimeException | Error cleanup) { if (cleanup != original) original.addSuppressed(cleanup); }
    }

    @Override public void close() {
        if (closed) {
            if (closeFailure != null) throw failure("previous owned cleanup failed", closeFailure);
            if (!createdObjects.isEmpty()) {
                try (Connection connection = ds.getConnection()) {
                    verifyRecordedOwnedAbsence(connection);
                } catch (Exception | Error exception) {
                    closeFailure = failure("verify repeated close owned absence", exception);
                    throw failure("owned Flowable cleanup failed", closeFailure);
                }
            }
            return;
        }
        require(!TransactionSynchronizationManager.isActualTransactionActive()
                && !TransactionSynchronizationManager.isSynchronizationActive()
                && !TransactionSynchronizationManager.hasResource(ds), "owned close requires the business transaction to finish");
        closed = true;
        List<Throwable> failures = new ArrayList<>();
        if (schemaOwned) attempt(failures, "snapshot before cleanup", () -> {
            try (Connection connection = ds.getConnection()) { cleanupBaseline = snapshot(connection); }
        });
        attempt(failures, "capture partial engines", this::captureRegistered);
        boolean deleteAllowed = !schemaOwned || deletionSafety(failures, "ownership/dependency check before database deletion");
        if (engine != null) {
            attempt(failures, "clear authentication", () -> engine.getIdentityService().setAuthenticatedUserId(null));
            if (deleteAllowed && deploymentId != null) attempt(failures, "delete owned deployment", () -> {
                destructiveCleanupStarted = true;
                engine.getRepositoryService().deleteDeployment(deploymentId, true);
            });
        }
        closeEngine(failures, engine, processConfig, ProcessEngines.getProcessEngines());
        closeEngine(failures, event, eventConfig, EventRegistryEngines.getEventRegistryEngines());
        closeEngine(failures, idm, idmConfig, IdmEngines.getIdmEngines());
        // Only initialized configurations with actual collaborators are closed without an engine instance.
        if (buildStarted && !processConfigurationCloseAttempted && (processConfig.getSqlSessionFactory() != null
                || processConfig.getAsyncTaskExecutor() != null || processConfig.getHttpClientConfig() != null)) {
            attempt(failures, "close partial process configuration", processConfig::close);
        }
        if (buildStarted && !eventConfigurationCloseAttempted) {
            if (event == null && eventConfig.getEventRegistryChangeDetectionExecutor() != null)
                attempt(failures, "shutdown partial event executor", () -> eventConfig.getEventRegistryChangeDetectionExecutor().shutdown());
            if (eventConfig.getSqlSessionFactory() != null)
                attempt(failures, "close partial event configuration", eventConfig::close);
        }
        if (buildStarted && !idmConfigurationCloseAttempted && idmConfig.getSqlSessionFactory() != null)
            attempt(failures, "close partial IDM configuration", idmConfig::close);
        if (deleteAllowed) dropSchema(failures);
        attempt(failures, "owned registries and Spring resources", () -> {
            require(ProcessEngines.getProcessEngines().get(processConfig.getEngineName()) == null
                    && IdmEngines.getIdmEngines().get(idmConfig.getEngineName()) == null
                    && EventRegistryEngines.getEventRegistryEngines().get(eventConfig.getEngineName()) == null,
                    "owned engine registry entries remain");
            require(springResources.equals(TransactionSynchronizationManager.getResourceMap()), "owned cleanup leaked Spring resources");
        });
        if (!schemaOwned && failures.stream().allMatch(failure -> failure.getMessage().equals("delete owned deployment")
                || (failure.getMessage().equals("close " + processConfig.getEngineName()) && configurationsClosed()
                    && (processConfig.getAsyncExecutor() == null || !processConfig.getAsyncExecutor().isActive())
                    && (processConfig.getAsyncHistoryExecutor() == null || !processConfig.getAsyncHistoryExecutor().isActive())))
                && ProcessEngines.getProcessEngines().get(processConfig.getEngineName()) == null
                && IdmEngines.getIdmEngines().get(idmConfig.getEngineName()) == null
                && EventRegistryEngines.getEventRegistryEngines().get(eventConfig.getEngineName()) == null)
            attempt(failures, "verify owned absence before releasing reservation", this::releaseReservation);
        if (!failures.isEmpty()) {
            closeFailure = failures.get(0);
            for (int i = 1; i < failures.size(); i++) closeFailure.addSuppressed(failures.get(i));
            throw failure("owned Flowable cleanup failed", closeFailure);
        }
    }

    /** Explicit recovery for an injected drop failure; never retries an engine close or hides the failed close. */
    void retryOwnedSchemaCleanup() {
        require(closed && closeFailure != null && reserved, "only failed reserved owned cleanup can be explicitly recovered");
        require(!buildStarted || configurationsClosed(), "configuration release must succeed before explicit schema recovery");
        require(springResources.equals(TransactionSynchronizationManager.getResourceMap()), "Spring resources remain after failed cleanup");
        require(ProcessEngines.getProcessEngines().get(processConfig.getEngineName()) == null
                && IdmEngines.getIdmEngines().get(idmConfig.getEngineName()) == null
                && EventRegistryEngines.getEventRegistryEngines().get(eventConfig.getEngineName()) == null, "engines still registered");
        List<Throwable> failures = new ArrayList<>();
        if (deletionSafety(failures, "ownership/dependency check before explicit recovery")) {
            if (!destructiveCleanupStarted) attempt(failures, "snapshot after owner removed foreign dependents", () -> {
                try (Connection connection = ds.getConnection()) {
                    Snapshot current = snapshot(connection);
                    require(cleanupBaseline.mesRows.equals(current.mesRows) && current.objects.containsAll(originalMesObjects),
                            "MES data/original objects changed during refused cleanup");
                    cleanupBaseline = current;
                }
            });
            if (!failures.isEmpty()) throw failure("owned recovery snapshot failed", failures.get(0));
            if (schemaOwned) dropSchema(failures);
            else attempt(failures, "verify already absent owned schema", () -> {
                try (Connection connection = ds.getConnection()) {
                    require(flowableObjects(connection).isEmpty(), "owned schema is no longer absent");
                    require(cleanupBaseline != null && cleanupBaseline.equals(snapshot(connection)), "recovery changed MES snapshot");
                    verifyRecordedOwnedAbsence(connection);
                }
            });
        }
        if (!failures.isEmpty()) {
            for (int i = 1; i < failures.size(); i++) failures.get(0).addSuppressed(failures.get(i));
            throw failure("owned schema recovery failed", failures.get(0));
        }
        releaseReservation();
    }

    private void dropSchema(List<Throwable> failures) {
        if (!schemaOwned) return;
        if (!deletionSafety(failures, "ownership/dependency check after resource release before DROP")) return;
        for (Script script : DROP) {
            if (!deletionSafety(failures, "ownership/dependency check before drop " + script.component)) return;
            if (script.component.equals("common")) attempt(failures, "owned bytearray prelude", () -> {
                destructiveCleanupStarted = true;
                try (Connection connection = ds.getConnection()) { execute(connection, OWNED_PRELUDE, bytearrayPrelude, hooks); }
            });
            // A SQL fault retains failure but does not erase independent safe full-script attempts.
            if (!deletionSafety(failures, "ownership/dependency check before full drop " + script.component)) return;
            attempt(failures, "drop " + script.component, () -> {
                destructiveCleanupStarted = true;
                try (Connection connection = ds.getConnection()) {
                    execute(connection, script.path(true), dropScripts.get(script.component), hooks);
                }
            });
        }
        attempt(failures, "verify owned schema absence and MES snapshot", () -> {
            try (Connection connection = ds.getConnection()) {
                require(flowableObjects(connection).isEmpty(), "owned ACT_/FLW_ objects remain after cleanup");
                require(cleanupBaseline != null && cleanupBaseline.equals(snapshot(connection)), "cleanup changed MES objects/data");
                verifyRecordedOwnedAbsence(connection);
                schemaOwned = false;
            }
        });
    }

    private void releaseReservation() {
        if (reserved) {
            if (!createdObjects.isEmpty()) {
                try (Connection connection = ds.getConnection()) {
                    verifyRecordedOwnedAbsence(connection);
                } catch (SQLException exception) {
                    throw failure("verify owned absence before releasing reservation", exception);
                }
            }
            synchronized (OCCUPIED) { OCCUPIED.remove(ds); }
            reserved = false;
        }
    }

    private void verifyRecordedOwnedAbsence(Connection connection) throws SQLException {
        SessionLocal session = localSession(connection);
        require(session.getDatabase() == ownedDatabase && connection.getSchema().equals(ownedSchema),
                "physical database/schema identity differs during absence verification");
        Set<SchemaObject> current = catalog(session);
        for (var entry : createdObjects.entrySet()) require(!current.contains(entry.getKey()),
                "recorded owned catalog object remains: " + entry.getValue());
    }

    private boolean deletionSafety(List<Throwable> failures, String stage) {
        try (Connection connection = ds.getConnection()) {
            require(Thread.currentThread() == ownerThread && !TransactionSynchronizationManager.isActualTransactionActive()
                    && !TransactionSynchronizationManager.isSynchronizationActive()
                    && !TransactionSynchronizationManager.hasResource(ds), "cleanup requires task-exclusive serial lifetime without a business transaction");
            require(cleanupBaseline != null, "cleanup snapshot is unavailable; deletion safety is unproven");
            require("false".equals(processConfig.getDatabaseSchemaUpdate()), "process close may delete schema");
            for (AbstractEngineConfiguration config : List.of(processConfig, idmConfig, eventConfig)) {
                require(!config.isForceCloseMybatisConnectionPool() && safeCloseListeners(config), "unverified resource-close behavior");
            }
            require(ProcessEngines.getProcessEngines().get(processConfig.getEngineName()) == null
                    || ProcessEngines.getProcessEngines().get(processConfig.getEngineName()) == engine, "foreign process registry identity");
            require(IdmEngines.getIdmEngines().get(idmConfig.getEngineName()) == null
                    || IdmEngines.getIdmEngines().get(idmConfig.getEngineName()) == idm, "foreign IDM registry identity");
            require(EventRegistryEngines.getEventRegistryEngines().get(eventConfig.getEngineName()) == null
                    || EventRegistryEngines.getEventRegistryEngines().get(eventConfig.getEngineName()) == event, "foreign event registry identity");
            SessionLocal session = localSession(connection);
            require(session.getDatabase() == ownedDatabase && connection.getSchema().equals(ownedSchema), "physical database/schema identity differs");
            for (org.h2.engine.Right right : session.getDatabase().getAllRights()) {
                boolean targetsOwnedTable = ownedTables.values().stream()
                        .anyMatch(table -> right.getGrantedObject() == table);
                require(!targetsOwnedTable, "foreign table right on owned target");
            }
            Set<SchemaObject> current = catalog(session);
            for (SchemaObject object : current) {
                boolean inOwnedSchema = object.getSchema().getName().equals(ownedSchema);
                if (createdObjects.containsKey(object)) {
                    require(inOwnedSchema && object.getName().equals(createdObjects.get(object)), "owned catalog object was renamed or moved");
                }
                if (object instanceof Table table) {
                    if (inOwnedSchema && (TABLES.contains(table.getName()) || isFlowable(table.getName())))
                        require(ownedTables.get(table.getName()) == table && createdObjects.containsKey(table), "foreign target table/view identity");
                    if (!ownedTables.containsValue(table)) {
                        var dependencies = new HashSet<DbObject>();
                        table.addDependencies(dependencies);
                        for (DbObject dependency : dependencies) require(!(dependency instanceof Sequence)
                                || !createdObjects.containsKey(dependency), "foreign table expression dependency on owned sequence");
                    }
                    if (ownedTables.containsValue(table)) {
                        require(!table.isView() && table.getDependentViews().isEmpty(), "foreign dependent view on owned target");
                        require(table.getDependentMaterializedViews().isEmpty(), "foreign materialized view on owned target");
                        require(table.getTriggers() == null || table.getTriggers().isEmpty(), "foreign trigger on owned target");
                        for (var column : table.getColumns()) require(column.getSequence() == null
                                || createdObjects.containsKey(column.getSequence()), "foreign sequence bound to owned target");
                    }
                } else if (object instanceof Constraint constraint) {
                    if (ownedTables.containsValue(constraint.getTable()) || ownedTables.containsValue(constraint.getRefTable()))
                        require(createdObjects.containsKey(constraint) && ownedTables.containsValue(constraint.getTable()),
                                "foreign constraint or incoming cross-schema FK on owned target");
                } else if (object instanceof Index index) {
                    if (ownedTables.containsValue(index.getTable()) || (inOwnedSchema && INDEXES.contains(index.getName())))
                        require(createdObjects.containsKey(index) && ownedTables.containsValue(index.getTable()), "foreign index ownership conflict");
                } else if (object instanceof TableSynonym synonym) {
                    require(!synonym.isInvalid(), "synonym dependency metadata is unavailable");
                    require(!ownedTables.containsValue(synonym.getSynonymFor()), "foreign synonym on owned target");
                }
                if (inOwnedSchema && isFlowable(object.getName()))
                    require(createdObjects.containsKey(object), "unexpected ACT_/FLW_ catalog object");
            }
            databaseDeletionRefused = false;
            return true;
        } catch (Exception | Error exception) {
            databaseDeletionRefused = true;
            failures.add(failure(stage, exception));
            return false; // Refusal is terminal for every database deletion in this cleanup invocation.
        }
    }

    private boolean safeCloseListeners(AbstractEngineConfiguration config) {
        if (config.getEngineLifecycleListeners() == null) return true;
        return config.getEngineLifecycleListeners().stream().allMatch(listener -> ownedListeners.contains(listener)
                || listener.getClass().getName().equals("org.flowable.common.engine.impl.AbstractEngineConfigurator$3"));
    }

    private static SessionLocal localSession(Connection connection) throws SQLException {
        var session = connection.unwrap(JdbcConnection.class).getSession();
        require(session instanceof SessionLocal, "resolved embedded H2 dependency metadata is unavailable");
        SessionLocal local = (SessionLocal) session;
        require(local.getUser().isAdmin(), "full cross-schema H2 dependency visibility is required");
        require(local.getLocalTempTables().isEmpty() && local.getLocalTempTableIndexes().isEmpty()
                && local.getLocalTempTableConstraints().isEmpty(), "temporary H2 dependencies are not supported in the exclusive fixture");
        return local;
    }

    /** Exact live resolved-H2 catalog, including all schemas; no SQL-definition parsing. */
    private static Set<SchemaObject> catalog(SessionLocal session) {
        Set<SchemaObject> objects = Collections.newSetFromMap(new IdentityHashMap<>());
        for (var schema : session.getDatabase().getAllSchemasNoMeta()) {
            for (Table table : schema.getAllTablesAndViews(session)) {
                objects.add(table);
                for (var column : table.getColumns()) if (column.getSequence() != null) objects.add(column.getSequence());
            }
            objects.addAll(schema.getAllIndexes());
            objects.addAll(schema.getAllConstraints());
            objects.addAll(schema.getAllSequences());
            objects.addAll(schema.getAllSynonyms());
            objects.addAll(schema.getAllTriggers());
        }
        return objects;
    }

    private void recordCreated(Connection connection, Set<SchemaObject> before) throws SQLException {
        Set<SchemaObject> added = catalog(localSession(connection));
        added.removeAll(before);
        for (SchemaObject object : added) if (object instanceof Table table) {
            require(table.getSchema().getName().equals(ownedSchema) && TABLES.contains(table.getName()) && !table.isView(),
                    "official CREATE produced an unexpected table/view");
            require(ownedTables.putIfAbsent(table.getName(), table) == null, "official CREATE target identity conflicts");
            createdObjects.put(object, object.getName());
        }
        for (SchemaObject object : added) {
            boolean owned = object instanceof Table && ownedTables.containsValue(object);
            if (object instanceof Index index) owned = ownedTables.containsValue(index.getTable());
            if (object instanceof Constraint constraint) owned = ownedTables.containsValue(constraint.getTable());
            if (object instanceof Sequence sequence) owned = ownedTables.values().stream().anyMatch(table ->
                    Arrays.stream(table.getColumns()).anyMatch(column -> column.getSequence() == sequence));
            require(owned && object.getSchema().getName().equals(ownedSchema), "official CREATE produced an unexpected dependency object");
            createdObjects.put(object, object.getName());
        }
    }

    private void capture(Engine built, AbstractEngineConfiguration config) {
        require(built.getName().equals(config.getEngineName()), "unexpected owned engine name");
        if (config == processConfig) {
            require(built instanceof ProcessEngine && ((ProcessEngine) built).getProcessEngineConfiguration() == config, "foreign process engine");
            engine = (ProcessEngine) built;
        } else if (config == idmConfig) {
            require(built instanceof IdmEngine && ((IdmEngine) built).getIdmEngineConfiguration() == config, "foreign IDM engine");
            idm = (IdmEngine) built;
        } else {
            require(built instanceof EventRegistryEngine && ((EventRegistryEngine) built).getEventRegistryEngineConfiguration() == config, "foreign event engine");
            event = (EventRegistryEngine) built;
        }
    }

    private void captureRegistered() {
        ProcessEngine registeredProcess = ProcessEngines.getProcessEngines().get(processConfig.getEngineName());
        if (registeredProcess != null) capture(registeredProcess, processConfig);
        EventRegistryEngine registeredEvent = EventRegistryEngines.getEventRegistryEngines().get(eventConfig.getEngineName());
        if (registeredEvent != null) capture(registeredEvent, eventConfig);
        IdmEngine registeredIdm = IdmEngines.getIdmEngines().get(idmConfig.getEngineName());
        if (registeredIdm != null) capture(registeredIdm, idmConfig);
    }

    private void closeEngine(List<Throwable> failures, Engine owned, AbstractEngineConfiguration config, Map<String, ? extends Engine> registry) {
        if (owned == null || !closeAttempted.add(owned)) return;
        attempt(failures, "close " + config.getEngineName(), () -> {
            Engine current = registry.get(config.getEngineName());
            require(current == null || current == owned, "registry engine identity conflict; refusing foreign close");
            require(safeCloseListeners(config), "unverified engine close callback; refusing callback execution");
            require(!config.isForceCloseMybatisConnectionPool(), "shared datasource pool close is forbidden");
            owned.close();
        });
    }

    private byte[] adapted(Script script) {
        String sql = new String(createScripts.get(script.component), StandardCharsets.UTF_8);
        if (script.component.equals("common")) sql = replaceOnce(sql, "ID_ identity,", "ID_ bigint generated by default as identity,");
        if (script.component.equals("engine")) sql = replaceOnce(sql, "LOG_NR_ identity,", "LOG_NR_ bigint generated by default as identity primary key,");
        return sql.getBytes(StandardCharsets.UTF_8);
    }

    private static String replaceOnce(String sql, String original, String replacement) {
        int position = sql.indexOf(original);
        require(position >= 0 && sql.indexOf(original, position + original.length()) < 0, "audited identity occurrence differs");
        return sql.substring(0, position) + replacement + sql.substring(position + original.length());
    }

    private static byte[] originalScript(Script script, boolean drop) {
        String path = script.path(drop);
        try (InputStream stream = MesEdhrFlowableTestSchema.class.getClassLoader().getResourceAsStream(path)) {
            require(stream != null, "missing official Flowable script " + path);
            byte[] bytes = stream.readAllBytes();
            require(hash(bytes).equals(drop ? script.dropHash : script.createHash), "official script SHA256 differs: " + path);
            return bytes;
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("cannot read official Flowable script " + path, exception);
        }
    }

    private void execute(Connection real, String label, byte[] bytes, FailureHooks hooks) {
        // Spring performs all splitting/execution. The proxy only exposes the actual execute boundary to fault tests.
        Connection connection = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class}, (proxy, method, args) -> {
            try {
                Object value = method.invoke(real, args);
                if (method.getName().equals("createStatement")) {
                    Statement statement = (Statement) value;
                    return Proxy.newProxyInstance(Statement.class.getClassLoader(), new Class<?>[]{Statement.class}, (p, call, arguments) -> {
                        boolean create = call.getName().equals("execute") && label.contains("/create/");
                        if (call.getName().equals("execute") && arguments != null && arguments[0] instanceof String sql) hooks.beforeSql(label, sql);
                        if (call.getName().equals("execute") && (label.contains("/drop/") || label.equals(OWNED_PRELUDE))) {
                            List<Throwable> safety = new ArrayList<>();
                            if (!deletionSafety(safety, "ownership/dependency check before SQL DROP"))
                                throw failure("database deletion refused", safety.get(0));
                        }
                        Set<SchemaObject> before = create ? catalog(localSession(real)) : null;
                        Throwable original = null;
                        try {
                            Object result = call.invoke(statement, arguments);
                            if (call.getName().equals("execute") && arguments != null && arguments[0] instanceof String sql)
                                hooks.afterSql(label, sql);
                            return result;
                        }
                        catch (InvocationTargetException exception) { original = exception.getCause(); throw original; }
                        catch (Exception | Error exception) { original = exception; throw exception; }
                        finally {
                            if (create) try { recordCreated(real, before); }
                            catch (Exception | Error metadata) { if (original != null) original.addSuppressed(metadata); else throw metadata; }
                        }
                    });
                }
                return value;
            } catch (InvocationTargetException exception) { throw exception.getCause(); }
        });
        ScriptUtils.executeSqlScript(connection, new EncodedResource(new ByteArrayResource(bytes, label), StandardCharsets.UTF_8),
                false, false, ScriptUtils.DEFAULT_COMMENT_PREFIX, ScriptUtils.DEFAULT_STATEMENT_SEPARATOR,
                ScriptUtils.DEFAULT_BLOCK_COMMENT_START_DELIMITER, ScriptUtils.DEFAULT_BLOCK_COMMENT_END_DELIMITER);
    }

    static void verifySchema(Connection connection) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        Set<String> tables = new TreeSet<>();
        try (ResultSet rows = metadata.getTables(null, connection.getSchema(), null, new String[]{"TABLE"})) {
            while (rows.next()) if (isFlowable(rows.getString("TABLE_NAME"))) tables.add(rows.getString("TABLE_NAME"));
        }
        require(tables.equals(TABLES), "official 45-table manifest differs");
        Set<String> indexes = new TreeSet<>();
        Set<String> foreignKeys = new TreeSet<>();
        for (String table : TABLES) {
            Map<Short, String> keys = new TreeMap<>();
            try (ResultSet rows = metadata.getPrimaryKeys(null, connection.getSchema(), table)) {
                while (rows.next()) keys.put(rows.getShort("KEY_SEQ"), rows.getString("COLUMN_NAME"));
            }
            List<String> expected = switch (table) {
                case "ACT_GE_PROPERTY", "ACT_ID_PROPERTY" -> List.of("NAME_");
                case "ACT_ID_MEMBERSHIP" -> List.of("USER_ID_", "GROUP_ID_");
                case "ACT_EVT_LOG" -> List.of("LOG_NR_");
                default -> List.of("ID_");
            };
            require(new ArrayList<>(keys.values()).equals(expected), "official primary key differs: " + table);
            try (ResultSet rows = metadata.getIndexInfo(null, connection.getSchema(), table, false, false)) {
                while (rows.next()) if (INDEXES.contains(rows.getString("INDEX_NAME"))) indexes.add(rows.getString("INDEX_NAME"));
            }
            try (ResultSet rows = metadata.getImportedKeys(null, connection.getSchema(), table)) {
                while (rows.next()) foreignKeys.add(rows.getString("FK_NAME"));
            }
        }
        require(indexes.equals(INDEXES) && indexes.size() == 103, "official explicit index manifest differs");
        require(foreignKeys.equals(FOREIGN_KEYS) && foreignKeys.size() == 47, "official foreign key manifest differs");
        verifyUnique(metadata, connection.getSchema(), "ACT_RE_PROCDEF", List.of("KEY_", "VERSION_", "DERIVED_VERSION_", "TENANT_ID_"));
        verifyUnique(metadata, connection.getSchema(), "ACT_PROCDEF_INFO", List.of("PROC_DEF_ID_"));
        verifyUnique(metadata, connection.getSchema(), "ACT_ID_PRIV", List.of("NAME_"));
        verifyUnique(metadata, connection.getSchema(), "ACT_HI_PROCINST", List.of("PROC_INST_ID_"));
        verifyUnique(metadata, connection.getSchema(), "FLW_EVENT_DEFINITION", List.of("KEY_", "VERSION_", "TENANT_ID_"));
        verifyUnique(metadata, connection.getSchema(), "FLW_CHANNEL_DEFINITION", List.of("KEY_", "VERSION_", "TENANT_ID_"));
        for (String[] identity : List.of(new String[]{"ACT_HI_TSK_LOG", "ID_"}, new String[]{"ACT_EVT_LOG", "LOG_NR_"})) {
            try (ResultSet rows = metadata.getColumns(null, connection.getSchema(), identity[0], identity[1])) {
                require(rows.next() && "YES".equals(rows.getString("IS_AUTOINCREMENT")), "official identity differs: " + identity[0]);
            }
        }
        require(properties(connection, "ACT_GE_PROPERTY").equals(Map.of(
                "common.schema.version", "7.2.0.2:1", "next.dbid", "1:1", "schema.version", "7.2.0.2:1",
                "schema.history", "create(7.2.0.2):1", "eventregistry.schema.version", "7.2.0.2:1")), "official common/process/event versions differ");
        require(properties(connection, "ACT_ID_PROPERTY").equals(Map.of("schema.version", "7.2.0.2:1")), "official IDM version differs");
    }

    private static void verifyUnique(DatabaseMetaData metadata, String schema, String table, List<String> columns) throws SQLException {
        Map<String, Map<Short, String>> indexes = new HashMap<>();
        try (ResultSet rows = metadata.getIndexInfo(null, schema, table, true, false)) {
            while (rows.next()) {
                String name = rows.getString("INDEX_NAME");
                if (name != null) indexes.computeIfAbsent(name, ignored -> new TreeMap<>()).put(rows.getShort("ORDINAL_POSITION"), rows.getString("COLUMN_NAME"));
            }
        }
        require(indexes.values().stream().anyMatch(index -> new ArrayList<>(index.values()).equals(columns)), "official unique columns differ: " + table);
    }

    private static Map<String, String> properties(Connection connection, String table) throws SQLException {
        Map<String, String> result = new TreeMap<>();
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT NAME_, VALUE_, REV_ FROM " + table)) {
            while (rows.next()) result.put(rows.getString(1), rows.getString(2) + ":" + rows.getInt(3));
        }
        return result;
    }

    static Set<String> flowableObjects(Connection connection) throws SQLException {
        Set<String> result = new TreeSet<>();
        for (String[] source : List.of(new String[]{"TABLES", "TABLE_NAME", "TABLE_SCHEMA"},
                new String[]{"INDEXES", "INDEX_NAME", "INDEX_SCHEMA"}, new String[]{"SEQUENCES", "SEQUENCE_NAME", "SEQUENCE_SCHEMA"},
                new String[]{"TABLE_CONSTRAINTS", "CONSTRAINT_NAME", "CONSTRAINT_SCHEMA"})) {
            try (var query = connection.prepareStatement("SELECT " + source[1] + " FROM INFORMATION_SCHEMA." + source[0] + " WHERE " + source[2] + "=?")) {
                query.setString(1, connection.getSchema());
                try (ResultSet rows = query.executeQuery()) {
                    while (rows.next()) if (isFlowable(rows.getString(1))) result.add(source[0] + ":" + rows.getString(1));
                }
            }
        }
        return result;
    }

    private record Snapshot(Set<String> objects, Map<String, List<String>> mesRows) { }

    private static Snapshot snapshot(Connection connection) throws SQLException {
        Set<String> objects = new TreeSet<>();
        Map<String, List<String>> data = new TreeMap<>();
        try (ResultSet tables = connection.getMetaData().getTables(null, connection.getSchema(), null, null)) {
            while (tables.next()) {
                String table = tables.getString("TABLE_NAME");
                if (isFlowable(table)) continue;
                objects.add(tables.getString("TABLE_TYPE") + ":" + table);
                if (!table.toLowerCase(java.util.Locale.ROOT).startsWith("mes_")) continue;
                List<String> rowHashes = new ArrayList<>();
                String quoted = '"' + table.replace("\"", "\"\"") + '"';
                try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT * FROM " + quoted)) {
                    while (rows.next()) {
                        MessageDigest digest = digest();
                        for (int column = 1; column <= rows.getMetaData().getColumnCount(); column++) {
                            int type = rows.getMetaData().getColumnType(column);
                            byte[] value;
                            if (type == java.sql.Types.BINARY || type == java.sql.Types.VARBINARY || type == java.sql.Types.LONGVARBINARY
                                    || type == java.sql.Types.BLOB) value = rows.getBytes(column);
                            else {
                                String text = rows.getString(column);
                                value = text == null ? null : text.getBytes(StandardCharsets.UTF_8);
                            }
                            digest.update(ByteBuffer.allocate(4).putInt(value == null ? -1 : value.length).array());
                            if (value != null) digest.update(value);
                        }
                        rowHashes.add(HexFormat.of().formatHex(digest.digest()));
                    }
                }
                Collections.sort(rowHashes);
                data.put(table, rowHashes);
            }
        }
        return new Snapshot(objects, data);
    }

    private static String singleValue(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(sql)) {
            require(rows.next(), "required H2 setting is missing");
            String result = rows.getString(1);
            require(!rows.next(), "required H2 setting is ambiguous");
            return result;
        }
    }
    private static DataSource target(DataSource ds) {
        return ds instanceof TransactionAwareDataSourceProxy proxy ? proxy.getTargetDataSource() : ds;
    }
    private static boolean isFlowable(String name) { return name.toUpperCase(java.util.Locale.ROOT).startsWith("ACT_") || name.toUpperCase(java.util.Locale.ROOT).startsWith("FLW_"); }
    private static Set<String> names(String value) { return Set.copyOf(Arrays.asList(value.strip().split("\\s+"))); }
    private static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256 is required", exception); }
    }
    private static String hash(byte[] bytes) { return HexFormat.of().formatHex(digest().digest(bytes)); }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
    private static IllegalStateException failure(String stage, Throwable cause) { return new IllegalStateException(stage, cause); }
    private interface Cleanup { void run() throws Exception; }
    private static void attempt(List<Throwable> failures, String stage, Cleanup cleanup) {
        try { cleanup.run(); }
        catch (Exception | Error exception) { failures.add(failure(stage, exception)); }
    }
}
