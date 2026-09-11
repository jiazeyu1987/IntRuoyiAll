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
        return selectByDccProjectCodeId(dccProjectCodeId, MesQaInspectionRegulationDO.OWNER_MODULE_MES_QA);
    }

    default MesQaInspectionRegulationDO selectByDccProjectCodeId(Long dccProjectCodeId, String ownerModule) {
        return selectOne(new LambdaQueryWrapperX<MesQaInspectionRegulationDO>()
                .eq(MesQaInspectionRegulationDO::getDccProjectCodeId, dccProjectCodeId)
                .eq(MesQaInspectionRegulationDO::getOwnerModule, ownerModule));
    }

    default List<MesQaInspectionRegulationDO> selectListByDccProjectCodeIds(
            Collection<Long> dccProjectCodeIds) {
        if (dccProjectCodeIds == null || dccProjectCodeIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesQaInspectionRegulationDO>()
                .in(MesQaInspectionRegulationDO::getDccProjectCodeId, dccProjectCodeIds)
                .eq(MesQaInspectionRegulationDO::getOwnerModule,
                        MesQaInspectionRegulationDO.OWNER_MODULE_MES_QA)
                .orderByAsc(MesQaInspectionRegulationDO::getDccProjectCodeId)
                .orderByDesc(MesQaInspectionRegulationDO::getId));
    }

    default List<MesQaInspectionRegulationDO> selectCommonList() {
        return selectList(new LambdaQueryWrapperX<MesQaInspectionRegulationDO>()
                .eq(MesQaInspectionRegulationDO::getOwnerModule,
                        MesQaInspectionRegulationDO.OWNER_MODULE_MES_QA_COMMON)
                .orderByAsc(MesQaInspectionRegulationDO::getDccProjectCodeId)
                .orderByDesc(MesQaInspectionRegulationDO::getId));
    }
}
