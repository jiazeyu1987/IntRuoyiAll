package cn.iocoder.yudao.module.bpm.approval.service;

import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskReviewResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ApprovalTaskReviewContext {

    private Long loginUserId;

    private ApprovalModuleCode moduleCode;

    private String sourceTaskType;

    private String sourceTaskId;

    private String businessKey;

    private String processInstanceId;

    private ApprovalTaskReviewResult result;

    private String reason;

    private String signaturePassword;

    private String signatureImageFileUrl;

    private boolean globalView;

    public static ApprovalTaskReviewContext of(Long loginUserId, ApprovalModuleCode moduleCode,
                                               String sourceTaskType, String sourceTaskId,
                                               String businessKey, String processInstanceId,
                                               ApprovalTaskReviewResult result, String reason,
                                               String signaturePassword, boolean globalView) {
        return new ApprovalTaskReviewContext()
                .setLoginUserId(loginUserId)
                .setModuleCode(moduleCode)
                .setSourceTaskType(sourceTaskType)
                .setSourceTaskId(sourceTaskId)
                .setBusinessKey(businessKey)
                .setProcessInstanceId(processInstanceId)
                .setResult(result)
                .setReason(reason)
                .setSignaturePassword(signaturePassword)
                .setGlobalView(globalView);
    }
}
