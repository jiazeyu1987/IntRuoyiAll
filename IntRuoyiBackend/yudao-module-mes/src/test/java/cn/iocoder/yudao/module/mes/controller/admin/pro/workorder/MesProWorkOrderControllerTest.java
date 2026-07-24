package cn.iocoder.yudao.module.mes.controller.admin.pro.workorder;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeSyncRunRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeSyncWatermarkRespVO;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTriggerTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTypeEnum;
import cn.iocoder.yudao.module.erp.service.sync.admin.ErpKingdeeSyncAdminService;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncCommand;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncContext;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncRuntimeService;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncTask;
import cn.iocoder.yudao.module.infra.dal.dataobject.job.JobDO;
import cn.iocoder.yudao.module.infra.service.job.JobService;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.MesProWorkOrderPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.MesProWorkOrderRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.MesProWorkOrderSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.MesProWorkOrderTemporaryFreezeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.MesProWorkOrderUpdateTemporaryFrozenReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesKingdeeProductionMaterialListDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesKingdeeProductionMaterialListMapper;
import cn.iocoder.yudao.module.mes.service.md.client.MesMdClientService;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.md.unitmeasure.MesMdUnitMeasureService;
import cn.iocoder.yudao.module.mes.service.md.vendor.MesMdVendorService;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import cn.iocoder.yudao.module.mes.service.pro.workorder.sync.MesKingdeeProductionOrderSyncResult;
import cn.iocoder.yudao.module.mes.service.pro.workorder.sync.MesKingdeeProductionOrderSyncService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;
import java.util.Collections;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_WORK_ORDER_MANUAL_OPERATION_FORBIDDEN;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesProWorkOrderControllerTest {
    @Mock private MesProWorkOrderService workOrderService;
    @Mock private MesKingdeeProductionOrderSyncService kingdeeProductionOrderSyncService;
    @Mock private ErpKingdeeSyncRuntimeService kingdeeSyncRuntimeService;
    @Mock private ErpKingdeeSyncAdminService kingdeeSyncAdminService;
    @Mock private JobService jobService;
    @Mock private MesMdItemService itemService;
    @Mock private MesMdClientService clientService;
    @Mock private MesMdVendorService vendorService;
    @Mock private MesMdUnitMeasureService unitMeasureService;
    @Mock private MesKingdeeProductionMaterialListMapper productionMaterialListMapper;
    @InjectMocks private MesProWorkOrderController controller;

    @Test void manualWriteEndpoints_rejectAndDoNotCallService() {
        MesProWorkOrderUpdateTemporaryFrozenReqVO reqVO = new MesProWorkOrderUpdateTemporaryFrozenReqVO(); reqVO.setId(100L); reqVO.setTemporaryFrozen(true);
        MesProWorkOrderTemporaryFreezeReqVO freezeReqVO = new MesProWorkOrderTemporaryFreezeReqVO(); freezeReqVO.setEnabled(true);
        assertManualWriteForbidden(() -> controller.createWorkOrder(new MesProWorkOrderSaveReqVO()));
        assertManualWriteForbidden(() -> controller.updateWorkOrder(new MesProWorkOrderSaveReqVO()));
        assertManualWriteForbidden(() -> controller.deleteWorkOrder(100L));
        assertManualWriteForbidden(() -> controller.updateTemporaryFreeze(freezeReqVO));
        assertManualWriteForbidden(() -> controller.updateWorkOrderTemporaryFrozen(reqVO));
        assertManualWriteForbidden(() -> controller.confirmWorkOrder(100L));
        assertManualWriteForbidden(() -> controller.finishWorkOrder(100L));
        assertManualWriteForbidden(() -> controller.cancelWorkOrder(100L));
        verifyNoInteractions(workOrderService);
    }

    @Test void contractMappings_exposeRowTemporaryFreezeEndpoint() throws Exception {
        Method method = MesProWorkOrderController.class.getDeclaredMethod("updateWorkOrderTemporaryFrozen", MesProWorkOrderUpdateTemporaryFrozenReqVO.class);
        assertArrayEquals(new String[]{"/update-temporary-frozen"}, method.getAnnotation(PutMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-work-order:update')", method.getAnnotation(PreAuthorize.class).value());
    }

    @Test void getKingdeeSyncStatus_reportsConfiguredAutoSyncAndLatestRun() {
        JobDO job = new JobDO(); job.setId(88L); job.setName("Kingdee Production Order Sync"); job.setCronExpression("0 0 2 * * ?"); job.setStatus(CommonStatusEnum.ENABLE.getStatus());
        when(jobService.getJobPage(any())).thenReturn(new PageResult<>(List.of(job), 1L));
        ErpKingdeeSyncRunRespVO run = new ErpKingdeeSyncRunRespVO(); run.setStatus(20); run.setTriggerType("MANUAL"); run.setStartedAt(LocalDateTime.of(2026, 6, 25, 9, 0)); run.setEndedAt(LocalDateTime.of(2026, 6, 25, 9, 3)); run.setCreatedCount(3); run.setUpdatedCount(1); run.setSkippedCount(2);
        when(kingdeeSyncAdminService.getRunPage(any())).thenReturn(new PageResult<>(List.of(run), 1L));
        ErpKingdeeSyncWatermarkRespVO watermark = new ErpKingdeeSyncWatermarkRespVO(); watermark.setSyncType("PRODUCTION_ORDER"); watermark.setLastSuccessTime(LocalDateTime.of(2026, 6, 25, 9, 3));
        when(kingdeeSyncAdminService.getWatermarks()).thenReturn(List.of(watermark));
        CommonResult<?> response = controller.getKingdeeSyncStatus();
        assertEquals(0, response.getCode()); assertNotNull(response.getData());
    }

    @Test void syncKingdeeWorkOrders_usesRuntimeWatermarkWindow() {
        MesKingdeeProductionOrderSyncResult syncResult = new MesKingdeeProductionOrderSyncResult(); syncResult.addCreated(501L);
        when(kingdeeProductionOrderSyncService.syncWorkOrders(any(ErpKingdeeSyncContext.class))).thenReturn(syncResult);
        when(kingdeeSyncRuntimeService.executeSync(any(ErpKingdeeSyncCommand.class), any(ErpKingdeeSyncTask.class))).thenAnswer(invocation -> {
            ErpKingdeeSyncTask task = invocation.getArgument(1);
            return task.run(ErpKingdeeSyncContext.builder().initialSync(false).windowStart(LocalDateTime.of(2026, 6, 23, 10, 0)).windowEnd(LocalDateTime.of(2026, 6, 24, 10, 0)).build());
        });
        CommonResult<?> response = controller.syncKingdeeWorkOrders();
        Assertions.assertEquals(0, response.getCode());
        verify(kingdeeProductionOrderSyncService).syncWorkOrders(any(ErpKingdeeSyncContext.class));
        ArgumentCaptor<ErpKingdeeSyncCommand> commandCaptor = ArgumentCaptor.forClass(ErpKingdeeSyncCommand.class);
        verify(kingdeeSyncRuntimeService).executeSync(commandCaptor.capture(), any(ErpKingdeeSyncTask.class));
        assertEquals(ErpKingdeeSyncTypeEnum.PRODUCTION_ORDER, commandCaptor.getValue().getSyncType());
        assertEquals(ErpKingdeeSyncTriggerTypeEnum.MANUAL, commandCaptor.getValue().getTriggerType());
        assertNotNull(commandCaptor.getValue().getInitialWindowStart());
        assertNotNull(commandCaptor.getValue().getWindowEnd());
    }

    @Test void getWorkOrderPage_shouldExposeProductionMaterialListSummary() {
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(1001L)
                .code("WO-1001")
                .name("导管工单")
                .quantity(new BigDecimal("10"))
                .quantityProduced(BigDecimal.ZERO)
                .quantityChanged(BigDecimal.ZERO)
                .quantityScheduled(BigDecimal.ZERO)
                .workshopName("组装车间")
                .bomVersion("BOM-2026-01")
                .pickMode("直接领料")
                .auxiliaryCode("K20260113")
                .businessStatus("424")
                .drawingNumber("255ACSXXXX")
                .scheduleStatus("未排产")
                .plannedStartTime(LocalDateTime.of(2026, 3, 25, 0, 0))
                .plannedEndTime(LocalDateTime.of(2026, 3, 26, 0, 0))
                .temporaryFrozen(Boolean.FALSE)
                .build();
        when(workOrderService.getWorkOrderPage(any(MesProWorkOrderPageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(workOrder), 1L));
        when(itemService.getItemMap(any())).thenReturn(Collections.emptyMap());
        when(clientService.getClientMap(any())).thenReturn(Collections.emptyMap());
        when(vendorService.getVendorMap(any())).thenReturn(Collections.emptyMap());
        when(unitMeasureService.getUnitMeasureMap(any())).thenReturn(Collections.emptyMap());
        when(workOrderService.getWorkOrderMap(any())).thenReturn(Collections.emptyMap());
        when(productionMaterialListMapper.selectListByWorkOrderIds(List.of(1001L))).thenReturn(List.of(
                MesKingdeeProductionMaterialListDO.builder().workOrderId(1001L).sourceBillNo("PPBOM-001").build(),
                MesKingdeeProductionMaterialListDO.builder().workOrderId(1001L).sourceBillNo("PPBOM-002").build()
        ));

        CommonResult<PageResult<MesProWorkOrderRespVO>> response =
                controller.getWorkOrderPage(new MesProWorkOrderPageReqVO());

        assertEquals(0, response.getCode());
        assertNotNull(response.getData());
        assertEquals(1L, response.getData().getTotal());
        assertEquals(1, response.getData().getList().size());
        MesProWorkOrderRespVO row = response.getData().getList().get(0);
        assertEquals(2L, row.getProductionMaterialListCount());
        assertEquals("PPBOM-001、PPBOM-002", row.getProductionMaterialListSummary());
        assertEquals("组装车间", row.getWorkshopName());
        assertEquals("BOM-2026-01", row.getBomVersion());
        assertEquals("直接领料", row.getPickMode());
        assertEquals("K20260113", row.getAuxiliaryCode());
        assertEquals("424", row.getBusinessStatus());
        assertEquals("255ACSXXXX", row.getDrawingNumber());
        assertEquals("未排产", row.getScheduleStatus());
        assertEquals(LocalDateTime.of(2026, 3, 25, 0, 0), row.getPlannedStartTime());
        assertEquals(LocalDateTime.of(2026, 3, 26, 0, 0), row.getPlannedEndTime());
    }

    private void assertManualWriteForbidden(Runnable runnable) {
        ServiceException exception = assertThrows(ServiceException.class, runnable::run);
        assertEquals(PRO_WORK_ORDER_MANUAL_OPERATION_FORBIDDEN.getCode(), exception.getCode());
        assertNotNull(exception.getMessage());
    }

}
