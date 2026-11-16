package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordChangeApproverRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrRecordChangeEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserIdempotentReqDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Assignment is the explicitly selected, authorized reviewer; never inferred from a role. */
@Service
public class MesEdhrRecordChangeTaskService {
    public static final String TYPE = "RECORD_CHANGE_APPROVE";
    public static final String SCOPE = "RECORD_CHANGE";
    public static final String APPROVE_PERMISSION = "mes:pro-edhr-change:approve";
    @Resource private MesProEdhrWorkTaskMapper taskMapper;
    @Resource private AdminUserApi userApi;
    @Resource private PermissionApi permissionApi;
    @Resource private NotifyMessageSendApi notifyApi;
    @Resource private MesEdhrTaskDeadlineService deadlineService;

    public List<EdhrRecordChangeApproverRespVO> approvers(String nickname) {
        return userApi.getUserListByNickname(nickname).stream()
                .filter(user -> Integer.valueOf(0).equals(user.getStatus()))
                .filter(user -> Objects.equals(user.getTenantId(), TenantContextHolder.getRequiredTenantId()))
                .filter(user -> !Objects.equals(user.getId(), SecurityFrameworkUtils.getLoginUserId()))
                .filter(user -> eligible(user.getId()))
                .map(user -> new EdhrRecordChangeApproverRespVO(user.getId(), user.getUsername(), user.getNickname()))
                .toList();
    }

    public void validateApprover(Long userId, Long requester) {
        if (userId == null || userId <= 0 || Objects.equals(userId, requester)) {
            throw new IllegalArgumentException("请选择申请人以外的变更审批人");
        }
        userApi.validateUser(userId);
        AdminUserRespDTO user = userApi.getUser(userId);
        if (user == null || !Objects.equals(user.getTenantId(), TenantContextHolder.getRequiredTenantId())) {
            throw new IllegalArgumentException("审批人必须属于当前租户");
        }
        if (!eligible(userId)) throw new IllegalArgumentException("所选审批人缺少记录变更审批、查询或工作待办权限");
    }

    private boolean eligible(Long userId) {
        return permissionApi.hasAnyPermissions(userId, APPROVE_PERMISSION)
                && permissionApi.hasAnyPermissions(userId, "mes:pro-edhr-change:query")
                && permissionApi.hasAnyPermissions(userId, "mes:pro-edhr-work-task:query");
    }

    public MesProEdhrWorkTaskDO find(Long eventId) {
        return taskMapper.selectOne(new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                .eq(MesProEdhrWorkTaskDO::getBusinessScopeType, SCOPE)
                .eq(MesProEdhrWorkTaskDO::getBusinessScopeId, eventId)
                .eq(MesProEdhrWorkTaskDO::getTaskType, TYPE));
    }

    public MesProEdhrWorkTaskDO findCorrection(Long eventId) {
        return taskMapper.selectActiveByBusinessScopeAndType("RECORD_CHANGE_CORRECTION", eventId, "RECORD_CHANGE_CORRECTION");
    }

    public void requireRequester(MesProEdhrRecordChangeEventDO event) {
        Long actor = SecurityFrameworkUtils.getLoginUserId();
        var task = findCorrection(event.getId());
        String permission = switch (event.getChangeType()) {
            case "SUPPLEMENT" -> "mes:pro-edhr-change:supplement";
            case "VOID" -> "mes:pro-edhr-change:void";
            case "REOPEN" -> "mes:pro-edhr-change:reopen";
            default -> throw new IllegalArgumentException("不支持的记录变更类型");
        };
        if (!Objects.equals(event.getRequestedBy(), actor) || task == null
                || !Objects.equals(task.getAssigneeUserId(), actor)
                || !permissionApi.hasAnyPermissions(actor, permission)) {
            throw new IllegalArgumentException("只有具备对应申请权限的原申请人可以办理补正重提");
        }
    }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void completeCorrection(MesProEdhrRecordChangeEventDO event) {
        requireRequester(event);
        var task = findCorrection(event.getId());
        if (taskMapper.updateById(new MesProEdhrWorkTaskDO().setId(task.getId()).setStatus("DONE")
                .setCompletedAt(LocalDateTime.now()).setRemark("补正已签名重提，新审批轮次已派发")) != 1) {
            throw new IllegalStateException("补正待办完成失败");
        }
    }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void completeNegativeDecision(MesProEdhrRecordChangeEventDO event) {
        if (!List.of("REJECTED", "RETURNED").contains(event.getChangeStatus())
                || event.getApprovalSignatureId() == null || event.getApprovedAt() == null
                || !Objects.equals(event.getApprovedBy(), SecurityFrameworkUtils.getLoginUserId())) {
            throw new IllegalStateException("正式否决决定与签名未保存");
        }
        var approval = requireReviewer(event);
        if (taskMapper.updateById(new MesProEdhrWorkTaskDO().setId(approval.getId()).setStatus("DONE")
                .setCompletedAt(event.getApprovedAt()).setRemark(event.getDecisionReason())) != 1) {
            throw new IllegalStateException("记录变更审批待办完成失败");
        }
        if ("RETURNED".equals(event.getChangeStatus())) {
            var correction = new MesProEdhrWorkTaskDO().setTaskCode("EDHR-CHANGE-CORRECTION-" + event.getId())
                    .setTaskType("RECORD_CHANGE_CORRECTION").setBusinessScopeType("RECORD_CHANGE_CORRECTION")
                    .setBusinessScopeId(event.getId()).setBatchExecutionId(event.getBatchExecutionId())
                    .setExecutionId(event.getExecutionId()).setAssigneeUserId(event.getRequestedBy())
                    .setCandidateUserSnapshot(event.getRequestedBy().toString()).setCandidateSourceType("USER")
                    .setCandidateSourceId(event.getRequestedBy()).setSourceUserId(event.getApprovedBy())
                    .setStatus("TODO").setOwnershipLocked(true).setSignatureCellKey("")
                    .setActionUrl(approval.getActionUrl()).setReason(event.getDecisionReason()).setRemark("补正变更申请：" + event.getChangeCode());
            deadlineService.applyToWorkTask(correction);
            if (taskMapper.insert(correction) != 1 || correction.getId() == null) {
                throw new IllegalStateException("记录变更补正待办保存失败");
            }
        }
        notify(event.getRequestedBy(), "MES_EDHR_RECORD_CHANGE_" + event.getChangeStatus(), event, approval.getActionUrl());
    }

