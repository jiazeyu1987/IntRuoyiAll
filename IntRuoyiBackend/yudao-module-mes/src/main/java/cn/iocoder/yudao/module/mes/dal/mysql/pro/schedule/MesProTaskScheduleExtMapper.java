package cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProTaskScheduleExtDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface MesProTaskScheduleExtMapper extends BaseMapperX<MesProTaskScheduleExtDO> {

    default List<MesProTaskScheduleExtDO> selectListByTaskIds(Collection<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProTaskScheduleExtDO>()
                .in(MesProTaskScheduleExtDO::getTaskId, taskIds));
    }

    default MesProTaskScheduleExtDO selectByTaskId(Long taskId) {
        return selectOne(MesProTaskScheduleExtDO::getTaskId, taskId);
    }

    default List<MesProTaskScheduleExtDO> selectListByScheduleOrderProcessIds(Collection<Long> scheduleOrderProcessIds) {
        if (scheduleOrderProcessIds == null || scheduleOrderProcessIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProTaskScheduleExtDO>()
                .in(MesProTaskScheduleExtDO::getScheduleOrderProcessId, scheduleOrderProcessIds));
    }

    default void deleteByTaskIds(Collection<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return;
        }
        delete(new LambdaQueryWrapperX<MesProTaskScheduleExtDO>()
                .in(MesProTaskScheduleExtDO::getTaskId, taskIds));
    }

}
