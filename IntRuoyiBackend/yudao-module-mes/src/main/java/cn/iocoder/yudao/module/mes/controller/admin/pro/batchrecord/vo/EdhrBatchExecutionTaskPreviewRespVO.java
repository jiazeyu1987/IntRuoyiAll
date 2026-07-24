package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EdhrBatchExecutionTaskPreviewRespVO {

    private Long batchExecutionId;

    private Long taskId;

    private Long executionId;

    private Integer taskStatus;

    private Boolean executionCreated;

    private EdhrBatchExecutionReviewTimelineRespVO.FormViewModel formViewModel;
}
