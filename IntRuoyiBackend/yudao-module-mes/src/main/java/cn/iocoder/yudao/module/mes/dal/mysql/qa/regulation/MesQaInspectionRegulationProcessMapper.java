package cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationProcessDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface MesQaInspectionRegulationProcessMapper
        extends BaseMapperX<MesQaInspectionRegulationProcessDO> {

    default List<MesQaInspectionRegulationProcessDO> selectListByVersionId(Long regulationVersionId) {
        return selectList(new LambdaQueryWrapperX<MesQaInspectionRegulationProcessDO>()
                .eq(MesQaInspectionRegulationProcessDO::getRegulationVersionId, regulationVersionId)
                .orderByAsc(MesQaInspectionRegulationProcessDO::getSort)
                .orderByAsc(MesQaInspectionRegulationProcessDO::getId));
    }

    default List<MesQaInspectionRegulationProcessDO> selectListByVersionIds(Collection<Long> regulationVersionIds) {
        if (regulationVersionIds == null || regulationVersionIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesQaInspectionRegulationProcessDO>()
                .in(MesQaInspectionRegulationProcessDO::getRegulationVersionId, regulationVersionIds)
                .orderByAsc(MesQaInspectionRegulationProcessDO::getRegulationVersionId)
                .orderByAsc(MesQaInspectionRegulationProcessDO::getSort)
                .orderByAsc(MesQaInspectionRegulationProcessDO::getId));
    }

    default Long selectCountByVersionIds(Collection<Long> regulationVersionIds) {
        if (regulationVersionIds == null || regulationVersionIds.isEmpty()) {
            return 0L;
        }
        return selectCount(new LambdaQueryWrapperX<MesQaInspectionRegulationProcessDO>()
                .in(MesQaInspectionRegulationProcessDO::getRegulationVersionId, regulationVersionIds));
    }

    default int deleteByVersionId(Long regulationVersionId) {
        return delete(new LambdaQueryWrapperX<MesQaInspectionRegulationProcessDO>()
                .eq(MesQaInspectionRegulationProcessDO::getRegulationVersionId, regulationVersionId));
    }

    default int deleteByVersionIds(Collection<Long> regulationVersionIds) {
        if (regulationVersionIds == null || regulationVersionIds.isEmpty()) {
            return 0;
        }
        return delete(new LambdaQueryWrapperX<MesQaInspectionRegulationProcessDO>()
                .in(MesQaInspectionRegulationProcessDO::getRegulationVersionId, regulationVersionIds));
    }
}
