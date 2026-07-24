package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileNasTransferTaskDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface DccControlledFileNasTransferTaskMapper extends BaseMapperX<DccControlledFileNasTransferTaskDO> {

    default DccControlledFileNasTransferTaskDO selectActiveTask() {
        return selectOne(new LambdaQueryWrapperX<DccControlledFileNasTransferTaskDO>()
                .in(DccControlledFileNasTransferTaskDO::getStatus, "UPLOADING", "WAITING", "RUNNING", "CANCELLING")
                .orderByDesc(DccControlledFileNasTransferTaskDO::getId)
                .last("LIMIT 1"));
    }

    default List<DccControlledFileNasTransferTaskDO> selectActiveTasksForDirectoryDeletion() {
        return selectList(new LambdaQueryWrapperX<DccControlledFileNasTransferTaskDO>()
                .in(DccControlledFileNasTransferTaskDO::getStatus, "UPLOADING", "WAITING", "RUNNING", "CANCELLING")
                .orderByDesc(DccControlledFileNasTransferTaskDO::getId));
    }

    default List<DccControlledFileNasTransferTaskDO> selectWaitingTasks(LocalDateTime now) {
        return selectList(new LambdaQueryWrapperX<DccControlledFileNasTransferTaskDO>()
                .eq(DccControlledFileNasTransferTaskDO::getStatus, "WAITING")
                .and(wrapper -> wrapper.isNull(DccControlledFileNasTransferTaskDO::getNextCheckAt)
                        .or().le(DccControlledFileNasTransferTaskDO::getNextCheckAt, now))
                .orderByAsc(DccControlledFileNasTransferTaskDO::getId));
    }

    default List<DccControlledFileNasTransferTaskDO> selectUnfinishedTasks() {
        return selectList(new LambdaQueryWrapperX<DccControlledFileNasTransferTaskDO>()
                .in(DccControlledFileNasTransferTaskDO::getStatus, "UPLOADING", "WAITING", "RUNNING", "CANCELLING")
                .orderByAsc(DccControlledFileNasTransferTaskDO::getId));
    }

    default int claimWaitingTask(Long taskId, LocalDateTime lastRunAt) {
        return update(null, new LambdaUpdateWrapper<DccControlledFileNasTransferTaskDO>()
                .eq(DccControlledFileNasTransferTaskDO::getId, taskId)
                .eq(DccControlledFileNasTransferTaskDO::getStatus, "WAITING")
                .set(DccControlledFileNasTransferTaskDO::getStatus, "RUNNING")
                .set(DccControlledFileNasTransferTaskDO::getNextCheckAt, null)
                .set(DccControlledFileNasTransferTaskDO::getLastRunAt, lastRunAt));
    }

    default int recoverRunningTasksToWaiting(LocalDateTime nextCheckAt) {
        return update(null, new LambdaUpdateWrapper<DccControlledFileNasTransferTaskDO>()
                .eq(DccControlledFileNasTransferTaskDO::getStatus, "RUNNING")
                .set(DccControlledFileNasTransferTaskDO::getStatus, "WAITING")
                .set(DccControlledFileNasTransferTaskDO::getNextCheckAt, nextCheckAt));
    }

    default int requestCancelRunningTask(Long taskId, String reason) {
        return update(null, new LambdaUpdateWrapper<DccControlledFileNasTransferTaskDO>()
                .eq(DccControlledFileNasTransferTaskDO::getId, taskId)
                .eq(DccControlledFileNasTransferTaskDO::getStatus, "RUNNING")
                .set(DccControlledFileNasTransferTaskDO::getStatus, "CANCELLING")
                .set(DccControlledFileNasTransferTaskDO::getLastFailureMessage, reason));
    }

    default int cancelWaitingOrCancellingTask(Long taskId, LocalDateTime completedAt, String reason) {
        return update(null, new LambdaUpdateWrapper<DccControlledFileNasTransferTaskDO>()
                .eq(DccControlledFileNasTransferTaskDO::getId, taskId)
                .in(DccControlledFileNasTransferTaskDO::getStatus, "UPLOADING", "WAITING", "CANCELLING")
                .set(DccControlledFileNasTransferTaskDO::getStatus, "CANCELLED")
                .set(DccControlledFileNasTransferTaskDO::getCompletedAt, completedAt)
                .set(DccControlledFileNasTransferTaskDO::getNextCheckAt, null)
                .set(DccControlledFileNasTransferTaskDO::getLastFailureMessage, reason));
    }
}
