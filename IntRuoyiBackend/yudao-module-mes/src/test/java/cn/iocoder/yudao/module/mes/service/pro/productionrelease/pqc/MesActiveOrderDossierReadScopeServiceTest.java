package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionOriginMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionOriginDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDossierFileDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionVisibilityService;
import org.springframework.test.util.ReflectionTestUtils;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.stubbing.Answer;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * HISTORY-DOSSIER01: actual scope + actual batch visibility + real security context; persistence boundary doubles.
 * Reflection only permits the pre-implementation RED to compile. It never substitutes service behavior.
 * Frozen manager candidate decisions use the production visibility implementation; this is not E2E.
 */
class MesActiveOrderDossierReadScopeServiceTest {
    private static final String TYPE = "cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesActiveOrderDossierReadScopeService";
    private static final String BATCH_PERMISSION = "mes:pro-edhr-batch-execution:query";
    private static final String PQC_PERMISSION = "mes:pro-production-release:query";
    private static final Long TENANT = 1L, ACTOR = 347L, LEADER = 341L, PQC_ACTOR = 346L;
    private static final Long BATCH = 1225L, ACTIVE = 1009L, APPLICATION = 224L, WORK_ORDER = 990274L, TASK = 2734L;

    private Fixture f;

    @BeforeEach
    void setup() {
        f = new Fixture();
        f.login(ACTOR, TENANT);
        f.target = requireService(f);
    }

    @AfterEach
    void cleanup() {
        if (f != null) {
            for (Object dependency : List.of(f.apps, f.orders, f.batches, f.pqcReader, f.tasks, f.permissions, f.batchTasks, f.transactions, f.origins)) {
                assertTrue(mockingDetails(dependency).getInvocations().stream().noneMatch(invocation -> {
                    String name = invocation.getMethod().getName();
                    return name.startsWith("insert") || name.startsWith("update") || name.startsWith("delete")
                            || name.startsWith("approve") || name.startsWith("reject");
                }), "dossier authorization may not mutate its dependencies");
            }
        }
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
    }

    @ParameterizedTest
    @CsvSource({"BATCH,MANAGER_RELEASE_PENDING", "BATCH,RELEASED", "ACTIVE,MANAGER_RELEASE_PENDING", "ACTIVE,RELEASED"})
    void managerReadsExactFormalScopeAfterTaskDoneOrRelease(String scope, String state) {
        f.app.setApplicationStatus(state);
        f.order.setBusinessStatus(state);
        f.batch.setStatus("RELEASED".equals(state) ? 40 : 30);
        f.task.setStatus("DONE");
        Object result = f.batch(ACTOR, "BATCH".equals(scope) ? BATCH : null, "ACTIVE".equals(scope) ? ACTIVE : null);
        assertContext(result, f.order, f.app);
        verify(f.batches).selectById(BATCH);
        verify(f.visibility).requireVisibleBatch(f.batch, ACTOR);
        verifyNoInteractions(f.pqcReader);
        assertTrue(f.lookups.contains("BATCH".equals(scope) ? "selectByBatchExecutionId:1225" : "selectLatestByActiveOrderId:1009"));
        assertFalse(f.lookups.contains("BATCH".equals(scope) ? "selectLatestByActiveOrderId:1009" : "selectByBatchExecutionId:1225"),
                "the supplied scope chooses one exact lookup, not a second guessed route");
    }

    @ParameterizedTest
    @CsvSource(value = {"null,null", "1225,1009", "0,null", "-1,null", "null,0", "null,-1", "1225,0", "0,1009"}, nullValues = "null")
    void missingDualOrNonpositiveScopeIsRejected(Long batch, Long active) {
        assertThrows(RuntimeException.class, () -> f.batch(ACTOR, batch, active));
        verifyNoInteractions(f.batches, f.pqcReader);
    }

    @ParameterizedTest
    @ValueSource(strings = {"BATCH", "ACTIVE"})
    void queryPermissionIsRequiredEvenForKnownApplication(String scope) {
        f.allowBatch = false;
        f.allowPqc = true;
        assertThrows(RuntimeException.class, () -> f.batch(ACTOR, "BATCH".equals(scope) ? BATCH : null,
                "ACTIVE".equals(scope) ? ACTIVE : null));
        verifyNoInteractions(f.batches, f.pqcReader);
    }

