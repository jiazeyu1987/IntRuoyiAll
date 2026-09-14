package cn.iocoder.yudao.module.erp.controller.admin.production;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.erp.controller.admin.production.vo.ErpProductionPickListSyncRespVO;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTriggerTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTypeEnum;
import cn.iocoder.yudao.module.erp.service.production.kingdee.ErpKingdeeProductionPickListService;
import cn.iocoder.yudao.module.erp.service.production.sync.ErpKingdeeProductionPickListSyncResult;
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
class ErpProductionPickListControllerTest {

    @Mock
    private ErpKingdeeProductionPickListService productionPickListService;
    @Mock
    private ErpKingdeeSyncRuntimeService kingdeeSyncRuntimeService;
    @InjectMocks
    private ErpProductionPickListController controller;

    @Test
    void syncKingdeeProductionPickLists_usesManualHalfYearWindowRuntime() {
        ErpKingdeeProductionPickListSyncResult syncResult = new ErpKingdeeProductionPickListSyncResult();
        syncResult.addCreated();
        when(productionPickListService.syncModifiedBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
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

        CommonResult<ErpProductionPickListSyncRespVO> response =
                controller.syncKingdeeProductionPickLists();

        assertEquals(0, response.getCode());
        assertNotNull(response.getData());
        assertEquals(1, response.getData().getCreatedCount());
        assertEquals(0, response.getData().getUpdatedCount());
        verify(productionPickListService, never()).syncAll(any(LocalDateTime.class), any(LocalDateTime.class));

        ArgumentCaptor<ErpKingdeeSyncCommand> commandCaptor =
                ArgumentCaptor.forClass(ErpKingdeeSyncCommand.class);
        verify(kingdeeSyncRuntimeService)
                .executeSync(commandCaptor.capture(), any(ErpKingdeeSyncTask.class));
        assertEquals(ErpKingdeeSyncTypeEnum.PRODUCTION_PICK_LIST,
                commandCaptor.getValue().getSyncType());
        assertEquals(ErpKingdeeSyncTriggerTypeEnum.MANUAL,
                commandCaptor.getValue().getTriggerType());
        assertTrue(commandCaptor.getValue().isForceInitialWindowStart());
        assertNotNull(commandCaptor.getValue().getInitialWindowStart());
        assertNotNull(commandCaptor.getValue().getWindowEnd());
        assertEquals(commandCaptor.getValue().getWindowEnd().toLocalDate().minusMonths(6).atStartOfDay(),
                commandCaptor.getValue().getInitialWindowStart());
        verify(productionPickListService).syncModifiedBetween(
                commandCaptor.getValue().getInitialWindowStart(),
                commandCaptor.getValue().getWindowEnd());
    }

}
