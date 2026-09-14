package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordRepeatRowGroupDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesProBatchRecordRepeatRowGroupMapper extends BaseMapperX<MesProBatchRecordRepeatRowGroupDO> {

    default List<MesProBatchRecordRepeatRowGroupDO> selectListByScope(String scopeType, Long scopeId) {
        return selectList(new LambdaQueryWrapperX<MesProBatchRecordRepeatRowGroupDO>()
                .eq(MesProBatchRecordRepeatRowGroupDO::getScopeType, scopeType)
                .eq(MesProBatchRecordRepeatRowGroupDO::getScopeId, scopeId)
                .orderByAsc(MesProBatchRecordRepeatRowGroupDO::getTargetReportId)
                .orderByAsc(MesProBatchRecordRepeatRowGroupDO::getRouteProcessId)
                .orderByAsc(MesProBatchRecordRepeatRowGroupDO::getId));
    }

    default List<MesProBatchRecordRepeatRowGroupDO> selectListByScopeAndTargetReport(String scopeType,
                                                                                     Long scopeId,
                                                                                     String targetReportId) {
        return selectList(new LambdaQueryWrapperX<MesProBatchRecordRepeatRowGroupDO>()
                .eq(MesProBatchRecordRepeatRowGroupDO::getScopeType, scopeType)
                .eq(MesProBatchRecordRepeatRowGroupDO::getScopeId, scopeId)
                .eq(MesProBatchRecordRepeatRowGroupDO::getTargetReportId, targetReportId)
                .orderByAsc(MesProBatchRecordRepeatRowGroupDO::getRouteProcessId)
                .orderByAsc(MesProBatchRecordRepeatRowGroupDO::getId));
    }

    default void deleteEnabledByScopeAndTargetReport(String scopeType, Long scopeId, String targetReportId) {
        delete(new LambdaQueryWrapperX<MesProBatchRecordRepeatRowGroupDO>()
                .eq(MesProBatchRecordRepeatRowGroupDO::getScopeType, scopeType)
                .eq(MesProBatchRecordRepeatRowGroupDO::getScopeId, scopeId)
                .eq(MesProBatchRecordRepeatRowGroupDO::getTargetReportId, targetReportId)
                .eq(MesProBatchRecordRepeatRowGroupDO::getEnabled, true));
    }
}