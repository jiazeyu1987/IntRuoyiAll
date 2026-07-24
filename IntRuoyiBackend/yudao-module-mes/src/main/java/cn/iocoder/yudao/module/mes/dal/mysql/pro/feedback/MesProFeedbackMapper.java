package cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.MesProFeedbackPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.enums.pro.MesProFeedbackStatusEnum;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.time.LocalDate;

/**
 * MES 生产报工 Mapper
 *
 * @author 瑛泰源码
 */
@Mapper
public interface MesProFeedbackMapper extends BaseMapperX<MesProFeedbackDO> {

    default PageResult<MesProFeedbackDO> selectPage(MesProFeedbackPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProFeedbackDO>()
                .eqIfPresent(MesProFeedbackDO::getId, reqVO.getId())
                .likeIfPresent(MesProFeedbackDO::getCode, reqVO.getCode())
                .eqIfPresent(MesProFeedbackDO::getType, reqVO.getType())
                .eqIfPresent(MesProFeedbackDO::getWorkOrderId, reqVO.getWorkOrderId())
                .eqIfPresent(MesProFeedbackDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(MesProFeedbackDO::getFeedbackTime, reqVO.getFeedbackTime())
                .eqIfPresent(MesProFeedbackDO::getItemId, reqVO.getItemId())
                .eqIfPresent(MesProFeedbackDO::getFeedbackUserId, reqVO.getFeedbackUserId())
                .eqIfPresent(MesProFeedbackDO::getCreator, reqVO.getCreator())
                .orderByDesc(MesProFeedbackDO::getId));
    }

    default List<MesProFeedbackDO> selectListByTaskIds(Collection<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProFeedbackDO>()
                .in(MesProFeedbackDO::getTaskId, taskIds));
    }

    default List<MesProFeedbackDO> selectListByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProFeedbackDO>()
                .in(MesProFeedbackDO::getId, ids)
                .orderByAsc(MesProFeedbackDO::getId));
    }

    default List<MesProFeedbackDO> selectListBySourceImportRecordId(Long sourceImportRecordId) {
        if (sourceImportRecordId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProFeedbackDO>()
                .eq(MesProFeedbackDO::getSourceImportRecordId, sourceImportRecordId)
                .orderByAsc(MesProFeedbackDO::getId));
    }

    default List<MesProFeedbackDO> selectListBySourceImportRecordIds(Collection<Long> sourceImportRecordIds) {
        if (sourceImportRecordIds == null || sourceImportRecordIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProFeedbackDO>()
                .in(MesProFeedbackDO::getSourceImportRecordId, sourceImportRecordIds)
                .orderByAsc(MesProFeedbackDO::getSourceImportRecordId)
                .orderByAsc(MesProFeedbackDO::getId));
    }

    default List<MesProFeedbackDO> selectFinishedListByScheduleOrderId(Long scheduleOrderId) {
        if (scheduleOrderId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProFeedbackDO>()
                .eq(MesProFeedbackDO::getScheduleOrderId, scheduleOrderId)
                .eq(MesProFeedbackDO::getStatus, MesProFeedbackStatusEnum.FINISHED.getStatus())
                .orderByAsc(MesProFeedbackDO::getFeedbackTime)
                .orderByAsc(MesProFeedbackDO::getId));
    }

    default List<MesProFeedbackDO> selectProgressListByScheduleOrderId(Long scheduleOrderId) {
        if (scheduleOrderId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProFeedbackDO>()
                .eq(MesProFeedbackDO::getScheduleOrderId, scheduleOrderId)
                .in(MesProFeedbackDO::getStatus, List.of(
                        MesProFeedbackStatusEnum.APPROVING.getStatus(),
                        MesProFeedbackStatusEnum.UNCHECK.getStatus(),
                        MesProFeedbackStatusEnum.FINISHED.getStatus()))
                .orderByAsc(MesProFeedbackDO::getFeedbackTime)
                .orderByAsc(MesProFeedbackDO::getId));
    }

    default List<MesProFeedbackDO> selectFinishedListByScheduleOrderProcessIdsToday(
            Collection<Long> scheduleOrderProcessIds, LocalDate date) {
        if (scheduleOrderProcessIds == null || scheduleOrderProcessIds.isEmpty() || date == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProFeedbackDO>()
                .in(MesProFeedbackDO::getScheduleOrderProcessId, scheduleOrderProcessIds)
                .eq(MesProFeedbackDO::getStatus, MesProFeedbackStatusEnum.FINISHED.getStatus())
                .ge(MesProFeedbackDO::getFeedbackTime, date.atStartOfDay())
                .lt(MesProFeedbackDO::getFeedbackTime, date.plusDays(1).atStartOfDay())
                .orderByAsc(MesProFeedbackDO::getFeedbackTime)
                .orderByAsc(MesProFeedbackDO::getId));
    }

    default List<MesProFeedbackDO> selectUnifiedApprovalList(Long approveUserId, Long feedbackUserId,
                                                             Collection<Integer> statuses, String keyword) {
        if (statuses == null || statuses.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProFeedbackDO>()
                .eqIfPresent(MesProFeedbackDO::getApproveUserId, approveUserId)
                .eqIfPresent(MesProFeedbackDO::getFeedbackUserId, feedbackUserId)
                .in(MesProFeedbackDO::getStatus, statuses)
                .likeIfPresent(MesProFeedbackDO::getCode, keyword)
                .orderByDesc(MesProFeedbackDO::getId));
    }

}
