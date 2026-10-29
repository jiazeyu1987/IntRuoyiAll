package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDeviationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDeviationHandlingDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskStatus;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserReqDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_DEVIATION_HANDLING_PERMISSION_DENIED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskErrorCodeConstants.PRO_EDHR_WORK_TASK_ASSIGNEE_INVALID;

/** Version-bound assignments for the existing deviation responsibilities; no inferred recipients. */
@Service
public class MesProEdhrDeviationReviewTaskService {
    @Resource private MesEdhrTaskDeadlineService taskDeadlineService;
    public static final String SIGN = "DEVIATION_SIGN";
    public static final String APPROVE = "DEVIATION_APPROVE";
    public static final String CLOSE = "DEVIATION_CLOSE";
    private static final Set<String> TYPES = Set.of(SIGN, APPROVE, CLOSE);
    private static final Set<String> APPROVAL_NODES = Set.of("QA", "QUALITY_OWNER", "MANAGEMENT_REP");

    @Resource private MesProEdhrWorkTaskMapper taskMapper;
    @Resource private AdminUserApi adminUserApi;
    @Resource private PermissionApi permissionApi;
    @Resource private NotifyMessageSendApi notifyApi;
    @Resource private MesWorkTaskAuxiliaryAudit auxiliaryAudit;

    public void validateAssignee(String node, Long userId) {
        var user = userId == null ? null : adminUserApi.getUser(userId);
        if (user == null || !Objects.equals(user.getId(), userId)
                || !Objects.equals(user.getTenantId(), TenantContextHolder.getRequiredTenantId())
                || !CommonStatusEnum.isEnable(user.getStatus())
                || !permissionApi.hasAnyPermissions(userId, "mes:pro-edhr-deviation:query")
                || !permissionApi.hasAnyPermissions(userId, "mes:pro-edhr-work-task:query")
                || !permissionApi.hasAnyPermissions(userId, permission(node))) {
            throw exception(PRO_EDHR_WORK_TASK_ASSIGNEE_INVALID);
        }
    }

