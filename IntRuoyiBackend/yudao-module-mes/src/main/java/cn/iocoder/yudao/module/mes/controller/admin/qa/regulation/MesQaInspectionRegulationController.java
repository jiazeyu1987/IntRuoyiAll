package cn.iocoder.yudao.module.mes.controller.admin.qa.regulation;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationPublishedVersionRespVO;
import cn.iocoder.yudao.module.mes.service.qa.regulation.MesQaInspectionRegulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - MES QA 检验规程")
@RestController
@RequestMapping("/mes/qa/inspection-regulation")
@Validated
public class MesQaInspectionRegulationController {

    @Resource
    private MesQaInspectionRegulationService regulationService;

    @GetMapping("/published-version")
    @Operation(summary = "获得 QA 检验规程发布版本只读证据")
    @Parameter(name = "versionId", description = "QA 检验规程发布版本 ID；为空时返回最新已发布版本")
    @PreAuthorize("@ss.hasPermission('mes:qc-template:query')")
    public CommonResult<MesQaInspectionRegulationPublishedVersionRespVO> getPublishedVersion(
            @RequestParam(value = "versionId", required = false) Long versionId) {
        return success(regulationService.getPublishedVersion(versionId));
    }
}
