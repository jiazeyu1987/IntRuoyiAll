package cn.iocoder.yudao.module.srm.service.outsourceexecution;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.srm.controller.admin.outsourceexecution.vo.*;

public interface SrmOutsourceExecutionService {

    Long createFromPurchaseOrder(SrmOutsourceExecutionCreateReqVO reqVO);

    SrmOutsourceExecutionRespVO getOutsourceExecution(Long id);

    SrmOutsourceExecutionRespVO getMyOutsourceExecution(Long id);

    PageResult<SrmOutsourceExecutionRespVO> getOutsourceExecutionPage(SrmOutsourceExecutionPageReqVO reqVO);

    PageResult<SrmOutsourceExecutionRespVO> getMyOutsourceExecutionPage(SrmOutsourceExecutionPageReqVO reqVO);

    void issue(SrmOutsourceExecutionIssueReqVO reqVO);

    void updateProgress(SrmOutsourceExecutionProgressReqVO reqVO);

    void receive(SrmOutsourceExecutionReceiveReqVO reqVO);

    void inspect(SrmOutsourceExecutionInspectReqVO reqVO);

    void reconcile(SrmOutsourceExecutionReconcileReqVO reqVO);
}