    public MesProEdhrWorkTaskDO findOwnRevision(Long eventId) {
        var task = taskMapper.selectActiveByBusinessScopeAndType(SCOPE, eventId, "REWORK");
        return task != null && MesProEdhrWorkTaskAuthorization.isAssignedOrCandidate(task,
                SecurityFrameworkUtils.getLoginUserId()) ? task : null;
    }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public MesProEdhrWorkTaskDO assign(MesProEdhrRecordChangeEventDO event, Long approver) {
        if (event == null || event.getId() == null || event.getRequestedBy() == null
                || !"SUBMITTED".equals(event.getChangeStatus())) {
            throw new IllegalArgumentException("只有已正式提交且有原申请人的记录变更可以派审");
        }
        validateApprover(approver, event.getRequestedBy());
        MesProEdhrWorkTaskDO existing = find(event.getId());
        if (existing != null) {
            if (!Objects.equals(existing.getAssigneeUserId(), approver)) {
                throw new IllegalStateException("该变更已有正式审批责任人");
            }
            return existing;
        }
        MesProEdhrWorkTaskDO task = new MesProEdhrWorkTaskDO()
                .setTaskCode("EDHR-CHANGE-" + event.getId()).setTaskType(TYPE)
                .setBusinessScopeType(SCOPE).setBusinessScopeId(event.getId())
                .setBatchExecutionId(event.getBatchExecutionId()).setExecutionId(event.getExecutionId())
                .setAssigneeUserId(approver).setCandidateUserSnapshot(approver.toString())
                .setCandidateSourceType("USER").setCandidateSourceId(approver)
                .setSourceUserId(event.getRequestedBy()).setStatus("TODO")
                .setSignatureCellKey("").setOwnershipLocked(true)
                .setActionUrl("/mes/pro/feedback/edhr-change?changeEventId=" + event.getId())
                .setReason(event.getReasonText()).setRemark(event.getChangeCode());
        deadlineService.applyToWorkTask(task);
        if (taskMapper.insert(task) != 1 || task.getId() == null) {
            throw new IllegalStateException("记录变更审批待办保存失败");
        }
        notify(approver, "MES_EDHR_RECORD_CHANGE_ASSIGNED", event, task.getActionUrl());
        return task;
    }

    public MesProEdhrWorkTaskDO requireReviewer(MesProEdhrRecordChangeEventDO event) {
        MesProEdhrWorkTaskDO task = find(event.getId());
        Long actor = SecurityFrameworkUtils.getLoginUserId();
        if (task == null || !Objects.equals(task.getAssigneeUserId(), actor)
                || Objects.equals(event.getRequestedBy(), actor)
                || !List.of("TODO", "OVERDUE").contains(task.getStatus())) {
            throw new IllegalArgumentException("只有本次变更的指定审批人可以审批，申请人不得自审");
        }
        validateApprover(actor, event.getRequestedBy());
        return task;
    }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void complete(MesProEdhrRecordChangeEventDO event) {
        if (!"EFFECTIVE".equals(event.getChangeStatus()) || event.getApprovalSignatureId() == null
                || event.getApprovedAt() == null || event.getEffectiveAt() == null
                || !Objects.equals(event.getApprovedBy(), SecurityFrameworkUtils.getLoginUserId())) {
            throw new IllegalStateException("正式批准事件与签名未保存，不得完成审批待办");
        }
        MesProEdhrWorkTaskDO task = requireReviewer(event);
        if (taskMapper.updateById(new MesProEdhrWorkTaskDO().setId(task.getId())
                .setStatus("DONE").setCompletedAt(event.getApprovedAt()).setRemark("变更已批准")) != 1) {
            throw new IllegalStateException("记录变更审批待办完成失败");
        }
        notify(event.getRequestedBy(), "MES_EDHR_RECORD_CHANGE_APPROVED", event, task.getActionUrl());
    }

    private void notify(Long userId, String template, MesProEdhrRecordChangeEventDO event, String url) {
        NotifySendSingleToUserIdempotentReqDTO request = new NotifySendSingleToUserIdempotentReqDTO();
        request.setUserId(userId);
        request.setTemplateCode(template);
        request.setBusinessKey(template + ":EVENT:" + event.getId() + ":USER:" + userId);
        request.setTemplateParams(Map.of("changeCode", event.getChangeCode(), "reason", event.getDecisionReason() == null ? event.getReasonText() : event.getDecisionReason(),
                "actionUrl", url));
        Long messageId = notifyApi.sendSingleMessageIdempotentlyToAdmin(request);
        if (messageId == null || messageId <= 0) {
            throw new IllegalStateException("记录变更站内通知未生成，请检查通知模板配置");
        }
    }
}
