package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDeliveryGateItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesProEdhrDeliveryGateItemMapper extends BaseMapperX<MesProEdhrDeliveryGateItemDO> {

    default List<MesProEdhrDeliveryGateItemDO> selectListByProjectId(Long projectId) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrDeliveryGateItemDO>()
                .eq(MesProEdhrDeliveryGateItemDO::getProjectId, projectId)
                .orderByAsc(MesProEdhrDeliveryGateItemDO::getSort)
                .orderByAsc(MesProEdhrDeliveryGateItemDO::getId));
    }
}
