package cn.iocoder.yudao.module.srm.controller.admin.supplierrisk;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.srm.controller.admin.supplierrisk.vo.*;
import cn.iocoder.yudao.module.srm.service.supplier.SrmSupplierAccessRiskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - SRM 供应商风险")
@RestController
@RequestMapping("/srm/supplier-risk")
@Validated
public class SrmSupplierRiskController {

    @Resource
    private SrmSupplierAccessRiskService supplierAccessRiskService;

    @PostMapping("/create")
    @Operation(summary = "创建 SRM 供应商风险记录")
    @PreAuthorize("@ss.hasPermission('srm:supplier-risk:create')")
    public CommonResult<Long> createSupplierRisk(@Valid @RequestBody SrmSupplierRiskSaveReqVO createReqVO) {
        return success(supplierAccessRiskService.createSupplierRisk(createReqVO));
    }

    @PutMapping("/resolve")
    @Operation(summary = "处理 SRM 供应商风险记录")
    @PreAuthorize("@ss.hasPermission('srm:supplier-risk:resolve')")
    public CommonResult<Boolean> resolveSupplierRisk(@Valid @RequestBody SrmSupplierRiskResolveReqVO resolveReqVO) {
        supplierAccessRiskService.resolveSupplierRisk(resolveReqVO);
        return success(true);
    }

    @GetMapping("/page")
    @Operation(summary = "获得 SRM 供应商风险分页")
    @PreAuthorize("@ss.hasPermission('srm:supplier-risk:query')")
    public CommonResult<PageResult<SrmSupplierRiskRespVO>> getSupplierRiskPage(@Valid SrmSupplierRiskPageReqVO pageReqVO) {
        return success(supplierAccessRiskService.getSupplierRiskPage(pageReqVO));
    }
}
