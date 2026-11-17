package cn.iocoder.yudao.module.mes.service.notify;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesDeviationTaskDescription;
import cn.iocoder.yudao.module.system.dal.dataobject.notify.NotifyMessageDO;
import cn.iocoder.yudao.module.system.service.notify.NotifyMessageReadEnricher;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.Map;
import java.util.Set;
import java.math.BigDecimal;
import java.util.LinkedHashMap;

/**
 * Adds a read-only projection for historical deviation notifications. The
 * persisted message snapshot and work task are intentionally never changed.
 */
@Component
public class MesDeviationNotifyMessageReadEnricher implements NotifyMessageReadEnricher {
    private static final Map<String, String> SIMPLE_TYPES = Map.of(
            "MES_EDHR_DEVIATION_RECEIVE_ASSIGNED", "DEVIATION_RECEIVE",
            "MES_EDHR_DEVIATION_CORRECT_ASSIGNED", "DEVIATION_CORRECT");
    private static final String REVIEW = "MES_EDHR_DEVIATION_REVIEW_ASSIGNED";
    private static final Set<String> REVIEW_TYPES = Set.of(
            "DEVIATION_SIGN", "DEVIATION_APPROVE", "DEVIATION_CLOSE", "DEVIATION_VERIFY");

    @Resource
    private MesProEdhrWorkTaskMapper taskMapper;

    @Override
    public void enrich(NotifyMessageDO message) {
        String code = message.getTemplateCode();
        if (code == null) return;
        String expectedType = SIMPLE_TYPES.get(code);
        if (expectedType == null && !REVIEW.equals(code)) return;
        Map<String, Object> params = message.getTemplateParams();
        // New notifications already contain their historical business snapshot. Never
        // re-resolve it against a task whose owner or status may since have changed.
        if (params != null && hasText(params.get("taskTitle")) && hasText(params.get("workOrderCode"))) return;
        if (params == null || params.get("workTaskId") == null) {
            invalid(message, "历史偏差通知缺少关联任务，请联系管理员核对");
            return;
        }
        Long taskId = parseTaskId(params.get("workTaskId"));
        if (taskId == null || taskId <= 0) {
            invalid(message, "历史偏差通知的关联任务编号无效，请联系管理员核对");
            return;
        }
        Long tenantId = message.getTenantId();
        if (tenantId == null) {
            invalid(message, "历史偏差通知缺少所属组织，请联系管理员核对");
            return;
        }
        MesProEdhrWorkTaskDO task = taskMapper.selectNotificationSource(taskId, tenantId);
        if (task == null) {
            invalid(message, "历史偏差通知的关联任务已失效，请联系管理员核对");
            return;
        }
        if (expectedType != null && !expectedType.equals(task.getTaskType())) {
            invalid(message, "历史偏差通知与关联任务类型不一致，请联系管理员核对");
            return;
        }
        if (expectedType == null && !REVIEW_TYPES.contains(task.getTaskType())) {
            invalid(message, "历史偏差通知与关联职责不一致，请联系管理员核对");
            return;
        }
        if (!hasText(task.getWorkOrderCode())) {
            invalid(message, "历史偏差任务缺少生产订单，请联系管理员核对");
            return;
        }
        if (REVIEW_TYPES.contains(task.getTaskType()) && !"DEVIATION_VERIFY".equals(task.getTaskType())
                && !"DEVIATION_CLOSE".equals(task.getTaskType())
                && !Set.of("PREPARER", "VERIFIER", "DEPARTMENT_OWNER", "QA", "QUALITY_OWNER", "MANAGEMENT_REP")
                    .contains(task.getSignatureCellKey() == null ? "" : task.getSignatureCellKey())) {
            invalid(message, "历史偏差任务缺少有效职责，请联系管理员核对");
            return;
        }
        String title = taskTitle(task);
        params.put("taskTitle", title);
        params.put("workTaskId", taskId);
        params.put("workOrderCode", task.getWorkOrderCode());
        params.put("batchCode", task.getBatchCode());
        if (task.getProcessName() != null) params.put("processName", task.getProcessName());
    }

    private static Long parseTaskId(Object raw) {
        try {
            if (raw instanceof Number) return new BigDecimal(raw.toString()).longValueExact();
            return Long.valueOf(String.valueOf(raw));
        } catch (NumberFormatException | ArithmeticException ex) {
            return null; // Invalid formal identity is explicitly reported on this item.
        }
    }

    private static boolean hasText(Object value) {
        return value instanceof String text && !text.isBlank();
    }

    private static void invalid(NotifyMessageDO message, String error) {
        Map<String, Object> params = message.getTemplateParams();
        if (params == null) {
            params = new LinkedHashMap<>();
            message.setTemplateParams(params);
        }
        params.put("presentationError", error);
        // No executable entry is returned for a failed historical identity.
        params.remove("actionUrl");
        params.remove("workTaskId");
        message.setTemplateContent(error);
    }

    private static String taskTitle(MesProEdhrWorkTaskDO task) {
        if ("DEVIATION_VERIFY".equals(task.getTaskType())) return "完成偏差结果验证";
        return MesDeviationTaskDescription.title(task.getTaskType(), task.getSignatureCellKey(), false);
    }
}
