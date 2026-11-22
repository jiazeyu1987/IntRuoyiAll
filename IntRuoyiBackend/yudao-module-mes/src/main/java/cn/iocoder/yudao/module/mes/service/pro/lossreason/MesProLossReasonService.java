package cn.iocoder.yudao.module.mes.service.pro.lossreason;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

/** Tenant-wide append-only catalog. The sequence row serializes all writers, including imports. */
@Service
public class MesProLossReasonService {
    @Resource private JdbcTemplate jdbc;
    public record Reason(Long id, String reasonCode, String reasonName, LocalDateTime createTime) {}

    public static ServiceException invalid(String message) {
        return ServiceExceptionUtil.exception(new ErrorCode(1_017_991_001, "{}"), message);
    }

    public List<Reason> list(String keyword) {
        String filter = keyword == null ? "" : keyword.strip();
        return jdbc.query("SELECT id,reason_code,reason_name,create_time FROM mes_pro_loss_reason "
                        + "WHERE tenant_id=? AND (LOCATE(?,reason_code)>0 OR LOCATE(?,reason_name)>0) ORDER BY reason_number",
                (rs, row) -> new Reason(rs.getLong(1), rs.getString(2), rs.getString(3),
                        rs.getTimestamp(4).toLocalDateTime()), TenantContextHolder.getRequiredTenantId(), filter, filter);
    }

    @Transactional(rollbackFor = Exception.class)
    public List<Reason> create(List<String> descriptions) {
        if (descriptions == null || descriptions.isEmpty()) throw invalid("请填写损耗原因描述，导入文件不能为空");
        Long tenant = TenantContextHolder.getRequiredTenantId();
        jdbc.update("INSERT INTO mes_pro_loss_reason_sequence(tenant_id,next_number) VALUES (?,1) "
                + "ON DUPLICATE KEY UPDATE tenant_id=VALUES(tenant_id)", tenant);
        Long next = jdbc.queryForObject("SELECT next_number FROM mes_pro_loss_reason_sequence WHERE tenant_id=? FOR UPDATE", Long.class, tenant);
        Set<String> existing = new HashSet<>(list(null).stream().map(Reason::reasonName).toList());
        List<String> normalized = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (int index = 0; index < descriptions.size(); index++) {
            String value = descriptions.get(index) == null ? "" : descriptions.get(index).strip();
            String prefix = "第" + (index + 2) + "行：";
            if (value.isBlank()) errors.add(prefix + "损耗原因描述不能为空");
            else if (value.length() > 128) errors.add(prefix + "损耗原因描述不能超过128个字符");
            else if (!existing.add(value)) errors.add(prefix + "损耗原因描述重复：" + value);
            normalized.add(value);
        }
        if (!errors.isEmpty()) throw invalid("未导入任何记录。" + String.join("；", errors));
        if (next == null || next <= 0 || next > Long.MAX_VALUE - normalized.size()) throw invalid("损耗原因编号序列无效");
        String creator = String.valueOf(Objects.requireNonNull(SecurityFrameworkUtils.getLoginUserId()));
        List<Reason> created = new ArrayList<>();
        for (String name : normalized) {
            String code = "SHYY-" + String.format(Locale.ROOT, "%03d", next);
            // Use the project's distributed identity rather than restarting legacy reason IDs at 1.
            jdbc.update("INSERT INTO mes_pro_loss_reason(id,tenant_id,reason_number,reason_code,reason_name,creator) VALUES (?,?,?,?,?,?)",
                    IdWorker.getId(), tenant, next, code, name, creator);
            Reason reason = jdbc.queryForObject("SELECT id,reason_code,reason_name,create_time FROM mes_pro_loss_reason WHERE tenant_id=? AND reason_number=?",
                    (rs, row) -> new Reason(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getTimestamp(4).toLocalDateTime()), tenant, next);
            created.add(reason);
            next++;
        }
        jdbc.update("UPDATE mes_pro_loss_reason_sequence SET next_number=? WHERE tenant_id=?", next, tenant);
        return created;
    }

