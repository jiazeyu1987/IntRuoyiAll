package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrCandidateResolver.MesProEdhrCandidateContract;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskArchiveRuleReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskAssignmentRuleRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskCloseRuleReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskReleaseApprovalRuleReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskStatsRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrProcessFormPermissionRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrProcessFormPermissionRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskStatus;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserReqDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.dto.SystemEntitlementRevokeReqDTO;
import cn.iocoder.yudao.module.system.api.permission.dto.SystemEntitlementSyncReqDTO;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.UNAUTHORIZED;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_EDHR_PROCESS_FORM_PERMISSION_RULE_VERSION_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_WRITE_TASK_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskErrorCodeConstants.PRO_EDHR_WORK_TASK_ADVANCE_PREREQUISITE_MISSING;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskErrorCodeConstants.PRO_EDHR_WORK_TASK_ASSIGNMENT_RULE_MISSING;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskErrorCodeConstants.PRO_EDHR_WORK_TASK_ASSIGNEE_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskErrorCodeConstants.PRO_EDHR_WORK_TASK_ASSIGNEE_MISMATCH;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskErrorCodeConstants.PRO_EDHR_WORK_TASK_CANDIDATE_POOL_EMPTY;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskErrorCodeConstants.PRO_EDHR_WORK_TASK_CANDIDATE_SOURCE_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskErrorCodeConstants.PRO_EDHR_WORK_TASK_DUE_RULE_MISSING;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskErrorCodeConstants.PRO_EDHR_WORK_TASK_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskErrorCodeConstants.PRO_EDHR_WORK_TASK_OWNERSHIP_SOURCE_MISSING;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskErrorCodeConstants.PRO_EDHR_WORK_TASK_OWNERSHIP_TRANSFER_LOCKED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskErrorCodeConstants.PRO_EDHR_WORK_TASK_REVIEW_USER_MISSING;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskErrorCodeConstants.PRO_EDHR_WORK_TASK_REVIEW_CONTEXT_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskErrorCodeConstants.PRO_EDHR_WORK_TASK_STATUS_INVALID;

@Service
public class MesProEdhrWorkTaskServiceImpl implements MesProEdhrWorkTaskService {

    private static final String BUSINESS_SCOPE_TYPE_BATCH_TASK = "BATCH_TASK";
    private static final String BUSINESS_SCOPE_TYPE_BATCH_ARCHIVE = "BATCH_ARCHIVE";
    private static final String BUSINESS_SCOPE_TYPE_RELEASE_TRANSACTION = "RELEASE_TRANSACTION";
    private static final String RULE_SCOPE_TYPE_ROUTE = "ROUTE";
    private static final String CANDIDATE_SOURCE_TYPE_USER = MesProEdhrCandidateResolver.CANDIDATE_SOURCE_TYPE_USER;
    private static final String CANDIDATE_SOURCE_TYPE_ROLE_GROUP = "ROLE_GROUP";
    private static final Set<String> SUPPORTED_CANDIDATE_SOURCE_TYPES =
            Set.of(CANDIDATE_SOURCE_TYPE_USER, "USER_GROUP", CANDIDATE_SOURCE_TYPE_ROLE_GROUP, "DEPT_GROUP");
    private static final String ENTITLEMENT_SOURCE_TYPE_FILLER = "EDHR_PROCESS_FORM_FILLER";
    private static final String ENTITLEMENT_SOURCE_TYPE_WORK_TASK = "EDHR_WORK_TASK_ASSIGNEE";
    private static final String ENTITLEMENT_POLICY_FILLER_MINIMAL = "MES_EDHR_FILLER_MINIMAL";
    private static final String ENTITLEMENT_POLICY_APPROVAL_REVIEWER_MINIMAL =
            "MES_EDHR_APPROVAL_REVIEWER_MINIMAL";
    private static final String ENTITLEMENT_POLICY_RELEASE_APPROVER_MINIMAL = "MES_EDHR_RELEASE_APPROVER_MINIMAL";

    @Resource
    private MesProEdhrWorkTaskMapper workTaskMapper;
    @Resource
    private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Resource
    private MesProEdhrWorkTaskAssignmentRuleMapper assignmentRuleMapper;
    @Resource
    private MesProEdhrBatchExecutionTaskMapper batchTaskMapper;
    @Resource
    private MesProEdhrProcessFormPermissionRuleMapper processFormPermissionRuleMapper;
    @Resource
    private NotifyMessageSendApi notifyMessageSendApi;
    @Resource
    private MesProRouteMapper routeMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private RoleApi roleApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private MesProEdhrCandidateResolver candidateResolver;
    @Resource
    private MesProRouteProcessService routeProcessService;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private MesProEdhrOperationAuditService operationAuditService;

    @Override
    public PageResult<MesProEdhrWorkTaskRespVO> getMyPage(MesProEdhrWorkTaskPageReqVO reqVO) {
        String status = StrUtil.blankToDefault(reqVO.getStatus(), MesProEdhrWorkTaskStatus.TODO);
        if (!Objects.equals(status, MesProEdhrWorkTaskStatus.TODO)
                && !Objects.equals(status, MesProEdhrWorkTaskStatus.OVERDUE)) {
            throw exception(PRO_EDHR_WORK_TASK_STATUS_INVALID);
        }
        PageResult<MesProEdhrWorkTaskDO> page = workTaskMapper.selectMyPage(reqVO, requireLoginUserId(), status);
        return buildWorkTaskRespPage(page, requireLoginUserId());
    }

    @Override
    public PageResult<MesProEdhrWorkTaskRespVO> getDonePage(MesProEdhrWorkTaskPageReqVO reqVO) {
        PageResult<MesProEdhrWorkTaskDO> page = workTaskMapper.selectDonePage(reqVO, requireLoginUserId());
        return buildWorkTaskRespPage(page, requireLoginUserId());
    }

    @Override
    public PageResult<MesProEdhrWorkTaskRespVO> getApprovalCenterTodoPage(MesProEdhrWorkTaskPageReqVO reqVO,
                                                                          boolean globalView) {
        Long assigneeUserId = globalView ? null : requireLoginUserId();
        String status = StrUtil.blankToDefault(reqVO.getStatus(), MesProEdhrWorkTaskStatus.TODO);
        if (!Objects.equals(status, MesProEdhrWorkTaskStatus.TODO)
                && !Objects.equals(status, MesProEdhrWorkTaskStatus.OVERDUE)) {
            throw exception(PRO_EDHR_WORK_TASK_STATUS_INVALID);
        }
        PageResult<MesProEdhrWorkTaskDO> page = workTaskMapper.selectApprovalCenterTodoPage(reqVO, assigneeUserId, status);
        return buildWorkTaskRespPage(page, requireLoginUserId());
    }

    @Override
    public PageResult<MesProEdhrWorkTaskRespVO> getApprovalCenterDonePage(MesProEdhrWorkTaskPageReqVO reqVO,
                                                                          boolean globalView) {
        Long assigneeUserId = globalView ? null : requireLoginUserId();
        PageResult<MesProEdhrWorkTaskDO> page = workTaskMapper.selectApprovalCenterDonePage(reqVO, assigneeUserId);
        return buildWorkTaskRespPage(page, requireLoginUserId());
    }

    @Override
    public PageResult<MesProEdhrWorkTaskRespVO> getCandidateSignatureTodoPage(MesProEdhrWorkTaskPageReqVO reqVO) {
        String status = StrUtil.blankToDefault(reqVO.getStatus(), MesProEdhrWorkTaskStatus.TODO);
        if (!Objects.equals(status, MesProEdhrWorkTaskStatus.TODO)
                && !Objects.equals(status, MesProEdhrWorkTaskStatus.OVERDUE)) {
            throw exception(PRO_EDHR_WORK_TASK_STATUS_INVALID);
        }
        PageResult<MesProEdhrWorkTaskDO> page =
                workTaskMapper.selectCandidateTodoPage(reqVO, requireLoginUserId(), status);
        return buildWorkTaskRespPage(page, requireLoginUserId());
    }

    @Override
    public PageResult<MesProEdhrWorkTaskRespVO> getApprovalCenterCandidateSignatureTodoPage(
            MesProEdhrWorkTaskPageReqVO reqVO, boolean globalView) {
        if (!globalView) {
            return getCandidateSignatureTodoPage(reqVO);
        }
        String status = StrUtil.blankToDefault(reqVO.getStatus(), MesProEdhrWorkTaskStatus.TODO);
        if (!Objects.equals(status, MesProEdhrWorkTaskStatus.TODO)
                && !Objects.equals(status, MesProEdhrWorkTaskStatus.OVERDUE)) {
            throw exception(PRO_EDHR_WORK_TASK_STATUS_INVALID);
        }
        PageResult<MesProEdhrWorkTaskDO> page = workTaskMapper.selectCandidateTodoPage(reqVO, null, status);
        return buildWorkTaskRespPage(page, requireLoginUserId());
    }

    @Override
    public Long countApprovalCenterTodoDuplicateTasks(MesProEdhrWorkTaskPageReqVO reqVO, boolean globalView) {
        String status = StrUtil.blankToDefault(reqVO.getStatus(), MesProEdhrWorkTaskStatus.TODO);
        if (!Objects.equals(status, MesProEdhrWorkTaskStatus.TODO)
                && !Objects.equals(status, MesProEdhrWorkTaskStatus.OVERDUE)) {
            throw exception(PRO_EDHR_WORK_TASK_STATUS_INVALID);
        }
        Long userId = globalView ? null : requireLoginUserId();
        return workTaskMapper.countApprovalCenterTodoDuplicateTasks(reqVO, userId, userId, status);
    }

    @Override
    public List<MesProEdhrWorkTaskDO> getApprovalTimelineTasks(Long workTaskId, Long executionId) {
        MesProEdhrWorkTaskDO sourceTask = requireTimelineSourceTask(workTaskId);
        ensureTimelineAccess(sourceTask);
        Long timelineExecutionId = executionId != null ? executionId : sourceTask.getExecutionId();
        List<MesProEdhrWorkTaskDO> tasks;
        if (timelineExecutionId != null) {
            tasks = workTaskMapper.selectTimelineListByExecutionId(timelineExecutionId);
        } else if (sourceTask.getBatchExecutionId() != null) {
            tasks = workTaskMapper.selectTimelineListByBatchExecutionId(sourceTask.getBatchExecutionId());
        } else {
            tasks = List.of(sourceTask);
        }
        Objects.requireNonNull(tasks, "APPROVAL_TIMELINE_SOURCE_REQUIRED: EDHR work task timeline list is required");
        if (tasks.isEmpty()) {
            throw new IllegalStateException("APPROVAL_TIMELINE_SOURCE_REQUIRED: EDHR work task timeline is empty");
        }
        return tasks;
    }

    @Override
    public List<MesProEdhrWorkTaskDO> getApprovalCenterTimelineTasks(Long workTaskId, Long executionId,
                                                                     boolean globalView) {
        MesProEdhrWorkTaskDO sourceTask = requireTimelineSourceTask(workTaskId);
        if (!globalView) {
            ensureTimelineAccess(sourceTask);
        }
        Long timelineExecutionId = executionId != null ? executionId : sourceTask.getExecutionId();
        List<MesProEdhrWorkTaskDO> tasks;
        if (timelineExecutionId != null) {
            tasks = workTaskMapper.selectTimelineListByExecutionId(timelineExecutionId);
        } else if (sourceTask.getBatchExecutionId() != null) {
            tasks = workTaskMapper.selectTimelineListByBatchExecutionId(sourceTask.getBatchExecutionId());
        } else {
            tasks = List.of(sourceTask);
        }
        Objects.requireNonNull(tasks, "APPROVAL_TIMELINE_SOURCE_REQUIRED: EDHR work task timeline list is required");
        if (tasks.isEmpty()) {
            throw new IllegalStateException("APPROVAL_TIMELINE_SOURCE_REQUIRED: EDHR work task timeline is empty");
        }
        return tasks;
    }

    @Override
    public MesProEdhrWorkTaskStatsRespVO getStats() {
        Long userId = requireLoginUserId();
        return new MesProEdhrWorkTaskStatsRespVO()
                .setTodoCount(workTaskMapper.countMy(userId, null, MesProEdhrWorkTaskStatus.TODO))
                .setFillCount(workTaskMapper.countMy(userId, TASK_TYPE_FILL, MesProEdhrWorkTaskStatus.TODO))
                .setReviewCount(workTaskMapper.countMy(userId, TASK_TYPE_REVIEW, MesProEdhrWorkTaskStatus.TODO))
                .setReworkCount(workTaskMapper.countMy(userId, TASK_TYPE_REWORK, MesProEdhrWorkTaskStatus.TODO))
                .setArchiveCount(workTaskMapper.countMy(userId, TASK_TYPE_ARCHIVE, MesProEdhrWorkTaskStatus.TODO))
                .setOverdueCount(workTaskMapper.countMy(userId, null, MesProEdhrWorkTaskStatus.OVERDUE))
                .setDoneCount(workTaskMapper.countMy(userId, null, MesProEdhrWorkTaskStatus.DONE));
    }

