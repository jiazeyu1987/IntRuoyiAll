package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskStatus;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderDossierFileMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesWorkTaskAuxiliaryAudit;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserReqDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesEdhrBatchLifecycleGuard;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrOperationAuditService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrOperationAuditCommand;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Explicit responsibility and completion lifecycle for active-order dossier categories. */
@Service
@RequiredArgsConstructor
public class MesActiveOrderDossierCompletionService {
    public static final String TASK_TYPE = "DOSSIER_COMPLETE";
    public static final String SCOPE_TYPE = "ACTIVE_ORDER_DOSSIER";
    private static final List<String> CATEGORIES = List.of("INCOMING_INSPECTION_FILE", "STERILIZATION_FILE", "FINISHED_PRODUCT_FILE", "OTHER_FILE");
    private final MesProcessPoolActiveOrderMapper activeOrderMapper;
    private final MesProcessPoolActiveOrderDossierFileMapper fileMapper;
    private final MesProEdhrWorkTaskMapper taskMapper;
    private final PermissionApi permissionApi;
    private final NotifyMessageSendApi notifyApi;
    private final MesWorkTaskAuxiliaryAudit auxiliaryAudit;
    private final AdminUserApi adminUserApi;
    private final MesProWorkOrderMapper workOrderMapper;
    private final MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper;
    private final MesProEdhrNonconformanceReviewService nonconformanceReviewService;
    private final MesEdhrBatchLifecycleGuard lifecycleGuard;
    private final MesProEdhrOperationAuditService operationAuditService;
    private final GxpAuditService gxpAuditService;
    @jakarta.annotation.Resource
    private cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesEdhrTaskDeadlineService deadlineService;
    private final cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrNonconformanceReviewMapper reviewMapper;
    @jakarta.annotation.Resource
    private cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper batchMapper;

    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrWorkTaskDO assign(Long actor, Long activeOrderId, String category, Long assignee, String reason) {
        requireCategory(category);
        if (assignee == null || assignee <= 0 || reason == null || reason.isBlank() || reason.length() > 1000)
            throw new IllegalArgumentException("资料责任人及指派原因必须明确提供");
        MesProcessPoolActiveOrderDO order = requireLeader(actor, activeOrderId);
        requireMutable(order);
        adminUserApi.validateUser(assignee);
        if (!permissionApi.hasAnyPermissions(assignee, "mes:pro-edhr-batch-execution:upload")
                || !permissionApi.hasAnyPermissions(assignee, "mes:pro-edhr-work-task:query"))
            throw new IllegalArgumentException("资料责任人必须具备资料上传和个人待办权限");
        var old = activeTask(activeOrderId, category);
        if (old != null && Objects.equals(old.getAssigneeUserId(), assignee) && currentRound(old)) return old;
        boolean transferWithinRound = old != null && currentRound(old);
        if (old != null) {
            old.setStatus(MesProEdhrWorkTaskStatus.CANCELED).setCompletedAt(LocalDateTime.now()).setRemark("资料责任人已转派：" + reason);
            if (taskMapper.updateById(old) != 1) throw new IllegalStateException("旧资料待办撤销失败");
            audit(old, actor, "DOSSIER_REASSIGN", "资料责任人转派", null);
        }
        var workOrder = workOrderMapper.selectById(order.getWorkOrderId());
        if (workOrder == null || workOrder.getCode() == null || workOrder.getCode().isBlank()
                || !Objects.equals(workOrder.getTenantId(), order.getTenantId()))
            throw new IllegalStateException("资料指派缺少同租户正式工单身份");
        MesProEdhrWorkTaskDO task = new MesProEdhrWorkTaskDO().setTaskCode("EDHRD-" + System.currentTimeMillis())
                .setTaskType(TASK_TYPE).setBusinessScopeType(SCOPE_TYPE).setBusinessScopeId(activeOrderId)
                .setWorkOrderId(order.getWorkOrderId()).setWorkOrderCode(workOrder.getCode()).setBatchCode(workOrder.getBatchCode())
                .setRouteId(order.getRouteId())
                .setProcessName(categoryLabel(category)).setReviewSourceType(category)
                .setAssigneeUserId(assignee).setCandidateUserSnapshot(String.valueOf(assignee))
                .setSourceUserId(actor).setResponsibilitySourceType("EXPLICIT_DOSSIER_ASSIGNMENT")
                .setResponsibilitySourceKey("ACTIVE_ORDER:" + activeOrderId + ":" + category)
                .setOwnershipLocked(true).setStatus(MesProEdhrWorkTaskStatus.TODO).setReason(reason)
                .setActionUrl("");
        var application = applicationMapper.selectLatestByActiveOrderId(activeOrderId);
        task.setReviewSourceId(application == null ? null : application.getId());
        if (transferWithinRound) task.setDueTime(old.getDueTime());
        else deadlineService.applyToWorkTask(task);
        if (taskMapper.insert(task) != 1 || task.getId() == null) throw new IllegalStateException("资料待办创建失败");
        task.setActionUrl("/mes/pro/feedback/edhr-dossier-completion?workTaskId=" + task.getId());
        if (taskMapper.updateById(new MesProEdhrWorkTaskDO().setId(task.getId()).setActionUrl(task.getActionUrl())) != 1)
            throw new IllegalStateException("资料待办入口保存失败");
        audit(task, actor, "DOSSIER_ASSIGN", "指派资料责任人", null);
        var params = Map.<String,Object>of("workOrderCode", task.getWorkOrderCode(), "processName", task.getProcessName(),
                "actionUrl", task.getActionUrl(), "reason", reason, "workTaskId", task.getId());
        var notice = new NotifySendSingleToUserReqDTO();
        notice.setUserId(assignee); notice.setTemplateCode("MES_EDHR_DOSSIER_COMPLETE_ASSIGNED"); notice.setTemplateParams(params);
        auxiliaryAudit.notifications(task, () -> List.of(notifyApi.sendSingleMessageToAdmin(notice)));
        return task;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean completeUploadedCategory(Long actor, Long activeOrderId, String category, Long attachmentId) {
        requireActor(actor);
        requireCategory(category);
        var task = activeTask(activeOrderId, category);
        if (task == null) return false;
        if (!Objects.equals(task.getAssigneeUserId(), actor)
                || !Objects.equals(task.getBusinessScopeId(), activeOrderId)
                || !Objects.equals(task.getReviewSourceType(), category))
            return false;
        if (!currentRound(task)) throw new IllegalStateException("资料责任任务的放行轮次已过期，请重新交接");
        var file = fileMapper.selectById(attachmentId);
        if (file == null || !Objects.equals(file.getActiveOrderId(), activeOrderId)
                || !Objects.equals(file.getCategoryKey(), category) || !Objects.equals(file.getOperatorId(), actor))
            throw new IllegalStateException("资料附件与责任人、订单或类别不一致");
        if (taskMapper.update(new MesProEdhrWorkTaskDO().setStatus("DONE").setCompletedAt(LocalDateTime.now()),
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<MesProEdhrWorkTaskDO>()
                        .eq(MesProEdhrWorkTaskDO::getId, task.getId()).eq(MesProEdhrWorkTaskDO::getAssigneeUserId, actor)
                        .in(MesProEdhrWorkTaskDO::getStatus, MesProEdhrWorkTaskStatus.TODO, MesProEdhrWorkTaskStatus.DOING, MesProEdhrWorkTaskStatus.OVERDUE)) != 1)
            throw new IllegalStateException("资料待办状态已改变，不能确认补齐");
        audit(task, actor, "DOSSIER_COMPLETE", "资料已上传补齐", attachmentId);
        return true;
    }

    private MesProEdhrWorkTaskDO activeTask(Long orderId, String category) {
        var rows = taskMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MesProEdhrWorkTaskDO>()
                .eq(MesProEdhrWorkTaskDO::getBusinessScopeType, SCOPE_TYPE).eq(MesProEdhrWorkTaskDO::getBusinessScopeId, orderId)
                .eq(MesProEdhrWorkTaskDO::getTaskType, TASK_TYPE).eq(MesProEdhrWorkTaskDO::getReviewSourceType, category)
                .in(MesProEdhrWorkTaskDO::getStatus, MesProEdhrWorkTaskStatus.TODO, MesProEdhrWorkTaskStatus.DOING, MesProEdhrWorkTaskStatus.OVERDUE));
        if (rows.size() > 1) throw new IllegalStateException("同订单、同资料类别存在重复责任待办");
        return rows.isEmpty() ? null : rows.get(0);
    }

