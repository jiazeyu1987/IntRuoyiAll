package cn.iocoder.yudao.module.showroom.dal.mysql.cover;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.showroom.dal.dataobject.cover.ShowroomProductCoverBatchTaskItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ShowroomProductCoverBatchTaskItemMapper extends BaseMapperX<ShowroomProductCoverBatchTaskItemDO> {

    default List<ShowroomProductCoverBatchTaskItemDO> selectListByTaskId(Long taskId) {
        return selectList(new LambdaQueryWrapperX<ShowroomProductCoverBatchTaskItemDO>()
                .eq(ShowroomProductCoverBatchTaskItemDO::getTaskId, taskId)
                .orderByAsc(ShowroomProductCoverBatchTaskItemDO::getId));
    }

    default List<ShowroomProductCoverBatchTaskItemDO> selectWaitingItemsByTaskId(Long taskId) {
        return selectList(new LambdaQueryWrapperX<ShowroomProductCoverBatchTaskItemDO>()
                .eq(ShowroomProductCoverBatchTaskItemDO::getTaskId, taskId)
                .eq(ShowroomProductCoverBatchTaskItemDO::getStatus, "WAITING")
                .orderByAsc(ShowroomProductCoverBatchTaskItemDO::getId));
    }

    default List<ShowroomProductCoverBatchTaskItemDO> selectRunningItemsByTaskId(Long taskId) {
        return selectList(new LambdaQueryWrapperX<ShowroomProductCoverBatchTaskItemDO>()
                .eq(ShowroomProductCoverBatchTaskItemDO::getTaskId, taskId)
                .eq(ShowroomProductCoverBatchTaskItemDO::getStatus, "RUNNING")
                .orderByAsc(ShowroomProductCoverBatchTaskItemDO::getId));
    }

    default int recoverRunningItemsToWaiting(Long taskId) {
        return update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ShowroomProductCoverBatchTaskItemDO>()
                .eq(ShowroomProductCoverBatchTaskItemDO::getTaskId, taskId)
                .eq(ShowroomProductCoverBatchTaskItemDO::getStatus, "RUNNING")
                .set(ShowroomProductCoverBatchTaskItemDO::getStatus, "WAITING")
                .set(ShowroomProductCoverBatchTaskItemDO::getCompletedAt, null));
    }
}