    public void assign(MesProEdhrDeviationDO deviation, MesProEdhrDeviationHandlingDO handling,
                       String node, Long userId) {
        validateAssignee(node, userId);
        List<MesProEdhrWorkTaskDO> existing = taskMapper.selectList(scope(deviation.getId())
                .eq(MesProEdhrWorkTaskDO::getSignatureCellKey, node));
        boolean transferred = false;
        LocalDateTime originalDeadline = null;
        for (var task : existing) {
            if (Objects.equals(task.getResponsibilitySourceVersion(), handling.getContentVersion().toString())
                    && Objects.equals(task.getResponsibilitySourceDigest(), handling.getContentHash())
                    && Objects.equals(task.getAssigneeUserId(), userId)
                    && Objects.equals(task.getReviewSourceId(), handling.getId())
                    && "EDHR_DEVIATION_HANDLING".equals(task.getReviewSourceType())
                    && !MesProEdhrWorkTaskStatus.CANCELED.equals(task.getStatus())) {
                return; // Exact replay, including a previously completed responsibility.
            }
            if (active(task)) {
                if (Objects.equals(task.getResponsibilitySourceVersion(), handling.getContentVersion().toString())
                        && Objects.equals(task.getResponsibilitySourceDigest(), handling.getContentHash())
                        && Objects.equals(task.getReviewSourceId(), handling.getId())) {
                    if (transferred) throw new IllegalStateException("同版本偏差节点存在重复生效责任");
                    transferred = true;
                    originalDeadline = task.getDueTime();
                }
                finish(task, MesProEdhrWorkTaskStatus.CANCELED, "偏差处理版本或接收人变更");
            }
        }
        String type = "CLOSE".equals(node) ? CLOSE : APPROVAL_NODES.contains(node) ? APPROVE : SIGN;
        String reason = "偏差" + deviation.getDeviationCode() + "：" + nodeLabel(node) + "（版本" + handling.getContentVersion() + "）";
        var task = new MesProEdhrWorkTaskDO().setTaskCode("EDHRD-" + UUID.randomUUID())
                .setTaskType(type).setBusinessScopeType("EDHR_DEVIATION").setBusinessScopeId(deviation.getId())
                .setBatchExecutionId(deviation.getBatchExecutionId()).setWorkOrderId(deviation.getWorkOrderId())
                .setWorkOrderCode(deviation.getWorkOrderCode()).setBatchCode(deviation.getBatchCode())
                .setAssigneeUserId(userId).setCandidateUserSnapshot(userId.toString())
                .setSignatureCellKey(node).setResponsibilitySourceType("DEVIATION_EXPLICIT_SELECTION")
                .setReviewSourceType("EDHR_DEVIATION_HANDLING").setReviewSourceId(handling.getId())
                .setResponsibilitySourceKey("DEVIATION|" + deviation.getId() + "|" + node)
                .setResponsibilitySourceVersion(handling.getContentVersion().toString())
                .setResponsibilitySourceDigest(handling.getContentHash())
                .setStatus(MesProEdhrWorkTaskStatus.TODO).setReason(reason).setRemark(reason)
                .setActionUrl("/mes/pro/feedback/edhr-deviation?deviationId=" + deviation.getId());
        if (transferred) task.setDueTime(originalDeadline);
        else taskDeadlineService.applyToWorkTask(task);
        if (taskMapper.insert(task) != 1 || task.getId() == null) {
            throw new IllegalStateException("偏差签署任务创建失败");
        }
        var notification = new NotifySendSingleToUserReqDTO();
        notification.setUserId(userId);
        notification.setTemplateCode("MES_EDHR_DEVIATION_REVIEW_ASSIGNED");
        notification.setTemplateParams(Map.of("reason", reason, "actionUrl", task.getActionUrl(),
                "workTaskId", task.getId()));
        auxiliaryAudit.notifications(task, () -> List.of(notifyApi.sendSingleMessageToAdmin(notification)));
    }

    public MesProEdhrWorkTaskDO require(Long deviationId, MesProEdhrDeviationHandlingDO handling,
                                         String node, Long actor) {
        var tasks = taskMapper.selectList(scope(deviationId)
                .eq(MesProEdhrWorkTaskDO::getTaskType, "CLOSE".equals(node) ? CLOSE : APPROVAL_NODES.contains(node) ? APPROVE : SIGN)
                .eq(MesProEdhrWorkTaskDO::getSignatureCellKey, node)
                .eq(MesProEdhrWorkTaskDO::getReviewSourceType, "EDHR_DEVIATION_HANDLING")
                .eq(MesProEdhrWorkTaskDO::getReviewSourceId, handling.getId())
                .eq(MesProEdhrWorkTaskDO::getAssigneeUserId, actor)
                .eq(MesProEdhrWorkTaskDO::getResponsibilitySourceVersion, handling.getContentVersion().toString())
                .eq(MesProEdhrWorkTaskDO::getResponsibilitySourceDigest, handling.getContentHash())
                .in(MesProEdhrWorkTaskDO::getStatus, MesProEdhrWorkTaskStatus.TODO, MesProEdhrWorkTaskStatus.OVERDUE));
        if (tasks.size() != 1) throw exception(PRO_EDHR_DEVIATION_HANDLING_PERMISSION_DENIED, node);
        validateAssignee(node, actor);
        return tasks.get(0);
    }

    public void complete(MesProEdhrWorkTaskDO task) {
        finish(task, MesProEdhrWorkTaskStatus.DONE, "当前版本偏差责任已完成");
    }

