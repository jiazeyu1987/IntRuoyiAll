package cn.iocoder.yudao.module.srm.controller.admin.nonbidding.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SrmNonBiddingQuoteReqVO {

    @NotNull(message = "非招标项目编号不能为空")
    private Long projectId;

    @NotNull(message = "供应商编号不能为空")
    private Long supplierId;

    @NotNull(message = "报价金额不能为空")
    private BigDecimal quoteAmount;

    private String attachmentUrl;

    @Valid
    private List<Line> lines;

    @Data
    public static class Line {

        @NotNull(message = "项目行编号不能为空")
        private Long projectLineId;

        @NotNull(message = "报价单价不能为空")
        private BigDecimal unitPrice;

        @NotNull(message = "报价行金额不能为空")
        private BigDecimal lineAmount;
    }
}
