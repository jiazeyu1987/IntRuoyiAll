package cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface MesQaInspectionRegulationMapper extends BaseMapperX<MesQaInspectionRegulationDO> {

    default MesQaInspectionRegulationDO selectByDccProjectCodeId(Long dccProjectCodeId) {
        return selectOne(new LambdaQueryWrapperX<MesQaInspectionRegulationDO>()
                .eq(MesQaInspectionRegulationDO::getDccProjectCodeId, dccProjectCodeId));
    }

    default List<MesQaInspectionRegulationDO> selectListByDccProjectCodeIds(
            Collection<Long> dccProjectCodeIds) {
        if (dccProjectCodeIds == null || dccProjectCodeIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesQaInspectionRegulationDO>()
                .in(MesQaInspectionRegulationDO::getDccProjectCodeId, dccProjectCodeIds)
                .orderByAsc(MesQaInspectionRegulationDO::getDccProjectCodeId)
                .orderByDesc(MesQaInspectionRegulationDO::getId));
    }
}
