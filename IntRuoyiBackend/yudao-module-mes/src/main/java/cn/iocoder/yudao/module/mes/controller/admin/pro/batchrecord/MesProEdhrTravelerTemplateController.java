package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrTravelerActivateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrTravelerTemplateCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrTravelerTemplatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrTravelerTemplateRespVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrTravelerService;
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

@Tag(name = "管理后台 - MES eDHR 流转单模板")
@RestController
@RequestMapping("/mes/pro/edhr-traveler-template")
@Validated
public class MesProEdhrTravelerTemplateController {

    @Resource
    private MesProEdhrTravelerService travelerService;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-traveler-template:query')")
    public CommonResult<PageResult<MesProEdhrTravelerTemplateRespVO>> getPage(
            @Valid MesProEdhrTravelerTemplatePageReqVO reqVO) {
        return success(travelerService.getTemplatePage(reqVO));
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-traveler-template:create')")
    public CommonResult<MesProEdhrTravelerTemplateRespVO> create(
            @Valid @RequestBody MesProEdhrTravelerTemplateCreateReqVO reqVO) {
        return success(travelerService.createTemplate(reqVO));
    }

    @PostMapping("/activate")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-traveler-template:activate')")
    public CommonResult<MesProEdhrTravelerTemplateRespVO> activate(
            @Valid @RequestBody MesProEdhrTravelerActivateReqVO reqVO) {
        return success(travelerService.activateTemplate(reqVO));
    }
}
