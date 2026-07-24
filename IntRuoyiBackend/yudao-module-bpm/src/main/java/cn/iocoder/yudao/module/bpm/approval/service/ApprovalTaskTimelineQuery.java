package cn.iocoder.yudao.module.bpm.approval.service;

import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ApprovalTaskTimelineQuery {

    private ApprovalModuleCode moduleCode;

    private String sourceTaskType;

    private String sourceTaskId;

    private String businessKey;

    private String processInstanceId;
}
