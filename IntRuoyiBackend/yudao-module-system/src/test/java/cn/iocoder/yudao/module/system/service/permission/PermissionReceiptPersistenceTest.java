package cn.iocoder.yudao.module.system.service.permission;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.*;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.*;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleMapper;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditServiceImpl;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.sql.Connection;
import java.sql.SQLException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Actual service proxy, audit writer, SQL and commit/rollback; not MySQL concurrency evidence. */
@Import({PermissionServiceImpl.class, GxpAuditServiceImpl.class, PermissionCommandProtocol.class,
        PermissionReceiptPersistenceTest.ProbeConfiguration.class})
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:permission_receipt;MODE=MYSQL;DATABASE_TO_UPPER=false;NON_KEYWORDS=value;")
class PermissionReceiptPersistenceTest extends BaseDbUnitTest {
    enum Operation { ROLE_MENU, USER_ROLE, DATA_SCOPE }
    @Resource PermissionServiceImpl service;
    @Resource javax.sql.DataSource dataSource;
    @Resource RoleMapper roleMapper;
    @Resource GxpAuditPolicyOperationMapper policies;
    @Resource GxpAuditPolicyActivationMapper activations;
    @Resource GxpAuditLedgerSequenceMapper ledger;
    @Resource SqlProbe probe;
    @Resource org.apache.ibatis.session.SqlSessionFactory sqlSessionFactory;
    @MockitoBean RoleService roles;
    @MockitoBean MenuService menus;
    @MockitoBean DeptService departments;
    @MockitoBean AdminUserService users;
    @MockitoBean SystemEntitlementService entitlements;
    @MockitoBean TemporaryRoleGrantService temporaryRoles;
    private JdbcTemplate db;
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void fixture() {
        probe.clear();
        assertTrue(AopUtils.isAopProxy(service));
        assertTrue(sqlSessionFactory.getConfiguration().getInterceptors().contains(probe),
                "fixture requires the real MyBatis INSERT observer");
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        db = new JdbcTemplate(dataSource);
        // Owned H2 fixture only. Production migration remains a separate RED/GREEN gate.
        db.execute("CREATE TABLE IF NOT EXISTS system_gxp_command_receipt ("
                + "id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, operation_id VARCHAR(96) NOT NULL,"
                + "subject_id VARCHAR(128) NOT NULL, source_key VARBINARY(1024) NOT NULL,"
                + "source_key_sha256 BINARY(32) NOT NULL, source_payload_hash BINARY(32) NOT NULL,"
                + "identity_json VARCHAR(8192) NOT NULL, source_json VARCHAR(16384) NOT NULL,"
                + "audit_event_id BIGINT NOT NULL, audit_event_hash BINARY(32) NOT NULL,"
                + "result_json VARCHAR(1024) NOT NULL, created_at_utc TIMESTAMP(3) NOT NULL,"
                + "UNIQUE(tenant_id,operation_id,source_key_sha256), UNIQUE(tenant_id,audit_event_id))");
        db.update("DELETE FROM system_gxp_command_receipt");
        LoginUser actor = new LoginUser();
        actor.setId(1001L); actor.setTenantId(1L);
        actor.setInfo(Map.of("username", "receipt.fixture", LoginUser.INFO_KEY_NICKNAME, "Fixture"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor, null));
        db.update("INSERT INTO system_role(id,name,code,sort,status,type,tenant_id) VALUES(12,'fixture','fixture',1,0,2,1)");
        db.update("INSERT INTO system_users(id,username,canonical_username,nickname,tenant_id) VALUES(12,'fixture','fixture','fixture',1)");
        when(roles.getRole(12L)).thenAnswer(i -> roleMapper.selectById(12L));
        doAnswer(i -> {
            var role = roleMapper.selectById(12L);
            role.setDataScope(i.getArgument(1)); role.setDataScopeDeptIds(i.getArgument(2));
            assertEquals(1, roleMapper.updateById(role));
            return null;
        }).when(roles).updateRoleDataScope(eq(12L), anyInt(), anySet());
        GxpAuditLedgerSequenceDO sequence = new GxpAuditLedgerSequenceDO();
        assertEquals(0, count("gxp_audit_ledger_sequence"), "owned fixture must start without a prior test watermark");
        sequence.setTenantId(1L); sequence.setNextLedgerSequence(1L); ledger.insert(sequence);
        GxpAuditPolicyActivationDO active = new GxpAuditPolicyActivationDO();
        active.setTenantId(1L); active.setPolicyVersion("receipt-test-only");
        active.setPolicyHash("a".repeat(64)); active.setPreviousActivationHash("0".repeat(64));
        active.setRequestId("fixture-only"); active.setActorId(1001L);
        active.setApprovalReference("ISOLATED-TEST-NOT-APPROVAL");
        active.setActivatedAtUtc(LocalDateTime.of(2026, 1, 1, 0, 0)); active.setEffectiveAfterSequence(0L);
        active.setCanonicalActivationJson("{}"); active.setActivationHash("b".repeat(64));
        activations.insert(active);
        for (Operation op : Operation.values()) {
            GxpAuditPolicyOperationDO policy = new GxpAuditPolicyOperationDO();
            policy.setTenantId(1L); policy.setPolicyVersion("receipt-test-only"); policy.setOperationId(operationId(op));
            policy.setSourceType("SERVICE_METHOD"); policy.setSourceLocator("PermissionServiceImpl");
            policy.setDomain("SYSTEM"); policy.setSubjectType(op == Operation.USER_ROLE ? "USER_ROLE" : "ROLE_PERMISSION");
            policy.setActionType("UPDATE"); policy.setReasonPolicy("USER_REQUIRED"); policy.setSignaturePolicy("NONE");
            policy.setStatePolicy("PRESENT_TO_PRESENT"); policy.setRetentionClass("TEST_ONLY");
            policy.setTestIds("PermissionReceiptPersistenceTest"); policy.setOwner("TEST_ONLY");
            policy.setApplicability("GXP"); policy.setActive(true); policies.insert(policy);
        }
    }

