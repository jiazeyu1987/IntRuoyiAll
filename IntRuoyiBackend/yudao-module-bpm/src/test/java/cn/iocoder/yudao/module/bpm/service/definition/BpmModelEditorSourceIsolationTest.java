package cn.iocoder.yudao.module.bpm.service.definition;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.util.BpmnModelUtils;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.flowable.engine.repository.Model;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Real private Flowable persistence reproduces legacy seed aliasing. No runtime DB or mock repository. */
class BpmModelEditorSourceIsolationTest {

    private static final String TENANT = "1";
    private static final String KEY = "model_source_isolation";
    private static final String RESOURCE_NAME = KEY + ".bpmn20.xml";
    private static final String ORIGINAL_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                         targetNamespace="urn:test:model-source-isolation">
              <process id="model_source_isolation" name="隔离资源回归" isExecutable="true">
                <startEvent id="start"/>
                <sequenceFlow id="start_review" sourceRef="start" targetRef="review"/>
                <userTask id="review" name="旧部署审核"/>
                <sequenceFlow id="review_end" sourceRef="review" targetRef="end"/>
                <endEvent id="end"/>
              </process>
            </definitions>
            """;
    private static final String IMPORTED_XML = ORIGINAL_XML.replace("旧部署审核", "新编辑源审核");

    private ProcessEngine engine;
    private RepositoryService repository;
    private JdbcTemplate jdbc;
    private BpmModelServiceImpl service;

    @BeforeEach
    void createPrivateEngineAndActualService() {
        TenantContextHolder.setTenantId(1L);
        var configuration = new StandaloneInMemProcessEngineConfiguration();
        configuration.setJdbcUrl("jdbc:h2:mem:bpm_editor_isolation_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        configuration.setDatabaseSchemaUpdate("true");
        configuration.setAsyncExecutorActivate(false);
        configuration.setDisableIdmEngine(true);
        engine = configuration.buildProcessEngine();
        repository = engine.getRepositoryService();
        jdbc = new JdbcTemplate(configuration.getDataSource());
        service = new BpmModelServiceImpl();
        ReflectionTestUtils.setField(service, "repositoryService", repository);
        ReflectionTestUtils.setField(service, "managementService", engine.getManagementService());
    }

    @AfterEach
    void closePrivateEngine() {
        try {
            if (engine != null) {
                engine.close();
            }
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Test
    void importingXmlMustDetachEditorSourceWithoutChangingHistoricalDeployment() throws IOException {
        var deployment = repository.createDeployment().tenantId(TENANT)
                .addString(RESOURCE_NAME, ORIGINAL_XML).deploy();
        var definition = repository.createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
        assertNotNull(definition);

        Model model = repository.newModel();
        model.setKey(KEY);
        model.setName("隔离资源回归");
        model.setCategory("DCC241016C15");
        model.setVersion(4);
        model.setTenantId(TENANT);
        model.setDeploymentId(deployment.getId());
        model.setMetaInfo("{\"type\":10,\"managerUserIds\":[99],\"formType\":20,"
                + "\"formCustomCreatePath\":\"/test/create\",\"formCustomViewPath\":\"/test/view\","
                + "\"description\":\"preserve original metadata\"}");
        repository.saveModel(model);

        String historicalResourceId = jdbc.queryForObject(
                "SELECT ID_ FROM ACT_GE_BYTEARRAY WHERE DEPLOYMENT_ID_ = ? AND NAME_ = ?",
                String.class, deployment.getId(), RESOURCE_NAME);
        assertNotNull(historicalResourceId);
        // Reproduce the legacy seed in this private engine only. Do not create a second resource.
        assertEquals(1, jdbc.update("UPDATE ACT_RE_MODEL SET EDITOR_SOURCE_VALUE_ID_ = ? WHERE ID_ = ?",
                historicalResourceId, model.getId()));
        assertEquals(historicalResourceId, editorSourceId(model.getId()));
        byte[] originalResource = deploymentBytes(deployment.getId());
        assertArrayEquals(originalResource, repository.getModelEditorSource(model.getId()));
        String originalDigest = fullGraphDigest(originalResource);
        assertNotEquals(originalDigest, fullGraphDigest(IMPORTED_XML.getBytes(StandardCharsets.UTF_8)),
                "the imported graph must be observably different before testing isolation");
        Model before = repository.getModel(model.getId());
        String originalMetaInfo = before.getMetaInfo();

        // Exercise the actual production update used by normal model import; do not publish.
        service.updateModelBpmnXml(model.getId(), IMPORTED_XML);

        byte[] historicalAfter = deploymentBytes(deployment.getId());
        Model after = repository.getModel(model.getId());
        String importedResourceId = editorSourceId(model.getId());
        assertAll("model import must isolate its editable source from the historical deployment",
                () -> assertArrayEquals(originalResource, historicalAfter,
                        "old deployment resource bytes were overwritten through the shared editor source"),
                () -> assertEquals(originalDigest, fullGraphDigest(historicalAfter),
                        "old deployed graph digest must remain unchanged (reparse actual bytes, not cached model)"),
                () -> assertNotEquals(historicalResourceId, importedResourceId,
                        "editor source must reference a newly independent resource"),
                () -> assertArrayEquals(IMPORTED_XML.getBytes(StandardCharsets.UTF_8),
                        repository.getModelEditorSource(model.getId())),
                () -> assertEquals(0L, jdbc.queryForObject(
                        "SELECT COUNT(*) FROM ACT_GE_BYTEARRAY WHERE ID_ = ? AND DEPLOYMENT_ID_ IS NOT NULL",
                        Long.class, importedResourceId), "new editable resource cannot belong to a deployed definition"),
                () -> assertEquals(deployment.getId(), jdbc.queryForObject(
                        "SELECT DEPLOYMENT_ID_ FROM ACT_GE_BYTEARRAY WHERE ID_ = ?",
                        String.class, historicalResourceId)),
                () -> assertEquals(before.getId(), after.getId()),
                () -> assertEquals(before.getKey(), after.getKey()),
                () -> assertEquals(before.getName(), after.getName()),
                () -> assertEquals(before.getCategory(), after.getCategory()),
                () -> assertEquals(before.getVersion(), after.getVersion()),
                () -> assertEquals(TENANT, after.getTenantId()),
                () -> assertEquals(originalMetaInfo, after.getMetaInfo()),
                () -> assertEquals(deployment.getId(), after.getDeploymentId()),
                () -> assertEquals(1L, repository.createDeploymentQuery().count(), "import must not publish"),
                () -> assertEquals(definition.getId(), repository.createProcessDefinitionQuery()
                        .deploymentId(deployment.getId()).singleResult().getId()));
    }

    private String editorSourceId(String modelId) {
        return jdbc.queryForObject("SELECT EDITOR_SOURCE_VALUE_ID_ FROM ACT_RE_MODEL WHERE ID_ = ?",
                String.class, modelId);
    }

    @Test
    void failedRebindMustRollBackTheNewSourceAndKeepTheOriginalPointer() throws IOException {
        var deployment = repository.createDeployment().tenantId(TENANT)
                .addString(RESOURCE_NAME, ORIGINAL_XML).deploy();
        Model model = repository.newModel();
        model.setKey(KEY);
        model.setName("隔离资源回滚");
        model.setTenantId(TENANT);
        model.setMetaInfo("{\"managerUserIds\":[99],\"description\":\"preserve on failure\"}");
        model.setDeploymentId(deployment.getId());
        repository.saveModel(model);
        String originalId = jdbc.queryForObject(
                "SELECT ID_ FROM ACT_GE_BYTEARRAY WHERE DEPLOYMENT_ID_ = ? AND NAME_ = ?",
                String.class, deployment.getId(), RESOURCE_NAME);
        assertNotNull(originalId);
        assertEquals(1, jdbc.update("UPDATE ACT_RE_MODEL SET EDITOR_SOURCE_VALUE_ID_ = ? WHERE ID_ = ?",
                originalId, model.getId()));
        byte[] originalBytes = deploymentBytes(deployment.getId());
        String originalDigest = fullGraphDigest(originalBytes);
        Long originalResourceCount = jdbc.queryForObject("SELECT COUNT(*) FROM ACT_GE_BYTEARRAY", Long.class);
        String metaInfo = repository.getModel(model.getId()).getMetaInfo();
        // Private H2 only: allow a detached null but reject committing any new source pointer.
        // A detach/save followed by a separate failing write would leave null and fail this test.
        jdbc.execute("ALTER TABLE ACT_RE_MODEL ADD CONSTRAINT editor_rebind_failure CHECK (ID_ <> '"
                + model.getId().replace("'", "''") + "' OR EDITOR_SOURCE_VALUE_ID_ IS NULL"
                + " OR EDITOR_SOURCE_VALUE_ID_ = '" + originalId.replace("'", "''") + "')");

        RuntimeException failure = assertThrows(RuntimeException.class,
                () -> service.updateModelBpmnXml(model.getId(), IMPORTED_XML));
        Throwable rootCause = failure;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }
        assertTrue(String.valueOf(rootCause.getMessage()).toUpperCase(java.util.Locale.ROOT)
                .contains("EDITOR_REBIND_FAILURE"), "must fail at the actual late H2 pointer-write constraint");

        assertAll("failed import cannot commit a detached model or an orphan new source",
                () -> assertEquals(originalId, editorSourceId(model.getId())),
                () -> assertArrayEquals(originalBytes, deploymentBytes(deployment.getId())),
                () -> assertEquals(originalDigest, fullGraphDigest(deploymentBytes(deployment.getId()))),
                () -> assertArrayEquals(originalBytes, repository.getModelEditorSource(model.getId())),
                () -> assertEquals(originalResourceCount,
                        jdbc.queryForObject("SELECT COUNT(*) FROM ACT_GE_BYTEARRAY", Long.class)),
                () -> assertEquals(TENANT, repository.getModel(model.getId()).getTenantId()),
                () -> assertEquals(metaInfo, repository.getModel(model.getId()).getMetaInfo()),
                () -> assertEquals(deployment.getId(), repository.getModel(model.getId()).getDeploymentId()));
    }

    @Test
    void independentEditorSourceCanBeCreatedAndUpdatedWithoutChangingItsResourceId() {
        Model model = repository.newModel();
        model.setKey(KEY);
        model.setTenantId(TENANT);
        model.setMetaInfo("{\"managerUserIds\":[99]}");
        repository.saveModel(model);
        assertNull(editorSourceId(model.getId()));

        service.updateModelBpmnXml(model.getId(), ORIGINAL_XML);
        String independentId = editorSourceId(model.getId());
        assertNotNull(independentId);
        Long resourceCount = jdbc.queryForObject("SELECT COUNT(*) FROM ACT_GE_BYTEARRAY", Long.class);
        service.updateModelBpmnXml(model.getId(), IMPORTED_XML);

        assertAll("normal standalone editor source remains supported",
                () -> assertEquals(independentId, editorSourceId(model.getId())),
                () -> assertArrayEquals(IMPORTED_XML.getBytes(StandardCharsets.UTF_8),
                        repository.getModelEditorSource(model.getId())),
                () -> assertEquals(resourceCount,
                        jdbc.queryForObject("SELECT COUNT(*) FROM ACT_GE_BYTEARRAY", Long.class)),
                () -> assertEquals(0L, repository.createDeploymentQuery().count()),
                () -> assertEquals(TENANT, repository.getModel(model.getId()).getTenantId()));
    }

    private byte[] deploymentBytes(String deploymentId) throws IOException {
        try (var input = repository.getResourceAsStream(deploymentId, RESOURCE_NAME)) {
            assertNotNull(input);
            return input.readAllBytes();
        }
    }

    private static String fullGraphDigest(byte[] bytes) {
        String xml = BpmnModelUtils.getBpmnXml(BpmnModelUtils.getBpmnModel(bytes));
        assertNotNull(xml);
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(xml.replace("\r\n", "\n").replace('\r', '\n').getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
