package cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProScheduleIssueDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface MesProScheduleIssueMapper extends BaseMapperX<MesProScheduleIssueDO> {

    default List<MesProScheduleIssueDO> selectList(Long workOrderId, Long taskId, String issueType, String severity) {
        return selectList(new LambdaQueryWrapperX<MesProScheduleIssueDO>()
                .eqIfPresent(MesProScheduleIssueDO::getWorkOrderId, workOrderId)
                .eqIfPresent(MesProScheduleIssueDO::getTaskId, taskId)
                .eqIfPresent(MesProScheduleIssueDO::getIssueType, issueType)
                .eqIfPresent(MesProScheduleIssueDO::getSeverity, severity)
                .orderByDesc(MesProScheduleIssueDO::getId));
    }

    default void deleteByWorkOrderIds(Collection<Long> workOrderIds) {
        if (workOrderIds == null || workOrderIds.isEmpty()) {
            return;
        }
        delete(new LambdaQueryWrapperX<MesProScheduleIssueDO>()
                .in(MesProScheduleIssueDO::getWorkOrderId, workOrderIds));
    }

    default void deleteByTaskIds(Collection<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return;
        }
        delete(new LambdaQueryWrapperX<MesProScheduleIssueDO>()
                .in(MesProScheduleIssueDO::getTaskId, taskIds));
    }

    default List<MesProScheduleIssueDO> selectListByCalendarDateRange(LocalDateTime startTime, LocalDateTime endTime) {
        return selectList(new LambdaQueryWrapperX<MesProScheduleIssueDO>()
                .geIfPresent(MesProScheduleIssueDO::getCalendarDate, startTime)
                .ltIfPresent(MesProScheduleIssueDO::getCalendarDate, endTime)
                .orderByAsc(MesProScheduleIssueDO::getCalendarDate)
                .orderByAsc(MesProScheduleIssueDO::getId));
    }

    default List<MesProScheduleIssueDO> selectListByWorkOrderIds(Collection<Long> workOrderIds) {
        if (workOrderIds == null || workOrderIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProScheduleIssueDO>()
                .in(MesProScheduleIssueDO::getWorkOrderId, workOrderIds)
                .orderByAsc(MesProScheduleIssueDO::getCalendarDate)
                .orderByAsc(MesProScheduleIssueDO::getId));
    }

}
