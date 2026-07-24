package cn.iocoder.yudao.module.dcc.dal.mysql.position;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.position.DccPositionAssignmentDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * DCC position assignment mapper.
 */
@Mapper
public interface DccPositionAssignmentMapper extends BaseMapperX<DccPositionAssignmentDO> {

    default List<DccPositionAssignmentDO> selectActiveListByPositionId(Long positionId) {
        return selectList(DccPositionAssignmentDO::getPositionId, positionId,
                DccPositionAssignmentDO::getActive, Boolean.TRUE);
    }
}
