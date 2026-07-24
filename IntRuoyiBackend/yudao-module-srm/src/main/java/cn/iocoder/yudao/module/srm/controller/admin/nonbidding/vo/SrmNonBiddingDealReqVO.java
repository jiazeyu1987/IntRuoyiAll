package cn.iocoder.yudao.module.srm.controller.admin.nonbidding.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SrmNonBiddingDealReqVO {

    @NotNull(message = "非招标项目编号不能为空")
    private Long projectId;

    @NotNull(message = "成交报价编号不能为空")
    private Long quoteId;

    private String dealRemark;
}
