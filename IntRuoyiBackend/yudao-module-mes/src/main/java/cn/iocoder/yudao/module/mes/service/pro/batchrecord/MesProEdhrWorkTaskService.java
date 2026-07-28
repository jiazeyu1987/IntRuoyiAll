package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskArchiveRuleReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskAssignmentRuleRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskCloseRuleReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskReleaseApprovalRuleReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskStatsRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrProcessFormPermissionRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;

import java.time.LocalDateTime;
import java.util.List;

public interface MesProEdhrWorkTaskService {

    String TASK_TYPE_FILL = "FILL";
    String TASK_TYPE_REVIEW = "REVIEW";
    String TASK_TYPE_APPROVE = "APPROVE";
    String TASK_TYPE_REWORK = "REWORK";
    String TASK_TYPE_ARCHIVE = "ARCHIVE";
    String TASK_TYPE_CLOSE = "CLOSE";
    String TASK_TYPE_RELEASE_APPROVE = "RELEASE_APPROVE";

    PageResult<MesProEdhrWorkTaskRespVO> getMyPage(MesProEdhrWorkTaskPageReqVO reqVO);

    PageResult<MesProEdhrWorkTaskRespVO> getDonePage(MesProEdhrWorkTaskPageReqVO reqVO);

    PageResult<MesProEdhrWorkTaskRespVO> getApprovalCenterTodoPage(MesProEdhrWorkTaskPageReqVO reqVO,
                                                                   boolean globalView);

    PageResult<MesProEdhrWorkTaskRespVO> getApprovalCenterDonePage(MesProEdhrWorkTaskPageReqVO reqVO,
                                                                   boolean globalView);

    PageResult<MesProEdhrWorkTaskRespVO> getApprovalCenterCandidateSignatureTodoPage(MesProEdhrWorkTaskPageReqVO reqVO,
                                                                                      boolean globalView);

    Long countApprovalCenterTodoDuplicateTasks(MesProEdhrWorkTaskPageReqVO reqVO, boolean globalView);

    MesProEdhrWorkTaskStatsRespVO getStats();

    MesProEdhrWorkTaskAssignmentRuleRespVO getArchiveRuleByRoute(Long routeId);

    MesProEdhrWorkTaskAssignmentRuleRespVO saveArchiveRule(MesProEdhrWorkTaskArchiveRuleReqVO reqVO);

    MesProEdhrWorkTaskAssignmentRuleRespVO getCloseRuleByRoute(Long routeId);

    MesProEdhrWorkTaskAssignmentRuleRespVO saveCloseRule(MesProEdhrWorkTaskCloseRuleReqVO reqVO);

    MesProEdhrWorkTaskAssignmentRuleRespVO getReleaseApprovalRuleByRoute(Long routeId);

    MesProEdhrWorkTaskAssignmentRuleRespVO saveReleaseApprovalRule(MesProEdhrWorkTaskReleaseApprovalRuleReqVO reqVO);

    MesProEdhrWorkTaskDO validateWritableTask(Long workTaskId, Long executionId, String expectedTaskType);

    MesProEdhrWorkTaskDO validateWritableFillTaskForExecution(Long workTaskId, Long executionId);

    MesProEdhrWorkTaskDO validateGoldenFingerFillTaskForExecution(Long workTaskId, Long executionId);

    MesProEdhrWorkTaskDO validateWritableReviewOrApproveTask(Long workTaskId, Long executionId);

    MesProEdhrWorkTaskDO getAssignedTaskForDetail(Long workTaskId, Long executionId, String expectedTaskType);

    MesProEdhrWorkTaskDO getAssignedReviewOrApproveTaskForDetail(Long workTaskId, Long executionId);

    void createInitialFillTask(MesProEdhrBatchExecutionDO batch);

    void createArchiveTaskAfterBatchClose(MesProEdhrBatchExecutionDO batch);

    MesProEdhrWorkTaskDO createReleaseApprovalTaskAfterSubmit(MesProEdhrReleaseTransactionDO transaction,
                                                              MesProEdhrBatchExecutionDO batch);

