package cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task;

import lombok.Data;
import java.util.Date;

@Data
public class BpmSupervisorHandoverRespVO {
    private String id;
    private String name;
    private String processInstanceId;
    private String assigneeUserId;
    private String assigneeName;
    private boolean assigneeAvailable;
    private Date createTime;
    private boolean transferable;
    private String blocker;
}
