package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.bpm.approval.service.ApprovalIdentity;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.MesProductionReleaseRoleCodes;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseManagerApprovalService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.stream.Stream;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesProEdhrReleasePreflightTest {
    private final MesProEdhrWorkTaskService tasks = mock(MesProEdhrWorkTaskService.class);
    private final MesProEdhrReleaseTransactionMapper transactions = mock(MesProEdhrReleaseTransactionMapper.class);
    private final MesProEdhrBatchExecutionMapper batches = mock(MesProEdhrBatchExecutionMapper.class);
    private final MesProductionReleaseManagerApprovalService manager = mock(MesProductionReleaseManagerApprovalService.class);
    private final MesEdhrBatchLifecycleGuard lifecycle = mock(MesEdhrBatchLifecycleGuard.class);
    private final MesProEdhrNonconformanceReviewService freeze = mock(MesProEdhrNonconformanceReviewService.class);
    private final MesProEdhrReleaseServiceImpl service = new MesProEdhrReleaseServiceImpl();
    private MockedStatic<SecurityFrameworkUtils> security;
    private MesProEdhrWorkTaskDO task;
    private MesProEdhrReleaseTransactionDO transaction;
    private MesProEdhrBatchExecutionDO batch;

    @BeforeEach
    void setup() {
        TenantContextHolder.setTenantId(1L);
        security = mockStatic(SecurityFrameworkUtils.class);
        security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(100L);
        ReflectionTestUtils.setField(service, "workTaskService", tasks);
        ReflectionTestUtils.setField(service, "releaseTransactionMapper", transactions);
        ReflectionTestUtils.setField(service, "batchExecutionMapper", batches);
        ReflectionTestUtils.setField(service, "managerApprovalService", manager);
        ReflectionTestUtils.setField(service, "lifecycleGuard", lifecycle);
        ReflectionTestUtils.setField(service, "nonconformanceReviewService", freeze);
        task = new MesProEdhrWorkTaskDO().setId(177L).setBusinessScopeId(9200L).setBatchExecutionId(901L).setWorkOrderId(301L)
                .setTaskType(MesProEdhrWorkTaskService.TASK_TYPE_RELEASE_APPROVE).setBusinessScopeType("RELEASE_TRANSACTION")
                .setStatus(MesProEdhrWorkTaskStatus.TODO).setAssigneeUserId(100L);
        transaction = new MesProEdhrReleaseTransactionDO().setId(9200L).setBatchExecutionId(901L)
                .setWorkOrderId(301L).setReleaseStatus("PENDING_APPROVAL");
        batch = new MesProEdhrBatchExecutionDO().setId(901L).setTenantId(1L).setWorkOrderId(301L);
        when(tasks.validateReleaseApprovalTask(177L, null)).thenReturn(task);
        when(transactions.selectById(9200L)).thenReturn(transaction);
        when(batches.selectById(901L)).thenReturn(batch);
    }
    @AfterEach void cleanup() { security.close(); TenantContextHolder.clear(); }

    @Test void validGraphUsesReadOnlyGuardsAndManagedQualification() {
        when(manager.isManagedReleaseTransaction(9200L)).thenReturn(true);
        service.preflightApprovalCenterReview(100L, 177L);
        verify(manager).preflightApprovalCenterReview(100L, task, transaction, batch);
        verify(lifecycle).requireReleaseAllowed(901L);
        verify(freeze).ensureBatchNotFrozen(901L, "上市放行");
        verify(transactions, never()).updateById(any(MesProEdhrReleaseTransactionDO.class));
        verify(batches, never()).updateById(any(MesProEdhrBatchExecutionDO.class));
        verify(tasks).validateReleaseApprovalTask(177L, null);
        verify(transactions).selectById(9200L);
        verify(batches).selectById(901L);
        verify(manager).isManagedReleaseTransaction(9200L);
        verifyNoMoreInteractions(tasks, transactions, batches, lifecycle, freeze, manager);
    }
    @Test void validGeneralReleaseUsesOnlyReadsAndGuards() {
        service.preflightApprovalCenterReview(100L, 177L);
        verify(tasks).validateReleaseApprovalTask(177L, null);
        verify(transactions).selectById(9200L);
        verify(batches).selectById(901L);
        verify(lifecycle).requireReleaseAllowed(901L);
        verify(freeze).ensureBatchNotFrozen(901L, "上市放行");
        verify(manager).isManagedReleaseTransaction(9200L);
        verifyNoMoreInteractions(tasks, transactions, batches, lifecycle, freeze, manager);
    }
    @Test void lifecycleRefusalStopsBeforeFreezeManagerOrWrites() {
        doThrow(new IllegalStateException("void pending")).when(lifecycle).requireReleaseAllowed(901L);
        assertThrows(IllegalStateException.class, () -> service.preflightApprovalCenterReview(100L, 177L));
        verify(tasks).validateReleaseApprovalTask(177L, null);
        verify(transactions).selectById(9200L);
        verify(batches).selectById(901L);
        verify(lifecycle).requireReleaseAllowed(901L);
        verifyNoMoreInteractions(tasks, transactions, batches, lifecycle);
        verifyNoInteractions(freeze, manager);
    }
    @Test void frozenBatchRejectsBeforeManagedQualificationOrWrites() {
        doThrow(new IllegalStateException("batch frozen")).when(freeze).ensureBatchNotFrozen(901L, "上市放行");
        assertThrows(IllegalStateException.class, () -> service.preflightApprovalCenterReview(100L, 177L));
        verifyNoInteractions(manager);
        verify(transactions, never()).updateById(any(MesProEdhrReleaseTransactionDO.class));
        verify(tasks).validateReleaseApprovalTask(177L, null);
        verifyNoMoreInteractions(tasks);
    }
    @Test void managedTaskCannotBecomeGeneralReleaseWhenItsApplicationIsMissing() {
        task.setResponsibilitySourceKey("MES_MANAGEMENT_REPRESENTATIVE");
        doThrow(new IllegalStateException("application missing")).when(manager)
                .preflightApprovalCenterReview(100L, task, transaction, batch);
        assertThrows(IllegalStateException.class, () -> service.preflightApprovalCenterReview(100L, 177L));
        verify(manager).preflightApprovalCenterReview(100L, task, transaction, batch);
        verify(transactions, never()).updateById(any(MesProEdhrReleaseTransactionDO.class));
        verify(tasks).validateReleaseApprovalTask(177L, null);
        verifyNoMoreInteractions(tasks);
    }
    @ParameterizedTest @ValueSource(strings = {"missingTask", "missingTransaction", "missingBatch", "batch", "tenant", "workOrder", "transactionOrder", "status"})
    void invalidGraphRejectsBeforeGuardsOrWrites(String failure) {
        switch (failure) {
            case "missingTask" -> when(tasks.validateReleaseApprovalTask(177L, null)).thenReturn(null);
            case "missingTransaction" -> when(transactions.selectById(9200L)).thenReturn(null);
            case "missingBatch" -> when(batches.selectById(901L)).thenReturn(null);
            case "batch" -> task.setBatchExecutionId(902L);
            case "tenant" -> batch.setTenantId(2L);
            case "workOrder" -> task.setWorkOrderId(302L);
            case "transactionOrder" -> transaction.setWorkOrderId(302L);
            case "status" -> transaction.setReleaseStatus("RELEASED");
        }
        assertThrows(RuntimeException.class, () -> service.preflightApprovalCenterReview(100L, 177L));
        verifyNoInteractions(manager, lifecycle, freeze);
        verify(tasks).validateReleaseApprovalTask(177L, null);
        if ("missingTask".equals(failure)) {
            verifyNoMoreInteractions(tasks);
            verifyNoInteractions(transactions, batches);
        } else {
            verify(transactions).selectById(9200L);
            if ("missingTransaction".equals(failure) || "batch".equals(failure)
                    || "workOrder".equals(failure) || "transactionOrder".equals(failure)) {
                verifyNoMoreInteractions(tasks, transactions);
                verifyNoInteractions(batches);
            } else {
                verify(batches).selectById(901L);
                verifyNoMoreInteractions(tasks, transactions, batches);
            }
        }
    }
    @ParameterizedTest @NullSource @ValueSource(longs = {0L, -1L})
    void nonPositiveWorkTaskInputRejectsBeforeDomainReads(Long workTaskId) {
        assertIdentityRejected(workTaskId);
        verifyNoInteractions(tasks, transactions, batches, manager, lifecycle, freeze);
    }

    @ParameterizedTest @MethodSource("nonPositiveTaskFields")
    void nonPositiveTaskIdentityStopsBeforeTransactionRead(String field, Long value) {
        switch (field) {
            case "id" -> task.setId(value);
            case "businessScopeId" -> task.setBusinessScopeId(value);
            case "batchExecutionId" -> task.setBatchExecutionId(value);
            case "workOrderId" -> task.setWorkOrderId(value);
            default -> fail("unexpected task identity field");
        }
        assertIdentityRejected(177L);
        verify(tasks).validateReleaseApprovalTask(177L, null);
        verifyNoMoreInteractions(tasks);
        verifyNoInteractions(transactions, batches, manager, lifecycle, freeze);
    }

    @ParameterizedTest @MethodSource("nonPositiveTransactionFields")
    void nonPositiveTransactionIdentityStopsBeforeBatchRead(String field, Long value) {
        switch (field) {
            case "id" -> transaction.setId(value);
            case "batchExecutionId" -> transaction.setBatchExecutionId(value);
            case "workOrderId" -> transaction.setWorkOrderId(value);
            default -> fail("unexpected transaction identity field");
        }
        assertIdentityRejected(177L);
        verify(tasks).validateReleaseApprovalTask(177L, null);
        verify(transactions).selectById(9200L);
        verifyNoMoreInteractions(tasks, transactions);
        verifyNoInteractions(batches, manager, lifecycle, freeze);
    }

    @ParameterizedTest @MethodSource("nonPositiveBatchFields")
    void nonPositiveBatchIdentityStopsBeforeGuards(String field, Long value) {
        switch (field) {
            case "id" -> batch.setId(value);
            case "workOrderId" -> batch.setWorkOrderId(value);
            default -> fail("unexpected batch identity field");
        }
        assertIdentityRejected(177L);
        verify(tasks).validateReleaseApprovalTask(177L, null);
        verify(transactions).selectById(9200L);
        verify(batches).selectById(901L);
        verifyNoMoreInteractions(tasks, transactions, batches);
        verifyNoInteractions(manager, lifecycle, freeze);
    }

    @ParameterizedTest @ValueSource(strings = {"task", "transaction", "batch"})
    void wrongReturnedEntityIdStopsAtItsReadNode(String entity) {
        switch (entity) {
            case "task" -> task.setId(178L);
            case "transaction" -> transaction.setId(9201L);
            case "batch" -> batch.setId(902L);
            default -> fail("unexpected entity");
        }
        assertIdentityRejected(177L);
        verify(tasks).validateReleaseApprovalTask(177L, null);
        if ("task".equals(entity)) {
            verifyNoMoreInteractions(tasks);
            verifyNoInteractions(transactions, batches);
        } else {
            verify(transactions).selectById(9200L);
            if ("transaction".equals(entity)) {
                verifyNoMoreInteractions(tasks, transactions);
                verifyNoInteractions(batches);
            } else {
                verify(batches).selectById(901L);
                verifyNoMoreInteractions(tasks, transactions, batches);
            }
        }
        verifyNoInteractions(manager, lifecycle, freeze);
    }

    @ParameterizedTest @ValueSource(strings = {"batchExecutionId", "workOrderId"})
    void transactionRelationMismatchStopsBeforeBatchRead(String field) {
        if ("batchExecutionId".equals(field)) {
            transaction.setBatchExecutionId(902L);
        } else {
            transaction.setWorkOrderId(302L);
        }
        assertIdentityRejected(177L);
        verify(tasks).validateReleaseApprovalTask(177L, null);
        verify(transactions).selectById(9200L);
        verifyNoMoreInteractions(tasks, transactions);
        verifyNoInteractions(batches, manager, lifecycle, freeze);
    }

    @ParameterizedTest @NullSource @ValueSource(longs = {0L, -1L})
    void equalNonPositiveWorkOrderGraphRejectsBeforeTransactionRead(Long workOrderId) {
        task.setWorkOrderId(workOrderId);
        transaction.setWorkOrderId(workOrderId);
        batch.setWorkOrderId(workOrderId);
        assertIdentityRejected(177L);
        verify(tasks).validateReleaseApprovalTask(177L, null);
        verifyNoMoreInteractions(tasks);
        verifyNoInteractions(transactions, batches, manager, lifecycle, freeze);
    }

    @Test void batchWorkOrderMismatchStopsBeforeGuards() {
        batch.setWorkOrderId(302L);
        assertIdentityRejected(177L);
        verify(tasks).validateReleaseApprovalTask(177L, null);
        verify(transactions).selectById(9200L);
        verify(batches).selectById(901L);
        verifyNoMoreInteractions(tasks, transactions, batches);
        verifyNoInteractions(manager, lifecycle, freeze);
    }

    private void assertIdentityRejected(Long workTaskId) {
        ServiceException error = assertThrows(ServiceException.class,
                () -> service.preflightApprovalCenterReview(100L, workTaskId));
        assertEquals(PRO_EDHR_BATCH_EXECUTION_NOT_EXISTS.getCode(), error.getCode());
    }

    private static Stream<Arguments> nonPositiveTaskFields() {
        return nonPositiveFields("id", "businessScopeId", "batchExecutionId", "workOrderId");
    }

    private static Stream<Arguments> nonPositiveTransactionFields() {
        return nonPositiveFields("id", "batchExecutionId", "workOrderId");
    }

    private static Stream<Arguments> nonPositiveBatchFields() {
        return nonPositiveFields("id", "workOrderId");
    }

    private static Stream<Arguments> nonPositiveFields(String... fields) {
        return Stream.of(fields).flatMap(field -> Stream.of((Long) null, 0L, -1L)
                .map(value -> Arguments.of(field, value)));
    }

    @Test void missingIgnoredTenantOrWrongActorFailsBeforeSourceReads() {
        TenantContextHolder.clear();
        assertThrows(RuntimeException.class, () -> service.preflightApprovalCenterReview(100L, 177L));
        TenantContextHolder.setTenantId(1L); TenantContextHolder.setIgnore(true);
        assertThrows(RuntimeException.class, () -> service.preflightApprovalCenterReview(100L, 177L));
        TenantContextHolder.setIgnore(false);
        assertThrows(RuntimeException.class, () -> service.preflightApprovalCenterReview(101L, 177L));
        verify(tasks, never()).validateReleaseApprovalTask(any(), any());
    }

    @ParameterizedTest(name = "{index}:{0}") @ValueSource(strings = {"missingTask", "missingTransaction", "missingBatch", "batch", "tenant", "workOrder", "transactionOrder", "status"})
    void resolveInvalidGraphRejectsBeforeGuardsOrWrites(String failure) {
        switch (failure) {
            case "missingTask" -> when(tasks.validateReleaseApprovalTask(177L, null)).thenReturn(null);
            case "missingTransaction" -> when(transactions.selectById(9200L)).thenReturn(null);
            case "missingBatch" -> when(batches.selectById(901L)).thenReturn(null);
            case "batch" -> task.setBatchExecutionId(902L);
            case "tenant" -> batch.setTenantId(2L);
            case "workOrder" -> task.setWorkOrderId(302L);
            case "transactionOrder" -> transaction.setWorkOrderId(302L);
            case "status" -> transaction.setReleaseStatus("RELEASED");
        }
        ServiceException error = assertThrows(ServiceException.class, () -> service.resolveApprovalContentIdentity(100L, 177L));
        assertEquals(("status".equals(failure)
                ? MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_RELEASE_STATUS_INVALID
                : PRO_EDHR_BATCH_EXECUTION_NOT_EXISTS).getCode(), error.getCode());
        verifyNoInteractions(manager, lifecycle, freeze);
        verify(tasks).validateReleaseApprovalTask(177L, null);
        if ("missingTask".equals(failure)) {
            verifyNoMoreInteractions(tasks);
            verifyNoInteractions(transactions, batches);
        } else {
            verify(transactions).selectById(9200L);
            if ("missingTransaction".equals(failure) || "batch".equals(failure)
                    || "workOrder".equals(failure) || "transactionOrder".equals(failure)) {
                verifyNoMoreInteractions(tasks, transactions);
                verifyNoInteractions(batches);
            } else {
                verify(batches).selectById(901L);
                verifyNoMoreInteractions(tasks, transactions, batches);
            }
        }
    }

    @ParameterizedTest(name = "{index}:{0}") @NullSource @ValueSource(longs = {0L, -1L})
    void resolveNonPositiveWorkTaskInputRejectsBeforeDomainReads(Long workTaskId) {
        assertResolvedIdentityRejected(workTaskId);
        verifyNoInteractions(tasks, transactions, batches, manager, lifecycle, freeze);
    }

    @ParameterizedTest(name = "{index}:{0}") @MethodSource("nonPositiveTaskFields")
    void resolveNonPositiveTaskIdentityStopsBeforeTransactionRead(String field, Long value) {
        switch (field) {
            case "id" -> task.setId(value);
            case "businessScopeId" -> task.setBusinessScopeId(value);
            case "batchExecutionId" -> task.setBatchExecutionId(value);
            case "workOrderId" -> task.setWorkOrderId(value);
            default -> fail("unexpected task identity field");
        }
        assertResolvedIdentityRejected(177L);
        verify(tasks).validateReleaseApprovalTask(177L, null);
        verifyNoMoreInteractions(tasks);
        verifyNoInteractions(transactions, batches, manager, lifecycle, freeze);
    }

    @ParameterizedTest(name = "{index}:{0}") @MethodSource("nonPositiveTransactionFields")
    void resolveNonPositiveTransactionIdentityStopsBeforeBatchRead(String field, Long value) {
        switch (field) {
            case "id" -> transaction.setId(value);
            case "batchExecutionId" -> transaction.setBatchExecutionId(value);
            case "workOrderId" -> transaction.setWorkOrderId(value);
            default -> fail("unexpected transaction identity field");
        }
        assertResolvedIdentityRejected(177L);
        verify(tasks).validateReleaseApprovalTask(177L, null);
        verify(transactions).selectById(9200L);
        verifyNoMoreInteractions(tasks, transactions);
        verifyNoInteractions(batches, manager, lifecycle, freeze);
    }

    @ParameterizedTest(name = "{index}:{0}") @MethodSource("nonPositiveBatchFields")
    void resolveNonPositiveBatchIdentityStopsBeforeGuards(String field, Long value) {
        switch (field) {
            case "id" -> batch.setId(value);
            case "workOrderId" -> batch.setWorkOrderId(value);
            default -> fail("unexpected batch identity field");
        }
        assertResolvedIdentityRejected(177L);
        verify(tasks).validateReleaseApprovalTask(177L, null);
        verify(transactions).selectById(9200L);
        verify(batches).selectById(901L);
        verifyNoMoreInteractions(tasks, transactions, batches);
        verifyNoInteractions(manager, lifecycle, freeze);
    }

    @ParameterizedTest(name = "{index}:{0}") @ValueSource(strings = {"task", "transaction", "batch"})
    void resolveWrongReturnedEntityIdStopsAtItsReadNode(String entity) {
        switch (entity) {
            case "task" -> task.setId(178L);
            case "transaction" -> transaction.setId(9201L);
            case "batch" -> batch.setId(902L);
            default -> fail("unexpected entity");
        }
        assertResolvedIdentityRejected(177L);
        verify(tasks).validateReleaseApprovalTask(177L, null);
        if ("task".equals(entity)) {
            verifyNoMoreInteractions(tasks);
            verifyNoInteractions(transactions, batches);
        } else {
            verify(transactions).selectById(9200L);
            if ("transaction".equals(entity)) {
                verifyNoMoreInteractions(tasks, transactions);
                verifyNoInteractions(batches);
            } else {
                verify(batches).selectById(901L);
                verifyNoMoreInteractions(tasks, transactions, batches);
            }
        }
        verifyNoInteractions(manager, lifecycle, freeze);
    }

    @ParameterizedTest(name = "{index}:{0}") @ValueSource(strings = {"batchExecutionId", "workOrderId"})
    void resolveTransactionRelationMismatchStopsBeforeBatchRead(String field) {
        if ("batchExecutionId".equals(field)) {
            transaction.setBatchExecutionId(902L);
        } else {
            transaction.setWorkOrderId(302L);
        }
        assertResolvedIdentityRejected(177L);
        verify(tasks).validateReleaseApprovalTask(177L, null);
        verify(transactions).selectById(9200L);
        verifyNoMoreInteractions(tasks, transactions);
        verifyNoInteractions(batches, manager, lifecycle, freeze);
    }

    @ParameterizedTest(name = "{index}:{0}") @NullSource @ValueSource(longs = {0L, -1L})
    void resolveEqualNonPositiveWorkOrderGraphRejectsBeforeTransactionRead(Long workOrderId) {
        task.setWorkOrderId(workOrderId);
        transaction.setWorkOrderId(workOrderId);
        batch.setWorkOrderId(workOrderId);
        assertResolvedIdentityRejected(177L);
        verify(tasks).validateReleaseApprovalTask(177L, null);
        verifyNoMoreInteractions(tasks);
        verifyNoInteractions(transactions, batches, manager, lifecycle, freeze);
    }

    @Test void resolveBatchWorkOrderMismatchStopsBeforeGuards() {
        batch.setWorkOrderId(302L);
        assertResolvedIdentityRejected(177L);
        verify(tasks).validateReleaseApprovalTask(177L, null);
        verify(transactions).selectById(9200L);
        verify(batches).selectById(901L);
        verifyNoMoreInteractions(tasks, transactions, batches);
        verifyNoInteractions(manager, lifecycle, freeze);
    }

    @ParameterizedTest(name = "{index}:{0}")
    @ValueSource(strings = {"general", "managedDetector", "managementRole"})
    void resolveCheckedGraphReturnsSevenScalarsOnce(String branch) {
        if ("managedDetector".equals(branch)) {
            // The validated port graph proves the detector call boundary, not real managed binding.
            when(manager.isManagedReleaseTransaction(9200L)).thenReturn(true);
        } else if ("managementRole".equals(branch)) {
            managedRoleFixture();
        }
        assertResolvedIdentity(service.resolveApprovalContentIdentity(100L, 177L));
        verifySuccessfulReadOrder(!"managementRole".equals(branch), !"general".equals(branch));
    }

    @ParameterizedTest(name = "{index}:{0}")
    @ValueSource(strings = {"missingTenant", "ignoredTenant", "wrongActor"})
    void resolveContextFailureStopsBeforeSourceReads(String failure) {
        Long actor = 100L;
        if ("missingTenant".equals(failure)) TenantContextHolder.clear();
        else if ("ignoredTenant".equals(failure)) TenantContextHolder.setIgnore(true);
        else actor = 101L;
        Long requestedActor = actor;
        if ("missingTenant".equals(failure)) {
            assertThrows(NullPointerException.class, () -> service.resolveApprovalContentIdentity(requestedActor, 177L));
        } else if ("ignoredTenant".equals(failure)) {
            assertEquals("APPROVAL_REVIEW_TENANT_REQUIRED: EDHR", assertThrows(IllegalStateException.class,
                    () -> service.resolveApprovalContentIdentity(requestedActor, 177L)).getMessage());
        } else {
            ServiceException error = assertThrows(ServiceException.class,
                    () -> service.resolveApprovalContentIdentity(requestedActor, 177L));
            assertEquals(cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.UNAUTHORIZED.getCode(), error.getCode());
        }
        security.verify(SecurityFrameworkUtils::getLoginUserId,
                times("wrongActor".equals(failure) ? 1 : 0));
        verifyNoInteractions(tasks, transactions, batches, lifecycle, freeze, manager);
    }

    @ParameterizedTest(name = "{index}:{0}")
    @ValueSource(strings = {"lifecycle", "freeze", "managedQualification"})
    void resolveQualificationFailurePropagatesWithoutIdentityOrWrites(String node) {
        IllegalStateException failure = new IllegalStateException("refused at " + node);
        if ("lifecycle".equals(node)) {
            doThrow(failure).when(lifecycle).requireReleaseAllowed(901L);
        } else if ("freeze".equals(node)) {
            doThrow(failure).when(freeze).ensureBatchNotFrozen(901L, "上市放行");
        } else {
            managedRoleFixture();
            doThrow(failure).when(manager).preflightApprovalCenterReview(100L, task, transaction, batch);
        }
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> service.resolveApprovalContentIdentity(100L, 177L)));
        org.mockito.InOrder order = inOrder(tasks, transactions, batches, lifecycle, freeze, manager);
        order.verify(tasks).validateReleaseApprovalTask(177L, null);
        order.verify(transactions).selectById(9200L);
        order.verify(batches).selectById(901L);
        order.verify(lifecycle).requireReleaseAllowed(901L);
        if (!"lifecycle".equals(node)) order.verify(freeze).ensureBatchNotFrozen(901L, "上市放行");
        if ("managedQualification".equals(node)) {
            order.verify(manager).preflightApprovalCenterReview(100L, task, transaction, batch);
        }
        verifyNoMoreInteractions(tasks, transactions, batches, lifecycle, freeze, manager);
    }

    @ParameterizedTest(name = "{index}:tenant={0}") @ValueSource(longs = {0L, -1L})
    void resolveNonPositiveTenantReturnDoesNotChangeLegacyPreflight(Long tenantId) {
        TenantContextHolder.setTenantId(tenantId); batch.setTenantId(tenantId);
        service.preflightApprovalCenterReview(100L, 177L);
        verifySuccessfulReadOrder(true, false);
        clearInvocations(tasks, transactions, batches, lifecycle, freeze, manager);
        assertResolvedIdentityRejected(177L);
        // The new return check follows the same graph reads and guards; it is not an entry gate.
        verifySuccessfulReadOrder(true, false);
    }

    @Test void resolvedIdentityDoesNotFollowLaterMutableGraphChanges() {
        ApprovalIdentity identity = service.resolveApprovalContentIdentity(100L, 177L);
        task.setId(178L).setBusinessScopeType("OTHER").setBusinessScopeId(9201L)
                .setBatchExecutionId(902L).setWorkOrderId(302L);
        transaction.setId(9201L).setBatchExecutionId(902L).setWorkOrderId(302L);
        batch.setId(902L).setTenantId(2L).setWorkOrderId(302L);
        assertResolvedIdentity(identity);
        verifySuccessfulReadOrder(true, false);
    }

    private void managedRoleFixture() {
        task.setCandidateSourceType("ROLE_GROUP")
                .setResponsibilitySourceKey(MesProductionReleaseRoleCodes.MANAGEMENT_REPRESENTATIVE)
                .setCandidateUserSnapshot("[100]");
    }

    private void assertResolvedIdentity(ApprovalIdentity identity) {
        assertEquals(1L, identity.tenantId());
        assertEquals("EDHR", identity.ownerCode());
        assertEquals("EDHR_WORK_TASK", identity.sourceTaskType());
        assertEquals("177", identity.sourceTaskId());
        assertNull(identity.processInstanceId());
        assertEquals("RELEASE_TRANSACTION", identity.domainSubjectType());
        assertEquals("9200", identity.domainSubjectId());
        assertEquals(7, ApprovalIdentity.class.getRecordComponents().length);
    }

    private void verifySuccessfulReadOrder(boolean detectManaged, boolean qualifyManaged) {
        org.mockito.InOrder order = inOrder(tasks, transactions, batches, lifecycle, freeze, manager);
        order.verify(tasks).validateReleaseApprovalTask(177L, null);
        order.verify(transactions).selectById(9200L);
        order.verify(batches).selectById(901L);
        order.verify(lifecycle).requireReleaseAllowed(901L);
        order.verify(freeze).ensureBatchNotFrozen(901L, "上市放行");
        if (detectManaged) order.verify(manager).isManagedReleaseTransaction(9200L);
        if (qualifyManaged) order.verify(manager).preflightApprovalCenterReview(100L, task, transaction, batch);
        verifyNoMoreInteractions(tasks, transactions, batches, lifecycle, freeze, manager);
    }

    private void assertResolvedIdentityRejected(Long workTaskId) {
        ServiceException error = assertThrows(ServiceException.class,
                () -> service.resolveApprovalContentIdentity(100L, workTaskId));
        assertEquals(PRO_EDHR_BATCH_EXECUTION_NOT_EXISTS.getCode(), error.getCode());
    }
}
