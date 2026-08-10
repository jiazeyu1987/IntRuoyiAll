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
        return selectOne(baseRouteProcessQuery(productId, routeId, routeVersionId, routeProcessId, processId)
                .eq(MesQaInspectionRegulationDO::getLifecycleStatus, "PUBLISHED"));
    }

    default MesQaInspectionRegulationDO selectByRouteProcess(Long productId, Long routeId,
                                                             Long routeVersionId, Long routeProcessId,
                                                             Long processId) {
        return selectOne(baseRouteProcessQuery(productId, routeId, routeVersionId, routeProcessId, processId));
    }

    default List<MesQaInspectionRegulationDO> selectPublishedListByProductRouteVersion(Long productId, Long routeId,
                                                                                       Long routeVersionId) {
        return selectList(new LambdaQueryWrapperX<MesQaInspectionRegulationDO>()
                .eq(MesQaInspectionRegulationDO::getProductId, productId)
                .eq(MesQaInspectionRegulationDO::getRouteId, routeId)
                .eq(MesQaInspectionRegulationDO::getRouteVersionId, routeVersionId)
                .eq(MesQaInspectionRegulationDO::getOwnerModule,
                        MesQaInspectionRegulationDO.OWNER_MODULE_MES_QA)
                .eq(MesQaInspectionRegulationDO::getLifecycleStatus, "PUBLISHED")
                .isNotNull(MesQaInspectionRegulationDO::getCurrentVersionId)
                .orderByAsc(MesQaInspectionRegulationDO::getRouteProcessId)
                .orderByAsc(MesQaInspectionRegulationDO::getProcessId)
                .orderByDesc(MesQaInspectionRegulationDO::getCurrentVersionId)
                .orderByDesc(MesQaInspectionRegulationDO::getId));
    }

    private static LambdaQueryWrapperX<MesQaInspectionRegulationDO> baseRouteProcessQuery(Long productId,
                                                                                          Long routeId,
                                                                                          Long routeVersionId,
                                                                                          Long routeProcessId,
                                                                                          Long processId) {
        return new LambdaQueryWrapperX<MesQaInspectionRegulationDO>()
                .eq(MesQaInspectionRegulationDO::getProductId, productId)
                .eq(MesQaInspectionRegulationDO::getRouteId, routeId)
                .eq(MesQaInspectionRegulationDO::getRouteVersionId, routeVersionId)
                .eq(MesQaInspectionRegulationDO::getRouteProcessId, routeProcessId)
                .eq(MesQaInspectionRegulationDO::getProcessId, processId)
                .eq(MesQaInspectionRegulationDO::getOwnerModule,
                        MesQaInspectionRegulationDO.OWNER_MODULE_MES_QA);
    }

    default List<MesQaInspectionRegulationDO> selectListByProductIds(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesQaInspectionRegulationDO>()
                .in(MesQaInspectionRegulationDO::getProductId, productIds)
                .eq(MesQaInspectionRegulationDO::getOwnerModule,
                        MesQaInspectionRegulationDO.OWNER_MODULE_MES_QA)
                .orderByAsc(MesQaInspectionRegulationDO::getProductId)
                .orderByDesc(MesQaInspectionRegulationDO::getCurrentVersionId)
                .orderByDesc(MesQaInspectionRegulationDO::getId));
    }
}
