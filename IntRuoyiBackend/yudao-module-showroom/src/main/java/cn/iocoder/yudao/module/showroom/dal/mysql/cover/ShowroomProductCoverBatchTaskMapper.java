package cn.iocoder.yudao.module.showroom.dal.mysql.cover;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.showroom.dal.dataobject.cover.ShowroomProductCoverBatchTaskDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ShowroomProductCoverBatchTaskMapper extends BaseMapperX<ShowroomProductCoverBatchTaskDO> {

    default ShowroomProductCoverBatchTaskDO selectActiveTask() {
        return selectOne(new LambdaQueryWrapperX<ShowroomProductCoverBatchTaskDO>()
                .in(ShowroomProductCoverBatchTaskDO::getStatus, "WAITING", "RUNNING")
                .orderByDesc(ShowroomProductCoverBatchTaskDO::getId)
                .last("LIMIT 1"));
    }

    default ShowroomProductCoverBatchTaskDO selectLatestTask() {
        return selectOne(new LambdaQueryWrapperX<ShowroomProductCoverBatchTaskDO>()
                .orderByDesc(ShowroomProductCoverBatchTaskDO::getId)
                .last("LIMIT 1"));
    }

    default List<ShowroomProductCoverBatchTaskDO> selectWaitingTasks() {
        return selectList(new LambdaQueryWrapperX<ShowroomProductCoverBatchTaskDO>()
                .eq(ShowroomProductCoverBatchTaskDO::getStatus, "WAITING")
                .orderByAsc(ShowroomProductCoverBatchTaskDO::getId));
    }

    default List<ShowroomProductCoverBatchTaskDO> selectUnfinishedTasks() {
        return selectList(new LambdaQueryWrapperX<ShowroomProductCoverBatchTaskDO>()
                .in(ShowroomProductCoverBatchTaskDO::getStatus, "WAITING", "RUNNING")
                .orderByAsc(ShowroomProductCoverBatchTaskDO::getId));
    }

    default int claimWaitingTask(Long taskId) {
        return update(null, new LambdaUpdateWrapper<ShowroomProductCoverBatchTaskDO>()
                .eq(ShowroomProductCoverBatchTaskDO::getId, taskId)
                .eq(ShowroomProductCoverBatchTaskDO::getStatus, "WAITING")
                .set(ShowroomProductCoverBatchTaskDO::getStatus, "RUNNING")
                .set(ShowroomProductCoverBatchTaskDO::getNextCheckAt, null));
    }

    default int recoverRunningTasksToWaiting(LocalDateTime nextCheckAt) {
        return update(null, new LambdaUpdateWrapper<ShowroomProductCoverBatchTaskDO>()
                .eq(ShowroomProductCoverBatchTaskDO::getStatus, "RUNNING")
                .set(ShowroomProductCoverBatchTaskDO::getStatus, "WAITING")
                .set(ShowroomProductCoverBatchTaskDO::getNextCheckAt, nextCheckAt));
    }
}
