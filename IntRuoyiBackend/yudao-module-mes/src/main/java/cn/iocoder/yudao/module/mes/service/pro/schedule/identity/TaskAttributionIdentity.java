package cn.iocoder.yudao.module.mes.service.pro.schedule.identity;

/**
 * 生产任务到排产工序、排产工单、生产工单的归属身份。
 */
public record TaskAttributionIdentity(Long taskId, Long scheduleOrderProcessId, Long scheduleOrderId,
                                      Long workOrderId) {

    public static TaskAttributionIdentity of(Long taskId, Long scheduleOrderProcessId, Long scheduleOrderId,
                                             Long workOrderId) {
        return new TaskAttributionIdentity(taskId, scheduleOrderProcessId, scheduleOrderId, workOrderId);
    }

}
