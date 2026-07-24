package cn.iocoder.yudao.module.mes.controller.admin.pro.workorder;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTriggerTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTypeEnum;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncCommand;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncContext;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncRuntimeService;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncTask;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee.MesKingdeeProductionMaterialListPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee.MesKingdeeProductionMaterialListOrderSyncReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee.MesKingdeeProductionMaterialListSyncRespVO;
import cn.iocoder.yudao.module.mes.service.pro.workorder.kingdee.MesKingdeeProductionMaterialListQueryService;
import cn.iocoder.yudao.module.mes.service.pro.workorder.sync.MesKingdeeProductionMaterialListSyncResult;
import cn.iocoder.yudao.module.mes.service.pro.workorder.sync.MesKingdeeProductionMaterialListSyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesKingdeeProductionMaterialListControllerTest {

    @Mock
    private MesKingdeeProductionMaterialListQueryService productionMaterialListQueryService;
    @Mock
    private MesKingdeeProductionMaterialListSyncService productionMaterialListSyncService;
    @Mock
    private ErpKingdeeSyncRuntimeService kingdeeSyncRuntimeService;

    @InjectMocks
    private MesKingdeeProductionMaterialListController controller;

    @Test
    void syncKingdeeProductionMaterialList_shouldUseManualOneYearBackfillWindow() {
        MesKingdeeProductionMaterialListSyncResult syncResult = new MesKingdeeProductionMaterialListSyncResult();
        syncResult.addCreated(101L);
        syncResult.addUpdated(202L);
        when(productionMaterialListSyncService.syncModifiedBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(syncResult);
        when(kingdeeSyncRuntimeService.executeSync(any(ErpKingdeeSyncCommand.class), any(ErpKingdeeSyncTask.class)))
                .thenAnswer(invocation -> {
                    ErpKingdeeSyncTask task = invocation.getArgument(1);
                    return task.run(ErpKingdeeSyncContext.builder()
                            .initialSync(false)
                            .windowStart(LocalDateTime.of(2025, 6, 30, 0, 0))
                            .windowEnd(LocalDateTime.of(2026, 6, 30, 10, 0))
                            .build());
                });

        CommonResult<MesKingdeeProductionMaterialListSyncRespVO> response = controller.syncKingdeeProductionMaterialList();

        assertEquals(0, response.getCode());
        assertNotNull(response.getData());
        assertEquals(Collections.singletonList(101L), response.getData().getCreatedIds());
        assertEquals(Collections.singletonList(202L), response.getData().getUpdatedIds());

        ArgumentCaptor<ErpKingdeeSyncCommand> commandCaptor = ArgumentCaptor.forClass(ErpKingdeeSyncCommand.class);
        verify(kingdeeSyncRuntimeService).executeSync(commandCaptor.capture(), any(ErpKingdeeSyncTask.class));
        assertEquals(ErpKingdeeSyncTypeEnum.PRODUCTION_MATERIAL_LIST, commandCaptor.getValue().getSyncType());
        assertEquals(ErpKingdeeSyncTriggerTypeEnum.MANUAL, commandCaptor.getValue().getTriggerType());
        assertTrue(commandCaptor.getValue().isForceInitialWindowStart());
        assertNotNull(commandCaptor.getValue().getInitialWindowStart());
        assertNotNull(commandCaptor.getValue().getWindowEnd());
        verify(productionMaterialListSyncService)
                .syncModifiedBetween(LocalDateTime.of(2025, 6, 30, 0, 0), LocalDateTime.of(2026, 6, 30, 10, 0));
    }

    @Test
    void contractMappings_exposeManualSyncEndpoint() throws Exception {
        Method method = MesKingdeeProductionMaterialListController.class
                .getDeclaredMethod("syncKingdeeProductionMaterialList");
        assertArrayEquals(new String[]{"/sync-kingdee"}, method.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasAnyPermissions('mes:pro-work-order:create', 'mes:pro-schedule-order:create')",
                method.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void syncKingdeeProductionMaterialListByOrderNos_shouldCallTargetedSyncService() {
        MesKingdeeProductionMaterialListSyncResult syncResult = new MesKingdeeProductionMaterialListSyncResult();
        syncResult.addUpdated(4491L);
        when(productionMaterialListSyncService.syncByProductionOrderNos(List.of("SMART-SCHED-20260630-RERUN9-MO")))
                .thenReturn(syncResult);
        MesKingdeeProductionMaterialListOrderSyncReqVO reqVO = new MesKingdeeProductionMaterialListOrderSyncReqVO();
        reqVO.setProductionOrderNos(List.of("SMART-SCHED-20260630-RERUN9-MO"));

        CommonResult<MesKingdeeProductionMaterialListSyncRespVO> response =
                controller.syncKingdeeProductionMaterialListByOrderNos(reqVO);

        assertEquals(0, response.getCode());
        assertNotNull(response.getData());
        assertEquals(Collections.singletonList(4491L), response.getData().getUpdatedIds());
        verify(productionMaterialListSyncService)
                .syncByProductionOrderNos(List.of("SMART-SCHED-20260630-RERUN9-MO"));
    }

    @Test
    void contractMappings_exposeTargetedProductionMaterialListSyncEndpoint() throws Exception {
        Method method = MesKingdeeProductionMaterialListController.class
                .getDeclaredMethod("syncKingdeeProductionMaterialListByOrderNos",
                        MesKingdeeProductionMaterialListOrderSyncReqVO.class);
        assertArrayEquals(new String[]{"/sync-kingdee-by-production-order-nos"},
                method.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasAnyPermissions('mes:pro-work-order:create', 'mes:pro-schedule-order:create')",
                method.getAnnotation(PreAuthorize.class).value());
    }
}
