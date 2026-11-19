package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesEdhrTaskDeadlineService;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.MesProductionReleaseRoleCandidates;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/** Four independent upload duties, shared by the frozen members of each upload role. */
@Service
@RequiredArgsConstructor
public class MesReleaseAttachmentUploadService {
    public static final String SOURCE = "RELEASE_ATTACHMENT_ROLE";
    public static final List<Definition> DEFINITIONS = List.of(
            new Definition("INCOMING_INSPECTION_REPORT", "来料检报告", "INCOMING_INSPECTION_FILE"),
            new Definition("STERILIZATION_REPORT", "灭菌报告", "STERILIZATION_FILE"),
            new Definition("FINISHED_PRODUCT_INSPECTION_REPORT", "成品检报告", "FINISHED_PRODUCT_FILE"),
            new Definition("FINISHED_PRODUCT_INSPECTION_RECORD", "成品检记录", "FINISHED_PRODUCT_FILE"));
    private final MesProEdhrWorkTaskMapper tasks;
    private final MesProcessPoolActiveOrderReleaseApplicationMapper applications;
    private final MesProWorkOrderMapper workOrders;
    private final RoleApi roles;
    private final PermissionApi permissions;
    private final AdminUserApi users;
    private final MesEdhrTaskDeadlineService deadlines;

    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public void initialize(MesProcessPoolActiveOrderReleaseApplicationDO application, Long actor) {
        if (application == null || application.getId() == null || application.getActiveOrderId() == null
                || !Objects.equals(application.getTenantId(), TenantContextHolder.getRequiredTenantId()))
            throw new IllegalStateException("放行资料待办缺少当前租户的正式放行申请");
        if (!"APPROVE".equals(application.getPqcDecision())
                || !List.of("REPORT_UPLOAD_PENDING", "MANAGER_RELEASE_PENDING").contains(application.getApplicationStatus()))
            throw new IllegalStateException("仅PQC生产放行通过后的当前放行申请可以生成上传任务");
        var order = workOrders.selectById(application.getWorkOrderId());
        if (order == null || order.getCode() == null || order.getCode().isBlank()
                || !Objects.equals(order.getTenantId(), TenantContextHolder.getRequiredTenantId())
                || application.getBatchExecutionId() == null)
            throw new IllegalStateException("放行资料待办缺少同租户正式工单或批次");
        for (var definition : DEFINITIONS) {
            var resolved = resolveCandidates(definition.roleCode());
            var task = new MesProEdhrWorkTaskDO().setTaskCode("EDHRU-" + UUID.randomUUID())
                    .setTaskType("DOSSIER_COMPLETE").setBusinessScopeType("ACTIVE_ORDER_DOSSIER")
                    .setBusinessScopeId(application.getActiveOrderId()).setReviewSourceId(application.getId())
                    .setReviewSourceType(definition.category()).setReviewSourceName(definition.label())
                    .setBatchExecutionId(application.getBatchExecutionId()).setWorkOrderId(order.getId())
                    .setWorkOrderCode(order.getCode()).setBatchCode(order.getBatchCode()).setRouteId(application.getRouteId())
                    .setProcessName(definition.label()).setCandidateSourceType("ROLE").setCandidateSourceId(resolved.roleId())
                    .setCandidateUserSnapshot(resolved.candidateUserIds().stream().map(String::valueOf).collect(Collectors.joining(",")))
                    .setResponsibilitySourceType(SOURCE).setResponsibilitySourceKey(definition.roleCode())
                    .setResponsibilitySourceDigest(resolved.candidateSnapshotHash()).setOwnershipLocked(true)
                    .setSourceUserId(actor).setStatus("TODO").setReason("PQC生产放行已通过，请上传" + definition.label())
                    .setActionUrl("");
            deadlines.applyToWorkTask(task);
            if (tasks.insert(task) != 1 || task.getId() == null) throw new IllegalStateException("放行资料上传待办创建失败");
            task.setActionUrl("/mes/pro/feedback/edhr-batch-execution/active-order-detail?batchExecutionId="
                    + application.getBatchExecutionId() + "&from=execution&tab=" + definition.category() + "&workTaskId=" + task.getId());
            if (tasks.updateById(new MesProEdhrWorkTaskDO().setId(task.getId()).setActionUrl(task.getActionUrl())) != 1)
                throw new IllegalStateException("放行资料上传待办入口保存失败");
        }
    }

    private MesProductionReleaseRoleCandidates resolveCandidates(String roleCode) {
        var role = roles.getRoleByCode(roleCode);
        if (role == null || role.getId() == null || !Integer.valueOf(0).equals(role.getStatus()))
            throw new IllegalStateException("缺少启用的放行资料上传角色：" + roleCode);
        var ids = Objects.requireNonNull(permissions.getUserRoleIdListByRoleIds(List.of(role.getId())), "上传角色成员查询失败");
        var rows = ids.isEmpty() ? List.<cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO>of()
                : Objects.requireNonNull(users.getUserList(ids), "上传角色用户查询失败");
        if (rows.size() != ids.size() || !rows.stream().map(row -> row.getId()).collect(Collectors.toSet()).equals(ids))
            throw new IllegalStateException("上传角色成员与本租户用户不一致：" + roleCode);
        var enabled = rows.stream().filter(row -> Integer.valueOf(0).equals(row.getStatus()))
                .map(row -> row.getId()).sorted().toList();
        String canonical = TenantContextHolder.getRequiredTenantId() + "|" + role.getId() + "|" + roleCode + "|" + enabled;
        return new MesProductionReleaseRoleCandidates(role.getId(), roleCode, enabled,
                cn.hutool.crypto.digest.DigestUtil.sha256Hex(canonical));
    }

