package cn.iocoder.yudao.module.srm.controller.admin.framework;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.srm.controller.admin.framework.vo.*;
import cn.iocoder.yudao.module.srm.service.framework.SrmFrameworkAgreementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - SRM 框架计划")
@RestController
@RequestMapping("/srm/framework-plan")
@Validated
public class SrmFrameworkPlanController {

    @Resource
    private SrmFrameworkAgreementService frameworkAgreementService;

    @PostMapping("/create")
    @Operation(summary = "创建框架计划")
    @PreAuthorize("@ss.hasPermission('srm:framework-plan:create')")
    public CommonResult<Long> createFrameworkPlan(@Valid @RequestBody SrmFrameworkPlanSaveReqVO createReqVO) {
        return success(frameworkAgreementService.createFrameworkPlan(createReqVO));
    }

    @PutMapping("/submit")
    @Operation(summary = "提交框架计划")
    @PreAuthorize("@ss.hasPermission('srm:framework-plan:submit')")
    public CommonResult<Boolean> submitFrameworkPlan(@RequestParam("id") Long id) {
        frameworkAgreementService.submitFrameworkPlan(id);
        return success(true);
    }

    @PutMapping("/approve")
    @Operation(summary = "通过框架计划")
    @PreAuthorize("@ss.hasPermission('srm:framework-plan:audit')")
    public CommonResult<Boolean> approveFrameworkPlan(@Valid @RequestBody SrmFrameworkPlanAuditReqVO auditReqVO) {
        frameworkAgreementService.approveFrameworkPlan(auditReqVO);
        return success(true);
    }

    @PutMapping("/reject")
    @Operation(summary = "驳回框架计划")
    @PreAuthorize("@ss.hasPermission('srm:framework-plan:audit')")
    public CommonResult<Boolean> rejectFrameworkPlan(@Valid @RequestBody SrmFrameworkPlanAuditReqVO auditReqVO) {
        frameworkAgreementService.rejectFrameworkPlan(auditReqVO);
        return success(true);
    }

    @PostMapping("/create-agreement")
    @Operation(summary = "生成框架协议")
    @PreAuthorize("@ss.hasPermission('srm:framework-plan:agreement')")
    public CommonResult<SrmFrameworkAgreementRespVO> createAgreement(@RequestParam("id") Long id) {
        return success(frameworkAgreementService.createAgreement(id));
    }

    @GetMapping("/get")
    @Operation(summary = "获得框架计划详情")
    @Parameter(name = "id", description = "框架计划编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('srm:framework-plan:query')")
    public CommonResult<SrmFrameworkPlanRespVO> getFrameworkPlan(@RequestParam("id") Long id) {
        return success(frameworkAgreementService.getFrameworkPlan(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得框架计划分页")
    @PreAuthorize("@ss.hasPermission('srm:framework-plan:query')")
    public CommonResult<PageResult<SrmFrameworkPlanRespVO>> getFrameworkPlanPage(@Valid SrmFrameworkPlanPageReqVO pageReqVO) {
        return success(frameworkAgreementService.getFrameworkPlanPage(pageReqVO));
    }

    @GetMapping("/agreement-page")
    @Operation(summary = "获得框架协议分页")
    @PreAuthorize("@ss.hasPermission('srm:framework-plan:query')")
    public CommonResult<PageResult<SrmFrameworkAgreementRespVO>> getAgreementPage(@Valid SrmFrameworkAgreementPageReqVO pageReqVO) {
        return success(frameworkAgreementService.getAgreementPage(pageReqVO));
    }
}
