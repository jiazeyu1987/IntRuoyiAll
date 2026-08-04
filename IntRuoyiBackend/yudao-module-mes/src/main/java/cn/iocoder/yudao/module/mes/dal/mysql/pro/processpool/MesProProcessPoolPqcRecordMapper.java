package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesProProcessPoolPqcRecordMapper extends BaseMapperX<MesProProcessPoolPqcRecordDO> {

    default MesProProcessPoolPqcRecordDO selectByEventId(Long eventId) {
        return selectOne(new LambdaQueryWrapperX<MesProProcessPoolPqcRecordDO>()
                .eq(MesProProcessPoolPqcRecordDO::getEventId, eventId));
    }

    default List<MesProProcessPoolPqcRecordDO> selectListByProductionSubmitEventId(Long productionSubmitEventId) {
        return selectList(new LambdaQueryWrapperX<MesProProcessPoolPqcRecordDO>()
                .eq(MesProProcessPoolPqcRecordDO::getProductionSubmitEventId, productionSubmitEventId)
                .orderByAsc(MesProProcessPoolPqcRecordDO::getId));
    }
}
