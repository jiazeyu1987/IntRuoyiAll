package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrPermissionRuleDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface MesProEdhrPermissionRuleMapper extends BaseMapperX<MesProEdhrPermissionRuleDO> {

    default List<MesProEdhrPermissionRuleDO> selectEnabledListByScopeAndAbilities(Long scopeId,
                                                                                  Collection<String> abilities) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrPermissionRuleDO>()
                .eq(MesProEdhrPermissionRuleDO::getScopeId, scopeId)
                .inIfPresent(MesProEdhrPermissionRuleDO::getAbility, abilities)
                .eq(MesProEdhrPermissionRuleDO::getStatus, "ENABLED")
                .orderByAsc(MesProEdhrPermissionRuleDO::getPriority)
                .orderByDesc(MesProEdhrPermissionRuleDO::getId));
    }

    default List<MesProEdhrPermissionRuleDO> selectListByScopeId(Long scopeId) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrPermissionRuleDO>()
                .eq(MesProEdhrPermissionRuleDO::getScopeId, scopeId)
                .orderByAsc(MesProEdhrPermissionRuleDO::getPriority)
                .orderByDesc(MesProEdhrPermissionRuleDO::getId));
    }

    default void deleteByScopeId(Long scopeId) {
        delete(new LambdaQueryWrapperX<MesProEdhrPermissionRuleDO>()
                .eq(MesProEdhrPermissionRuleDO::getScopeId, scopeId));
    }
}
