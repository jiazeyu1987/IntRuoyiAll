package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesProcessPoolActiveOrderPickListBindingMapper
        extends BaseMapperX<MesProcessPoolActiveOrderPickListBindingDO> {

    default List<MesProcessPoolActiveOrderPickListBindingDO> selectListByActiveOrderId(Long activeOrderId) {
        return selectList(new LambdaQueryWrapperX<MesProcessPoolActiveOrderPickListBindingDO>()
                .eq(MesProcessPoolActiveOrderPickListBindingDO::getActiveOrderId, activeOrderId)
                .orderByAsc(MesProcessPoolActiveOrderPickListBindingDO::getBoundAt)
                .orderByAsc(MesProcessPoolActiveOrderPickListBindingDO::getId));
    }

    default MesProcessPoolActiveOrderPickListBindingDO selectByActiveOrderId(Long activeOrderId) {
        return selectOne(new LambdaQueryWrapperX<MesProcessPoolActiveOrderPickListBindingDO>()
                .eq(MesProcessPoolActiveOrderPickListBindingDO::getActiveOrderId, activeOrderId)
                .orderByAsc(MesProcessPoolActiveOrderPickListBindingDO::getBoundAt)
                .orderByAsc(MesProcessPoolActiveOrderPickListBindingDO::getId)
                .last("LIMIT 1"));
    }

    default MesProcessPoolActiveOrderPickListBindingDO selectByActiveOrderIdAndPickListId(Long activeOrderId,
                                                                                          Long pickListId) {
        return selectOne(new LambdaQueryWrapperX<MesProcessPoolActiveOrderPickListBindingDO>()
                .eq(MesProcessPoolActiveOrderPickListBindingDO::getActiveOrderId, activeOrderId)
                .eq(MesProcessPoolActiveOrderPickListBindingDO::getPickListId, pickListId));
    }

    default MesProcessPoolActiveOrderPickListBindingDO selectByIdempotencyKey(String idempotencyKey) {
        return selectOne(new LambdaQueryWrapperX<MesProcessPoolActiveOrderPickListBindingDO>()
                .eq(MesProcessPoolActiveOrderPickListBindingDO::getIdempotencyKey, idempotencyKey));
    }
}
