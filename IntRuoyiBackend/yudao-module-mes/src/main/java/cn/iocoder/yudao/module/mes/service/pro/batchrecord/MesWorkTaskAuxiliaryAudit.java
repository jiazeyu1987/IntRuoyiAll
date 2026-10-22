package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.util.*;
import java.util.function.Supplier;

/** Records only the actual auxiliary writes made by the current MES task call. */
@Component
public class MesWorkTaskAuxiliaryAudit {
    private final JdbcTemplate jdbc;
    private final GxpAuditService audit;

    public MesWorkTaskAuxiliaryAudit(DataSource dataSource, GxpAuditService audit) {
        this.jdbc = new JdbcTemplate(dataSource);
        this.audit = audit;
    }

    public void entitlements(MesProEdhrWorkTaskDO task, String policyCode, Set<Long> candidates, Runnable change) {
        Long tenant = lockTask(task);
        String sourceKey = "WORK_TASK|" + task.getId();
        var claims = sourceRows("system_entitlement_claim", tenant, sourceKey, policyCode);
        Set<Long> affected = new TreeSet<>(candidates);
        for (var claim : claims) affected.add(((Number) claim.get("resolved_user_id")).longValue());
        String before = entitlementState(tenant, sourceKey, policyCode, affected);
        change.run();
        String after = entitlementState(tenant, sourceKey, policyCode, affected);
        // Existing revoke/no-write replay does not create a second fact.
        if (!before.equals(after)) append("entitlement", "entitlements", task, before, after);
    }

    public void notifications(MesProEdhrWorkTaskDO task, Supplier<List<Long>> change) {
        Long tenant = lockTask(task);
        List<Long> ids = Objects.requireNonNull(change.get(), "Notification result IDs are required");
        if (ids.isEmpty()) return; // Preserve the existing no-recipient/no-write behavior.
        if (ids.stream().anyMatch(Objects::isNull) || new HashSet<>(ids).size() != ids.size()) {
            throw new IllegalStateException("Notification result identities are missing or duplicated");
        }
        List<Map<String, Object>> messages = rows("system_notify_message", tenant,
                "id IN (" + placeholders(ids.size()) + ")", ids.toArray());
        if (messages.size() != ids.size()) {
            throw new IllegalStateException("Created notifications must belong to the current task tenant");
        }
        append("notify", "notifications", task,
                JsonUtils.toJsonString(Map.of("system_notify_message", List.of())),
                JsonUtils.toJsonString(Map.of("system_notify_message", messages)));
    }

    private Long lockTask(MesProEdhrWorkTaskDO task) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Task auxiliary auditing requires its business transaction");
        }
        Objects.requireNonNull(task, "Work task is required");
        Objects.requireNonNull(task.getId(), "Persisted work task is required");
        Long tenant = TenantContextHolder.getRequiredTenantId();
        audit.acquireLedgerLock(); // Also validates authenticated actor/current tenant.
        if (rows("mes_pro_edhr_work_task", tenant, "id = ?", task.getId()).size() != 1) {
            throw new IllegalStateException("Work task does not belong to the current tenant");
        }
        return tenant;
    }

    private String entitlementState(Long tenant, String sourceKey, String policy, Set<Long> users) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("system_entitlement_claim", sourceRows("system_entitlement_claim", tenant, sourceKey, policy));
        // The existing algorithm rebuilds ALL grants of each affected user, across policies.
        state.put("system_entitlement_grant", users.isEmpty() ? List.of() : rows(
                "system_entitlement_grant", tenant, "resolved_user_id IN (" + placeholders(users.size()) + ")",
                users.toArray()));
        state.put("system_entitlement_audit_event", sourceRows(
                "system_entitlement_audit_event", tenant, sourceKey, policy));
        return JsonUtils.toJsonString(state); // Freeze before mutation; never retain mutable DO references.
    }

    private List<Map<String, Object>> sourceRows(String table, Long tenant, String sourceKey, String policy) {
        return rows(table, tenant, "source_type = ? AND source_key = ? AND policy_code = ?",
                "EDHR_WORK_TASK_ASSIGNEE", sourceKey, policy);
    }

    private List<Map<String, Object>> rows(String table, Long tenant, String predicate, Object... values) {
        List<Object> args = new ArrayList<>();
        args.add(tenant);
        Collections.addAll(args, values);
        return jdbc.queryForList("SELECT * FROM " + table
                + " WHERE tenant_id = ? AND deleted = FALSE AND " + predicate + " ORDER BY id FOR UPDATE",
                args.toArray());
    }

    private static String placeholders(int size) { return String.join(",", Collections.nCopies(size, "?")); }

    private void append(String kind, String method, MesProEdhrWorkTaskDO task, String before, String after) {
        String version = DigestUtil.sha256Hex(after);
        audit.append(GxpAuditCommand.builder().operationId("mes.work-task." + kind)
                .subjectId(task.getId().toString()).subjectVersion(version)
                .beforeState(envelope(before)).afterState(envelope(after))
                .reason("Current work task " + kind + " effects").reasonSource("SYSTEM")
                .reasonCode("WORK_TASK_AUXILIARY_CHANGE").resultStatus("SUCCESS")
                // The caller owns replay. Each actual mutation is one fact; no-write calls append nothing.
                .idempotencyKey("task-aux:" + UUID.randomUUID())
                .sourceType("SERVICE_METHOD").sourceLocator(MesWorkTaskAuxiliaryAudit.class.getName() + "#" + method)
                .links(List.of(new GxpAuditRelation("SUBJECT", "MES_WORK_TASK", task.getId().toString(), version, null)))
                .build());
    }

    private static GxpAuditStateEnvelope envelope(String json) {
        return GxpAuditStateEnvelope.builder().state("PRESENT")
                .objectVersion(DigestUtil.sha256Hex(json)).canonicalJson(json).build();
    }
}
