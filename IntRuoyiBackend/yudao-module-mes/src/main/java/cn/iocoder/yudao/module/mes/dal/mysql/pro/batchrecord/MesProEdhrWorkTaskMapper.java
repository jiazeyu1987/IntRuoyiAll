package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MesProEdhrWorkTaskMapper extends BaseMapperX<MesProEdhrWorkTaskDO> {

    List<String> APPROVAL_CENTER_TASK_TYPES = List.of("REVIEW", "APPROVE", "RELEASE_APPROVE");

    default PageResult<MesProEdhrWorkTaskDO> selectMyPage(MesProEdhrWorkTaskPageReqVO reqVO,
                                                          Long assigneeUserId,
                                                          String status) {
        return selectPage(reqVO, baseMyWrapper(reqVO, assigneeUserId)
                .eq(MesProEdhrWorkTaskDO::getStatus, status)
                .orderByDesc(MesProEdhrWorkTaskDO::getId));
    }

    default PageResult<MesProEdhrWorkTaskDO> selectDonePage(MesProEdhrWorkTaskPageReqVO reqVO,
                                                            Long assigneeUserId) {
        return selectPage(reqVO, baseMyWrapper(reqVO, assigneeUserId)
                .eq(MesProEdhrWorkTaskDO::getStatus, MesProEdhrWorkTaskStatus.DONE)
                .orderByDesc(MesProEdhrWorkTaskDO::getCompletedAt)
                .orderByDesc(MesProEdhrWorkTaskDO::getId));
    }

    default PageResult<MesProEdhrWorkTaskDO> selectApprovalCenterTodoPage(MesProEdhrWorkTaskPageReqVO reqVO,
                                                                          Long assigneeUserId,
                                                                          String status) {
        return selectPage(reqVO, baseApprovalCenterWrapper(reqVO, assigneeUserId)
                .eq(MesProEdhrWorkTaskDO::getStatus, status)
                .orderByDesc(MesProEdhrWorkTaskDO::getId));
    }

    default PageResult<MesProEdhrWorkTaskDO> selectApprovalCenterDonePage(MesProEdhrWorkTaskPageReqVO reqVO,
                                                                          Long assigneeUserId) {
        return selectPage(reqVO, baseApprovalCenterWrapper(reqVO, assigneeUserId)
                .eq(MesProEdhrWorkTaskDO::getStatus, MesProEdhrWorkTaskStatus.DONE)
                .orderByDesc(MesProEdhrWorkTaskDO::getCompletedAt)
                .orderByDesc(MesProEdhrWorkTaskDO::getId));
    }

    default Long countMy(Long assigneeUserId, String taskType, String status) {
        return selectCount(new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                .eq(MesProEdhrWorkTaskDO::getAssigneeUserId, assigneeUserId)
                .eqIfPresent(MesProEdhrWorkTaskDO::getTaskType, taskType)
                .eqIfPresent(MesProEdhrWorkTaskDO::getStatus, status));
    }

    default PageResult<MesProEdhrWorkTaskDO> selectCandidateTodoPage(MesProEdhrWorkTaskPageReqVO reqVO,
                                                                     Long candidateUserId,
                                                                     String status) {
        LambdaQueryWrapperX<MesProEdhrWorkTaskDO> wrapper = new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                .eq(MesProEdhrWorkTaskDO::getTaskType, "REVIEW")
                .eq(MesProEdhrWorkTaskDO::getStatus, status)
                .eqIfPresent(MesProEdhrWorkTaskDO::getTaskType, reqVO.getTaskType())
                .likeIfPresent(MesProEdhrWorkTaskDO::getWorkOrderCode, reqVO.getWorkOrderCode())
                .likeIfPresent(MesProEdhrWorkTaskDO::getBatchCode, reqVO.getBatchCode())
                .likeIfPresent(MesProEdhrWorkTaskDO::getProcessName, reqVO.getProcessName());
        if (candidateUserId != null) {
            String candidateToken = "," + candidateUserId + ",";
            wrapper.apply("CONCAT(',', candidate_user_snapshot, ',') LIKE {0}", "%" + candidateToken + "%");
        }
        wrapper.orderByDesc(MesProEdhrWorkTaskDO::getId);
        return selectPage(reqVO, wrapper);
    }

    default Long countApprovalCenterTodoDuplicateTasks(MesProEdhrWorkTaskPageReqVO reqVO,
                                                       Long assigneeUserId,
                                                       Long candidateUserId,
                                                       String status) {
        LambdaQueryWrapperX<MesProEdhrWorkTaskDO> wrapper = baseApprovalCenterWrapper(reqVO, assigneeUserId)
                .eq(MesProEdhrWorkTaskDO::getTaskType, "REVIEW")
                .eq(MesProEdhrWorkTaskDO::getStatus, status);
        if (candidateUserId != null) {
            String candidateToken = "," + candidateUserId + ",";
            wrapper.apply("CONCAT(',', candidate_user_snapshot, ',') LIKE {0}", "%" + candidateToken + "%");
        }
        return selectCount(wrapper);
    }

    default List<MesProEdhrWorkTaskDO> selectTimelineListByExecutionId(Long executionId) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                .eq(MesProEdhrWorkTaskDO::getExecutionId, executionId)
                .orderByAsc(MesProEdhrWorkTaskDO::getCreateTime)
                .orderByAsc(MesProEdhrWorkTaskDO::getId));
    }

    default List<MesProEdhrWorkTaskDO> selectTimelineListByBatchExecutionId(Long batchExecutionId) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                .eq(MesProEdhrWorkTaskDO::getBatchExecutionId, batchExecutionId)
                .orderByAsc(MesProEdhrWorkTaskDO::getCreateTime)
                .orderByAsc(MesProEdhrWorkTaskDO::getId));
    }

    default List<MesProEdhrWorkTaskDO> selectDueActiveTasks(LocalDateTime now, int limit) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                .in(MesProEdhrWorkTaskDO::getStatus, MesProEdhrWorkTaskStatus.TODO, MesProEdhrWorkTaskStatus.DOING)
                .isNotNull(MesProEdhrWorkTaskDO::getDueTime)
                .le(MesProEdhrWorkTaskDO::getDueTime, now)
                .orderByAsc(MesProEdhrWorkTaskDO::getDueTime)
                .orderByAsc(MesProEdhrWorkTaskDO::getId)
                .last("LIMIT " + limit));
    }

    default int updateToOverdueIfActive(Long id, LocalDateTime now, String reason) {
        return update(new MesProEdhrWorkTaskDO()
                        .setStatus(MesProEdhrWorkTaskStatus.OVERDUE)
                        .setOverdueAt(now)
                        .setOverdueReason(reason)
                        .setReason(reason)
                        .setRemark(reason),
                new LambdaUpdateWrapper<MesProEdhrWorkTaskDO>()
                        .eq(MesProEdhrWorkTaskDO::getId, id)
                        .in(MesProEdhrWorkTaskDO::getStatus,
                                MesProEdhrWorkTaskStatus.TODO, MesProEdhrWorkTaskStatus.DOING));
    }

    default MesProEdhrWorkTaskDO selectActiveByBatchTaskAndType(Long batchTaskId, String taskType) {
        return selectOne(new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                .eq(MesProEdhrWorkTaskDO::getBatchTaskId, batchTaskId)
                .eq(MesProEdhrWorkTaskDO::getTaskType, taskType)
                .in(MesProEdhrWorkTaskDO::getStatus,
                        MesProEdhrWorkTaskStatus.TODO,
                        MesProEdhrWorkTaskStatus.DOING,
                        MesProEdhrWorkTaskStatus.OVERDUE)
                .orderByDesc(MesProEdhrWorkTaskDO::getId));
    }

    default List<MesProEdhrWorkTaskDO> selectActiveListByExecutionAndType(Long executionId, String taskType) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                .eq(MesProEdhrWorkTaskDO::getExecutionId, executionId)
                .eq(MesProEdhrWorkTaskDO::getTaskType, taskType)
                .in(MesProEdhrWorkTaskDO::getStatus,
                        MesProEdhrWorkTaskStatus.TODO,
                        MesProEdhrWorkTaskStatus.DOING,
                        MesProEdhrWorkTaskStatus.OVERDUE)
                .orderByAsc(MesProEdhrWorkTaskDO::getId));
    }

    default MesProEdhrWorkTaskDO selectActiveByBusinessScopeAndType(String businessScopeType, Long businessScopeId,
                                                                    String taskType) {
        return selectOne(new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                .eq(MesProEdhrWorkTaskDO::getBusinessScopeType, businessScopeType)
                .eq(MesProEdhrWorkTaskDO::getBusinessScopeId, businessScopeId)
                .eq(MesProEdhrWorkTaskDO::getTaskType, taskType)
                .in(MesProEdhrWorkTaskDO::getStatus,
                        MesProEdhrWorkTaskStatus.TODO,
                        MesProEdhrWorkTaskStatus.DOING,
                        MesProEdhrWorkTaskStatus.OVERDUE)
                .orderByDesc(MesProEdhrWorkTaskDO::getId));
    }

    default List<MesProEdhrWorkTaskDO> selectActiveListByBatchExecutionId(Long batchExecutionId) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                .eq(MesProEdhrWorkTaskDO::getBatchExecutionId, batchExecutionId)
                .in(MesProEdhrWorkTaskDO::getStatus,
                        MesProEdhrWorkTaskStatus.TODO,
                        MesProEdhrWorkTaskStatus.DOING,
                        MesProEdhrWorkTaskStatus.OVERDUE)
                .orderByAsc(MesProEdhrWorkTaskDO::getId));
    }

    default List<MesProEdhrWorkTaskDO> selectActiveFillOrReworkList() {
        return selectList(new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                .in(MesProEdhrWorkTaskDO::getTaskType, "FILL", "REWORK")
                .in(MesProEdhrWorkTaskDO::getStatus,
                        MesProEdhrWorkTaskStatus.TODO,
                        MesProEdhrWorkTaskStatus.DOING,
                        MesProEdhrWorkTaskStatus.OVERDUE)
                .orderByAsc(MesProEdhrWorkTaskDO::getId));
    }

    default List<MesProEdhrWorkTaskDO> selectActiveListByExecutionAndBpmTaskId(Long executionId, String bpmTaskId) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                .eq(MesProEdhrWorkTaskDO::getExecutionId, executionId)
                .eq(MesProEdhrWorkTaskDO::getTaskType, "REVIEW")
                .eq(MesProEdhrWorkTaskDO::getBpmTaskId, bpmTaskId)
                .in(MesProEdhrWorkTaskDO::getStatus,
                        MesProEdhrWorkTaskStatus.TODO,
                        MesProEdhrWorkTaskStatus.DOING,
                        MesProEdhrWorkTaskStatus.OVERDUE)
                .orderByAsc(MesProEdhrWorkTaskDO::getId));
    }

    default MesProEdhrWorkTaskDO selectActiveReviewByExecutionAndBpmTaskId(Long executionId, String bpmTaskId) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                .eq(MesProEdhrWorkTaskDO::getExecutionId, executionId)
                .eq(MesProEdhrWorkTaskDO::getTaskType, "REVIEW")
                .eq(MesProEdhrWorkTaskDO::getBpmTaskId, bpmTaskId)
                .in(MesProEdhrWorkTaskDO::getStatus,
                        MesProEdhrWorkTaskStatus.TODO,
                        MesProEdhrWorkTaskStatus.DOING,
                        MesProEdhrWorkTaskStatus.OVERDUE)
                .orderByAsc(MesProEdhrWorkTaskDO::getId))
                .stream()
                .findFirst()
                .orElse(null);
    }

    default List<MesProEdhrWorkTaskDO> selectActiveCandidatePeers(Long executionId, String signatureCellKey,
                                                                  Long excludingWorkTaskId) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                .eq(MesProEdhrWorkTaskDO::getExecutionId, executionId)
                .eq(MesProEdhrWorkTaskDO::getTaskType, "REVIEW")
                .eq(MesProEdhrWorkTaskDO::getSignatureCellKey, signatureCellKey)
                .ne(MesProEdhrWorkTaskDO::getId, excludingWorkTaskId)
                .in(MesProEdhrWorkTaskDO::getStatus,
                        MesProEdhrWorkTaskStatus.TODO,
                        MesProEdhrWorkTaskStatus.DOING,
                        MesProEdhrWorkTaskStatus.OVERDUE)
                .orderByAsc(MesProEdhrWorkTaskDO::getId));
    }

    private LambdaQueryWrapperX<MesProEdhrWorkTaskDO> baseMyWrapper(MesProEdhrWorkTaskPageReqVO reqVO,
                                                                    Long assigneeUserId) {
        return new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                .eq(MesProEdhrWorkTaskDO::getAssigneeUserId, assigneeUserId)
                .eqIfPresent(MesProEdhrWorkTaskDO::getTaskType, reqVO.getTaskType())
                .likeIfPresent(MesProEdhrWorkTaskDO::getWorkOrderCode, reqVO.getWorkOrderCode())
                .likeIfPresent(MesProEdhrWorkTaskDO::getBatchCode, reqVO.getBatchCode())
                .likeIfPresent(MesProEdhrWorkTaskDO::getProcessName, reqVO.getProcessName());
    }

    private LambdaQueryWrapperX<MesProEdhrWorkTaskDO> baseApprovalCenterWrapper(MesProEdhrWorkTaskPageReqVO reqVO,
                                                                                Long assigneeUserId) {
        return new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                .eqIfPresent(MesProEdhrWorkTaskDO::getAssigneeUserId, assigneeUserId)
                .in(MesProEdhrWorkTaskDO::getTaskType, APPROVAL_CENTER_TASK_TYPES)
                .eqIfPresent(MesProEdhrWorkTaskDO::getTaskType, reqVO.getTaskType())
                .likeIfPresent(MesProEdhrWorkTaskDO::getWorkOrderCode, reqVO.getWorkOrderCode())
                .likeIfPresent(MesProEdhrWorkTaskDO::getBatchCode, reqVO.getBatchCode())
                .likeIfPresent(MesProEdhrWorkTaskDO::getProcessName, reqVO.getProcessName());
    }
}
