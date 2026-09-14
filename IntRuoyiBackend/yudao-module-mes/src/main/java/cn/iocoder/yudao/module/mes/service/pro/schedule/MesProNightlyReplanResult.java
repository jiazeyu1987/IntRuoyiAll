package cn.iocoder.yudao.module.mes.service.pro.schedule;

import lombok.Data;

@Data
public class MesProNightlyReplanResult {

    public static final String JOB_SUCCESS_PREFIX = "夜间重排完成：";

    private int scheduleOrderCount;
    private int generatedTaskCount;
    private int preservedTaskCount;
    private int blockingIssueCount;
    private int shortageCount;

    public String toJobMessage() {
        if (scheduleOrderCount == 0) {
            return JOB_SUCCESS_PREFIX + "没有待重排排产工单";
        }
        return String.format(JOB_SUCCESS_PREFIX + "排产工单 %d，生成任务 %d，保护任务 %d，阻塞 %d，短缺 %d",
                scheduleOrderCount, generatedTaskCount, preservedTaskCount, blockingIssueCount, shortageCount);
    }

    public static boolean isSuccessfulJobMessage(String message) {
        return message != null && message.startsWith(JOB_SUCCESS_PREFIX);
    }

}