    private MesProcessPoolActiveOrderDO requireLeader(Long actor, Long orderId) {
        requireActor(actor);
        gxpAuditService.acquireLedgerLock();
        var order = activeOrderMapper.selectByIdForUpdate(orderId);
        if (order == null || !Objects.equals(order.getLeaderUserId(), actor)
                || !Objects.equals(order.getTenantId(), TenantContextHolder.getRequiredTenantId())
                || !permissionApi.hasAnyPermissions(actor, "mes:pro-process-pool-team-leader:maintain"))
            throw new IllegalStateException("仅当前活跃订单生产组长可指派资料责任人");
        return order;
    }
    public boolean isAssigned(Long actor, Long orderId, String category) {
        requireActor(actor);
        var task = activeTask(orderId, category);
        return task != null && Objects.equals(task.getAssigneeUserId(), actor) && currentRound(task);
    }
    public void validateCompletionUpload(Long actor, Long taskId, Long activeOrderId, String category) {
        requireActor(actor);
        var task = activeTask(activeOrderId, category);
        if (task == null || !Objects.equals(task.getId(), taskId) || !Objects.equals(task.getAssigneeUserId(), actor)
                || !currentRound(task) || !permissionApi.hasAnyPermissions(actor, "mes:pro-edhr-batch-execution:upload"))
            throw new IllegalStateException("资料责任待办已变更、轮次已过期或不属于当前上传人");
    }
    private boolean currentRound(MesProEdhrWorkTaskDO task) {
        var app = applicationMapper.selectLatestByActiveOrderId(task.getBusinessScopeId());
        // A pre-application dossier obligation remains attached to its active order; an application-bound task cannot cross rounds.
        return task.getReviewSourceId() == null || app != null && Objects.equals(task.getReviewSourceId(), app.getId());
    }
    public boolean hasAssignment(Long actor, Long orderId) {
        return CATEGORIES.stream().anyMatch(category -> isAssigned(actor, orderId, category));
    }
    @Transactional(readOnly = true)
    public List<Responsibility> responsibilities(Long actor, Long orderId) {
        requireActor(actor);
        var order = activeOrderMapper.selectById(orderId);
        if (order == null || !Objects.equals(order.getTenantId(), TenantContextHolder.getRequiredTenantId())
                || !Objects.equals(order.getLeaderUserId(), actor)
                || !permissionApi.hasAnyPermissions(actor, "mes:pro-process-pool-team-leader:maintain"))
            throw new IllegalStateException("仅当前订单生产组长可查看资料指派");
        var rows = taskMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MesProEdhrWorkTaskDO>()
                .eq(MesProEdhrWorkTaskDO::getTaskType, TASK_TYPE).eq(MesProEdhrWorkTaskDO::getBusinessScopeType, SCOPE_TYPE)
                .eq(MesProEdhrWorkTaskDO::getBusinessScopeId, orderId).orderByDesc(MesProEdhrWorkTaskDO::getId));
        var latest = new java.util.LinkedHashMap<String,Responsibility>();
        for (var task : rows) {
            if (latest.containsKey(task.getReviewSourceType())) continue;
            var user = adminUserApi.getUser(task.getAssigneeUserId());
            if (user == null) throw new IllegalStateException("资料任务责任人不存在");
            latest.put(task.getReviewSourceType(), new Responsibility(task.getReviewSourceType(), task.getId(),
                    task.getAssigneeUserId(), user.getNickname(), task.getStatus(), task.getReason()));
        }
        return List.copyOf(latest.values());
    }
    @Transactional(readOnly = true)
    public TaskDetail get(Long actor, Long taskId) {
        requireActor(actor);
        var task = taskMapper.selectById(taskId);
        if (task == null || !TASK_TYPE.equals(task.getTaskType()) || !SCOPE_TYPE.equals(task.getBusinessScopeType())
                || !Objects.equals(task.getAssigneeUserId(), actor)
                || !permissionApi.hasAnyPermissions(actor, "mes:pro-edhr-work-task:query"))
            throw new IllegalStateException("当前用户无权查看该资料责任待办");
        var order = activeOrderMapper.selectById(task.getBusinessScopeId());
        if (order == null || !Objects.equals(order.getTenantId(), TenantContextHolder.getRequiredTenantId())
                || !Objects.equals(order.getWorkOrderId(), task.getWorkOrderId()))
            throw new IllegalStateException("资料任务与正式订单归属不一致");
        requireCategory(task.getReviewSourceType());
        var app = applicationMapper.selectLatestByActiveOrderId(order.getId());
        boolean active = List.of("TODO", "DOING", "OVERDUE").contains(task.getStatus());
        return new TaskDetail(task.getId(), order.getId(), app == null ? null : app.getId(), task.getReviewSourceType(),
                task.getProcessName(), task.getWorkOrderCode(), task.getReason(), task.getStatus(),
                active && currentRound(task) && permissionApi.hasAnyPermissions(actor, "mes:pro-edhr-batch-execution:upload") && mutable(order));
    }
    private void requireMutable(MesProcessPoolActiveOrderDO order) {
        if (!mutable(order)) throw new IllegalStateException("订单已冻结、关闭、作废或上市放行，不能指派资料补件");
        nonconformanceReviewService.ensureWorkOrderNotFrozen(order.getWorkOrderId(), "资料责任交接");
        var app = applicationMapper.selectLatestByActiveOrderId(order.getId());
        if (app != null && app.getBatchExecutionId() != null) lifecycleGuard.requireDossierMutable(app.getBatchExecutionId());
    }
    private boolean mutable(MesProcessPoolActiveOrderDO order) {
        var app = applicationMapper.selectLatestByActiveOrderId(order.getId());
        if (app != null && app.getBatchExecutionId() != null) {
            var batch = batchMapper.selectById(app.getBatchExecutionId());
            if (batch == null || !Objects.equals(batch.getTenantId(), order.getTenantId()) || batch.getStatus() == null
                    || List.of(15, 40, 50, 60).contains(batch.getStatus())) return false;
        }
        return "ACTIVE".equals(order.getActiveStatus())
                && reviewMapper.selectFirstBlockingByWorkOrderId(order.getWorkOrderId()) == null
                && (app == null || !List.of("RELEASED", "BATCH_VOIDED").contains(Objects.toString(app.getApplicationStatus(), "")))
                && !List.of("FROZEN", "CLOSED", "VOIDED", "RELEASED").contains(Objects.toString(order.getBusinessStatus(), ""));
    }
    private void requireActor(Long actor) {
        var login = SecurityFrameworkUtils.getLoginUser();
        if (actor == null || login == null || !Objects.equals(login.getId(), actor)
                || !Objects.equals(login.getTenantId(), TenantContextHolder.getRequiredTenantId()))
            throw new IllegalStateException("资料任务操作者与当前登录身份或租户不一致");
    }
    private void audit(MesProEdhrWorkTaskDO task, Long actor, String operation, String action, Long attachmentId) {
        var metadata = new java.util.LinkedHashMap<String,Object>();
        metadata.put("activeOrderId", task.getBusinessScopeId()); metadata.put("categoryKey", task.getReviewSourceType());
        metadata.put("assigneeUserId", task.getAssigneeUserId()); metadata.put("attachmentId", attachmentId);
        operationAuditService.recordInCallerTransaction(new MesProEdhrOperationAuditCommand().setRequestId(java.util.UUID.randomUUID().toString())
                .setPermissionDecision("ALLOW").setObjectType("ACTIVE_ORDER_DOSSIER")
                .setObjectId(String.valueOf(task.getBusinessScopeId())).setWorkTaskId(task.getId())
                .setOperationType(operation).setActionName(action).setActorUserId(actor).setResultStatus("SUCCESS")
                .setMetadataJson(JsonUtils.toJsonString(metadata)).setOccurredAt(LocalDateTime.now()));
    }
    public record TaskDetail(Long workTaskId, Long activeOrderId, Long applicationId, String categoryKey,
                             String categoryLabel, String workOrderCode, String reason, String status, boolean writable) { }
    public record Responsibility(String categoryKey, Long workTaskId, Long assigneeUserId, String assigneeUserName,
                                 String status, String reason) { }
    private static void requireCategory(String category) {
        if (category == null || !CATEGORIES.contains(category)) throw new IllegalArgumentException("资料类别无效");
    }
    private static String categoryLabel(String key) { return switch (key) {
        case "INCOMING_INSPECTION_FILE" -> "来料检文件"; case "STERILIZATION_FILE" -> "灭菌文件";
        case "FINISHED_PRODUCT_FILE" -> "成品检文件"; case "OTHER_FILE" -> "其他文件"; default -> throw new IllegalArgumentException("资料类别无效");
    }; }
}
