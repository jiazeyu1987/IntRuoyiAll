package cn.iocoder.yudao.module.bpm.approval.service;

import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ApprovalTaskTimelineQueryContext {

    private Long loginUserId;

    private ApprovalModuleCode moduleCode;

    private String sourceTaskType;

    private String sourceTaskId;

    private String businessKey;

    private String processInstanceId;

    private boolean globalView;

    public static ApprovalTaskTimelineQueryContext of(Long loginUserId, ApprovalModuleCode moduleCode,
                                                      String sourceTaskType, String sourceTaskId,
                                                      String businessKey, String processInstanceId) {
        return new ApprovalTaskTimelineQueryContext(loginUserId, moduleCode, sourceTaskType, sourceTaskId,
                businessKey, processInstanceId, false);
    }

    public static ApprovalTaskTimelineQueryContext of(Long loginUserId, ApprovalModuleCode moduleCode,
                                                      String sourceTaskType, String sourceTaskId,
                                                      String businessKey, String processInstanceId,
                                                      boolean globalView) {
        return new ApprovalTaskTimelineQueryContext(loginUserId, moduleCode, sourceTaskType, sourceTaskId,
                businessKey, processInstanceId, globalView);
    }
}
