package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionRejectReqVO {

    @NotNull(message = "执行记录不能为空")
    private Long executionId;

    @NotNull(message = "workTaskId 不能为空")
    private Long workTaskId;

    @NotEmpty(message = "BPM 流程实例不能为空")
    private String processInstanceId;

    @NotNull(message = "审批快照不能为空")
    private Long approvalSnapshotId;

    @NotEmpty(message = "审批快照哈希不能为空")
    private String approvalSnapshotHash;

    @NotEmpty(message = "BPM 任务不能为空")
    private String bpmTaskId;

    @NotEmpty(message = "签名密码不能为空")
    private String password;

    @NotEmpty(message = "驳回原因不能为空")
    private String reason;

    @Valid
    private MesProBatchRecordExecutionSignatureTimeReqVO signatureTime;
}
