package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesProEdhrWorkTaskAssignmentRuleMapper
        extends BaseMapperX<MesProEdhrWorkTaskAssignmentRuleDO> {

    default MesProEdhrWorkTaskAssignmentRuleDO selectEnabledByRouteProcessAndType(Long routeProcessId, String taskType) {
        return selectOne(new LambdaQueryWrapperX<MesProEdhrWorkTaskAssignmentRuleDO>()
                .eq(MesProEdhrWorkTaskAssignmentRuleDO::getRouteProcessId, routeProcessId)
                .eq(MesProEdhrWorkTaskAssignmentRuleDO::getTaskType, taskType)
                .eq(MesProEdhrWorkTaskAssignmentRuleDO::getEnabled, true)
                .orderByDesc(MesProEdhrWorkTaskAssignmentRuleDO::getId));
    }

    default MesProEdhrWorkTaskAssignmentRuleDO selectEnabledByScopeAndType(String scopeType, Long scopeId,
                                                                           String taskType) {
        return selectOne(new LambdaQueryWrapperX<MesProEdhrWorkTaskAssignmentRuleDO>()
                .eq(MesProEdhrWorkTaskAssignmentRuleDO::getScopeType, scopeType)
                .eq(MesProEdhrWorkTaskAssignmentRuleDO::getScopeId, scopeId)
                .eq(MesProEdhrWorkTaskAssignmentRuleDO::getTaskType, taskType)
                .eq(MesProEdhrWorkTaskAssignmentRuleDO::getEnabled, true)
                .orderByDesc(MesProEdhrWorkTaskAssignmentRuleDO::getId));
    }

    default MesProEdhrWorkTaskAssignmentRuleDO selectByScopeAndType(String scopeType, Long scopeId, String taskType) {
        return selectOne(new LambdaQueryWrapperX<MesProEdhrWorkTaskAssignmentRuleDO>()
                .eq(MesProEdhrWorkTaskAssignmentRuleDO::getScopeType, scopeType)
                .eq(MesProEdhrWorkTaskAssignmentRuleDO::getScopeId, scopeId)
                .eq(MesProEdhrWorkTaskAssignmentRuleDO::getTaskType, taskType)
                .orderByDesc(MesProEdhrWorkTaskAssignmentRuleDO::getId));
    }

    default List<MesProEdhrWorkTaskAssignmentRuleDO> selectListByScopeAndType(String scopeType, Long scopeId,
                                                                              String taskType) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrWorkTaskAssignmentRuleDO>()
                .eq(MesProEdhrWorkTaskAssignmentRuleDO::getScopeType, scopeType)
                .eq(MesProEdhrWorkTaskAssignmentRuleDO::getScopeId, scopeId)
                .eq(MesProEdhrWorkTaskAssignmentRuleDO::getTaskType, taskType)
                .orderByDesc(MesProEdhrWorkTaskAssignmentRuleDO::getId));
    }
}
