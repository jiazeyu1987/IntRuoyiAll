package cn.iocoder.yudao.module.erp.service.sync.runtime;

import cn.iocoder.yudao.module.erp.dal.dataobject.sync.ErpKingdeeSyncRunDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sync.ErpKingdeeSyncWatermarkDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sync.ErpKingdeeSyncRunMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sync.ErpKingdeeSyncWatermarkMapper;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncRunStatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

@Service
@Validated
@RequiredArgsConstructor
public class ErpKingdeeSyncRuntimeServiceImpl implements ErpKingdeeSyncRuntimeService {

    private static final int FAILURE_MESSAGE_MAX_LENGTH = 1000;

    private final ErpKingdeeSyncWatermarkMapper watermarkMapper;
    private final ErpKingdeeSyncRunMapper runMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpKingdeeSyncRunResult executeSync(ErpKingdeeSyncCommand command, ErpKingdeeSyncTask task) {
        ErpKingdeeSyncRunDO running = runMapper.selectRunningBySyncType(command.getSyncType().getType());
        if (running != null) {
            throw new IllegalStateException("Kingdee sync task already running: " + command.getSyncType().name());
        }
        LocalDateTime now = LocalDateTime.now();
        ErpKingdeeSyncWatermarkDO watermark = watermarkMapper.selectBySyncType(command.getSyncType().getType());
        boolean forceInitialWindowStart = command.isForceInitialWindowStart();
        boolean initialSync = watermark == null || forceInitialWindowStart;
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
        try {
            ErpKingdeeSyncRunResult result = task.run(ErpKingdeeSyncContext.builder()
                    .syncType(command.getSyncType())
                    .triggerType(command.getTriggerType())
                    .initialSync(initialSync)
                    .windowStart(windowStart)
                    .windowEnd(command.getWindowEnd())
                    .build());
            if (result.getWatermarkTime() != null) {
                if (watermark == null) {
                    watermark = new ErpKingdeeSyncWatermarkDO();
                    watermark.setSyncType(command.getSyncType().getType());
                    watermark.setLastSuccessTime(result.getWatermarkTime());
                    watermarkMapper.insert(watermark);
                } else {
                    watermark.setLastSuccessTime(result.getWatermarkTime());
                    watermarkMapper.updateById(watermark);
                }
            }
            updateRun(run, ErpKingdeeSyncRunStatusEnum.SUCCESS.getStatus(), result, null, now);
            return result;
        } catch (RuntimeException ex) {
            updateRun(run, ErpKingdeeSyncRunStatusEnum.FAILED.getStatus(), null, ex.getMessage(), now);
            throw ex;
        }
    }

    private LocalDateTime resolveInitialWindowStart(ErpKingdeeSyncCommand command) {
        if (command.getInitialWindowStart() != null) {
            return command.getInitialWindowStart();
        }
        return command.getWindowEnd().minusSeconds(1);
    }

    private void updateRun(ErpKingdeeSyncRunDO run, Integer status, ErpKingdeeSyncRunResult result,
                           String failureMessage, LocalDateTime endedAt) {
        ErpKingdeeSyncRunDO update = new ErpKingdeeSyncRunDO();
        update.setId(run.getId());
        update.setStatus(status);
        update.setEndedAt(endedAt);
        if (result != null) {
            update.setCreatedCount(result.getCreatedCount());
            update.setUpdatedCount(result.getUpdatedCount());
            update.setSkippedCount(result.getSkippedCount());
            update.setFailedCount(result.getFailedCount());
            update.setWindowEndTime(result.getWatermarkTime() != null ? result.getWatermarkTime() : run.getWindowEndTime());
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
