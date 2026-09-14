package cn.iocoder.yudao.module.erp.job.production;

import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTriggerTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTypeEnum;
import cn.iocoder.yudao.module.erp.service.production.kingdee.ErpKingdeeProductionPickListService;
import cn.iocoder.yudao.module.erp.service.production.sync.ErpKingdeeProductionPickListSyncResult;
import cn.iocoder.yudao.module.erp.service.sync.admin.ErpKingdeeFullSyncHandler;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncCommand;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncContext;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncRuntimeService;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncTask;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KingdeeProductionPickListSyncJobTest {

    @Test
    void execute_shouldDispatchExplicitFullParameterToFullSync() {
        ErpKingdeeProductionPickListService pickListService =
                mock(ErpKingdeeProductionPickListService.class);
        ErpKingdeeSyncRuntimeService runtimeService = mock(ErpKingdeeSyncRuntimeService.class);
        ErpKingdeeProductionPickListSyncResult result =
                new ErpKingdeeProductionPickListSyncResult();
        result.addCreated();
        result.addSkipped("FID-OLD");
        when(pickListService.syncAllSkipExisting(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(result);
        when(runtimeService.executeSync(any(ErpKingdeeSyncCommand.class), any(ErpKingdeeSyncTask.class)))
                .thenAnswer(invocation -> invocation.<ErpKingdeeSyncTask>getArgument(1).run(
                        ErpKingdeeSyncContext.builder()
                                .syncType(ErpKingdeeSyncTypeEnum.PRODUCTION_PICK_LIST)
                                .triggerType(ErpKingdeeSyncTriggerTypeEnum.FULL)
                                .windowStart(invocation.<ErpKingdeeSyncCommand>getArgument(0).getInitialWindowStart())
                                .windowEnd(invocation.<ErpKingdeeSyncCommand>getArgument(0).getWindowEnd())
                                .build()));
        KingdeeProductionPickListSyncJob job =
                new KingdeeProductionPickListSyncJob(pickListService, runtimeService);

        String output = job.execute(ErpKingdeeFullSyncHandler.FULL_SYNC_JOB_PARAM);

        assertTrue(output.contains("full sync"));
        verify(pickListService).syncAllSkipExisting(
                any(LocalDateTime.class), any(LocalDateTime.class));
        org.mockito.ArgumentCaptor<ErpKingdeeSyncCommand> commandCaptor =
                org.mockito.ArgumentCaptor.forClass(ErpKingdeeSyncCommand.class);
        verify(runtimeService).executeSync(commandCaptor.capture(), any(ErpKingdeeSyncTask.class));
        assertTrue(commandCaptor.getValue().isForceInitialWindowStart());
        assertTrue(commandCaptor.getValue().getInitialWindowStart()
                .equals(commandCaptor.getValue().getWindowEnd().toLocalDate()
                        .minusDays(365).atStartOfDay()));
        verify(pickListService, never()).syncModifiedBetween(any(), any());
    }

}
