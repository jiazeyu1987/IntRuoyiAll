package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormActivateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormTemplateCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormTemplatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormTemplateRespVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrFormService;
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

@Tag(name = "管理后台 - MES eDHR 独立表单模板")
@RestController
@RequestMapping("/mes/pro/edhr-form-template")
@Validated
public class MesProEdhrFormTemplateController {

    @Resource
    private MesProEdhrFormService formService;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-form-template:query')")
    public CommonResult<PageResult<MesProEdhrFormTemplateRespVO>> getPage(
            @Valid MesProEdhrFormTemplatePageReqVO reqVO) {
        return success(formService.getTemplatePage(reqVO));
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-form-template:create')")
    public CommonResult<MesProEdhrFormTemplateRespVO> create(
            @Valid @RequestBody MesProEdhrFormTemplateCreateReqVO reqVO) {
        return success(formService.createTemplate(reqVO));
    }

    @PostMapping("/activate")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-form-template:activate')")
    public CommonResult<MesProEdhrFormTemplateRespVO> activate(
            @Valid @RequestBody MesProEdhrFormActivateReqVO reqVO) {
        return success(formService.activateTemplate(reqVO));
    }
}
