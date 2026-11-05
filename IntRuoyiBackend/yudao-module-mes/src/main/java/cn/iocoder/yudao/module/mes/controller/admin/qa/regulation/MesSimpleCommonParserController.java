package cn.iocoder.yudao.module.mes.controller.admin.qa.regulation;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.service.qa.regulation.MesQaInspectionRegulationService;
import cn.iocoder.yudao.module.mes.service.qa.regulation.MesQaInspectionRegulationWordImportService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QA_INSPECTION_REGULATION_SNAPSHOT_INVALID;

@RestController
@RequestMapping("/mes/qa/inspection-regulation/simple-common-parser")
@RequiredArgsConstructor
@Validated
public class MesSimpleCommonParserController {
    private final MesQaInspectionRegulationWordImportService importer;
    private final MesQaInspectionRegulationService regulations;

    @PostMapping("/parse")
    @Operation(summary = "只读解析通用规程供编辑及预览")
    @PreAuthorize("@ss.hasPermission('form:parser:query')")
    public CommonResult<MesQaInspectionRegulationSaveReqVO> parse(
            @RequestParam("file") MultipartFile file) {
        return success(importer.previewCommonWord(file));
    }

    @PostMapping("/publish")
    @Operation(summary = "确认编辑内容并正式发布通用规程")
    @PreAuthorize("@ss.hasPermission('mes:qc-template:update')")
    public CommonResult<MesQaInspectionRegulationPublishedVersionRespVO> publish(
            @Valid @RequestBody MesQaInspectionRegulationSaveReqVO request) {
        if (!MesQaInspectionRegulationDO.OWNER_MODULE_MES_QA_COMMON.equals(request.getOwnerModule())) {
            throw exception(QA_INSPECTION_REGULATION_SNAPSHOT_INVALID, "仅允许发布通用检验规程");
        }
        return success(regulations.publish(request));
    }
}
