package cn.iocoder.yudao.module.showroom.dal.mysql.workflow;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.showroom.dal.dataobject.workflow.ShowroomChangeRequestDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ShowroomChangeRequestMapper extends BaseMapperX<ShowroomChangeRequestDO> {

    default List<ShowroomChangeRequestDO> selectListByTarget(String targetType, Long targetId) {
        return selectList(new LambdaQueryWrapperX<ShowroomChangeRequestDO>()
                .eq(ShowroomChangeRequestDO::getTargetType, targetType)
                .eq(ShowroomChangeRequestDO::getTargetId, targetId)
                .orderByDesc(ShowroomChangeRequestDO::getSubmittedAt)
                .orderByDesc(ShowroomChangeRequestDO::getId));
    }

    default List<ShowroomChangeRequestDO> selectListOrdered() {
        return selectList(new LambdaQueryWrapperX<ShowroomChangeRequestDO>()
                .orderByDesc(ShowroomChangeRequestDO::getSubmittedAt)
                .orderByDesc(ShowroomChangeRequestDO::getId));
    }

    default ShowroomChangeRequestDO selectByProcessInstanceId(String processInstanceId) {
        return selectOne(new LambdaQueryWrapperX<ShowroomChangeRequestDO>()
                .eq(ShowroomChangeRequestDO::getProcessInstanceId, processInstanceId)
                .last("LIMIT 1"));
    }

}
