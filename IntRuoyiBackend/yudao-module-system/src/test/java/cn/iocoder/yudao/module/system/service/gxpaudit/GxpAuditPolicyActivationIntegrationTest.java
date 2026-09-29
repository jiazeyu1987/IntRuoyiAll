package cn.iocoder.yudao.module.system.service.gxpaudit;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.*;
import cn.iocoder.yudao.module.system.dal.dataobject.tenant.TenantDO;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.*;
import cn.iocoder.yudao.module.system.dal.mysql.tenant.TenantMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.GXP_AUDIT_POLICY_ACTIVATION_INVALID;
import static org.junit.jupiter.api.Assertions.*;

/** Real loader/service/mapper integration, not approval or MySQL concurrency evidence. */
@Import({GxpAuditPolicyActivationServiceImpl.class, GxpAuditServiceImpl.class,
        GxpAuditPolicyActivationIntegrationTest.LoaderConfiguration.class})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:act04_integration;MODE=MYSQL;DATABASE_TO_UPPER=false;NON_KEYWORDS=value;",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
class GxpAuditPolicyActivationIntegrationTest extends BaseDbUnitTest {

    private static final long TENANT = 1L;
    private static final long ACTOR = 1001L;
    private static final String COVERAGE = "d".repeat(64);

    // act04-loader-red/after.xml, operationSemanticChangeChangesPolicyHash failure message/CDATA.
    // Source XML SHA256: A1AB280886D120B3698581AEC4B149A89F1E2DE743557242FE6C11475CE0E359.
    // These are observed bytes/hashes, not a reimplementation guessed from envelope field names.
    private static final String H_ARTIFACT_HASH =
            "5fc4d4be12df26dcb88f127a0e900f0830535ae4db079884d2c4022a3cfee1a0";
    private static final String H_POLICY_HASH =
            "3248272264aa40dfea7760dca98b9d6ed9ea4cc8ac0166d4c76f4d9bbbea0fd7";
    private static final String H_CANONICAL = "{\"approvalReference\":\"CODEX-IMPLEMENTATION-20260908\","
            + "\"policyVersion\":\"2026-09-approved-02\","
            + "\"rawYamlSha256\":\"5fc4d4be12df26dcb88f127a0e900f0830535ae4db079884d2c4022a3cfee1a0\","
            + "\"schemaVersion\":\"gxp-audit-policy.v2\",\"status\":\"APPROVED\"}";

    @Resource private GxpAuditPolicyActivationService activationService;
    @Resource private GxpAuditService auditService;
    @Resource private InputLoader bundleLoader;
    @Resource private GxpAuditPolicyVersionMapper versionMapper;
    @Resource private GxpAuditPolicyOperationMapper operationMapper;
    @Resource private GxpAuditPolicyActivationMapper activationMapper;
    @Resource private GxpAuditEventMapper eventMapper;
    @Resource private GxpAuditEventRelationMapper relationMapper;
    @Resource private GxpAuditLedgerSequenceMapper ledgerMapper;
    @Resource private TenantMapper tenantMapper;
    @Resource private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactions;
    private GxpAuditPolicyBundle baseline;

    @BeforeEach
    void prepareOwnedInMemoryFixture() {
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive(),
                "The service proxy, not an enclosing test transaction, must commit activation");
        assertTrue(AopUtils.isAopProxy(activationService));
        assertTrue(AopUtils.isAopProxy(auditService));
        transactions = new TransactionTemplate(transactionManager);
        bundleLoader.useClasspath();
        baseline = bundleLoader.load();
        assertTrue(baseline.approved(), "Use the unchanged checked resource; do not change approval to pass");

