package cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesQaInspectionRegulationVersionMapper
        extends BaseMapperX<MesQaInspectionRegulationVersionDO> {

    default MesQaInspectionRegulationVersionDO selectByRegulationIdAndVersionNo(Long regulationId, String versionNo) {
        return selectOne(new LambdaQueryWrapperX<MesQaInspectionRegulationVersionDO>()
                .eq(MesQaInspectionRegulationVersionDO::getRegulationId, regulationId)
                .eq(MesQaInspectionRegulationVersionDO::getVersionNo, versionNo)
                .orderByDesc(MesQaInspectionRegulationVersionDO::getId)
                .last("LIMIT 1"));
    }

    default MesQaInspectionRegulationVersionDO selectLatestDraftByRegulationId(Long regulationId) {
        return selectOne(new LambdaQueryWrapperX<MesQaInspectionRegulationVersionDO>()
                .eq(MesQaInspectionRegulationVersionDO::getRegulationId, regulationId)
                .eq(MesQaInspectionRegulationVersionDO::getLifecycleStatus, "DRAFT")
                .orderByDesc(MesQaInspectionRegulationVersionDO::getId)
                .last("LIMIT 1"));
    }

    default List<MesQaInspectionRegulationVersionDO> selectListByRegulationId(Long regulationId) {
        if (regulationId == null) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesQaInspectionRegulationVersionDO>()
                .eq(MesQaInspectionRegulationVersionDO::getRegulationId, regulationId)
                .orderByAsc(MesQaInspectionRegulationVersionDO::getId));
    }

    default List<MesQaInspectionRegulationVersionDO> selectListDraftByRegulationId(Long regulationId) {
        return selectList(new LambdaQueryWrapperX<MesQaInspectionRegulationVersionDO>()
                .eq(MesQaInspectionRegulationVersionDO::getRegulationId, regulationId)
                .eq(MesQaInspectionRegulationVersionDO::getLifecycleStatus, "DRAFT")
                .orderByDesc(MesQaInspectionRegulationVersionDO::getId));
    }

    default int deleteByRegulationId(Long regulationId) {
        if (regulationId == null) {
            return 0;
        }
        return delete(new LambdaQueryWrapperX<MesQaInspectionRegulationVersionDO>()
                .eq(MesQaInspectionRegulationVersionDO::getRegulationId, regulationId));
    }

}
