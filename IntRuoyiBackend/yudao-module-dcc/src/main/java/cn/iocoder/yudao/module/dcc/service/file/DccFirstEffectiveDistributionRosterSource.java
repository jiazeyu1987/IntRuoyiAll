package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Reads the first effective dispatch's direct members; the batch owns persistence and frozen replays. */
@Service
public class DccFirstEffectiveDistributionRosterSource implements DccDistributionRosterSource {
    private final DeptService deptService;
    private final AdminUserApi adminUserApi;

    public DccFirstEffectiveDistributionRosterSource(DeptService deptService, AdminUserApi adminUserApi) {
        this.deptService = deptService;
        this.adminUserApi = adminUserApi;
    }

    @Override
    public Resolution resolve(Request request) {
        if (request == null || TenantContextHolder.isIgnore()
                || !Objects.equals(request.tenantId(), TenantContextHolder.getTenantId())
                || !positive(request.tenantId()) || !positive(request.batchId()) || !positive(request.sourceEventId())
                || !positive(request.masterId()) || !positive(request.controlledFileId())
                || !text(request.eventKey()) || !text(request.versionNo()) || !text(request.approvalProcessInstanceId())
                || request.activatedAt() == null || request.activatedAt().isAfter(LocalDateTime.now()))
            return new NotReady("ROSTER_REQUEST_INVALID", "首次生效下发缺少当前租户、精确版本或生效事件身份");
        if (request.savedDepartmentIds() == null || request.savedDepartmentIds().isEmpty())
            return new NotReady("ROSTER_DEPARTMENTS_MISSING", "本版本没有保存下发部门，不能生成空名单成功");
        var ids = new TreeSet<Long>();
        for (String value : request.savedDepartmentIds()) {
            if (value == null || !value.matches("[1-9][0-9]{0,18}"))
                return new NotReady("ROSTER_REQUEST_INVALID", "保存下发部门身份无效：" + value);
            final long id;
            try { id = Long.parseLong(value); }
            catch (NumberFormatException invalid) { return new NotReady("ROSTER_REQUEST_INVALID", "保存下发部门身份超出 Long 范围：" + value); }
            if (!ids.add(id)) return new NotReady("ROSTER_REQUEST_INVALID", "保存下发部门身份重复：" + value);
        }
        // Only these internal directory reads ignore the actor's department data permission. Tenant filtering remains enabled.
        return DataPermissionUtils.executeIgnore(() -> readDirectory(request, List.copyOf(ids)));
    }

    private Resolution readDirectory(Request request, List<Long> departmentIds) {
        var unavailable = new ArrayList<Long>();
        for (Long id : departmentIds) {
            var department = deptService.getDept(id);
            if (department == null || !Objects.equals(id, department.getId())
                    || !Objects.equals(request.tenantId(), department.getTenantId())
                    || !Objects.equals(CommonStatusEnum.ENABLE.getStatus(), department.getStatus())) unavailable.add(id);
        }
        if (!unavailable.isEmpty())
            return new NotReady("ROSTER_DEPARTMENT_UNAVAILABLE", "保存下发部门不存在、停用或租户身份不匹配：" + unavailable);
        var users = adminUserApi.getUserListByDeptIds(departmentIds);
        if (users == null) throw new IllegalStateException("系统目录未返回下发部门成员读取结果：" + departmentIds);
        var members = new TreeMap<Long, List<Long>>();
        departmentIds.forEach(id -> members.put(id, new ArrayList<>()));
        var seen = new HashSet<Long>();
        for (var user : users) {
            if (user == null || !positive(user.getId()) || !Objects.equals(request.tenantId(), user.getTenantId())
                    || !members.containsKey(user.getDeptId()) || !seen.add(user.getId())
                    || (!Objects.equals(CommonStatusEnum.ENABLE.getStatus(), user.getStatus())
                        && !Objects.equals(CommonStatusEnum.DISABLE.getStatus(), user.getStatus())))
                return new NotReady("ROSTER_DIRECTORY_INVALID", "下发目录人员身份、租户、直接部门或状态无效/重复：" + (user == null ? "null" : user.getId()));
            if (Objects.equals(CommonStatusEnum.ENABLE.getStatus(), user.getStatus())) members.get(user.getDeptId()).add(user.getId());
        }
        var empty = members.entrySet().stream().filter(entry -> entry.getValue().isEmpty()).map(Map.Entry::getKey).toList();
        if (!empty.isEmpty()) return new NotReady("ROSTER_DEPARTMENT_EMPTY", "下发部门没有当前启用直接成员：" + empty);
        var scopes = members.entrySet().stream().map(entry -> new DccDistributionRosterSnapshot.Department(entry.getKey(), entry.getValue().stream().sorted().toList())).toList();
        // Match the existing DATETIME(6) precision so the persisted snapshot hash survives a read/retry.
        var snapshotAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        var snapshot = new DccDistributionRosterSnapshot(request.tenantId(), request.controlledFileId(), request.versionNo(),
                request.approvalProcessInstanceId(), snapshotAt, "FIRST_EFFECTIVE_DISPATCH_V1", scopes);
        var identity = new TreeMap<String, Object>();
        identity.put("sourceSystem", "SYSTEM_USER_DIRECTORY"); identity.put("tenantId", request.tenantId().toString());
        identity.put("batchId", request.batchId().toString()); identity.put("sourceEventId", request.sourceEventId().toString());
        identity.put("eventKey", request.eventKey()); identity.put("masterId", request.masterId().toString());
        identity.put("controlledFileId", request.controlledFileId().toString()); identity.put("versionNo", request.versionNo());
        identity.put("approvalProcessInstanceId", request.approvalProcessInstanceId()); identity.put("savedDepartmentIds", request.savedDepartmentIds());
        return new Ready(snapshot, JsonUtils.toJsonString(identity),
                "IC32 用户规则定案（2026-10-13）：下发名单是首次生效下发时冻结；保存的启用直接部门当前启用成员；后续重试保留原义务，失效账号具体阻断");
    }
    private static boolean positive(Long value) { return value != null && value > 0; }
    private static boolean text(String value) { return value != null && !value.isBlank(); }
}
