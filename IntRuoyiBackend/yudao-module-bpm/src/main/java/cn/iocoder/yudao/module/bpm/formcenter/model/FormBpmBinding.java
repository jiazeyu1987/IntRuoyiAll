package cn.iocoder.yudao.module.bpm.formcenter.model;

public class FormBpmBinding {

    private final String processInstanceId;
    private final String lastTaskId;

    public FormBpmBinding(String processInstanceId, String lastTaskId) {
        this.processInstanceId = processInstanceId;
        this.lastTaskId = lastTaskId;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public String getLastTaskId() {
        return lastTaskId;
    }

}
