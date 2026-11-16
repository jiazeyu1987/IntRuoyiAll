package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrOperationAuditCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrOperationAuditService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Business changes and their log share the caller transaction. Read operations use a separate audit transaction. */
@Service
@RequiredArgsConstructor
public class MesActiveOrderActionLog {
    private final MesProEdhrOperationAuditService audit;
    private final AdminUserService users;

    public void record(Long actor, Long orderId, String operation, Map<String, Object> details) {
        audit.recordInCallerTransaction(command(actor, orderId, operation, details));
    }

    public void recordRead(Long actor, Long orderId, Long batchId, String operation, Map<String, Object> details) {
        audit.record(command(actor, orderId, operation, details).setBatchExecutionId(batchId));
    }

    public void recordPreparation(Long actor, Long workOrderId, boolean rebuild, Object documents) {
        var command = command(actor, workOrderId, rebuild ? "REBUILD_TEST_DOCUMENTS" : "PREPARE_TEST_DOCUMENTS",
                Map.of("workOrderId", workOrderId, "documents", documents));
        command.setObjectType("WORK_ORDER_PREPARATION");
        command.setMetadataJson(JsonUtils.toJsonString(Map.of("workOrderId", workOrderId, "documents", documents)));
        audit.recordInCallerTransaction(command);
    }

    private MesProEdhrOperationAuditCommand command(Long actor, Long orderId, String operation, Map<String, Object> details) {
        if (actor == null || actor <= 0 || orderId == null || orderId <= 0)
            throw new IllegalArgumentException("操作日志缺少操作人或订单身份");
        var user = users.getUser(actor);
        if (user == null || user.getNickname() == null || user.getNickname().isBlank())
            throw new IllegalStateException("操作日志缺少操作人姓名");
        String action = switch (operation) {
            case "PQC_PATROL_CREATE" -> "新建巡检轮次";
            case "PQC_PATROL_CANCEL" -> "撤销巡检轮次";
            case "PQC_PATROL_RESTORE" -> "恢复巡检轮次";
            case "PQC_FINAL_CREATE" -> "新建末检任务";
            case "REVERSE_TRACE_QUERY" -> "反查关联批次";
            case "REVERSE_TRACE_EVIDENCE" -> "查看反查命中记录";
            case "PREPARE_TEST_DOCUMENTS" -> "补齐固定测试工单来源单据";
            case "REBUILD_TEST_DOCUMENTS" -> "重置固定测试工单来源单据";
            default -> throw new IllegalArgumentException("操作日志动作未定义中文名称：" + operation);
        };
        var metadata = new LinkedHashMap<String, Object>(details);
        metadata.put("activeOrderId", orderId);
        return new MesProEdhrOperationAuditCommand().setRequestId(UUID.randomUUID().toString())
                .setObjectType("ACTIVE_ORDER_ACTION").setObjectId(orderId.toString())
                .setOperationType(operation).setActionName(action).setActorUserId(actor)
                .setActorUsername(user.getNickname()).setOccurredAt(LocalDateTime.now())
                .setPermissionDecision("ALLOW").setResultStatus("SUCCESS")
                .setMetadataJson(JsonUtils.toJsonString(metadata));
    }
}
