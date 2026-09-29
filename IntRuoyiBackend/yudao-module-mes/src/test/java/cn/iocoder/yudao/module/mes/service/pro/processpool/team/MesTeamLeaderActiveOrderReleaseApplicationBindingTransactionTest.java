package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditCommand;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Real Spring transaction boundary and isolated H2 writes; no external database. */
class MesTeamLeaderActiveOrderReleaseApplicationBindingTransactionTest {

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void existingBindingPersistsTruthfulBeforeAfterAudit(boolean generated) {
        Fixture f = new Fixture(false, false, false);
        var result = f.invoke(generated);
        assertEquals(93L, f.batch());
        assertEquals(2, f.version());
        assertEquals(2, result.getVersion());
        assertEquals(1, f.audits.size(), "existing application binding must append an audit");
        var audit = f.audits.get(0);
        assertEquals("mes.pqc-release.bind-batch", audit.getOperationId());
        assertTrue(audit.getSourceLocator().endsWith(generated ? "#applyGenerated" : "#apply"));
        assertEquals("PQC_RELEASE_PENDING", audit.getBeforeState().getState(),
                "an existing application is not ABSENT");
        assertEquals("1", audit.getBeforeState().getObjectVersion());
        Map<?, ?> before = JsonUtils.parseObject(audit.getBeforeState().getCanonicalJson(), Map.class);
        assertEquals(99, ((Number) before.get("applicationId")).intValue());
        assertNull(before.get("batchExecutionId"));
        Map<?, ?> beforeReceipt = (Map<?, ?>) before.get("applicationReceipt");
        assertEquals(1, ((Number) beforeReceipt.get("version")).intValue());
        assertNull(beforeReceipt.get("batchExecutionId"));
        assertEquals("PQC_RELEASE_PENDING", audit.getAfterState().getState());
        assertEquals("2", audit.getAfterState().getObjectVersion());
        Map<?, ?> after = JsonUtils.parseObject(audit.getAfterState().getCanonicalJson(), Map.class);
        assertEquals(93, ((Number) after.get("batchExecutionId")).intValue());
        assertFalse(audit.getReason().contains("创建"), "binding must not claim application creation");
        assertNotEquals("PQC_RELEASE_APPLY:99", audit.getIdempotencyKey(),
                "binding must not collide with the original creation event");
        assertEquals(1, f.auditRows());
        // replayExisting returns the very same receipt mutated by the first bind.
        f.invoke(generated);
        assertEquals(1, f.bindingAttempts.get());
        assertEquals(1, f.auditCalls.get());
        assertEquals(1, f.auditRows());
        assertEquals(2, f.version());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void auditFailureRollsBackBindingAndVersionInDatabase(boolean generated) {
        Fixture f = new Fixture(false, false, true);
        Throwable failure = null;
        try {
            f.invoke(generated);
        } catch (IllegalStateException ex) {
            failure = ex;
        }
        assertNull(f.batch(), "audit failure must leave the persisted binding empty");
        assertEquals(1, f.version(), "audit failure must roll back the version increment");
        assertEquals(0, f.auditRows(), "audit write before failure must roll back too");
        assertNotNull(failure, "audit failure must reach the caller");
        assertEquals("injected audit failure", failure.getMessage());
        assertEquals(1, f.auditCalls.get(), "failure must occur after observing the real SQL update");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void alreadyBoundReplayDoesNotWriteAgain(boolean generated) {
        Fixture f = new Fixture(true, false, false);
        f.invoke(generated);
        assertEquals(93L, f.batch());
        assertEquals(1, f.version());
        assertEquals(0, f.bindingAttempts.get());
        assertEquals(0, f.auditCalls.get());
        assertEquals(0, f.auditRows());
        verifyNoInteractions(f.receipts, f.batches);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void failedCompareAndSetLeavesDatabaseAndAuditUnchanged(boolean generated) {
        Fixture f = new Fixture(false, true, false);
        assertThrows(IllegalStateException.class, () -> f.invoke(generated));
        assertNull(f.batch());
        assertEquals(2, f.version());
        assertEquals(1, f.bindingAttempts.get());
        assertEquals(0, f.auditCalls.get());
        assertEquals(0, f.auditRows());
    }

    private static final class Fixture {
        final JdbcTemplate jdbc;
        final MesTeamLeaderActiveOrderReleaseApplicationService service;
        final MesProcessPoolActiveOrderCompletionReceiptMapper receipts = mock(MesProcessPoolActiveOrderCompletionReceiptMapper.class);
        final MesProEdhrBatchExecutionMapper batches = mock(MesProEdhrBatchExecutionMapper.class);
        final cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionOriginMapper origins =
                mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionOriginMapper.class);
        final List<GxpAuditCommand> audits = new ArrayList<>();
        final AtomicInteger bindingAttempts = new AtomicInteger();
        final AtomicInteger auditCalls = new AtomicInteger();

        Fixture(boolean bound, boolean stale, boolean auditFails) {
            this(bound, stale, auditFails, false);
        }

        Fixture(boolean bound, boolean stale, boolean auditFails, boolean missingPolicy) {
            JdbcDataSource ds = new JdbcDataSource();
            ds.setURL("jdbc:h2:mem:m7_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
            jdbc = new JdbcTemplate(ds);
            jdbc.execute("CREATE TABLE application (id BIGINT PRIMARY KEY, batch_execution_id BIGINT, version INT, application_status VARCHAR(40))");
            jdbc.execute("CREATE TABLE audit_event (id INT PRIMARY KEY)");
            jdbc.update("INSERT INTO application VALUES (99, ?, ?, 'PQC_RELEASE_PENDING')", bound ? 93L : null, stale ? 2 : 1);
            var generation = mock(MesTeamLeaderActiveOrderReleaseGenerationService.class);
            when(generation.replayExisting(any(), any())).thenReturn(
                    new MesTeamLeaderActiveOrderReleaseApplicationResult().setApplicationId(99L)
                            .setActiveOrderId(10L).setBatchExecutionId(bound ? 93L : null).setVersion(1)
                            .setStatus("PQC_RELEASE_PENDING").setSourceSnapshotHash("formal-source-hash"));
            when(receipts.selectByActiveOrderIdForUpdate(10L)).thenReturn(
                    MesProcessPoolActiveOrderCompletionReceiptDO.builder().id(88L).activeOrderId(10L)
                            .leaderUserId(20L).workOrderId(90L).batchCode("B90").routeId(30L)
                            .receiptStatus("BACKFILL_SUCCEEDED").batchRecordStatus("SUCCESS")
                            .processInspectionStatus("SUCCESS").batchRecordId(91L).processInspectionId(92L)
                            .routeVersionId(31L).build());
            when(origins.selectListByTraceFilter(10L, 90L, null, "ACTIVE_ORDER_COMPLETION")).thenReturn(
                    List.of(new cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionOriginDO()
                            .setActiveOrderId(10L).setWorkOrderId(90L).setEntryType("ACTIVE_ORDER_COMPLETION")
                            .setCompletionBackfillReceiptId(88L).setBatchExecutionId(93L)));
            when(batches.selectById(93L)).thenReturn(
                    MesProEdhrBatchExecutionDO.builder().id(93L).status(0)
                            .workOrderId(90L).batchCode("B90").routeId(30L).routeVersionId(31L).build());
            var applications = mock(MesProcessPoolActiveOrderReleaseApplicationMapper.class);
            when(applications.bindP3BatchExecution(any(), any(), any())).thenAnswer(invocation -> {
                assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
                bindingAttempts.incrementAndGet();
                return jdbc.update("UPDATE application SET batch_execution_id=?, version=version+1 WHERE id=? AND version=? AND batch_execution_id IS NULL AND application_status='PQC_RELEASE_PENDING'",
                        invocation.getArgument(2), invocation.getArgument(0), invocation.getArgument(1));
            });
            var audit = mock(GxpAuditService.class);
            doAnswer(invocation -> {
                assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
                assertEquals(93L, batch(), "audit observes the binding in the same transaction");
                assertEquals(2, version());
                auditCalls.incrementAndGet();
                audits.add(invocation.getArgument(0));
                jdbc.update("INSERT INTO audit_event VALUES (1)");
                if (auditFails) throw new IllegalStateException("injected audit failure");
                return null;
            }).when(audit).append(any(GxpAuditCommand.class));
            var target = new MesTeamLeaderActiveOrderReleaseApplicationServiceImpl(generation,
                    mock(MesTeamLeaderActiveOrderCompletionService.class), receipts, batches, applications,
                    mock(MesTeamLeaderActiveOrderCompletionBatchExecutionService.class), origins);
            ReflectionTestUtils.setField(target, "gxpAuditService", audit);
            if (missingPolicy) {
                var realAudit = new cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditServiceImpl();
                var policies = mock(cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditPolicyOperationMapper.class);
                String policyVersion = "fixture-activated-without-bind";
                when(policies.selectByPolicyVersionForUpdate(1L, policyVersion, "mes.pqc-release.bind-batch"))
                        .thenAnswer(invocation -> {
                    assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
                    assertEquals(93L, batch());
                    assertEquals(2, version());
                    auditCalls.incrementAndGet();
                    return null;
                });
                var ledger = mock(cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditLedgerSequenceMapper.class);
                var watermark = new cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditLedgerSequenceDO();
                watermark.setTenantId(1L);
                watermark.setNextLedgerSequence(1L);
                when(ledger.selectByTenantIdForUpdate(1L)).thenReturn(watermark);
                var activations = mock(cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditPolicyActivationMapper.class);
                var activation = new cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditPolicyActivationDO();
                activation.setTenantId(1L);
                activation.setPolicyVersion(policyVersion);
                when(activations.selectLatestForUpdate(1L)).thenReturn(activation);
                ReflectionTestUtils.setField(realAudit, "policyOperationMapper", policies);
                ReflectionTestUtils.setField(realAudit, "ledgerSequenceMapper", ledger);
                ReflectionTestUtils.setField(realAudit, "policyActivationMapper", activations);
                ReflectionTestUtils.setField(target, "gxpAuditService", realAudit);
            }
            ProxyFactory proxy = new ProxyFactory(target);
            proxy.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(ds),
                    new AnnotationTransactionAttributeSource()));
            service = (MesTeamLeaderActiveOrderReleaseApplicationService) proxy.getProxy();
        }

        MesTeamLeaderActiveOrderReleaseApplicationResult invoke(boolean generated) {
            var command = new MesTeamLeaderActiveOrderReleaseApplyCommand().setActiveOrderId(10L).setIdempotencyKey("P3-10");
            return generated ? service.applyGenerated(20L, command) : service.apply(20L, command);
        }

        Long batch() { return jdbc.queryForObject("SELECT batch_execution_id FROM application WHERE id=99", Long.class); }
        int version() { return jdbc.queryForObject("SELECT version FROM application WHERE id=99", Integer.class); }
        int auditRows() { return jdbc.queryForObject("SELECT COUNT(*) FROM audit_event", Integer.class); }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void missingActivatedBindPolicyFailsClosedAndRollsBack(boolean generated) {
        Fixture f = new Fixture(false, false, false, true);
        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(1L);
        try {
            var failure = assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                    () -> f.invoke(generated));
            assertEquals(cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.GXP_AUDIT_POLICY_NOT_FOUND.getCode(),
                    failure.getCode());
            assertEquals(1, f.auditCalls.get(), "must query the new operation, not the old CREATE policy");
            assertNull(f.batch());
            assertEquals(1, f.version());
            assertEquals(0, f.auditRows());
        } finally {
            cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear();
        }
    }

    @org.junit.jupiter.api.Test
    void draftIsUnapprovedOperationFragmentAndNotARuntimeBundle() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper(new com.fasterxml.jackson.dataformat.yaml.YAMLFactory());
        String yaml;
        try (var input = getClass().getResourceAsStream("/gxp/gxp-audit-pqc-bind-policy.draft.yaml")) {
            assertNotNull(input, "test-owned PQC bind draft fixture must exist");
            yaml = new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
        var fragment = mapper.readTree(yaml);
        assertEquals("GXP_AUDIT_OPERATION_FRAGMENT", fragment.path("proposalType").asText());
        assertEquals("DRAFT", fragment.path("status").asText());
        assertTrue(fragment.path("approvalReference").isNull());
        assertFalse(fragment.path("runtimeLoad").asBoolean());
        assertEquals("mes.pqc-release.bind-batch", fragment.path("operation").path("operationId").asText());
        assertEquals("UPDATE", fragment.path("operation").path("actionType").asText());
        assertEquals("PRESENT_TO_PRESENT", fragment.path("operation").path("statePolicy").asText());
        assertTrue(fragment.path("operation").path("sourceLocator").asText().endsWith("#apply"));
        assertTrue(fragment.path("additionalSourceLocators").get(0).asText().endsWith("#applyGenerated"));
        var loader = new cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditPolicyBundleLoader();
        var packaged = loader.load();
        assertTrue(packaged.policyNode().path("operations").toString().contains("mes.pqc-release.bind-batch"));
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> ReflectionTestUtils.invokeMethod(loader, "load", yaml, packaged.rawSchema()));
    }
}
