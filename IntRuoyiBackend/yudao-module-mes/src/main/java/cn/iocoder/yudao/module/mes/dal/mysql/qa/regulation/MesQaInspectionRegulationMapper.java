package cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface MesQaInspectionRegulationMapper extends BaseMapperX<MesQaInspectionRegulationDO> {

    default MesQaInspectionRegulationDO selectPublishedByRouteProcess(Long productId, Long routeId,
                                                                      Long routeVersionId, Long routeProcessId,
                                                                      Long processId) {
        return selectOne(new LambdaQueryWrapperX<MesQaInspectionRegulationDO>()
                .eq(MesQaInspectionRegulationDO::getProductId, productId)
                .eq(MesQaInspectionRegulationDO::getRouteId, routeId)
                .eq(MesQaInspectionRegulationDO::getRouteVersionId, routeVersionId)
                .eq(MesQaInspectionRegulationDO::getRouteProcessId, routeProcessId)
                .eq(MesQaInspectionRegulationDO::getProcessId, processId)
                .eq(MesQaInspectionRegulationDO::getLifecycleStatus, "PUBLISHED"));
    }

    default List<MesQaInspectionRegulationDO> selectListByProductIds(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesQaInspectionRegulationDO>()
                .in(MesQaInspectionRegulationDO::getProductId, productIds)
                .orderByAsc(MesQaInspectionRegulationDO::getProductId)
                .orderByDesc(MesQaInspectionRegulationDO::getCurrentVersionId)
                .orderByDesc(MesQaInspectionRegulationDO::getId));
    }
}