        LoginUser user = new LoginUser();
        user.setId(ACTOR);
        user.setTenantId(TENANT);
        user.setInfo(Map.of("username", "act04.fixture", LoginUser.INFO_KEY_NICKNAME, "ACT04 fixture"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
        transactions.executeWithoutResult(status -> {
            assertNull(tenantMapper.selectById(TENANT));
            assertNull(ledgerMapper.selectById(TENANT));
            TenantDO tenant = TenantDO.builder().id(TENANT).name("act04-integration")
                    .contactUserId(ACTOR).contactName("ACT04 fixture").status(0)
                    .packageId(0L).expireTime(LocalDateTime.of(2099, 1, 1, 0, 0))
                    .accountCount(1).build();
            assertEquals(1, tenantMapper.insert(tenant));
            GxpAuditLedgerSequenceDO ledger = new GxpAuditLedgerSequenceDO();
            ledger.setTenantId(TENANT);
            ledger.setNextLedgerSequence(1L);
            assertEquals(1, ledgerMapper.insert(ledger));
        });
    }

    @AfterEach
    void clearOwnedFixture() {
        try {
            // BaseDbUnitTest's AFTER_TEST_METHOD clean.sql clears all other rows in this private H2 DB.
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> ledgerMapper.deleteById(TENANT));
        } finally {
            bundleLoader.useClasspath();
            SecurityContextHolder.clearContext();
        }
    }

    // BDD I01: Given real A, When activation commits then replays, Then immutable evidence is reused.
    @Test
    void sameRequestReplaysCommittedRealLoaderActivationWithoutWrites() {
        GxpAuditPolicyActivationResult first = activationService.activate(command("i01-r1"));
        assertFalse(first.replayed());
        assertCommittedActivation(first, "i01-r1");
        assertActivationCounts(1L);
        Snapshot committed = snapshot();
        assertReplay(first, activationService.activate(command("i01-r1")));
        assertEquals(committed, snapshot());
    }

    // BDD I02: Given A/r1, When the same version/artifact uses r2, Then only a new activation is added.
    @Test
    void newRequestReusesVersionAndOperationsAndBothRequestsReplay() {
        GxpAuditPolicyActivationResult first = activationService.activate(command("i02-r1"));
        assertCommittedActivation(first, "i02-r1");
        assertActivationCounts(1L);
        Snapshot before = snapshot();
        GxpAuditPolicyActivationResult second = activationService.activate(command("i02-r2"));
        assertFalse(second.replayed());
        assertNotEquals(first.activationId(), second.activationId());
        assertCommittedActivation(second, "i02-r2");
        Snapshot after = snapshot();
        assertEquals(before.versions(), after.versions());
        assertEquals(before.operations(), after.operations());
        assertActivationCounts(2L);
        transactions.executeWithoutResult(status -> {
            assertEquals(activationMapper.selectById(first.activationId()).getActivationHash(),
                    activationMapper.selectById(second.activationId()).getPreviousActivationHash());
        });
        assertReplay(second, activationService.activate(command("i02-r2")));
        assertEquals(after, snapshot());
        assertReplay(first, activationService.activate(command("i02-r1")));
        assertEquals(after, snapshot());
    }

    // BDD I03: Given A/r1, When only artifact bytes change, Then exact replay rejects with no mutation.
    @Test
    void sameRequestWithEquivalentSemanticButDifferentArtifactRejectsWithoutWrites() {
        GxpAuditPolicyActivationResult first = activationService.activate(command("i03-r1"));
        assertCommittedActivation(first, "i03-r1");
        assertActivationCounts(1L);
        Snapshot before = snapshot();
        bundleLoader.useCommentedArtifact();
        GxpAuditPolicyBundle changed = bundleLoader.load();
        assertEquals(baseline.policyNode(), changed.policyNode());
        assertEquals(baseline.canonicalPolicyJson(), changed.canonicalPolicyJson());
        assertEquals(baseline.policyHash(), changed.policyHash());
        assertNotEquals(baseline.artifactHash(), changed.artifactHash());

        ServiceException failure = assertThrows(ServiceException.class,
                () -> activationService.activate(command("i03-r1")));
        assertEquals(GXP_AUDIT_POLICY_ACTIVATION_INVALID.getCode(), failure.getCode());
        assertTrue(failure.getMessage().contains("activation-request-payload-conflict"));
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        assertEquals(before, snapshot(), "Read after the rejected proxy transaction has rolled back");
        bundleLoader.useClasspath();
        assertReplay(first, activationService.activate(command("i03-r1")));
        assertEquals(before, snapshot());
    }

