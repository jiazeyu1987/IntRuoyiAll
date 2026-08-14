package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportBatchDeleteReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportDeleteAllRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportCellRulesReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportCellRulesRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportDesignerPathRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportImportPreflightRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportImportRouteProductRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportRenameReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportSignatureCellMarkersReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportSignatureCellMarkersRespVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordImportResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordImportPreflightResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportView;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordVersionApprovalResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - MES 电子批记录生成报表")
@RestController
@RequestMapping("/mes/pro/batch-record-report")
@Validated
public class MesProBatchRecordReportController {

    @Resource
    private MesProBatchRecordReportService batchRecordReportService;

    @PostMapping("/import")
    @Operation(summary = "导入电子批记录试点 DOC 并生成报表")
    public CommonResult<BatchRecordReportImportRespVO> importPilotDoc(@RequestParam("file") MultipartFile file) {
        return success(toImportRespVO(batchRecordReportService.importPilotDoc(file)));
    }

    @PostMapping("/import-image")
    @Operation(summary = "导入电子批记录图片并生成报表")
    public CommonResult<BatchRecordReportImportRespVO> importImage(@RequestParam("file") MultipartFile file) {
        return success(toImportRespVO(batchRecordReportService.importImage(file)));
    }

    @PostMapping("/recognize-fixed")
    @Operation(summary = "指定识别路线识别固定电子批记录 DOC 并生成报表")
    public CommonResult<BatchRecordReportImportRespVO> recognizeFixedRoute(@RequestParam("routeKey") String routeKey) {
        return success(toImportRespVO(batchRecordReportService.recognizeFixedRoute(routeKey)));
    }

    @PostMapping("/recognize-uploaded")
    @Operation(summary = "上传电子批记录 DOC 并指定识别路线生成报表")
    public CommonResult<BatchRecordReportImportRespVO> recognizeUploadedRoute(
            @RequestParam("file") MultipartFile file,
            @RequestParam("routeKey") String routeKey,
            @RequestParam("batchRecordName") String batchRecordName,
            @RequestParam("upgrade") Boolean upgrade,
            @RequestParam(value = "importAction", required = false) String importAction,
            @RequestParam(value = "expectedSourceVersionId", required = false) Long expectedSourceVersionId,
            @RequestParam(value = "expectedTargetVersionNo", required = false) String expectedTargetVersionNo,
            @RequestParam("productNames") List<String> productNames,
            @RequestParam(value = "rebuildBatchRecord", defaultValue = "true") Boolean rebuildBatchRecord,
            @RequestParam(value = "selectedRouteProductIds", required = false) List<Long> selectedRouteProductIds,
            @RequestParam(value = "selectedProductNames", required = false) List<String> selectedProductNames,
            @RequestParam(value = "routeUpgradeConfirmed", defaultValue = "false") Boolean routeUpgradeConfirmed,
            @RequestParam(value = "expectedRouteId", required = false) Long expectedRouteId,
            @RequestParam(value = "expectedRouteVersionId", required = false) Long expectedRouteVersionId,
            @RequestParam(value = "expectedRouteCandidateVersionId", required = false)
            Long expectedRouteCandidateVersionId) {
        return success(toImportRespVO(batchRecordReportService.recognizeUploadedRoute(
                file, routeKey, batchRecordName, resolveImportAction(importAction, upgrade), expectedSourceVersionId,
                expectedTargetVersionNo, productNames,
                rebuildBatchRecord, selectedRouteProductIds, selectedProductNames,
                routeUpgradeConfirmed, expectedRouteId, expectedRouteVersionId,
                expectedRouteCandidateVersionId, getLoginUserId())));
    }

    @GetMapping("/recognize-uploaded/preflight")
    @Operation(summary = "上传电子批记录 DOC 前预检当前批记录版本和工艺路线产品绑定")
    public CommonResult<BatchRecordReportImportPreflightRespVO> preflightUploadedRoute(
            @RequestParam("routeKey") String routeKey,
            @RequestParam("batchRecordName") String batchRecordName,
            @RequestParam("productNames") List<String> productNames) {
        return success(toPreflightRespVO(batchRecordReportService.preflightUploadedRoute(
                routeKey, batchRecordName, productNames)));
    }

    @PostMapping("/version-approval/submit")
    @Operation(summary = "提交批记录版本升版审批")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-template:version-approve')")
    public CommonResult<MesProBatchRecordVersionApprovalResult> submitBatchRecordVersionApproval(
            @RequestParam("versionId") Long versionId) {
        return success(batchRecordReportService.submitBatchRecordVersionApproval(versionId, getLoginUserId()));
    }

