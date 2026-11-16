package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/** Explicit route deadlines. Absence means no agreed deadline, never a default SLA. */
@Service
public class MesEdhrTaskDeadlineService {
    public static final String SCOPE = "ROUTE_DEADLINE";
    public static final Set<String> TYPES = Set.of("DEVIATION_RECEIVE", "DEVIATION_CORRECT",
            "DEVIATION_SIGN", "DEVIATION_APPROVE", "DEVIATION_CLOSE", "RECORD_CHANGE_APPROVE",
            "RECORD_CHANGE_REWORK", "RECORD_CHANGE_CORRECTION", "DOSSIER_COMPLETE", "PQC_RELEASE_CORRECTION", "RELEASE_APPROVE",
            "QA_REVIEW", "QA_DECISION_HANDOFF");
    @Resource private MesProEdhrWorkTaskAssignmentRuleMapper rules;
    @Resource private MesProEdhrBatchExecutionMapper batches;
    @Resource private MesProRouteMapper routes;

    public List<MesProEdhrWorkTaskAssignmentRuleDO> list(Long routeId) {
        requireRoute(routeId);
        return rules.selectList(new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<MesProEdhrWorkTaskAssignmentRuleDO>()
                .eq(MesProEdhrWorkTaskAssignmentRuleDO::getScopeType, SCOPE)
                .eq(MesProEdhrWorkTaskAssignmentRuleDO::getScopeId, routeId)
                .orderByAsc(MesProEdhrWorkTaskAssignmentRuleDO::getTaskType));
    }

    @Transactional(rollbackFor = Exception.class)
    public void save(Long routeId, String taskType, Integer dueMinutes, Boolean enabled, String reason) {
        requireRoute(routeId);
        requireType(taskType);
        if (enabled == null || dueMinutes == null || dueMinutes <= 0 || reason == null || reason.isBlank())
            throw new IllegalArgumentException("须明确正整数时限、启用状态及业务时限依据");
        // Serialize configuration changes using the formal route row; never create duplicate rules.
        routes.selectOne(new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO>()
                .eq(cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO::getId, routeId).last("FOR UPDATE"));
        var existing = rules.selectByScopeAndType(SCOPE, routeId, taskType);
        var rule = new MesProEdhrWorkTaskAssignmentRuleDO().setScopeType(SCOPE).setScopeId(routeId)
                .setTaskType(taskType).setDueMinutes(dueMinutes).setEnabled(enabled).setRemark(reason.trim());
        int changed = existing == null ? rules.insert(rule) : rules.updateById(rule.setId(existing.getId()));
        if (changed != 1) throw new IllegalStateException("任务时限配置保存失败");
    }

    public LocalDateTime calculateDueTime(Long routeId, String taskType, LocalDateTime assignedAt) {
        TenantContextHolder.getRequiredTenantId();
        requireType(taskType);
        if (routeId == null || assignedAt == null) throw new IllegalArgumentException("任务时限缺少正式路线或派发时间");
        var rule = rules.selectEnabledByScopeAndType(SCOPE, routeId, taskType);
        if (rule == null) return null;
        if (rule.getDueMinutes() == null || rule.getDueMinutes() <= 0)
            throw new IllegalStateException("已启用的任务时限配置无有效dueMinutes：" + taskType);
        return assignedAt.plusMinutes(rule.getDueMinutes());
    }

    public void applyToWorkTask(MesProEdhrWorkTaskDO task) {
        if (task == null) throw new IllegalArgumentException("任务不能为空");
        if (task.getDueTime() != null) return; // A replay/transfer cannot extend an existing promise.
        Long routeId = task.getRouteId();
        if (routeId == null && task.getBatchExecutionId() != null) {
            var batch = batches.selectById(task.getBatchExecutionId());
            if (batch == null) throw new IllegalStateException("任务时限缺少正式批次");
            routeId = batch.getRouteId();
        }
        String type = "RECORD_CHANGE".equals(task.getBusinessScopeType()) && "REWORK".equals(task.getTaskType())
                ? "RECORD_CHANGE_REWORK" : task.getTaskType();
        task.setDueTime(calculateDueTime(routeId, type, LocalDateTime.now()));
    }

    private void requireRoute(Long routeId) {
        TenantContextHolder.getRequiredTenantId();
        if (routeId == null || routes.selectById(routeId) == null) throw new IllegalArgumentException("工艺路线不存在");
    }
    private void requireType(String type) {
        if (!TYPES.contains(type)) throw new IllegalArgumentException("不支持的任务时限节点：" + type);
    }
}
