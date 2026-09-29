package cn.iocoder.yudao.module.system.service.gxpaudit;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.*;
import cn.iocoder.yudao.module.system.dal.dataobject.tenant.TenantDO;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.*;
import cn.iocoder.yudao.module.system.dal.mysql.tenant.TenantMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.annotations.Select;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.GXP_AUDIT_POLICY_ACTIVATION_INVALID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * BDD: Given tenant/ledger facts and a competing committed receipt, When activation runs,
 * Then reject unsafe initialization and read the winner under tenant -> ledger locks.
 * Mapper doubles model a stale snapshot deterministically; this is NOT MySQL/transaction proof.
 * RED must be executed by the coordinator. No production API is added to make tests compile.
 */
class GxpAuditPolicyActivationLockBehaviorTest {
    private final GxpAuditPolicyActivationServiceImpl service = new GxpAuditPolicyActivationServiceImpl();
    private final List<String> reads = new ArrayList<>();
    private final List<String> writes = new ArrayList<>();
    private boolean tenantExists = true;
    private boolean hasEvent;
    private boolean tenantLocked;
    private boolean ledgerLocked;
    private boolean winnerCommitted;
    private Long nextSequence = 10L;
    private boolean ledgerExists = true;
    private RuntimeException ledgerFailure;
    private GxpAuditPolicyActivationDO winner;
    private GxpAuditPolicyActivationDO inserted;
    private GxpAuditPolicyBundle bundle;
    private GxpAuditPolicyVersionDO version;
    private boolean differentRequest;