    // BDD I05: Given complete synthetic legacy history, When a new request uses the current hash,
    // Then the same version is rejected without rewriting any historical row.
    @Test
    void historicalGoldenVersionRejectsNewRequestWithoutRewritingHistory() throws Exception {
        seedHistoricalGolden("i05-old");
        Snapshot history = snapshot();
        ServiceException failure = assertThrows(ServiceException.class,
                () -> activationService.activate(command("i05-new")));
        assertEquals(GXP_AUDIT_POLICY_ACTIVATION_INVALID.getCode(), failure.getCode());
        assertTrue(failure.getMessage().contains("policy-version-hash-conflict"));
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        assertEquals(history, snapshot());
        assertActivationCounts(1L);
    }

    // BDD I06: Given the same complete history, When its original request uses the current hash,
    // Then replay rejects explicitly and cannot backfill old evidence from the current bundle.
    @Test
    void historicalGoldenRequestRejectsCurrentLoaderReplayWithoutRewritingHistory() throws Exception {
        seedHistoricalGolden("i06-old");
        Snapshot history = snapshot();
        ServiceException failure = assertThrows(ServiceException.class,
                () -> activationService.activate(command("i06-old")));
        assertEquals(GXP_AUDIT_POLICY_ACTIVATION_INVALID.getCode(), failure.getCode());
        assertTrue(failure.getMessage().contains("activation-request-payload-conflict"));
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        assertEquals(history, snapshot());
        assertActivationCounts(1L);
    }

