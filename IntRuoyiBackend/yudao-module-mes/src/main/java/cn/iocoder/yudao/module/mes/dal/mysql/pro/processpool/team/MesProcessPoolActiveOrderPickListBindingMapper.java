package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProcessPoolActiveOrderPickListBindingMapper
        extends BaseMapperX<MesProcessPoolActiveOrderPickListBindingDO> {

    default MesProcessPoolActiveOrderPickListBindingDO selectByActiveOrderId(Long activeOrderId) {
        return selectOne(new LambdaQueryWrapperX<MesProcessPoolActiveOrderPickListBindingDO>()
                .eq(MesProcessPoolActiveOrderPickListBindingDO::getActiveOrderId, activeOrderId));
    }

    default MesProcessPoolActiveOrderPickListBindingDO selectByIdempotencyKey(String idempotencyKey) {
        return selectOne(new LambdaQueryWrapperX<MesProcessPoolActiveOrderPickListBindingDO>()
                .eq(MesProcessPoolActiveOrderPickListBindingDO::getIdempotencyKey, idempotencyKey));
    }
}
