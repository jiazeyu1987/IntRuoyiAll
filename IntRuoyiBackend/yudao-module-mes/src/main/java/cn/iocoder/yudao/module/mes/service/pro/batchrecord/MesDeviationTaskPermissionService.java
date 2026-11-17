package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Set;

/** Re-evaluate the stored responsibility against the recipient's current authority. */
@Service
public class MesDeviationTaskPermissionService {
    private static final Set<String> TYPES = Set.of("DEVIATION_RECEIVE", "DEVIATION_CORRECT", "DEVIATION_VERIFY",
            "DEVIATION_SIGN", "DEVIATION_APPROVE", "DEVIATION_CLOSE");
    @Resource private PermissionApi permissionApi;
    @Resource private AdminUserApi adminUserApi;

    public boolean isDeviationTask(String type) {
        return type != null && TYPES.contains(type);
    }

    public String blockedReason(Long tenant, Long userId, String type, String node) {
        if (!isDeviationTask(type)) throw new IllegalArgumentException("不是偏差职责待办");
        var user = userId == null ? null : adminUserApi.getUser(userId);
        if (user == null || !Objects.equals(userId, user.getId()) || !Objects.equals(tenant, user.getTenantId())
                || !CommonStatusEnum.isEnable(user.getStatus())) {
            return "当前账号已停用或不属于本租户，无法办理偏差任务，请联系管理员调整责任人";
        }
        if (!permissionApi.hasAnyPermissions(userId, "mes:pro-edhr-work-task:query")) {
            return "当前账号缺少工作任务查询权限，无法办理偏差任务，请联系管理员";
        }
        String permission = switch (type) {
            case "DEVIATION_RECEIVE" -> "mes:pro-edhr-deviation:handle";
            // Rectification is authorized by the assigned personal task, not an investigation role.
            case "DEVIATION_CORRECT" -> null;
            case "DEVIATION_VERIFY" -> "CORRECTIVE_VERIFIER".equals(node)
                    ? MesProEdhrDeviationReviewTaskService.permission(node) : "INVALID";
            case "DEVIATION_CLOSE" -> "CLOSE".equals(node)
                    ? MesProEdhrDeviationReviewTaskService.permission(node) : "INVALID";
            case "DEVIATION_SIGN" -> node != null && Set.of("PREPARER", "VERIFIER", "DEPARTMENT_OWNER").contains(node)
                    ? MesProEdhrDeviationReviewTaskService.permission(node) : "INVALID";
            case "DEVIATION_APPROVE" -> node != null && Set.of("QA", "QUALITY_OWNER", "MANAGEMENT_REP").contains(node)
                    ? MesProEdhrDeviationReviewTaskService.permission(node) : "INVALID";
            default -> throw new IllegalArgumentException("不是偏差职责待办");
        };
        if ("INVALID".equals(permission)) return "偏差任务职责信息不完整，请联系管理员核对后办理";
        if (permission != null && !permissionApi.hasAnyPermissions(userId, permission)) {
            return "当前账号已无此偏差职责的办理权限，请联系管理员恢复权限或调整责任人";
        }
        if ("DEVIATION_RECEIVE".equals(type)
                && !permissionApi.hasAnyPermissions(userId, "mes:pro-edhr-deviation:query")) {
            return "当前账号缺少偏差查询权限，无法办理调查任务，请联系管理员";
        }
        return null;
    }
}
