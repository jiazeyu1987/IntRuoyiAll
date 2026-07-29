package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProProcessPoolPqcRecordMapper extends BaseMapperX<MesProProcessPoolPqcRecordDO> {

    default MesProProcessPoolPqcRecordDO selectByEventId(Long eventId) {
        return selectOne(new LambdaQueryWrapperX<MesProProcessPoolPqcRecordDO>()
                .eq(MesProProcessPoolPqcRecordDO::getEventId, eventId));
    }
}
