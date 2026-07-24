package cn.iocoder.yudao.module.srm.controller.admin.supplierportal;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.srm.controller.admin.supplierportal.vo.*;
import cn.iocoder.yudao.module.srm.service.supplier.SrmSupplierPortalApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - SRM 供应商门户申请")
@RestController
@RequestMapping("/srm/supplier-portal")
@Validated
public class SrmSupplierPortalApplicationController {

    @Resource
    private SrmSupplierPortalApplicationService supplierPortalApplicationService;

    @GetMapping("/my")
    @Operation(summary = "获取当前登录人的供应商门户申请")
    public CommonResult<SrmSupplierPortalApplicationRespVO> getMyApplication() {
        return success(supplierPortalApplicationService.getCurrentApplication());
    }

    @PostMapping("/save-draft")
    @Operation(summary = "保存供应商门户申请草稿")
    public CommonResult<SrmSupplierPortalApplicationRespVO> saveDraft(@Valid @RequestBody SrmSupplierPortalApplicationSaveReqVO reqVO) {
        return success(supplierPortalApplicationService.saveDraft(reqVO));
    }

    @PostMapping("/submit")
    @Operation(summary = "提交供应商门户申请")
    public CommonResult<SrmSupplierPortalApplicationRespVO> submit(@Valid @RequestBody SrmSupplierPortalApplicationSaveReqVO reqVO) {
        return success(supplierPortalApplicationService.submit(reqVO));
    }

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('srm:supplier-portal:review')")
    @Operation(summary = "分页查询供应商门户申请")
    public CommonResult<PageResult<SrmSupplierPortalApplicationRespVO>> getApplicationPage(@Valid SrmSupplierPortalApplicationPageReqVO pageReqVO) {
        return success(supplierPortalApplicationService.getApplicationPage(pageReqVO));
    }

    @PutMapping("/approve")
    @PreAuthorize("@ss.hasPermission('srm:supplier-portal:audit')")
    @Operation(summary = "审核通过供应商门户申请")
    public CommonResult<Boolean> approve(@Valid @RequestBody SrmSupplierPortalApplicationAuditReqVO reqVO) {
        supplierPortalApplicationService.approve(reqVO);
        return success(true);
    }

    @PutMapping("/reject")
    @PreAuthorize("@ss.hasPermission('srm:supplier-portal:audit')")
    @Operation(summary = "审核驳回供应商门户申请")
    public CommonResult<Boolean> reject(@Valid @RequestBody SrmSupplierPortalApplicationAuditReqVO reqVO) {
        supplierPortalApplicationService.reject(reqVO);
        return success(true);
    }
}
