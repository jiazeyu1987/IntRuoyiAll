package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeliveryGateSummaryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeliveryProjectCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeliveryProjectPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeliveryProjectRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrEvidencePackagePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrEvidencePackageRespVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrDeliveryService;
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

@Tag(name = "管理后台 - MES eDHR 交付驾驶舱")
@RestController
@RequestMapping("/mes/pro/edhr-delivery-cockpit")
@Validated
public class MesProEdhrDeliveryCockpitController {

    @Resource
    private MesProEdhrDeliveryService deliveryService;

    @GetMapping("/project/page")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-delivery:query')")
    public CommonResult<PageResult<MesProEdhrDeliveryProjectRespVO>> getProjectPage(
            @Valid MesProEdhrDeliveryProjectPageReqVO reqVO) {
        return success(deliveryService.getProjectPage(reqVO));
    }

    @PostMapping("/project/create")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-delivery:create')")
    public CommonResult<MesProEdhrDeliveryProjectRespVO> createProject(
            @Valid @RequestBody MesProEdhrDeliveryProjectCreateReqVO reqVO) {
        return success(deliveryService.createProject(reqVO));
    }

    @GetMapping("/project/detail")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-delivery:query')")
    public CommonResult<MesProEdhrDeliveryProjectRespVO> getProjectDetail(@RequestParam("id") Long id) {
        return success(deliveryService.getProjectDetail(id));
    }

    @GetMapping("/evidence-package/page")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-delivery:query')")
    public CommonResult<PageResult<MesProEdhrEvidencePackageRespVO>> getEvidencePackagePage(
            @Valid MesProEdhrEvidencePackagePageReqVO reqVO) {
        return success(deliveryService.getEvidencePackagePage(reqVO));
    }

    @GetMapping("/gate-summary")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-delivery:query')")
    public CommonResult<MesProEdhrDeliveryGateSummaryRespVO> getGateSummary(@RequestParam("projectId") Long projectId) {
        return success(deliveryService.getGateSummary(projectId));
    }
}
