package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

/** Read-only summary of the review and its original-cycle formal QA task. */
@Data
@Accessors(chain = true)
public class MesProEdhrNonconformanceReviewSummaryRespVO {
    private MesProEdhrNonconformanceReviewRespVO review;
    private String qaReviewerName;
    private String qaUserName;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long qaTaskId;
    private String qaTaskStatus;
}
