package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDhrCatalogCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDhrCatalogPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDhrCatalogRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDhrTemplateCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDhrTemplateImpactPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDhrTemplateImpactReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDhrTemplateImpactRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDhrTemplateLifecycleReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDhrTemplatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDhrTemplateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDhrTemplateSignoffReqVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrDhrTemplateService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - MES eDHR DHR模板")
@RestController
@RequestMapping("/mes/pro/edhr-dhr-template")
@Validated
public class MesProEdhrDhrTemplateController {

    @Resource
    private MesProEdhrDhrTemplateService dhrTemplateService;

    @GetMapping("/catalog/page")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-dhr-template:query')")
    public CommonResult<PageResult<MesProEdhrDhrCatalogRespVO>> getCatalogPage(
            @Valid MesProEdhrDhrCatalogPageReqVO reqVO) {
        return success(dhrTemplateService.getCatalogPage(reqVO));
    }

    @PostMapping("/catalog/create")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-dhr-template:create')")
    public CommonResult<MesProEdhrDhrCatalogRespVO> createCatalog(
            @Valid @RequestBody MesProEdhrDhrCatalogCreateReqVO reqVO) {
        return success(dhrTemplateService.createCatalog(reqVO));
    }

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-dhr-template:query')")
    public CommonResult<PageResult<MesProEdhrDhrTemplateRespVO>> getPage(
            @Valid MesProEdhrDhrTemplatePageReqVO reqVO) {
        return success(dhrTemplateService.getTemplatePage(reqVO));
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-dhr-template:create')")
    public CommonResult<MesProEdhrDhrTemplateRespVO> create(
            @Valid @RequestBody MesProEdhrDhrTemplateCreateReqVO reqVO) {
        return success(dhrTemplateService.createTemplate(reqVO));
    }

    @PostMapping("/integrity-check")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-dhr-template:check')")
    public CommonResult<MesProEdhrDhrTemplateRespVO> runIntegrityCheck(
            @Valid @RequestBody MesProEdhrDhrTemplateLifecycleReqVO reqVO) {
        return success(dhrTemplateService.runIntegrityCheck(reqVO));
    }

    @PostMapping("/approve")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-dhr-template:approve')")
    public CommonResult<MesProEdhrDhrTemplateRespVO> approve(
            @Valid @RequestBody MesProEdhrDhrTemplateLifecycleReqVO reqVO) {
        return success(dhrTemplateService.approveTemplate(reqVO));
    }

    @PostMapping("/signoff")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-dhr-template:signoff')")
    public CommonResult<MesProEdhrDhrTemplateRespVO> signoff(
            @Valid @RequestBody MesProEdhrDhrTemplateSignoffReqVO reqVO) {
        return success(dhrTemplateService.signoffTemplate(reqVO));
    }

    @PostMapping("/activate")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-dhr-template:activate')")
    public CommonResult<MesProEdhrDhrTemplateRespVO> activate(
            @Valid @RequestBody MesProEdhrDhrTemplateLifecycleReqVO reqVO) {
        return success(dhrTemplateService.activateTemplate(reqVO));
    }

    @PostMapping("/retire")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-dhr-template:retire')")
    public CommonResult<MesProEdhrDhrTemplateRespVO> retire(
            @Valid @RequestBody MesProEdhrDhrTemplateImpactReqVO reqVO) {
        return success(dhrTemplateService.retireTemplate(reqVO));
    }

    @PostMapping("/void")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-dhr-template:void')")
    public CommonResult<MesProEdhrDhrTemplateRespVO> voidTemplate(
            @Valid @RequestBody MesProEdhrDhrTemplateImpactReqVO reqVO) {
        return success(dhrTemplateService.voidTemplate(reqVO));
    }

    @GetMapping("/impact/page")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-dhr-template:query')")
    public CommonResult<PageResult<MesProEdhrDhrTemplateImpactRespVO>> getImpactPage(
            @Valid MesProEdhrDhrTemplateImpactPageReqVO reqVO) {
        return success(dhrTemplateService.getImpactPage(reqVO));
    }
}