    private void seedHistoricalGolden(String request) throws Exception {
        ObjectMapper json = new ObjectMapper();
        String artifact;
        try (var input = getClass().getResourceAsStream("/gxp/legacy-policy-act04.json")) {
            assertNotNull(input);
            artifact = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertEquals(H_ARTIFACT_HASH, DigestUtil.sha256Hex(artifact.getBytes(StandardCharsets.UTF_8)));
        assertEquals(H_POLICY_HASH, DigestUtil.sha256Hex(H_CANONICAL.getBytes(StandardCharsets.UTF_8)));
        assertThrows(ServiceException.class,
                () -> new GxpAuditPolicyBundleLoader().load(artifact, baseline.rawSchema()),
                "The old artifact is historical evidence, not a valid v2 runtime bundle");

        // Authorized synthetic H2 seed, not a production historical record or an old deployment.
        // Real service + writer build internally linked version/operations/activation/event/relation.
        bundleLoader.useHistoricalGolden(artifact, true);
        baseline = bundleLoader.load();
        assertEquals(H_POLICY_HASH, baseline.policyHash());
        GxpAuditPolicyActivationResult old = activationService.activate(command(request));
        assertFalse(old.replayed());
        assertCommittedActivation(old, request);
        assertActivationCounts(1L);
        Snapshot completeHistory = snapshot();
        assertReplay(old, activationService.activate(command(request)));
        assertEquals(completeHistory, snapshot(), "Complete legacy fixture must replay before switching algorithms");

        // Compare a valid current bundle under the historical version, solely in this H2 fixture.
        bundleLoader.useHistoricalGolden(artifact, false);
        baseline = bundleLoader.load();
        assertEquals("2026-09-approved-02", baseline.policyVersion());
        assertNotEquals(H_POLICY_HASH, baseline.policyHash());

    }

    private GxpAuditPolicyActivationCommand command(String request) {
        return new GxpAuditPolicyActivationCommand(TENANT, ACTOR, baseline.approvalReference(), request, COVERAGE);
    }

    private void assertActivationCounts(long expected) {
        transactions.executeWithoutResult(status -> {
            assertEquals(expected, activationMapper.selectCount().longValue());
            assertEquals(expected, eventMapper.selectCount().longValue());
            assertEquals(expected, relationMapper.selectCount().longValue());
        });
    }

    private void assertReplay(GxpAuditPolicyActivationResult original, GxpAuditPolicyActivationResult replay) {
        assertTrue(replay.replayed());
        assertEquals(original.activationId(), replay.activationId());
        assertEquals(original.policyVersion(), replay.policyVersion());
        assertEquals(original.policyHash(), replay.policyHash());
    }

    private void assertCommittedActivation(GxpAuditPolicyActivationResult result, String request) {
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        transactions.executeWithoutResult(status -> {
            List<GxpAuditPolicyVersionDO> versions = versionMapper.selectList();
            assertEquals(1, versions.size());
            GxpAuditPolicyVersionDO version = versions.get(0);
            assertEquals(TENANT, version.getTenantId());
            assertEquals(baseline.policyVersion(), version.getPolicyVersion());
            assertEquals(baseline.policyHash(), version.getPolicyHash());
            assertEquals(baseline.canonicalPolicyJson(), version.getCanonicalPolicyJson());
            assertEquals(baseline.artifactHash(), version.getArtifactHash());
            assertEquals(baseline.schemaVersion(), version.getSchemaVersion());
            assertEquals(baseline.approvalReference(), version.getApprovalReference());
            assertEquals(COVERAGE, version.getCoverageReportHash());
            assertEquals(ACTOR, version.getApprovedBy());
            assertNotNull(version.getApprovedAt());
            assertEquals(baseline.policyNode().path("operations").size(), operationMapper.selectCount().intValue());
            for (JsonNode node : baseline.policyNode().path("operations")) {
                GxpAuditPolicyOperationDO operation = operationMapper.selectByPolicyVersion(
                        TENANT, baseline.policyVersion(), node.path("operationId").asText());
                assertNotNull(operation);
                assertEquals(TENANT, operation.getTenantId());
                assertEquals(baseline.policyVersion(), operation.getPolicyVersion());
                assertEquals(node.path("sourceType").asText(), operation.getSourceType());
                assertEquals(node.path("sourceLocators").toString(), operation.getSourceLocator());
                assertEquals(node.path("domain").asText(), operation.getDomain());
                assertEquals(node.path("subjectType").asText(), operation.getSubjectType());
                assertEquals(node.path("actionType").asText(), operation.getActionType());
                assertEquals(node.path("reasonPolicy").asText(), operation.getReasonPolicy());
                assertEquals(node.path("signaturePolicy").asText(), operation.getSignaturePolicy());
                assertEquals(node.path("statePolicy").asText(), operation.getStatePolicy());
                assertEquals(node.path("retentionClass").asText(), operation.getRetentionClass());
                assertEquals(node.path("testIds").toString(), operation.getTestIds());
                assertEquals(node.path("ownerRole").asText(), operation.getOwner());
                assertEquals(node.path("applicability").asText(), operation.getApplicability());
                assertEquals(Boolean.TRUE, operation.getActive());
            }
            GxpAuditPolicyActivationDO activation = activationMapper.selectById(result.activationId());
            assertNotNull(activation);
            assertEquals(request, activation.getRequestId());
            assertEquals(TENANT, activation.getTenantId());
            assertEquals(ACTOR, activation.getActorId());
            assertEquals(baseline.approvalReference(), activation.getApprovalReference());
            assertEquals(baseline.policyHash(), activation.getPolicyHash());
            assertEquals(baseline.policyVersion(), activation.getPolicyVersion());
            assertEquals(baseline.policyVersion(), result.policyVersion());
            assertEquals(baseline.policyHash(), result.policyHash());
            assertNotNull(activation.getActivatedAtUtc());
            assertEquals(DigestUtil.sha256Hex(activation.getCanonicalActivationJson()), activation.getActivationHash());
            GxpAuditEventDO event = eventMapper.selectOne(new QueryWrapper<GxpAuditEventDO>()
                    .eq("tenant_id", TENANT).eq("request_id", request));
            assertNotNull(event);
            assertEquals("gxp.policy.activate", event.getOperationId());
            assertEquals(baseline.policyVersion(), event.getPolicyVersion());
            assertEquals(activation.getCanonicalActivationJson(), event.getAfterStateJson());
            assertEquals(DigestUtil.sha256Hex(event.getCanonicalEventJson()), event.getEventHash());
            GxpAuditEventRelationDO relation = relationMapper.selectOne(new QueryWrapper<GxpAuditEventRelationDO>()
                    .eq("tenant_id", TENANT).eq("event_id", event.getId()));
            assertNotNull(relation);
            assertEquals("ACTIVE_POLICY", relation.getRelationType());
            assertEquals("GXP_POLICY_ACTIVATION", relation.getTargetType());
            assertEquals(String.valueOf(result.activationId()), relation.getTargetId());
            assertEquals(baseline.policyVersion(), relation.getTargetVersion());
            assertEquals(activation.getActivationHash(), relation.getTargetHash());
        });
    }

    private Snapshot snapshot() {
        return transactions.execute(status -> new Snapshot(
                JsonUtils.toJsonString(versionMapper.selectList(new QueryWrapper<GxpAuditPolicyVersionDO>().orderByAsc("id"))),
                JsonUtils.toJsonString(operationMapper.selectList(new QueryWrapper<GxpAuditPolicyOperationDO>().orderByAsc("id"))),
                JsonUtils.toJsonString(activationMapper.selectList(new QueryWrapper<GxpAuditPolicyActivationDO>().orderByAsc("id"))),
                JsonUtils.toJsonString(eventMapper.selectList(new QueryWrapper<GxpAuditEventDO>().orderByAsc("id"))),
                JsonUtils.toJsonString(relationMapper.selectList(new QueryWrapper<GxpAuditEventRelationDO>().orderByAsc("id"))),
                JsonUtils.toJsonString(ledgerMapper.selectById(TENANT))));
    }

    private record Snapshot(String versions, String operations, String activations,
                            String events, String relations, String ledger) { }

    @TestConfiguration(proxyBeanMethods = false)
    static class LoaderConfiguration {
        @Bean
        InputLoader bundleLoader() {
            return new InputLoader();
        }
    }

    /** Real v2 parsing plus an explicit immutable legacy H2 seed; no production parser fallback. */
    static class InputLoader extends GxpAuditPolicyBundleLoader {
        private boolean commented;
        private String historicalArtifact;
        private boolean legacySeed;

        void useClasspath() {
            commented = false;
            historicalArtifact = null;
            legacySeed = false;
        }
        void useCommentedArtifact() { commented = true; }

        void useHistoricalGolden(String artifact, boolean seed) {
            assertEquals(H_ARTIFACT_HASH, DigestUtil.sha256Hex(artifact.getBytes(StandardCharsets.UTF_8)));
            historicalArtifact = artifact;
            legacySeed = seed;
            commented = false;
        }

        @Override
        public GxpAuditPolicyBundle load() {
            GxpAuditPolicyBundle original = super.load();
            if (historicalArtifact != null) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    if (!legacySeed) {
                        ObjectNode current = original.policyNode().deepCopy();
                        current.put("policyVersion", "2026-09-approved-02");
                        return super.load(mapper.writeValueAsString(current), original.rawSchema());
                    }
                    // Explicit H2-only historical seed. Normalize projection field names for the
                    // current persistence adapter; archived bytes/hash remain immutable evidence.
                    ObjectNode historical = (ObjectNode) mapper.readTree(historicalArtifact);
                    for (JsonNode item : historical.path("operations")) {
                        ObjectNode operation = (ObjectNode) item;
                        operation.putArray("sourceLocators").add(operation.remove("sourceLocator").asText());
                        operation.set("ownerRole", operation.remove("owner"));
                    }
                    return new GxpAuditPolicyBundle(historical.path("schemaVersion").asText(),
                            historical.path("policyVersion").asText(), historical.path("status").asText(),
                            historical.path("approvalReference").asText(), H_POLICY_HASH, H_ARTIFACT_HASH,
                            H_CANONICAL, historicalArtifact, original.rawSchema(), historical);
                } catch (java.io.IOException exception) {
                    throw new AssertionError("Invalid immutable historical test fixture", exception);
                }
            }
            return commented ? super.load("# ACT04 representation only\n" + original.rawYaml(), original.rawSchema()) : original;
        }
    }
}
