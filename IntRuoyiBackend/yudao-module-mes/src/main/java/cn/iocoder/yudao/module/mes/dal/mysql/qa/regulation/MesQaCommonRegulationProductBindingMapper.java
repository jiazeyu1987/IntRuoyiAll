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

    default MesQaCommonRegulationProductBindingDO selectEnabledByProductId(Long productId) {
        if (productId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<MesQaCommonRegulationProductBindingDO>()
                .eq(MesQaCommonRegulationProductBindingDO::getProductId, productId)
                .eq(MesQaCommonRegulationProductBindingDO::getScopeCode,
                        MesQaCommonRegulationProductBindingDO.SCOPE_COMMON_PACKAGING)
                .eq(MesQaCommonRegulationProductBindingDO::getBindingStatus,
                        MesQaCommonRegulationProductBindingDO.STATUS_ENABLED)
                .last("LIMIT 1"));
    }

    default List<MesQaCommonRegulationProductBindingDO> selectEnabledListByProductIds(
            Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesQaCommonRegulationProductBindingDO>()
                .in(MesQaCommonRegulationProductBindingDO::getProductId, productIds)
                .eq(MesQaCommonRegulationProductBindingDO::getScopeCode,
                        MesQaCommonRegulationProductBindingDO.SCOPE_COMMON_PACKAGING)
                .eq(MesQaCommonRegulationProductBindingDO::getBindingStatus,
                        MesQaCommonRegulationProductBindingDO.STATUS_ENABLED)
                .orderByAsc(MesQaCommonRegulationProductBindingDO::getProductId)
                .orderByDesc(MesQaCommonRegulationProductBindingDO::getId));
    }

    default List<MesQaCommonRegulationProductBindingDO> selectByProductIdOrderByCreateTimeDesc(Long productId) {
        if (productId == null) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesQaCommonRegulationProductBindingDO>()
                .eq(MesQaCommonRegulationProductBindingDO::getProductId, productId)
                .eq(MesQaCommonRegulationProductBindingDO::getScopeCode,
                        MesQaCommonRegulationProductBindingDO.SCOPE_COMMON_PACKAGING)
                .orderByDesc(MesQaCommonRegulationProductBindingDO::getCreateTime)
                .orderByDesc(MesQaCommonRegulationProductBindingDO::getId));
    }

    default int updateEnabledStatusByProductId(Long productId, String targetStatus) {
        if (productId == null) {
            return 0;
        }
        return update(null, new LambdaUpdateWrapper<MesQaCommonRegulationProductBindingDO>()
                .set(MesQaCommonRegulationProductBindingDO::getBindingStatus, targetStatus)
                .eq(MesQaCommonRegulationProductBindingDO::getProductId, productId)
                .eq(MesQaCommonRegulationProductBindingDO::getScopeCode,
                        MesQaCommonRegulationProductBindingDO.SCOPE_COMMON_PACKAGING)
                .eq(MesQaCommonRegulationProductBindingDO::getBindingStatus,
                        MesQaCommonRegulationProductBindingDO.STATUS_ENABLED));
    }
}
