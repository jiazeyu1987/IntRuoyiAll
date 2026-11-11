package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrOperationAuditEventDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrOperationAuditEventMapper;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

/** Interaction observations never assert a business decision or alter an order. */
@Service
@RequiredArgsConstructor
public class MesWorkflowInteractionLog {
    public static final String OBJECT_TYPE = "WORKFLOW_INTERACTION";
    private final MesProEdhrOperationAuditService audit;
    private final MesProEdhrOperationAuditEventMapper mapper;
    private final AdminUserService users;

    public static String scopeLabel(String scope) {
        return switch (scope) {
            case "TEAM_LEADER" -> "生产与检验组长";
            case "PRODUCTION" -> "一线生产";
            case "PQC" -> "一线过程检验";
            case "PQC_RELEASE" -> "生产放行";
            case "MARKET_RELEASE" -> "上市放行与反查";
            case "BUSINESS_REQUEST" -> "生产业务请求";
            default -> throw new IllegalArgumentException("操作日志页面范围无效");
        };
    }

    public void recordClient(Long actor, String scope, String outcome, String control) {
        String label = scopeLabel(scope);
        if (!Set.of("CANCELED", "BLOCKED").contains(outcome))
            throw new IllegalArgumentException("页面只能记录取消或校验拦截，不能宣称业务成功");
        if (control == null || control.isBlank() || control.length() > 80
                || !control.matches("[\\p{IsHan}A-Za-z0-9（）()：:、，。/＋+·\\s-]+"))
            throw new IllegalArgumentException("操作日志控件名称无效");
        record(actor, label + "：" + ("CANCELED".equals(outcome) ? "取消" : "操作被拦截") + "（" + control + "）",
                outcome, "PAGE_INTERACTION", Map.of("scope", scope, "source", "PAGE_INTERACTION", "control", control));
    }

    public void recordRequestFailure(Long actor, String action, String method, String endpoint, int code) {
        String chinese = action != null && action.matches(".*[\\p{IsHan}].*") ? action : "执行生产业务操作";
        record(actor, chinese + "失败", "FAILED", "REQUEST_FAILED",
                Map.of("source", "HTTP_REQUEST", "method", method, "endpoint", endpoint, "errorCode", code));
    }

    private void record(Long actor, String action, String result, String operation, Map<String, Object> metadata) {
        if (actor == null || actor <= 0) throw new IllegalArgumentException("操作日志缺少已认证操作人");
        var user = users.getUser(actor);
        if (user == null || !Objects.equals(user.getTenantId(), TenantContextHolder.getRequiredTenantId())
                || user.getNickname() == null || user.getNickname().isBlank())
            throw new IllegalStateException("操作日志的人员或租户身份不一致");
        String requestId = UUID.randomUUID().toString();
        audit.record(new MesProEdhrOperationAuditCommand().setRequestId(requestId).setObjectType(OBJECT_TYPE)
                .setObjectId(requestId).setOperationType(operation).setActionName(action)
                .setActorUserId(actor).setActorUsername(user.getNickname()).setOccurredAt(LocalDateTime.now())
                .setPermissionDecision("BLOCKED".equals(result) || Objects.equals(metadata.get("errorCode"), 403)
                        ? "DENY" : "ALLOW").setResultStatus(result)
                .setMetadataJson(JsonUtils.toJsonString(metadata)));
    }

    public List<MesProEdhrOperationAuditEventDO> ownRecent(Long actor) {
        if (actor == null || actor <= 0) throw new IllegalArgumentException("操作日志缺少已认证操作人");
        TenantContextHolder.getRequiredTenantId();
        return mapper.selectList(new LambdaQueryWrapperX<MesProEdhrOperationAuditEventDO>()
                .eq(MesProEdhrOperationAuditEventDO::getObjectType, OBJECT_TYPE)
                .eq(MesProEdhrOperationAuditEventDO::getActorUserId, actor)
                .orderByDesc(MesProEdhrOperationAuditEventDO::getId).last("LIMIT 100"));
    }
}
