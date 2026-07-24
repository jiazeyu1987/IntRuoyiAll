package cn.iocoder.yudao.module.srm.controller.admin.supplieraccess;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.srm.controller.admin.supplieraccess.vo.*;
import cn.iocoder.yudao.module.srm.service.supplier.SrmSupplierAccessRiskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - SRM 供应商准入")
@RestController
@RequestMapping("/srm/supplier-access")
@Validated
public class SrmSupplierAccessController {

    @Resource
    private SrmSupplierAccessRiskService supplierAccessRiskService;

    @PostMapping("/create")
    @Operation(summary = "创建 SRM 供应商准入档案")
    @PreAuthorize("@ss.hasPermission('srm:supplier-access:create')")
    public CommonResult<Long> createSupplierAccess(@Valid @RequestBody SrmSupplierAccessSaveReqVO createReqVO) {
        return success(supplierAccessRiskService.createSupplierAccess(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新 SRM 供应商准入档案")
    @PreAuthorize("@ss.hasPermission('srm:supplier-access:update')")
    public CommonResult<Boolean> updateSupplierAccess(@Valid @RequestBody SrmSupplierAccessSaveReqVO updateReqVO) {
        supplierAccessRiskService.updateSupplierAccess(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除 SRM 供应商准入档案")
    @Parameter(name = "id", description = "准入档案编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('srm:supplier-access:delete')")
    public CommonResult<Boolean> deleteSupplierAccess(@RequestParam("id") Long id) {
        supplierAccessRiskService.deleteSupplierAccess(id);
        return success(true);
    }

    @PutMapping("/approve")
    @Operation(summary = "通过 SRM 供应商准入")
    @PreAuthorize("@ss.hasPermission('srm:supplier-access:audit')")
    public CommonResult<Boolean> approveSupplierAccess(@Valid @RequestBody SrmSupplierAccessAuditReqVO auditReqVO) {
        supplierAccessRiskService.approveSupplierAccess(auditReqVO);
        return success(true);
    }

    @PutMapping("/reject")
    @Operation(summary = "驳回 SRM 供应商准入")
    @PreAuthorize("@ss.hasPermission('srm:supplier-access:audit')")
    public CommonResult<Boolean> rejectSupplierAccess(@Valid @RequestBody SrmSupplierAccessAuditReqVO auditReqVO) {
        supplierAccessRiskService.rejectSupplierAccess(auditReqVO);
        return success(true);
    }

    @PutMapping("/sample/approve")
    @Operation(summary = "通过供应商样品测试")
    @PreAuthorize("@ss.hasPermission('srm:supplier-access:audit')")
    public CommonResult<Boolean> approveSampleTest(@Valid @RequestBody SrmSupplierAccessAuditReqVO auditReqVO) {
        supplierAccessRiskService.approveSampleTest(auditReqVO);
        return success(true);
    }

    @PutMapping("/sample/reject")
    @Operation(summary = "驳回供应商样品测试")
    @PreAuthorize("@ss.hasPermission('srm:supplier-access:audit')")
    public CommonResult<Boolean> rejectSampleTest(@Valid @RequestBody SrmSupplierAccessAuditReqVO auditReqVO) {
        supplierAccessRiskService.rejectSampleTest(auditReqVO);
        return success(true);
    }

    @PutMapping("/trial/approve")
    @Operation(summary = "通过供应商小批试用")
    @PreAuthorize("@ss.hasPermission('srm:supplier-access:audit')")
    public CommonResult<Boolean> approveTrialOrder(@Valid @RequestBody SrmSupplierAccessAuditReqVO auditReqVO) {
        supplierAccessRiskService.approveTrialOrder(auditReqVO);
        return success(true);
    }

    @PutMapping("/trial/reject")
    @Operation(summary = "驳回供应商小批试用")
    @PreAuthorize("@ss.hasPermission('srm:supplier-access:audit')")
    public CommonResult<Boolean> rejectTrialOrder(@Valid @RequestBody SrmSupplierAccessAuditReqVO auditReqVO) {
        supplierAccessRiskService.rejectTrialOrder(auditReqVO);
        return success(true);
    }

    @PutMapping("/enable")
    @Operation(summary = "启停 SRM 供应商准入档案")
    @PreAuthorize("@ss.hasPermission('srm:supplier-access:enable')")
    public CommonResult<Boolean> enableSupplierAccess(@Valid @RequestBody SrmSupplierAccessEnableReqVO enableReqVO) {
        supplierAccessRiskService.enableSupplierAccess(enableReqVO);
        return success(true);
    }

    @GetMapping("/page")
    @Operation(summary = "获得 SRM 供应商准入分页")
    @PreAuthorize("@ss.hasPermission('srm:supplier-access:query')")
    public CommonResult<PageResult<SrmSupplierAccessRespVO>> getSupplierAccessPage(@Valid SrmSupplierAccessPageReqVO pageReqVO) {
        return success(supplierAccessRiskService.getSupplierAccessPage(pageReqVO));
    }

    @GetMapping("/profile")
    @Operation(summary = "获得 SRM 供应商统一档案")
    @Parameter(name = "supplierId", description = "ERP 供应商编号", required = true, example = "2")
    @PreAuthorize("@ss.hasPermission('srm:supplier-profile:query')")
    public CommonResult<SrmSupplierProfileRespVO> getSupplierProfile(@RequestParam("supplierId") Long supplierId) {
        return success(supplierAccessRiskService.getSupplierProfile(supplierId));
    }

    @GetMapping("/check")
    @Operation(summary = "执行 SRM 供应商资格校验")
    @Parameter(name = "supplierId", description = "ERP 供应商编号", required = true, example = "2")
    @PreAuthorize("@ss.hasPermission('srm:supplier-access:check')")
    public CommonResult<SrmSupplierEligibilityRespVO> checkSupplierEligibility(@RequestParam("supplierId") Long supplierId) {
        return success(supplierAccessRiskService.checkSupplierEligibility(supplierId));
    }

    @GetMapping("/reference-suppliers")
    @Operation(summary = "获得 SRM 可引用 ERP 供应商列表")
    @PreAuthorize("@ss.hasPermission('srm:supplier-access:query')")
    public CommonResult<List<SrmSupplierReferenceRespVO>> getReferenceSuppliers(@RequestParam(value = "keyword", required = false) String keyword) {
        return success(supplierAccessRiskService.getReferenceSupplierList(keyword));
    }
}
