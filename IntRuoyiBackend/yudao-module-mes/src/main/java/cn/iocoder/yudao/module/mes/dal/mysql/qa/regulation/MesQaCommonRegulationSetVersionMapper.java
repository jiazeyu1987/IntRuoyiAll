package cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaCommonRegulationSetVersionDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface MesQaCommonRegulationSetVersionMapper
        extends BaseMapperX<MesQaCommonRegulationSetVersionDO> {

    default List<MesQaCommonRegulationSetVersionDO> selectListBySetId(Long setId) {
        if (setId == null) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesQaCommonRegulationSetVersionDO>()
                .eq(MesQaCommonRegulationSetVersionDO::getSetId, setId)
                .orderByDesc(MesQaCommonRegulationSetVersionDO::getPublishedAt)
                .orderByDesc(MesQaCommonRegulationSetVersionDO::getId));
    }

    default List<MesQaCommonRegulationSetVersionDO> selectListBySetIds(Collection<Long> setIds) {
        if (setIds == null || setIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesQaCommonRegulationSetVersionDO>()
                .in(MesQaCommonRegulationSetVersionDO::getSetId, setIds)
                .orderByAsc(MesQaCommonRegulationSetVersionDO::getSetId)
                .orderByDesc(MesQaCommonRegulationSetVersionDO::getPublishedAt)
                .orderByDesc(MesQaCommonRegulationSetVersionDO::getId));
    }

    default MesQaCommonRegulationSetVersionDO selectBySetIdAndVersionNo(Long setId, String versionNo) {
        return selectOne(new LambdaQueryWrapperX<MesQaCommonRegulationSetVersionDO>()
                .eq(MesQaCommonRegulationSetVersionDO::getSetId, setId)
                .eq(MesQaCommonRegulationSetVersionDO::getVersionNo, versionNo)
                .last("LIMIT 1"));
    }
}