    @AfterEach
    void cleanupReceipt() {
        probe.clear();
        db.execute("ALTER TABLE system_gxp_command_receipt DROP CONSTRAINT IF EXISTS reject_receipt");
        db.update("DELETE FROM system_gxp_command_receipt");
        // BaseDbUnitTest clean.sql clears audit events/policy, but not this fixture's watermark.
        db.update("DELETE FROM gxp_audit_ledger_sequence WHERE tenant_id = 1");
        SecurityContextHolder.clearContext();
    }

    private String operationId(Operation op) {
        return "system.permission." + switch (op) {
            case ROLE_MENU -> "role-menu.assign";
            case USER_ROLE -> "user-role.assign";
            case DATA_SCOPE -> "role-data-scope.assign";
        };
    }
    private void invoke(Operation op, String reason) {
        invoke(op, reason, "owned-v2-command");
    }
    private void invoke(Operation op, String reason, String key) {
        switch (op) {
            case ROLE_MENU -> service.assignRoleMenu(12L, Set.of(2L), reason, key);
            case USER_ROLE -> service.assignUserRole(12L, Set.of(2L), reason, key);
            case DATA_SCOPE -> service.assignRoleDataScope(12L, 2, Set.of(2L), reason, key);
        }
    }
    private int count(String table) { return db.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class); }
    private String businessState(Operation op) {
        return switch (op) {
            case ROLE_MENU -> db.queryForList("SELECT role_id,menu_id,deleted FROM system_role_menu ORDER BY id").toString();
            case USER_ROLE -> db.queryForList("SELECT user_id,role_id,deleted FROM system_user_role ORDER BY id").toString();
            case DATA_SCOPE -> db.queryForList("SELECT data_scope,data_scope_dept_ids FROM system_role WHERE id=12").toString();
        };
    }

    @ParameterizedTest @EnumSource(Operation.class)
    void commitsActualReceiptAndClosedVoidResult(Operation op) throws Exception {
        invoke(op, "reason");
        assertEquals(1, count("gxp_audit_event"));
        assertEquals(1, count("system_gxp_command_receipt"), "service must persist receipt, not just evidence text");
        var receipt = db.queryForMap("SELECT * FROM system_gxp_command_receipt");
        assertEquals(operationId(op), receipt.get("operation_id"));
        assertEquals(json.readTree("{\"schemaVersion\":\"system.permission.result.v1\",\"kind\":\"VOID\"}"),
                json.readTree(receipt.get("result_json").toString()));
        assertEquals(1, db.queryForObject("SELECT COUNT(*) FROM gxp_audit_event WHERE id=?", Integer.class, receipt.get("audit_event_id")));
    }

    @ParameterizedTest @EnumSource(Operation.class)
    void replaysHistoricalResultAfterBusinessStateChangesWithoutWrites(Operation op) {
        invoke(op, "reason");
        switch (op) {
            case ROLE_MENU -> db.update("DELETE FROM system_role_menu");
            case USER_ROLE -> db.update("DELETE FROM system_user_role");
            case DATA_SCOPE -> db.update("UPDATE system_role SET data_scope=1,data_scope_dept_ids='' WHERE id=12");
        }
        String before = businessState(op);
        assertDoesNotThrow(() -> invoke(op, "reason"));
        assertEquals(before, businessState(op), "replay must not reapply current business writes");
        assertEquals(1, count("gxp_audit_event"));
        assertEquals(1, count("system_gxp_command_receipt"));
    }

    @ParameterizedTest @EnumSource(Operation.class)
    void alteredInputCannotReturnVoidSuccess(Operation op) {
        invoke(op, "reason");
        String before = businessState(op);
        assertThrows(RuntimeException.class, () -> invoke(op, "different reason"));
        assertEquals(before, businessState(op));
        assertEquals(1, count("gxp_audit_event"));
    }

    @ParameterizedTest @EnumSource(Operation.class)
    void receiptInsertFailureRollsBackBusinessEventRelationsAndWatermark(Operation op) {
        db.execute("ALTER TABLE system_gxp_command_receipt ADD CONSTRAINT reject_receipt CHECK (id < 0)");
        String before = businessState(op);
        ProbeState observed = probe.arm(null, connection -> {
            assertSameTransactionConnection(connection);
            assertNotEquals(before, businessState(op), "business SQL must precede receipt INSERT");
            assertEquals(1, count("gxp_audit_event"), "event must already exist inside this transaction");
            assertEquals(2L, ledger.selectById(1L).getNextLedgerSequence());
            assertEquals(0, count("system_gxp_command_receipt"));
        }, null);
        RuntimeException failure = assertThrows(RuntimeException.class, () -> invoke(op, "reason"));
        assertEquals(1, observed.eventAttempts);
        assertEquals(1, observed.receiptAttempts, "must reach the real receipt INSERT");
        SQLException sql = sqlCause(failure);
        assertEquals("23513", sql.getSQLState(), "H2 CHECK violation, not any earlier failure");
        assertTrue(sql.getMessage().toLowerCase(java.util.Locale.ROOT).contains("reject_receipt"));
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        assertEquals(before, businessState(op));
        assertEquals(0, count("gxp_audit_event"));
        assertEquals(0, count("gxp_audit_event_relation"));
        assertEquals(0, count("system_gxp_command_receipt"));
        assertEquals(1L, ledger.selectById(1L).getNextLedgerSequence());
    }

    @ParameterizedTest @EnumSource(Operation.class)
    void missingOperationRejectsBeforeBusinessWrites(Operation op) {
        db.update("DELETE FROM gxp_audit_policy_operation");
        String before = businessState(op);
        ProbeState observed = probe.arm(null, null, null);
        assertThrows(RuntimeException.class, () -> invoke(op, "reason"));
        assertEquals(0, observed.writes, "this is preflight rejection, not append rollback evidence");
        assertEquals(before, businessState(op));
        assertEquals(0, count("gxp_audit_event"));
        assertEquals(0, count("system_gxp_command_receipt"));
        assertEquals(1L, ledger.selectById(1L).getNextLedgerSequence());
    }

    @ParameterizedTest @EnumSource(Operation.class)
    void appendInsertFailureAfterObservedBusinessWriteRollsBackWholeTransaction(Operation op) {
        String before = businessState(op);
        RuntimeException injected = new IllegalStateException("PRC02-owned-event-insert-fault");
        ProbeState observed = probe.arm(connection -> {
            assertSameTransactionConnection(connection);
            assertNotEquals(before, businessState(op), "actual business SQL must have executed");
            assertEquals(0, count("gxp_audit_event"));
            assertEquals(0, count("system_gxp_command_receipt"));
            assertEquals(2L, ledger.selectById(1L).getNextLedgerSequence(), "append allocated the watermark");
        }, null, injected);
        RuntimeException failure = assertThrows(RuntimeException.class, () -> invoke(op, "reason"));
        assertTrue(hasCause(failure, injected), "must propagate the designated INSERT fault");
        assertEquals(1, observed.eventAttempts);
        assertEquals(0, observed.receiptAttempts);
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        assertEquals(before, businessState(op));
        assertEquals(0, count("gxp_audit_event"));
        assertEquals(0, count("gxp_audit_event_relation"));
        assertEquals(0, count("system_gxp_command_receipt"));
        assertEquals(1L, ledger.selectById(1L).getNextLedgerSequence());
    }

    @ParameterizedTest @EnumSource(Operation.class)
    void eventWithoutReceiptCannotBecomeOrdinarySuccess(Operation op) {
        invoke(op, "reason");
        db.update("DELETE FROM system_gxp_command_receipt");
        String before = businessState(op);
        assertThrows(RuntimeException.class, () -> invoke(op, "reason"));
        assertEquals(before, businessState(op));
        assertEquals(1, count("gxp_audit_event"));
        assertEquals(0, count("system_gxp_command_receipt"));
    }

    static Stream<Arguments> receiptMutations() {
        return Stream.of(Operation.values()).flatMap(op -> Stream.of(
                "source_key", "source_key_sha256", "source_payload_hash", "subject_id",
                "audit_event_id", "audit_event_hash", "identity_json", "source_json", "result_json")
                .map(field -> Arguments.of(op, field)));
    }

    @ParameterizedTest @MethodSource("receiptMutations")
    void singleReceiptFieldTamperingRejectsWithoutRepair(Operation op, String field) throws Exception {
        invoke(op, "reason");
        assertDoesNotThrow(() -> invoke(op, "reason"), "paired legitimate replay must succeed");
        Object replacement = switch (field) {
            case "source_key" -> "damaged-original".getBytes(StandardCharsets.UTF_8);
            case "source_key_sha256", "source_payload_hash", "audit_event_hash" -> new byte[32];
            case "subject_id" -> "SYSTEM_ROLE:999";
            case "audit_event_id" -> -1L;
            case "identity_json" -> {
                var value = (com.fasterxml.jackson.databind.node.ArrayNode) json.readTree(
                        db.queryForObject("SELECT identity_json FROM system_gxp_command_receipt", String.class));
                value.add("unexpected");
                yield json.writeValueAsString(value);
            }
            case "source_json", "result_json" -> {
                ObjectNode value = (ObjectNode) json.readTree(db.queryForObject(
                        "SELECT " + field + " FROM system_gxp_command_receipt", String.class));
                value.put("unexpected", "not-in-contract");
                yield json.writeValueAsString(value);
            }
            default -> throw new AssertionError("unknown owned mutation");
        };
        assertEquals(1, db.update("UPDATE system_gxp_command_receipt SET " + field + "=?", replacement));
        String damaged = protocolTablesSnapshot();
        String business = businessState(op);
        ProbeState observed = probe.arm(null, null, null);
        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> invoke(op, "reason"));
        String expected = switch (field) {
            case "source_key_sha256" -> "event-without-receipt";
            case "audit_event_id", "audit_event_hash" -> "event-binding-conflict";
            default -> "receipt-binding-conflict";
        };
        assertTrue(failure.getMessage().contains(expected), failure.getMessage());
        assertEquals(0, observed.writes);
        assertEquals(business, businessState(op));
        assertEquals(damaged, protocolTablesSnapshot(), "reject must not repair or rewrite damaged history");
    }

    @ParameterizedTest @EnumSource(Operation.class)
    void receiptWithoutEventRejectsWithoutRecreatingHistory(Operation op) throws Exception {
        invoke(op, "reason");
        assertDoesNotThrow(() -> invoke(op, "reason"));
        assertEquals(1, db.update("DELETE FROM gxp_audit_event"));
        String damaged = protocolTablesSnapshot();
        String business = businessState(op);
        ProbeState observed = probe.arm(null, null, null);
        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> invoke(op, "reason"));
        assertTrue(failure.getMessage().contains("event-binding-conflict"));
        assertEquals(0, observed.writes);
        assertEquals(business, businessState(op));
        assertEquals(damaged, protocolTablesSnapshot());
        assertEquals(0, count("gxp_audit_event"));
        assertEquals(1, count("system_gxp_command_receipt"));
    }

    static Stream<Arguments> receiptMemberMutations() {
        return Stream.of(Operation.values()).flatMap(op -> {
            Stream<String> common = Stream.of("identity:0", "identity:1", "identity:2",
                    "source:schemaVersion", "source:sourceKey", "source:sourceKeySha256",
                    "source:actorId", "source:inputSha256", "input:subject", "input:ids",
                    "input:reason", "result:schemaVersion", "result:kind");
            return Stream.concat(common, op == Operation.DATA_SCOPE ? Stream.of("input:dataScope") : Stream.empty())
                    .map(member -> Arguments.of(op, member));
        });
    }

    @ParameterizedTest @MethodSource("receiptMemberMutations")
    void receiptJsonMemberTamperingRejectsWithoutRepair(Operation op, String member) throws Exception {
        invoke(op, "reason");
        assertDoesNotThrow(() -> invoke(op, "reason"));
        String[] path = member.split(":");
        String column = switch (path[0]) {
            case "identity" -> "identity_json";
            case "source", "input" -> "source_json";
            case "result" -> "result_json";
            default -> throw new AssertionError("unknown mutation path");
        };
        var node = json.readTree(db.queryForObject("SELECT " + column + " FROM system_gxp_command_receipt", String.class));
        if (path[0].equals("identity")) {
            var identity = (com.fasterxml.jackson.databind.node.ArrayNode) node;
            int index = Integer.parseInt(path[1]);
            assertNotNull(identity.get(index));
            identity.set(index, json.getNodeFactory().textNode("changed"));
        } else {
            ObjectNode target = (ObjectNode) (path[0].equals("input") ? node.get("input") : node);
            String field = switch (path[1]) {
                case "subject" -> op == Operation.USER_ROLE ? "userId" : "roleId";
                case "ids" -> switch (op) {
                    case ROLE_MENU -> "menuIds";
                    case USER_ROLE -> "roleIds";
                    case DATA_SCOPE -> "dataScopeDeptIds";
                };
                default -> path[1];
            };
            assertTrue(target.has(field), "fixture must mutate an existing member: " + member);
            if (field.equals("dataScope")) target.put(field, 1);
            else if (path[1].equals("ids")) target.putArray(field).add("999");
            else target.put(field, "changed");
        }
        assertEquals(1, db.update("UPDATE system_gxp_command_receipt SET " + column + "=?", json.writeValueAsString(node)));
        assertRejectedWithoutRepair(op, "receipt-binding-conflict");
    }

    static Stream<Arguments> evidenceMutations() {
        return Stream.of(Operation.values()).flatMap(op -> Stream.of(
                "evidenceType", "sourceId", "sourceVersion", "sha256", "role", "extra", "duplicate")
                .map(field -> Arguments.of(op, field)));
    }

    @ParameterizedTest @MethodSource("evidenceMutations")
    void evidenceMemberTamperingReachesBindingCheckWithoutRepair(Operation op, String field) throws Exception {
        invoke(op, "reason");
        assertDoesNotThrow(() -> invoke(op, "reason"));
        var manifest = (com.fasterxml.jackson.databind.node.ArrayNode) json.readTree(
                db.queryForObject("SELECT evidence_manifest_json FROM gxp_audit_event", String.class));
        assertEquals(1, manifest.size());
        ObjectNode evidence = (ObjectNode) manifest.get(0);
        switch (field) {
            case "extra" -> evidence.put("unexpected", "changed");
            case "duplicate" -> manifest.add(evidence.deepCopy());
            default -> {
                assertTrue(evidence.has(field), "fixture must mutate an existing evidence member");
                evidence.put(field, field.equals("sha256") ? "0".repeat(64) : "changed");
            }
        }
        String manifestJson = json.writeValueAsString(manifest);
        ObjectNode canonical = (ObjectNode) json.readTree(
                db.queryForObject("SELECT canonical_event_json FROM gxp_audit_event", String.class));
        assertTrue(canonical.get("evidenceManifestJson").isTextual());
        canonical.put("evidenceManifestJson", manifestJson);
        String canonicalJson = json.writeValueAsString(canonical);
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(canonicalJson.getBytes(StandardCharsets.UTF_8));
        // Isolated H2 fault injection: bypass earlier hash/column mismatch to test evidence binding itself.
        assertEquals(1, db.update("UPDATE gxp_audit_event SET evidence_manifest_json=?,canonical_event_json=?,event_hash=?",
                manifestJson, canonicalJson, HexFormat.of().formatHex(hash)));
        assertEquals(1, db.update("UPDATE system_gxp_command_receipt SET audit_event_hash=?", hash));
        assertRejectedWithoutRepair(op, "receipt-evidence-conflict");
    }

    static Stream<Arguments> eventBindingMutations() {
        return Stream.of(Operation.values()).flatMap(op -> Stream.of(
                "actor_id", "subject_id", "idempotency_key", "event_hash", "canonical_actor")
                .map(field -> Arguments.of(op, field)));
    }

    @ParameterizedTest @MethodSource("eventBindingMutations")
    void eventIdentityTamperingRejectsAtSpecifiedBoundary(Operation op, String field) throws Exception {
        invoke(op, "reason");
        assertDoesNotThrow(() -> invoke(op, "reason"));
        if (field.equals("canonical_actor")) {
            ObjectNode canonical = (ObjectNode) json.readTree(
                    db.queryForObject("SELECT canonical_event_json FROM gxp_audit_event", String.class));
            assertEquals(1001L, canonical.get("actorId").longValue());
            canonical.put("actorId", 1002L);
            String changed = json.writeValueAsString(canonical);
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(changed.getBytes(StandardCharsets.UTF_8));
            assertEquals(1, db.update("UPDATE gxp_audit_event SET canonical_event_json=?,event_hash=?",
                    changed, HexFormat.of().formatHex(hash)));
            assertEquals(1, db.update("UPDATE system_gxp_command_receipt SET audit_event_hash=?", hash));
        } else {
            Object changed = switch (field) {
                case "actor_id" -> 1002L;
                case "subject_id" -> "SYSTEM_ROLE:999";
                case "idempotency_key" -> "GXP2:" + "0".repeat(64);
                case "event_hash" -> "0".repeat(64);
                default -> throw new AssertionError("unknown event mutation");
            };
            assertEquals(1, db.update("UPDATE gxp_audit_event SET " + field + "=?", changed));
        }
        assertRejectedWithoutRepair(op, field.equals("canonical_actor")
                ? "event-columns-conflict" : "event-binding-conflict");
    }

    private void assertRejectedWithoutRepair(Operation op, String reason) throws Exception {
        String damaged = protocolTablesSnapshot();
        String business = businessState(op);
        ProbeState observed = probe.arm(null, null, null);
        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> invoke(op, "reason"));
        assertTrue(failure.getMessage().contains(reason), failure.getMessage());
        assertEquals(0, observed.writes);
        assertEquals(business, businessState(op));
        assertEquals(damaged, protocolTablesSnapshot(), "no repair or rewrite of corrupted history");
    }

    private String protocolTablesSnapshot() throws Exception {
        return json.writeValueAsString(List.of(
                storedRows("system_gxp_command_receipt", "id"),
                storedRows("gxp_audit_event", "id"),
                storedRows("gxp_audit_event_relation", "id"),
                storedRows("gxp_audit_ledger_sequence", "tenant_id")));
    }

    private List<List<String>> storedRows(String table, String order) {
        return db.query("SELECT * FROM " + table + " ORDER BY " + order, (rs, row) -> {
            List<String> values = new java.util.ArrayList<>();
            for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                Object value = rs.getObject(i);
                values.add(value instanceof byte[] bytes ? HexFormat.of().formatHex(bytes) : rs.getString(i));
            }
            return values;
        });
    }

    static Stream<Arguments> numericActors() {
        return Stream.of(Operation.values()).flatMap(op ->
                Stream.of(1001L, 9007199254740993L, Long.MAX_VALUE).map(actor -> Arguments.of(op, actor)));
    }

    @ParameterizedTest @MethodSource("numericActors")
    void replayPreservesExactIntegralBindingsAcrossJavaNodeWidths(Operation op, long actorId) throws Exception {
        LoginUser actor = new LoginUser(); actor.setId(actorId); actor.setTenantId(1L);
        actor.setInfo(Map.of("username", "numeric.fixture", LoginUser.INFO_KEY_NICKNAME, "Fixture"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor, null));
        invoke(op, "reason");
        var canonical = json.readTree(db.queryForObject("SELECT canonical_event_json FROM gxp_audit_event", String.class));
        assertInstanceOf(IntNode.class, canonical.get("tenantId"));
        assertInstanceOf(LongNode.class, json.valueToTree(1L));
        assertEquals(java.math.BigInteger.ONE, canonical.get("tenantId").bigIntegerValue());
        assertTrue(canonical.get("actorId").isIntegralNumber());
        assertEquals(java.math.BigInteger.valueOf(actorId), canonical.get("actorId").bigIntegerValue());
        String before = businessState(op);
        ProbeState observed = probe.arm(null, null, null);
        assertDoesNotThrow(() -> invoke(op, "reason"), "equal JSON integers must not conflict due to IntNode/LongNode");
        assertEquals(0, observed.writes);
        assertEquals(before, businessState(op));
        assertEquals(1, count("gxp_audit_event"));
        assertEquals(1, count("system_gxp_command_receipt"));
        assertEquals(2L, ledger.selectById(1L).getNextLedgerSequence());
    }

    static Stream<Arguments> invalidNumericBindings() {
        return Stream.of(Operation.values()).flatMap(op -> Stream.of("string", "fractional", "different", "missing")
                .map(kind -> Arguments.of(op, kind)));
    }

    @ParameterizedTest @MethodSource("invalidNumericBindings")
    void replayRejectsChangedJsonTypeOrValueWithoutCoercion(Operation op, String kind) throws Exception {
        invoke(op, "reason");
        // Paired positive must work first; an unrelated earlier rejection is not negative evidence.
        assertDoesNotThrow(() -> invoke(op, "reason"));
        ObjectNode canonical = (ObjectNode) json.readTree(
                db.queryForObject("SELECT canonical_event_json FROM gxp_audit_event", String.class));
        switch (kind) {
            case "string" -> canonical.put("tenantId", "1");
            case "fractional" -> canonical.put("tenantId", 1.0);
            case "different" -> canonical.put("tenantId", 2);
            case "missing" -> canonical.remove("tenantId");
            default -> throw new AssertionError("unknown owned fixture mutation");
        }
        String damaged = json.writeValueAsString(canonical);
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(damaged.getBytes(StandardCharsets.UTF_8));
        // Owned H2 corruption only. Consistent hash references let this reach column validation.
        db.update("UPDATE gxp_audit_event SET canonical_event_json=?,event_hash=?", damaged, HexFormat.of().formatHex(hash));
        db.update("UPDATE system_gxp_command_receipt SET audit_event_hash=?", hash);
        String before = businessState(op);
        ProbeState observed = probe.arm(null, null, null);
        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> invoke(op, "reason"));
        assertTrue(failure.getMessage().contains("event-columns-conflict"));
        assertEquals(0, observed.writes);
        assertEquals(before, businessState(op));
        assertEquals(damaged, db.queryForObject("SELECT canonical_event_json FROM gxp_audit_event", String.class));
        assertArrayEquals(hash, db.queryForObject("SELECT audit_event_hash FROM system_gxp_command_receipt", byte[].class));
        assertEquals(1, count("gxp_audit_event"));
        assertEquals(1, count("system_gxp_command_receipt"));
        assertEquals(2L, ledger.selectById(1L).getNextLedgerSequence());
    }

    private void assertSameTransactionConnection(Connection connection) {
        assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
        try (var statement = connection.prepareStatement("SELECT SESSION_ID()"); var result = statement.executeQuery()) {
            assertTrue(result.next());
            assertEquals(db.queryForObject("SELECT SESSION_ID()", Long.class).longValue(), result.getLong(1));
        } catch (SQLException failure) {
            throw new AssertionError("Cannot verify actual transaction connection", failure);
        }
    }

    private static boolean hasCause(Throwable failure, Throwable expected) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) if (cause == expected) return true;
        return false;
    }

    private static SQLException sqlCause(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql) return sql;
        }
        throw new AssertionError("Expected actual SQL constraint failure", failure);
    }

    static final class ProbeState {
        int writes;
        int eventAttempts;
        int receiptAttempts;
        Consumer<Connection> atEvent;
        Consumer<Connection> atReceipt;
        RuntimeException eventFailure;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ProbeConfiguration {
        @Bean SqlProbe permissionReceiptSqlProbe() { return new SqlProbe(); }
    }

    @Intercepts(@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}))
    static class SqlProbe implements Interceptor {
        private final ThreadLocal<ProbeState> active = new ThreadLocal<>();
        ProbeState arm(Consumer<Connection> atEvent, Consumer<Connection> atReceipt, RuntimeException eventFailure) {
            ProbeState state = new ProbeState(); state.atEvent = atEvent;
            state.atReceipt = atReceipt; state.eventFailure = eventFailure;
            active.set(state); return state;
        }
        void clear() { active.remove(); }
        @Override public Object intercept(Invocation invocation) throws Throwable {
            ProbeState state = active.get();
            if (state != null) {
                state.writes++;
                String id = ((MappedStatement) invocation.getArgs()[0]).getId();
                if (id.equals(GxpAuditEventMapper.class.getName() + ".insert")) {
                    state.eventAttempts++;
                    if (state.atEvent != null) state.atEvent.accept(((Executor) invocation.getTarget()).getTransaction().getConnection());
                    if (state.eventFailure != null) throw state.eventFailure;
                }
                if (id.equals(cn.iocoder.yudao.module.system.dal.mysql.permission.PermissionCommandReceiptMapper.class.getName() + ".insert")) {
                    state.receiptAttempts++;
                    if (state.atReceipt != null) state.atReceipt.accept(((Executor) invocation.getTarget()).getTransaction().getConnection());
                }
            }
            return invocation.proceed();
        }
    }
}
