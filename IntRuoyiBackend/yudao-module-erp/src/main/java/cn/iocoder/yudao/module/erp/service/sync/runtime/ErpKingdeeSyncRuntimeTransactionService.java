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

    /** Caller must own both source and execution locks; a previous owner can no longer write. */
    @Transactional(rollbackFor = Exception.class)
    public void recoverInterruptedAfterLock(String syncType) {
        if (runMapper.selectRunningBySyncType(syncType) == null) return;
        runMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ErpKingdeeSyncRunDO>()
                .eq(ErpKingdeeSyncRunDO::getSyncType, syncType)
                .in(ErpKingdeeSyncRunDO::getStatus, 10, 40)
                .set(ErpKingdeeSyncRunDO::getStatus, ErpKingdeeSyncRunStatusEnum.FAILED.getStatus())
                .set(ErpKingdeeSyncRunDO::getEndedAt, LocalDateTime.now())
                .set(ErpKingdeeSyncRunDO::getFailureMessage,
                        "旧执行已中断：已取得来源及同步执行锁，保留已提交计数和原水位，开始重试"));
    }
    /** Commits ordinary business imports, their result and watermark together. */
    @Transactional(rollbackFor = Exception.class)
    public ErpKingdeeSyncRunResult execute(ErpKingdeeSyncExecution execution,
                                          ErpKingdeeSyncTask task,
                                          ErpKingdeeSyncCancellation cancellation) {
        ErpKingdeeSyncRunResult result = execution.run(task);
        cancellation.check(execution.getRunId());
        complete(execution, result);
        return result;
    }
    @Transactional(rollbackFor = Exception.class)
    public ErpKingdeeSyncExecution start(ErpKingdeeSyncCommand command) {
        ErpKingdeeSyncRunDO running = runMapper.selectRunningBySyncType(command.getSyncType().getType());
        if (running != null) {
            throw new IllegalStateException("Kingdee sync task already running: " + command.getSyncType().name());
        }
        LocalDateTime now = LocalDateTime.now();
        boolean targeted = command.getTargetCode() != null;
        ErpKingdeeSyncWatermarkDO watermark = targeted ? null
                : watermarkMapper.selectBySyncType(command.getSyncType().getType());
        boolean initialSync = !targeted && (watermark == null || command.isForceInitialWindowStart());
        LocalDateTime windowStart = targeted ? null
                : initialSync ? resolveInitialWindowStart(command) : watermark.getLastSuccessTime();
        ErpKingdeeSyncRunDO run = ErpKingdeeSyncRunDO.builder()
                .syncType(command.getSyncType().getType())
                .triggerType(command.getTriggerType().getTriggerType())
                .targetCode(command.getTargetCode())
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
                .runId(run.getId())
                .syncType(command.getSyncType())
                .triggerType(command.getTriggerType())
                .targetCode(command.getTargetCode())
                .initialSync(initialSync)
                .windowStart(windowStart)
                .windowEnd(command.getWindowEnd())
                .build());
    }

    @Transactional(rollbackFor = Exception.class)
    public void complete(ErpKingdeeSyncExecution execution, ErpKingdeeSyncRunResult result) {
        if (execution.getContext().getTargetCode() != null && result.getWatermarkTime() != null) {
            throw new IllegalStateException("按编号同步不得推进增量同步水位");
        }
        if (result.getFailedCount() > 0) {
            throw new IllegalStateException("同步结果包含失败记录，禁止推进水位");
        }
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
        ErpKingdeeSyncRunResult result = execution.getContext().getTargetCode() == null ? null
                : ErpKingdeeSyncRunResult.builder().failedCount(1).build();
        if (failure instanceof ErpKingdeeOrderBatchSyncFailure batch) {
            ErpKingdeeSyncRunDO update = new ErpKingdeeSyncRunDO();
            update.setId(execution.getRunId());
            update.setStatus(ErpKingdeeSyncRunStatusEnum.FAILED.getStatus());
            update.setEndedAt(LocalDateTime.now());
            update.setFailedCount(batch.getFailedCount());
            update.setFailureMessage(truncateFailureMessage(failure.getMessage()));
            if (runMapper.update(update, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ErpKingdeeSyncRunDO>()
                    .eq(ErpKingdeeSyncRunDO::getId, execution.getRunId())
                    .in(ErpKingdeeSyncRunDO::getStatus, 10, 40)) != 1) {
                throw new IllegalStateException("失败记录状态已变化，未覆盖终态");
            }
            return;
        }
        if (failure instanceof ErpKingdeeOrderSyncFailure) {
            // These order imports are one transaction: none of the attempted writes committed.
            result = ErpKingdeeSyncRunResult.builder().createdCount(0).updatedCount(0)
                    .skippedCount(0).failedCount(1).build();
        }
        updateRun(execution.getRunId(), ErpKingdeeSyncRunStatusEnum.FAILED.getStatus(), result,
                failure.getMessage(), execution.getContext().getWindowEnd());
    }

    /** Must share the page transaction so progress never precedes committed business data. */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY,
            rollbackFor = Exception.class)
    public void recordProductionMaterialListProgress(Long runId, ErpKingdeeSyncRunResult page) {
        if (runId == null || runMapper.addProductionMaterialListProgress(runId, page.getCreatedCount(),
                page.getUpdatedCount(), page.getSkippedCount()) != 1) {
            throw new IllegalStateException("生产用料清单任务已结束或不存在，当前批次未提交");
        }
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY, rollbackFor = Exception.class)
    public void recordOrderProgress(Long runId, ErpKingdeeSyncRunResult progress) {
        if (runId == null || runMapper.addOrderProgress(runId, progress.getCreatedCount(),
                progress.getUpdatedCount(), progress.getSkippedCount()) != 1) {
            throw new IllegalStateException("采购销售任务已结束或不存在，当前订单未提交");
        }
    }

    private LocalDateTime resolveInitialWindowStart(ErpKingdeeSyncCommand command) {
        if (command.getInitialWindowStart() != null) {
            return command.getInitialWindowStart();
        }
        return LocalDateTime.of(1970, 1, 1, 0, 0);
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
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ErpKingdeeSyncRunDO> guard =
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ErpKingdeeSyncRunDO>()
                        .eq(ErpKingdeeSyncRunDO::getId, runId);
        if (status.equals(ErpKingdeeSyncRunStatusEnum.SUCCESS.getStatus())) {
            guard.eq(ErpKingdeeSyncRunDO::getStatus, ErpKingdeeSyncRunStatusEnum.RUNNING.getStatus());
        } else {
            guard.in(ErpKingdeeSyncRunDO::getStatus, 10, 40);
        }
        if (runMapper.update(update, guard) != 1) {
            throw new java.util.concurrent.CancellationException("同步状态已变化，当前结果未提交");
        }
    }

    private String truncateFailureMessage(String failureMessage) {
        if (failureMessage == null || failureMessage.length() <= FAILURE_MESSAGE_MAX_LENGTH) {
            return failureMessage;
        }
        return failureMessage.substring(0, FAILURE_MESSAGE_MAX_LENGTH);
    }

}
