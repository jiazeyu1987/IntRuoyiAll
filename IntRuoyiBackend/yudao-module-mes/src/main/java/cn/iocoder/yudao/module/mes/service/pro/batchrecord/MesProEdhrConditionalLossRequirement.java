package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionOriginDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import com.alibaba.fastjson.JSON;
import java.util.List;

/** Shared interpretation of the frozen HAS_ACTUAL_LOSS condition. */
public final class MesProEdhrConditionalLossRequirement {
    private MesProEdhrConditionalLossRequirement() { }

    public static boolean isConditionalLoss(MesProEdhrBatchExecutionTaskDO task) {
        return "CONDITIONAL_REQUIRED".equals(task.getRequiredPolicy())
                && "LOSS_REPORT".equals(task.getFormSlotType());
    }

    public static boolean required(MesProEdhrBatchExecutionTaskDO task, Boolean hasActualLoss) {
        if (!isConditionalLoss(task)) return !Boolean.FALSE.equals(task.getRequiredFlag());
        var condition = JSON.parseObject(task.getRequiredConditionJson());
        if (condition == null || !"HAS_ACTUAL_LOSS".equals(condition.getString("type"))) {
            throw new IllegalStateException("unsupported conditional loss form: " + task.getRequiredConditionJson());
        }
        // Absence of a formal decision never establishes no loss.
        return !Boolean.FALSE.equals(task.getRequiredFlag()) && !Boolean.FALSE.equals(hasActualLoss);
    }

    public static Boolean formalDecision(List<MesProEdhrBatchExecutionOriginDO> origins) {
        if (origins == null || origins.isEmpty()) return null;
        if (origins.stream().anyMatch(origin -> Boolean.TRUE.equals(origin.getHasActualLoss()))) return true;
        return origins.stream().anyMatch(origin -> origin.getHasActualLoss() == null) ? null : false;
    }
}
