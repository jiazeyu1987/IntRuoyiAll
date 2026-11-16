package cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaCommonRegulationProductBindingDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface MesQaCommonRegulationProductBindingMapper
        extends BaseMapperX<MesQaCommonRegulationProductBindingDO> {

    default MesQaCommonRegulationProductBindingDO selectEnabledByProjectId(Long projectId) {
        if (projectId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<MesQaCommonRegulationProductBindingDO>()
                .eq(MesQaCommonRegulationProductBindingDO::getProductDccProjectCodeId, projectId)
                .eq(MesQaCommonRegulationProductBindingDO::getScopeCode,
                        MesQaCommonRegulationProductBindingDO.SCOPE_COMMON_PACKAGING)
                .eq(MesQaCommonRegulationProductBindingDO::getBindingStatus,
                        MesQaCommonRegulationProductBindingDO.STATUS_ENABLED));
    }

    default List<MesQaCommonRegulationProductBindingDO> selectEnabledListByProjectIds(
            Collection<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesQaCommonRegulationProductBindingDO>()
                .in(MesQaCommonRegulationProductBindingDO::getProductDccProjectCodeId, projectIds)
                .eq(MesQaCommonRegulationProductBindingDO::getScopeCode,
                        MesQaCommonRegulationProductBindingDO.SCOPE_COMMON_PACKAGING)
                .eq(MesQaCommonRegulationProductBindingDO::getBindingStatus,
                        MesQaCommonRegulationProductBindingDO.STATUS_ENABLED)
                .orderByAsc(MesQaCommonRegulationProductBindingDO::getProductDccProjectCodeId)
                .orderByDesc(MesQaCommonRegulationProductBindingDO::getId));
    }

    default List<MesQaCommonRegulationProductBindingDO> selectByProjectIdOrderByCreateTimeDesc(Long projectId) {
        if (projectId == null) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesQaCommonRegulationProductBindingDO>()
                .eq(MesQaCommonRegulationProductBindingDO::getProductDccProjectCodeId, projectId)
                .eq(MesQaCommonRegulationProductBindingDO::getScopeCode,
                        MesQaCommonRegulationProductBindingDO.SCOPE_COMMON_PACKAGING)
                .orderByDesc(MesQaCommonRegulationProductBindingDO::getCreateTime)
                .orderByDesc(MesQaCommonRegulationProductBindingDO::getId));
    }

    default int updateEnabledStatusByProjectId(Long projectId, String targetStatus) {
        if (projectId == null) {
            return 0;
        }
        return update(null, new LambdaUpdateWrapper<MesQaCommonRegulationProductBindingDO>()
                .set(MesQaCommonRegulationProductBindingDO::getBindingStatus, targetStatus)
                .eq(MesQaCommonRegulationProductBindingDO::getProductDccProjectCodeId, projectId)
                .eq(MesQaCommonRegulationProductBindingDO::getScopeCode,
                        MesQaCommonRegulationProductBindingDO.SCOPE_COMMON_PACKAGING)
                .eq(MesQaCommonRegulationProductBindingDO::getBindingStatus,
                        MesQaCommonRegulationProductBindingDO.STATUS_ENABLED));
    }
}
