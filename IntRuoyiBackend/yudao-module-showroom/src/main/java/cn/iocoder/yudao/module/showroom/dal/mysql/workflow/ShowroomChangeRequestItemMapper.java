package cn.iocoder.yudao.module.showroom.dal.mysql.workflow;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.showroom.dal.dataobject.workflow.ShowroomChangeRequestItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ShowroomChangeRequestItemMapper extends BaseMapperX<ShowroomChangeRequestItemDO> {

    default List<ShowroomChangeRequestItemDO> selectListByChangeRequestId(Long changeRequestId) {
        return selectList(new LambdaQueryWrapperX<ShowroomChangeRequestItemDO>()
                .eq(ShowroomChangeRequestItemDO::getChangeRequestId, changeRequestId)
                .orderByAsc(ShowroomChangeRequestItemDO::getId));
    }

}
