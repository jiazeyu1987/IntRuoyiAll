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
                .orderByAsc(MesProBatchRecordCellLinkRuleDO::getTargetReportId)
                .orderByAsc(MesProBatchRecordCellLinkRuleDO::getTargetRowIndex)
                .orderByAsc(MesProBatchRecordCellLinkRuleDO::getTargetColumnIndex)
                .orderByAsc(MesProBatchRecordCellLinkRuleDO::getId));
    }

    default List<MesProBatchRecordCellLinkRuleDO> selectEnabledListByScopeAndTargetReport(String scopeType,
                                                                                         Long scopeId,
                                                                                         String targetReportId) {
        return selectList(new LambdaQueryWrapperX<MesProBatchRecordCellLinkRuleDO>()
                .eq(MesProBatchRecordCellLinkRuleDO::getScopeType, scopeType)
                .eq(MesProBatchRecordCellLinkRuleDO::getScopeId, scopeId)
                .eq(MesProBatchRecordCellLinkRuleDO::getTargetReportId, targetReportId)
                .eq(MesProBatchRecordCellLinkRuleDO::getEnabled, true)
                .orderByAsc(MesProBatchRecordCellLinkRuleDO::getTargetRowIndex)
                .orderByAsc(MesProBatchRecordCellLinkRuleDO::getTargetColumnIndex));
    }

    default void deleteByScope(String scopeType, Long scopeId) {
        delete(new LambdaQueryWrapperX<MesProBatchRecordCellLinkRuleDO>()
                .eq(MesProBatchRecordCellLinkRuleDO::getScopeType, scopeType)
                .eq(MesProBatchRecordCellLinkRuleDO::getScopeId, scopeId));
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
