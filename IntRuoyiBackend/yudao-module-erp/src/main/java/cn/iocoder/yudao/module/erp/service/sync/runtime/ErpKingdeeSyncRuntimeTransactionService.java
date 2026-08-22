package cn.iocoder.yudao.module.erp.service.sync.runtime;

import cn.iocoder.yudao.module.erp.dal.dataobject.sync.ErpKingdeeSyncRunDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sync.ErpKingdeeSyncWatermarkDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sync.ErpKingdeeSyncRunMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sync.ErpKingdeeSyncWatermarkMapper;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncRunStatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ErpKingdeeSyncRuntimeTransactionService {

    private static final int FAILURE_MESSAGE_MAX_LENGTH = 1000;

    private final ErpKingdeeSyncWatermarkMapper watermarkMapper;
    private final ErpKingdeeSyncRunMapper runMapper;

    @Transactional(rollbackFor = Exception.class)
    public ErpKingdeeSyncExecution start(ErpKingdeeSyncCommand command) {
        ErpKingdeeSyncRunDO running = runMapper.selectRunningBySyncType(command.getSyncType().getType());
        if (running != null) {
            throw new IllegalStateException("Kingdee sync task already running: " + command.getSyncType().name());
        }
        LocalDateTime now = LocalDateTime.now();
        ErpKingdeeSyncWatermarkDO watermark = watermarkMapper.selectBySyncType(command.getSyncType().getType());
        boolean initialSync = watermark == null || command.isForceInitialWindowStart();
        LocalDateTime windowStart = initialSync ? resolveInitialWindowStart(command) : watermark.getLastSuccessTime();
        ErpKingdeeSyncRunDO run = ErpKingdeeSyncRunDO.builder()
                .syncType(command.getSyncType().getType())
                .triggerType(command.getTriggerType().getTriggerType())
                .status(ErpKingdeeSyncRunStatusEnum.RUNNING.getStatus())
                .windowStartTime(windowStart)
                .windowEndTime(command.getWindowEnd())
                .startedAt(now)
                .createdCount(0)
                .updatedCount(0)
                .skippedCount(0)
                .failedCount(0)
                .build();
        runMapper.insert(run);
        return new ErpKingdeeSyncExecution(run.getId(), ErpKingdeeSyncContext.builder()
                .syncType(command.getSyncType())
                .triggerType(command.getTriggerType())
                .initialSync(initialSync)
                .windowStart(windowStart)
                .windowEnd(command.getWindowEnd())
                .build());
    }

    @Transactional(rollbackFor = Exception.class)
    public void complete(ErpKingdeeSyncExecution execution, ErpKingdeeSyncRunResult result) {
        if (result.getWatermarkTime() != null) {
            ErpKingdeeSyncWatermarkDO watermark = watermarkMapper.selectBySyncType(
                    execution.getContext().getSyncType().getType());
            if (watermark == null) {
                watermark = new ErpKingdeeSyncWatermarkDO();
                watermark.setSyncType(execution.getContext().getSyncType().getType());
                watermark.setLastSuccessTime(result.getWatermarkTime());
                watermarkMapper.insert(watermark);
            } else {
                watermark.setLastSuccessTime(result.getWatermarkTime());
                watermarkMapper.updateById(watermark);
            }
        }
        updateRun(execution.getRunId(), ErpKingdeeSyncRunStatusEnum.SUCCESS.getStatus(), result,
                null, execution.getContext().getWindowEnd());
    }

    @Transactional(rollbackFor = Exception.class)
    public void fail(ErpKingdeeSyncExecution execution, RuntimeException failure) {
        updateRun(execution.getRunId(), ErpKingdeeSyncRunStatusEnum.FAILED.getStatus(), null,
                failure.getMessage(), execution.getContext().getWindowEnd());
    }

    private LocalDateTime resolveInitialWindowStart(ErpKingdeeSyncCommand command) {
        if (command.getInitialWindowStart() != null) {
            return command.getInitialWindowStart();
        }
        return command.getWindowEnd().minusSeconds(1);
    }

    private void updateRun(Long runId, Integer status, ErpKingdeeSyncRunResult result,
                           String failureMessage, LocalDateTime windowEnd) {
        ErpKingdeeSyncRunDO update = new ErpKingdeeSyncRunDO();
        update.setId(runId);
        update.setStatus(status);
        update.setEndedAt(LocalDateTime.now());
        if (result != null) {
            update.setCreatedCount(result.getCreatedCount());
            update.setUpdatedCount(result.getUpdatedCount());
            update.setSkippedCount(result.getSkippedCount());
            update.setFailedCount(result.getFailedCount());
            update.setWindowEndTime(result.getWatermarkTime() != null ? result.getWatermarkTime() : windowEnd);
        }
        update.setFailureMessage(truncateFailureMessage(failureMessage));
        runMapper.updateById(update);
    }

    private String truncateFailureMessage(String failureMessage) {
        if (failureMessage == null || failureMessage.length() <= FAILURE_MESSAGE_MAX_LENGTH) {
            return failureMessage;
        }
        return failureMessage.substring(0, FAILURE_MESSAGE_MAX_LENGTH);
    }

}
