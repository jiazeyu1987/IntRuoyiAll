package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrValidationPackageCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrValidationPackagePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrValidationPackageRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrValidationTraceEvaluateRespVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrValidationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - MES eDHR 验证包矩阵")
@RestController
@RequestMapping("/mes/pro/edhr-validation-package")
@Validated
public class MesProEdhrValidationPackageController {

    @Resource
    private MesProEdhrValidationService validationService;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-validation:query')")
    public CommonResult<PageResult<MesProEdhrValidationPackageRespVO>> getPackagePage(
            @Valid MesProEdhrValidationPackagePageReqVO reqVO) {
        return success(validationService.getPackagePage(reqVO));
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-validation:create')")
    public CommonResult<MesProEdhrValidationPackageRespVO> createPackage(
            @Valid @RequestBody MesProEdhrValidationPackageCreateReqVO reqVO) {
        return success(validationService.createPackage(reqVO));
    }

    @GetMapping("/detail")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-validation:query')")
    public CommonResult<MesProEdhrValidationPackageRespVO> getPackageDetail(@RequestParam("id") Long id) {
        return success(validationService.getPackageDetail(id));
    }

    @PostMapping("/evaluate-trace")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-validation:evaluate-trace')")
    public CommonResult<MesProEdhrValidationTraceEvaluateRespVO> evaluateTrace(@RequestParam("packageId") Long packageId) {
        return success(validationService.evaluateTrace(packageId));
    }
}
