package cn.iocoder.yudao.module.srm.controller.admin.purchaseorder;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.srm.controller.admin.purchaseorder.vo.SrmPurchaseOrderChangeReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.purchaseorder.vo.SrmPurchaseOrderConfirmReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.purchaseorder.vo.SrmPurchaseOrderCreateReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.purchaseorder.vo.SrmPurchaseOrderPageReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.purchaseorder.vo.SrmPurchaseOrderRejectChangeReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.purchaseorder.vo.SrmPurchaseOrderRespVO;
import cn.iocoder.yudao.module.srm.controller.admin.purchaseorder.vo.SrmPurchaseOrderWithdrawChangeReqVO;
import cn.iocoder.yudao.module.srm.service.purchaseorder.SrmPurchaseOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - SRM 采购订单协同")
@RestController
@RequestMapping("/srm/purchase-order")
@Validated
public class SrmPurchaseOrderController {

    @Resource
    private SrmPurchaseOrderService purchaseOrderService;

    @PostMapping("/create-from-plan")
    @Operation(summary = "从采购计划生成采购订单协同单")
    @PreAuthorize("@ss.hasPermission('srm:purchase-order:create')")
    public CommonResult<Long> createFromPlan(@Valid @RequestBody SrmPurchaseOrderCreateReqVO reqVO) {
        return success(purchaseOrderService.createFromPlan(reqVO));
    }

    @GetMapping("/page")
    @Operation(summary = "获得采购订单协同分页")
    @PreAuthorize("@ss.hasPermission('srm:purchase-order:query')")
    public CommonResult<PageResult<SrmPurchaseOrderRespVO>> getPurchaseOrderPage(@Valid @ParameterObject SrmPurchaseOrderPageReqVO pageReqVO) {
        return success(purchaseOrderService.getPurchaseOrderPage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得采购订单协同详情")
    @Parameter(name = "id", description = "采购订单协同单编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('srm:purchase-order:query')")
    public CommonResult<SrmPurchaseOrderRespVO> getPurchaseOrder(@RequestParam("id") Long id) {
        return success(purchaseOrderService.getPurchaseOrder(id));
    }

    @PostMapping("/change")
    @Operation(summary = "提交采购订单变更申请")
    @PreAuthorize("@ss.hasPermission('srm:purchase-order:create')")
    public CommonResult<Long> submitOrderChange(@Valid @RequestBody SrmPurchaseOrderChangeReqVO reqVO) {
        return success(purchaseOrderService.submitOrderChange(reqVO));
    }

    @PutMapping("/change/withdraw")
    @Operation(summary = "采购侧撤回采购订单变更申请")
    @PreAuthorize("@ss.hasPermission('srm:purchase-order:create')")
    public CommonResult<Boolean> withdrawOrderChange(@Valid @RequestBody SrmPurchaseOrderWithdrawChangeReqVO reqVO) {
        purchaseOrderService.withdrawOrderChange(reqVO);
        return success(true);
    }

    @GetMapping("/my/page")
    @Operation(summary = "获得当前供应商的采购订单协同分页")
    public CommonResult<PageResult<SrmPurchaseOrderRespVO>> getMyPurchaseOrderPage(@Valid @ParameterObject SrmPurchaseOrderPageReqVO pageReqVO) {
        return success(purchaseOrderService.getMyPurchaseOrderPage(pageReqVO));
    }

    @GetMapping("/my/get")
    @Operation(summary = "获得当前供应商的采购订单协同详情")
    @Parameter(name = "id", description = "采购订单协同单编号", required = true, example = "1")
    public CommonResult<SrmPurchaseOrderRespVO> getMyPurchaseOrder(@RequestParam("id") Long id) {
        return success(purchaseOrderService.getMyPurchaseOrder(id));
    }

    @PutMapping("/confirm-my")
    @Operation(summary = "当前供应商确认采购订单协同单")
    public CommonResult<Boolean> confirmMyPurchaseOrder(@Valid @RequestBody SrmPurchaseOrderConfirmReqVO reqVO) {
        purchaseOrderService.confirmMyPurchaseOrder(reqVO);
        return success(true);
    }

    @PutMapping("/change/reject-my")
    @Operation(summary = "当前供应商拒绝采购订单变更")
    public CommonResult<Boolean> rejectMyPurchaseOrderChange(@Valid @RequestBody SrmPurchaseOrderRejectChangeReqVO reqVO) {
        purchaseOrderService.rejectMyPurchaseOrderChange(reqVO);
        return success(true);
    }
}
