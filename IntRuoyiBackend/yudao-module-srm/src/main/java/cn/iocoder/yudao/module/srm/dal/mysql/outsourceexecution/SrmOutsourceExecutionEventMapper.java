package cn.iocoder.yudao.module.srm.dal.mysql.outsourceexecution;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.srm.dal.dataobject.outsourceexecution.SrmOutsourceExecutionEventDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SrmOutsourceExecutionEventMapper extends BaseMapperX<SrmOutsourceExecutionEventDO> {

    default List<SrmOutsourceExecutionEventDO> selectListByExecutionId(Long executionId) {
        return selectList(new LambdaQueryWrapperX<SrmOutsourceExecutionEventDO>()
                .eq(SrmOutsourceExecutionEventDO::getExecutionId, executionId)
                .orderByAsc(SrmOutsourceExecutionEventDO::getId));
    }
}
