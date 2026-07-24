package cn.iocoder.yudao.module.mes.service.pro.feedback;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.task.MesProTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FEEDBACK_IMPORT_SCHEDULE_ORDER_PROCESS_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FEEDBACK_IMPORT_SCHEDULE_ORDER_PROCESS_REMAINING_NOT_ENOUGH;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FEEDBACK_IMPORT_TARGET_TASK_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_TASK_ROUTE_PROCESS_MISMATCH;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_TASK_WORK_ORDER_MISMATCH;

/**
 * 固化报工归属链：taskId -> scheduleOrderProcessId -> scheduleOrderId -> workOrderId。
 */
@Component
public class FeedbackScheduleLinkageGuard {

    public void validateProvidedSnapshotRemaining(MesProFeedbackDO feedback,
                                                  MesProScheduleOrderProcessDO scheduleOrderProcess) {
        if (scheduleOrderProcess == null) {
            throw exception(PRO_FEEDBACK_IMPORT_SCHEDULE_ORDER_PROCESS_NOT_EXISTS);
        }
        validateRemaining(feedback.getFeedbackQuantity(), scheduleOrderProcess);
    }

    public boolean validateDirectImportAttributionChain(MesProWorkOrderDO workOrder,
                                                        MesProTaskDO task,
                                                        MesProScheduleOrderDO scheduleOrder,
                                                        MesProScheduleOrderProcessDO scheduleOrderProcess,
                                                        BigDecimal feedbackQuantity) {
        if (workOrder == null || task == null || scheduleOrder == null || scheduleOrderProcess == null) {
            return false;
        }
        if (!Objects.equals(scheduleOrder.getWorkOrderId(), workOrder.getId())
                || !Objects.equals(scheduleOrderProcess.getScheduleOrderId(), scheduleOrder.getId())) {
            return false;
        }
        if (!Objects.equals(task.getWorkOrderId(), workOrder.getId())) {
            return false;
        }
        return hasRemaining(feedbackQuantity, scheduleOrderProcess);
    }

    public Optional<MesProTaskDO> resolveUniqueTaskForScheduleProcess(String rowTaskCode,
                                                                      MesProWorkOrderDO workOrder,
                                                                      MesProScheduleOrderProcessDO scheduleOrderProcess,
                                                                      Map<Long, List<MesProTaskDO>> taskByScheduleProcessId) {
        List<MesProTaskDO> tasks = taskByScheduleProcessId.getOrDefault(scheduleOrderProcess.getId(), List.of()).stream()
                .filter(task -> Objects.equals(task.getWorkOrderId(), workOrder.getId()))
                .filter(task -> Objects.equals(task.getProcessId(), scheduleOrderProcess.getProcessId()))
                .toList();
        if (tasks.isEmpty()) {
            return Optional.empty();
        }
        List<MesProTaskDO> taskCodeMatches = tasks.stream()
                .filter(task -> StrUtil.equals(task.getCode(), rowTaskCode))
                .toList();
        if (taskCodeMatches.size() == 1) {
            return Optional.of(taskCodeMatches.get(0));
        }
        if (taskCodeMatches.size() > 1) {
            return Optional.empty();
        }
        if (tasks.size() == 1) {
            return Optional.of(tasks.get(0));
        }
        return rejectTextOnlyAttribution();
    }

    private Optional<MesProTaskDO> rejectTextOnlyAttribution() {
        return Optional.empty();
    }

    private void validateRemaining(BigDecimal feedbackQuantity,
                                   MesProScheduleOrderProcessDO scheduleOrderProcess) {
        BigDecimal quantity = ObjectUtil.defaultIfNull(feedbackQuantity, BigDecimal.ZERO);
        BigDecimal remainingQuantity = ObjectUtil.defaultIfNull(scheduleOrderProcess.getRemainingQuantity(), BigDecimal.ZERO);
        if (quantity.compareTo(remainingQuantity) > 0) {
            throw exception(PRO_FEEDBACK_IMPORT_SCHEDULE_ORDER_PROCESS_REMAINING_NOT_ENOUGH);
        }
    }

    private boolean hasRemaining(BigDecimal feedbackQuantity,
                                 MesProScheduleOrderProcessDO scheduleOrderProcess) {
        BigDecimal quantity = ObjectUtil.defaultIfNull(feedbackQuantity, BigDecimal.ZERO);
        BigDecimal remainingQuantity = ObjectUtil.defaultIfNull(scheduleOrderProcess.getRemainingQuantity(), BigDecimal.ZERO);
        return quantity.compareTo(remainingQuantity) <= 0;
    }

}
