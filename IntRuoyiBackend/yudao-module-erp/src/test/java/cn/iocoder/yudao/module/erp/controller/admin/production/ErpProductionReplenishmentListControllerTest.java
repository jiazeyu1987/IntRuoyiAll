package cn.iocoder.yudao.module.erp.controller.admin.production;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.erp.controller.admin.production.vo.ErpProductionReplenishmentListSyncRespVO;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTriggerTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTypeEnum;
import cn.iocoder.yudao.module.erp.service.production.kingdee.ErpKingdeeProductionReplenishmentListService;
import cn.iocoder.yudao.module.erp.service.production.sync.ErpKingdeeProductionReplenishmentListSyncResult;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncCommand;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncContext;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncRuntimeService;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErpProductionReplenishmentListControllerTest {

    @Mock
    private ErpKingdeeProductionReplenishmentListService productionReplenishmentListService;
    @Mock
    private ErpKingdeeSyncRuntimeService kingdeeSyncRuntimeService;
    @InjectMocks
    private ErpProductionReplenishmentListController controller;

    @Test
    void syncKingdeeProductionReplenishmentLists_usesManualHalfYearWindowRuntime() {
        ErpKingdeeProductionReplenishmentListSyncResult syncResult = new ErpKingdeeProductionReplenishmentListSyncResult();
        syncResult.addCreated();
        when(productionReplenishmentListService.syncModifiedBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(syncResult);
        when(kingdeeSyncRuntimeService.executeSync(any(ErpKingdeeSyncCommand.class), any(ErpKingdeeSyncTask.class)))
                .thenAnswer(invocation -> {
                    ErpKingdeeSyncCommand command = invocation.getArgument(0);
                    ErpKingdeeSyncTask task = invocation.getArgument(1);
                    return task.run(ErpKingdeeSyncContext.builder()
                            .initialSync(true)
                            .windowStart(command.getInitialWindowStart())
                            .windowEnd(command.getWindowEnd())
                            .build());
                });

        CommonResult<ErpProductionReplenishmentListSyncRespVO> response =
                controller.syncKingdeeProductionReplenishmentLists();

        assertEquals(0, response.getCode());
        assertNotNull(response.getData());
        assertEquals(1, response.getData().getCreatedCount());
        assertEquals(0, response.getData().getUpdatedCount());
        verify(productionReplenishmentListService, never()).syncAll(any(LocalDateTime.class), any(LocalDateTime.class));

        ArgumentCaptor<ErpKingdeeSyncCommand> commandCaptor =
                ArgumentCaptor.forClass(ErpKingdeeSyncCommand.class);
        verify(kingdeeSyncRuntimeService)
                .executeSync(commandCaptor.capture(), any(ErpKingdeeSyncTask.class));
        assertEquals(ErpKingdeeSyncTypeEnum.PRODUCTION_REPLENISHMENT_LIST,
                commandCaptor.getValue().getSyncType());
        assertEquals(ErpKingdeeSyncTriggerTypeEnum.MANUAL,
                commandCaptor.getValue().getTriggerType());
        assertTrue(commandCaptor.getValue().isForceInitialWindowStart());
        assertNotNull(commandCaptor.getValue().getInitialWindowStart());
        assertNotNull(commandCaptor.getValue().getWindowEnd());
        assertEquals(commandCaptor.getValue().getWindowEnd().toLocalDate().minusMonths(6).atStartOfDay(),
                commandCaptor.getValue().getInitialWindowStart());
        verify(productionReplenishmentListService).syncModifiedBetween(
                commandCaptor.getValue().getInitialWindowStart(),
                commandCaptor.getValue().getWindowEnd());
    }

}
