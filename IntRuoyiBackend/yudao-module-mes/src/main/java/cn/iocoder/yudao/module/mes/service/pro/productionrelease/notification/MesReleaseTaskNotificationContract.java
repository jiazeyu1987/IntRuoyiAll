package cn.iocoder.yudao.module.mes.service.pro.productionrelease.notification;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

final class MesReleaseTaskNotificationContract {
    static final String QUERY_PERMISSION = "mes:pro-edhr-work-task:query";
    static final String PQC_TEMPLATE = "MES_EDHR_PQC_PRODUCTION_RELEASE_TASK_ASSIGNED";
    static final String MANAGER_TEMPLATE = "MES_EDHR_RELEASE_APPROVE_TASK_ASSIGNED";

    private MesReleaseTaskNotificationContract() { }

    static void tenant(Long tenantId) {
        if (tenantId == null || tenantId <= 0 || !Objects.equals(tenantId, TenantContextHolder.getRequiredTenantId())) {
            throw new IllegalArgumentException("当前租户与通知投递身份不一致");
        }
    }

    static List<Long> candidates(String snapshot) {
        if (snapshot == null || snapshot.isBlank()) { throw new IllegalArgumentException("放行任务冻结候选快照缺失"); }
        TreeSet<Long> ids = new TreeSet<>();
        for (String token : snapshot.split(",", -1)) {
            if (!token.matches("[1-9][0-9]*")) { throw new IllegalArgumentException("放行任务候选快照含无效身份"); }
            try { ids.add(Long.parseLong(token)); }
            catch (NumberFormatException invalid) { throw new IllegalArgumentException("放行任务候选身份超出范围", invalid); }
        }
        return List.copyOf(ids);
    }

    static String taskTemplate(MesProEdhrWorkTaskDO task) {
        if (task == null || task.getId() == null || task.getId() <= 0
                || task.getBusinessScopeId() == null || task.getBusinessScopeId() <= 0
                || task.getWorkOrderId() == null || task.getWorkOrderId() <= 0
                || blank(task.getWorkOrderCode()) || blank(task.getBatchCode())
                || blank(task.getProcessName()) || !Boolean.TRUE.equals(task.getOwnershipLocked())) {
            throw new IllegalArgumentException("正式放行任务身份不完整");
        }
        String path;
        String template;
        if ("PQC_PRODUCTION_RELEASE".equals(task.getTaskType())
                && "RELEASE_APPLICATION".equals(task.getBusinessScopeType())) {
            template = PQC_TEMPLATE;
            path = "/mes/production-release/pqc";
        } else if ("RELEASE_APPROVE".equals(task.getTaskType())
                && "RELEASE_TRANSACTION".equals(task.getBusinessScopeType())
                && task.getBatchExecutionId() != null && task.getBatchExecutionId() > 0) {
            template = MANAGER_TEMPLATE;
            path = "/mes/pro/feedback/edhr-batch-execution";
        } else { throw new IllegalArgumentException("任务不是当前活跃订单正式放行交接"); }
        URI uri;
        try { uri = URI.create(Objects.requireNonNull(task.getActionUrl(), "放行任务入口缺失")); }
        catch (IllegalArgumentException invalid) { throw new IllegalArgumentException("放行任务入口无效", invalid); }
        if (uri.isAbsolute() || uri.getRawAuthority() != null || !path.equals(uri.getPath()) || uri.getFragment() != null) {
            throw new IllegalArgumentException("放行任务入口必须为规定的同源页面");
        }
        var query = new java.util.LinkedHashMap<String, String>();
        for (String part : Objects.requireNonNull(uri.getRawQuery(), "放行任务入口身份缺失").split("&", -1)) {
            String[] pair = part.split("=", -1);
            if (pair.length != 2 || query.putIfAbsent(pair[0], pair[1]) != null) {
                throw new IllegalArgumentException("放行任务入口参数重复或无效");
            }
        }
        if (!Objects.equals(query.get("workTaskId"), task.getId().toString())) {
            throw new IllegalArgumentException("放行任务入口与工作任务身份不一致");
        }
        if (PQC_TEMPLATE.equals(template)) {
            if (query.size() != 2 || !Objects.equals(query.get("applicationId"), task.getBusinessScopeId().toString())) {
                throw new IllegalArgumentException("PQC通知入口与正式申请不一致");
            }
        } else if (query.size() != 4 || !Objects.equals(query.get("batchExecutionId"), task.getBatchExecutionId().toString())
                || !Objects.equals(query.get("releaseTransactionId"), task.getBusinessScopeId().toString())
                || !"marketRelease".equals(query.get("action"))) {
            throw new IllegalArgumentException("上市放行通知入口与正式批次和事务不一致");
        }
        return template;
    }

    static void access(PermissionApi permissions, Long tenantId, Long actorId, MesProEdhrWorkTaskDO task) {
        tenant(tenantId);
        if (actorId == null || actorId <= 0 || !permissions.hasAnyPermissions(actorId, QUERY_PERMISSION)
                || task == null || !candidates(task.getCandidateUserSnapshot()).contains(actorId)) {
            throw new ServiceException(1_076_040_001, "无权查询或重试此正式放行任务的通知");
        }
    }

    static void reason(Long actorId, String reason) {
        if (actorId == null || actorId <= 0 || blank(reason) || reason.length() > 1000) {
            throw new IllegalArgumentException("通知重试必须提供真实操作者和有效原因");
        }
    }

    static String safeError(Throwable error) {
        // Do not persist exception messages: they may contain SQL values or credentials.
        String code = error instanceof ServiceException service ? ":code=" + service.getCode() : "";
        return error.getClass().getSimpleName() + code;
    }

    static boolean blank(String value) { return value == null || value.isBlank(); }
}
