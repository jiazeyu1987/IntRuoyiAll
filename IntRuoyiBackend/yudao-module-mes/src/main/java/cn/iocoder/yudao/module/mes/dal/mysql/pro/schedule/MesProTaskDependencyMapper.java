package cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProTaskDependencyDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface MesProTaskDependencyMapper extends BaseMapperX<MesProTaskDependencyDO> {

    default List<MesProTaskDependencyDO> selectListByTaskIds(Collection<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProTaskDependencyDO>()
                .and(wrapper -> wrapper.in(MesProTaskDependencyDO::getSourceTaskId, taskIds)
                        .or()
                        .in(MesProTaskDependencyDO::getTargetTaskId, taskIds))
                .eq(MesProTaskDependencyDO::getEnabled, Boolean.TRUE));
    }

    default void deleteByTaskIds(Collection<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return;
        }
        delete(new LambdaQueryWrapperX<MesProTaskDependencyDO>()
                .and(wrapper -> wrapper.in(MesProTaskDependencyDO::getSourceTaskId, taskIds)
                        .or()
                        .in(MesProTaskDependencyDO::getTargetTaskId, taskIds)));
    }

}
