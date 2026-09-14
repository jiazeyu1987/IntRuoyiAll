package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordCellLinkRuleDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface MesProBatchRecordCellLinkRuleMapper extends BaseMapperX<MesProBatchRecordCellLinkRuleDO> {

    default List<MesProBatchRecordCellLinkRuleDO> selectListByScope(String scopeType, Long scopeId) {
        return selectList(new LambdaQueryWrapperX<MesProBatchRecordCellLinkRuleDO>()
                .eq(MesProBatchRecordCellLinkRuleDO::getScopeType, scopeType)
                .eq(MesProBatchRecordCellLinkRuleDO::getScopeId, scopeId)
                .orderByAsc(MesProBatchRecordCellLinkRuleDO::getRouteProcessId)
                .orderByAsc(MesProBatchRecordCellLinkRuleDO::getTargetReportId)
                .orderByAsc(MesProBatchRecordCellLinkRuleDO::getTargetRowIndex)
                .orderByAsc(MesProBatchRecordCellLinkRuleDO::getTargetColumnIndex)
                .orderByAsc(MesProBatchRecordCellLinkRuleDO::getId));
    }

    default List<MesProBatchRecordCellLinkRuleDO> selectEnabledListByScopeAndTargetReport(String scopeType,
                                                                                         Long scopeId,
                                                                                         String targetReportId) {
        return selectEnabledListByScopeAndTargetReport(scopeType, scopeId, targetReportId, null);
    }

    default List<MesProBatchRecordCellLinkRuleDO> selectEnabledListByScopeAndTargetReport(String scopeType,
                                                                                         Long scopeId,
                                                                                         String targetReportId,
                                                                                         Long routeProcessId) {
        LambdaQueryWrapperX<MesProBatchRecordCellLinkRuleDO> query =
                new LambdaQueryWrapperX<MesProBatchRecordCellLinkRuleDO>()
                        .eq(MesProBatchRecordCellLinkRuleDO::getScopeType, scopeType)
                        .eq(MesProBatchRecordCellLinkRuleDO::getScopeId, scopeId)
                        .eq(MesProBatchRecordCellLinkRuleDO::getTargetReportId, targetReportId)
                        .eq(MesProBatchRecordCellLinkRuleDO::getEnabled, true);
        if (routeProcessId != null) {
            query.eq(MesProBatchRecordCellLinkRuleDO::getRouteProcessId, routeProcessId);
        }
        return selectList(query
                .orderByAsc(MesProBatchRecordCellLinkRuleDO::getTargetRowIndex)
                .orderByAsc(MesProBatchRecordCellLinkRuleDO::getTargetColumnIndex));
    }

    default List<MesProBatchRecordCellLinkRuleDO> selectListByScopeAndRouteProcessId(String scopeType,
                                                                                    Long scopeId,
                                                                                    Long routeProcessId) {
        LambdaQueryWrapperX<MesProBatchRecordCellLinkRuleDO> query =
                new LambdaQueryWrapperX<MesProBatchRecordCellLinkRuleDO>()
                        .eq(MesProBatchRecordCellLinkRuleDO::getScopeType, scopeType)
                        .eq(MesProBatchRecordCellLinkRuleDO::getScopeId, scopeId);
        if (routeProcessId != null) {
            query.eq(MesProBatchRecordCellLinkRuleDO::getRouteProcessId, routeProcessId);
        }
        return selectList(new LambdaQueryWrapperX<MesProBatchRecordCellLinkRuleDO>()
                .eq(MesProBatchRecordCellLinkRuleDO::getScopeType, scopeType)
                .eq(MesProBatchRecordCellLinkRuleDO::getScopeId, scopeId)
                .eqIfPresent(MesProBatchRecordCellLinkRuleDO::getRouteProcessId, routeProcessId)
                .orderByAsc(MesProBatchRecordCellLinkRuleDO::getRouteProcessId)
                .orderByAsc(MesProBatchRecordCellLinkRuleDO::getTargetReportId)
                .orderByAsc(MesProBatchRecordCellLinkRuleDO::getTargetRowIndex)
                .orderByAsc(MesProBatchRecordCellLinkRuleDO::getTargetColumnIndex)
                .orderByAsc(MesProBatchRecordCellLinkRuleDO::getId));
    }

    default void deleteByScope(String scopeType, Long scopeId) {
        delete(new LambdaQueryWrapperX<MesProBatchRecordCellLinkRuleDO>()
                .eq(MesProBatchRecordCellLinkRuleDO::getScopeType, scopeType)
                .eq(MesProBatchRecordCellLinkRuleDO::getScopeId, scopeId));
    }

    default void deleteByScopeAndRouteProcessId(String scopeType, Long scopeId, Long routeProcessId) {
        delete(new LambdaQueryWrapperX<MesProBatchRecordCellLinkRuleDO>()
                .eq(MesProBatchRecordCellLinkRuleDO::getScopeType, scopeType)
                .eq(MesProBatchRecordCellLinkRuleDO::getScopeId, scopeId)
                .eq(MesProBatchRecordCellLinkRuleDO::getRouteProcessId, routeProcessId));
    }

    default List<MesProBatchRecordCellLinkRuleDO> selectListByReportIds(Collection<String> reportIds) {
        if (reportIds == null || reportIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesProBatchRecordCellLinkRuleDO>()
                .and(wrapper -> wrapper
                        .in(MesProBatchRecordCellLinkRuleDO::getSourceReportId, reportIds)
                        .or()
                        .in(MesProBatchRecordCellLinkRuleDO::getTargetReportId, reportIds)));
    }
}
