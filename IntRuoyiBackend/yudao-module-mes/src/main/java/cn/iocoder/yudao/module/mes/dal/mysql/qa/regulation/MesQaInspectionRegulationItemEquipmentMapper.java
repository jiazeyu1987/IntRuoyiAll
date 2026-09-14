package cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemEquipmentDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface MesQaInspectionRegulationItemEquipmentMapper
        extends BaseMapperX<MesQaInspectionRegulationItemEquipmentDO> {

    default List<MesQaInspectionRegulationItemEquipmentDO> selectListByVersionId(Long regulationVersionId) {
        return selectList(new LambdaQueryWrapperX<MesQaInspectionRegulationItemEquipmentDO>()
                .eq(MesQaInspectionRegulationItemEquipmentDO::getRegulationVersionId, regulationVersionId)
                .orderByAsc(MesQaInspectionRegulationItemEquipmentDO::getInspectionType)
                .orderByAsc(MesQaInspectionRegulationItemEquipmentDO::getItemCode)
                .orderByDesc(MesQaInspectionRegulationItemEquipmentDO::getDefaultFlag)
                .orderByAsc(MesQaInspectionRegulationItemEquipmentDO::getSort)
                .orderByAsc(MesQaInspectionRegulationItemEquipmentDO::getId));
    }

    default Long selectCountByVersionIds(Collection<Long> regulationVersionIds) {
        if (regulationVersionIds == null || regulationVersionIds.isEmpty()) {
            return 0L;
        }
        return selectCount(new LambdaQueryWrapperX<MesQaInspectionRegulationItemEquipmentDO>()
                .in(MesQaInspectionRegulationItemEquipmentDO::getRegulationVersionId, regulationVersionIds));
    }

    default int deleteByVersionId(Long regulationVersionId) {
        return delete(new LambdaQueryWrapperX<MesQaInspectionRegulationItemEquipmentDO>()
                .eq(MesQaInspectionRegulationItemEquipmentDO::getRegulationVersionId, regulationVersionId));
    }

    default int deleteByVersionIds(Collection<Long> regulationVersionIds) {
        if (regulationVersionIds == null || regulationVersionIds.isEmpty()) {
            return 0;
        }
        return delete(new LambdaQueryWrapperX<MesQaInspectionRegulationItemEquipmentDO>()
                .in(MesQaInspectionRegulationItemEquipmentDO::getRegulationVersionId, regulationVersionIds));
    }
}
