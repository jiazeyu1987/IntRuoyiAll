package cn.iocoder.yudao.module.srm.controller.admin.nonbidding.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SrmNonBiddingPublishReqVO {

    @NotNull(message = "非招标项目编号不能为空")
    private Long projectId;

    @NotNull(message = "报价开始时间不能为空")
    private LocalDateTime quoteStartTime;

    @NotNull(message = "报价截止时间不能为空")
    private LocalDateTime quoteEndTime;

    @NotNull(message = "询价模式不能为空")
    private String quoteMode;

    private String attachmentUrl;

    private List<Long> supplierIds;
}
