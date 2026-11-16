package cn.iocoder.yudao.module.bpm.service.task;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.datapermission.core.annotation.DataPermission;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.*;
import cn.iocoder.yudao.module.bpm.dal.mysql.task.BpmSupervisorHandoverMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.businessapproval.BusinessApprovalRequestMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper;
import cn.iocoder.yudao.module.bpm.enums.task.BpmCommentTypeEnum;
import cn.iocoder.yudao.module.bpm.enums.definition.BpmUserTaskAssignStartUserHandlerTypeEnum;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.candidate.strategy.other.BpmTaskCandidateMixedStrategy;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmTaskCandidateStrategyEnum;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.util.BpmnModelUtils;
import cn.iocoder.yudao.module.system.api.permission.ApprovalAuthorityApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import jakarta.annotation.Resource;
import org.flowable.engine.*;
import org.flowable.task.api.Task;
import org.flowable.task.api.DelegationState;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

/** 只显式交接普通 BPM 主管待办，业务域审批必须走其自己的交接入口。 */
@Service
@DataPermission(enable = false)
public class BpmSupervisorHandoverService {
    public static final String HANDOVER_VARIABLE = "systemSupervisorHandover";
    private static final ErrorCode HANDOVER_REJECTED = new ErrorCode(1_009_002_080, "主管待办交接失败：{}");
    private static final Set<Integer> SUPERVISOR_STRATEGIES = Set.of(
            BpmTaskCandidateStrategyEnum.DEPT_LEADER.getStrategy(),
            BpmTaskCandidateStrategyEnum.MULTI_DEPT_LEADER_MULTI.getStrategy(),
            BpmTaskCandidateStrategyEnum.START_USER_DEPT_LEADER.getStrategy(),
            BpmTaskCandidateStrategyEnum.START_USER_DEPT_LEADER_MULTI.getStrategy(),
            BpmTaskCandidateStrategyEnum.FORM_DEPT_LEADER.getStrategy());
    @Resource private TaskService taskService;
    @Resource private RuntimeService runtimeService;
    @Resource private RepositoryService repositoryService;
    @Resource private AdminUserApi adminUserApi;
    @Resource private ApprovalAuthorityApi approvalAuthority;
    @Resource private BpmSupervisorHandoverMapper handoverMapper;
    @Resource private BusinessApprovalRequestMapper requestMapper;
    @Resource private FormActionInstanceMapper formMapper;

