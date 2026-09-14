package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesProcessPoolActiveOrderPickListBindingItemMapper
        extends BaseMapperX<MesProcessPoolActiveOrderPickListBindingItemDO> {

    default List<MesProcessPoolActiveOrderPickListBindingItemDO> selectListByBindingId(Long bindingId) {
        return selectList(new LambdaQueryWrapperX<MesProcessPoolActiveOrderPickListBindingItemDO>()
                .eq(MesProcessPoolActiveOrderPickListBindingItemDO::getBindingId, bindingId)
                .orderByAsc(MesProcessPoolActiveOrderPickListBindingItemDO::getSourceEntryId)
                .orderByAsc(MesProcessPoolActiveOrderPickListBindingItemDO::getId));
    }
}
