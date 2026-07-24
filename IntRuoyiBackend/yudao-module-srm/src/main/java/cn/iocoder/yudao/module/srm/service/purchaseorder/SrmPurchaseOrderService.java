package cn.iocoder.yudao.module.srm.service.purchaseorder;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.srm.controller.admin.purchaseorder.vo.SrmPurchaseOrderChangeReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.purchaseorder.vo.SrmPurchaseOrderConfirmReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.purchaseorder.vo.SrmPurchaseOrderCreateReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.purchaseorder.vo.SrmPurchaseOrderPageReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.purchaseorder.vo.SrmPurchaseOrderRejectChangeReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.purchaseorder.vo.SrmPurchaseOrderRespVO;
import cn.iocoder.yudao.module.srm.controller.admin.purchaseorder.vo.SrmPurchaseOrderWithdrawChangeReqVO;
import jakarta.validation.Valid;

public interface SrmPurchaseOrderService {

    Long createFromPlan(@Valid SrmPurchaseOrderCreateReqVO createReqVO);

    SrmPurchaseOrderRespVO getPurchaseOrder(Long id);

    PageResult<SrmPurchaseOrderRespVO> getPurchaseOrderPage(SrmPurchaseOrderPageReqVO pageReqVO);

    PageResult<SrmPurchaseOrderRespVO> getMyPurchaseOrderPage(SrmPurchaseOrderPageReqVO pageReqVO);

    SrmPurchaseOrderRespVO getMyPurchaseOrder(Long id);

    void confirmMyPurchaseOrder(@Valid SrmPurchaseOrderConfirmReqVO reqVO);

    Long submitOrderChange(@Valid SrmPurchaseOrderChangeReqVO reqVO);

    void rejectMyPurchaseOrderChange(@Valid SrmPurchaseOrderRejectChangeReqVO reqVO);

    void withdrawOrderChange(@Valid SrmPurchaseOrderWithdrawChangeReqVO reqVO);
}
