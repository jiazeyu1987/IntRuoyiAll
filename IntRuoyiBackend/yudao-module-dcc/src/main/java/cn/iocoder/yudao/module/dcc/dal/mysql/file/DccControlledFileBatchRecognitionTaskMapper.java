package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileBatchRecognitionTaskDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface DccControlledFileBatchRecognitionTaskMapper extends BaseMapperX<DccControlledFileBatchRecognitionTaskDO> {

    default DccControlledFileBatchRecognitionTaskDO selectActiveTask(String recognitionType) {
        return selectOne(new LambdaQueryWrapperX<DccControlledFileBatchRecognitionTaskDO>()
                .eq(DccControlledFileBatchRecognitionTaskDO::getRecognitionType, recognitionType)
                .in(DccControlledFileBatchRecognitionTaskDO::getStatus, "WAITING", "RUNNING")
                .orderByDesc(DccControlledFileBatchRecognitionTaskDO::getId)
                .last("LIMIT 1"));
    }

    default DccControlledFileBatchRecognitionTaskDO selectLatestTask(Long operatorUserId, String recognitionType) {
        return selectOne(new LambdaQueryWrapperX<DccControlledFileBatchRecognitionTaskDO>()
                .eq(DccControlledFileBatchRecognitionTaskDO::getOperatorUserId, operatorUserId)
                .eq(DccControlledFileBatchRecognitionTaskDO::getRecognitionType, recognitionType)
                .orderByDesc(DccControlledFileBatchRecognitionTaskDO::getId)
                .last("LIMIT 1"));
    }

    default DccControlledFileBatchRecognitionTaskDO selectLatestTask(String recognitionType) {
        return selectOne(new LambdaQueryWrapperX<DccControlledFileBatchRecognitionTaskDO>()
                .eq(DccControlledFileBatchRecognitionTaskDO::getRecognitionType, recognitionType)
                .orderByDesc(DccControlledFileBatchRecognitionTaskDO::getId)
                .last("LIMIT 1"));
    }

    default List<DccControlledFileBatchRecognitionTaskDO> selectWaitingTasks() {
        return selectList(new LambdaQueryWrapperX<DccControlledFileBatchRecognitionTaskDO>()
                .eq(DccControlledFileBatchRecognitionTaskDO::getStatus, "WAITING")
                .orderByAsc(DccControlledFileBatchRecognitionTaskDO::getId));
    }

    default List<DccControlledFileBatchRecognitionTaskDO> selectRunningTasks() {
        return selectList(new LambdaQueryWrapperX<DccControlledFileBatchRecognitionTaskDO>()
                .eq(DccControlledFileBatchRecognitionTaskDO::getStatus, "RUNNING")
                .orderByAsc(DccControlledFileBatchRecognitionTaskDO::getId));
    }

    default int claimWaitingTask(Long taskId, LocalDateTime startedAt) {
        return update(null, new LambdaUpdateWrapper<DccControlledFileBatchRecognitionTaskDO>()
                .eq(DccControlledFileBatchRecognitionTaskDO::getId, taskId)
                .eq(DccControlledFileBatchRecognitionTaskDO::getStatus, "WAITING")
                .set(DccControlledFileBatchRecognitionTaskDO::getStatus, "RUNNING")
                .set(DccControlledFileBatchRecognitionTaskDO::getStartedAt, startedAt));
    }

    default int requeueRunningTasksOnStartup() {
        return update(null, new LambdaUpdateWrapper<DccControlledFileBatchRecognitionTaskDO>()
                .eq(DccControlledFileBatchRecognitionTaskDO::getStatus, "RUNNING")
                .set(DccControlledFileBatchRecognitionTaskDO::getStatus, "WAITING")
                .set(DccControlledFileBatchRecognitionTaskDO::getProcessedCount, 0L)
                .set(DccControlledFileBatchRecognitionTaskDO::getSuccessCount, 0L)
                .set(DccControlledFileBatchRecognitionTaskDO::getFailedCount, 0L)
                .set(DccControlledFileBatchRecognitionTaskDO::getSkippedExistingCount, 0L)
                .set(DccControlledFileBatchRecognitionTaskDO::getUnclassifiedCount, 0L)
                .set(DccControlledFileBatchRecognitionTaskDO::getAmbiguousCount, 0L)
                .set(DccControlledFileBatchRecognitionTaskDO::getConflictCount, 0L)
                .setSql("remaining_count = total_count")
                .set(DccControlledFileBatchRecognitionTaskDO::getStartedAt, null)
                .set(DccControlledFileBatchRecognitionTaskDO::getCompletedAt, null)
                .set(DccControlledFileBatchRecognitionTaskDO::getLastFailureMessage, null));
    }

    default int stopActiveTask(Long taskId, Long operatorUserId, LocalDateTime completedAt, String reason) {
        return update(null, new LambdaUpdateWrapper<DccControlledFileBatchRecognitionTaskDO>()
                .eq(DccControlledFileBatchRecognitionTaskDO::getId, taskId)
                .eq(DccControlledFileBatchRecognitionTaskDO::getOperatorUserId, operatorUserId)
                .in(DccControlledFileBatchRecognitionTaskDO::getStatus, "WAITING", "RUNNING")
                .set(DccControlledFileBatchRecognitionTaskDO::getStatus, "STOPPED")
                .set(DccControlledFileBatchRecognitionTaskDO::getCompletedAt, completedAt)
                .set(DccControlledFileBatchRecognitionTaskDO::getLastFailureMessage, reason));
    }
}
