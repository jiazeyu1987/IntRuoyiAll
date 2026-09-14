package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class EdhrBatchExecutionSpecialNodeCompleteReqVO {

    @NotNull(message = "任务 ID 不能为空")
    private Long taskId;

    private Integer expectedVersion;

    private String idempotencyKey;

    private String sterilizationBatchNo;

    private List<EdhrBatchExecutionSpecialNodeAttachmentVO> attachments;
}