    public boolean canRead(Long actor, Long batchId) {
        if (actor == null || batchId == null) return false;
        return duties(actor, null, batchId, false).size() > 0;
    }

    public Set<Long> readableBatchIds(Long actor, Collection<Long> batchIds) {
        if (actor == null || batchIds.isEmpty()) return Set.of();
        return dutiesInBatches(actor, null, batchIds, false).stream()
                .map(MesProEdhrWorkTaskDO::getBatchExecutionId).collect(Collectors.toSet());
    }

    public List<Duty> pending(Long actor, Long activeOrderId) {
        return duties(actor, activeOrderId, null, true).stream()
                .map(task -> new Duty(String.valueOf(task.getId()), task.getReviewSourceType(), task.getReviewSourceName())).toList();
    }

    public boolean hasDuty(Long actor, Long activeOrderId) {
        return !duties(actor, activeOrderId, null, false).isEmpty();
    }

    public boolean isAssigned(Long actor, Long activeOrderId, String category) {
        return pending(actor, activeOrderId).stream().anyMatch(duty -> duty.categoryKey().equals(category));
    }

    public boolean validateUpload(Long actor, Long taskId, Long activeOrderId, String category) {
        var task = tasks.selectById(taskId);
        if (task == null || !SOURCE.equals(task.getResponsibilitySourceType())) return false;
        if (pending(actor, activeOrderId).stream().noneMatch(duty -> duty.workTaskId().equals(String.valueOf(taskId))
                && duty.categoryKey().equals(category))) throw new IllegalStateException("放行资料上传任务已完成、轮次已过期或不属于当前用户");
        return true;
    }

    public boolean complete(Long actor, Long taskId, Long activeOrderId, String category) {
        if (taskId == null || !validateUpload(actor, taskId, activeOrderId, category)) return false;
        if (tasks.update(new MesProEdhrWorkTaskDO().setStatus("DONE").setAssigneeUserId(actor).setCompletedAt(LocalDateTime.now()),
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<MesProEdhrWorkTaskDO>()
                        .eq(MesProEdhrWorkTaskDO::getId, taskId).in(MesProEdhrWorkTaskDO::getStatus, "TODO", "DOING", "OVERDUE")) != 1)
            throw new IllegalStateException("放行资料上传任务状态已改变");
        return true;
    }

    private List<MesProEdhrWorkTaskDO> duties(Long actor, Long activeOrderId, Long batchId, boolean open) {
        return dutiesInBatches(actor, activeOrderId, batchId == null ? List.of() : List.of(batchId), open);
    }

    private List<MesProEdhrWorkTaskDO> dutiesInBatches(Long actor, Long activeOrderId, Collection<Long> batchIds, boolean open) {
        var login = SecurityFrameworkUtils.getLoginUser();
        if (login == null || !Objects.equals(actor, login.getId())
                || !Objects.equals(login.getTenantId(), TenantContextHolder.getRequiredTenantId()))
            throw new IllegalStateException("放行资料上传身份与当前登录租户不一致");
        var query = new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                .eq(MesProEdhrWorkTaskDO::getResponsibilitySourceType, SOURCE)
                .eq(MesProEdhrWorkTaskDO::getTaskType, "DOSSIER_COMPLETE")
                .eq(MesProEdhrWorkTaskDO::getBusinessScopeType, "ACTIVE_ORDER_DOSSIER")
                .eqIfPresent(MesProEdhrWorkTaskDO::getBusinessScopeId, activeOrderId)
                .in(MesProEdhrWorkTaskDO::getStatus, open ? List.of("TODO", "DOING", "OVERDUE") : List.of("TODO", "DOING", "OVERDUE", "DONE"))
                .apply("FIND_IN_SET({0}, candidate_user_snapshot) > 0", actor);
        if (!batchIds.isEmpty()) query.in(MesProEdhrWorkTaskDO::getBatchExecutionId, batchIds);
        var rows = tasks.selectList(query);
        if (rows.isEmpty()) return List.of();
        var latest = applications.selectList(new LambdaQueryWrapperX<MesProcessPoolActiveOrderReleaseApplicationDO>()
                        .in(MesProcessPoolActiveOrderReleaseApplicationDO::getActiveOrderId,
                                rows.stream().map(MesProEdhrWorkTaskDO::getBusinessScopeId).distinct().toList())).stream()
                .collect(Collectors.toMap(MesProcessPoolActiveOrderReleaseApplicationDO::getActiveOrderId, app -> app,
                        (left, right) -> left.getId() > right.getId() ? left : right));
        return rows.stream().filter(task -> {
            var application = latest.get(task.getBusinessScopeId());
            return application != null && Objects.equals(application.getId(), task.getReviewSourceId())
                    && Objects.equals(application.getTenantId(), TenantContextHolder.getRequiredTenantId())
                    && Objects.equals(application.getWorkOrderId(), task.getWorkOrderId())
                    && Objects.equals(application.getBatchExecutionId(), task.getBatchExecutionId())
                    && "APPROVE".equals(application.getPqcDecision())
                    && (open ? List.of("REPORT_UPLOAD_PENDING", "MANAGER_RELEASE_PENDING")
                        : List.of("REPORT_UPLOAD_PENDING", "MANAGER_RELEASE_PENDING", "RELEASED")).contains(application.getApplicationStatus())
                    && task.getCandidateUserSnapshot() != null && java.util.Arrays.stream(task.getCandidateUserSnapshot().split(","))
                        .map(String::trim).anyMatch(String.valueOf(actor)::equals);
        }).toList();
    }

    public record Definition(String node, String label, String category) {
        public String roleCode() { return "BATCH_ATTACHMENT_" + node + "_UPLOAD_1"; }
    }
    public record Duty(String workTaskId, String categoryKey, String label) { }
}