    @Override
    public MesProEdhrWorkTaskAssignmentRuleRespVO getArchiveRuleByRoute(Long routeId) {
        return getRouteRuleByRoute(routeId, TASK_TYPE_ARCHIVE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrWorkTaskAssignmentRuleRespVO saveArchiveRule(MesProEdhrWorkTaskArchiveRuleReqVO reqVO) {
        saveRouteRule(reqVO.getRouteId(), reqVO.getAssigneeUserId(), reqVO.getDueMinutes(),
                reqVO.getEnabled(), reqVO.getRemark(), TASK_TYPE_ARCHIVE);
        return getArchiveRuleByRoute(reqVO.getRouteId());
    }

    @Override
    public MesProEdhrWorkTaskAssignmentRuleRespVO getCloseRuleByRoute(Long routeId) {
        return getRouteRuleByRoute(routeId, TASK_TYPE_CLOSE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrWorkTaskAssignmentRuleRespVO saveCloseRule(MesProEdhrWorkTaskCloseRuleReqVO reqVO) {
        saveRouteRule(reqVO.getRouteId(), reqVO.getAssigneeUserId(), reqVO.getDueMinutes(),
                reqVO.getEnabled(), reqVO.getRemark(), TASK_TYPE_CLOSE);
        return getCloseRuleByRoute(reqVO.getRouteId());
    }

    @Override
    public MesProEdhrWorkTaskAssignmentRuleRespVO getReleaseApprovalRuleByRoute(Long routeId) {
        return getRouteRuleByRoute(routeId, TASK_TYPE_RELEASE_APPROVE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrWorkTaskAssignmentRuleRespVO saveReleaseApprovalRule(
            MesProEdhrWorkTaskReleaseApprovalRuleReqVO reqVO) {
        saveReleaseApprovalRuleCandidate(reqVO.getRouteId(), reqVO.getCandidateSourceType(),
                reqVO.getCandidateSourceId(), reqVO.getEnabled(), reqVO.getRemark());
        return getReleaseApprovalRuleByRoute(reqVO.getRouteId());
    }

    private MesProEdhrWorkTaskAssignmentRuleRespVO getRouteRuleByRoute(Long routeId, String taskType) {
        validateRouteExists(routeId);
        MesProEdhrWorkTaskAssignmentRuleDO rule =
                assignmentRuleMapper.selectByScopeAndType(RULE_SCOPE_TYPE_ROUTE, routeId, taskType);
        return BeanUtils.toBean(rule, MesProEdhrWorkTaskAssignmentRuleRespVO.class);
    }

    private void saveRouteRule(Long routeId, Long assigneeUserId, Integer dueMinutes, Boolean enabled,
                               String remark, String taskType) {
        saveRouteCandidateRule(routeId, CANDIDATE_SOURCE_TYPE_USER, assigneeUserId, assigneeUserId,
                dueMinutes, enabled, remark, taskType);
    }

    private void saveReleaseApprovalRuleCandidate(Long routeId, String candidateSourceType, Long candidateSourceId,
                                                  Boolean enabled, String remark) {
        String normalizedSourceType = normalizeReleaseApprovalCandidateSourceType(candidateSourceType);
        Long assigneeUserId = CANDIDATE_SOURCE_TYPE_USER.equals(normalizedSourceType) ? candidateSourceId : null;
        saveRouteCandidateRule(routeId, normalizedSourceType, candidateSourceId, assigneeUserId,
                null, enabled, remark, TASK_TYPE_RELEASE_APPROVE);
    }

    private void saveRouteCandidateRule(Long routeId, String candidateSourceType, Long candidateSourceId,
                                        Long assigneeUserId, Integer dueMinutes, Boolean enabled,
                                        String remark, String taskType) {
        validateRouteExists(routeId);
        validateRouteCandidateSource(candidateSourceType, candidateSourceId);
        List<MesProEdhrWorkTaskAssignmentRuleDO> existingRules =
                assignmentRuleMapper.selectListByScopeAndType(RULE_SCOPE_TYPE_ROUTE, routeId, taskType);
        MesProEdhrWorkTaskAssignmentRuleDO rule = new MesProEdhrWorkTaskAssignmentRuleDO()
                .setRouteProcessId(null)
                .setScopeType(RULE_SCOPE_TYPE_ROUTE)
                .setScopeId(routeId)
                .setTaskType(taskType)
                .setReviewUserId(null)
                .setAssigneeUserId(assigneeUserId)
                .setCandidateSourceType(candidateSourceType)
                .setCandidateSourceId(candidateSourceId)
                .setDueMinutes(dueMinutes)
                .setEnabled(enabled)
                .setRemark(remark);
        validateCandidateSource(rule);
        if (existingRules.isEmpty()) {
            assignmentRuleMapper.insert(rule);
        } else {
            for (MesProEdhrWorkTaskAssignmentRuleDO existingRule : existingRules) {
                assignmentRuleMapper.updateById(new MesProEdhrWorkTaskAssignmentRuleDO()
                        .setId(existingRule.getId())
                        .setRouteProcessId(rule.getRouteProcessId())
                        .setScopeType(rule.getScopeType())
                        .setScopeId(rule.getScopeId())
                        .setTaskType(rule.getTaskType())
                        .setReviewUserId(rule.getReviewUserId())
                        .setAssigneeUserId(rule.getAssigneeUserId())
                        .setCandidateSourceType(rule.getCandidateSourceType())
                        .setCandidateSourceId(rule.getCandidateSourceId())
                        .setDueMinutes(rule.getDueMinutes())
                        .setEnabled(rule.getEnabled())
                        .setRemark(rule.getRemark()));
            }
            rule.setId(existingRules.get(0).getId());
        }
        recordWorkTaskRuleSaveAudit(routeId, taskType, existingRules,
                assignmentRuleMapper.selectListByScopeAndType(RULE_SCOPE_TYPE_ROUTE, routeId, taskType), remark);
    }

    @Override
    public MesProEdhrWorkTaskDO validateWritableTask(Long workTaskId, Long executionId, String expectedTaskType) {
        MesProEdhrWorkTaskDO workTask = workTaskMapper.selectById(workTaskId);
        validateAssignedTask(workTask, executionId, expectedTaskType);
        if (!isProcessableStatus(workTask.getStatus())) {
            throw exception(PRO_EDHR_WORK_TASK_STATUS_INVALID);
        }
        return workTask;
    }

    @Override
    public MesProEdhrWorkTaskDO validateWritableFillTaskForExecution(Long workTaskId, Long executionId) {
        MesProEdhrWorkTaskDO workTask = workTaskMapper.selectById(workTaskId);
        if (workTask == null) {
            throw exception(PRO_EDHR_WORK_TASK_NOT_EXISTS);
        }
        boolean fillTaskType = Objects.equals(workTask.getTaskType(), TASK_TYPE_FILL)
                || Objects.equals(workTask.getTaskType(), TASK_TYPE_REWORK);
        if (!fillTaskType) {
            throw exception(PRO_EDHR_WORK_TASK_STATUS_INVALID);
        }
        if (!Objects.equals(workTask.getExecutionId(), executionId)) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_WRITE_TASK_INVALID, "非当前活动表单，禁止写入");
        }
        if (!isAssignedOrCandidate(workTask, requireLoginUserId())) {
            throw exception(PRO_EDHR_WORK_TASK_ASSIGNEE_MISMATCH);
        }
        if (!isProcessableStatus(workTask.getStatus())) {
            throw exception(PRO_EDHR_WORK_TASK_STATUS_INVALID);
        }
        return workTask;
    }

    @Override
    public MesProEdhrWorkTaskDO validateGoldenFingerFillTaskForExecution(Long workTaskId, Long executionId) {
        MesProEdhrWorkTaskDO workTask = workTaskMapper.selectById(workTaskId);
        if (workTask == null || !Objects.equals(workTask.getExecutionId(), executionId)) {
            throw exception(PRO_EDHR_WORK_TASK_NOT_EXISTS);
        }
        boolean fillTaskType = Objects.equals(workTask.getTaskType(), TASK_TYPE_FILL)
                || Objects.equals(workTask.getTaskType(), TASK_TYPE_REWORK);
        if (!fillTaskType || !isProcessableStatus(workTask.getStatus())) {
            throw exception(PRO_EDHR_WORK_TASK_STATUS_INVALID);
        }
        return workTask;
    }

    @Override
    public MesProEdhrWorkTaskDO validateWritableReviewOrApproveTask(Long workTaskId, Long executionId) {
        MesProEdhrWorkTaskDO workTask = workTaskMapper.selectById(workTaskId);
        if (workTask == null || !Objects.equals(workTask.getExecutionId(), executionId)) {
            throw exception(PRO_EDHR_WORK_TASK_NOT_EXISTS);
        }
        if (!isAssignedOrCandidate(workTask, requireLoginUserId())) {
            throw exception(PRO_EDHR_WORK_TASK_ASSIGNEE_MISMATCH);
        }
        boolean approvalActionTask = Objects.equals(workTask.getTaskType(), TASK_TYPE_REVIEW)
                || Objects.equals(workTask.getTaskType(), TASK_TYPE_APPROVE);
        if (!approvalActionTask || !isProcessableStatus(workTask.getStatus())) {
            throw exception(PRO_EDHR_WORK_TASK_STATUS_INVALID);
        }
        return workTask;
    }

    @Override
    public MesProEdhrWorkTaskDO getAssignedTaskForDetail(Long workTaskId, Long executionId, String expectedTaskType) {
        MesProEdhrWorkTaskDO workTask = workTaskMapper.selectById(workTaskId);
        validateAssignedTask(workTask, executionId, expectedTaskType);
        return workTask;
    }

    @Override
    public MesProEdhrWorkTaskDO getAssignedReviewOrApproveTaskForDetail(Long workTaskId, Long executionId) {
        MesProEdhrWorkTaskDO workTask = workTaskMapper.selectById(workTaskId);
        validateAssignedTask(workTask, executionId, TASK_TYPE_REVIEW, TASK_TYPE_APPROVE);
        return workTask;
    }

    private void validateAssignedTask(MesProEdhrWorkTaskDO workTask, Long executionId, String expectedTaskType) {
        validateAssignedTask(workTask, executionId, new String[]{expectedTaskType});
    }

    private void validateAssignedTask(MesProEdhrWorkTaskDO workTask, Long executionId, String... expectedTaskTypes) {
        if (workTask == null) {
            throw exception(PRO_EDHR_WORK_TASK_NOT_EXISTS);
        }
        if (!Objects.equals(workTask.getExecutionId(), executionId)) {
            throw exception(PRO_EDHR_WORK_TASK_NOT_EXISTS);
        }
        if (!isAssignedOrCandidate(workTask, requireLoginUserId())) {
            throw exception(PRO_EDHR_WORK_TASK_ASSIGNEE_MISMATCH);
        }
        if (Arrays.stream(expectedTaskTypes).noneMatch(expectedTaskType -> Objects.equals(workTask.getTaskType(), expectedTaskType))) {
            throw exception(PRO_EDHR_WORK_TASK_STATUS_INVALID);
        }
    }

    private PageResult<MesProEdhrWorkTaskRespVO> buildWorkTaskRespPage(PageResult<MesProEdhrWorkTaskDO> page,
                                                                       Long currentUserId) {
        List<MesProEdhrWorkTaskDO> list = page.getList();
        if (list == null || list.isEmpty()) {
            return new PageResult<>(List.of(), page.getTotal());
        }
        Set<Long> userIds = new LinkedHashSet<>();
        Set<Long> roleIds = new LinkedHashSet<>();
        Set<Long> deptIds = new LinkedHashSet<>();
        list.forEach(task -> {
            if (task.getAssigneeUserId() != null) {
                userIds.add(task.getAssigneeUserId());
            }
            if (task.getSourceUserId() != null) {
                userIds.add(task.getSourceUserId());
            }
            if (Objects.equals(task.getCandidateSourceType(), "USER") && task.getCandidateSourceId() != null) {
                userIds.add(task.getCandidateSourceId());
            }
            collectCandidateSnapshotUserIds(task.getCandidateUserSnapshot(), userIds);
            if (Objects.equals(task.getCandidateSourceType(), "ROLE_GROUP") && task.getCandidateSourceId() != null) {
                roleIds.add(task.getCandidateSourceId());
            }
            if (Objects.equals(task.getCandidateSourceType(), "DEPT_GROUP") && task.getCandidateSourceId() != null) {
                deptIds.add(task.getCandidateSourceId());
            }
        });
        Map<Long, AdminUserRespDTO> userMap = userIds.isEmpty() ? Map.of() : adminUserApi.getUserMap(userIds);
        Map<Long, String> roleNameMap = buildRoleNameMap(roleIds);
        Map<Long, DeptRespDTO> deptMap = deptIds.isEmpty() ? Map.of() : deptApi.getDeptMap(deptIds);
        List<MesProEdhrWorkTaskRespVO> rows = list.stream()
                .map(task -> toWorkTaskResp(task, currentUserId, userMap, roleNameMap, deptMap))
                .toList();
        return new PageResult<>(rows, page.getTotal());
    }

    private MesProEdhrWorkTaskRespVO toWorkTaskResp(MesProEdhrWorkTaskDO task,
                                                    Long currentUserId,
                                                    Map<Long, AdminUserRespDTO> userMap,
                                                    Map<Long, String> roleNameMap,
                                                    Map<Long, DeptRespDTO> deptMap) {
        MesProEdhrWorkTaskRespVO respVO = BeanUtils.toBean(task, MesProEdhrWorkTaskRespVO.class);
        respVO.setAssigneeUserName(resolveUserName(userMap, task.getAssigneeUserId()));
        respVO.setSourceUserName(resolveUserName(userMap, task.getSourceUserId()));
        respVO.setCandidatePoolName(resolveCandidatePoolName(task, userMap, roleNameMap, deptMap));
        respVO.setCandidateSnapshotDisplay(resolveCandidateSnapshotDisplay(task, userMap));
        respVO.setResponsibilitySource(resolveResponsibilitySource(task));
        respVO.setInactionReason(resolveInactionReason(task, currentUserId));
        return respVO;
    }

    private void collectCandidateSnapshotUserIds(String candidateUserSnapshot, Set<Long> userIds) {
        userIds.addAll(MesProEdhrWorkTaskAuthorization.parseCandidateSnapshotUserIds(candidateUserSnapshot));
    }

    private String resolveUserName(Map<Long, AdminUserRespDTO> userMap, Long userId) {
        if (userId == null) {
            return null;
        }
        AdminUserRespDTO user = userMap.get(userId);
        if (user == null) {
            return String.valueOf(userId);
        }
        return StrUtil.blankToDefault(user.getNickname(), String.valueOf(userId));
    }

    private Map<Long, String> buildRoleNameMap(Set<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Map.of();
        }
        List<?> roles = roleApi.getRoleList(roleIds);
        if (roles == null || roles.isEmpty()) {
            return Map.of();
        }
        return roles.stream()
                .filter(Objects::nonNull)
                .map(role -> Map.entry(
                        parseLongSafely(toDisplayString(ReflectUtil.getFieldValue(role, "id"))),
                        toDisplayString(ReflectUtil.getFieldValue(role, "name"))))
                .filter(entry -> entry.getKey() != null && StrUtil.isNotBlank(entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left));
    }

    private String toDisplayString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String resolveCandidatePoolName(MesProEdhrWorkTaskDO task,
                                            Map<Long, AdminUserRespDTO> userMap,
                                            Map<Long, String> roleNameMap,
                                            Map<Long, DeptRespDTO> deptMap) {
        if (StrUtil.isBlank(task.getCandidateSourceType())) {
            return null;
        }
        if (Objects.equals(task.getCandidateSourceType(), CANDIDATE_SOURCE_TYPE_USER)) {
            return resolveUserName(userMap, task.getCandidateSourceId() != null
                    ? task.getCandidateSourceId() : task.getAssigneeUserId());
        }
        if (Objects.equals(task.getCandidateSourceType(), "ROLE_GROUP")) {
            String roleName = roleNameMap.get(task.getCandidateSourceId());
            return StrUtil.blankToDefault(roleName, String.valueOf(task.getCandidateSourceId()));
        }
        if (Objects.equals(task.getCandidateSourceType(), "DEPT_GROUP")) {
            DeptRespDTO dept = deptMap.get(task.getCandidateSourceId());
            return dept == null ? String.valueOf(task.getCandidateSourceId()) : dept.getName();
        }
        return String.valueOf(task.getCandidateSourceId());
    }

    private String resolveCandidateSnapshotDisplay(MesProEdhrWorkTaskDO task,
                                                   Map<Long, AdminUserRespDTO> userMap) {
        if (StrUtil.isBlank(task.getCandidateUserSnapshot())) {
            return null;
        }
        List<String> labels = new ArrayList<>();
        for (Long userId : MesProEdhrWorkTaskAuthorization.parseCandidateSnapshotUserIds(
                task.getCandidateUserSnapshot())) {
            String token = String.valueOf(userId);
            String userName = resolveUserName(userMap, userId);
            labels.add(Objects.equals(userName, token) ? token : userName + "(" + userId + ")");
        }
        return labels.isEmpty() ? null : String.join("，", labels);
    }

    private Long parseLongSafely(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String resolveResponsibilitySource(MesProEdhrWorkTaskDO task) {
        if (TASK_TYPE_RELEASE_APPROVE.equals(task.getTaskType())) {
            return "路线放行规则";
        }
        if (TASK_TYPE_ARCHIVE.equals(task.getTaskType())) {
            return "路线归档规则";
        }
        if (TASK_TYPE_CLOSE.equals(task.getTaskType())) {
            return "路线关闭规则";
        }
        if (TASK_TYPE_APPROVE.equals(task.getTaskType())) {
            return task.getCandidateSourceId() != null ? "候选批准池派发" : "批准直派";
        }
        if (TASK_TYPE_REWORK.equals(task.getTaskType())) {
            return task.getSourceUserId() != null ? "驳回返工回退" : "返工修订任务";
        }
        if (TASK_TYPE_REVIEW.equals(task.getTaskType())) {
            return task.getCandidateSourceId() != null ? "候选审核池派发" : "审核直派";
        }
        return "工序填写责任";
    }

    private String resolveInactionReason(MesProEdhrWorkTaskDO task, Long currentUserId) {
        if (currentUserId == null) {
            return "当前未登录，无法处理任务";
        }
        if (Objects.equals(task.getStatus(), MesProEdhrWorkTaskStatus.DONE)) {
            return "任务已完成，当前用户不可重复处理";
        }
        if (Objects.equals(task.getAssigneeUserId(), currentUserId)) {
            return "当前用户为直接责任人，可处理";
        }
        if (MesProEdhrWorkTaskAuthorization.containsCandidate(task.getCandidateUserSnapshot(), currentUserId)) {
            return "当前用户在候选池中，需按候选审核路径处理";
        }
        if (StrUtil.isNotBlank(task.getCandidateUserSnapshot())) {
            return "当前用户不在候选池中，不能处理该任务";
        }
        if (task.getAssigneeUserId() != null) {
            return "当前用户不是该任务责任人，不能处理";
        }
        return "任务责任归属缺失，当前用户不可处理";
    }

    private MesProEdhrWorkTaskDO requireTimelineSourceTask(Long workTaskId) {
        if (workTaskId == null) {
            throw new IllegalStateException("APPROVAL_BUSINESS_KEY_REQUIRED: eDHR work task id is required");
        }
        MesProEdhrWorkTaskDO workTask = workTaskMapper.selectById(workTaskId);
        if (workTask == null) {
            throw new IllegalStateException("APPROVAL_BUSINESS_OBJECT_REQUIRED: eDHR work task not found " + workTaskId);
        }
        return workTask;
    }

    private void ensureTimelineAccess(MesProEdhrWorkTaskDO sourceTask) {
        Long loginUserId = requireLoginUserId();
        if (Objects.equals(sourceTask.getAssigneeUserId(), loginUserId)) {
            return;
        }
        if (Objects.equals(sourceTask.getSourceUserId(), loginUserId)) {
            return;
        }
        if (hasCandidateUser(sourceTask.getCandidateUserSnapshot(), loginUserId)) {
            return;
        }
        throw new IllegalStateException("APPROVAL_TIMELINE_ACCESS_DENIED: EDHR timeline is not visible to login user");
    }

    private static boolean hasCandidateUser(String snapshot, Long loginUserId) {
        if (snapshot == null || snapshot.isBlank() || loginUserId == null) {
            return false;
        }
        String token = "," + loginUserId + ",";
        return ("," + snapshot + ",").contains(token);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createInitialFillTask(MesProEdhrBatchExecutionDO batch) {
        MesProEdhrBatchExecutionTaskDO firstTask = batchTaskMapper.selectListByBatchExecutionId(batch.getId()).stream()
                .filter(task -> !Boolean.FALSE.equals(task.getRequiredFlag()))
                .filter(task -> Objects.equals(task.getStatus(), MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_WAITING))
                .filter(task -> task.getRouteProcessId() != null)
                .min(Comparator.comparing(MesProEdhrBatchExecutionTaskDO::getRouteProcessSort)
                        .thenComparing(MesProEdhrBatchExecutionTaskDO::getId))
                .orElse(null);
        if (firstTask == null) {
            return;
        }
        MesProRouteProcessDO currentRouteProcess = resolveFrozenRouteProcess(batch == null ? null : batch.getRouteId(),
                firstTask.getRouteProcessId(), firstTask.getProcessId());
        MesProEdhrProcessFormPermissionRuleDO processFormRule = findProcessFormFillRule(currentRouteProcess, firstTask);
        MesProEdhrWorkTaskAssignmentRuleDO rule = processFormRule == null
                ? findRule(currentRouteProcess, TASK_TYPE_FILL) : null;
        List<String> missingItems = collectAdvancePrerequisiteNames(batch, firstTask, processFormRule, rule, null);
        if (!missingItems.isEmpty()) {
            if (missingItems.size() == 1 && missingItems.contains("权限")) {
                return;
            }
            throw exception(PRO_EDHR_WORK_TASK_ADVANCE_PREREQUISITE_MISSING,
                    String.join("、", missingItems));
        }
        if ((processFormRule == null || !hasProcessFormCandidateSource(processFormRule))
                && (rule == null || !hasCandidateSource(rule))) {
            return;
        }
        createFillTask(batch, firstTask, null, null);
        createOptionalCompanionFillTasks(batch, firstTask, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createArchiveTaskAfterBatchClose(MesProEdhrBatchExecutionDO batch) {
        MesProEdhrWorkTaskDO existing = workTaskMapper.selectActiveByBusinessScopeAndType(
                BUSINESS_SCOPE_TYPE_BATCH_ARCHIVE, batch.getId(), TASK_TYPE_ARCHIVE);
        if (existing != null) {
            return;
        }
        MesProEdhrWorkTaskAssignmentRuleDO rule = requireRouteRule(batch.getRouteId(), TASK_TYPE_ARCHIVE);
        MesProEdhrCandidateContract candidate = candidateResolver.resolveAssignmentRule(rule);
        MesProEdhrWorkTaskDO task = new MesProEdhrWorkTaskDO()
                .setTaskType(TASK_TYPE_ARCHIVE)
                .setBatchExecutionId(batch.getId())
                .setBusinessScopeType(BUSINESS_SCOPE_TYPE_BATCH_ARCHIVE)
                .setBusinessScopeId(batch.getId())
                .setWorkOrderId(batch.getWorkOrderId())
                .setWorkOrderCode(batch.getWorkOrderCode())
                .setBatchCode(batch.getBatchCode())
                .setRouteId(batch.getRouteId())
                .setProcessName("最终归档")
                .setAssigneeUserId(resolveCandidateAssigneeUserId(candidate, rule.getAssigneeUserId()))
                .setCandidateSourceType(candidate.sourceType())
                .setCandidateSourceId(candidate.sourceId())
                .setCandidateUserSnapshot(candidate.userSnapshot())
                .setSourceUserId(requireLoginUserId())
                .setRemark("eDHR最终归档任务");
        createTask(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrWorkTaskDO createReleaseApprovalTaskAfterSubmit(MesProEdhrReleaseTransactionDO transaction,
                                                                     MesProEdhrBatchExecutionDO batch) {
        if (transaction == null || transaction.getId() == null || batch == null || batch.getId() == null) {
            throw exception(PRO_EDHR_WORK_TASK_NOT_EXISTS);
        }
        MesProEdhrWorkTaskDO existing = workTaskMapper.selectActiveByBusinessScopeAndType(
                BUSINESS_SCOPE_TYPE_RELEASE_TRANSACTION, transaction.getId(), TASK_TYPE_RELEASE_APPROVE);
        if (existing != null) {
            return existing;
        }
        MesProEdhrWorkTaskAssignmentRuleDO rule = requireRouteRule(batch.getRouteId(), TASK_TYPE_RELEASE_APPROVE);
        MesProEdhrCandidateContract candidate = candidateResolver.resolveAssignmentRule(rule);
        MesProEdhrWorkTaskDO task = new MesProEdhrWorkTaskDO()
                .setTaskType(TASK_TYPE_RELEASE_APPROVE)
                .setBatchExecutionId(batch.getId())
                .setBusinessScopeType(BUSINESS_SCOPE_TYPE_RELEASE_TRANSACTION)
                .setBusinessScopeId(transaction.getId())
                .setWorkOrderId(batch.getWorkOrderId())
                .setWorkOrderCode(batch.getWorkOrderCode())
                .setBatchCode(batch.getBatchCode())
                .setRouteId(batch.getRouteId())
                .setProcessName("最终放行审批")
                .setAssigneeUserId(resolveCandidateAssigneeUserId(candidate, rule.getAssigneeUserId()))
                .setCandidateSourceType(candidate.sourceType())
                .setCandidateSourceId(candidate.sourceId())
                .setCandidateUserSnapshot(candidate.userSnapshot())
                .setSourceUserId(requireLoginUserId())
                .setResponsibilitySourceType(ENTITLEMENT_SOURCE_TYPE_WORK_TASK)
                .setResponsibilitySourceKey("RELEASE_TRANSACTION|" + transaction.getId())
                .setResponsibilitySourceVersion(String.valueOf(transaction.getId()))
                .setResponsibilitySourceDigest("releaseStatus=" + transaction.getReleaseStatus()
                        + ";batchExecutionId=" + transaction.getBatchExecutionId())
                .setRemark("eDHR最终放行审批任务");
        return createTask(task);
    }

    @Override
    public MesProEdhrWorkTaskDO validateArchiveTask(Long workTaskId, Long batchExecutionId) {
        MesProEdhrWorkTaskDO workTask = workTaskMapper.selectById(workTaskId);
        if (workTask == null) {
            throw exception(PRO_EDHR_WORK_TASK_NOT_EXISTS);
        }
        if (!Objects.equals(workTask.getBatchExecutionId(), batchExecutionId)
                || !Objects.equals(workTask.getBusinessScopeType(), BUSINESS_SCOPE_TYPE_BATCH_ARCHIVE)
                || !Objects.equals(workTask.getBusinessScopeId(), batchExecutionId)) {
            throw exception(PRO_EDHR_WORK_TASK_NOT_EXISTS);
        }
        if (!Objects.equals(workTask.getAssigneeUserId(), requireLoginUserId())) {
            throw exception(PRO_EDHR_WORK_TASK_ASSIGNEE_MISMATCH);
        }
        if (!Objects.equals(workTask.getTaskType(), TASK_TYPE_ARCHIVE) || !isProcessableStatus(workTask.getStatus())) {
            throw exception(PRO_EDHR_WORK_TASK_STATUS_INVALID);
        }
        return workTask;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeArchiveTask(Long workTaskId, Long batchExecutionId) {
        completeTask(validateArchiveTask(workTaskId, batchExecutionId));
    }

    @Override
    public MesProEdhrWorkTaskDO validateReleaseApprovalTask(Long workTaskId, Long releaseTransactionId) {
        MesProEdhrWorkTaskDO workTask = resolveReleaseApprovalTask(workTaskId, releaseTransactionId);
        if (!isAssignedOrCandidate(workTask, requireLoginUserId())) {
            throw exception(PRO_EDHR_WORK_TASK_ASSIGNEE_MISMATCH);
        }
        if (!isProcessableStatus(workTask.getStatus())) {
            throw exception(PRO_EDHR_WORK_TASK_STATUS_INVALID);
        }
        return workTask;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeReleaseApprovalTask(Long workTaskId, Long releaseTransactionId, String result, String reason) {
        MesProEdhrWorkTaskDO workTask = validateReleaseApprovalTask(workTaskId, releaseTransactionId);
        completeTask(workTask, result, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelReleaseApprovalTask(Long releaseTransactionId, String reason) {
        MesProEdhrWorkTaskDO workTask = resolveReleaseApprovalTask(null, releaseTransactionId);
        workTaskMapper.updateById(new MesProEdhrWorkTaskDO()
                .setId(workTask.getId())
                .setStatus(MesProEdhrWorkTaskStatus.CANCELED)
                .setReason(reason)
                .setRemark(reason)
                .setCompletedAt(LocalDateTime.now()));
        revokeRuntimeTaskEntitlement(workTask);
    }

    private MesProEdhrWorkTaskDO resolveReleaseApprovalTask(Long workTaskId, Long releaseTransactionId) {
        if (workTaskId == null && releaseTransactionId == null) {
            throw exception(PRO_EDHR_WORK_TASK_NOT_EXISTS);
        }
        MesProEdhrWorkTaskDO workTask = workTaskId == null
                ? workTaskMapper.selectActiveByBusinessScopeAndType(BUSINESS_SCOPE_TYPE_RELEASE_TRANSACTION,
                releaseTransactionId, TASK_TYPE_RELEASE_APPROVE)
                : workTaskMapper.selectById(workTaskId);
        if (workTask == null) {
            throw exception(PRO_EDHR_WORK_TASK_NOT_EXISTS);
        }
        boolean scopeMatches = Objects.equals(workTask.getTaskType(), TASK_TYPE_RELEASE_APPROVE)
                && Objects.equals(workTask.getBusinessScopeType(), BUSINESS_SCOPE_TYPE_RELEASE_TRANSACTION)
                && workTask.getBusinessScopeId() != null
                && (releaseTransactionId == null || Objects.equals(workTask.getBusinessScopeId(), releaseTransactionId));
        if (!scopeMatches) {
            throw exception(PRO_EDHR_WORK_TASK_NOT_EXISTS);
        }
        return workTask;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindExecution(Long batchTaskId, Long executionId) {
        MesProEdhrWorkTaskDO fillTask = workTaskMapper.selectActiveByBatchTaskAndType(batchTaskId, TASK_TYPE_FILL);
        if (fillTask == null) {
            fillTask = workTaskMapper.selectActiveByBatchTaskAndType(batchTaskId, TASK_TYPE_REWORK);
        }
        if (fillTask == null) {
            return;
        }
        workTaskMapper.updateById(new MesProEdhrWorkTaskDO()
                .setId(fillTask.getId())
                .setExecutionId(executionId)
                .setActionUrl(buildExecutionActionUrl(fillTask, executionId)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeOptionalFillTaskBySkip(Long workTaskId, String reason) {
        MesProEdhrWorkTaskDO workTask = workTaskMapper.selectById(workTaskId);
        if (workTask == null) {
            throw exception(PRO_EDHR_WORK_TASK_NOT_EXISTS);
        }
        boolean fillOrReworkTask = TASK_TYPE_FILL.equals(workTask.getTaskType())
                || TASK_TYPE_REWORK.equals(workTask.getTaskType());
        if (!fillOrReworkTask || !isActiveFillOrReworkStatus(workTask.getStatus())) {
            throw exception(PRO_EDHR_WORK_TASK_STATUS_INVALID);
        }
        completeTask(workTask, "OPTIONAL_SKIP", reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<MesProEdhrWorkTaskDO> createReviewTasks(Long workTaskId, Long executionId,
                                                        List<MesProEdhrReviewTaskCreateCommand> reviewTasks) {
        MesProEdhrWorkTaskDO workTask = validateSubmitTask(workTaskId, executionId);
        if (reviewTasks == null || reviewTasks.isEmpty()) {
            throw exception(PRO_EDHR_WORK_TASK_REVIEW_USER_MISSING);
        }
        Set<String> reviewTaskKeys = new LinkedHashSet<>();
        for (MesProEdhrReviewTaskCreateCommand reviewTask : reviewTasks) {
            validateReviewTaskCreateCommand(reviewTask, reviewTaskKeys);
        }
        completeTask(workTask);
        List<MesProEdhrWorkTaskDO> createdTasks = new ArrayList<>();
        for (MesProEdhrReviewTaskCreateCommand reviewTask : reviewTasks) {
            createdTasks.add(createReviewTask(workTask, reviewTask));
        }
        return createdTasks;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrWorkTaskDO completeFillAndCreateNextFillAfterOrdinarySubmit(Long workTaskId, Long executionId) {
        MesProEdhrWorkTaskDO workTask = validateSubmitTask(workTaskId, executionId);
        completeTask(workTask);
        MesProEdhrBatchExecutionTaskDO batchTask = batchTaskMapper.selectById(workTask.getBatchTaskId());
        if (batchTask != null) {
            batchTaskMapper.updateById(new MesProEdhrBatchExecutionTaskDO()
                    .setId(batchTask.getId())
                    .setExecutionId(executionId)
                    .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED)
                    .setSubmittedAt(LocalDateTime.now())
                    .setApprovedAt(LocalDateTime.now()));
        }
        MesProEdhrWorkTaskDO completed = workTaskMapper.selectById(workTask.getId());
        createNextFillAfterReview(completed);
        return completed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrWorkTaskDO completeFillAndCreateNextFillAfterGoldenFingerSubmit(Long workTaskId, Long executionId) {
        MesProEdhrWorkTaskDO workTask = validateGoldenFingerFillTaskForExecution(workTaskId, executionId);
        completeTask(workTask, "GOLDEN_FINGER_SUBMIT", "批记录金手指管理员提交");
        MesProEdhrBatchExecutionTaskDO batchTask = batchTaskMapper.selectById(workTask.getBatchTaskId());
        if (batchTask != null) {
            batchTaskMapper.updateById(new MesProEdhrBatchExecutionTaskDO()
                    .setId(batchTask.getId())
                    .setExecutionId(executionId)
                    .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED)
                    .setSubmittedAt(LocalDateTime.now())
                    .setApprovedAt(LocalDateTime.now()));
        }
        MesProEdhrWorkTaskDO completed = workTaskMapper.selectById(workTask.getId());
        createNextFillAfterReview(completed);
        return completed;
    }

    private MesProEdhrWorkTaskDO validateSubmitTask(Long workTaskId, Long executionId) {
        MesProEdhrWorkTaskDO workTask = workTaskMapper.selectById(workTaskId);
        if (workTask == null || !Objects.equals(workTask.getExecutionId(), executionId)) {
            throw exception(PRO_EDHR_WORK_TASK_NOT_EXISTS);
        }
        if (!isAssignedOrCandidate(workTask, requireLoginUserId())) {
            throw exception(PRO_EDHR_WORK_TASK_ASSIGNEE_MISMATCH);
        }
        boolean submitTaskType = TASK_TYPE_FILL.equals(workTask.getTaskType())
                || TASK_TYPE_REWORK.equals(workTask.getTaskType());
        if (!submitTaskType || !isProcessableStatus(workTask.getStatus())) {
            throw exception(PRO_EDHR_WORK_TASK_STATUS_INVALID);
        }
        return workTask;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrWorkTaskDO completeOneReviewTask(Long workTaskId, Long executionId) {
        MesProEdhrWorkTaskDO reviewTask = validateWritableTask(workTaskId, executionId, TASK_TYPE_REVIEW);
        completeTask(reviewTask);
        return reviewTask;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrWorkTaskDO completeCandidateSignatureTask(Long workTaskId, Long executionId) {
        MesProEdhrWorkTaskDO reviewTask = workTaskMapper.selectById(workTaskId);
        if (reviewTask == null || !Objects.equals(reviewTask.getExecutionId(), executionId)) {
            throw exception(PRO_EDHR_WORK_TASK_NOT_EXISTS);
        }
        if (!Objects.equals(reviewTask.getTaskType(), TASK_TYPE_REVIEW) || !isProcessableStatus(reviewTask.getStatus())) {
            throw exception(PRO_EDHR_WORK_TASK_STATUS_INVALID);
        }
        Long loginUserId = requireLoginUserId();
        if (!MesProEdhrWorkTaskAuthorization.containsCandidate(reviewTask.getCandidateUserSnapshot(), loginUserId)
                && !Objects.equals(reviewTask.getAssigneeUserId(), loginUserId)) {
            throw exception(PRO_EDHR_WORK_TASK_ASSIGNEE_MISMATCH);
        }
        completeTask(reviewTask);
        String cancelReason = "同一签名位已有候选人完成";
        List<MesProEdhrWorkTaskDO> canceledPeersBefore = cancelPeerCandidateSignatureTasksInternal(
                executionId, reviewTask.getSignatureCellKey(), workTaskId, cancelReason);
        List<MesProEdhrWorkTaskDO> canceledPeersAfter = canceledPeersBefore.stream()
                .map(peer -> workTaskMapper.selectById(peer.getId()))
                .filter(Objects::nonNull)
                .toList();
        recordCandidateSignatureCompleteAudit(reviewTask,
                workTaskMapper.selectById(reviewTask.getId()), canceledPeersBefore, canceledPeersAfter,
                cancelReason, loginUserId);
        return reviewTask;
    }

    @Override
    public MesProEdhrWorkTaskDO validateWritableApproveTask(Long workTaskId, Long executionId) {
        return validateWritableTask(workTaskId, executionId, TASK_TYPE_APPROVE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrWorkTaskDO completeApproveTask(Long workTaskId, Long executionId) {
        MesProEdhrWorkTaskDO approveTask = validateWritableApproveTask(workTaskId, executionId);
        completeTask(approveTask);
        return approveTask;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelPeerCandidateSignatureTasks(Long executionId, String signatureCellKey,
                                                  Long excludingWorkTaskId, String reason) {
        cancelPeerCandidateSignatureTasksInternal(executionId, signatureCellKey, excludingWorkTaskId, reason);
    }

    private List<MesProEdhrWorkTaskDO> cancelPeerCandidateSignatureTasksInternal(Long executionId,
                                                                                 String signatureCellKey,
                                                                                 Long excludingWorkTaskId,
                                                                                 String reason) {
        if (executionId == null || StrUtil.isBlank(signatureCellKey) || excludingWorkTaskId == null) {
            throw exception(PRO_EDHR_WORK_TASK_REVIEW_CONTEXT_INVALID, "缺少执行记录、签名位或排除任务");
        }
        LocalDateTime canceledAt = LocalDateTime.now();
        List<MesProEdhrWorkTaskDO> canceledTasks =
                workTaskMapper.selectActiveCandidatePeers(executionId, signatureCellKey, excludingWorkTaskId);
        for (MesProEdhrWorkTaskDO reviewTask : canceledTasks) {
            cancelTaskAndRevokeRuntimeEntitlement(reviewTask, reason, canceledAt);
        }
        return canceledTasks;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelPendingReviewTasks(Long executionId, Long excludingWorkTaskId, String reason) {
        List<MesProEdhrWorkTaskDO> reviewTasks = workTaskMapper.selectActiveListByExecutionAndType(executionId, TASK_TYPE_REVIEW);
        LocalDateTime canceledAt = LocalDateTime.now();
        for (MesProEdhrWorkTaskDO reviewTask : reviewTasks) {
            if (Objects.equals(reviewTask.getId(), excludingWorkTaskId)) {
                continue;
            }
            cancelTaskAndRevokeRuntimeEntitlement(reviewTask, reason, canceledAt);
        }
    }

    private void cancelTaskAndRevokeRuntimeEntitlement(MesProEdhrWorkTaskDO task, String reason,
                                                       LocalDateTime canceledAt) {
        int updated = workTaskMapper.updateById(new MesProEdhrWorkTaskDO()
                .setId(task.getId())
                .setStatus(MesProEdhrWorkTaskStatus.CANCELED)
                .setReason(reason)
                .setRemark(reason)
                .setCompletedAt(canceledAt));
        if (updated != 1) {
            throw new IllegalStateException("Failed to cancel eDHR work task: id=" + task.getId());
        }
        revokeRuntimeTaskEntitlement(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelActiveTasksByBatch(Long batchExecutionId, String reason) {
        if (batchExecutionId == null || StrUtil.isBlank(reason)) {
            throw exception(PRO_EDHR_WORK_TASK_NOT_EXISTS);
        }
        LocalDateTime now = LocalDateTime.now();
        List<MesProEdhrWorkTaskDO> activeTasks = workTaskMapper.selectActiveListByBatchExecutionId(batchExecutionId);
        for (MesProEdhrWorkTaskDO task : activeTasks) {
            workTaskMapper.updateById(new MesProEdhrWorkTaskDO()
                    .setId(task.getId())
                    .setStatus(MesProEdhrWorkTaskStatus.CANCELED)
                    .setReason(reason)
                    .setRemark(reason)
                    .setCompletedAt(now));
            revokeRuntimeTaskEntitlement(task);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrWorkTaskDO reassignFillTask(Long workTaskId, String reason) {
        MesProEdhrWorkTaskDO workTask = workTaskMapper.selectById(workTaskId);
        if (workTask == null) {
            throw exception(PRO_EDHR_WORK_TASK_NOT_EXISTS);
        }
        if (!TASK_TYPE_FILL.equals(workTask.getTaskType()) || !isProcessableStatus(workTask.getStatus())) {
            throw exception(PRO_EDHR_WORK_TASK_STATUS_INVALID);
        }
        MesProEdhrBatchExecutionTaskDO batchTask = batchTaskMapper.selectById(workTask.getBatchTaskId());
        MesProEdhrProcessFormPermissionRuleDO processFormRule =
                findProcessFormFillRule(workTask.getRouteId(), batchTask);
        if (processFormRule == null || !hasProcessFormCandidateSource(processFormRule)) {
            throw exception(PRO_EDHR_WORK_TASK_ASSIGNMENT_RULE_MISSING);
        }
        MesProEdhrCandidateContract candidate = candidateResolver.resolveProcessFormRule(processFormRule);
        Long assigneeUserId = resolveFirstCandidateUserId(candidate.userSnapshot());
        MesProEdhrWorkTaskDO update = new MesProEdhrWorkTaskDO()
                .setId(workTask.getId())
                .setAssigneeUserId(assigneeUserId)
                .setCandidateSourceType(candidate.sourceType())
                .setCandidateSourceId(candidate.sourceId())
                .setCandidateUserSnapshot(candidate.userSnapshot())
                .setSourceUserId(requireLoginUserId())
                .setResponsibilitySourceType(ENTITLEMENT_SOURCE_TYPE_FILLER)
                .setResponsibilitySourceKey(buildProcessFormResponsibilitySourceKey(processFormRule, batchTask))
                .setResponsibilitySourceVersion(String.valueOf(requireProcessFormResponsibilityVersionId(processFormRule)))
                .setResponsibilitySourceDigest(buildProcessFormResponsibilitySourceDigest(processFormRule))
                .setOwnershipLocked(false)
                .setOwnershipLastTransferredAt(LocalDateTime.now())
                .setOwnershipLastTransferredBy(SecurityFrameworkUtils.getLoginUserId())
                .setDueTime(calculateDueTime(workTask))
                .setReason(reason)
                .setActionUrl(buildActionUrl(workTask))
                .setRemark(StrUtil.blankToDefault(reason, "按当前工序表单权限规则重新派发"));
        workTaskMapper.updateById(update);
        MesProEdhrWorkTaskDO reassigned = workTaskMapper.selectById(workTask.getId());
        syncRuntimeTaskEntitlement(reassigned);
        boolean ownerChanged = hasTaskOwnerChanged(workTask, reassigned);
        if (ownerChanged) {
            sendReassignmentNotify(reassigned, reason);
        }
        recordFillTaskReassignAudit(workTask, reassigned, reason, ownerChanged);
        return reassigned;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reconcileProcessFormFillTaskOwnership(String responsibilitySourceKey,
                                                      MesProEdhrProcessFormPermissionRuleDO fillRule,
                                                      String reason) {
        if (StrUtil.isBlank(responsibilitySourceKey) || fillRule == null
                || StrUtil.isBlank(fillRule.getBatchRecordReportId())) {
            throw exception(PRO_EDHR_WORK_TASK_OWNERSHIP_SOURCE_MISSING,
                    StrUtil.blankToDefault(responsibilitySourceKey, "blank"));
        }
        Long batchRecordVersionId = requireProcessFormResponsibilityVersionId(fillRule);
        MesProEdhrCandidateContract candidate = candidateResolver.resolveProcessFormRule(fillRule);
        Long assigneeUserId = resolveFirstCandidateUserId(candidate.userSnapshot());
        LocalDateTime now = LocalDateTime.now();
        for (MesProEdhrWorkTaskDO task : workTaskMapper.selectActiveFillOrReworkList()) {
            MesProEdhrBatchExecutionTaskDO batchTask = batchTaskMapper.selectById(task.getBatchTaskId());
            if (!isSameProcessFormResponsibilityTarget(batchTask, fillRule)) {
                continue;
            }
            if (StrUtil.isBlank(task.getResponsibilitySourceKey())) {
                if (isFormLevelProcessFormRule(fillRule)) {
                    continue;
                }
                throw exception(PRO_EDHR_WORK_TASK_OWNERSHIP_SOURCE_MISSING,
                        "workTaskId=" + task.getId());
            }
            if (!Objects.equals(responsibilitySourceKey, task.getResponsibilitySourceKey())) {
                continue;
            }
            if (Boolean.TRUE.equals(task.getOwnershipLocked())) {
                throw exception(PRO_EDHR_WORK_TASK_OWNERSHIP_TRANSFER_LOCKED,
                        "workTaskId=" + task.getId());
            }
            MesProEdhrWorkTaskDO update = new MesProEdhrWorkTaskDO()
                    .setId(task.getId())
                    .setAssigneeUserId(assigneeUserId)
                    .setCandidateSourceType(candidate.sourceType())
                    .setCandidateSourceId(candidate.sourceId())
                    .setCandidateUserSnapshot(candidate.userSnapshot())
                    .setSourceUserId(SecurityFrameworkUtils.getLoginUserId())
                    .setResponsibilitySourceType(ENTITLEMENT_SOURCE_TYPE_FILLER)
                    .setResponsibilitySourceKey(responsibilitySourceKey)
                    .setResponsibilitySourceVersion(String.valueOf(batchRecordVersionId))
                    .setResponsibilitySourceDigest(buildProcessFormResponsibilitySourceDigest(fillRule))
                    .setOwnershipLastTransferredAt(now)
                    .setOwnershipLastTransferredBy(SecurityFrameworkUtils.getLoginUserId())
                    .setReason(reason)
                    .setActionUrl(buildActionUrl(task))
                    .setRemark(StrUtil.blankToDefault(reason, "填写人配置变更同步任务所有权"));
            if (fillRule.getDueMinutes() != null && fillRule.getDueMinutes() > 0) {
                update.setDueTime(now.plusMinutes(fillRule.getDueMinutes()));
            }
            workTaskMapper.updateById(update);
            MesProEdhrWorkTaskDO reassigned = workTaskMapper.selectById(task.getId());
            syncRuntimeTaskEntitlement(reassigned);
            if (hasTaskOwnerChanged(task, reassigned)) {
                sendReassignmentNotify(reassigned, reason);
            }
        }
    }

    @Override
    public boolean hasActiveReviewTasks(Long executionId) {
        return !workTaskMapper.selectActiveListByExecutionAndType(executionId, TASK_TYPE_REVIEW).isEmpty();
    }

    @Override
    public boolean hasActiveReviewTasksByBpmTaskId(Long executionId, String bpmTaskId) {
        return StrUtil.isNotBlank(bpmTaskId)
                && !workTaskMapper.selectActiveListByExecutionAndBpmTaskId(executionId, bpmTaskId).isEmpty();
    }

    @Override
    public MesProEdhrWorkTaskDO getActiveReviewTaskByBpmTaskId(Long executionId, String bpmTaskId) {
        if (executionId == null || StrUtil.isBlank(bpmTaskId)) {
            return null;
        }
        return workTaskMapper.selectActiveReviewByExecutionAndBpmTaskId(executionId, bpmTaskId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createNextFillAfterReview(MesProEdhrWorkTaskDO completedTask) {
        if (completedTask == null || completedTask.getBatchTaskId() == null || completedTask.getBatchExecutionId() == null) {
            throw exception(PRO_EDHR_WORK_TASK_NOT_EXISTS);
        }
        MesProEdhrBatchExecutionTaskDO currentBatchTask = batchTaskMapper.selectById(completedTask.getBatchTaskId());
        if (currentBatchTask == null) {
            throw exception(PRO_EDHR_WORK_TASK_NOT_EXISTS);
        }
        List<MesProEdhrBatchExecutionTaskDO> batchTasks =
                batchTaskMapper.selectListByBatchExecutionId(completedTask.getBatchExecutionId());
        if (TASK_TYPE_REVIEW.equals(completedTask.getTaskType()) && hasPendingReviewForCurrentExecution(completedTask)) {
            return;
        }
        createNextFillAfterBatchTask(completedTask.getBatchExecutionId(), currentBatchTask, null,
                TASK_TYPE_REVIEW.equals(completedTask.getTaskType()) ? completedTask.getSignatureCellKey() : null,
                completedTask, batchTasks);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createNextFillAfterSpecialNodeResolved(MesProEdhrBatchExecutionTaskDO specialTask) {
        if (specialTask == null || specialTask.getId() == null || specialTask.getBatchExecutionId() == null) {
            throw exception(PRO_EDHR_WORK_TASK_NOT_EXISTS);
        }
        MesProEdhrBatchExecutionTaskDO persistedSpecialTask = batchTaskMapper.selectById(specialTask.getId());
        if (persistedSpecialTask == null) {
            throw exception(PRO_EDHR_WORK_TASK_NOT_EXISTS);
        }
        if (Objects.equals(persistedSpecialTask.getNodeType(), "ROUTE_FORM")
                || !isApprovedOrSkipped(persistedSpecialTask)) {
            return;
        }
        List<MesProEdhrBatchExecutionTaskDO> batchTasks =
                batchTaskMapper.selectListByBatchExecutionId(persistedSpecialTask.getBatchExecutionId());
        MesProEdhrBatchExecutionTaskDO anchorTask =
                resolveSpecialNodeAdvanceAnchor(persistedSpecialTask, batchTasks);
        createNextFillAfterBatchTask(persistedSpecialTask.getBatchExecutionId(), anchorTask,
                null, null, null, batchTasks);
    }

    private void createNextFillAfterBatchTask(Long batchExecutionId,
                                              MesProEdhrBatchExecutionTaskDO currentBatchTask,
                                              Long sourceUserId,
                                              String requiredSignatureCellKey,
                                              MesProEdhrWorkTaskDO contextTask,
                                              List<MesProEdhrBatchExecutionTaskDO> batchTasks) {
        if (batchExecutionId == null || currentBatchTask == null) {
            throw exception(PRO_EDHR_WORK_TASK_NOT_EXISTS);
        }
        MesProEdhrBatchExecutionTaskDO nextTask = batchTasks.stream()
                .filter(task -> !Boolean.FALSE.equals(task.getRequiredFlag()))
                .filter(task -> Objects.equals(task.getStatus(), MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_WAITING))
                .filter(task -> task.getRouteProcessSort() == null || currentBatchTask.getRouteProcessSort() == null
                        || task.getRouteProcessSort() > currentBatchTask.getRouteProcessSort()
                        || task.getId() > currentBatchTask.getId())
                .min(Comparator.comparing(MesProEdhrBatchExecutionTaskDO::getRouteProcessSort)
                        .thenComparing(MesProEdhrBatchExecutionTaskDO::getId))
                .orElse(null);
        if (nextTask != null) {
            boolean sameProcessNext = Objects.equals(nextTask.getRouteProcessId(), currentBatchTask.getRouteProcessId());
            if ((!sameProcessNext && hasUnsatisfiedParallelPeer(currentBatchTask, batchTasks))
                    || (!sameProcessNext && hasUnsatisfiedSpecialBeforeNext(nextTask, batchTasks))
                    || hasActiveFillForBatchTask(nextTask.getId())) {
                return;
            }
            MesProEdhrBatchExecutionDO batch = batchExecutionMapper.selectById(batchExecutionId);
            if (batch == null && contextTask != null) {
                batch = new MesProEdhrBatchExecutionDO()
                        .setId(batchExecutionId)
                        .setWorkOrderId(contextTask.getWorkOrderId())
                        .setWorkOrderCode(contextTask.getWorkOrderCode())
                        .setBatchCode(contextTask.getBatchCode())
                        .setRouteId(contextTask.getRouteId());
            }
            if (batch == null) {
                throw exception(PRO_EDHR_WORK_TASK_NOT_EXISTS);
            }
            Long resolvedSourceUserId = sourceUserId == null ? requireLoginUserId() : sourceUserId;
            createFillTask(batch, nextTask, resolvedSourceUserId, requiredSignatureCellKey);
            createOptionalCompanionFillTasks(batch, nextTask, resolvedSourceUserId);
        }
    }

    private String normalizeReleaseApprovalCandidateSourceType(String candidateSourceType) {
        String normalizedSourceType = StrUtil.trim(candidateSourceType);
        if (CANDIDATE_SOURCE_TYPE_USER.equals(normalizedSourceType)
                || CANDIDATE_SOURCE_TYPE_ROLE_GROUP.equals(normalizedSourceType)) {
            return normalizedSourceType;
        }
        throw exception(PRO_EDHR_WORK_TASK_CANDIDATE_SOURCE_INVALID);
    }

    private void validateRouteCandidateSource(String candidateSourceType, Long candidateSourceId) {
        if (candidateSourceId == null) {
            throw exception(PRO_EDHR_WORK_TASK_CANDIDATE_SOURCE_INVALID);
        }
        if (CANDIDATE_SOURCE_TYPE_ROLE_GROUP.equals(candidateSourceType)) {
            roleApi.validRoleList(Set.of(candidateSourceId));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrWorkTaskDO createApproveTaskAfterReview(MesProEdhrWorkTaskDO reviewTask) {
        if (reviewTask == null || reviewTask.getBatchTaskId() == null || reviewTask.getBatchExecutionId() == null) {
            throw exception(PRO_EDHR_WORK_TASK_NOT_EXISTS);
        }
        if (hasPendingReviewForCurrentExecution(reviewTask)) {
            return null;
        }
        MesProEdhrWorkTaskDO existing = workTaskMapper.selectActiveByBatchTaskAndType(
                reviewTask.getBatchTaskId(), TASK_TYPE_APPROVE);
        if (existing != null) {
            return existing;
        }
        MesProEdhrWorkTaskAssignmentRuleDO rule = requireRule(reviewTask.getRouteId(), reviewTask.getRouteProcessId(),
                reviewTask.getProcessId(), TASK_TYPE_APPROVE);
        MesProEdhrCandidateContract candidate = candidateResolver.resolveAssignmentRule(rule);
        MesProEdhrWorkTaskDO task = new MesProEdhrWorkTaskDO()
                .setTaskType(TASK_TYPE_APPROVE)
                .setBatchExecutionId(reviewTask.getBatchExecutionId())
                .setBatchTaskId(reviewTask.getBatchTaskId())
                .setExecutionId(reviewTask.getExecutionId())
                .setWorkOrderId(reviewTask.getWorkOrderId())
                .setWorkOrderCode(reviewTask.getWorkOrderCode())
                .setBatchCode(reviewTask.getBatchCode())
                .setRouteId(reviewTask.getRouteId())
                .setRouteProcessId(reviewTask.getRouteProcessId())
                .setProcessId(reviewTask.getProcessId())
                .setProcessName(reviewTask.getProcessName())
                .setAssigneeUserId(resolveCandidateAssigneeUserId(candidate, rule.getAssigneeUserId()))
                .setCandidateSourceType(candidate.sourceType())
                .setCandidateSourceId(candidate.sourceId())
                .setCandidateUserSnapshot(candidate.userSnapshot())
                .setSourceUserId(requireLoginUserId())
                .setSignatureCellKey(reviewTask.getSignatureCellKey())
                .setSignatureRowIndex(reviewTask.getSignatureRowIndex())
                .setSignatureColumnIndex(reviewTask.getSignatureColumnIndex())
                .setReviewSourceType(reviewTask.getReviewSourceType())
                .setReviewSourceId(reviewTask.getReviewSourceId())
                .setReviewSourceName(reviewTask.getReviewSourceName())
                .setBpmTaskId(reviewTask.getBpmTaskId())
                .setRemark("eDHR批准任务");
        return createTask(task);
    }

    private boolean hasPendingReviewForCurrentExecution(MesProEdhrWorkTaskDO reviewTask) {
        return workTaskMapper.selectActiveListByExecutionAndType(reviewTask.getExecutionId(), TASK_TYPE_REVIEW).stream()
                .anyMatch(task -> !Objects.equals(task.getId(), reviewTask.getId())
                        && Objects.equals(task.getBatchTaskId(), reviewTask.getBatchTaskId()));
    }

    private boolean hasUnsatisfiedParallelPeer(MesProEdhrBatchExecutionTaskDO currentTask,
                                               List<MesProEdhrBatchExecutionTaskDO> batchTasks) {
        if (currentTask.getRouteProcessId() == null) {
            return false;
        }
        return batchTasks.stream()
                .filter(task -> !Boolean.FALSE.equals(task.getRequiredFlag()))
                .filter(task -> Objects.equals(task.getNodeType(), "ROUTE_FORM"))
                .filter(task -> Objects.equals(task.getRouteProcessId(), currentTask.getRouteProcessId()))
                .anyMatch(task -> !isApprovedOrSkipped(task));
    }

    private boolean hasUnsatisfiedSpecialBeforeNext(MesProEdhrBatchExecutionTaskDO nextTask,
                                                    List<MesProEdhrBatchExecutionTaskDO> batchTasks) {
        return batchTasks.stream()
                .filter(task -> !Boolean.FALSE.equals(task.getRequiredFlag()))
                .filter(task -> !Objects.equals(task.getNodeType(), "ROUTE_FORM"))
                .filter(task -> task.getRouteProcessSort() == null || nextTask.getRouteProcessSort() == null
                        || task.getRouteProcessSort() <= nextTask.getRouteProcessSort())
                .anyMatch(task -> !isApprovedOrSkipped(task));
    }

    private MesProEdhrBatchExecutionTaskDO resolveSpecialNodeAdvanceAnchor(MesProEdhrBatchExecutionTaskDO specialTask,
                                                                           List<MesProEdhrBatchExecutionTaskDO> batchTasks) {
        return batchTasks.stream()
                .filter(task -> !Boolean.FALSE.equals(task.getRequiredFlag()))
                .filter(task -> Objects.equals(task.getNodeType(), "ROUTE_FORM"))
                .filter(this::isApprovedOrSkipped)
                .filter(task -> isBeforeBatchTask(task, specialTask))
                .max(Comparator.comparing(MesProEdhrBatchExecutionTaskDO::getRouteProcessSort,
                                Comparator.nullsFirst(Integer::compareTo))
                        .thenComparing(MesProEdhrBatchExecutionTaskDO::getId,
                                Comparator.nullsFirst(Long::compareTo)))
                .orElse(specialTask);
    }

    private boolean isBeforeBatchTask(MesProEdhrBatchExecutionTaskDO task,
                                      MesProEdhrBatchExecutionTaskDO referenceTask) {
        if (task == null || referenceTask == null) {
            return false;
        }
        Integer taskSort = task.getRouteProcessSort();
        Integer referenceSort = referenceTask.getRouteProcessSort();
        if (taskSort != null && referenceSort != null && !Objects.equals(taskSort, referenceSort)) {
            return taskSort < referenceSort;
        }
        return task.getId() != null && referenceTask.getId() != null
                && task.getId() < referenceTask.getId();
    }

    private boolean hasActiveFillForBatchTask(Long batchTaskId) {
        return workTaskMapper.selectList().stream()
                .anyMatch(task -> Objects.equals(task.getBatchTaskId(), batchTaskId)
                        && (TASK_TYPE_FILL.equals(task.getTaskType()) || TASK_TYPE_REWORK.equals(task.getTaskType()))
                        && isActiveFillOrReworkStatus(task.getStatus()));
    }

    private boolean isApprovedOrSkipped(MesProEdhrBatchExecutionTaskDO task) {
        if (Objects.equals(task.getStatus(), MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED)) {
            return true;
        }
        if (!Objects.equals(task.getStatus(), MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_SKIPPED)) {
            return false;
        }
        return !"SKIPPABLE_CONTROLLED".equals(task.getRequiredPolicy())
                || (task.getSkippedBy() != null && task.getSkippedAt() != null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long requireReworkAssigneeUserId(Long workTaskId, Long executionId) {
        MesProEdhrWorkTaskDO reviewTask = validateWritableTask(workTaskId, executionId, TASK_TYPE_REVIEW);
        MesProEdhrWorkTaskAssignmentRuleDO fillRule = requireRule(reviewTask.getRouteId(), reviewTask.getRouteProcessId(),
                reviewTask.getProcessId(), TASK_TYPE_FILL);
        return fillRule.getAssigneeUserId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrWorkTaskDO completeReviewAndCreateRework(Long workTaskId, Long rejectedExecutionId,
                                                              Long revisionExecutionId, String reason) {
        MesProEdhrWorkTaskDO reviewTask = validateWritableTask(workTaskId, rejectedExecutionId, TASK_TYPE_REVIEW);
        MesProEdhrWorkTaskAssignmentRuleDO fillRule = requireRule(reviewTask.getRouteId(), reviewTask.getRouteProcessId(),
                reviewTask.getProcessId(), TASK_TYPE_FILL);
        completeTask(reviewTask);
        cancelPendingReviewTasks(rejectedExecutionId, reviewTask.getId(), reason);
        return createTask(TASK_TYPE_REWORK, reviewTask, revisionExecutionId, rejectedExecutionId,
                fillRule.getAssigneeUserId(), requireLoginUserId(), reason, reason);
    }

    @Override
    public MesProEdhrWorkTaskDO getActiveByExecutionAndType(Long executionId, String taskType) {
        List<MesProEdhrWorkTaskDO> tasks = workTaskMapper.selectActiveListByExecutionAndType(executionId, taskType);
        return tasks.isEmpty() ? null : tasks.get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int processOverdueTasks(LocalDateTime now, int limit) {
        return processOverdueTasksWithSummary(now, limit).getOverdueCount();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrWorkTaskOverdueProcessResult processOverdueTasksWithSummary(LocalDateTime now, int limit) {
        if (now == null || limit <= 0) {
            throw exception(PRO_EDHR_WORK_TASK_DUE_RULE_MISSING);
        }
        int processed = 0;
        List<MesProEdhrWorkTaskDO> dueTasks = workTaskMapper.selectDueActiveTasks(now, limit);
        for (MesProEdhrWorkTaskDO task : dueTasks) {
            String reason = "逾期自动处理：任务到期时间 " + task.getDueTime();
            int updated = workTaskMapper.updateToOverdueIfActive(task.getId(), now, reason);
            if (updated > 0) {
                task.setStatus(MesProEdhrWorkTaskStatus.OVERDUE)
                        .setOverdueAt(now)
                        .setOverdueReason(reason)
                        .setReason(reason)
                        .setRemark(reason);
                sendOverdueNotify(task);
                processed++;
            }
        }
        int skipped = dueTasks.size() - processed;
        return new MesProEdhrWorkTaskOverdueProcessResult(dueTasks.size(), processed, skipped,
                skipped > 0 ? "CONCURRENT_STATUS_CHANGED" : "");
    }

    private void validateReviewTaskCreateCommand(MesProEdhrReviewTaskCreateCommand command,
                                                 Set<String> reviewTaskKeys) {
        if (command == null || StrUtil.isBlank(command.getSignatureCellKey())
                || command.getSignatureRowIndex() == null || command.getSignatureColumnIndex() == null
                || StrUtil.isBlank(command.getReviewSourceType()) || command.getReviewSourceId() == null
                || command.getAssigneeUserId() == null || StrUtil.isBlank(command.getBpmTaskId())
                || StrUtil.isBlank(command.getCandidateSourceType())
                || StrUtil.isBlank(command.getCandidateUserSnapshot())) {
            throw exception(PRO_EDHR_WORK_TASK_REVIEW_CONTEXT_INVALID,
                    "缺少签字格、来源、审核人、BPM 任务、候选来源或候选快照");
        }
        String reviewTaskKey = command.getSignatureCellKey() + ":" + command.getAssigneeUserId();
        if (!reviewTaskKeys.add(reviewTaskKey)) {
            throw exception(PRO_EDHR_WORK_TASK_REVIEW_CONTEXT_INVALID,
                    "签字格候选人重复：" + command.getSignatureCellKey());
        }
    }

    private void createFillTask(MesProEdhrBatchExecutionDO batch, MesProEdhrBatchExecutionTaskDO batchTask,
                                Long sourceUserId, String requiredSignatureCellKey) {
        MesProRouteProcessDO currentRouteProcess = resolveFrozenRouteProcess(batch == null ? null : batch.getRouteId(),
                batchTask == null ? null : batchTask.getRouteProcessId(),
                batchTask == null ? null : batchTask.getProcessId());
        MesProEdhrProcessFormPermissionRuleDO processFormRule = findProcessFormFillRule(currentRouteProcess, batchTask);
        MesProEdhrWorkTaskAssignmentRuleDO rule = processFormRule == null
                ? findRule(currentRouteProcess, TASK_TYPE_FILL) : null;
        validateAdvancePrerequisites(batch, batchTask, processFormRule, rule, requiredSignatureCellKey);
        MesProEdhrCandidateContract candidate = processFormRule == null
                ? candidateResolver.resolveAssignmentRule(rule) : candidateResolver.resolveProcessFormRule(processFormRule);
        MesProEdhrWorkTaskDO task = new MesProEdhrWorkTaskDO()
                .setTaskType(TASK_TYPE_FILL)
                .setBatchExecutionId(batch.getId())
                .setBatchTaskId(batchTask.getId())
                .setWorkOrderId(batch.getWorkOrderId())
                .setWorkOrderCode(batch.getWorkOrderCode())
                .setBatchCode(batch.getBatchCode())
                .setRouteId(batch.getRouteId())
                .setRouteProcessId(currentRouteProcess.getId())
                .setProcessId(currentRouteProcess.getProcessId())
                .setProcessName(batchTask.getProcessName())
                .setAssigneeUserId(processFormRule == null
                        ? resolveCandidateAssigneeUserId(candidate, rule.getAssigneeUserId())
                        : resolveFirstCandidateUserId(candidate.userSnapshot()))
                .setCandidateSourceType(candidate.sourceType())
                .setCandidateSourceId(candidate.sourceId())
                .setCandidateUserSnapshot(candidate.userSnapshot())
                .setSourceUserId(sourceUserId)
                .setRemark("eDHR填写任务");
        if (processFormRule != null) {
            task.setResponsibilitySourceType(ENTITLEMENT_SOURCE_TYPE_FILLER)
                    .setResponsibilitySourceKey(buildProcessFormResponsibilitySourceKey(
                            processFormRule, currentRouteProcess, batchTask))
                    .setResponsibilitySourceVersion(String.valueOf(requireProcessFormResponsibilityVersionId(processFormRule)))
                    .setResponsibilitySourceDigest(buildProcessFormResponsibilitySourceDigest(processFormRule))
                    .setOwnershipLocked(false);
        }
        createTask(task);
    }

    private void createOptionalCompanionFillTasks(MesProEdhrBatchExecutionDO batch,
                                                  MesProEdhrBatchExecutionTaskDO anchorTask,
                                                  Long sourceUserId) {
        if (batch == null || batch.getId() == null || anchorTask == null || anchorTask.getRouteProcessId() == null) {
            return;
        }
        batchTaskMapper.selectListByBatchExecutionId(batch.getId()).stream()
                .filter(task -> !Objects.equals(task.getId(), anchorTask.getId()))
                .filter(task -> Objects.equals(task.getRouteProcessId(), anchorTask.getRouteProcessId()))
                .filter(task -> MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_ROUTE_FORM.equals(task.getNodeType()))
                .filter(task -> Boolean.FALSE.equals(task.getRequiredFlag()))
                .filter(task -> Objects.equals(task.getStatus(), MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_WAITING))
                .filter(task -> !hasActiveFillForBatchTask(task.getId()))
                .sorted(Comparator.comparing(MesProEdhrBatchExecutionTaskDO::getBatchRecordSort,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(MesProEdhrBatchExecutionTaskDO::getId,
                                Comparator.nullsLast(Long::compareTo)))
                .forEach(task -> createFillTask(batch, task, sourceUserId, null));
    }

    private void validateAdvancePrerequisites(MesProEdhrBatchExecutionDO batch,
                                              MesProEdhrBatchExecutionTaskDO batchTask,
                                              MesProEdhrProcessFormPermissionRuleDO processFormRule,
                                              MesProEdhrWorkTaskAssignmentRuleDO rule,
                                              String requiredSignatureCellKey) {
        List<String> missingItems = collectAdvancePrerequisiteNames(batch, batchTask, processFormRule, rule,
                requiredSignatureCellKey);
        if (!missingItems.isEmpty()) {
            throw exception(PRO_EDHR_WORK_TASK_ADVANCE_PREREQUISITE_MISSING,
                    String.join("、", missingItems));
        }
    }

    private List<String> collectAdvancePrerequisiteNames(MesProEdhrBatchExecutionDO batch,
                                                         MesProEdhrBatchExecutionTaskDO batchTask,
                                                         MesProEdhrProcessFormPermissionRuleDO processFormRule,
                                                         MesProEdhrWorkTaskAssignmentRuleDO rule,
                                                         String requiredSignatureCellKey) {
        List<String> missingItems = new ArrayList<>();
        if (batch == null || batch.getWorkOrderId() == null || StrUtil.isBlank(batch.getWorkOrderCode())) {
            missingItems.add("工单");
        }
        if (batch == null || StrUtil.isBlank(batch.getBatchCode())) {
            missingItems.add("批次");
        }
        if (batch == null || batch.getProductId() == null) {
            missingItems.add("产品");
        }
        if (batch == null || batch.getRouteId() == null) {
            missingItems.add("路线");
        }
        if (batchTask == null || batchTask.getRouteProcessId() == null || batchTask.getProcessId() == null
                || StrUtil.isBlank(batchTask.getProcessName())) {
            missingItems.add("工序");
        }
        if (batchTask == null || StrUtil.isBlank(resolveProcessFormRuleBindingKey(batchTask))) {
            missingItems.add("批记录绑定");
        }
        if (requiredSignatureCellKey != null && StrUtil.isBlank(requiredSignatureCellKey)) {
            missingItems.add("签名位");
        }
        if ((processFormRule == null || !hasProcessFormCandidateSource(processFormRule))
                && (rule == null || !hasCandidateSource(rule))) {
            missingItems.add("权限");
        }
        return missingItems;
    }

    private void createTask(String taskType, MesProEdhrWorkTaskDO sourceTask, Long assigneeUserId,
                            Long sourceUserId, String remark) {
        createTask(taskType, sourceTask, sourceTask.getExecutionId(), null, assigneeUserId, sourceUserId, null, remark);
    }

    private MesProEdhrWorkTaskDO createReviewTask(MesProEdhrWorkTaskDO sourceTask,
                                                  MesProEdhrReviewTaskCreateCommand command) {
        MesProEdhrWorkTaskDO task = new MesProEdhrWorkTaskDO()
                .setTaskType(TASK_TYPE_REVIEW)
                .setBatchExecutionId(sourceTask.getBatchExecutionId())
                .setBatchTaskId(sourceTask.getBatchTaskId())
                .setExecutionId(sourceTask.getExecutionId())
                .setWorkOrderId(sourceTask.getWorkOrderId())
                .setWorkOrderCode(sourceTask.getWorkOrderCode())
                .setBatchCode(sourceTask.getBatchCode())
                .setRouteId(sourceTask.getRouteId())
                .setRouteProcessId(sourceTask.getRouteProcessId())
                .setProcessId(sourceTask.getProcessId())
                .setProcessName(sourceTask.getProcessName())
                .setAssigneeUserId(command.getAssigneeUserId())
                .setCandidateSourceType(command.getCandidateSourceType())
                .setCandidateSourceId(command.getCandidateSourceId())
                .setCandidateUserSnapshot(command.getCandidateUserSnapshot())
                .setSourceUserId(requireLoginUserId())
                .setSignatureCellKey(command.getSignatureCellKey())
                .setSignatureRowIndex(command.getSignatureRowIndex())
                .setSignatureColumnIndex(command.getSignatureColumnIndex())
                .setReviewSourceType(command.getReviewSourceType())
                .setReviewSourceId(command.getReviewSourceId())
                .setReviewSourceName(command.getReviewSourceName())
                .setBpmTaskId(command.getBpmTaskId())
                .setRemark("eDHR审核任务");
        return createTask(task);
    }

    private MesProEdhrWorkTaskDO createTask(String taskType, MesProEdhrWorkTaskDO sourceTask, Long executionId,
                                            Long sourceExecutionId, Long assigneeUserId, Long sourceUserId,
                                            String reason, String remark) {
        MesProEdhrWorkTaskDO task = new MesProEdhrWorkTaskDO()
                .setTaskType(taskType)
                .setBatchExecutionId(sourceTask.getBatchExecutionId())
                .setBatchTaskId(sourceTask.getBatchTaskId())
                .setExecutionId(executionId)
                .setSourceExecutionId(sourceExecutionId)
                .setWorkOrderId(sourceTask.getWorkOrderId())
                .setWorkOrderCode(sourceTask.getWorkOrderCode())
                .setBatchCode(sourceTask.getBatchCode())
                .setRouteId(sourceTask.getRouteId())
                .setRouteProcessId(sourceTask.getRouteProcessId())
                .setProcessId(sourceTask.getProcessId())
                .setProcessName(sourceTask.getProcessName())
                .setAssigneeUserId(assigneeUserId)
                .setCandidateSourceType(StrUtil.blankToDefault(sourceTask.getCandidateSourceType(),
                        CANDIDATE_SOURCE_TYPE_USER))
                .setCandidateSourceId(sourceTask.getCandidateSourceId())
                .setCandidateUserSnapshot(StrUtil.blankToDefault(sourceTask.getCandidateUserSnapshot(),
                        assigneeUserId == null ? "" : assigneeUserId.toString()))
                .setSourceUserId(sourceUserId)
                .setReason(reason)
                .setRemark(remark);
        return createTask(task);
    }

    private MesProEdhrWorkTaskDO createTask(MesProEdhrWorkTaskDO task) {
        fillBusinessScope(task);
        task.setStatus(MesProEdhrWorkTaskStatus.TODO)
                .setSignatureCellKey(StrUtil.blankToDefault(task.getSignatureCellKey(), ""))
                .setTaskCode("EDHRT-" + System.currentTimeMillis())
                .setDueTime(calculateDueTime(task))
                .setActionUrl("");
        workTaskMapper.insert(task);
        task.setActionUrl(buildActionUrl(task));
        workTaskMapper.updateById(new MesProEdhrWorkTaskDO()
                .setId(task.getId())
                .setActionUrl(task.getActionUrl()));
        syncRuntimeTaskEntitlement(task);
        sendNotify(task);
        return task;
    }

    private void fillBusinessScope(MesProEdhrWorkTaskDO task) {
        if (StrUtil.isBlank(task.getBusinessScopeType())) {
            task.setBusinessScopeType(BUSINESS_SCOPE_TYPE_BATCH_TASK);
        }
        if (BUSINESS_SCOPE_TYPE_BATCH_TASK.equals(task.getBusinessScopeType()) && task.getBusinessScopeId() == null) {
            task.setBusinessScopeId(task.getBatchTaskId());
        }
    }

    private void completeTask(MesProEdhrWorkTaskDO workTask) {
        completeTask(workTask, null, null);
    }

    private void completeTask(MesProEdhrWorkTaskDO workTask, String result, String reason) {
        String completionReason = buildCompletionReason(result, reason);
        MesProEdhrWorkTaskDO update = new MesProEdhrWorkTaskDO()
                .setId(workTask.getId())
                .setStatus(MesProEdhrWorkTaskStatus.DONE)
                .setCompletedAt(LocalDateTime.now());
        if (StrUtil.isNotBlank(completionReason)) {
            update.setReason(completionReason)
                    .setRemark(StrUtil.blankToDefault(StrUtil.trim(reason), completionReason));
        }
        workTaskMapper.updateById(new MesProEdhrWorkTaskDO()
                .setId(update.getId())
                .setStatus(update.getStatus())
                .setReason(update.getReason())
                .setRemark(update.getRemark())
                .setCompletedAt(update.getCompletedAt()));
        revokeRuntimeTaskEntitlement(workTask);
    }

    private String buildCompletionReason(String result, String reason) {
        if (StrUtil.isBlank(result)) {
            return null;
        }
        return StrUtil.trim(result) + ":" + StrUtil.blankToDefault(StrUtil.trim(reason), "");
    }

    private MesProEdhrWorkTaskAssignmentRuleDO requireRule(Long routeId, Long routeProcessId, Long processId,
                                                           String taskType) {
        MesProEdhrWorkTaskAssignmentRuleDO rule = findRule(routeId, routeProcessId, processId, taskType);
        if (rule == null || !hasCandidateSource(rule)) {
            throw exception(PRO_EDHR_WORK_TASK_ASSIGNMENT_RULE_MISSING);
        }
        validateCandidateSource(rule);
        return rule;
    }

    private MesProEdhrWorkTaskAssignmentRuleDO findRule(Long routeId, Long routeProcessId, Long processId,
                                                        String taskType) {
        return findRule(resolveFrozenRouteProcess(routeId, routeProcessId, processId), taskType);
    }

    private MesProEdhrWorkTaskAssignmentRuleDO findRule(MesProRouteProcessDO currentRouteProcess, String taskType) {
        if (currentRouteProcess == null || currentRouteProcess.getId() == null) {
            return null;
        }
        return assignmentRuleMapper.selectEnabledByRouteProcessAndType(currentRouteProcess.getId(), taskType);
    }

    private MesProEdhrProcessFormPermissionRuleDO findProcessFormFillRule(Long routeId,
                                                                          MesProEdhrBatchExecutionTaskDO batchTask) {
        return findProcessFormFillRule(resolveFrozenRouteProcess(routeId,
                batchTask == null ? null : batchTask.getRouteProcessId(),
                batchTask == null ? null : batchTask.getProcessId()), batchTask);
    }

    private MesProEdhrProcessFormPermissionRuleDO findProcessFormFillRule(MesProRouteProcessDO currentRouteProcess,
                                                                           MesProEdhrBatchExecutionTaskDO batchTask) {
        String bindingKey = resolveProcessFormRuleBindingKey(batchTask);
        if (currentRouteProcess == null || currentRouteProcess.getId() == null
                || batchTask == null || StrUtil.isBlank(bindingKey)) {
            return null;
        }
        return processFormPermissionRuleMapper.selectEnabledFillRuleForRouteOrReport(
                currentRouteProcess.getId(), bindingKey,
                batchTask.getBatchRecordVersionId());
    }

    private MesProRouteProcessDO resolveFrozenRouteProcess(Long routeId, Long routeProcessId, Long processId) {
        if (routeProcessId == null && processId == null) {
            return null;
        }
        if (routeId == null) {
            return null;
        }
        return routeProcessService.resolveFrozenRouteProcess(routeProcessId, routeId, processId);
    }

    private boolean hasProcessFormCandidateSource(MesProEdhrProcessFormPermissionRuleDO rule) {
        return rule != null && StrUtil.isNotBlank(rule.getCandidateSourceType())
                && StrUtil.isNotBlank(rule.getCandidateSourceIds());
    }

    private String resolveProcessFormRuleBindingKey(MesProEdhrBatchExecutionTaskDO batchTask) {
        if (batchTask == null) {
            return null;
        }
        return StrUtil.blankToDefault(StrUtil.trim(batchTask.getBatchRecordReportId()),
                StrUtil.trim(batchTask.getFormBindingKey()));
    }

    private String buildProcessFormResponsibilitySourceKey(MesProEdhrProcessFormPermissionRuleDO rule,
                                                           MesProEdhrBatchExecutionTaskDO batchTask) {
        return buildProcessFormResponsibilitySourceKey(rule, null, batchTask);
    }

    private String buildProcessFormResponsibilitySourceKey(MesProEdhrProcessFormPermissionRuleDO rule,
                                                           MesProRouteProcessDO currentRouteProcess,
                                                           MesProEdhrBatchExecutionTaskDO batchTask) {
        String bindingKey = resolveProcessFormRuleBindingKey(batchTask);
        if (rule == null || batchTask == null || StrUtil.isBlank(bindingKey)) {
            throw exception(PRO_EDHR_WORK_TASK_OWNERSHIP_SOURCE_MISSING, "processFormRule");
        }
        Long versionId = requireProcessFormResponsibilityVersionId(rule);
        if (Objects.equals(rule.getRouteProcessId(),
                MesProEdhrProcessFormPermissionRuleMapper.FORM_LEVEL_ROUTE_PROCESS_ID)) {
            return "FORM|" + bindingKey + "|" + versionId;
        }
        Long routeProcessId = rule.getRouteProcessId() != null
                ? rule.getRouteProcessId()
                : currentRouteProcess == null ? null : currentRouteProcess.getId();
        if (routeProcessId == null) {
            throw exception(PRO_EDHR_WORK_TASK_OWNERSHIP_SOURCE_MISSING, "routeProcessId");
        }
        return "ROUTE|" + routeProcessId + "|" + bindingKey + "|" + versionId;
    }

    private Long requireProcessFormResponsibilityVersionId(MesProEdhrProcessFormPermissionRuleDO rule) {
        if (rule == null || rule.getBatchRecordVersionId() == null) {
            throw exception(PRO_EDHR_PROCESS_FORM_PERMISSION_RULE_VERSION_REQUIRED,
                    rule == null ? null : rule.getRouteProcessId(),
                    rule == null ? null : rule.getBatchRecordReportId());
        }
        return rule.getBatchRecordVersionId();
    }

    private String buildProcessFormResponsibilitySourceDigest(MesProEdhrProcessFormPermissionRuleDO rule) {
        List<Long> candidateSourceIds = parseRawIds(rule.getCandidateSourceIds()).stream()
                .sorted()
                .toList();
        return "candidateSourceType=" + rule.getCandidateSourceType()
                + ";candidateSourceIds=" + candidateSourceIds
                + ";completionPolicy=" + rule.getCompletionPolicy()
                + ";dueMinutes=" + rule.getDueMinutes()
                + ";enabled=" + Boolean.TRUE.equals(rule.getEnabled());
    }

    private boolean isSameProcessFormResponsibilityTarget(MesProEdhrBatchExecutionTaskDO batchTask,
                                                          MesProEdhrProcessFormPermissionRuleDO fillRule) {
        String bindingKey = resolveProcessFormRuleBindingKey(batchTask);
        if (batchTask == null || fillRule == null
                || StrUtil.isBlank(bindingKey)
                || StrUtil.isBlank(fillRule.getBatchRecordReportId())) {
            return false;
        }
        if (!Objects.equals(bindingKey, StrUtil.trim(fillRule.getBatchRecordReportId()))) {
            return false;
        }
        if (!Objects.equals(batchTask.getBatchRecordVersionId(), fillRule.getBatchRecordVersionId())) {
            return false;
        }
        if (Objects.equals(fillRule.getRouteProcessId(),
                MesProEdhrProcessFormPermissionRuleMapper.FORM_LEVEL_ROUTE_PROCESS_ID)) {
            return true;
        }
        return Objects.equals(batchTask.getRouteProcessId(), fillRule.getRouteProcessId());
    }

    private boolean isFormLevelProcessFormRule(MesProEdhrProcessFormPermissionRuleDO fillRule) {
        return fillRule != null && Objects.equals(fillRule.getRouteProcessId(),
                MesProEdhrProcessFormPermissionRuleMapper.FORM_LEVEL_ROUTE_PROCESS_ID);
    }

    private void syncRuntimeTaskEntitlement(MesProEdhrWorkTaskDO task) {
        if (!requiresRuntimeTaskEntitlement(task)) {
            return;
        }
        permissionApi.syncEntitlementClaims(SystemEntitlementSyncReqDTO.builder()
                .tenantId(requireTenantIdForEntitlement())
                .sourceType(ENTITLEMENT_SOURCE_TYPE_WORK_TASK)
                .sourceKey(buildWorkTaskEntitlementSourceKey(task))
                .sourceVersion(String.valueOf(task.getId()))
                .sourceDigest(buildRuntimeWorkTaskSourceDigest(task))
                .policyCode(resolveRuntimeTaskEntitlementPolicyCode(task))
                .resolvedUserIds(parseCandidateUserIds(task.getCandidateUserSnapshot()))
                .operatorUserId(SecurityFrameworkUtils.getLoginUserId())
                .operatorUsername(SecurityFrameworkUtils.getLoginUserNickname())
                .build());
    }

    private void revokeRuntimeTaskEntitlement(MesProEdhrWorkTaskDO task) {
        if (!requiresRuntimeTaskEntitlement(task)) {
            return;
        }
        permissionApi.revokeEntitlementSource(SystemEntitlementRevokeReqDTO.builder()
                .tenantId(requireTenantIdForEntitlement())
                .sourceType(ENTITLEMENT_SOURCE_TYPE_WORK_TASK)
                .sourceKey(buildWorkTaskEntitlementSourceKey(task))
                .policyCode(resolveRuntimeTaskEntitlementPolicyCode(task))
                .operatorUserId(SecurityFrameworkUtils.getLoginUserId())
                .operatorUsername(SecurityFrameworkUtils.getLoginUserNickname())
                .build());
    }

    private boolean requiresRuntimeTaskEntitlement(MesProEdhrWorkTaskDO task) {
        return task != null && (TASK_TYPE_FILL.equals(task.getTaskType())
                || TASK_TYPE_REWORK.equals(task.getTaskType())
                || TASK_TYPE_REVIEW.equals(task.getTaskType())
                || TASK_TYPE_APPROVE.equals(task.getTaskType())
                || TASK_TYPE_RELEASE_APPROVE.equals(task.getTaskType()));
    }

    private String resolveRuntimeTaskEntitlementPolicyCode(MesProEdhrWorkTaskDO task) {
        if (TASK_TYPE_RELEASE_APPROVE.equals(task.getTaskType())) {
            return ENTITLEMENT_POLICY_RELEASE_APPROVER_MINIMAL;
        }
        if (TASK_TYPE_REVIEW.equals(task.getTaskType()) || TASK_TYPE_APPROVE.equals(task.getTaskType())) {
            return ENTITLEMENT_POLICY_APPROVAL_REVIEWER_MINIMAL;
        }
        return ENTITLEMENT_POLICY_FILLER_MINIMAL;
    }

    private String buildWorkTaskEntitlementSourceKey(MesProEdhrWorkTaskDO task) {
        if (task == null || task.getId() == null) {
            throw exception(PRO_EDHR_WORK_TASK_NOT_EXISTS);
        }
        return "WORK_TASK|" + task.getId();
    }

    private Long requireTenantIdForEntitlement() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("tenantId is required to sync eDHR work task entitlement");
        }
        return tenantId;
    }

    private String buildRuntimeWorkTaskSourceDigest(MesProEdhrWorkTaskDO task) {
        return "taskType=" + task.getTaskType()
                + ";status=" + task.getStatus()
                + ";candidateSourceType=" + task.getCandidateSourceType()
                + ";candidateSourceId=" + task.getCandidateSourceId()
                + ";candidateUserSnapshot=" + task.getCandidateUserSnapshot()
                + ";responsibilitySourceType=" + task.getResponsibilitySourceType()
                + ";responsibilitySourceKey=" + task.getResponsibilitySourceKey()
                + ";ownershipLocked=" + Boolean.TRUE.equals(task.getOwnershipLocked());
    }

    private Set<Long> parseCandidateUserIds(String candidateUserSnapshot) {
        return MesProEdhrWorkTaskAuthorization.parseRequiredCandidateSnapshotUserIds(candidateUserSnapshot);
    }

    private List<Long> parseRawIds(String rawIds) {
        if (StrUtil.isBlank(rawIds)) {
            return List.of();
        }
        return Arrays.stream(rawIds.split(","))
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .map(Long::valueOf)
                .distinct()
                .toList();
    }

    private MesProEdhrWorkTaskAssignmentRuleDO requireRouteRule(Long routeId, String taskType) {
        MesProEdhrWorkTaskAssignmentRuleDO rule =
                assignmentRuleMapper.selectEnabledByScopeAndType(RULE_SCOPE_TYPE_ROUTE, routeId, taskType);
        if (rule == null || !hasCandidateSource(rule)) {
            throw exception(PRO_EDHR_WORK_TASK_ASSIGNMENT_RULE_MISSING);
        }
        validateCandidateSource(rule);
        return rule;
    }

    private void validateCandidateSource(MesProEdhrWorkTaskAssignmentRuleDO rule) {
        MesProEdhrCandidateContract candidate = candidateResolver.resolveAssignmentRule(rule);
        String sourceType = candidate.sourceType();
        if (!SUPPORTED_CANDIDATE_SOURCE_TYPES.contains(sourceType)) {
            throw exception(PRO_EDHR_WORK_TASK_CANDIDATE_SOURCE_INVALID);
        }
        if ("USER_GROUP".equals(sourceType)) {
            throw exception(PRO_EDHR_WORK_TASK_CANDIDATE_SOURCE_INVALID);
        }
        if (candidate.userSnapshot().isBlank()) {
            throw exception(PRO_EDHR_WORK_TASK_CANDIDATE_POOL_EMPTY);
        }
    }

    private boolean hasCandidateSource(MesProEdhrWorkTaskAssignmentRuleDO rule) {
        String sourceType = resolveCandidateSourceType(rule);
        if (CANDIDATE_SOURCE_TYPE_USER.equals(sourceType)) {
            return rule.getCandidateSourceId() != null || rule.getAssigneeUserId() != null;
        }
        return rule.getCandidateSourceId() != null;
    }

    private String resolveCandidateSourceType(MesProEdhrWorkTaskAssignmentRuleDO rule) {
        return StrUtil.blankToDefault(rule.getCandidateSourceType(), CANDIDATE_SOURCE_TYPE_USER);
    }

    private Long resolveCandidateSourceId(MesProEdhrWorkTaskAssignmentRuleDO rule) {
        return rule.getCandidateSourceId() != null ? rule.getCandidateSourceId() : rule.getAssigneeUserId();
    }

    private Long resolveCandidateAssigneeUserId(MesProEdhrCandidateContract candidate, Long explicitAssigneeUserId) {
        String snapshot = candidate.userSnapshot();
        if (explicitAssigneeUserId != null) {
            if (!MesProEdhrWorkTaskAuthorization.containsCandidate(snapshot, explicitAssigneeUserId)) {
                throw exception(PRO_EDHR_WORK_TASK_ASSIGNEE_INVALID);
            }
            validateAssignee(explicitAssigneeUserId);
            return explicitAssigneeUserId;
        }
        return resolveFirstCandidateUserId(snapshot);
    }

    private Long resolveFirstCandidateUserId(String snapshot) {
        return MesProEdhrWorkTaskAuthorization.resolveFirstRequiredCandidateUserId(snapshot);
    }

    private boolean isAssignedOrCandidate(MesProEdhrWorkTaskDO workTask, Long userId) {
        return MesProEdhrWorkTaskAuthorization.isAssignedOrCandidate(workTask, userId);
    }

    private void validateRouteExists(Long routeId) {
        if (routeId == null || routeMapper.selectById(routeId) == null) {
            throw exception(PRO_ROUTE_NOT_EXISTS);
        }
    }

    private void validateAssignee(Long assigneeUserId) {
        AdminUserRespDTO assignee = assigneeUserId == null ? null : adminUserApi.getUser(assigneeUserId);
        if (assignee == null || !CommonStatusEnum.isEnable(assignee.getStatus())) {
            throw exception(PRO_EDHR_WORK_TASK_ASSIGNEE_INVALID);
        }
    }

    private LocalDateTime calculateDueTime(MesProEdhrWorkTaskDO task) {
        if (TASK_TYPE_FILL.equals(task.getTaskType())) {
            MesProEdhrBatchExecutionTaskDO batchTask = batchTaskMapper.selectById(task.getBatchTaskId());
            MesProEdhrProcessFormPermissionRuleDO processFormRule =
                    findProcessFormFillRule(task.getRouteId(), batchTask);
            if (processFormRule != null) {
                if (processFormRule.getDueMinutes() == null || processFormRule.getDueMinutes() <= 0) {
                    throw exception(PRO_EDHR_WORK_TASK_DUE_RULE_MISSING);
                }
                return LocalDateTime.now().plusMinutes(processFormRule.getDueMinutes());
            }
        }
        if (TASK_TYPE_RELEASE_APPROVE.equals(task.getTaskType())) {
            return null;
        }
        MesProEdhrWorkTaskAssignmentRuleDO rule = isRouteLevelTaskType(task.getTaskType())
                ? requireRouteRule(task.getRouteId(), task.getTaskType())
                : requireRule(task.getRouteId(), task.getRouteProcessId(), task.getProcessId(), task.getTaskType());
        if (rule.getDueMinutes() == null || rule.getDueMinutes() <= 0) {
            throw exception(PRO_EDHR_WORK_TASK_DUE_RULE_MISSING);
        }
        return LocalDateTime.now().plusMinutes(rule.getDueMinutes());
    }

    private boolean isRouteLevelTaskType(String taskType) {
        return TASK_TYPE_ARCHIVE.equals(taskType)
                || TASK_TYPE_CLOSE.equals(taskType)
                || TASK_TYPE_RELEASE_APPROVE.equals(taskType);
    }

    private boolean isProcessableStatus(String status) {
        return Objects.equals(status, MesProEdhrWorkTaskStatus.TODO)
                || Objects.equals(status, MesProEdhrWorkTaskStatus.OVERDUE);
    }

    private boolean isActiveFillOrReworkStatus(String status) {
        return Objects.equals(status, MesProEdhrWorkTaskStatus.TODO)
                || Objects.equals(status, MesProEdhrWorkTaskStatus.DOING)
                || Objects.equals(status, MesProEdhrWorkTaskStatus.OVERDUE);
    }

    private void sendNotify(MesProEdhrWorkTaskDO task) {
        NotifySendSingleToUserReqDTO reqDTO = new NotifySendSingleToUserReqDTO();
        reqDTO.setUserId(task.getAssigneeUserId());
        reqDTO.setTemplateCode(resolveNotifyTemplateCode(task.getTaskType()));
        reqDTO.setTemplateParams(Map.of(
                "workOrderCode", Objects.toString(task.getWorkOrderCode(), ""),
                "batchCode", Objects.toString(task.getBatchCode(), ""),
                "processName", Objects.toString(task.getProcessName(), ""),
                "actionUrl", task.getActionUrl(),
                "reason", Objects.toString(task.getReason(), ""),
                "workTaskId", task.getId()));
        notifyMessageSendApi.sendSingleMessageToAdmin(reqDTO);
    }

    private void sendReassignmentNotify(MesProEdhrWorkTaskDO task, String reason) {
        NotifySendSingleToUserReqDTO reqDTO = new NotifySendSingleToUserReqDTO();
        reqDTO.setUserId(task.getAssigneeUserId());
        reqDTO.setTemplateCode("MES_EDHR_FILL_TASK_REASSIGNED");
        reqDTO.setTemplateParams(Map.of(
                "workOrderCode", Objects.toString(task.getWorkOrderCode(), ""),
                "batchCode", Objects.toString(task.getBatchCode(), ""),
                "processName", Objects.toString(task.getProcessName(), ""),
                "actionUrl", task.getActionUrl(),
                "reason", Objects.toString(reason, ""),
                "workTaskId", task.getId()));
        notifyMessageSendApi.sendSingleMessageToAdmin(reqDTO);
    }

    private boolean hasTaskOwnerChanged(MesProEdhrWorkTaskDO beforeTask, MesProEdhrWorkTaskDO afterTask) {
        return !Objects.equals(beforeTask.getAssigneeUserId(), afterTask.getAssigneeUserId())
                || !Objects.equals(beforeTask.getCandidateSourceType(), afterTask.getCandidateSourceType())
                || !Objects.equals(beforeTask.getCandidateSourceId(), afterTask.getCandidateSourceId())
                || !Objects.equals(beforeTask.getCandidateUserSnapshot(), afterTask.getCandidateUserSnapshot());
    }

    private void sendOverdueNotify(MesProEdhrWorkTaskDO task) {
        NotifySendSingleToUserReqDTO reqDTO = new NotifySendSingleToUserReqDTO();
        reqDTO.setUserId(task.getAssigneeUserId());
        reqDTO.setTemplateCode("MES_EDHR_WORK_TASK_OVERDUE");
        reqDTO.setTemplateParams(Map.of(
                "workOrderCode", Objects.toString(task.getWorkOrderCode(), ""),
                "batchCode", Objects.toString(task.getBatchCode(), ""),
                "processName", Objects.toString(task.getProcessName(), ""),
                "dueTime", Objects.toString(task.getDueTime(), ""),
                "actionUrl", task.getActionUrl(),
                "workTaskId", task.getId()));
        notifyMessageSendApi.sendSingleMessageToAdmin(reqDTO);
    }

    private String resolveNotifyTemplateCode(String taskType) {
        if (TASK_TYPE_REVIEW.equals(taskType)) {
            return "MES_EDHR_REVIEW_TASK_ASSIGNED";
        }
        if (TASK_TYPE_APPROVE.equals(taskType)) {
            return "MES_EDHR_APPROVE_TASK_ASSIGNED";
        }
        if (TASK_TYPE_REWORK.equals(taskType)) {
            return "MES_EDHR_REWORK_TASK_ASSIGNED";
        }
        if (TASK_TYPE_ARCHIVE.equals(taskType)) {
            return "MES_EDHR_ARCHIVE_TASK_ASSIGNED";
        }
        if (TASK_TYPE_RELEASE_APPROVE.equals(taskType)) {
            return "MES_EDHR_RELEASE_APPROVE_TASK_ASSIGNED";
        }
        return "MES_EDHR_FILL_TASK_ASSIGNED";
    }

    private String buildActionUrl(MesProEdhrWorkTaskDO task) {
        if (TASK_TYPE_RELEASE_APPROVE.equals(task.getTaskType())) {
            return "/mes/pro/feedback/edhr-batch-execution/detail?id=" + task.getBatchExecutionId()
                    + "&workTaskId=" + task.getId()
                    + "&focus=approval"
                    + "&releaseTransactionId=" + task.getBusinessScopeId();
        }
        if (TASK_TYPE_ARCHIVE.equals(task.getTaskType())) {
            return "/mes/pro/feedback/edhr-batch-execution/detail?id=" + task.getBatchExecutionId()
                    + "&workTaskId=" + task.getId();
        }
        if (TASK_TYPE_REVIEW.equals(task.getTaskType()) || TASK_TYPE_APPROVE.equals(task.getTaskType())) {
            return "/mes/pro/feedback/edhr-approval/detail?id=" + task.getExecutionId()
                    + "&workTaskId=" + task.getId();
        }
        if (task.getExecutionId() == null) {
            return "/mes/pro/feedback/edhr-batch-execution/detail?id=" + task.getBatchExecutionId()
                    + "&batchTaskId=" + task.getBatchTaskId()
                    + "&workTaskId=" + task.getId();
        }
        return buildExecutionActionUrl(task, task.getExecutionId());
    }

    private String buildExecutionActionUrl(MesProEdhrWorkTaskDO task, Long executionId) {
        StringBuilder url = new StringBuilder("/mes/pro/feedback/edhr-execution/form")
                .append("?id=").append(executionId)
                .append("&executionId=").append(executionId)
                .append("&workTaskId=").append(task.getId())
                .append("&fillCarrier=FORM")
                .append("&recordCategory=BATCH_RECORD");
        if (task.getBatchExecutionId() != null) {
            url.append("&batchExecutionId=").append(task.getBatchExecutionId());
        }
        if (task.getBatchTaskId() != null) {
            url.append("&batchTaskId=").append(task.getBatchTaskId());
        }
        return url.toString();
    }

    private void recordWorkTaskRuleSaveAudit(Long routeId, String taskType,
                                             List<MesProEdhrWorkTaskAssignmentRuleDO> beforeRules,
                                             List<MesProEdhrWorkTaskAssignmentRuleDO> afterRules,
                                             String reason) {
        List<Map<String, Object>> beforePayload = beforeRules.stream()
                .map(this::toRuleAuditPayload)
                .toList();
        List<Map<String, Object>> afterPayload = afterRules.stream()
                .map(this::toRuleAuditPayload)
                .toList();
        Map<String, Object> metadata = new LinkedHashMap<>();
        String requestId = "EDHR-WORK-RULE-" + java.util.UUID.randomUUID();
        metadata.put("requestSource", "WORK_TASK_RULE_CONFIG");
        metadata.put("idempotencyKey", requestId);
        metadata.put("routeId", routeId);
        metadata.put("taskType", taskType);
        metadata.put("reason", reason);
        metadata.put("associatedSignatureId", "NOT_APPLICABLE");
        metadata.put("beforeRules", beforePayload);
        metadata.put("afterRules", afterPayload);
        metadata.put("permissionDecision", "ALLOW");
        metadata.put("resultStatus", "SUCCESS");
        operationAuditService.record(new MesProEdhrOperationAuditCommand()
                .setRequestId(requestId)
                .setObjectType("WORK_TASK_ASSIGNMENT_RULE")
                .setObjectId(afterRules.isEmpty() ? routeId + ":" + taskType : String.valueOf(afterRules.get(0).getId()))
                .setRouteId(routeId)
                .setOperationType("WORK_TASK_RULE_SAVE")
                .setActionName("保存 eDHR 工作任务规则")
                .setActorUserId(requireLoginUserId())
                .setActorUsername(SecurityFrameworkUtils.getLoginUserNickname())
                .setPermissionCode("mes:pro-edhr-work-task-rule:update")
                .setPermissionDecision("ALLOW")
                .setResultStatus("SUCCESS")
                .setBeforeSummaryHash(hashAuditPayload(beforePayload))
                .setAfterSummaryHash(hashAuditPayload(afterPayload))
                .setMetadataJson(JSON.toJSONString(metadata)));
    }

    private Map<String, Object> toRuleAuditPayload(MesProEdhrWorkTaskAssignmentRuleDO rule) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (rule == null) {
            return payload;
        }
        payload.put("id", rule.getId());
        payload.put("scopeType", rule.getScopeType());
        payload.put("scopeId", rule.getScopeId());
        payload.put("taskType", rule.getTaskType());
        payload.put("routeProcessId", rule.getRouteProcessId());
        payload.put("assigneeUserId", rule.getAssigneeUserId());
        payload.put("reviewUserId", rule.getReviewUserId());
        payload.put("candidateSourceType", rule.getCandidateSourceType());
        payload.put("candidateSourceId", rule.getCandidateSourceId());
        payload.put("dueMinutes", rule.getDueMinutes());
        payload.put("enabled", rule.getEnabled());
        payload.put("remark", rule.getRemark());
        return payload;
    }

    private void recordCandidateSignatureCompleteAudit(MesProEdhrWorkTaskDO beforeTask,
                                                       MesProEdhrWorkTaskDO completedTask,
                                                       List<MesProEdhrWorkTaskDO> canceledPeersBefore,
                                                       List<MesProEdhrWorkTaskDO> canceledPeersAfter,
                                                       String reason,
                                                       Long actorUserId) {
        Map<String, Object> beforePayload = toWorkTaskAuditPayload(beforeTask);
        Map<String, Object> afterPayload = toWorkTaskAuditPayload(completedTask);
        List<Map<String, Object>> canceledBeforePayload = canceledPeersBefore.stream()
                .map(this::toWorkTaskAuditPayload)
                .toList();
        List<Map<String, Object>> canceledAfterPayload = canceledPeersAfter.stream()
                .map(this::toWorkTaskAuditPayload)
                .toList();
        Map<String, Object> metadata = new LinkedHashMap<>();
        String requestId = "EDHR-CANDIDATE-SIGNATURE-" + java.util.UUID.randomUUID();
        String signatureBindingId = beforeTask.getExecutionId() + ":" + beforeTask.getSignatureCellKey();
        metadata.put("requestSource", "CANDIDATE_SIGNATURE_TASK");
        metadata.put("idempotencyKey", requestId);
        metadata.put("reason", reason);
        metadata.put("signatureCellKey", beforeTask.getSignatureCellKey());
        metadata.put("signatureBindingId", signatureBindingId);
        metadata.put("associatedSignatureId", signatureBindingId);
        metadata.put("completedTask", afterPayload);
        metadata.put("canceledCandidateTasksBefore", canceledBeforePayload);
        metadata.put("canceledCandidateTasksAfter", canceledAfterPayload);
        metadata.put("permissionDecision", "ALLOW");
        metadata.put("resultStatus", "SUCCESS");
        operationAuditService.record(new MesProEdhrOperationAuditCommand()
                .setRequestId(requestId)
                .setObjectType("WORK_TASK")
                .setObjectId(String.valueOf(beforeTask.getId()))
                .setBatchExecutionId(beforeTask.getBatchExecutionId())
                .setExecutionId(beforeTask.getExecutionId())
                .setWorkTaskId(beforeTask.getId())
                .setRouteId(beforeTask.getRouteId())
                .setRouteProcessId(beforeTask.getRouteProcessId())
                .setOperationType("CANDIDATE_SIGNATURE_COMPLETE")
                .setActionName("完成候选签名任务")
                .setActorUserId(actorUserId)
                .setActorUsername(SecurityFrameworkUtils.getLoginUserNickname())
                .setPermissionCode("mes:pro-edhr-work-task:update")
                .setPermissionDecision("ALLOW")
                .setResultStatus("SUCCESS")
                .setBeforeSummaryHash(hashAuditPayload(beforePayload))
                .setAfterSummaryHash(hashAuditPayload(Map.of(
                        "completedTask", afterPayload,
                        "canceledCandidateTasks", canceledAfterPayload)))
                .setMetadataJson(JSON.toJSONString(metadata)));
    }

    private void recordFillTaskReassignAudit(MesProEdhrWorkTaskDO beforeTask,
                                             MesProEdhrWorkTaskDO afterTask,
                                             String reason,
                                             boolean notificationSent) {
        Map<String, Object> beforePayload = toWorkTaskAuditPayload(beforeTask);
        Map<String, Object> afterPayload = toWorkTaskAuditPayload(afterTask);
        Map<String, Object> metadata = new LinkedHashMap<>();
        String requestId = "EDHR-FILL-TASK-REASSIGN-" + java.util.UUID.randomUUID();
        metadata.put("requestSource", "WORK_TASK_CENTER");
        metadata.put("idempotencyKey", requestId);
        metadata.put("reason", reason);
        metadata.put("oldAssigneeUserId", beforeTask.getAssigneeUserId());
        metadata.put("newAssigneeUserId", afterTask.getAssigneeUserId());
        metadata.put("authorizationSyncResult", "SYNCED");
        metadata.put("notificationResult", notificationSent ? "SENT" : "SKIPPED_OWNER_UNCHANGED");
        metadata.put("beforeTask", beforePayload);
        metadata.put("afterTask", afterPayload);
        metadata.put("associatedSignatureId", "NOT_APPLICABLE");
        metadata.put("permissionDecision", "ALLOW");
        metadata.put("resultStatus", "SUCCESS");
        operationAuditService.record(new MesProEdhrOperationAuditCommand()
                .setRequestId(requestId)
                .setObjectType("WORK_TASK")
                .setObjectId(String.valueOf(beforeTask.getId()))
                .setBatchExecutionId(beforeTask.getBatchExecutionId())
                .setExecutionId(beforeTask.getExecutionId())
                .setWorkTaskId(beforeTask.getId())
                .setRouteId(beforeTask.getRouteId())
                .setRouteProcessId(beforeTask.getRouteProcessId())
                .setOperationType("FILL_TASK_REASSIGN")
                .setActionName("重新派发 eDHR 填写任务")
                .setActorUserId(requireLoginUserId())
                .setActorUsername(SecurityFrameworkUtils.getLoginUserNickname())
                .setPermissionCode("mes:pro-edhr-work-task:update")
                .setPermissionDecision("ALLOW")
                .setResultStatus("SUCCESS")
                .setBeforeSummaryHash(hashAuditPayload(beforePayload))
                .setAfterSummaryHash(hashAuditPayload(afterPayload))
                .setMetadataJson(JSON.toJSONString(metadata)));
    }

    private Map<String, Object> toWorkTaskAuditPayload(MesProEdhrWorkTaskDO task) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (task == null) {
            return payload;
        }
        payload.put("id", task.getId());
        payload.put("taskType", task.getTaskType());
        payload.put("batchExecutionId", task.getBatchExecutionId());
        payload.put("batchTaskId", task.getBatchTaskId());
        payload.put("executionId", task.getExecutionId());
        payload.put("routeId", task.getRouteId());
        payload.put("routeProcessId", task.getRouteProcessId());
        payload.put("assigneeUserId", task.getAssigneeUserId());
        payload.put("candidateSourceType", task.getCandidateSourceType());
        payload.put("candidateSourceId", task.getCandidateSourceId());
        payload.put("candidateUserSnapshot", task.getCandidateUserSnapshot());
        payload.put("responsibilitySourceType", task.getResponsibilitySourceType());
        payload.put("responsibilitySourceKey", task.getResponsibilitySourceKey());
        payload.put("responsibilitySourceVersion", task.getResponsibilitySourceVersion());
        payload.put("responsibilitySourceDigest", task.getResponsibilitySourceDigest());
        payload.put("ownershipLocked", task.getOwnershipLocked());
        payload.put("signatureCellKey", task.getSignatureCellKey());
        payload.put("status", task.getStatus());
        payload.put("completedAt", task.getCompletedAt());
        payload.put("reason", task.getReason());
        payload.put("remark", task.getRemark());
        return payload;
    }

    private String hashAuditPayload(Object payload) {
        return MesProBatchRecordExecutionFieldAuditHasher.sha256(JSON.toJSONString(payload));
    }

    private Long requireLoginUserId() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            throw exception(UNAUTHORIZED);
        }
        return userId;
    }
}
