package cn.iocoder.yudao.server.profileworkbench;

import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.profileworkbench.*;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import com.baomidou.dynamic.datasource.annotation.DS;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import java.util.List;
@Service
@DS("master")
public class ProfileWorkbenchTodoQueryService {
    @Resource private PermissionApi permissionApi;
    @Resource private ProfileWorkbenchTodoReadTransaction reader;
    public ProfileWorkbenchTodoPageRespVO page(ProfileWorkbenchTodoPageReqVO request) {
        long start = System.nanoTime();
        ProfileWorkbenchTodoQueryDTO query = identityAndAuthorize(request.getEnabledSources());
        query.setVisibility(request.getVisibility()); query.setTaskType(request.getTaskType());
        if (request.getQuickFilter() != null) {
            query.setFilterField(request.getQuickFilter().getFieldKey());
            query.setFilterOperator(request.getQuickFilter().getOperator());
            query.setFilterValue(request.getQuickFilter().getValue().replaceAll("^[\\s\\p{Z}\\uFEFF]+|[\\s\\p{Z}\\uFEFF]+$", ""));
        }
        if (request.getSort() != null) {
            query.setSortKey(request.getSort().getKey()); query.setSortOrder(request.getSort().getOrder());
        }
        return reader.page(query, sources(request.getEnabledSources()), request.getPageNo(), request.getPageSize(), start);
    }
    public ProfileWorkbenchTodoCountRespVO count(ProfileWorkbenchTodoCountReqVO request) {
        long start = System.nanoTime();
        ProfileWorkbenchTodoQueryDTO query = identityAndAuthorize(request.getEnabledSources());
        return reader.count(query, sources(request.getEnabledSources()), start);
    }
    private List<ProfileWorkbenchTodoSourceId> sources(List<ProfileWorkbenchTodoSourceId> ids) {
        if (ids == null || ids.stream().anyMatch(java.util.Objects::isNull)) throw exception(new ErrorCode(400, "工作台来源选择无效"));
        return ids.stream().distinct().sorted().toList();
    }
    private ProfileWorkbenchTodoQueryDTO identityAndAuthorize(List<ProfileWorkbenchTodoSourceId> requested) {
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("Workbench authorization must precede its own read transaction");
        Long user = SecurityFrameworkUtils.getLoginUserId();
        Long tenant = TenantContextHolder.getTenantId();
        if (user == null) throw exception(new ErrorCode(401, "工作台登录上下文缺失"));
        if (tenant == null) throw new IllegalStateException("Workbench tenant context missing");
        // Standard authorization may write temporary permission USE audits.
        // It must complete before the separate read-only RR transaction starts.
        for (ProfileWorkbenchTodoSourceId id : sources(requested)) {
            String[] permissions = switch (id) {
                case DCC_DISTRIBUTION -> new String[]{"dcc:controlled-file:query"};
                case DCC_TRAINING -> new String[]{"dcc:controlled-file:training:mine"};
                case EDHR_WORK_TASK -> new String[]{"mes:pro-edhr-work-task:query", "mes:pro-edhr-batch-execution:query"};
                case WORK_ORDER -> new String[]{"mes:pro-work-order:query"};
                case SHOWROOM_ASSIGNMENT -> new String[0];
            };
            if (permissions.length > 0 && !permissionApi.hasAnyPermissions(user, permissions))
                throw exception(new ErrorCode(403, "没有该工作台来源的访问权限"));
        }
        ProfileWorkbenchTodoQueryDTO query = new ProfileWorkbenchTodoQueryDTO();
        query.setUserId(user); query.setTenantId(tenant);
        return query;
    }
}