    @PostMapping("/upload-extra-slot")
    @Operation(summary = "上传批记录附加表单槽位 Word 并生成表单")
    public CommonResult<BatchRecordReportImportRespVO> uploadExtraFormSlot(
            @RequestParam("file") MultipartFile file,
            @RequestParam("batchRecordName") String batchRecordName,
            @RequestParam("formSlotType") String formSlotType) {
        return success(toImportRespVO(batchRecordReportService.uploadExtraFormSlot(
                file, batchRecordName, formSlotType, getLoginUserId())));
    }

    @GetMapping("/exists")
    @Operation(summary = "检查批记录名称在指定识别路线下是否已存在")
    public CommonResult<Boolean> existsBatchRecordName(@RequestParam("routeKey") String routeKey,
                                                       @RequestParam("batchRecordName") String batchRecordName) {
        return success(batchRecordReportService.existsBatchRecordName(routeKey, batchRecordName));
    }

    @GetMapping("/batch-record-names")
    @Operation(summary = "查询批记录名称下拉选项")
    public CommonResult<List<String>> getBatchRecordNameOptions() {
        return success(batchRecordReportService.getBatchRecordNameOptions());
    }

    @GetMapping("/product-name-options")
    @Operation(summary = "查询批记录表单产品名称下拉选项")
    public CommonResult<List<String>> getProductNameOptions(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "latestVersionOnly", required = false, defaultValue = "false")
            Boolean latestVersionOnly) {
        return success(batchRecordReportService.getProductNameOptions(keyword, latestVersionOnly));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询电子批记录生成报表")
    public CommonResult<PageResult<BatchRecordReportRespVO>> getGeneratedReportPage(@Valid BatchRecordReportPageReqVO pageReqVO) {
        PageResult<MesProBatchRecordReportView> pageResult = batchRecordReportService.getGeneratedReportPage(pageReqVO);
        return success(new PageResult<>(toRespVOList(pageResult.getList()), pageResult.getTotal()));
    }

    @GetMapping("/designer-path")
    @Operation(summary = "获取电子批记录报表设计器路径")
    @Parameter(name = "reportId", description = "积木报表 ID", required = true)
    public CommonResult<BatchRecordReportDesignerPathRespVO> getDesignerPath(@RequestParam("reportId") String reportId) {
        BatchRecordReportDesignerPathRespVO response = new BatchRecordReportDesignerPathRespVO();
        response.setPath(batchRecordReportService.getDesignerPath(reportId));
        return success(response);
    }

    @GetMapping("/edit-path")
    @Operation(summary = "获取电子批记录报表编辑路径")
    @Parameter(name = "reportId", description = "积木报表 ID", required = true)
    public CommonResult<BatchRecordReportDesignerPathRespVO> getEditPath(@RequestParam("reportId") String reportId) {
        BatchRecordReportDesignerPathRespVO response = new BatchRecordReportDesignerPathRespVO();
        response.setPath(batchRecordReportService.getEditPath(reportId));
        return success(response);
    }

    @GetMapping("/signature-cell-markers")
    @Operation(summary = "获取电子批记录报表签名单元格配置")
    @Parameter(name = "reportId", description = "积木报表 ID", required = true)
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-template:query')")
    public CommonResult<BatchRecordReportSignatureCellMarkersRespVO> getSignatureCellMarkers(
            @RequestParam("reportId") String reportId) {
        return success(batchRecordReportService.getSignatureCellMarkers(reportId));
    }

    @PutMapping("/signature-cell-markers")
    @Operation(summary = "保存电子批记录报表签名单元格配置")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-template:update')")
    public CommonResult<BatchRecordReportSignatureCellMarkersRespVO> saveSignatureCellMarkers(
            @Valid @RequestBody BatchRecordReportSignatureCellMarkersReqVO reqVO) {
        return success(batchRecordReportService.saveSignatureCellMarkers(reqVO));
    }

    @GetMapping("/cell-rules")
    @Operation(summary = "获取电子批记录报表单元格填写规则")
    @Parameter(name = "reportId", description = "积木报表 ID", required = true)
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-template:query')")
    public CommonResult<BatchRecordReportCellRulesRespVO> getCellRules(@RequestParam("reportId") String reportId) {
        return success(batchRecordReportService.getCellRules(reportId));
    }

    @PutMapping("/cell-rules")
    @Operation(summary = "保存电子批记录报表单元格填写规则")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-template:update')")
    public CommonResult<BatchRecordReportCellRulesRespVO> saveCellRules(
            @Valid @RequestBody BatchRecordReportCellRulesReqVO reqVO) {
        return success(batchRecordReportService.saveCellRules(reqVO));
    }

    @PutMapping("/rename")
    @Operation(summary = "重命名电子批记录报表")
    public CommonResult<Boolean> renameGeneratedReport(@Valid @RequestBody BatchRecordReportRenameReqVO reqVO) {
        batchRecordReportService.renameGeneratedReport(reqVO.getReportId(), reqVO.getReportName());
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除电子批记录生成报表")
    @Parameter(name = "reportId", description = "积木报表 ID", required = true)
    public CommonResult<Boolean> deleteGeneratedReport(@RequestParam("reportId") String reportId) {
        batchRecordReportService.deleteGeneratedReport(reportId);
        return success(true);
    }

    @DeleteMapping("/delete-batch")
    @Operation(summary = "批量删除电子批记录生成报表")
    public CommonResult<BatchRecordReportDeleteAllRespVO> deleteGeneratedReports(
            @Valid @RequestBody BatchRecordReportBatchDeleteReqVO reqVO) {
        return success(batchRecordReportService.deleteGeneratedReports(reqVO.getReportIds(), reqVO.getForceUnbind()));
    }

    @DeleteMapping("/delete-extra-slot")
    @Operation(summary = "按批记录名称和附加表单槽位删除电子批记录生成报表")
    public CommonResult<Boolean> deleteGeneratedReportByBatchRecordNameAndFormSlotType(
            @RequestParam("batchRecordName") String batchRecordName,
            @RequestParam("formSlotType") String formSlotType) {
        batchRecordReportService.deleteGeneratedReportByBatchRecordNameAndFormSlotType(batchRecordName, formSlotType);
        return success(true);
    }

    @DeleteMapping("/delete-by-batch-record-name")
    @Operation(summary = "按批记录名称删除电子批记录生成报表")
    @Parameter(name = "batchRecordName", description = "批记录名称", required = true)
    @Parameter(name = "forceUnbind", description = "是否先解除工艺路线/用途绑定后删除")
    public CommonResult<BatchRecordReportDeleteAllRespVO> deleteGeneratedReportsByBatchRecordName(
            @RequestParam("batchRecordName") String batchRecordName,
            @RequestParam(value = "forceUnbind", defaultValue = "false") Boolean forceUnbind) {
        return success(batchRecordReportService.deleteGeneratedReportsByBatchRecordName(batchRecordName, forceUnbind));
    }

    @DeleteMapping("/delete-all")
    @Operation(summary = "删除电子批记录目录下全部生成报表")
    @Parameter(name = "confirm", description = "删除确认码，必须为 PROD", required = true)
    public CommonResult<BatchRecordReportDeleteAllRespVO> deleteAllGeneratedReports(
            @RequestParam("confirm") String confirm) {
        return success(batchRecordReportService.deleteAllGeneratedReports(confirm));
    }

    private BatchRecordReportImportRespVO toImportRespVO(MesProBatchRecordImportResult result) {
        BatchRecordReportImportRespVO response = new BatchRecordReportImportRespVO();
        response.setImportedCount(result.importedCount());
        response.setCreatedCount(result.createdCount());
        response.setUpdatedCount(result.updatedCount());
        response.setBatchRecordDefinitionId(result.batchRecordDefinitionId());
        response.setBatchRecordVersionId(result.batchRecordVersionId());
        response.setSourceBatchRecordVersionId(result.sourceBatchRecordVersionId());
        response.setVersionNo(result.versionNo());
        response.setVersionStatus(result.versionStatus());
        response.setApprovalInstanceId(result.approvalInstanceId());
        response.setRouteId(result.routeId());
        response.setRouteCode(result.routeCode());
        response.setRouteName(result.routeName());
        response.setRouteVersionId(result.routeVersionId());
        response.setRouteVersionNo(result.routeVersionNo());
        response.setRouteProcessCount(result.routeProcessCount());
        response.setBatchRecordRouteBindingCount(result.batchRecordRouteBindingCount());
        response.setBoundProductNameCount(result.boundProductNameCount());
        response.setBoundProductCodeCount(result.boundProductCodeCount());
        response.setSkippedProductNames(result.skippedProductNames());
        response.setReports(toRespVOList(result.reports()));
        return response;
    }

    private BatchRecordReportImportPreflightRespVO toPreflightRespVO(MesProBatchRecordImportPreflightResult result) {
        BatchRecordReportImportPreflightRespVO response = new BatchRecordReportImportPreflightRespVO();
        response.setRouteKey(result.routeKey());
        response.setBatchRecordName(result.batchRecordName());
        response.setBatchRecordDefinitionId(result.batchRecordDefinitionId());
        response.setCurrentBatchRecordVersionId(result.currentBatchRecordVersionId());
        response.setCurrentBatchRecordVersionNo(result.currentBatchRecordVersionNo());
        response.setCurrentBatchRecordVersionStatus(result.currentBatchRecordVersionStatus());
        response.setLatestBatchRecordVersionId(result.latestBatchRecordVersionId());
        response.setLatestBatchRecordVersionNo(result.latestBatchRecordVersionNo());
        response.setLatestBatchRecordVersionStatus(result.latestBatchRecordVersionStatus());
        response.setCurrentBatchRecordHasMainReports(result.currentBatchRecordHasMainReports());
        response.setRouteGovernanceStatus(result.routeGovernanceStatus());
        response.setRouteUpgradeRequired(result.routeUpgradeRequired());
        response.setDuplicateRoutes(result.duplicateRoutes() == null ? List.of()
                : result.duplicateRoutes().stream().map(route -> {
                    BatchRecordReportImportPreflightRespVO.DuplicateRouteRespVO item =
                            new BatchRecordReportImportPreflightRespVO.DuplicateRouteRespVO();
                    item.setRouteId(route.routeId());
                    item.setRouteCode(route.routeCode());
                    item.setRouteName(route.routeName());
                    item.setRouteVersionId(route.routeVersionId());
                    item.setRouteVersionNo(route.routeVersionNo());
                    return item;
                }).toList());
        response.setCurrentRouteId(result.currentRouteId());
        response.setCurrentRouteCode(result.currentRouteCode());
        response.setCurrentRouteName(result.currentRouteName());
        response.setCurrentRouteVersionId(result.currentRouteVersionId());
        response.setCurrentRouteVersionNo(result.currentRouteVersionNo());
        response.setCurrentRouteVersionActive(result.currentRouteVersionActive());
        response.setCurrentRouteCandidateVersionId(result.currentRouteCandidateVersionId());
        response.setCurrentRouteCandidateVersionNo(result.currentRouteCandidateVersionNo());
        response.setCurrentRouteCandidateVersionStatus(result.currentRouteCandidateVersionStatus());
        response.setHasHistoricalReferences(result.hasHistoricalReferences());
        response.setAllowedActions(result.allowedActions());
        response.setRecommendedAction(result.recommendedAction());
        response.setNextVersionNo(result.nextVersionNo());
        response.setReferenceBlockers(result.referenceBlockers() == null ? List.of()
                : result.referenceBlockers().stream().map(blocker -> {
                    BatchRecordReportImportPreflightRespVO.ReferenceBlockerRespVO item =
                            new BatchRecordReportImportPreflightRespVO.ReferenceBlockerRespVO();
                    item.setVersionNo(blocker.versionNo());
                    item.setReferenceName(blocker.referenceName());
                    item.setCount(blocker.count());
                    item.setCleanupEntrance(blocker.cleanupEntrance());
                    item.setCleanupAction(blocker.cleanupAction());
                    return item;
                }).toList());
        response.setRouteProductOptions(result.routeProductOptions() == null ? List.of()
                : result.routeProductOptions().stream().map(option -> {
                    BatchRecordReportImportRouteProductRespVO item = new BatchRecordReportImportRouteProductRespVO();
                    item.setOptionKey(option.optionKey());
                    item.setRouteProductId(option.routeProductId());
                    item.setRouteId(option.routeId());
                    item.setRouteCode(option.routeCode());
                    item.setRouteName(option.routeName());
                    item.setRouteVersionId(option.routeVersionId());
                    item.setRouteVersionNo(option.routeVersionNo());
                    item.setProductId(option.productId());
                    item.setProductCode(option.productCode());
                    item.setProductName(option.productName());
                    item.setExisting(option.existing());
                    return item;
                }).toList());
        return response;
    }

    private String resolveImportAction(String importAction, Boolean upgrade) {
        if (importAction != null && !importAction.isBlank()) {
            return importAction;
        }
        return Boolean.TRUE.equals(upgrade) ? "UPGRADE" : "REBUILD_V1";
    }

    private List<BatchRecordReportRespVO> toRespVOList(List<MesProBatchRecordReportView> reports) {
        return BeanUtils.toBean(reports, BatchRecordReportRespVO.class);
    }
}
