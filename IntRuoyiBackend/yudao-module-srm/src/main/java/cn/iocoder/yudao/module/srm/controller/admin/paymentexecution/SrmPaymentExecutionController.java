package cn.iocoder.yudao.module.srm.controller.admin.paymentexecution;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.srm.controller.admin.paymentexecution.vo.*;
import cn.iocoder.yudao.module.srm.service.paymentexecution.SrmPaymentExecutionService;
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

@Tag(name = "管理后台 - SRM 付款执行")
@RestController
@RequestMapping("/srm/payment-execution")
@Validated
public class SrmPaymentExecutionController {

    @Resource
    private SrmPaymentExecutionService paymentExecutionService;

    @PostMapping("/create-from-reconciliation")
    @Operation(summary = "从委外对账单创建付款执行单")
    @PreAuthorize("@ss.hasPermission('srm:payment-execution:create')")
    public CommonResult<Long> createFromReconciliation(@Valid @RequestBody SrmPaymentExecutionCreateReqVO reqVO) {
        return success(paymentExecutionService.createFromReconciliation(reqVO));
    }

    @GetMapping("/page")
    @Operation(summary = "获得付款执行分页")
    @PreAuthorize("@ss.hasPermission('srm:payment-execution:query')")
    public CommonResult<PageResult<SrmPaymentExecutionRespVO>> getPaymentExecutionPage(
            @Valid @ParameterObject SrmPaymentExecutionPageReqVO reqVO) {
        return success(paymentExecutionService.getPaymentExecutionPage(reqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得付款执行详情")
    @Parameter(name = "id", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('srm:payment-execution:query')")
    public CommonResult<SrmPaymentExecutionRespVO> getPaymentExecution(@RequestParam("id") Long id) {
        return success(paymentExecutionService.getPaymentExecution(id));
    }

    @PutMapping("/submit")
    @Operation(summary = "提交付款申请")
    @PreAuthorize("@ss.hasPermission('srm:payment-execution:create')")
    public CommonResult<Boolean> submit(@Valid @RequestBody SrmPaymentExecutionSubmitReqVO reqVO) {
        paymentExecutionService.submit(reqVO);
        return success(true);
    }

    @PutMapping("/approve")
    @Operation(summary = "审批付款申请")
    @PreAuthorize("@ss.hasPermission('srm:payment-execution:approve')")
    public CommonResult<Boolean> approve(@Valid @RequestBody SrmPaymentExecutionApproveReqVO reqVO) {
        paymentExecutionService.approve(reqVO);
        return success(true);
    }

    @PutMapping("/reject")
    @Operation(summary = "驳回付款申请")
    @PreAuthorize("@ss.hasPermission('srm:payment-execution:approve')")
    public CommonResult<Boolean> reject(@Valid @RequestBody SrmPaymentExecutionRejectReqVO reqVO) {
        paymentExecutionService.reject(reqVO);
        return success(true);
    }

    @PutMapping("/finance-push")
    @Operation(summary = "记录财务推送状态")
    @PreAuthorize("@ss.hasPermission('srm:payment-execution:approve')")
    public CommonResult<Boolean> financePush(@Valid @RequestBody SrmPaymentExecutionRejectReqVO reqVO) {
        paymentExecutionService.financePush(reqVO);
        return success(true);
    }
}
