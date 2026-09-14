package cn.iocoder.yudao.module.mes.controller.admin.qa.regulation;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaCommonRegulationBindReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaCommonRegulationBindingRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaCommonRegulationSetRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaCommonRegulationSetSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaCommonRegulationSetVersionOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaCommonRegulationSetVersionSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaCommonRegulationVersionOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationProjectStatusRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationParseRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationPublishedVersionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationResetRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationSaveRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationVersionOptionRespVO;
import cn.iocoder.yudao.module.mes.service.qa.regulation.MesQaInspectionRegulationService;
import cn.iocoder.yudao.module.mes.service.qa.regulation.MesQaInspectionRegulationParseService;
import cn.iocoder.yudao.module.mes.service.qa.regulation.MesQaInspectionRegulationWordImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
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

    @Resource
    private MesQaInspectionRegulationParseService parseService;

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
            @RequestParam("dccProjectCodeId") Long dccProjectCodeId,
            @RequestParam(value = "ownerModule", required = false) String ownerModule,
            @RequestParam(value = "publishAfterImport", required = false, defaultValue = "false")
            Boolean publishAfterImport) {
        return success(wordImportService.importWordDraft(
                file, dccProjectCodeId, ownerModule, Boolean.TRUE.equals(publishAfterImport)));
    }

    @PostMapping("/form-parser-json")
    @Operation(summary = "解析 QA 检验规程 Word 为表单解析 JSON")
    @PreAuthorize("@ss.hasPermission('form:parser:query')")
    public CommonResult<MesQaInspectionRegulationParseRespVO> parseQaInspectionRegulationJson(
            @RequestParam("file") MultipartFile file) {
        return success(parseService.parseWord(file));
    }

    @PostMapping("/test-reset")
    @Operation(summary = "测试阶段重置指定 DCC 项目的 QA 检验规程")
    @PreAuthorize("@ss.hasPermission('mes:qc-template:update')")
    public CommonResult<MesQaInspectionRegulationResetRespVO> resetForTesting(
            @RequestParam("dccProjectCodeId") Long dccProjectCodeId) {
        return success(regulationService.resetForTesting(dccProjectCodeId));
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

    @GetMapping("/versions")
    @Operation(summary = "获得 DCC 项目全部 QA 检验规程版本")
    @Parameter(name = "dccProjectCodeId", description = "DCC 项目代码 ID")
    @PreAuthorize("@ss.hasPermission('mes:qc-template:query')")
    public CommonResult<List<MesQaInspectionRegulationVersionOptionRespVO>> listVersions(
            @RequestParam("dccProjectCodeId") Long dccProjectCodeId) {
        return success(regulationService.listVersions(dccProjectCodeId));
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

    @GetMapping("/common-binding/current")
    @Operation(summary = "获得产品当前通用检验规程绑定")
    @Parameter(name = "dccProjectCodeId", description = "产品 QA 所选 DCC 项目代码 ID")
    @PreAuthorize("@ss.hasPermission('mes:qc-template:query')")
    public CommonResult<MesQaCommonRegulationBindingRespVO> getCurrentCommonRegulationBinding(
            @RequestParam("dccProjectCodeId") Long dccProjectCodeId) {
        return success(regulationService.getCurrentCommonRegulationBinding(dccProjectCodeId));
    }

    @GetMapping("/common-binding/published-versions")
    @Operation(summary = "获得可绑定的已发布通用检验规程版本")
    @PreAuthorize("@ss.hasPermission('mes:qc-template:query')")
    public CommonResult<List<MesQaCommonRegulationVersionOptionRespVO>> listCommonRegulationPublishedVersions() {
        return success(regulationService.listCommonRegulationPublishedVersions());
    }

    @GetMapping("/common-sets")
    @Operation(summary = "获得通用检验规程套列表")
    @PreAuthorize("@ss.hasPermission('mes:qc-template:query')")
    public CommonResult<List<MesQaCommonRegulationSetRespVO>> listCommonRegulationSets() {
        return success(regulationService.listCommonRegulationSets());
    }

    @GetMapping("/common-sets/get")
    @Operation(summary = "获得通用检验规程套详情")
    @PreAuthorize("@ss.hasPermission('mes:qc-template:query')")
    public CommonResult<MesQaCommonRegulationSetRespVO> getCommonRegulationSet(
            @RequestParam("setId") Long setId) {
        return success(regulationService.getCommonRegulationSet(setId));
    }

    @PostMapping("/common-sets/save")
    @Operation(summary = "保存通用检验规程套")
    @PreAuthorize("@ss.hasPermission('mes:qc-template:update')")
    public CommonResult<MesQaCommonRegulationSetRespVO> saveCommonRegulationSet(
            @Valid @RequestBody MesQaCommonRegulationSetSaveReqVO reqVO) {
        return success(regulationService.saveCommonRegulationSet(reqVO));
    }

    @DeleteMapping("/common-sets/delete")
    @Operation(summary = "删除通用检验规程套")
    @PreAuthorize("@ss.hasPermission('mes:qc-template:update')")
    public CommonResult<Boolean> deleteCommonRegulationSet(@RequestParam("setId") Long setId) {
        regulationService.deleteCommonRegulationSet(setId);
        return success(true);
    }

    @PostMapping("/common-set-versions/save")
    @Operation(summary = "保存通用检验规程套版本")
    @PreAuthorize("@ss.hasPermission('mes:qc-template:update')")
    public CommonResult<MesQaCommonRegulationSetRespVO.Version> saveCommonRegulationSetVersion(
            @Valid @RequestBody MesQaCommonRegulationSetVersionSaveReqVO reqVO) {
        return success(regulationService.saveCommonRegulationSetVersion(reqVO));
    }

    @DeleteMapping("/common-set-versions/delete")
    @Operation(summary = "删除通用检验规程套版本")
    @PreAuthorize("@ss.hasPermission('mes:qc-template:update')")
    public CommonResult<Boolean> deleteCommonRegulationSetVersion(
            @RequestParam("setVersionId") Long setVersionId) {
        regulationService.deleteCommonRegulationSetVersion(setVersionId);
        return success(true);
    }

    @GetMapping("/common-binding/published-set-versions")
    @Operation(summary = "获得可绑定的已发布通用检验规程套版本")
    @PreAuthorize("@ss.hasPermission('mes:qc-template:query')")
    public CommonResult<List<MesQaCommonRegulationSetVersionOptionRespVO>>
    listCommonRegulationPublishedSetVersions() {
        return success(regulationService.listCommonRegulationPublishedSetVersions());
    }

    @PostMapping("/common-binding/bind")
    @Operation(summary = "绑定产品当前通用检验规程版本")
    @PreAuthorize("@ss.hasPermission('mes:qc-template:update')")
    public CommonResult<MesQaCommonRegulationBindingRespVO> bindCommonRegulationVersion(
            @Valid @RequestBody MesQaCommonRegulationBindReqVO reqVO) {
        return success(regulationService.bindCommonRegulationVersion(reqVO));
    }

    @PostMapping("/common-binding/unbind")
    @Operation(summary = "解除产品当前通用检验规程绑定")
    @PreAuthorize("@ss.hasPermission('mes:qc-template:update')")
    public CommonResult<MesQaCommonRegulationBindingRespVO> unbindCommonRegulation(
            @RequestParam("dccProjectCodeId") Long dccProjectCodeId) {
        return success(regulationService.unbindCommonRegulation(dccProjectCodeId));
    }
}
