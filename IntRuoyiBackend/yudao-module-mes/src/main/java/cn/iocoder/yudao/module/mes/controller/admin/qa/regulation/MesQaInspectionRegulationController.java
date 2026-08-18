package cn.iocoder.yudao.module.mes.controller.admin.qa.regulation;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationProjectStatusRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationPublishedVersionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationSaveRespVO;
import cn.iocoder.yudao.module.mes.service.qa.regulation.MesQaInspectionRegulationService;
import cn.iocoder.yudao.module.mes.service.qa.regulation.MesQaInspectionRegulationWordImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - MES QA 检验规程")
@RestController
@RequestMapping("/mes/qa/inspection-regulation")
@Validated
public class MesQaInspectionRegulationController {

    @Resource
    private MesQaInspectionRegulationService regulationService;

    @Resource
    private MesQaInspectionRegulationWordImportService wordImportService;

    @PostMapping("/draft")
    @Operation(summary = "保存 QA 检验规程草稿")
    @PreAuthorize("@ss.hasPermission('mes:qc-template:update')")
    public CommonResult<MesQaInspectionRegulationSaveRespVO> saveDraft(
            @Valid @RequestBody MesQaInspectionRegulationSaveReqVO reqVO) {
        return success(regulationService.saveDraft(reqVO));
    }

    @PostMapping("/import-word-draft")
    @Operation(summary = "解析 QA Word 模板并保存规程草稿")
    @PreAuthorize("@ss.hasPermission('mes:qc-template:update')")
    public CommonResult<MesQaInspectionRegulationImportRespVO> importWordDraft(
            @RequestParam("file") MultipartFile file,
            @RequestParam("dccProjectCodeId") Long dccProjectCodeId) {
        return success(wordImportService.importWordDraft(file, dccProjectCodeId));
    }

    @PostMapping("/publish")
    @Operation(summary = "发布 QA 检验规程并生成不可变版本")
    @PreAuthorize("@ss.hasPermission('mes:qc-template:update')")
    public CommonResult<MesQaInspectionRegulationPublishedVersionRespVO> publish(
            @Valid @RequestBody MesQaInspectionRegulationSaveReqVO reqVO) {
        return success(regulationService.publish(reqVO));
    }

    @GetMapping("/published-version")
    @Operation(summary = "获得 QA 检验规程发布版本只读证据")
    @Parameter(name = "versionId", description = "QA 检验规程发布版本 ID；为空时返回该 DCC 当前发布版本")
    @PreAuthorize("@ss.hasPermission('mes:qc-template:query')")
    public CommonResult<MesQaInspectionRegulationPublishedVersionRespVO> getPublishedVersion(
            @RequestParam("dccProjectCodeId") Long dccProjectCodeId,
            @RequestParam(value = "versionId", required = false) Long versionId) {
        return success(regulationService.getPublishedVersion(dccProjectCodeId, versionId));
    }

    @GetMapping("/current")
    @Operation(summary = "获得 DCC 项目当前 QA 规程配置")
    @PreAuthorize("@ss.hasPermission('mes:qc-template:query')")
    public CommonResult<MesQaInspectionRegulationPublishedVersionRespVO> getCurrent(
            @RequestParam("dccProjectCodeId") Long dccProjectCodeId) {
        return success(regulationService.getCurrent(dccProjectCodeId));
    }

    @GetMapping("/project-statuses")
    @Operation(summary = "批量获得 DCC 项目 QA 检验规程配置状态")
    @Parameter(name = "dccProjectCodeIds", description = "DCC 项目代码 ID 集合")
    @PreAuthorize("@ss.hasPermission('mes:qc-template:query')")
    public CommonResult<List<MesQaInspectionRegulationProjectStatusRespVO>> getProjectStatuses(
            @RequestParam("dccProjectCodeIds") List<Long> dccProjectCodeIds) {
        return success(regulationService.getProjectStatuses(dccProjectCodeIds));
    }
}
