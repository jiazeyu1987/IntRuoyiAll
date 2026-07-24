package cn.iocoder.yudao.module.srm.controller.admin.contract;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.srm.controller.admin.contract.vo.SrmProcurementContractCancelReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.contract.vo.SrmProcurementContractPageReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.contract.vo.SrmProcurementContractRespVO;
import cn.iocoder.yudao.module.srm.controller.admin.contract.vo.SrmProcurementContractSaveReqVO;
import cn.iocoder.yudao.module.srm.service.contract.SrmProcurementContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - SRM 采购合同")
@RestController
@RequestMapping("/srm/procurement-contract")
@Validated
public class SrmProcurementContractController {

    @Resource
    private SrmProcurementContractService procurementContractService;

    @GetMapping("/page")
    @Operation(summary = "获得采购合同分页")
    @PreAuthorize("@ss.hasPermission('srm:procurement-contract:query')")
    public CommonResult<PageResult<SrmProcurementContractRespVO>> getContractPage(@Valid SrmProcurementContractPageReqVO pageReqVO) {
        return success(procurementContractService.getContractPage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得采购合同详情")
    @Parameter(name = "id", description = "采购合同编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('srm:procurement-contract:query')")
    public CommonResult<SrmProcurementContractRespVO> getContract(@RequestParam("id") Long id) {
        return success(procurementContractService.getContract(id));
    }

    @PostMapping("/create")
    @Operation(summary = "创建采购合同")
    @PreAuthorize("@ss.hasPermission('srm:procurement-contract:create')")
    public CommonResult<SrmProcurementContractRespVO> createContract(@Valid @RequestBody SrmProcurementContractSaveReqVO createReqVO) {
        return success(procurementContractService.createContract(createReqVO));
    }

    @PutMapping("/cancel")
    @Operation(summary = "作废采购合同")
    @PreAuthorize("@ss.hasPermission('srm:procurement-contract:cancel')")
    public CommonResult<Boolean> cancelContract(@Valid @RequestBody SrmProcurementContractCancelReqVO cancelReqVO) {
        procurementContractService.cancelContract(cancelReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除采购合同")
    @Parameter(name = "id", description = "采购合同编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('srm:procurement-contract:delete')")
    public CommonResult<Boolean> deleteContract(@RequestParam("id") Long id) {
        procurementContractService.deleteContract(id);
        return success(true);
    }
}
