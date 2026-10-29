package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionOriginDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionOriginMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesProEdhrActiveOrderLeaderBatchVisibilityTest {
    private static final Long USER = 9908090341L;
    private static final Long BATCH = 900000001249L;
    private static final Long ACTIVE = 1009200438L;
    private static final Long WORK = 990274L;
    private static final Long ROUTE = 980091L;
    private static final Long VERSION = 749L;
    @Mock private PermissionApi permissionApi;
    @Mock private MesProEdhrReleaseTransactionMapper releaseTransactionMapper;
    @Mock private MesProEdhrWorkTaskMapper workTaskMapper;
    @Mock private MesProEdhrBatchExecutionTaskMapper batchTaskMapper;
    @Mock private MesProEdhrBatchExecutionOriginMapper batchOriginMapper;
    @Mock private MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Mock private MesProcessPoolActiveOrderReleaseApplicationMapper releaseApplicationMapper;
    @Mock private MesProEdhrWorkTaskAssignmentRuleMapper assignmentRuleMapper;
    @Mock private MesProRouteProcessService routeProcessService;
    private MesProEdhrBatchExecutionVisibilityService service;
    private MesProEdhrBatchExecutionDO batch;
    private MesProEdhrBatchExecutionOriginDO origin;
    private MesProcessPoolActiveOrderDO active;

    @BeforeEach void setUp() {
        TenantContextHolder.setTenantId(1L);
        service = new MesProEdhrBatchExecutionVisibilityService();
        var marketAccess = org.mockito.Mockito.mock(MesMarketReleaseAccess.class);
        ReflectionTestUtils.setField(service, "marketReleaseAccess", marketAccess);
        org.mockito.Mockito.lenient().when(marketAccess.get(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new MesMarketReleaseAccess.Access(true, true, false, false, null, null, null, null));
        ReflectionTestUtils.setField(service, "permissionApi", permissionApi);
        ReflectionTestUtils.setField(service, "releaseTransactionMapper", releaseTransactionMapper);
        ReflectionTestUtils.setField(service, "workTaskMapper", workTaskMapper);
        ReflectionTestUtils.setField(service, "batchTaskMapper", batchTaskMapper);
        ReflectionTestUtils.setField(service, "batchOriginMapper", batchOriginMapper);
        ReflectionTestUtils.setField(service, "activeOrderMapper", activeOrderMapper);
        ReflectionTestUtils.setField(service, "releaseApplicationMapper", releaseApplicationMapper);
        batch = new MesProEdhrBatchExecutionDO().setId(BATCH).setTenantId(1L).setWorkOrderId(WORK)
                .setRouteId(ROUTE).setRouteVersionId(VERSION).setRouteVersionNo("V17")
                .setRouteSnapshotJson("{\"routeId\":980091,\"configSnapshots\":{}}")
                .setStatus(0);
        origin = origin(237L);
        active = new MesProcessPoolActiveOrderDO().setId(ACTIVE).setLeaderUserId(USER)
                .setWorkOrderId(WORK).setRouteId(ROUTE).setRouteVersionId(VERSION)
                .setActiveStatus("ACTIVE").setBusinessStatus("COMPLETED");
        active.setTenantId(1L);
        lenient().when(batchOriginMapper.selectListByBatchExecutionId(BATCH)).thenReturn(List.of(origin));
        lenient().when(activeOrderMapper.selectById(ACTIVE)).thenReturn(active);
    }

    @AfterEach void clearTenant() { TenantContextHolder.clear(); }

    private MesProEdhrBatchExecutionOriginDO origin(long id) {
        return new MesProEdhrBatchExecutionOriginDO().setId(id).setTenantId(1L)
                .setBatchExecutionId(BATCH).setActiveOrderId(ACTIVE).setWorkOrderId(WORK)
                .setEntryType("ACTIVE_ORDER_COMPLETION").setRelationStatus("CAPTURED")
                .setCompletionBackfillReceiptId(296L);
    }
    private boolean visible() { return service.canViewBatch(batch, List.of(), USER); }

    @Test void completedOwnActiveOrderWithoutApplicationCanViewBatch() {
        assertTrue(visible());
        assertDoesNotThrow(() -> service.requireVisibleBatch(batch, List.of(), USER));
    }

    @Test void bothSharedVisibilityOverloadsPermitTheSameOwner() {
        when(batchTaskMapper.selectListByBatchExecutionId(BATCH)).thenReturn(List.of());
        assertTrue(service.canViewBatch(batch, USER));
        assertDoesNotThrow(() -> service.requireVisibleBatch(batch, USER));
    }

    @Test void multiplePickListOriginsOfTheSameActiveOrderRemainReadable() {
        when(batchOriginMapper.selectListByBatchExecutionId(BATCH)).thenReturn(List.of(
                origin.setPickListBindingId(901L), origin(238L).setPickListBindingId(902L)));
        assertTrue(visible());
    }

    @Test void closedHistoricalBatchRemainsReadableByItsOriginalActiveOrderLeader() {
        active.setActiveStatus("REMOVED").setBusinessStatus("RELEASED");
        batch.setStatus(40);
        assertTrue(visible());
        verifyNoInteractions(routeProcessService, assignmentRuleMapper);
        verify(permissionApi, never()).getUserRoleIdListByUserId(anyLong());
    }

    @Test void ordinaryQueryOrRouteResponsibilityDoesNotExposeSomeoneElsesActiveOrder() {
        active.setLeaderUserId(9908090999L);
        assertFalse(visible());
        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.requireVisibleBatch(batch, List.of(), USER));
        assertEquals(MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_NOT_VISIBLE.getCode(),
                ex.getCode());
    }

    @Test void batchCreatorDoesNotReplaceTheFormalActiveOrderLeader() {
        batch.setCreator(String.valueOf(USER));
        active.setLeaderUserId(9908090999L);
        assertFalse(visible());
    }

    @Test void noOriginDoesNotInventAnActiveOrderOrGrantView() {
        when(batchOriginMapper.selectListByBatchExecutionId(BATCH)).thenReturn(List.of());
        assertFalse(visible());
        verifyNoInteractions(activeOrderMapper);
    }

    @Test void absentOrDeletedActiveOrderDoesNotGrantView() {
        when(activeOrderMapper.selectById(ACTIVE)).thenReturn(null);
        assertFalse(visible());
        when(activeOrderMapper.selectById(ACTIVE)).thenReturn(active);
        active.setDeleted(true);
        assertFalse(visible());
    }

    @Test void originFromAnotherTenantOrBatchIsDenied() {
        origin.setTenantId(2L); assertFalse(visible());
        origin.setTenantId(1L).setBatchExecutionId(BATCH + 1); assertFalse(visible());
    }

    @Test void crossTenantBatchOrActiveOrderIsDenied() {
        batch.setTenantId(2L); assertFalse(visible());
        batch.setTenantId(1L); active.setTenantId(2L); assertFalse(visible());
        active.setTenantId(1L); TenantContextHolder.setTenantId(2L); assertFalse(visible());
    }

    @Test void mixedOriginActiveOrdersAreDeniedEvenWhenFirstOneIsOwned() {
        when(batchOriginMapper.selectListByBatchExecutionId(BATCH)).thenReturn(List.of(
                origin, origin(238L).setActiveOrderId(ACTIVE + 1)));
        assertFalse(visible());
    }

    @Test void everyOriginMustHaveTheSameFormalWorkOrder() {
        when(batchOriginMapper.selectListByBatchExecutionId(BATCH)).thenReturn(List.of(
                origin, origin(238L).setWorkOrderId(WORK + 1)));
        assertFalse(visible());
    }

    @Test void originNeedsPersistentIdentityAndAnActiveOrder() {
        origin.setId(null); assertFalse(visible());
        origin.setId(237L).setActiveOrderId(null); assertFalse(visible());
        origin.setActiveOrderId(0L); assertFalse(visible());
    }

    @Test void activeIdentityWorkRouteAndFrozenVersionMustAllMatch() {
        active.setId(ACTIVE + 1); assertFalse(visible());
        active.setId(ACTIVE).setWorkOrderId(WORK + 1); assertFalse(visible());
        active.setWorkOrderId(WORK).setRouteId(ROUTE + 1); assertFalse(visible());
        active.setRouteId(ROUTE).setRouteVersionId(VERSION + 1); assertFalse(visible());
    }

    @Test void incompleteBatchContextAndDeletedOriginDoNotGrantOwnerView() {
        batch.setRouteVersionId(null); assertFalse(visible());
        batch.setRouteVersionId(VERSION).setRouteId(null); assertFalse(visible());
        batch.setRouteId(ROUTE).setWorkOrderId(null); assertFalse(visible());
        batch.setWorkOrderId(WORK); origin.setDeleted(true); assertFalse(visible());
    }

    @Test void batchViewDoesNotGrantAnyFillOrCloseTaskScope() {
        assertTrue(visible());
        var taskService = new MesProEdhrBatchTaskVisibilityService();
        ReflectionTestUtils.setField(taskService, "permissionApi", permissionApi);
        ReflectionTestUtils.setField(taskService, "workTaskMapper", workTaskMapper);
        ReflectionTestUtils.setField(taskService, "assignmentRuleMapper", assignmentRuleMapper);
        ReflectionTestUtils.setField(taskService, "routeProcessService", routeProcessService);
        when(workTaskMapper.selectTimelineListByBatchExecutionId(BATCH)).thenReturn(List.of());
        var task = new MesProEdhrBatchExecutionTaskDO().setId(700L).setBatchExecutionId(BATCH)
                .setBatchRecordReportId("independent-report").setRouteProcessId(2162L).setProcessId(113L);
        var scope = taskService.resolve(batch, List.of(task), USER);
        assertEquals(MesProEdhrBatchTaskVisibilityService.VISIBILITY_MODE_ASSIGNED, scope.mode());
        assertTrue(scope.tasks().isEmpty());
        assertFalse(scope.canView(task.getId()));
        verify(activeOrderMapper).selectById(ACTIVE);
        verifyNoMoreInteractions(activeOrderMapper);
    }
}
