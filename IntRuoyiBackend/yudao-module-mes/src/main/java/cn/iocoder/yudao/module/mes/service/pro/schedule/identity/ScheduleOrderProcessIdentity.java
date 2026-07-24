package cn.iocoder.yudao.module.mes.service.pro.schedule.identity;

/**
 * 排产工单内的排产工序身份。
 *
 * <p>用于区分 {@code scheduleOrderId + routeProcessId} 的快照工序口径，
 * 并集中保留历史任务重关联所需的字符串 Key。</p>
 */
public record ScheduleOrderProcessIdentity(Long scheduleOrderId, Long routeProcessId,
                                           Long scheduleOrderProcessId) {

    public static ScheduleOrderProcessIdentity of(Long scheduleOrderId, Long routeProcessId,
                                                  Long scheduleOrderProcessId) {
        return new ScheduleOrderProcessIdentity(scheduleOrderId, routeProcessId, scheduleOrderProcessId);
    }

    public static String workOrderProcessKey(Long workOrderId, Long processId) {
        return workOrderId + "_" + processId;
    }

    public static String scheduleOrderProcessTaskKey(Long workOrderId, Long scheduleOrderProcessId) {
        return workOrderId + "_SOP_" + scheduleOrderProcessId;
    }

}
