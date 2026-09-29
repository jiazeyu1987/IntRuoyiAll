package cn.iocoder.yudao.module.system.service.gxpaudit;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.*;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.*;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.*;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Field;
import java.util.*;

import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * W01-W06: real writer + mapper SQL/wrapper driven deterministic stale-snapshot doubles.
 * Ordinary reads see the old snapshot; locking reads see committed facts.
 * This does not claim InnoDB concurrency or Spring transaction/rollback evidence.
 */
class GxpAuditWriterCurrentReadTest {
    private final GxpAuditServiceImpl service = new GxpAuditServiceImpl();
    private final List<String> reads = new ArrayList<>();
    private final List<String> writes = new ArrayList<>();
    private GxpAuditPolicyOperationDO policy;
    private GxpAuditEventDO committed;
    private GxpAuditEventDO inserted;
    private boolean staleSnapshot;
    private boolean stalePolicy;
    private boolean locked;
    private boolean missingPolicy;
    private boolean missingLedger;
    private Long watermark = 1L;

    @BeforeEach
    void fixture() throws Exception {
        Configuration configuration = new Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        for (Class<?> entity : List.of(GxpAuditEventDO.class, GxpAuditPolicyOperationDO.class,
                GxpAuditPolicyActivationDO.class)) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, "writer-current-read"), entity);
        }
        LoginUser actor = new LoginUser();
        actor.setId(21L);
        actor.setTenantId(11L);
        actor.setInfo(Map.of("username", "test-actor", LoginUser.INFO_KEY_NICKNAME, "test-actor"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor, null));
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        policy = new GxpAuditPolicyOperationDO();
        policy.setTenantId(11L);
        policy.setPolicyVersion("test-v1");
        policy.setOperationId("test.update");
        policy.setSubjectType("TEST_OBJECT");
        policy.setDomain("TEST");
        policy.setActionType("UPDATE");
        policy.setReasonPolicy("REQUIRED_CATEGORY_AND_TEXT");
        policy.setSignaturePolicy("NOT_REQUIRED");
        policy.setStatePolicy("PRESENT_TO_PRESENT");
        policy.setActive(true);
        policy.setApplicability("GXP");
        for (Field field : GxpAuditServiceImpl.class.getDeclaredFields()) {
            Class<?> type = field.getType();
            if (List.of(GxpAuditEventMapper.class, GxpAuditLedgerSequenceMapper.class,
                    GxpAuditPolicyOperationMapper.class, GxpAuditPolicyActivationMapper.class,
                    GxpAuditEventRelationMapper.class).contains(type)) {
                field.setAccessible(true);
                field.set(service, mock(type, call -> answer(type, call)));
            }
        }
    }

    @AfterEach
    void cleanup() {
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        }
        TransactionSynchronizationManager.clearSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(false);
        SecurityContextHolder.clearContext();
    }

    @Test
    void newKeyChainsToCommittedTailHiddenByOldSnapshot() {
        seedCommittedEvent();
        GxpAuditAppendResult result = service.append(command("new-key"));
        assertEquals(committed.getEventHash(), inserted.getPreviousEventHash());
        assertEquals(2L, result.ledgerSequence());
        assertEquals(DigestUtil.sha256Hex(inserted.getCanonicalEventJson()), result.eventHash());
        assertEquals(3L, watermark);
        assertEquals(11L, inserted.getTenantId());
    }

    @Test
    void committedSameKeyReplaysWithNoWatermarkEventOrRelationWrites() {
        seedCommittedEvent();
        GxpAuditAppendResult result = service.append(command("same-key"));
        assertTrue(result.replayed());
        assertEquals(committed.getId(), result.eventId());
        assertEquals(committed.getEventHash(), result.eventHash());
        assertEquals(committed.getLedgerSequence(), result.ledgerSequence());
        assertEquals(2L, watermark);
        assertTrue(writes.isEmpty(), writes.toString());
    }

    @Test
    void committedSameKeyDifferentPayloadRejectsWithoutWrites() {
        seedCommittedEvent();
        GxpAuditCommand changed = command("same-key");
        changed.setReason("changed reason");
        ServiceException error = assertThrows(ServiceException.class, () -> service.append(changed));
        assertEquals(GXP_AUDIT_IDEMPOTENCY_CONFLICT.getCode(), error.getCode());
        assertEquals(2L, watermark);
        assertTrue(writes.isEmpty(), writes.toString());
    }

    @Test
    void latestCommittedPolicyIsVisibleDespiteOldSnapshot() {
        staleSnapshot = true;
        stalePolicy = true;
        policy.setPolicyVersion("test-v2");
        assertDoesNotThrow(() -> service.append(command("new-policy")));
        assertEquals("test-v2", inserted.getPolicyVersion());
        assertEquals("ledger", reads.get(0), reads.toString());
        assertTrue(reads.contains("activation-current"), reads.toString());
    }

    @Test
    void missingActivePolicyRejectsWithoutWrites() {
        missingPolicy = true;
        ServiceException error = assertThrows(ServiceException.class, () -> service.append(command("missing")));
        assertEquals(GXP_AUDIT_POLICY_NOT_FOUND.getCode(), error.getCode());
        assertTrue(writes.isEmpty(), writes.toString());
    }

    @Test
    void missingWatermarkCannotReplayOrInitialize() {
        seedCommittedEvent();
        missingLedger = true;
        ServiceException error = assertThrows(ServiceException.class, () -> service.append(command("same-key")));
        assertEquals(GXP_AUDIT_APPEND_FAILED.getCode(), error.getCode());
        assertTrue(writes.isEmpty(), writes.toString());
    }

    @Test
    void ledgerLockPrecedesPolicyAndEventReads() {
        service.append(command("ordered"));
        assertEquals("ledger", reads.get(0), reads.toString());
    }

    private void seedCommittedEvent() {
        service.append(command("same-key"));
        committed = inserted;
        inserted = null;
        staleSnapshot = true;
        locked = false;
        reads.clear();
        writes.clear();
    }

    private Object answer(Class<?> type, InvocationOnMock call) throws Throwable {
        String method = call.getMethod().getName();
        if (method.equals("toString")) return type.getSimpleName();
        if (method.startsWith("insert") || method.startsWith("update") || method.startsWith("delete")) {
            writes.add(type.getSimpleName() + ":" + method);
            if (type == GxpAuditLedgerSequenceMapper.class) {
                GxpAuditLedgerSequenceDO row = call.getArgument(0);
                assertEquals(11L, row.getTenantId());
                watermark = row.getNextLedgerSequence();
            } else if (type == GxpAuditEventMapper.class) {
                inserted = call.getArgument(0);
                inserted.setId(100L + inserted.getLedgerSequence());
            }
            return 1;
        }
        if (!method.startsWith("select")) return RETURNS_DEFAULTS.answer(call);
        if (call.getMethod().isDefault() && Arrays.stream(call.getArguments())
                .noneMatch(AbstractWrapper.class::isInstance)) return call.callRealMethod();
        String sql = "";
        Collection<?> values = List.of(call.getArguments());
        for (Object arg : call.getArguments()) {
            if (arg instanceof AbstractWrapper<?, ?, ?> wrapper) {
                sql = wrapper.getSqlSegment();
                values = wrapper.getParamNameValuePairs().values();
            }
        }
        Select select = call.getMethod().getAnnotation(Select.class);
        if (select != null) sql = String.join(" ", select.value());
        assertTrue(sql.contains("tenant_id"), sql);
        assertTrue(values.contains(11L), "actual tenant binding: " + values);
        boolean current = sql.toUpperCase(Locale.ROOT).contains("FOR UPDATE");
        if (type == GxpAuditLedgerSequenceMapper.class) {
            assertTrue(current, sql);
            reads.add("ledger");
            locked = true;
            if (missingLedger) return null;
            GxpAuditLedgerSequenceDO row = new GxpAuditLedgerSequenceDO();
            row.setTenantId(11L);
            row.setNextLedgerSequence(watermark);
            return row;
        }
        if (type == GxpAuditPolicyActivationMapper.class) {
            assertTrue(current && locked, sql);
            reads.add("activation-current");
            if (missingPolicy) return null;
            GxpAuditPolicyActivationDO row = new GxpAuditPolicyActivationDO();
            row.setTenantId(11L);
            row.setPolicyVersion(policy.getPolicyVersion());
            return row;
        }
        if (type == GxpAuditPolicyOperationMapper.class) {
            reads.add("policy");
            assertTrue(sql.contains("operation_id") && values.contains("test.update"), sql);
            if (current) {
                assertTrue(locked && reads.contains("activation-current"), reads.toString());
                assertTrue(values.contains(policy.getPolicyVersion()), values.toString());
            }
            return missingPolicy || (stalePolicy && !current) ? null : policy;
        }
        if (type == GxpAuditEventMapper.class) {
            reads.add("event");
            if (staleSnapshot && (!current || !locked)) return null;
            if (sql.contains("idempotency_key")) {
                return values.contains("same-key") ? committed : null;
            }
            assertTrue(sql.toUpperCase(Locale.ROOT).contains("ORDER BY"), sql);
            return committed;
        }
        return RETURNS_DEFAULTS.answer(call);
    }

    private GxpAuditCommand command(String key) {
        return GxpAuditCommand.builder().operationId("test.update").subjectId("object-1")
                .subjectVersion("2").reason("test reason").idempotencyKey(key).requestId("request-1")
                .source("test#update")
                .beforeState(GxpAuditStateEnvelope.builder().state("PRESENT").objectVersion("1")
                        .canonicalJson("{\"value\":1}").build())
                .afterState(GxpAuditStateEnvelope.builder().state("PRESENT").objectVersion("2")
                        .canonicalJson("{\"value\":2}").build())
                .links(List.of(new GxpAuditRelation("SOURCE", "TEST_OBJECT", "object-1", "2", "hash")))
                .build();
    }
}