    /** Existing bindings are immutable; newly appended IDs must be from this tenant's catalog. */
    public void validateBindings(JSONObject before, JSONObject after) {
        Map<Long, JSONArray> previous = configs(before);
        Map<Long, JSONArray> incoming = configs(after);
        Map<Long, Reason> catalog = new HashMap<>();
        list(null).forEach(reason -> catalog.put(reason.id(), reason));
        for (Map.Entry<Long, JSONArray> entry : previous.entrySet()) {
            if (!entry.getValue().isEmpty() && !incoming.containsKey(entry.getKey()))
                throw invalid("已关联损耗原因的工序不能删除");
        }
        for (Map.Entry<Long, JSONArray> entry : incoming.entrySet()) {
            Map<Long, JSONObject> old = reasons(previous.getOrDefault(entry.getKey(), new JSONArray()));
            Map<Long, JSONObject> current = reasons(entry.getValue());
            for (Map.Entry<Long, JSONObject> reason : old.entrySet()) {
                if (!Objects.equals(reason.getValue(), current.get(reason.getKey())))
                    throw invalid("工序已关联的损耗原因禁止修改或解除关联");
            }
            for (Map.Entry<Long, JSONObject> reason : current.entrySet()) {
                if (old.containsKey(reason.getKey())) continue;
                Reason source = catalog.get(reason.getKey());
                JSONObject value = reason.getValue();
                if (source == null || !Objects.equals(source.reasonCode(), value.getString("reasonCode"))
                        || !Objects.equals(source.reasonName(), value.getString("reasonName"))
                        || !Boolean.TRUE.equals(value.getBoolean("enabled")))
                    throw invalid("新增损耗原因必须选择当前租户原因库中的有效记录");
            }
        }
    }
    /** 显式迁移时逐项核对正式原因，保留旧身份和停用状态；普通新增仍只允许原因库记录。 */
    public void validateLegacyBindings(Long routeId, JSONObject after) {
        record LegacyReason(Long id, Long routeProcessId, Long processId, String code, String name, Boolean enabled) {}
        List<LegacyReason> source = jdbc.query("SELECT r.id,r.route_process_id,r.process_id,r.reason_code,r.reason_name,r.enabled "
                        + "FROM mes_pro_process_pool_defect_reason r JOIN mes_pro_route_process p ON p.id=r.route_process_id "
                        + "AND p.tenant_id=r.tenant_id AND p.process_id=r.process_id "
                        + "WHERE r.tenant_id=? AND p.route_id=? AND r.reason_type='LOSS' AND r.deleted=0 AND p.deleted=0",
                (rs, row) -> new LegacyReason(rs.getLong("id"), rs.getLong("route_process_id"),
                        rs.getLong("process_id"), rs.getString("reason_code"), rs.getString("reason_name"),
                        rs.getObject("enabled", Boolean.class)), TenantContextHolder.getRequiredTenantId(), routeId);
        Map<Long, LegacyReason> expected = new HashMap<>();
        source.forEach(reason -> expected.put(reason.id(), reason));
        Set<Long> seen = new HashSet<>();
        for (Object raw : after.getJSONArray("productionProcessConfigs")) {
            JSONObject config = (JSONObject) JSON.toJSON(raw);
            for (var entry : reasons(config.getJSONArray("lossReasons")).entrySet()) {
                LegacyReason reason = expected.get(entry.getKey());
                JSONObject value = entry.getValue();
                if (reason == null || !seen.add(reason.id())
                        || !Objects.equals(reason.routeProcessId(), config.getLong("routeProcessId"))
                        || !Objects.equals(reason.processId(), config.getLong("processId"))
                        || !Objects.equals(reason.code(), value.getString("reasonCode"))
                        || !Objects.equals(reason.name(), value.getString("reasonName"))
                        || !Objects.equals(reason.enabled(), value.getBoolean("enabled"))) {
                    throw invalid("迁移损耗原因与所选路线正式配置不一致，请刷新后重试");
                }
            }
        }
        if (!seen.equals(expected.keySet())) {
            throw invalid("迁移损耗原因不完整，请刷新后重试");
        }
    }

    private static Map<Long, JSONArray> configs(JSONObject snapshot) {
        Map<Long, JSONArray> result = new LinkedHashMap<>();
        JSONArray configs = snapshot.getJSONArray("productionProcessConfigs");
        if (configs == null) return result;
        for (Object raw : configs) {
            JSONObject config = (JSONObject) com.alibaba.fastjson.JSON.toJSON(raw);
            JSONArray values = config.getJSONArray("lossReasons");
            if (values == null) throw invalid("工序损耗原因列表缺失");
            result.put(config.getLong("routeProcessId"), values);
        }
        return result;
    }
    private static Map<Long, JSONObject> reasons(JSONArray values) {
        Map<Long, JSONObject> result = new LinkedHashMap<>();
        for (Object raw : values) {
            JSONObject value = new JSONObject();
            value.putAll((JSONObject) com.alibaba.fastjson.JSON.toJSON(raw));
            Long id = value.getLong("id");
            if (id == null || id <= 0 || result.putIfAbsent(id, value) != null)
                throw invalid("损耗原因ID不能为空或重复");
            // The HTTP contract serializes large Long identities as strings for browser precision.
            value.put("id", id);
        }
        return result;
    }
}
