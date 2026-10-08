package cn.iocoder.yudao.module.system.api.profileworkbench;

public enum ProfileWorkbenchTodoSourceId {
    DCC_DISTRIBUTION("文控分发", "文控"), DCC_TRAINING("文控培训", "文控"),
    EDHR_WORK_TASK("eDHR工作任务", "批记录"), WORK_ORDER("待排产工单", "排产"),
    SHOWROOM_ASSIGNMENT("展厅补充指派", "展厅");
    public final String label;
    public final String taskType;
    ProfileWorkbenchTodoSourceId(String label, String taskType) { this.label = label; this.taskType = taskType; }
}
