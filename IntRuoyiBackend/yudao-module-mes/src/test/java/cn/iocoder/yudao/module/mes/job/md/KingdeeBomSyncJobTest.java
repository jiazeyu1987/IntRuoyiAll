package cn.iocoder.yudao.module.mes.job.md;

import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTriggerTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTypeEnum;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncCommand;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncContext;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncRuntimeService;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncTask;
import cn.iocoder.yudao.module.mes.service.md.item.kingdee.MesKingdeeBomListService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KingdeeBomSyncJobTest {

    @Test
    void execute_shouldSyncVersionedListWithoutApplyingProductBom() {
        MesKingdeeBomListService bomListService = mock(MesKingdeeBomListService.class);
        ErpKingdeeSyncRuntimeService runtimeService = mock(ErpKingdeeSyncRuntimeService.class);
        LocalDateTime windowStart = LocalDateTime.of(2026, 6, 12, 8, 0);
        LocalDateTime windowEnd = LocalDateTime.of(2026, 6, 12, 9, 0);
        when(bomListService.syncModifiedBetween(windowStart, windowEnd)).thenReturn(5);
        when(runtimeService.executeSync(any(ErpKingdeeSyncCommand.class), any(ErpKingdeeSyncTask.class)))
                .thenAnswer(invocation -> {
                    ErpKingdeeSyncTask task = invocation.getArgument(1);
                    var result = task.run(ErpKingdeeSyncContext.builder()
                            .syncType(ErpKingdeeSyncTypeEnum.BOM)
                            .windowStart(windowStart)
                            .windowEnd(windowEnd)
                            .build());
                    assertEquals(5, result.getUpdatedCount());
                    assertEquals(windowEnd, result.getWatermarkTime());
                    return result;
                });
        KingdeeBomSyncJob job = new KingdeeBomSyncJob(bomListService, runtimeService);

        String output = job.execute("");

        assertTrue(output.contains("bomLines=5"));
        verify(bomListService).syncModifiedBetween(windowStart, windowEnd);
        ArgumentCaptor<ErpKingdeeSyncCommand> commandCaptor = ArgumentCaptor.forClass(ErpKingdeeSyncCommand.class);
        verify(runtimeService).executeSync(commandCaptor.capture(), any(ErpKingdeeSyncTask.class));
        assertEquals(ErpKingdeeSyncTypeEnum.BOM, commandCaptor.getValue().getSyncType());
        assertEquals(ErpKingdeeSyncTriggerTypeEnum.AUTO, commandCaptor.getValue().getTriggerType());
        assertNotNull(commandCaptor.getValue().getWindowEnd());
    }

    @Test
    void execute_shouldExposeRuntimeFailure() {
        MesKingdeeBomListService bomListService = mock(MesKingdeeBomListService.class);
        ErpKingdeeSyncRuntimeService runtimeService = mock(ErpKingdeeSyncRuntimeService.class);
        when(runtimeService.executeSync(any(ErpKingdeeSyncCommand.class), any(ErpKingdeeSyncTask.class)))
                .thenThrow(new IllegalStateException("kingdee runtime failed"));
        KingdeeBomSyncJob job = new KingdeeBomSyncJob(bomListService, runtimeService);

        Assertions.assertThrows(IllegalStateException.class, () -> job.execute(""));
    }

    @Test
    void execute_shouldPropagateListSyncFailure() {
        MesKingdeeBomListService bomListService = mock(MesKingdeeBomListService.class);
        ErpKingdeeSyncRuntimeService runtimeService = mock(ErpKingdeeSyncRuntimeService.class);
        when(bomListService.syncModifiedBetween(any(), any()))
                .thenThrow(new IllegalStateException("ERP BOM snapshot invalid"));
        when(runtimeService.executeSync(any(), any())).thenAnswer(invocation -> {
            ErpKingdeeSyncTask task = invocation.getArgument(1);
            return task.run(ErpKingdeeSyncContext.builder().windowEnd(LocalDateTime.now()).build());
        });

        var job = new KingdeeBomSyncJob(bomListService, runtimeService);
        assertEquals("ERP BOM snapshot invalid", Assertions.assertThrows(IllegalStateException.class,
                () -> job.execute("")).getMessage());
    }

}
