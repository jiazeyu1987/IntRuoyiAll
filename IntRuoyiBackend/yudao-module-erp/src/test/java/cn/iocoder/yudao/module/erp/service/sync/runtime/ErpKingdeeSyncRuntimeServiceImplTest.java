package cn.iocoder.yudao.module.erp.service.sync.runtime;

import cn.iocoder.yudao.module.erp.dal.dataobject.sync.ErpKingdeeSyncRunDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sync.ErpKingdeeSyncWatermarkDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sync.ErpKingdeeSyncRunMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sync.ErpKingdeeSyncWatermarkMapper;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncRunStatusEnum;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTriggerTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErpKingdeeSyncRuntimeServiceImplTest {

    @Mock
    private ErpKingdeeSyncWatermarkMapper watermarkMapper;
    @Mock
    private ErpKingdeeSyncRunMapper runMapper;

    private ErpKingdeeSyncRuntimeServiceImpl runtimeService;

    @BeforeEach
    void setUp() {
        runtimeService = new ErpKingdeeSyncRuntimeServiceImpl(
                new ErpKingdeeSyncRuntimeTransactionService(watermarkMapper, runMapper));
    }

    @Test
    void executeSync_rejectsWhenSameTypeIsAlreadyRunning() {
        when(runMapper.selectRunningBySyncType(ErpKingdeeSyncTypeEnum.PRODUCTION_ORDER.getType()))
                .thenReturn(new ErpKingdeeSyncRunDO());

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> runtimeService.executeSync(
                buildCommand(),
                context -> ErpKingdeeSyncRunResult.success(context.getWindowEnd(), 1, 0, 0, 0)));

        assertEquals("Kingdee sync task already running: PRODUCTION_ORDER", exception.getMessage());
        verify(runMapper, never()).insert(any(ErpKingdeeSyncRunDO.class));
        verify(watermarkMapper, never()).insert(any(ErpKingdeeSyncWatermarkDO.class));
    }

    @Test
    void executeSync_advancesWatermarkOnlyAfterSuccess() {
        LocalDateTime previousWatermark = LocalDateTime.of(2026, 6, 12, 1, 0);
        LocalDateTime windowEnd = LocalDateTime.of(2026, 6, 12, 2, 0);
        ErpKingdeeSyncWatermarkDO watermark = new ErpKingdeeSyncWatermarkDO();
        watermark.setId(11L);
        watermark.setSyncType(ErpKingdeeSyncTypeEnum.PRODUCTION_ORDER.getType());
        watermark.setLastSuccessTime(previousWatermark);
        when(watermarkMapper.selectBySyncType(ErpKingdeeSyncTypeEnum.PRODUCTION_ORDER.getType())).thenReturn(watermark);

        ErpKingdeeSyncRunResult result = runtimeService.executeSync(
                buildCommand(windowEnd),
                context -> {
                    assertEquals(previousWatermark, context.getWindowStart());
                    assertEquals(windowEnd, context.getWindowEnd());
                    assertEquals(false, context.isInitialSync());
                    return ErpKingdeeSyncRunResult.success(windowEnd, 2, 3, 1, 0);
                });

        assertEquals(2, result.getCreatedCount());
        assertEquals(3, result.getUpdatedCount());
        ArgumentCaptor<ErpKingdeeSyncRunDO> runInsertCaptor = ArgumentCaptor.forClass(ErpKingdeeSyncRunDO.class);
        verify(runMapper).insert(runInsertCaptor.capture());
        assertEquals(ErpKingdeeSyncRunStatusEnum.RUNNING.getStatus(), runInsertCaptor.getValue().getStatus());
        assertEquals(previousWatermark, runInsertCaptor.getValue().getWindowStartTime());
        assertEquals(windowEnd, runInsertCaptor.getValue().getWindowEndTime());

        ArgumentCaptor<ErpKingdeeSyncWatermarkDO> watermarkUpdateCaptor =
                ArgumentCaptor.forClass(ErpKingdeeSyncWatermarkDO.class);
        verify(watermarkMapper).updateById(watermarkUpdateCaptor.capture());
        assertEquals(11L, watermarkUpdateCaptor.getValue().getId());
        assertEquals(windowEnd, watermarkUpdateCaptor.getValue().getLastSuccessTime());

        ArgumentCaptor<ErpKingdeeSyncRunDO> runUpdateCaptor = ArgumentCaptor.forClass(ErpKingdeeSyncRunDO.class);
        verify(runMapper).updateById(runUpdateCaptor.capture());
        assertEquals(ErpKingdeeSyncRunStatusEnum.SUCCESS.getStatus(), runUpdateCaptor.getValue().getStatus());
        assertEquals(2, runUpdateCaptor.getValue().getCreatedCount());
        assertEquals(3, runUpdateCaptor.getValue().getUpdatedCount());
        assertEquals(1, runUpdateCaptor.getValue().getSkippedCount());
        assertNotNull(runUpdateCaptor.getValue().getEndedAt());
    }

    @Test
    void executeSync_initializesIncrementalWindowWhenWatermarkMissing() {
        LocalDateTime windowEnd = LocalDateTime.of(2026, 6, 12, 2, 0);
        when(watermarkMapper.selectBySyncType(ErpKingdeeSyncTypeEnum.PRODUCTION_ORDER.getType())).thenReturn(null);

        ErpKingdeeSyncRunResult result = runtimeService.executeSync(
                buildCommand(windowEnd),
                context -> {
                    assertEquals(windowEnd.minusSeconds(1), context.getWindowStart());
                    assertEquals(windowEnd, context.getWindowEnd());
                    assertEquals(true, context.isInitialSync());
                    return ErpKingdeeSyncRunResult.success(windowEnd, 0, 0, 0, 0);
                });

        assertEquals(0, result.getCreatedCount());
        ArgumentCaptor<ErpKingdeeSyncRunDO> runInsertCaptor = ArgumentCaptor.forClass(ErpKingdeeSyncRunDO.class);
        verify(runMapper).insert(runInsertCaptor.capture());
        assertEquals(windowEnd.minusSeconds(1), runInsertCaptor.getValue().getWindowStartTime());
        assertEquals(windowEnd, runInsertCaptor.getValue().getWindowEndTime());

        ArgumentCaptor<ErpKingdeeSyncWatermarkDO> watermarkInsertCaptor =
                ArgumentCaptor.forClass(ErpKingdeeSyncWatermarkDO.class);
        verify(watermarkMapper).insert(watermarkInsertCaptor.capture());
        assertEquals(ErpKingdeeSyncTypeEnum.PRODUCTION_ORDER.getType(), watermarkInsertCaptor.getValue().getSyncType());
        assertEquals(windowEnd, watermarkInsertCaptor.getValue().getLastSuccessTime());
    }

    @Test
    void executeSync_usesRequestedInitialWindowWhenWatermarkMissing() {
        LocalDateTime windowStart = LocalDateTime.of(2025, 6, 24, 0, 0);
        LocalDateTime windowEnd = LocalDateTime.of(2026, 6, 24, 9, 30);
        when(watermarkMapper.selectBySyncType(ErpKingdeeSyncTypeEnum.PRODUCTION_ORDER.getType())).thenReturn(null);

        runtimeService.executeSync(ErpKingdeeSyncCommand.builder()
                .syncType(ErpKingdeeSyncTypeEnum.PRODUCTION_ORDER)
                .triggerType(ErpKingdeeSyncTriggerTypeEnum.MANUAL)
                .initialWindowStart(windowStart)
                .windowEnd(windowEnd)
                .build(), context -> {
            assertEquals(windowStart, context.getWindowStart());
            assertEquals(windowEnd, context.getWindowEnd());
            assertEquals(true, context.isInitialSync());
            return ErpKingdeeSyncRunResult.success(windowEnd, 0, 0, 0, 0);
        });

        ArgumentCaptor<ErpKingdeeSyncRunDO> runInsertCaptor = ArgumentCaptor.forClass(ErpKingdeeSyncRunDO.class);
        verify(runMapper).insert(runInsertCaptor.capture());
        assertEquals(windowStart, runInsertCaptor.getValue().getWindowStartTime());
        assertEquals(windowEnd, runInsertCaptor.getValue().getWindowEndTime());
    }

    @Test
    void executeSync_recordsFailureAndKeepsWatermark() {
        RuntimeException failure = new RuntimeException("Kingdee API timeout");
        when(watermarkMapper.selectBySyncType(ErpKingdeeSyncTypeEnum.PRODUCTION_ORDER.getType())).thenReturn(null);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> runtimeService.executeSync(
                buildCommand(),
                context -> {
                    throw failure;
                }));

        assertSame(failure, thrown);
        verify(watermarkMapper, never()).insert(any(ErpKingdeeSyncWatermarkDO.class));
        verify(watermarkMapper, never()).updateById(any(ErpKingdeeSyncWatermarkDO.class));
        ArgumentCaptor<ErpKingdeeSyncRunDO> runUpdateCaptor = ArgumentCaptor.forClass(ErpKingdeeSyncRunDO.class);
        verify(runMapper).updateById(runUpdateCaptor.capture());
        assertEquals(ErpKingdeeSyncRunStatusEnum.FAILED.getStatus(), runUpdateCaptor.getValue().getStatus());
        assertEquals("Kingdee API timeout", runUpdateCaptor.getValue().getFailureMessage());
    }

    @Test
    void executeSync_truncatesLongFailureMessageToDatabaseColumnLimit() {
        String longMessage = "金蝶同步失败：".repeat(200);
        when(watermarkMapper.selectBySyncType(ErpKingdeeSyncTypeEnum.PRODUCTION_ORDER.getType())).thenReturn(null);

        assertThrows(RuntimeException.class, () -> runtimeService.executeSync(
                buildCommand(),
                context -> {
                    throw new RuntimeException(longMessage);
                }));

        ArgumentCaptor<ErpKingdeeSyncRunDO> runUpdateCaptor = ArgumentCaptor.forClass(ErpKingdeeSyncRunDO.class);
        verify(runMapper).updateById(runUpdateCaptor.capture());
        assertEquals(ErpKingdeeSyncRunStatusEnum.FAILED.getStatus(), runUpdateCaptor.getValue().getStatus());
        assertEquals(1000, runUpdateCaptor.getValue().getFailureMessage().length());
        assertEquals(longMessage.substring(0, 1000), runUpdateCaptor.getValue().getFailureMessage());
    }

    private static ErpKingdeeSyncCommand buildCommand() {
        return buildCommand(LocalDateTime.of(2026, 6, 12, 2, 0));
    }

    private static ErpKingdeeSyncCommand buildCommand(LocalDateTime windowEnd) {
        return ErpKingdeeSyncCommand.builder()
                .syncType(ErpKingdeeSyncTypeEnum.PRODUCTION_ORDER)
                .triggerType(ErpKingdeeSyncTriggerTypeEnum.AUTO)
                .windowEnd(windowEnd)
                .build();
    }

}
