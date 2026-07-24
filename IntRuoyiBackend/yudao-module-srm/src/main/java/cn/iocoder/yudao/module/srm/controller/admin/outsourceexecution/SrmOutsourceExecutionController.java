package cn.iocoder.yudao.module.srm.controller.admin.outsourceexecution;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.srm.controller.admin.outsourceexecution.vo.*;
import cn.iocoder.yudao.module.srm.service.outsourceexecution.SrmOutsourceExecutionService;
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

@Tag(name = "管理后台 - SRM 委外执行")
@RestController
@RequestMapping("/srm/outsource-execution")
@Validated
public class SrmOutsourceExecutionController {

    @Resource
    private SrmOutsourceExecutionService outsourceExecutionService;

    @PostMapping("/create-from-purchase-order")
    @Operation(summary = "从采购订单协同单创建委外执行单")
    @PreAuthorize("@ss.hasPermission('srm:outsource-execution:create')")
    public CommonResult<Long> createFromPurchaseOrder(@Valid @RequestBody SrmOutsourceExecutionCreateReqVO reqVO) {
        return success(outsourceExecutionService.createFromPurchaseOrder(reqVO));
    }

    @GetMapping("/page")
    @Operation(summary = "获得委外执行分页")
    @PreAuthorize("@ss.hasPermission('srm:outsource-execution:query')")
    public CommonResult<PageResult<SrmOutsourceExecutionRespVO>> getOutsourceExecutionPage(
            @Valid @ParameterObject SrmOutsourceExecutionPageReqVO reqVO) {
        return success(outsourceExecutionService.getOutsourceExecutionPage(reqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得委外执行详情")
    @Parameter(name = "id", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('srm:outsource-execution:query')")
    public CommonResult<SrmOutsourceExecutionRespVO> getOutsourceExecution(@RequestParam("id") Long id) {
        return success(outsourceExecutionService.getOutsourceExecution(id));
    }

    @GetMapping("/my/page")
    @Operation(summary = "获得当前供应商的委外执行分页")
    public CommonResult<PageResult<SrmOutsourceExecutionRespVO>> getMyOutsourceExecutionPage(
            @Valid @ParameterObject SrmOutsourceExecutionPageReqVO reqVO) {
        return success(outsourceExecutionService.getMyOutsourceExecutionPage(reqVO));
    }

    @GetMapping("/my/get")
    @Operation(summary = "获得当前供应商的委外执行详情")
    @Parameter(name = "id", required = true, example = "1")
    public CommonResult<SrmOutsourceExecutionRespVO> getMyOutsourceExecution(@RequestParam("id") Long id) {
        return success(outsourceExecutionService.getMyOutsourceExecution(id));
    }

    @PutMapping("/issue")
    @Operation(summary = "发起模拟发料通知")
    @PreAuthorize("@ss.hasPermission('srm:outsource-execution:update')")
    public CommonResult<Boolean> issue(@Valid @RequestBody SrmOutsourceExecutionIssueReqVO reqVO) {
        outsourceExecutionService.issue(reqVO);
        return success(true);
    }

    @PutMapping("/my/progress")
    @Operation(summary = "供应商回传加工进度")
    public CommonResult<Boolean> updateProgress(@Valid @RequestBody SrmOutsourceExecutionProgressReqVO reqVO) {
        outsourceExecutionService.updateProgress(reqVO);
        return success(true);
    }

    @PutMapping("/my/receive")
    @Operation(summary = "供应商回传送收货结果")
    public CommonResult<Boolean> receive(@Valid @RequestBody SrmOutsourceExecutionReceiveReqVO reqVO) {
        outsourceExecutionService.receive(reqVO);
        return success(true);
    }

    @PutMapping("/inspect")
    @Operation(summary = "登记来料检验结果")
    @PreAuthorize("@ss.hasPermission('srm:outsource-execution:update')")
    public CommonResult<Boolean> inspect(@Valid @RequestBody SrmOutsourceExecutionInspectReqVO reqVO) {
        outsourceExecutionService.inspect(reqVO);
        return success(true);
    }

    @PutMapping("/reconcile")
    @Operation(summary = "确认对账结果")
    @PreAuthorize("@ss.hasPermission('srm:outsource-execution:update')")
    public CommonResult<Boolean> reconcile(@Valid @RequestBody SrmOutsourceExecutionReconcileReqVO reqVO) {
        outsourceExecutionService.reconcile(reqVO);
        return success(true);
    }
}
