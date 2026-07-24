package cn.iocoder.yudao.module.srm.service.paymentexecution;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.srm.controller.admin.paymentexecution.vo.*;

public interface SrmPaymentExecutionService {

    Long createFromReconciliation(SrmPaymentExecutionCreateReqVO reqVO);

    SrmPaymentExecutionRespVO getPaymentExecution(Long id);

    PageResult<SrmPaymentExecutionRespVO> getPaymentExecutionPage(SrmPaymentExecutionPageReqVO reqVO);

    void submit(SrmPaymentExecutionSubmitReqVO reqVO);

    void approve(SrmPaymentExecutionApproveReqVO reqVO);

    void reject(SrmPaymentExecutionRejectReqVO reqVO);

    void financePush(SrmPaymentExecutionRejectReqVO reqVO);
}
