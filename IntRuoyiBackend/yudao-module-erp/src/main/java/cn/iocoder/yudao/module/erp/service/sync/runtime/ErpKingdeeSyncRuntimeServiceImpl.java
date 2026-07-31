package cn.iocoder.yudao.module.erp.service.sync.runtime;

import cn.iocoder.yudao.module.erp.dal.dataobject.sync.ErpKingdeeSyncRunDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sync.ErpKingdeeSyncWatermarkDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sync.ErpKingdeeSyncRunMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sync.ErpKingdeeSyncWatermarkMapper;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncRunStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ErpKingdeeSyncRuntimeServiceImpl implements ErpKingdeeSyncRuntimeService {

    @Resource
    private ErpKingdeeSyncRunMapper syncRunMapper;
    @Resource
    private ErpKingdeeSyncWatermarkMapper syncWatermarkMapper;

    @Override
    public ErpKingdeeSyncRunResult executeSync(ErpKingdeeSyncCommand command, ErpKingdeeSyncTask task) {
        validate(command, task);
        String syncType = command.getSyncType().getType();
        if (syncRunMapper.selectRunningBySyncType(syncType) != null) {
            throw new IllegalStateException("ERP Kingdee sync is already running: " + syncType);
        }

        ErpKingdeeSyncWatermarkDO watermark = syncWatermarkMapper.selectBySyncType(syncType);
        LocalDateTime windowEnd = command.getWindowEnd();
        boolean initialSync = command.isForceInitialWindowStart()
                || watermark == null || watermark.getLastSuccessTime() == null;
        LocalDateTime windowStart = initialSync
                ? resolveInitialWindowStart(command)
                : watermark.getLastSuccessTime();

        ErpKingdeeSyncRunDO run = ErpKingdeeSyncRunDO.builder()
                .syncType(syncType)
                .triggerType(command.getTriggerType().getTriggerType())
                .status(ErpKingdeeSyncRunStatusEnum.RUNNING.getStatus())
                .windowStartTime(windowStart)
                .windowEndTime(windowEnd)
                .startedAt(LocalDateTime.now())
                .createdCount(0)
                .updatedCount(0)
                .skippedCount(0)
                .failedCount(0)
                .build();
        syncRunMapper.insert(run);

        try {
            ErpKingdeeSyncRunResult result = task.run(ErpKingdeeSyncContext.builder()
                    .syncType(command.getSyncType())
                    .triggerType(command.getTriggerType())
                    .initialSync(initialSync)
                    .windowStart(windowStart)
                    .windowEnd(windowEnd)
                    .build());
            if (result == null) {
                throw new IllegalStateException("ERP Kingdee sync task returned null result: " + syncType);
            }
            markSuccess(run, result);
            updateWatermark(watermark, syncType, result.getWatermarkTime());
            return result;
        } catch (RuntimeException | Error ex) {
            markFailed(run, ex);
            throw ex;
        }
    }

    private void validate(ErpKingdeeSyncCommand command, ErpKingdeeSyncTask task) {
        if (command == null) {
            throw new IllegalArgumentException("ERP Kingdee sync command is required");
        }
        if (command.getSyncType() == null) {
            throw new IllegalArgumentException("ERP Kingdee sync type is required");
        }
        if (command.getTriggerType() == null) {
            throw new IllegalArgumentException("ERP Kingdee sync trigger type is required");
        }
        if (command.getWindowEnd() == null) {
            throw new IllegalArgumentException("ERP Kingdee sync window end is required");
        }
        if (command.isForceInitialWindowStart() && command.getInitialWindowStart() == null) {
            throw new IllegalArgumentException("ERP Kingdee forced initial sync requires initial window start");
        }
        if (task == null) {
            throw new IllegalArgumentException("ERP Kingdee sync task is required");
        }
    }

    private LocalDateTime resolveInitialWindowStart(ErpKingdeeSyncCommand command) {
        if (command.getInitialWindowStart() != null) {
            return command.getInitialWindowStart();
        }
        return command.getWindowEnd();
    }

    private void markSuccess(ErpKingdeeSyncRunDO run, ErpKingdeeSyncRunResult result) {
        run.setStatus(ErpKingdeeSyncRunStatusEnum.SUCCESS.getStatus());
        run.setEndedAt(LocalDateTime.now());
        run.setCreatedCount(result.getCreatedCount());
        run.setUpdatedCount(result.getUpdatedCount());
        run.setSkippedCount(result.getSkippedCount());
        run.setFailedCount(result.getFailedCount());
        syncRunMapper.updateById(run);
    }

    private void markFailed(ErpKingdeeSyncRunDO run, Throwable ex) {
        run.setStatus(ErpKingdeeSyncRunStatusEnum.FAILED.getStatus());
        run.setEndedAt(LocalDateTime.now());
        run.setFailureMessage(ex.getMessage());
        syncRunMapper.updateById(run);
    }

    private void updateWatermark(ErpKingdeeSyncWatermarkDO watermark, String syncType, LocalDateTime watermarkTime) {
        if (watermarkTime == null) {
            throw new IllegalStateException("ERP Kingdee sync success watermark is required: " + syncType);
        }
        if (watermark == null) {
            syncWatermarkMapper.insert(ErpKingdeeSyncWatermarkDO.builder()
                    .syncType(syncType)
                    .lastSuccessTime(watermarkTime)
                    .build());
            return;
        }
        watermark.setLastSuccessTime(watermarkTime);
        syncWatermarkMapper.updateById(watermark);
    }

}