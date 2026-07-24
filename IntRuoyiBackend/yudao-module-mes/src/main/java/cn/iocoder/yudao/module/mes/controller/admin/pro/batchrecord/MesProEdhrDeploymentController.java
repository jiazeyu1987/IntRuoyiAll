package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeploymentCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeploymentPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeploymentPrecheckRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeploymentRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeploymentUpdateReqVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrDeploymentService;
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

@Tag(name = "管理后台 - MES eDHR 部署授权接口")
@RestController
@RequestMapping("/mes/pro/edhr-deployment")
@Validated
public class MesProEdhrDeploymentController {

    @Resource
    private MesProEdhrDeploymentService deploymentService;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-deployment:query')")
    public CommonResult<PageResult<MesProEdhrDeploymentRespVO>> getPage(@Valid MesProEdhrDeploymentPageReqVO reqVO) {
        return success(deploymentService.getPage(reqVO));
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-deployment:create')")
    public CommonResult<MesProEdhrDeploymentRespVO> createEvidence(@Valid @RequestBody MesProEdhrDeploymentCreateReqVO reqVO) {
        return success(deploymentService.createEvidence(reqVO));
    }

    @GetMapping("/detail")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-deployment:query')")
    public CommonResult<MesProEdhrDeploymentRespVO> getDetail(@RequestParam("id") Long id) {
        return success(deploymentService.getDetail(id));
    }

    @PostMapping("/update-evidence")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-deployment:update')")
    public CommonResult<MesProEdhrDeploymentRespVO> updateEvidence(@Valid @RequestBody MesProEdhrDeploymentUpdateReqVO reqVO) {
        return success(deploymentService.updateEvidence(reqVO));
    }

    @PostMapping("/precheck")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-deployment:precheck')")
    public CommonResult<MesProEdhrDeploymentPrecheckRespVO> precheckEvidence(@RequestParam("deploymentId") Long deploymentId) {
        return success(deploymentService.precheckEvidence(deploymentId));
    }
}

