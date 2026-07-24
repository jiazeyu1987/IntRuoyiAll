package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrLabelTemplateActivateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrLabelTemplateCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrLabelTemplatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrLabelTemplateRespVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrLabelPrintService;
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

@Tag(name = "管理后台 - MES eDHR 标签模板")
@RestController
@RequestMapping("/mes/pro/edhr-label-template")
@Validated
public class MesProEdhrLabelTemplateController {

    @Resource
    private MesProEdhrLabelPrintService labelPrintService;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-label-template:query')")
    public CommonResult<PageResult<MesProEdhrLabelTemplateRespVO>> getPage(
            @Valid MesProEdhrLabelTemplatePageReqVO reqVO) {
        return success(labelPrintService.getLabelTemplatePage(reqVO));
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-label-template:create')")
    public CommonResult<MesProEdhrLabelTemplateRespVO> create(
            @Valid @RequestBody MesProEdhrLabelTemplateCreateReqVO reqVO) {
        return success(labelPrintService.createLabelTemplate(reqVO));
    }

    @PostMapping("/activate")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-label-template:activate')")
    public CommonResult<MesProEdhrLabelTemplateRespVO> activate(
            @Valid @RequestBody MesProEdhrLabelTemplateActivateReqVO reqVO) {
        return success(labelPrintService.activateLabelTemplate(reqVO));
    }
}