    public PageResult<BpmSupervisorHandoverRespVO> getPage(PageParam page, Long assigneeUserId, Long applicantUserId) {
        if (assigneeUserId == null) throw exception(HANDOVER_REJECTED, "请指定原处理人");
        var query = taskService.createTaskQuery().active()
                .taskTenantId(String.valueOf(TenantContextHolder.getRequiredTenantId()))
                .taskAssignee(assigneeUserId.toString())
                .orderByTaskCreateTime().desc();
        if (applicantUserId != null) {
            var processes = runtimeService.createProcessInstanceQuery()
                    .processInstanceTenantId(String.valueOf(TenantContextHolder.getRequiredTenantId()))
                    .startedBy(applicantUserId.toString()).list();
            if (processes.isEmpty()) return PageResult.empty();
            query.processInstanceIdIn(processes.stream().map(org.flowable.engine.runtime.ProcessInstance::getId).toList());
        }
        long total = query.count();
        if (total == 0) return PageResult.empty();
        List<Task> tasks = query.listPage((page.getPageNo() - 1) * page.getPageSize(), page.getPageSize());
        List<Long> ids = tasks.stream().map(Task::getAssignee).filter(StrUtil::isNotBlank)
                .map(Long::valueOf).distinct().toList();
        var users = adminUserApi.getUserMap(ids);
        List<BpmSupervisorHandoverRespVO> result = new ArrayList<>();
        for (Task task : tasks) {
            var user = StrUtil.isBlank(task.getAssignee()) ? null : users.get(Long.valueOf(task.getAssignee()));
            String blocker = blocker(task);
            if (blocker == null && applicantUserId != null && !isApplicantSupervisorTask(task)) {
                blocker = "该节点不按申请人的主管分配，请使用原流程的转办入口";
            }
            result.add(new BpmSupervisorHandoverRespVO().setId(task.getId()).setName(task.getName())
                    .setProcessInstanceId(task.getProcessInstanceId()).setAssigneeUserId(task.getAssignee())
                    .setAssigneeName(user == null ? null : user.getNickname())
                    .setAssigneeAvailable(user != null && CommonStatusEnum.isEnable(user.getStatus()))
                    .setCreateTime(task.getCreateTime()).setTransferable(blocker == null).setBlocker(blocker));
        }
        return new PageResult<>(result, total);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handover(Long actorId, BpmSupervisorHandoverReqVO request) {
        // 与用户停用使用一致锁顺序，避免选择人校验后立即失效。
        var target = adminUserApi.lockSignatureIdentity(request.getAssigneeUserId());
        if (target == null || !CommonStatusEnum.isEnable(target.getStatus())) {
            throw exception(HANDOVER_REJECTED, "接收人不存在或已停用");
        }
        Long tenant = TenantContextHolder.getRequiredTenantId();
        approvalAuthority.lockTenantApprovalAuthority(tenant);
        if (handoverMapper.lockTask(request.getId(), tenant.toString()) == null) {
            throw exception(HANDOVER_REJECTED, "任务已结束或不属于当前租户，请刷新");
        }
        Task task = taskService.createTaskQuery().taskId(request.getId()).taskTenantId(tenant.toString()).singleResult();
        if (task == null || !Objects.equals(task.getAssignee(), request.getPreviousAssigneeUserId().toString())) {
            throw exception(HANDOVER_REJECTED, "原处理人已变化，请刷新后重新选择");
        }
        String blocker = blocker(task);
        if (blocker != null) throw exception(HANDOVER_REJECTED, blocker);
        if (Objects.equals(request.getPreviousAssigneeUserId(), target.getId())) {
            throw exception(HANDOVER_REJECTED, "接收人不能与原处理人相同");
        }
        var process = runtimeService.createProcessInstanceQuery().processInstanceId(task.getProcessInstanceId())
                .processInstanceTenantId(tenant.toString()).singleResult();
        if (process == null || Objects.equals(process.getStartUserId(), target.getId().toString())) {
            throw exception(HANDOVER_REJECTED, "接收人不能是申请人，或流程已结束");
        }
        if (request.getApplicantUserId() != null) {
            if (!Objects.equals(process.getStartUserId(), request.getApplicantUserId().toString())
                    || !isApplicantSupervisorTask(task)) {
                throw exception(HANDOVER_REJECTED, "该待办不属于本次员工主管变更");
            }
            var relation = adminUserApi.getDirectSupervisorRelation(request.getApplicantUserId());
            if (!Objects.equals(relation.getSupervisorUserId(), target.getId())) {
                throw exception(HANDOVER_REJECTED, "员工的直属主管已变化，请重新读取后办理交接");
            }
        }
        var actor = adminUserApi.getUser(actorId);
        if (actor == null) throw exception(HANDOVER_REJECTED, "操作人不存在");
        // 保持待审状态，阻止分配监听器按历史去重自动审批。
        taskService.setVariableLocal(task.getId(), HANDOVER_VARIABLE, true);
        taskService.addComment(task.getId(), task.getProcessInstanceId(), BpmCommentTypeEnum.TRANSFER.getType(),
                "主管待办交接：操作人 " + actor.getNickname() + "（" + actorId + "），原处理人编号 "
                        + task.getAssignee() + "，接收人 " + target.getNickname() + "（" + target.getId()
                        + "）；原因：" + request.getReason().trim());
        if (StrUtil.isBlank(task.getOwner())) taskService.setOwner(task.getId(), task.getAssignee());
        taskService.setAssignee(task.getId(), target.getId().toString());
    }

    private boolean isApplicantSupervisorTask(Task task) {
        var model = repositoryService.getBpmnModel(task.getProcessDefinitionId());
        var element = BpmnModelUtils.getFlowElementById(model, task.getTaskDefinitionKey());
        Integer strategy = BpmnModelUtils.parseCandidateStrategy(element);
        if (Objects.equals(strategy, BpmTaskCandidateStrategyEnum.START_USER_DEPT_LEADER.getStrategy())
                || Objects.equals(strategy, BpmTaskCandidateStrategyEnum.START_USER_DEPT_LEADER_MULTI.getStrategy())) return true;
        if (Objects.equals(strategy, BpmTaskCandidateStrategyEnum.MIXED.getStrategy())) {
            var entries = JsonUtils.parseArray(BpmnModelUtils.parseCandidateParam(element), BpmTaskCandidateMixedStrategy.Entry.class);
            return entries != null && entries.stream().anyMatch(entry ->
                    Objects.equals(entry.getStrategy(), BpmTaskCandidateStrategyEnum.START_USER_DEPT_LEADER.getStrategy()));
        }
        return Objects.equals(BpmnModelUtils.parseAssignStartUserHandlerType(element),
                BpmUserTaskAssignStartUserHandlerTypeEnum.TRANSFER_DEPT_LEADER.getType());
    }

    private String blocker(Task task) {
        if (task.isSuspended()) return "流程已挂起";
        if (StrUtil.isBlank(task.getAssignee())) return "任务尚未分配处理人";
        if (task.getDelegationState() == DelegationState.PENDING) return "任务正在委派，请先完成委派处理";
        if (StrUtil.isBlank(task.getProcessDefinitionId())) return "缺少流程定义";
        var definition = repositoryService.getProcessDefinition(task.getProcessDefinitionId());
        if (definition == null) return "流程定义不存在";
        if (Set.of("mes-edhr-approval-v1", "dcc-controlled-file-approval").contains(definition.getKey())) {
            return "请通过 eDHR/DCC 对应业务流程处理交接";
        }
        Long tenant = TenantContextHolder.getRequiredTenantId();
        if (!requestMapper.selectOwnershipByTenantAndProcessId(tenant, task.getProcessInstanceId()).isEmpty()
                || !formMapper.selectOwnershipByTenantAndProcessId(tenant, task.getProcessInstanceId()).isEmpty()) {
            return "该待办由业务模块管理，请通过对应业务入口交接";
        }
        var model = repositoryService.getBpmnModel(task.getProcessDefinitionId());
        if (model == null) return "流程模型不存在";
        var element = BpmnModelUtils.getFlowElementById(model, task.getTaskDefinitionKey());
        if (element == null) return "审批节点不存在";
        Integer strategy = BpmnModelUtils.parseCandidateStrategy(element);
        if (strategy != null && SUPERVISOR_STRATEGIES.contains(strategy)) return null;
        if (Objects.equals(strategy, BpmTaskCandidateStrategyEnum.MIXED.getStrategy())) {
            var entries = JsonUtils.parseArray(BpmnModelUtils.parseCandidateParam(element), BpmTaskCandidateMixedStrategy.Entry.class);
            if (entries != null && entries.stream().anyMatch(entry ->
                    Objects.equals(entry.getStrategy(), BpmTaskCandidateStrategyEnum.START_USER_DEPT_LEADER.getStrategy()))) return null;
        }
        if (Objects.equals(BpmnModelUtils.parseAssignStartUserHandlerType(element),
                BpmUserTaskAssignStartUserHandlerTypeEnum.TRANSFER_DEPT_LEADER.getType())) return null;
        return "非主管相关待办，请使用原流程的转办入口";
    }
}
