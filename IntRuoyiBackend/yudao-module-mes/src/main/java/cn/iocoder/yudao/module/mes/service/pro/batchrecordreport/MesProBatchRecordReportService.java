package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportDeleteAllRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportCellRulesReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportCellRulesRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportSignatureCellMarkersReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportSignatureCellMarkersRespVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MesProBatchRecordReportService {

    /**
     * 导入项目的批记录总识别 JSON，并同步正式一线设备参数配置。
     */
    void importTotalRecognitionJson(Long dccProjectCodeId, MultipartFile file);

    MesProBatchRecordImportResult importPilotDoc(MultipartFile file);

    MesProBatchRecordImportResult importImage(MultipartFile file);

    MesProBatchRecordImportResult recognizeFixedRoute(String routeKey);

    MesProBatchRecordImportResult recognizeUploadedRoute(MultipartFile file, String routeKey,
                                                         String batchRecordName, Boolean upgrade,
                                                         List<String> productNames);

    MesProBatchRecordImportPreflightResult preflightUploadedRoute(String routeKey, String batchRecordName,
                                                                  List<String> productNames);

    MesProBatchRecordImportPreflightResult preflightUploadedRoute(String routeKey, String batchRecordName,
                                                                  List<String> productNames,
                                                                  Long dccProjectCodeId);

    MesProBatchRecordImportResult recognizeUploadedRoute(MultipartFile file, String routeKey,
                                                         String batchRecordName, Boolean upgrade,
                                                         List<String> productNames,
                                                         Boolean rebuildBatchRecord,
                                                         List<Long> selectedRouteProductIds,
                                                         List<String> selectedProductNames);

    MesProBatchRecordImportResult recognizeUploadedRoute(MultipartFile file, String routeKey,
                                                         String batchRecordName, String importAction,
                                                         Long expectedSourceVersionId,
                                                         List<String> productNames,
                                                         Boolean rebuildBatchRecord,
                                                         List<Long> selectedRouteProductIds,
                                                         List<String> selectedProductNames);

    MesProBatchRecordImportResult recognizeUploadedRoute(MultipartFile file, String routeKey,
                                                         String batchRecordName, String importAction,
                                                         Long expectedSourceVersionId,
                                                         List<String> productNames,
                                                         Boolean rebuildBatchRecord,
                                                         List<Long> selectedRouteProductIds,
                                                         List<String> selectedProductNames,
                                                         Long dccProjectCodeId);

    MesProBatchRecordImportResult recognizeUploadedRoute(MultipartFile file, String routeKey,
                                                         String batchRecordName, String importAction,
                                                         Long expectedSourceVersionId,
                                                         String expectedTargetVersionNo,
                                                         List<String> productNames,
                                                         Boolean rebuildBatchRecord,
                                                         List<Long> selectedRouteProductIds,
                                                         List<String> selectedProductNames);

    MesProBatchRecordImportResult recognizeUploadedRoute(MultipartFile file, String routeKey,
                                                         String batchRecordName, String importAction,
                                                         Long expectedSourceVersionId,
                                                         String expectedTargetVersionNo,
                                                         List<String> productNames,
                                                         Boolean rebuildBatchRecord,
                                                         List<Long> selectedRouteProductIds,
                                                         List<String> selectedProductNames,
                                                         Long approvalSubmitterUserId);

    MesProBatchRecordImportResult recognizeUploadedRoute(MultipartFile file, String routeKey,
                                                         String batchRecordName, String importAction,
                                                         Long expectedSourceVersionId,
                                                         String expectedTargetVersionNo,
                                                         List<String> productNames,
                                                         Boolean rebuildBatchRecord,
                                                         List<Long> selectedRouteProductIds,
                                                         List<String> selectedProductNames,
                                                         Boolean routeUpgradeConfirmed,
                                                         Long expectedRouteId,
                                                         Long expectedRouteVersionId,
                                                         Long approvalSubmitterUserId);

    MesProBatchRecordImportResult recognizeUploadedRoute(MultipartFile file, String routeKey,
                                                         String batchRecordName, String importAction,
                                                         Long expectedSourceVersionId,
                                                         String expectedTargetVersionNo,
                                                         List<String> productNames,
                                                         Boolean rebuildBatchRecord,
                                                         List<Long> selectedRouteProductIds,
                                                         List<String> selectedProductNames,
                                                         Boolean routeUpgradeConfirmed,
                                                         Long expectedRouteId,
                                                         Long expectedRouteVersionId,
                                                         Long expectedRouteCandidateVersionId,
                                                         Long approvalSubmitterUserId);

    MesProBatchRecordImportResult recognizeUploadedRoute(MultipartFile file, String routeKey,
                                                         String batchRecordName, String importAction,
                                                         Long expectedSourceVersionId,
                                                         String expectedTargetVersionNo,
                                                         List<String> productNames,
                                                         Boolean rebuildBatchRecord,
                                                         List<Long> selectedRouteProductIds,
                                                         List<String> selectedProductNames,
                                                         Boolean routeUpgradeConfirmed,
                                                         Long expectedRouteId,
                                                         Long expectedRouteVersionId,
                                                         Long expectedRouteCandidateVersionId,
                                                         Long dccProjectCodeId,
                                                         Long approvalSubmitterUserId);

    MesProBatchRecordVersionApprovalResult submitBatchRecordVersionApproval(Long versionId, Long actorUserId);

    MesProBatchRecordVersionApprovalResult handleBatchRecordVersionApprovalCallback(String approvalInstanceId,
                                                                                   String approvalEventId,
                                                                                   String approvalResult,
                                                                                   Long actorUserId);

    MesProBatchRecordVersionApprovalResult handleBatchRecordVersionApprovalCallback(String approvalInstanceId,
                                                                                   String approvalEventId,
                                                                                   String approvalResult,
                                                                                   String rejectReason,
                                                                                   Long actorUserId);

    MesProBatchRecordImportResult uploadExtraFormSlot(MultipartFile file, String batchRecordName, String formSlotType);

    MesProBatchRecordImportResult uploadExtraFormSlot(MultipartFile file, String batchRecordName, String formSlotType,
                                                      Long approvalSubmitterUserId);

    Boolean existsBatchRecordName(String routeKey, String batchRecordName);

    List<String> getBatchRecordNameOptions();

    List<String> getProductNameOptions(String keyword, Boolean latestVersionOnly);

    PageResult<MesProBatchRecordReportView> getGeneratedReportPage(BatchRecordReportPageReqVO pageReqVO);

    String getDesignerPath(String reportId);

    String getEditPath(String reportId);

    BatchRecordReportSignatureCellMarkersRespVO getSignatureCellMarkers(String reportId);

    BatchRecordReportSignatureCellMarkersRespVO saveSignatureCellMarkers(BatchRecordReportSignatureCellMarkersReqVO reqVO);

    BatchRecordReportCellRulesRespVO getCellRules(String reportId);

    BatchRecordReportCellRulesRespVO formalizeCellRules(String reportId);

    BatchRecordReportCellRulesRespVO saveCellRules(BatchRecordReportCellRulesReqVO reqVO);

    void renameGeneratedReport(String reportId, String reportName);

    void deleteGeneratedReport(String reportId);

    BatchRecordReportDeleteAllRespVO deleteGeneratedReports(List<String> reportIds, Boolean forceUnbind);

    void deleteGeneratedReportByBatchRecordNameAndFormSlotType(String batchRecordName, String formSlotType);

    BatchRecordReportDeleteAllRespVO deleteGeneratedReportsByBatchRecordName(String batchRecordName);

    BatchRecordReportDeleteAllRespVO deleteGeneratedReportsByBatchRecordName(String batchRecordName, Boolean forceUnbind);

    BatchRecordReportDeleteAllRespVO deleteAllGeneratedReports(String confirm);
}
