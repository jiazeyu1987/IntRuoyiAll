package cn.iocoder.yudao.module.bpm.workbench;
import cn.iocoder.yudao.module.system.api.workbench.TaskSummary;
public class EdhrVoidWorkbenchRow extends TaskSummary {
    private String processInstanceId;
    public String getProcessInstanceId(){return processInstanceId;}
    public void setProcessInstanceId(String value){processInstanceId=value;}
}
