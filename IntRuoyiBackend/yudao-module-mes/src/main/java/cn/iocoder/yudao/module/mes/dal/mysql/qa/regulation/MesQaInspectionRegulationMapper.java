package cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import org.apache.ibatis.annotations.Mapper;

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
}
