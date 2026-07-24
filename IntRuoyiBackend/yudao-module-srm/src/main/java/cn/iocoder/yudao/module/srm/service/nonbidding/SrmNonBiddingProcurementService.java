package cn.iocoder.yudao.module.srm.service.nonbidding;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.srm.controller.admin.nonbidding.vo.*;
import jakarta.validation.Valid;

public interface SrmNonBiddingProcurementService {

    SrmNonBiddingProjectRespVO publishProject(@Valid SrmNonBiddingPublishReqVO publishReqVO);

    SrmNonBiddingProjectRespVO submitQuote(@Valid SrmNonBiddingQuoteReqVO quoteReqVO);

    SrmNonBiddingProjectRespVO confirmDeal(@Valid SrmNonBiddingDealReqVO dealReqVO);

    SrmNonBiddingProjectRespVO getProject(Long id);

    PageResult<SrmNonBiddingProjectRespVO> getProjectPage(SrmNonBiddingProjectPageReqVO pageReqVO);

    PageResult<SrmNonBiddingProjectRespVO> getContractableProjectPage(SrmNonBiddingProjectPageReqVO pageReqVO);
}
