package cn.iocoder.yudao.module.erp.service.sync.runtime;

import cn.iocoder.yudao.module.erp.dal.dataobject.sync.ErpKingdeeSyncRunDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sync.ErpKingdeeSyncWatermarkDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sync.ErpKingdeeSyncRunMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sync.ErpKingdeeSyncWatermarkMapper;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncRunStatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

@Service
@Validated
@RequiredArgsConstructor
public class ErpKingdeeSyncRuntimeServiceImpl implements ErpKingdeeSyncRuntimeService {

    private static final int FAILURE_MESSAGE_MAX_LENGTH = 1000;

    private final ErpKingdeeSyncRunMapper runMapper;
    private final ErpKingdeeSyncWatermarkMapper watermarkMapper;

    @Override
    public ErpKingdeeSyncRunResult executeSync(ErpKingdeeSyncCommand command, ErpKingdeeSyncTask task) {
        requireCommand(command);
        if (task == null) {
            throw new IllegalArgumentException("Kingdee sync task must not be null");
        }

        String syncType = command.getSyncType().getType();
        ErpKingdeeSyncRunDO runningRun = runMapper.selectRunningBySyncType(syncType);
        if (runningRun != null) {
            throw new IllegalStateException("Kingdee sync is already running: " + syncType);
        }

        ErpKingdeeSyncWatermarkDO watermark = watermarkMapper.selectBySyncType(syncType);
        boolean initialSync = isInitialSync(command, watermark);
        LocalDateTime windowStart = resolveWindowStart(command, watermark, initialSync);
        ErpKingdeeSyncRunDO run = createRunningRun(command, windowStart);
        runMapper.insert(run);

        ErpKingdeeSyncContext context = ErpKingdeeSyncContext.builder()
                .syncType(command.getSyncType())
                .triggerType(command.getTriggerType())
                .initialSync(initialSync)
                .windowStart(windowStart)
                .windowEnd(command.getWindowEnd())
                .build();
        try {
            ErpKingdeeSyncRunResult result = task.run(context);
            completeSuccess(run, result);
            upsertWatermark(syncType, result.getWatermarkTime());
            return result;
        } catch (RuntimeException ex) {
            completeFailure(run, ex);
            throw ex;
        }
    }

    private void requireCommand(ErpKingdeeSyncCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Kingdee sync command must not be null");
        }
        if (command.getSyncType() == null) {
            throw new IllegalArgumentException("Kingdee sync type must not be null");
        }
        if (command.getTriggerType() == null) {
            throw new IllegalArgumentException("Kingdee sync trigger type must not be null");
        }
        if (command.getWindowEnd() == null) {
            throw new IllegalArgumentException("Kingdee sync window end must not be null");
        }
        if (Boolean.TRUE.equals(command.isForceInitialWindowStart()) && command.getInitialWindowStart() == null) {
            throw new IllegalArgumentException("Kingdee sync initial window start must not be null when forced");
        }
        if (command.getInitialWindowStart() != null && command.getInitialWindowStart().isAfter(command.getWindowEnd())) {
            throw new IllegalArgumentException("Kingdee sync initial window start must not be after window end");
        }
    }

    private boolean isInitialSync(ErpKingdeeSyncCommand command, ErpKingdeeSyncWatermarkDO watermark) {
        if (Boolean.TRUE.equals(command.isForceInitialWindowStart())) {
            return true;
        }
        return command.getInitialWindowStart() != null
                && (watermark == null || watermark.getLastSuccessTime() == null);
    }

    private LocalDateTime resolveWindowStart(ErpKingdeeSyncCommand command, ErpKingdeeSyncWatermarkDO watermark,
                                             boolean initialSync) {
        if (initialSync) {
            return command.getInitialWindowStart();
        }
        if (watermark == null || watermark.getLastSuccessTime() == null) {
            return command.getWindowEnd();
        }
        return watermark.getLastSuccessTime();
    }

    private ErpKingdeeSyncRunDO createRunningRun(ErpKingdeeSyncCommand command, LocalDateTime windowStart) {
        LocalDateTime now = LocalDateTime.now();
        return ErpKingdeeSyncRunDO.builder()
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
    }

    private void completeSuccess(ErpKingdeeSyncRunDO run, ErpKingdeeSyncRunResult result) {
        if (result == null) {
            throw new IllegalStateException("Kingdee sync task returned null result");
        }
        if (result.getWatermarkTime() == null) {
            throw new IllegalStateException("Kingdee sync success result must include watermark time");
        }
        run.setStatus(ErpKingdeeSyncRunStatusEnum.SUCCESS.getStatus());
        run.setEndedAt(LocalDateTime.now());
        run.setCreatedCount(nullToZero(result.getCreatedCount()));
        run.setUpdatedCount(nullToZero(result.getUpdatedCount()));
        run.setSkippedCount(nullToZero(result.getSkippedCount()));
        run.setFailedCount(nullToZero(result.getFailedCount()));
        run.setFailureMessage(null);
        runMapper.updateById(run);
    }

    private void completeFailure(ErpKingdeeSyncRunDO run, RuntimeException ex) {
        run.setStatus(ErpKingdeeSyncRunStatusEnum.FAILED.getStatus());
        run.setEndedAt(LocalDateTime.now());
        run.setFailureMessage(truncateFailureMessage(ex.getMessage()));
        runMapper.updateById(run);
    }

    private void upsertWatermark(String syncType, LocalDateTime watermarkTime) {
        ErpKingdeeSyncWatermarkDO watermark = watermarkMapper.selectBySyncType(syncType);
        if (watermark == null) {
            watermarkMapper.insert(ErpKingdeeSyncWatermarkDO.builder()
                    .syncType(syncType)
                    .lastSuccessTime(watermarkTime)
                    .build());
            return;
        }
        watermark.setLastSuccessTime(watermarkTime);
        watermarkMapper.updateById(watermark);
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String truncateFailureMessage(String message) {
        if (message == null || message.length() <= FAILURE_MESSAGE_MAX_LENGTH) {
            return message;
        }
        return message.substring(0, FAILURE_MESSAGE_MAX_LENGTH);
    }

}