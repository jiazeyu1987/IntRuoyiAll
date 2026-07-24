package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrRecordbookTemplateActivateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrRecordbookTemplateCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrRecordbookTemplatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrRecordbookTemplateRespVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrRecordbookService;
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

@Tag(name = "管理后台 - MES eDHR 记录本模板")
@RestController
@RequestMapping("/mes/pro/edhr-recordbook-template")
@Validated
public class MesProEdhrRecordbookTemplateController {

    @Resource
    private MesProEdhrRecordbookService recordbookService;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-recordbook-template:query')")
    public CommonResult<PageResult<MesProEdhrRecordbookTemplateRespVO>> getPage(
            @Valid MesProEdhrRecordbookTemplatePageReqVO reqVO) {
        return success(recordbookService.getTemplatePage(reqVO));
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-recordbook-template:create')")
    public CommonResult<MesProEdhrRecordbookTemplateRespVO> create(
            @Valid @RequestBody MesProEdhrRecordbookTemplateCreateReqVO reqVO) {
        return success(recordbookService.createTemplate(reqVO));
    }

    @PostMapping("/activate")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-recordbook-template:activate')")
    public CommonResult<MesProEdhrRecordbookTemplateRespVO> activate(
            @Valid @RequestBody MesProEdhrRecordbookTemplateActivateReqVO reqVO) {
        return success(recordbookService.activateTemplate(reqVO));
    }
}
