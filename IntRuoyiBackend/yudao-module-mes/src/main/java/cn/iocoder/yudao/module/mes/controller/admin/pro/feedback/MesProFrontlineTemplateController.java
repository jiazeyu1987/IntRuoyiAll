package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineTemplatePayloadReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineTemplateResolveReqVO;
import cn.iocoder.yudao.module.mes.service.pro.frontline.template.FrontlineTemplateDefinition;
import cn.iocoder.yudao.module.mes.service.pro.frontline.template.FrontlineTemplatePayload;
import cn.iocoder.yudao.module.mes.service.pro.frontline.template.FrontlineTemplateService;
import io.swagger.v3.oas.annotations.Operation;
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

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - MES 一线固定模板")
@RestController
@RequestMapping("/mes/pro/feedback/frontline-template")
@Validated
public class MesProFrontlineTemplateController {

    @Resource
    private FrontlineTemplateService frontlineTemplateService;

    @GetMapping("/catalog")
    @Operation(summary = "查询一线固定模板目录")
    @PreAuthorize("@ss.hasPermission('mes:pro-feedback:query')")
    public CommonResult<List<FrontlineTemplateDefinition>> getCatalog() {
        return success(frontlineTemplateService.listCatalog());
    }

    @GetMapping("/resolve")
    @Operation(summary = "按员工和工序解析一线固定模板")
    @PreAuthorize("@ss.hasPermission('mes:pro-feedback:query')")
    public CommonResult<FrontlineTemplateDefinition> resolveTemplate(@Valid MesProFrontlineTemplateResolveReqVO reqVO) {
        return success(frontlineTemplateService.resolveTemplate(reqVO.toCommand()));
    }

    @PostMapping("/payload/validate")
    @Operation(summary = "校验一线固定模板 payload")
    @PreAuthorize("@ss.hasPermission('mes:pro-feedback:query')")
    public CommonResult<FrontlineTemplatePayload> validatePayload(
            @Valid @RequestBody MesProFrontlineTemplatePayloadReqVO reqVO) {
        return success(frontlineTemplateService.buildPayload(reqVO.toCommand()));
    }
}