    @BeforeEach
    void givenCompleteTestOnlyFixture() throws Exception {
        Configuration configuration = new Configuration();
        // Match application.yaml; plain MyBatis Configuration defaults this to false.
        configuration.setMapUnderscoreToCamelCase(true);
        for (Class<?> entity : List.of(TenantDO.class, GxpAuditPolicyActivationDO.class,
                GxpAuditPolicyVersionDO.class, GxpAuditPolicyOperationDO.class)) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, "ACT02-test"), entity);
        }
        for (Class<?> entity : List.of(GxpAuditPolicyActivationDO.class,
                GxpAuditPolicyVersionDO.class, GxpAuditPolicyOperationDO.class)) {
            assertEquals("tenant_id", TableInfoHelper.getTableInfo(entity).getFieldList().stream()
                    .filter(field -> field.getProperty().equals("tenantId"))
                    .findFirst().orElseThrow().getColumn());
            assertEquals("policy_version", TableInfoHelper.getTableInfo(entity).getFieldList().stream()
                    .filter(field -> field.getProperty().equals("policyVersion"))
                    .findFirst().orElseThrow().getColumn());
        }
        assertEquals("operation_id", TableInfoHelper.getTableInfo(GxpAuditPolicyOperationDO.class)
                .getFieldList().stream().filter(field -> field.getProperty().equals("operationId"))
                .findFirst().orElseThrow().getColumn());
        TenantContextHolder.setTenantId(11L);
        LoginUser user = new LoginUser();
        user.setId(21L);
        user.setTenantId(11L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null));
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        ObjectNode operation = node.putArray("operations").addObject();
        for (String key : List.of("operationId", "sourceType", "sourceLocator", "domain", "subjectType",
                "actionType", "reasonPolicy", "signaturePolicy", "statePolicy", "retentionClass", "owner",
                "applicability")) {
            operation.put(key, "TEST_ONLY");
        }
        operation.putArray("testIds").add("ACT02-LOCK");
        bundle = new GxpAuditPolicyBundle("gxp-audit-policy.v2", "test-v1", "APPROVED",
                "TEST-ONLY-APPROVAL", "a".repeat(64), "b".repeat(64), node.toString(),
                "test-only", "test-only", node);
        version = new GxpAuditPolicyVersionDO();
        version.setTenantId(11L);
        version.setPolicyVersion(bundle.policyVersion());
        version.setPolicyHash(bundle.policyHash());
        version.setApprovalReference(bundle.approvalReference());
        version.setCoverageReportHash("d".repeat(64));
        version.setArtifactHash(bundle.artifactHash());
        version.setSchemaVersion(bundle.schemaVersion());
        version.setCanonicalPolicyJson(bundle.canonicalPolicyJson());
        // Inject by existing field type. Absent tenant/event dependencies remain observable
        // as missing reads, not a reflective setup error masquerading as business RED.
        for (Field field : GxpAuditPolicyActivationServiceImpl.class.getDeclaredFields()) {
            Object dependency = null;
            Class<?> type = field.getType();
            if (type == GxpAuditPolicyBundleLoader.class) {
                GxpAuditPolicyBundleLoader loader = mock(GxpAuditPolicyBundleLoader.class);
                when(loader.load()).thenReturn(bundle);
                dependency = loader;
            } else if (type == TenantMapper.class || type == GxpAuditEventMapper.class
                    || type == GxpAuditLedgerSequenceMapper.class || type == GxpAuditPolicyActivationMapper.class
                    || type == GxpAuditPolicyVersionMapper.class || type == GxpAuditPolicyOperationMapper.class
                    || type == GxpAuditService.class) {
                dependency = mock(type, invocation -> answer(type, invocation));
            }
            if (dependency != null) {
                field.setAccessible(true);
                field.set(service, dependency);
            }
        }
    }

    @AfterEach
    void clearContexts() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void missingTenantRejectsBeforeAnyWrite() {
        tenantExists = false;
        assertRejected();
    }

    @Test
    void existingEventWithoutWatermarkCannotInitializeLedger() {
        ledgerExists = false;
        hasEvent = true;
        assertRejected();
    }

    @Test
    void emptyLedgerInitializesOnlyAfterTenantLockAndEventCheck() {
        ledgerExists = false;
        service.activate(command("d".repeat(64)));
        assertBefore("tenant-lock", "ledger-lock");
        assertBefore("ledger-lock", "event-current-read");
        assertBefore("event-current-read", "request-current-read");
        assertEquals(1, writes.stream().filter("ledger-insert"::equals).count());
        assertEquals(0L, inserted.getEffectiveAfterSequence());
    }

    @Test
    void existingWatermarkLocksTenantThenLedgerThenRequestWithoutReinitializing() {
        service.activate(command("d".repeat(64)));
        assertBefore("tenant-lock", "ledger-lock");
        assertBefore("ledger-lock", "request-current-read");
        assertFalse(writes.contains("ledger-insert"));
        assertEquals(9L, inserted.getEffectiveAfterSequence());
    }

    @Test
    void winnerInvisibleToOldSnapshotReplaysUnderLockWithoutWrites() {
        givenWinner();
        GxpAuditPolicyActivationResult result = service.activate(command("d".repeat(64)));
        assertEquals(new GxpAuditPolicyActivationResult(31L, "test-v1", "a".repeat(64), true), result);
        assertTrue(writes.isEmpty(), writes.toString());
        assertBefore("ledger-lock", "request-current-read");
    }

    @Test
    void winnerWithDifferentCoverageRejectsUnderLockWithoutWrites() {
        givenWinner();
        ServiceException error = assertRejected("e".repeat(64));
        assertTrue(error.getMessage().contains("activation-request-payload-conflict"));
    }

    @Test
    void invalidWatermarkRejectsWithoutWrites() {
        nextSequence = 0L;
        assertRejected();
    }

    @Test
    void ledgerReadFailurePropagatesWithoutWrites() {
        ledgerFailure = new IllegalStateException("test-ledger-read-failure");
        assertSame(ledgerFailure, assertThrows(IllegalStateException.class,
                () -> service.activate(command("d".repeat(64)))));
        assertTrue(writes.isEmpty(), writes.toString());
    }

    @Test
    void newRequestSeesCommittedOperationWithoutDuplicateInsert() {
        givenWinner();
        differentRequest = true;
        service.activate(command("d".repeat(64)));
        assertFalse(writes.contains("GxpAuditPolicyOperationMapper:insert"), writes.toString());
        assertFalse(writes.contains("GxpAuditPolicyVersionMapper:insert"), writes.toString());
        assertEquals(1, writes.stream().filter("GxpAuditPolicyActivationMapper:insert"::equals).count());
        assertEquals(1, writes.stream().filter("append"::equals).count());
    }

    @Test
    void newRequestUsesCommittedPreviousActivationHashDespiteOldSnapshot() {
        givenWinner();
        differentRequest = true;
        service.activate(command("d".repeat(64)));
        assertEquals(winner.getActivationHash(), inserted.getPreviousActivationHash());
        assertTrue(reads.contains("version-current-read"), reads.toString());
        assertTrue(reads.contains("latest-current-read"), reads.toString());
    }

    private Object answer(Class<?> type, InvocationOnMock invocation) throws Throwable {
        String method = invocation.getMethod().getName();
        if (method.equals("toString")) return type.getSimpleName();
        if (method.startsWith("insert") || method.startsWith("update") || method.startsWith("delete")) {
            writes.add(type == GxpAuditLedgerSequenceMapper.class ? "ledger-insert" : type.getSimpleName() + ":" + method);
            if (type == GxpAuditPolicyActivationMapper.class) {
                inserted = invocation.getArgument(0);
                inserted.setId(99L);
            }
            return 1;
        }
        if (type == GxpAuditService.class && method.equals("append")) {
            writes.add("append");
            return null; // activate does not consume the append return value.
        }
        String sql = selectSql(type, invocation);
        boolean currentRead = sql.toUpperCase(java.util.Locale.ROOT).contains("FOR UPDATE");
        if (type == TenantMapper.class && method.startsWith("select")) {
            if (currentRead) { reads.add("tenant-lock"); tenantLocked = true; }
            TenantDO tenant = new TenantDO();
            tenant.setId(11L);
            return tenantExists ? tenant : null;
        }
        if (type == GxpAuditLedgerSequenceMapper.class && method.equals("selectByTenantIdForUpdate")) {
            assertTrue(currentRead, sql);
            assertTrue(sql.contains("tenant_id"), sql);
            reads.add("ledger-lock");
            if (ledgerFailure != null) throw ledgerFailure;
            ledgerLocked = true;
            winnerCommitted = winner != null;
            if (!ledgerExists) return null;
            GxpAuditLedgerSequenceDO row = new GxpAuditLedgerSequenceDO();
            row.setTenantId(11L);
            row.setNextLedgerSequence(nextSequence);
            return row;
        }
        if (type == GxpAuditEventMapper.class && method.startsWith("select")) {
            assertTrue(currentRead && sql.contains("tenant_id"), sql);
            if (currentRead) reads.add("event-current-read");
            return hasEvent ? new GxpAuditEventDO() : null;
        }
        if (type == GxpAuditPolicyActivationMapper.class && method.contains("RequestId")) {
            if (currentRead) reads.add("request-current-read");
            return currentRead && tenantLocked && ledgerLocked && winnerCommitted && !differentRequest ? winner : null;
        }
        if (type == GxpAuditPolicyActivationMapper.class && method.contains("Latest")) {
            if (currentRead) reads.add("latest-current-read");
            assertTrue(sql.contains("tenant_id") && sql.toUpperCase(java.util.Locale.ROOT).contains("LIMIT 1"), sql);
            return currentRead && winnerCommitted ? winner : null;
        }
        if (type == GxpAuditPolicyVersionMapper.class && method.startsWith("select")) {
            if (currentRead) reads.add("version-current-read");
            assertTrue(sql.contains("tenant_id") && sql.contains("policy_version"), sql);
            return currentRead ? version : null;
        }
        if (type == GxpAuditPolicyOperationMapper.class && method.startsWith("select")) {
            assertTrue(sql.contains("tenant_id") && sql.contains("policy_version") && sql.contains("operation_id"), sql);
            return currentRead && winnerCommitted ? new GxpAuditPolicyOperationDO() : null;
        }
        return RETURNS_DEFAULTS.answer(invocation);
    }

    /** Execute the actual mapper default method on a probe and capture its real wrapper.
     * Annotation SQL is read directly. Neither the method name nor the simulated row proves SQL. */
    private String selectSql(Class<?> type, InvocationOnMock invocation) throws Throwable {
        for (Object arg : invocation.getArguments()) {
            if (arg instanceof AbstractWrapper<?, ?, ?> wrapper) return wrapper.getSqlSegment();
        }
        Select annotation = invocation.getMethod().getAnnotation(Select.class);
        if (annotation != null) return String.join(" ", annotation.value());
        if (!invocation.getMethod().isDefault()) return "";
        List<String> captured = new ArrayList<>();
        Object probe = mock(type, call -> {
            for (Object arg : call.getArguments()) {
                if (arg instanceof AbstractWrapper<?, ?, ?> wrapper) {
                    captured.add(wrapper.getSqlSegment());
                    return null;
                }
            }
            if (call.getMethod().isDefault()) return call.callRealMethod();
            return RETURNS_DEFAULTS.answer(call);
        });
        invocation.getMethod().invoke(probe, invocation.getArguments());
        assertFalse(captured.isEmpty(), "Mapper default method must expose its actual query wrapper");
        return String.join(" ", captured);
    }

    private void givenWinner() {
        winner = new GxpAuditPolicyActivationDO();
        winner.setId(31L);
        winner.setTenantId(11L);
        winner.setRequestId("test-lock-request");
        winner.setActorId(21L);
        winner.setPolicyVersion(bundle.policyVersion());
        winner.setPolicyHash(bundle.policyHash());
        winner.setApprovalReference(bundle.approvalReference());
        winner.setActivationHash("c".repeat(64));
        winner.setCanonicalActivationJson("{\"committed\":true}");
    }

    private GxpAuditPolicyActivationCommand command(String coverage) {
        return new GxpAuditPolicyActivationCommand(11L, 21L, bundle.approvalReference(),
                "test-lock-request", coverage);
    }

    private void assertRejected() { assertRejected("d".repeat(64)); }

    private ServiceException assertRejected(String coverage) {
        ServiceException error = assertThrows(ServiceException.class, () -> service.activate(command(coverage)));
        assertEquals(GXP_AUDIT_POLICY_ACTIVATION_INVALID.getCode(), error.getCode());
        assertTrue(writes.isEmpty(), writes.toString());
        return error;
    }

    private void assertBefore(String first, String second) {
        assertTrue(reads.contains(first) && reads.contains(second)
                && reads.indexOf(first) < reads.indexOf(second), reads.toString());
    }
}
