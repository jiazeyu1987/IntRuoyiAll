package cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaCommonRegulationSetVersionMemberDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface MesQaCommonRegulationSetVersionMemberMapper
        extends BaseMapperX<MesQaCommonRegulationSetVersionMemberDO> {

    default List<MesQaCommonRegulationSetVersionMemberDO> selectListBySetVersionId(Long setVersionId) {
        if (setVersionId == null) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesQaCommonRegulationSetVersionMemberDO>()
                .eq(MesQaCommonRegulationSetVersionMemberDO::getSetVersionId, setVersionId)
                .orderByAsc(MesQaCommonRegulationSetVersionMemberDO::getSort)
                .orderByAsc(MesQaCommonRegulationSetVersionMemberDO::getId));
    }

    default List<MesQaCommonRegulationSetVersionMemberDO> selectListBySetVersionIds(
            Collection<Long> setVersionIds) {
        if (setVersionIds == null || setVersionIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesQaCommonRegulationSetVersionMemberDO>()
                .in(MesQaCommonRegulationSetVersionMemberDO::getSetVersionId, setVersionIds)
                .orderByAsc(MesQaCommonRegulationSetVersionMemberDO::getSetVersionId)
                .orderByAsc(MesQaCommonRegulationSetVersionMemberDO::getSort)
                .orderByAsc(MesQaCommonRegulationSetVersionMemberDO::getId));
    }

    default int deleteBySetVersionId(Long setVersionId) {
        if (setVersionId == null) {
            return 0;
        }
        return delete(new LambdaQueryWrapperX<MesQaCommonRegulationSetVersionMemberDO>()
                .eq(MesQaCommonRegulationSetVersionMemberDO::getSetVersionId, setVersionId));
    }
}