    @ParameterizedTest
    @ValueSource(strings = {"NO_LOGIN", "WRONG_LOGIN", "NULL_ACTOR", "ZERO_ACTOR", "NULL_TENANT", "ZERO_TENANT", "LOGIN_TENANT_MISMATCH"})
    void batchRequiresAuthenticatedActorAndPositiveMatchingTenant(String fault) {
        Long supplied = ACTOR;
        switch (fault) {
            case "NO_LOGIN" -> SecurityContextHolder.clearContext();
            case "WRONG_LOGIN" -> f.login(348L, TENANT);
            case "NULL_ACTOR" -> supplied = null;
            case "ZERO_ACTOR" -> supplied = 0L;
            case "NULL_TENANT" -> TenantContextHolder.clear();
            case "ZERO_TENANT" -> TenantContextHolder.setTenantId(0L);
            case "LOGIN_TENANT_MISMATCH" -> { f.login(ACTOR, 2L); TenantContextHolder.setTenantId(TENANT); }
            default -> throw new AssertionError(fault);
        }
        Long requestedActor = supplied;
        assertThrows(RuntimeException.class, () -> f.batch(requestedActor, BATCH, null));
        verifyNoInteractions(f.batches, f.pqcReader);
    }

    @ParameterizedTest
    @ValueSource(strings = {"NO_BATCH", "NO_APPLICATION", "NO_ACTIVE", "BATCH_ID", "BATCH_ACTIVE", "BATCH_WORK_ORDER", "BATCH_TENANT", "BATCH_NULL_TENANT",
            "APP_ID", "APP_ACTIVE", "APP_BATCH", "APP_WORK_ORDER", "APP_TENANT", "APP_NULL_TENANT",
            "ACTIVE_ID", "ACTIVE_WORK_ORDER", "ACTIVE_TENANT", "ACTIVE_NULL_TENANT"})
    void exactBatchRejectsMissingOrCrossSourceFacts(String fault) {
        f.corrupt(fault);
        assertThrows(RuntimeException.class, () -> f.batch(ACTOR, BATCH, null));
        verifyNoInteractions(f.pqcReader);
        assertFalse(f.lookups.stream().anyMatch(value -> value.startsWith("selectLatestByActiveOrderId:")),
                "an explicit failed batch must not switch to latest application");
    }

    @ParameterizedTest
    @ValueSource(strings = {"NO_APPLICATION", "NO_ACTIVE", "APP_ACTIVE", "APP_BATCH_NULL", "APP_BATCH_ZERO", "BATCH_ACTIVE", "BATCH_WORK_ORDER", "ACTIVE_TENANT"})
    void activeOnlyStillRequiresItsExactFormalApplicationAndBatch(String fault) {
        f.corrupt(fault);
        assertThrows(RuntimeException.class, () -> f.batch(ACTOR, null, ACTIVE));
        verifyNoInteractions(f.pqcReader);
    }

    @ParameterizedTest
    @ValueSource(strings = {"BATCH", "ACTIVE"})
    void formalBatchFailurePropagatesWithoutPqcOrLeaderFallback(String scope) {
        ServiceException original = new ServiceException(1040750457, "formal frozen batch visibility denied");
        doThrow(original).when(f.visibility).requireVisibleBatch(f.batch, ACTOR);
        assertSame(original, assertThrows(RuntimeException.class, () -> f.batch(ACTOR,
                "BATCH".equals(scope) ? BATCH : null, "ACTIVE".equals(scope) ? ACTIVE : null)));
        verifyNoInteractions(f.pqcReader);
    }