    MesProEdhrWorkTaskDO validateArchiveTask(Long workTaskId, Long batchExecutionId);

    void completeArchiveTask(Long workTaskId, Long batchExecutionId);

    MesProEdhrWorkTaskDO validateReleaseApprovalTask(Long workTaskId, Long releaseTransactionId);

    void completeReleaseApprovalTask(Long workTaskId, Long releaseTransactionId, String result, String reason);

    void cancelReleaseApprovalTask(Long releaseTransactionId, String reason);

    void bindExecution(Long batchTaskId, Long executionId);

    void completeOptionalFillTaskBySkip(Long workTaskId, String reason);

    List<MesProEdhrWorkTaskDO> createReviewTasks(Long workTaskId, Long executionId,
                                                 List<MesProEdhrReviewTaskCreateCommand> reviewTasks);

    MesProEdhrWorkTaskDO completeFillAndCreateNextFillAfterOrdinarySubmit(Long workTaskId, Long executionId);

    MesProEdhrWorkTaskDO completeRouteFormFillAndCreateNextFill(Long batchTaskId, Long actorUserId);

    MesProEdhrWorkTaskDO completeFillAndCreateNextFillAfterGoldenFingerSubmit(Long workTaskId, Long executionId);

    MesProEdhrWorkTaskDO completeOneReviewTask(Long workTaskId, Long executionId);

    MesProEdhrWorkTaskDO completeCandidateSignatureTask(Long workTaskId, Long executionId);

    MesProEdhrWorkTaskDO validateWritableApproveTask(Long workTaskId, Long executionId);

    MesProEdhrWorkTaskDO completeApproveTask(Long workTaskId, Long executionId);

    void cancelPeerCandidateSignatureTasks(Long executionId, String signatureCellKey,
                                           Long excludingWorkTaskId, String reason);

    PageResult<MesProEdhrWorkTaskRespVO> getCandidateSignatureTodoPage(MesProEdhrWorkTaskPageReqVO reqVO);

    List<MesProEdhrWorkTaskDO> getApprovalTimelineTasks(Long workTaskId, Long executionId);

    List<MesProEdhrWorkTaskDO> getApprovalCenterTimelineTasks(Long workTaskId, Long executionId, boolean globalView);

    void cancelPendingReviewTasks(Long executionId, Long excludingWorkTaskId, String reason);

    void cancelActiveTasksByBatch(Long batchExecutionId, String reason);

    MesProEdhrWorkTaskDO reassignFillTask(Long workTaskId, String reason);

    void reconcileProcessFormFillTaskOwnership(String responsibilitySourceKey,
                                               MesProEdhrProcessFormPermissionRuleDO fillRule,
                                               String reason);

    boolean hasActiveReviewTasks(Long executionId);

    boolean hasActiveReviewTasksByBpmTaskId(Long executionId, String bpmTaskId);

    MesProEdhrWorkTaskDO getActiveReviewTaskByBpmTaskId(Long executionId, String bpmTaskId);

    void createNextFillAfterReview(MesProEdhrWorkTaskDO completedReviewTask);

    void createNextFillAfterSpecialNodeResolved(MesProEdhrBatchExecutionTaskDO specialTask);

    MesProEdhrWorkTaskDO createApproveTaskAfterReview(MesProEdhrWorkTaskDO completedReviewTask);

    Long requireReworkAssigneeUserId(Long workTaskId, Long executionId);

    MesProEdhrWorkTaskDO completeReviewAndCreateRework(Long workTaskId, Long rejectedExecutionId,
                                                       Long revisionExecutionId, String reason);

    MesProEdhrWorkTaskDO getActiveByExecutionAndType(Long executionId, String taskType);

    int processOverdueTasks(LocalDateTime now, int limit);

    MesProEdhrWorkTaskOverdueProcessResult processOverdueTasksWithSummary(LocalDateTime now, int limit);
}