    public void completeSigned(MesProEdhrWorkTaskDO task, Long signatureId, String comment) {
        if (signatureId == null || signatureId <= 0) throw new IllegalArgumentException("签名证据编号必填");
        var receipt = Map.of("signatureId", signatureId, "actorUserId", task.getAssigneeUserId(),
                "handlingId", task.getReviewSourceId(), "node", task.getSignatureCellKey(),
                "contentVersion", task.getResponsibilitySourceVersion(),
                "contentHash", task.getResponsibilitySourceDigest(),
                "userOpinion", comment == null ? "" : comment.trim());
        finish(task, MesProEdhrWorkTaskStatus.DONE, "当前版本偏差责任已签署", JSON.toJSONString(receipt));
    }

    public void cancelAll(Long deviationId, String reason) {
        for (var task : taskMapper.selectList(scope(deviationId))) {
            if (active(task)) finish(task, MesProEdhrWorkTaskStatus.CANCELED, reason);
        }
    }

    private boolean active(MesProEdhrWorkTaskDO task) {
        return Set.of(MesProEdhrWorkTaskStatus.TODO, MesProEdhrWorkTaskStatus.DOING,
                MesProEdhrWorkTaskStatus.OVERDUE).contains(task.getStatus());
    }

    private void finish(MesProEdhrWorkTaskDO task, String status, String reason) {
        finish(task, status, reason, null);
    }

    private void finish(MesProEdhrWorkTaskDO task, String status, String reason, String signatureReceipt) {
        var update = new MesProEdhrWorkTaskDO().setStatus(status).setReason(reason)
                .setRemark(reason).setCompletedAt(LocalDateTime.now(MesProEdhrDeviationNumberGenerator.BUSINESS_ZONE));
        if (signatureReceipt != null) update.setResponsibilityScopeJson(signatureReceipt);
        int updated = taskMapper.update(update,
                new LambdaUpdateWrapper<MesProEdhrWorkTaskDO>().eq(MesProEdhrWorkTaskDO::getId, task.getId())
                        .eq(MesProEdhrWorkTaskDO::getStatus, task.getStatus()));
        if (updated != 1) throw new IllegalStateException("偏差签署任务状态已变更，请刷新");
    }

    private LambdaQueryWrapper<MesProEdhrWorkTaskDO> scope(Long deviationId) {
        return new LambdaQueryWrapper<MesProEdhrWorkTaskDO>()
                .eq(MesProEdhrWorkTaskDO::getBusinessScopeType, "EDHR_DEVIATION")
                .eq(MesProEdhrWorkTaskDO::getBusinessScopeId, deviationId)
                .eq(MesProEdhrWorkTaskDO::getDeleted, false)
                .in(MesProEdhrWorkTaskDO::getTaskType, TYPES)
                .apply("tenant_id = {0}", TenantContextHolder.getRequiredTenantId());
    }

    static String permission(String node) {
        return switch (node) {
            case "PREPARER" -> "mes:pro-edhr-deviation:handle";
            case "VERIFIER" -> "mes:pro-edhr-deviation:verify";
            case "DEPARTMENT_OWNER" -> "mes:pro-edhr-deviation:department-confirm";
            case "QA", "CLOSE" -> "mes:pro-edhr-deviation:qa-close";
            case "QUALITY_OWNER" -> "mes:pro-edhr-deviation:quality-approve";
            case "MANAGEMENT_REP" -> "mes:pro-edhr-deviation:critical-management-approve";
            default -> throw new IllegalArgumentException("未知偏差签署节点：" + node);
        };
    }

    private String nodeLabel(String node) {
        return switch (node) {
            case "PREPARER" -> "处理记录编制";
            case "VERIFIER" -> "纠正措施核验";
            case "DEPARTMENT_OWNER" -> "部门负责人确认";
            case "QA" -> "QA关闭确认";
            case "QUALITY_OWNER" -> "质量负责人批准";
            case "MANAGEMENT_REP" -> "管理者代表批准";
            case "CLOSE" -> "偏差正式关闭";
            default -> throw new IllegalArgumentException("未知偏差签署节点：" + node);
        };
    }
}