    @Test
    void originalLeaderCanPreviewOwnedFileBeforeAnyApplication() {
        f.login(LEADER, TENANT);
        f.row.setApplicationId(null);
        f.missingApplication = true;
        f.allowBatch = false;
        f.allowPqc = false;
        assertDoesNotThrow(() -> f.preview(LEADER, f.row));
        verifyNoInteractions(f.batches, f.pqcReader);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MANAGER_RELEASE_PENDING", "RELEASED"})
    void originalFrozenPqcCandidateCanPreviewDoneTask(String status) {
        f.login(PQC_ACTOR, TENANT);
        f.allowBatch = false;
        f.allowPqc = true;
        f.app.setApplicationStatus(status);
        f.task.setStatus("DONE");
        f.decision.setStatus(status);
        assertDoesNotThrow(() -> f.preview(PQC_ACTOR, f.row));
        verify(f.pqcReader).get(PQC_ACTOR, APPLICATION);
        verifyNoInteractions(f.batches);
        assertEquals("DONE", f.task.getStatus());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void managerPreviewUsesExplicitOrLatestFormalBatch(boolean explicitApplication) {
        if (!explicitApplication) { f.row.setApplicationId(null); }
        assertDoesNotThrow(() -> f.preview(ACTOR, f.row));
        verify(f.batches).selectById(BATCH);
        verify(f.visibility).requireVisibleBatch(f.batch, ACTOR);
        verifyNoInteractions(f.pqcReader);
        assertTrue(f.lookups.contains(explicitApplication ? "selectById:224" : "selectLatestByActiveOrderId:1009"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"NO_PERMISSION", "PQC_NONCANDIDATE", "PQC_SUBSTRING", "PQC_WRONG_TYPE", "PQC_WRONG_SCOPE",
            "PQC_WRONG_APPLICATION", "PQC_WRONG_TASK", "PQC_NOT_LOCKED", "PQC_NO_TASK"})
    void knownApplicationDoesNotAuthorizeNonownerPreview(String fault) {
        f.login(PQC_ACTOR, TENANT);
        f.allowBatch = false;
        f.allowPqc = true;
        switch (fault) {
            case "NO_PERMISSION" -> f.allowPqc = false;
            case "PQC_NONCANDIDATE" -> f.task.setCandidateUserSnapshot("347");
            case "PQC_SUBSTRING" -> f.task.setCandidateUserSnapshot("1346,3460");
            case "PQC_WRONG_TYPE" -> f.task.setTaskType("RELEASE_APPROVE");
            case "PQC_WRONG_SCOPE" -> f.task.setBusinessScopeType("RELEASE_TRANSACTION");
            case "PQC_WRONG_APPLICATION" -> f.task.setBusinessScopeId(225L);
            case "PQC_WRONG_TASK" -> f.task.setId(2735L);
            case "PQC_NOT_LOCKED" -> f.task.setOwnershipLocked(false);
            case "PQC_NO_TASK" -> f.missingTask = true;
            default -> throw new AssertionError(fault);
        }
        assertThrows(RuntimeException.class, () -> f.preview(PQC_ACTOR, f.row));
        verifyNoInteractions(f.batches, f.pqcReader);
    }

    @Test
    void nonPqcCandidateMayUseSeparatelyAuthorizedExactBatch() {
        f.allowPqc = true;
        f.task.setCandidateUserSnapshot("346");
        assertDoesNotThrow(() -> f.preview(ACTOR, f.row));
        verify(f.batches).selectById(BATCH);
        verify(f.visibility).requireVisibleBatch(f.batch, ACTOR);
        verifyNoInteractions(f.pqcReader);
    }

    @Test
    void pqcReaderFailureIsNotRetriedUsingAvailableBatchPermission() {
        f.login(PQC_ACTOR, TENANT);
        f.allowPqc = true;
        f.allowBatch = true;
        ServiceException original = new ServiceException(1040760409, "formal PQC frozen scope rejected");
        when(f.pqcReader.get(PQC_ACTOR, APPLICATION)).thenThrow(original);
        assertSame(original, assertThrows(RuntimeException.class, () -> f.preview(PQC_ACTOR, f.row)));
        verifyNoInteractions(f.batches);
    }

    @Test
    void managerPreviewPreservesFormalBatchFailure() {
        ServiceException original = new ServiceException(1040750457, "formal manager candidate denied");
        doThrow(original).when(f.visibility).requireVisibleBatch(f.batch, ACTOR);
        assertSame(original, assertThrows(RuntimeException.class, () -> f.preview(ACTOR, f.row)));
        verifyNoInteractions(f.pqcReader);
    }

    @ParameterizedTest
    @ValueSource(strings = {"NULL_RESULT", "APP_ID", "TASK_ID", "BATCH_ID"})
    void pqcFormalDecisionMustMatchTheFrozenFileApplication(String fault) {
        f.login(PQC_ACTOR, TENANT);
        f.allowPqc = true;
        f.allowBatch = false;
        switch (fault) {
            case "NULL_RESULT" -> when(f.pqcReader.get(PQC_ACTOR, APPLICATION)).thenReturn(null);
            case "APP_ID" -> f.decision.setApplicationId(225L);
            case "TASK_ID" -> f.decision.setPqcReleaseWorkTaskId(2735L);
            case "BATCH_ID" -> f.decision.setBatchExecutionId(1226L);
            default -> throw new AssertionError(fault);
        }
        assertThrows(RuntimeException.class, () -> f.preview(PQC_ACTOR, f.row));
        verifyNoInteractions(f.batches);
    }

    @ParameterizedTest
    @ValueSource(strings = {"NULL_ROW", "ROW_ID", "FILE_ID", "ROW_ACTIVE", "ROW_TENANT", "ROW_NULL_TENANT",
            "ROW_APP_MISSING", "ROW_APP_ZERO", "APP_ACTIVE", "APP_WORK_ORDER", "APP_TENANT", "ACTIVE_TENANT",
            "BATCH_ACTIVE", "BATCH_WORK_ORDER", "APP_BATCH"})
    void previewRejectsInvalidFileOrCrossSourceFacts(String fault) {
        MesProcessPoolActiveOrderDossierFileDO supplied = f.row;
        switch (fault) {
            case "NULL_ROW" -> supplied = null;
            case "ROW_ID" -> f.row.setId(0L);
            case "FILE_ID" -> f.row.setFileId(0L);
            case "ROW_ACTIVE" -> f.row.setActiveOrderId(1010L);
            case "ROW_TENANT" -> f.row.setTenantId(2L);
            case "ROW_NULL_TENANT" -> f.row.setTenantId(null);
            case "ROW_APP_MISSING" -> f.row.setApplicationId(225L);
            case "ROW_APP_ZERO" -> f.row.setApplicationId(0L);
            default -> f.corrupt(fault);
        }
        MesProcessPoolActiveOrderDossierFileDO requested = supplied;
        assertThrows(RuntimeException.class, () -> f.preview(ACTOR, requested));
        assertFalse(f.lookups.stream().anyMatch(value -> value.startsWith("selectLatestByActiveOrderId:")),
                "an explicit file application must not be replaced by latest application");
    }

    @ParameterizedTest
    @ValueSource(strings = {"NO_LOGIN", "WRONG_LOGIN", "NULL_ACTOR", "ZERO_ACTOR", "NULL_TENANT", "ZERO_TENANT", "LOGIN_TENANT_MISMATCH"})
    void previewRequiresAuthenticatedActorAndMatchingTenantEvenForLeader(String fault) {
        f.login(LEADER, TENANT);
        Long supplied = LEADER;
        switch (fault) {
            case "NO_LOGIN" -> SecurityContextHolder.clearContext();
            case "WRONG_LOGIN" -> f.login(348L, TENANT);
            case "NULL_ACTOR" -> supplied = null;
            case "ZERO_ACTOR" -> supplied = 0L;
            case "NULL_TENANT" -> TenantContextHolder.clear();
            case "ZERO_TENANT" -> TenantContextHolder.setTenantId(0L);
            case "LOGIN_TENANT_MISMATCH" -> { f.login(LEADER, 2L); TenantContextHolder.setTenantId(TENANT); }
            default -> throw new AssertionError(fault);
        }
        Long requestedActor = supplied;
        assertThrows(RuntimeException.class, () -> f.preview(requestedActor, f.row));
        verifyNoInteractions(f.batches, f.pqcReader);
    }

    @ParameterizedTest
    @ValueSource(strings = {"NONCANDIDATE", "SUBSTRING", "WRONG_TRANSACTION", "WRONG_TASK_BATCH",
            "WRONG_TRANSACTION_BATCH", "NO_TRANSACTION", "NO_TASK", "WRONG_TASK_TYPE", "WRONG_SCOPE", "UNLOCKED",
            "CANCELLED", "UNKNOWN_STATUS"})
    void actualVisibilityRejectsForeignOrObsoleteCandidateWithoutRouteRecovery(String fault) {
        switch (fault) {
            case "NONCANDIDATE" -> f.managerTask.setCandidateUserSnapshot("348");
            case "SUBSTRING" -> f.managerTask.setCandidateUserSnapshot("1347,3470");
            case "WRONG_TRANSACTION" -> f.managerTask.setBusinessScopeId(225L);
            case "WRONG_TASK_BATCH" -> f.managerTask.setBatchExecutionId(1226L);
            case "WRONG_TRANSACTION_BATCH" -> f.transaction.setBatchExecutionId(1226L);
            case "NO_TRANSACTION" -> when(f.transactions.selectByBatchExecutionId(BATCH)).thenReturn(null);
            case "NO_TASK" -> when(f.tasks.selectTimelineListByBatchExecutionId(BATCH)).thenReturn(List.of());
            case "WRONG_TASK_TYPE" -> f.managerTask.setTaskType("PQC_PRODUCTION_RELEASE");
            case "WRONG_SCOPE" -> f.managerTask.setBusinessScopeType("RELEASE_APPLICATION");
            case "UNLOCKED" -> f.managerTask.setOwnershipLocked(false);
            case "CANCELLED" -> f.managerTask.setStatus("CANCELLED");
            case "UNKNOWN_STATUS" -> f.managerTask.setStatus("UNKNOWN");
            default -> throw new AssertionError(fault);
        }
        // No route/form configuration exists. It cannot become a blocked detail that bypasses authorization.
        ServiceException batchFailure = assertThrows(ServiceException.class, () -> f.batch(ACTOR, BATCH, null));
        assertEquals(1040750457, batchFailure.getCode());
        ServiceException previewFailure = assertThrows(ServiceException.class, () -> f.preview(ACTOR, f.row));
        assertEquals(1040750457, previewFailure.getCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"TODO", "DOING", "DONE"})
    void realFrozenManagerVisibilityAllowsOnlyReadsWithNoRouteConfiguration(String status) {
        f.managerTask.setStatus(status);
        assertNull(f.batch.getRouteId());
        assertContext(f.batch(ACTOR, BATCH, null), f.order, f.app);
        assertDoesNotThrow(() -> f.preview(ACTOR, f.row));
        verify(f.visibility, times(2)).requireVisibleBatch(f.batch, ACTOR);
        verify(f.tasks, times(2)).selectTimelineListByBatchExecutionId(BATCH);
    }

    @ParameterizedTest
    @ValueSource(strings = {"EMPTY", "NULL", "ID", "TENANT", "BATCH", "WORK_ORDER", "ACTIVE", "SECOND_FOREIGN"})
    void formalOriginsMustAllMatchTheAuthorizedApplication(String fault) {
        switch (fault) {
            case "EMPTY" -> when(f.origins.selectListByBatchExecutionId(BATCH)).thenReturn(List.of());
            case "NULL" -> when(f.origins.selectListByBatchExecutionId(BATCH)).thenReturn(null);
            case "ID" -> f.origin.setId(0L);
            case "TENANT" -> f.origin.setTenantId(2L);
            case "BATCH" -> f.origin.setBatchExecutionId(1226L);
            case "WORK_ORDER" -> f.origin.setWorkOrderId(990275L);
            case "ACTIVE" -> f.origin.setActiveOrderId(1010L);
            case "SECOND_FOREIGN" -> when(f.origins.selectListByBatchExecutionId(BATCH)).thenReturn(List.of(f.origin,
                    new MesProEdhrBatchExecutionOriginDO().setId(701L).setTenantId(TENANT)
                            .setBatchExecutionId(BATCH).setWorkOrderId(WORK_ORDER).setActiveOrderId(1010L)));
            default -> throw new AssertionError(fault);
        }
        assertThrows(ServiceException.class, () -> f.batch(ACTOR, BATCH, null));
        assertThrows(ServiceException.class, () -> f.preview(ACTOR, f.row));
    }

    @Test
    void multiplePickListOriginsForSameOrderRemainReadable() {
        f.origin.setPickListId(1L);
        when(f.origins.selectListByBatchExecutionId(BATCH)).thenReturn(List.of(f.origin,
                new MesProEdhrBatchExecutionOriginDO().setId(701L).setTenantId(TENANT)
                        .setBatchExecutionId(BATCH).setWorkOrderId(WORK_ORDER).setActiveOrderId(ACTIVE).setPickListId(2L)));
        assertContext(f.batch(ACTOR, BATCH, null), f.order, f.app);
        assertDoesNotThrow(() -> f.preview(ACTOR, f.row));
    }

    private static Object requireService(Fixture f) {
        try {
            Class<?> type = Class.forName(TYPE);
            Method exactLookup = MesProcessPoolActiveOrderReleaseApplicationMapper.class
                    .getMethod("selectByBatchExecutionId", Long.class);
            assertTrue(exactLookup.isDefault(), "exact formal batch application lookup must be an implemented Mapper default method");
            type.getMethod("requireBatch", Long.class, Long.class, Long.class);
            type.getMethod("requirePreview", Long.class, MesProcessPoolActiveOrderDossierFileDO.class);
            return type.getConstructor(MesProcessPoolActiveOrderReleaseApplicationMapper.class,
                    MesProcessPoolActiveOrderMapper.class, MesProEdhrBatchExecutionMapper.class,
                    MesProEdhrBatchExecutionVisibilityService.class, MesProEdhrBatchExecutionOriginMapper.class,
                    MesPqcProductionReleaseService.class, MesProEdhrWorkTaskMapper.class, PermissionApi.class)
                    .newInstance(f.apps, f.orders, f.batches, f.visibility, f.origins, f.pqcReader, f.tasks, f.permissions);
        } catch (ReflectiveOperationException missingContract) {
            throw new AssertionError("HISTORY-DOSSIER01 requires the real read-scope service with pure visibility authorization and exact mapper method", missingContract);
        }
    }

    private static Object call(Object target, String method, Class<?>[] types, Object... values) {
        try {
            return target.getClass().getMethod(method, types).invoke(target, values);
        } catch (InvocationTargetException delegated) {
            Throwable failure = delegated.getCause();
            if (failure instanceof RuntimeException runtime) { throw runtime; }
            if (failure instanceof Error error) { throw error; }
            throw new AssertionError("unexpected checked failure from actual read scope", failure);
        } catch (ReflectiveOperationException missingContract) {
            throw new AssertionError("actual read-scope method contract is missing", missingContract);
        }
    }

    private static void assertContext(Object result, MesProcessPoolActiveOrderDO active,
                                      MesProcessPoolActiveOrderReleaseApplicationDO app) {
        assertNotNull(result);
        assertTrue(result.getClass().isRecord(), "Context is an explicit record of already-authorized formal objects");
        assertEquals(active, call(result, "activeOrder", new Class<?>[0]));
        assertEquals(app, call(result, "application", new Class<?>[0]));
    }

    private static final class Fixture {
        final List<String> lookups = new ArrayList<>();
        final MesProcessPoolActiveOrderDO order = new MesProcessPoolActiveOrderDO().setId(ACTIVE)
                .setLeaderUserId(LEADER).setWorkOrderId(WORK_ORDER).setBusinessStatus("RELEASED");
        final MesProcessPoolActiveOrderReleaseApplicationDO app = new MesProcessPoolActiveOrderReleaseApplicationDO()
                .setId(APPLICATION).setActiveOrderId(ACTIVE).setBatchExecutionId(BATCH).setWorkOrderId(WORK_ORDER)
                .setPqcReleaseWorkTaskId(TASK).setApplicationStatus("RELEASED");
        final MesProEdhrWorkTaskDO task = new MesProEdhrWorkTaskDO().setId(TASK).setTaskType("PQC_PRODUCTION_RELEASE")
                .setBusinessScopeType("RELEASE_APPLICATION").setBusinessScopeId(APPLICATION).setCandidateUserSnapshot("346")
                .setOwnershipLocked(true).setWorkOrderId(WORK_ORDER).setBatchExecutionId(BATCH).setStatus("DONE");
        final MesProEdhrBatchExecutionDO batch = new MesProEdhrBatchExecutionDO().setId(BATCH)
                .setWorkOrderId(WORK_ORDER).setStatus(40).setTenantId(TENANT);
        final MesProEdhrBatchExecutionOriginDO origin = new MesProEdhrBatchExecutionOriginDO().setId(700L)
                .setBatchExecutionId(BATCH).setActiveOrderId(ACTIVE).setWorkOrderId(WORK_ORDER).setTenantId(TENANT);
        final MesPqcProductionReleaseDecisionResult decision = new MesPqcProductionReleaseDecisionResult()
                .setApplicationId(APPLICATION).setPqcReleaseWorkTaskId(TASK).setBatchExecutionId(BATCH).setStatus("RELEASED");
        final MesProcessPoolActiveOrderDossierFileDO row = new MesProcessPoolActiveOrderDossierFileDO()
                .setId(8001L).setActiveOrderId(ACTIVE).setApplicationId(APPLICATION).setFileId(7001L)
                .setCategoryKey("OTHER_FILE").setStorageConfigId(1L).setStoragePath("mes/active-order-dossier/1009/OTHER_FILE/receipt.pdf")
                .setFileUrl("/test-only-owned-file").setFileName("receipt.pdf").setContentType("application/pdf")
                .setFileSize(10L).setSha256("test-only-file-hash").setOperatorId(LEADER).setOperatorName("fixture leader")
                .setOperatedAt(LocalDateTime.of(2026, 10, 3, 12, 0));
        final MesProcessPoolActiveOrderReleaseApplicationMapper apps;
        final MesProcessPoolActiveOrderMapper orders = mock(MesProcessPoolActiveOrderMapper.class);
        final MesProEdhrBatchExecutionMapper batches = mock(MesProEdhrBatchExecutionMapper.class);
        final MesProEdhrBatchExecutionOriginMapper origins = mock(MesProEdhrBatchExecutionOriginMapper.class);
        final MesProEdhrBatchExecutionTaskMapper batchTasks = mock(MesProEdhrBatchExecutionTaskMapper.class);
        final MesProEdhrReleaseTransactionMapper transactions = mock(MesProEdhrReleaseTransactionMapper.class);
        final MesProEdhrBatchExecutionVisibilityService visibility = spy(new MesProEdhrBatchExecutionVisibilityService());
        final MesProEdhrReleaseTransactionDO transaction = new MesProEdhrReleaseTransactionDO()
                .setId(226L).setBatchExecutionId(BATCH);
        final MesProEdhrWorkTaskDO managerTask = new MesProEdhrWorkTaskDO().setId(2735L)
                .setBatchExecutionId(BATCH).setTaskType("RELEASE_APPROVE").setBusinessScopeType("RELEASE_TRANSACTION")
                .setBusinessScopeId(226L).setOwnershipLocked(true).setCandidateUserSnapshot("347").setStatus("DONE");
        final MesPqcProductionReleaseService pqcReader = mock(MesPqcProductionReleaseService.class);
        final MesProEdhrWorkTaskMapper tasks = mock(MesProEdhrWorkTaskMapper.class);
        final PermissionApi permissions = mock(PermissionApi.class);
        boolean allowBatch = true, allowPqc, missingApplication, missingActive, missingBatch, missingTask;
        Object target;

        Fixture() {
            order.setTenantId(TENANT); app.setTenantId(TENANT); row.setTenantId(TENANT);
            ReflectionTestUtils.setField(visibility, "permissionApi", permissions);
            ReflectionTestUtils.setField(visibility, "batchTaskMapper", batchTasks);
            ReflectionTestUtils.setField(visibility, "workTaskMapper", tasks);
            ReflectionTestUtils.setField(visibility, "releaseTransactionMapper", transactions);
            when(batchTasks.selectListByBatchExecutionId(BATCH)).thenReturn(List.of());
            when(transactions.selectByBatchExecutionId(BATCH)).thenReturn(transaction);
            when(tasks.selectTimelineListByBatchExecutionId(BATCH)).thenReturn(List.of(managerTask));
            when(origins.selectListByBatchExecutionId(BATCH)).thenReturn(List.of(origin));
            apps = mock(MesProcessPoolActiveOrderReleaseApplicationMapper.class, (Answer<Object>) invocation -> {
                String method = invocation.getMethod().getName();
                if (List.of("selectById", "selectByBatchExecutionId", "selectLatestByActiveOrderId").contains(method)) {
                    Object id = invocation.getArgument(0);
                    lookups.add(method + ":" + id);
                    Long expected = "selectById".equals(method) ? APPLICATION : "selectByBatchExecutionId".equals(method) ? BATCH : ACTIVE;
                    return !missingApplication && Objects.equals(id, expected) ? app : null;
                }
                return RETURNS_DEFAULTS.answer(invocation);
            });
            when(orders.selectById(anyLong())).thenAnswer(invocation ->
                    !missingActive && Objects.equals(invocation.getArgument(0), ACTIVE) ? order : null);
            when(tasks.selectById(anyLong())).thenAnswer(invocation ->
                    !missingTask && Objects.equals(invocation.getArgument(0), TASK) ? task : null);
            when(batches.selectById(anyLong())).thenAnswer(invocation ->
                    !missingBatch && Objects.equals(invocation.getArgument(0), BATCH) ? batch : null);
            when(pqcReader.get(anyLong(), anyLong())).thenAnswer(invocation ->
                    Objects.equals(invocation.getArgument(0), PQC_ACTOR)
                            && Objects.equals(invocation.getArgument(1), APPLICATION) ? decision : null);
            when(permissions.hasAnyPermissions(anyLong(), any(String[].class))).thenAnswer(invocation -> {
                String[] requested = (String[]) invocation.getRawArguments()[1];
                return Arrays.stream(requested).anyMatch(permission ->
                        BATCH_PERMISSION.equals(permission) && allowBatch || PQC_PERMISSION.equals(permission) && allowPqc);
            });
        }

        void login(Long actor, Long tenant) {
            TenantContextHolder.setTenantId(tenant);
            LoginUser login = new LoginUser().setId(actor).setTenantId(tenant).setUserType(2)
                    .setInfo(Map.of("username", "fixture-" + actor));
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login, null, List.of()));
        }
        Object batch(Long actor, Long batchId, Long activeId) {
            return call(target, "requireBatch", new Class<?>[]{Long.class, Long.class, Long.class}, actor, batchId, activeId);
        }
        void preview(Long actor, MesProcessPoolActiveOrderDossierFileDO file) {
            call(target, "requirePreview", new Class<?>[]{Long.class, MesProcessPoolActiveOrderDossierFileDO.class}, actor, file);
        }
        void corrupt(String fault) {
            switch (fault) {
                case "NO_BATCH" -> missingBatch = true;
                case "NO_APPLICATION" -> missingApplication = true;
                case "NO_ACTIVE" -> missingActive = true;
                case "BATCH_ID" -> batch.setId(1226L);
                case "BATCH_TENANT" -> batch.setTenantId(2L);
                case "BATCH_NULL_TENANT" -> batch.setTenantId(null);
                case "BATCH_ACTIVE" -> origin.setActiveOrderId(1010L);
                case "BATCH_WORK_ORDER" -> batch.setWorkOrderId(990275L);
                case "APP_ID" -> app.setId(0L);
                case "APP_ACTIVE" -> app.setActiveOrderId(1010L);
                case "APP_BATCH" -> app.setBatchExecutionId(1226L);
                case "APP_BATCH_NULL" -> app.setBatchExecutionId(null);
                case "APP_BATCH_ZERO" -> app.setBatchExecutionId(0L);
                case "APP_WORK_ORDER" -> app.setWorkOrderId(990275L);
                case "APP_TENANT" -> app.setTenantId(2L);
                case "APP_NULL_TENANT" -> app.setTenantId(null);
                case "ACTIVE_ID" -> order.setId(1010L);
                case "ACTIVE_WORK_ORDER" -> order.setWorkOrderId(990275L);
                case "ACTIVE_TENANT" -> order.setTenantId(2L);
                case "ACTIVE_NULL_TENANT" -> order.setTenantId(null);
                default -> throw new AssertionError("unknown source mismatch " + fault);
            }
        }
    }
}
