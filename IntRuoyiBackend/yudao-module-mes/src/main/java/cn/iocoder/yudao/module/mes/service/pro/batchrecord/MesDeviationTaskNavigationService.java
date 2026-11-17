package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesDeviationTaskNavigationRespVO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_DEVIATION_HANDLING_PERMISSION_DENIED;

/** Resolve a notification against its stored task, never against client-provided responsibilities. */
@Service
public class MesDeviationTaskNavigationService {
    private static final Set<String> TYPES = Set.of("DEVIATION_RECEIVE", "DEVIATION_CORRECT", "DEVIATION_VERIFY",
            "DEVIATION_SIGN", "DEVIATION_APPROVE", "DEVIATION_CLOSE");
    private static final Set<String> PATHS = Set.of("/mes/pro/feedback/edhr-deviation",
            "/mes/pro/feedback/edhr-deviation/detail");
    @Resource private MesProEdhrWorkTaskMapper taskMapper;
    @Resource private MesProEdhrDeviationAccessService accessService;
    @Resource private MesDeviationTaskPermissionService taskPermissionService;

    public MesDeviationTaskNavigationRespVO get(Long actor, Long workTaskId) {
        if (actor == null || actor <= 0 || workTaskId == null || workTaskId <= 0) denied();
        // This legacy task DO has no tenant field; scope the SQL by the real table column.
        var task = taskMapper.selectOne(new QueryWrapper<MesProEdhrWorkTaskDO>()
                .eq("id", workTaskId).eq("tenant_id", TenantContextHolder.getRequiredTenantId()).eq("deleted", false));
        if (task == null || !Objects.equals(task.getId(), workTaskId)
                || !Boolean.FALSE.equals(task.getDeleted()) || !Objects.equals(actor, task.getAssigneeUserId())
                || task.getTaskType() == null || !TYPES.contains(task.getTaskType()) || !"EDHR_DEVIATION".equals(task.getBusinessScopeType())
                || task.getBusinessScopeId() == null || task.getBusinessScopeId() <= 0) denied();
        accessService.requireRead(task.getBusinessScopeId());
        String blocked = taskPermissionService.blockedReason(TenantContextHolder.getRequiredTenantId(), actor,
                task.getTaskType(), task.getSignatureCellKey());
        if (blocked != null) throw exception(PRO_EDHR_DEVIATION_HANDLING_PERMISSION_DENIED, blocked);
        if (task.getActionUrl() == null) denied();
        try {
            URI url = URI.create(task.getActionUrl());
            if (url.isAbsolute() || url.getRawAuthority() != null || !PATHS.contains(url.getPath())) denied();
            var deviationIds = values(url, "deviationId");
            var taskIds = values(url, "workTaskId");
            if (deviationIds.size() != 1 || !task.getBusinessScopeId().toString().equals(deviationIds.get(0))
                    || taskIds.size() > 1 || (!taskIds.isEmpty() && !workTaskId.toString().equals(taskIds.get(0)))) denied();
        } catch (IllegalArgumentException malformedUrl) {
            denied();
        }
        return new MesDeviationTaskNavigationRespVO(task.getId(), task.getTaskType(), task.getBusinessScopeType(),
                task.getBusinessScopeId(), task.getActionUrl());
    }

    private List<String> values(URI url, String key) {
        if (url.getRawQuery() == null) return List.of();
        return Arrays.stream(url.getRawQuery().split("&")).map(part -> part.split("=", 2))
                .filter(parts -> key.equals(URLDecoder.decode(parts[0], StandardCharsets.UTF_8)))
                .map(parts -> parts.length == 1 ? "" : URLDecoder.decode(parts[1], StandardCharsets.UTF_8)).toList();
    }

    private void denied() {
        throw exception(PRO_EDHR_DEVIATION_HANDLING_PERMISSION_DENIED, "查看本人偏差任务入口");
    }
}
