package cn.iocoder.yudao.module.dcc.controller.admin.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccApprovalPrintHtmlRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileAccessExplanationRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledPreviewWatermarkRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileApproveTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileBatchRecognitionCreateReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileBatchRecognitionTaskRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileCreateSignTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileCurrentVersionRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileMetadataUpdateReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileMessageJobReplayReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileMetadataImportPreviewRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileLocalFolderImportBatchReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileLocalFolderImportChunkReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileLocalFolderImportChunkRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileLocalFolderImportReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileLocalFolderImportSessionCreateReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileLocalFolderImportUploadStateRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileNasTransferReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileNasTransferRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileObsoleteReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePreviewMetadataRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePrintCreateReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePrintHtmlRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePrintRecordRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileProjectCodeRecognitionRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePublishReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRecognitionMigrationImportPreviewRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRejectTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileReturnTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRoutePreviewReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRouteReadinessRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSignatureExportSummaryRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSourceMigrationReadinessRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSourceMigrationResultRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSubmitReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileTaskReadinessReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileTaskReadinessRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileTransferTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileUploadDirectoryTreeRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileUploadNameOptionRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileUploadPreviewReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileUploadRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileUploadTemporaryCleanupReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileUploadTemporaryStatusRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileWithdrawReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccBrowserExtensionBlacklistRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccBrowserExtensionBlacklistSaveReqVO;
import cn.iocoder.yudao.module.dcc.service.audit.DccAccessBoundaryLogCreateCommand;
import cn.iocoder.yudao.module.dcc.service.audit.DccControlledFileAccessAuditService;
import cn.iocoder.yudao.module.dcc.service.file.DccApprovalPrintRenderedWord;
import cn.iocoder.yudao.module.dcc.service.file.DccApprovalPrintTemplateService;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccSignatureActionRespVO;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileFinalizationService;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileMetadataUpdateService;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileObsoleteService;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileQueryService;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileSourceMigrationService;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileBrowserSettingsService;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileMessageReplayService;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileMetadataImportExportService;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileBatchRecognitionService;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileNasTransferService;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileProjectCodeRecognitionService;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFilePublishService;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFilePrintService;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileUploadService;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileWorkflowService;
import cn.iocoder.yudao.module.dcc.service.file.DccDmrSheetExportService;
import cn.iocoder.yudao.module.dcc.service.file.DccRequestAuditContext;
import cn.iocoder.yudao.module.dcc.service.file.DccElectronicSignatureManagementService;
import cn.iocoder.yudao.module.dcc.service.file.DccSignatureEvidenceExportArtifact;
import cn.iocoder.yudao.module.dcc.service.file.DccTrainingAssignmentAckService;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTemporaryFileStatus;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_DOWNLOAD_REQUEST_ID_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PERSONAL_PAGE_DISABLED;

@Tag(name = "Admin - DCC Controlled Files")
@RestController
@RequestMapping("/dcc/controlled-files")
@Validated
public class DccControlledFileController {

    public static final String PREVIEW_WATERMARK_HEADER = "X-DCC-Preview-Watermark";
    public static final String ACCESS_EVENT_CODE_HEADER = "X-DCC-Access-Event-Code";
    public static final String VIEWER_TOKEN_HEADER = "X-DCC-Viewer-Token";
    public static final String VIEWER_TOKEN_ID_HEADER = "X-DCC-Viewer-Token-Id";
    public static final String VIEWER_TOKEN_NONCE_HEADER = "X-DCC-Viewer-Token-Nonce";
    public static final String WATERMARK_TRACE_CODE_HEADER = "X-DCC-Watermark-Trace-Code";
    public static final String DOWNLOAD_REQUEST_ID_HEADER = "X-DCC-Download-Request-Id";
    public static final String DOWNLOAD_PLAIN_SHA256_HEADER = "X-DCC-Plain-SHA256";
    public static final String REQUEST_ID_HEADER = DccRequestAuditContext.REQUEST_ID_HEADER;

    @Resource
    private DccControlledFileWorkflowService workflowService;
    @Resource
    private DccElectronicSignatureManagementService signatureManagementService;
    @Resource
    private DccControlledFileFinalizationService finalizationService;
    @Resource
    private DccControlledFileQueryService queryService;
    @Resource
    private DccControlledFileSourceMigrationService sourceMigrationService;
    @Resource
    private DccControlledFileBrowserSettingsService browserSettingsService;
    @Resource
    private DccControlledFileMetadataUpdateService metadataUpdateService;
    @Resource
    private DccControlledFileMetadataImportExportService metadataImportExportService;
    @Resource
    private DccDmrSheetExportService dmrSheetExportService;
    @Resource
    private DccControlledFileProjectCodeRecognitionService projectCodeRecognitionService;
    @Resource
    private DccControlledFileBatchRecognitionService batchRecognitionService;
    @Resource
    private DccControlledFileObsoleteService obsoleteService;
    @Resource
    private DccControlledFilePublishService publishService;
    @Resource
    private DccControlledFileMessageReplayService messageReplayService;
    @Resource
    private DccTrainingAssignmentAckService trainingAssignmentAckService;
    @Resource
    private DccControlledFileUploadService uploadService;
    @Resource
    private DccControlledFileNasTransferService nasTransferService;
    @Resource
    private DccApprovalPrintTemplateService approvalPrintTemplateService;
    @Resource
    private DccControlledFilePrintService controlledFilePrintService;
    @Resource
    private DccUploadTicketService uploadTicketService;
    @Resource
    private DccControlledFileAccessAuditService accessAuditService;

    @PostMapping("/upload-preview")
    @Operation(summary = "Upload one controlled file before submit")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:submit')")
    public CommonResult<DccControlledFileUploadRespVO> uploadPreviewFile(@Valid DccControlledFileUploadPreviewReqVO reqVO,
                                                                         HttpServletRequest request)
            throws Exception {
        return success(uploadService.uploadPreviewFile(getLoginUserId(), reqVO,
                DccRequestAuditContext.from(request, null)));
    }

    @GetMapping("/upload-temporary/status")
    @Operation(summary = "Get current user temporary upload status by request id")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:submit')")
    public CommonResult<DccControlledFileUploadTemporaryStatusRespVO> getUploadTemporaryStatus(
            @RequestParam("requestId") String requestId) {
        return success(toTemporaryStatusRespVO(
                uploadTicketService.getTemporaryFileStatusByRequestId(getLoginUserId(), requestId), null));
    }

    @PostMapping("/upload-temporary/session-cleanup")
    @Operation(summary = "Clean current user's unbound temporary uploads for one upload session")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:submit')")
    public CommonResult<DccControlledFileUploadTemporaryStatusRespVO> cleanupUploadTemporarySession(
            @Valid @RequestBody DccControlledFileUploadTemporaryCleanupReqVO reqVO,
            HttpServletRequest request) throws Exception {
        DccRequestAuditContext auditContext = DccRequestAuditContext.from(request, null);
        LocalDateTime cleanupTime = LocalDateTime.now();
        accessAuditService.recordBoundaryLog(new DccAccessBoundaryLogCreateCommand(getLoginUserId(), "TEMP_FILE",
                "UPLOAD_TEMPORARY_FILE", "REQUESTED", null, "USER_DISCARDED sessionId=" + reqVO.getSessionId(),
                auditContext.sourceIp(), auditContext.requireRequestId("upload temporary cleanup"),
                auditContext.userAgent()));
        int cleanedCount = uploadTicketService.cleanupSessionTemporaryFiles(getLoginUserId(), reqVO.getSessionId(),
                cleanupTime, "USER_DISCARDED");
        DccControlledFileUploadTemporaryStatusRespVO respVO = new DccControlledFileUploadTemporaryStatusRespVO();
        respVO.setRequestId(auditContext.requestId());
        respVO.setTemporaryFileCount(0);
        respVO.setBindable(false);
        respVO.setSessionId(reqVO.getSessionId());
        respVO.setCleanupStatus("CLEANED");
        respVO.setCleanupReason("USER_DISCARDED");
        respVO.setCleanupTime(cleanupTime);
        respVO.setCleanedCount(cleanedCount);
        return success(respVO);
    }

    @GetMapping("/upload-preview/{fileId}/onlyoffice-file")
    @TenantIgnore
    @PermitAll
    @Operation(summary = "Read one temporary upload preview file for OnlyOffice")
    public ResponseEntity<byte[]> getUploadPreviewOnlyOfficeFile(@PathVariable("fileId") Long fileId,
                                                                 @RequestParam("token") String token) throws Exception {
        var binary = uploadService.readUploadPreviewOnlyOfficeFile(fileId, token);
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(binary.contentType()))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        contentDispositionInline(binary.fileName()))
                .body(binary.bytes());
    }

    @PostMapping("/route-preview")
    @Operation(summary = "Preview the resolved submit route")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:submit')")
    public CommonResult<DccControlledFileRouteReadinessRespVO> previewRoute(
            @Valid @RequestBody DccControlledFileRoutePreviewReqVO reqVO) {
        return success(workflowService.previewRoute(getLoginUserId(), reqVO.getCategoryId(),
                reqVO.getSelectedSignoffUserIds()));
    }

    @GetMapping("/upload-name-options")
    @Operation(summary = "List historical upload names for one DCC project and file type taxonomy")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:submit')")
    public CommonResult<List<DccControlledFileUploadNameOptionRespVO>> getUploadNameOptions(
            @RequestParam("dccProjectCodeId") Long dccProjectCodeId,
            @RequestParam("fileTypeTaxonomyId") Long fileTypeTaxonomyId) {
        return success(queryService.listUploadNameOptions(dccProjectCodeId, fileTypeTaxonomyId));
    }

    @GetMapping("/current-version")
    @Operation(summary = "Get current active controlled file version by file number")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:submit')")
    public CommonResult<DccControlledFileCurrentVersionRespVO> getCurrentVersion(
            @RequestParam("fileNumber") String fileNumber) {
        return success(workflowService.getCurrentVersionByFileNumber(getLoginUserId(), fileNumber));
    }

    @GetMapping("/upload-directory-tree")
    @Operation(summary = "Get upload directory tree for one controlled file category")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:submit')")
    public CommonResult<DccControlledFileUploadDirectoryTreeRespVO> getUploadDirectoryTree(Long categoryId) {
        return success(queryService.getUploadDirectoryTree(categoryId));
    }

    @GetMapping("/upload-revision-candidates")
    @Operation(summary = "List active revision candidates by upload project and file type taxonomy")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:submit')")
    public CommonResult<PageResult<DccControlledFileRespVO>> getUploadRevisionCandidates(
            @RequestParam("dccProjectCodeId") Long dccProjectCodeId,
            @RequestParam("fileTypeTaxonomyId") Long fileTypeTaxonomyId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        return success(workflowService.getUploadRevisionCandidates(getLoginUserId(), dccProjectCodeId,
                fileTypeTaxonomyId, keyword, pageNo, pageSize));
    }

    @PostMapping("/submit")
    @Operation(summary = "Submit a controlled file revision")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:submit')")
    public CommonResult<Long> submitControlledFile(@Valid @RequestBody DccControlledFileSubmitReqVO reqVO) {
        return success(workflowService.submitControlledFile(getLoginUserId(), reqVO));
    }

    @PostMapping("/nas-transfer")
    @Operation(summary = "Transfer selected NAS directories into DCC controlled directories/files")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:submit') and @ss.hasPermission('dcc:controlled-file:directory:manage') and @ss.hasPermission('dcc:controlled-file:category:manage')")
    public CommonResult<DccControlledFileNasTransferRespVO> transferNasDirectories(
            @Valid @RequestBody DccControlledFileNasTransferReqVO reqVO) {
        return success(nasTransferService.transfer(getLoginUserId(), reqVO));
    }

    @PostMapping("/local-folder-import")
    @Operation(summary = "Import one local browser-selected folder into DCC controlled directories/files")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:submit') and @ss.hasPermission('dcc:controlled-file:directory:manage') and @ss.hasPermission('dcc:controlled-file:category:manage')")
    public CommonResult<DccControlledFileNasTransferRespVO> importLocalFolder(
            @Valid DccControlledFileLocalFolderImportReqVO reqVO) {
        return success(nasTransferService.importLocalFolder(getLoginUserId(), reqVO));
    }

    @PostMapping("/local-folder-import/sessions")
    @Operation(summary = "Create one local folder import upload session")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:submit') and @ss.hasPermission('dcc:controlled-file:directory:manage') and @ss.hasPermission('dcc:controlled-file:category:manage')")
    public CommonResult<DccControlledFileNasTransferRespVO> createLocalFolderImportSession(
            @Valid @RequestBody DccControlledFileLocalFolderImportSessionCreateReqVO reqVO) {
        return success(nasTransferService.createLocalFolderImportSession(getLoginUserId(), reqVO));
    }

    @PostMapping("/local-folder-import/sessions/{taskId}/batches")
    @Operation(summary = "Upload one local folder import batch")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:submit') and @ss.hasPermission('dcc:controlled-file:directory:manage') and @ss.hasPermission('dcc:controlled-file:category:manage')")
    public CommonResult<DccControlledFileNasTransferRespVO> uploadLocalFolderImportBatch(
            @PathVariable("taskId") Long taskId,
            @Valid DccControlledFileLocalFolderImportBatchReqVO reqVO) {
        return success(nasTransferService.uploadLocalFolderImportBatch(getLoginUserId(), taskId, reqVO));
    }

    @GetMapping("/local-folder-import/sessions/{taskId}/upload-state")
    @Operation(summary = "Get one local folder import resumable upload state")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:submit') and @ss.hasPermission('dcc:controlled-file:directory:manage') and @ss.hasPermission('dcc:controlled-file:category:manage')")
    public CommonResult<DccControlledFileLocalFolderImportUploadStateRespVO> getLocalFolderImportUploadState(
            @PathVariable("taskId") Long taskId) {
        return success(nasTransferService.getLocalFolderImportUploadState(getLoginUserId(), taskId));
    }

    @PostMapping("/local-folder-import/sessions/{taskId}/chunks")
    @Operation(summary = "Upload one local folder import chunk")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:submit') and @ss.hasPermission('dcc:controlled-file:directory:manage') and @ss.hasPermission('dcc:controlled-file:category:manage')")
    public CommonResult<DccControlledFileLocalFolderImportChunkRespVO> uploadLocalFolderImportChunk(
            @PathVariable("taskId") Long taskId,
            @Valid DccControlledFileLocalFolderImportChunkReqVO reqVO) {
        return success(nasTransferService.uploadLocalFolderImportChunk(getLoginUserId(), taskId, reqVO));
    }

    @PostMapping("/local-folder-import/sessions/{taskId}/complete")
    @Operation(summary = "Complete one local folder import upload session")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:submit') and @ss.hasPermission('dcc:controlled-file:directory:manage') and @ss.hasPermission('dcc:controlled-file:category:manage')")
    public CommonResult<DccControlledFileNasTransferRespVO> completeLocalFolderImportSession(
            @PathVariable("taskId") Long taskId) {
        return success(nasTransferService.completeLocalFolderImportSession(getLoginUserId(), taskId));
    }

    @GetMapping("/nas-transfer/tasks/{taskId}")
    @Operation(summary = "Get one NAS transfer task state")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:submit') and @ss.hasPermission('dcc:controlled-file:directory:manage') and @ss.hasPermission('dcc:controlled-file:category:manage')")
    public CommonResult<DccControlledFileNasTransferRespVO> getNasTransferTask(
            @PathVariable("taskId") Long taskId) {
        return success(nasTransferService.getTask(getLoginUserId(), taskId));
    }

    @GetMapping("/page")
    @Operation(summary = "Get controlled file page")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:query')")
    public CommonResult<PageResult<DccControlledFileRespVO>> getControlledFilePage(@Valid DccControlledFilePageReqVO reqVO) {
        throw exception(CONTROLLED_FILE_PERSONAL_PAGE_DISABLED);
    }

    @GetMapping("/browser-page")
    @Operation(summary = "Get controlled file browser summary page")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:query')")
    public CommonResult<PageResult<DccControlledFileRespVO>> getControlledFileBrowserPage(
            @Valid DccControlledFilePageReqVO reqVO) {
        return success(queryService.getControlledFileBrowserPage(getLoginUserId(), reqVO));
    }

    @GetMapping("/browser-extension-blacklist")
    @Operation(summary = "Get controlled file browser extension blacklist")
    @PreAuthorize("@ss.hasRole('doc_control')")
    public CommonResult<DccBrowserExtensionBlacklistRespVO> getBrowserExtensionBlacklist() {
        return success(browserSettingsService.getExtensionBlacklist());
    }

    @PutMapping("/browser-extension-blacklist")
    @Operation(summary = "Save controlled file browser extension blacklist")
    @PreAuthorize("@ss.hasRole('doc_control')")
    public CommonResult<Boolean> saveBrowserExtensionBlacklist(
            @Valid @RequestBody DccBrowserExtensionBlacklistSaveReqVO reqVO) {
        browserSettingsService.saveExtensionBlacklist(reqVO);
        return success(true);
    }

    @GetMapping("/{id:\\d+}")
    @Operation(summary = "Get controlled file detail")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<DccControlledFileRespVO> getControlledFile(@PathVariable("id") Long id) {
        return success(queryService.getControlledFile(getLoginUserId(), id));
    }

    @GetMapping("/{id:\\d+}/access-explanation")
    @Operation(summary = "Explain why current user can or cannot access one controlled file")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:query')")
    public CommonResult<DccControlledFileAccessExplanationRespVO> explainControlledFileAccess(
            @PathVariable("id") Long id) {
        return success(queryService.explainControlledFileAccess(getLoginUserId(), id));
    }

    @PostMapping("/batch-recognition/tasks")
    @Operation(summary = "Create one controlled file batch recognition task")
    @PreAuthorize("@ss.hasRole('doc_control')")
    public CommonResult<DccControlledFileBatchRecognitionTaskRespVO> createBatchRecognitionTask(
            @Valid @RequestBody DccControlledFileBatchRecognitionCreateReqVO reqVO) {
        return success(batchRecognitionService.createTask(getLoginUserId(), reqVO));
    }

    @GetMapping("/batch-recognition/tasks/latest")
    @Operation(summary = "Get latest controlled file batch recognition task by type")
    @PreAuthorize("@ss.hasRole('doc_control')")
    public CommonResult<DccControlledFileBatchRecognitionTaskRespVO> getLatestBatchRecognitionTask(
            @RequestParam("recognitionType") String recognitionType) {
        return success(batchRecognitionService.getLatestTask(getLoginUserId(), recognitionType));
    }

    @GetMapping("/batch-recognition/tasks/{taskId}")
    @Operation(summary = "Get one controlled file batch recognition task")
    @PreAuthorize("@ss.hasRole('doc_control')")
    public CommonResult<DccControlledFileBatchRecognitionTaskRespVO> getBatchRecognitionTask(
            @PathVariable("taskId") Long taskId) {
        return success(batchRecognitionService.getTask(getLoginUserId(), taskId));
    }

    @PostMapping("/batch-recognition/tasks/{taskId}/stop")
    @Operation(summary = "Stop one controlled file batch recognition task")
    @PreAuthorize("@ss.hasRole('doc_control')")
    public CommonResult<DccControlledFileBatchRecognitionTaskRespVO> stopBatchRecognitionTask(
            @PathVariable("taskId") Long taskId) {
        return success(batchRecognitionService.stopTask(getLoginUserId(), taskId));
    }

    @PutMapping("/{id:\\d+}/metadata")
    @Operation(summary = "Update controlled file metadata")
    @PreAuthorize("@ss.hasRole('doc_control') or @ss.hasPermission('dcc:project-code-assignment:execute')")
    public CommonResult<Boolean> updateMetadata(@PathVariable("id") Long id,
                                                @Valid @RequestBody DccControlledFileMetadataUpdateReqVO reqVO) {
        metadataUpdateService.updateMetadata(getLoginUserId(), id, reqVO);
        return success(true);
    }

    @GetMapping("/metadata/export-excel")
    @Operation(summary = "Export controlled file recognized file-name and file-number metadata")
    @PreAuthorize("@ss.hasRole('doc_control')")
    public ResponseEntity<byte[]> exportMetadataExcel(@Valid DccControlledFilePageReqVO reqVO) {
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        org.springframework.http.ContentDisposition.attachment()
                                .filename("受控文件基础信息.xlsx", StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .body(metadataImportExportService.buildExportExcel(getLoginUserId(), reqVO));
    }

    @GetMapping("/recognition-records/export-excel")
    @Operation(summary = "Export controlled file product recognition records")
    @PreAuthorize("@ss.hasRole('doc_control')")
    public ResponseEntity<byte[]> exportRecognitionRecordsExcel(@Valid DccControlledFilePageReqVO reqVO) {
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        org.springframework.http.ContentDisposition.attachment()
                                .filename("受控文件识别记录.xlsx", StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .body(metadataImportExportService.buildRecognitionRecordExportExcel(getLoginUserId(), reqVO));
    }

    @GetMapping("/recognition-records/migration-export-excel")
    @Operation(summary = "Export controlled file recognition migration package")
    @PreAuthorize("@ss.hasRole('doc_control')")
    public ResponseEntity<byte[]> exportRecognitionMigrationExcel(@Valid DccControlledFilePageReqVO reqVO) {
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        org.springframework.http.ContentDisposition.attachment()
                                .filename("受控文件识别结果迁移包.xlsx", StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .body(metadataImportExportService.buildRecognitionMigrationExportExcel(getLoginUserId(), reqVO));
    }

    @GetMapping("/metadata/import-template")
    @Operation(summary = "Download controlled file metadata import template")
    @PreAuthorize("@ss.hasRole('doc_control')")
    public ResponseEntity<byte[]> downloadMetadataImportTemplate() {
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        org.springframework.http.ContentDisposition.attachment()
                                .filename("受控文件基础信息导入模板.xlsx", StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .body(metadataImportExportService.buildImportTemplate());
    }

    @GetMapping("/dmr-sheet/export")
    @Operation(summary = "Export NAS DMR category workbook")
    @PreAuthorize("@ss.hasRole('doc_control')")
    public ResponseEntity<byte[]> exportDmrSheetWorkbook() {
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        org.springframework.http.ContentDisposition.attachment()
                                .filename("DMR-sheet.xlsx", StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .body(dmrSheetExportService.exportWorkbook());
    }

    @PostMapping("/metadata/import-preview")
    @Operation(summary = "Preview controlled file metadata import")
    @PreAuthorize("@ss.hasRole('doc_control')")
    public CommonResult<DccControlledFileMetadataImportPreviewRespVO> previewMetadataImport(
            @RequestParam("file") MultipartFile file) {
        return success(metadataImportExportService.previewImport(getLoginUserId(), file));
    }

    @PostMapping("/metadata/import-confirm")
    @Operation(summary = "Confirm controlled file metadata import")
    @PreAuthorize("@ss.hasRole('doc_control')")
    public CommonResult<DccControlledFileMetadataImportPreviewRespVO> confirmMetadataImport(
            @RequestParam("file") MultipartFile file) {
        return success(metadataImportExportService.confirmImport(getLoginUserId(), file));
    }

    @PostMapping("/recognition-records/migration-import-preview")
    @Operation(summary = "Preview controlled file recognition migration import")
    @PreAuthorize("@ss.hasRole('doc_control')")
    public CommonResult<DccControlledFileRecognitionMigrationImportPreviewRespVO> previewRecognitionMigrationImport(
            @RequestParam("file") MultipartFile file) {
        return success(metadataImportExportService.previewRecognitionMigrationImport(getLoginUserId(), file));
    }

    @PostMapping("/recognition-records/migration-import-confirm")
    @Operation(summary = "Confirm controlled file recognition migration import")
    @PreAuthorize("@ss.hasRole('doc_control')")
    public CommonResult<DccControlledFileRecognitionMigrationImportPreviewRespVO> confirmRecognitionMigrationImport(
            @RequestParam("file") MultipartFile file) {
        return success(metadataImportExportService.confirmRecognitionMigrationImport(getLoginUserId(), file));
    }

    @PostMapping("/{id:\\d+}/recognize-project-code")
    @Operation(summary = "Recognize controlled file DCC basic data with Codex CLI")
    @PreAuthorize("@ss.hasRole('doc_control')")
    public CommonResult<DccControlledFileProjectCodeRecognitionRespVO> recognizeProjectCode(
            @PathVariable("id") Long id) {
        return success(projectCodeRecognitionService.recognizeProjectCode(getLoginUserId(), id));
    }

    @PostMapping("/{id:\\d+}/withdraw")
    @Operation(summary = "Withdraw a pending controlled file revision")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:submit')")
    public CommonResult<Boolean> withdrawControlledFile(@PathVariable("id") Long id,
                                                        @Valid @RequestBody DccControlledFileWithdrawReqVO reqVO) {
        workflowService.withdrawControlledFile(getLoginUserId(), id, reqVO);
        return success(true);
    }

    @DeleteMapping("/{id:\\d+}/withdrawn-flow")
    @Operation(summary = "Delete one withdrawn controlled file workflow from business lists")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:submit')")
    public CommonResult<Boolean> deleteWithdrawnControlledFile(@PathVariable("id") Long id) {
        workflowService.deleteWithdrawnControlledFile(getLoginUserId(), id);
        return success(true);
    }

    @PostMapping("/{id:\\d+}/resubmit")
    @Operation(summary = "Resubmit one withdrawn controlled file as a new BPM workflow")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:submit')")
    public CommonResult<Long> resubmitWithdrawnControlledFile(@PathVariable("id") Long id) {
        return success(workflowService.resubmitWithdrawnControlledFile(getLoginUserId(), id));
    }

    @PostMapping("/{id:\\d+}/task-action-readiness")
    @Operation(summary = "Check whether one DCC workflow task action is ready")
    @PreAuthorize("@ss.hasAnyPermissions('dcc:controlled-file:submit','dcc:controlled-file:review','dcc:controlled-file:approve')")
    public CommonResult<DccControlledFileTaskReadinessRespVO> getTaskActionReadiness(
            @PathVariable("id") Long id,
            @Valid @RequestBody DccControlledFileTaskReadinessReqVO reqVO) {
        return success(workflowService.getTaskActionReadiness(getLoginUserId(), id, reqVO));
    }

    @PostMapping("/{id:\\d+}/approve-task")
    @Operation(summary = "Approve one DCC workflow task with password signature")
    @PreAuthorize("@ss.hasAnyPermissions('dcc:controlled-file:submit','dcc:controlled-file:review','dcc:controlled-file:approve')")
    public CommonResult<DccSignatureActionRespVO> approveTask(@PathVariable("id") Long id,
                                                              @Valid @RequestBody DccControlledFileApproveTaskReqVO reqVO) {
        return success(workflowService.approveTask(getLoginUserId(), id, reqVO));
    }

    @PostMapping("/{id:\\d+}/reject-task")
    @Operation(summary = "Reject one DCC workflow task with password signature")
    @PreAuthorize("@ss.hasAnyPermissions('dcc:controlled-file:review','dcc:controlled-file:approve')")
    public CommonResult<DccSignatureActionRespVO> rejectTask(@PathVariable("id") Long id,
                                                             @Valid @RequestBody DccControlledFileRejectTaskReqVO reqVO) {
        return success(workflowService.rejectTask(getLoginUserId(), id, reqVO));
    }

    @GetMapping("/{id:\\d+}/signature-export-summary")
    @Operation(summary = "Get controlled file signature evidence export summary")
    @PreAuthorize("@ss.hasAnyPermissions('dcc:controlled-file:query','dcc:controlled-file:download')")
    public CommonResult<DccControlledFileSignatureExportSummaryRespVO> getSignatureExportSummary(
            @PathVariable("id") Long id) {
        queryService.getControlledFile(getLoginUserId(), id);
        return success(signatureManagementService.getSignatureExportSummary(id));
    }

    @PostMapping("/{id:\\d+}/signature-binding-migration")
    @Operation(summary = "Migrate historical signatures to the final controlled copy")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:signature:manage')")
    public CommonResult<DccControlledFileSignatureExportSummaryRespVO> migrateSignatureBinding(
            @PathVariable("id") Long id, HttpServletRequest request) {
        return success(signatureManagementService.migratePublishedCopyBindings(id, getLoginUserId(),
                auditContext(request, null).requireRequestId("signature binding migration")));
    }

    @GetMapping("/{id:\\d+}/signature-evidence-export")
    @Operation(summary = "Download controlled file signature evidence artifact")
    @PreAuthorize("@ss.hasAnyPermissions('dcc:controlled-file:query','dcc:controlled-file:download')")
    public ResponseEntity<byte[]> downloadSignatureEvidenceExport(@PathVariable("id") Long id) {
        queryService.getControlledFile(getLoginUserId(), id);
        DccSignatureEvidenceExportArtifact artifact = signatureManagementService.exportSignatureEvidence(id);
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(artifact.contentType()))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        org.springframework.http.ContentDisposition.attachment()
                                .filename(artifact.fileName(), StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .header(org.springframework.http.HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                        org.springframework.http.HttpHeaders.CONTENT_DISPOSITION)
                .body(artifact.bytes());
    }

    @PostMapping("/{id:\\d+}/return-task")
    @Operation(summary = "Return one DCC workflow task with password signature")
    @PreAuthorize("@ss.hasAnyPermissions('dcc:controlled-file:review','dcc:controlled-file:approve')")
    public CommonResult<Boolean> returnTask(@PathVariable("id") Long id,
                                            @Valid @RequestBody DccControlledFileReturnTaskReqVO reqVO) {
        workflowService.returnTask(getLoginUserId(), id, reqVO);
        return success(true);
    }

    @PostMapping("/{id:\\d+}/transfer-task")
    @Operation(summary = "Transfer one DCC workflow task with password signature")
    @PreAuthorize("@ss.hasAnyPermissions('dcc:controlled-file:review','dcc:controlled-file:approve')")
    public CommonResult<Boolean> transferTask(@PathVariable("id") Long id,
                                              @Valid @RequestBody DccControlledFileTransferTaskReqVO reqVO) {
        workflowService.transferTask(getLoginUserId(), id, reqVO);
        return success(true);
    }

    @PostMapping("/{id:\\d+}/sign-task")
    @Operation(summary = "Create DCC workflow sign task with password signature")
    @PreAuthorize("@ss.hasAnyPermissions('dcc:controlled-file:review','dcc:controlled-file:approve')")
    public CommonResult<Boolean> createSignTask(@PathVariable("id") Long id,
                                                @Valid @RequestBody DccControlledFileCreateSignTaskReqVO reqVO) {
        workflowService.createSignTask(getLoginUserId(), id, reqVO);
        return success(true);
    }

    @PostMapping("/{id:\\d+}/stamp-retry")
    @Operation(summary = "Retry controlled file finalization")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:stamp:retry')")
    public CommonResult<Boolean> retryControlledFileStamp(@PathVariable("id") Long id) {
        finalizationService.retryStamp(id);
        return success(true);
    }

    @PostMapping("/message-jobs/replay")
    @Operation(summary = "Replay historical DCC message jobs")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:stamp:retry')")
    public CommonResult<Integer> replayMessageJobs(@Valid @RequestBody DccControlledFileMessageJobReplayReqVO reqVO) {
        return success(messageReplayService.replayMessageJobs(reqVO));
    }

    @PostMapping("/{id:\\d+}/manual-release")
    @Operation(summary = "Manually release one training-gated controlled file distribution")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:query')")
    public CommonResult<Boolean> releaseManualDistribution(@PathVariable("id") Long id) {
        finalizationService.releaseManualDistribution(getLoginUserId(), id);
        return success(true);
    }

    @PostMapping("/{id:\\d+}/controlled-print")
    @Operation(summary = "Create one controlled print record")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:print')")
    public CommonResult<DccControlledFilePrintRecordRespVO> createControlledPrint(
            @PathVariable("id") Long id,
            @Valid @RequestBody DccControlledFilePrintCreateReqVO reqVO) {
        return success(controlledFilePrintService.createPrintRecord(getLoginUserId(), id, reqVO));
    }

    @GetMapping("/{id:\\d+}/controlled-print/records")
    @Operation(summary = "List controlled print records")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:print')")
    public CommonResult<List<DccControlledFilePrintRecordRespVO>> getControlledPrintRecords(
            @PathVariable("id") Long id) {
        return success(controlledFilePrintService.getPrintRecords(getLoginUserId(), id));
    }

    @GetMapping("/{id:\\d+}/controlled-print/print-html")
    @Operation(summary = "Build controlled print HTML with traceable metadata")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:print')")
    public CommonResult<DccControlledFilePrintHtmlRespVO> getControlledPrintHtml(
            @PathVariable("id") Long id,
            @RequestParam("printRecordId") Long printRecordId) {
        return success(controlledFilePrintService.getPrintHtml(getLoginUserId(), id, printRecordId));
    }

    @GetMapping("/{id:\\d+}/preview")
    @Operation(summary = "Preview controlled file")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> previewControlledFile(@PathVariable("id") Long id,
                                                        @RequestHeader(VIEWER_TOKEN_HEADER) String viewerToken,
                                                        @RequestHeader(ACCESS_EVENT_CODE_HEADER) String accessEventCode,
                                                        @RequestHeader(WATERMARK_TRACE_CODE_HEADER) String watermarkTraceCode,
                                                        @RequestHeader(VIEWER_TOKEN_ID_HEADER) String viewerTokenId,
                                                        @RequestHeader(VIEWER_TOKEN_NONCE_HEADER) String viewerTokenNonce,
                                                        HttpServletRequest request) {
        var binary = queryService.readPreviewFile(getLoginUserId(), id, viewerToken, accessEventCode,
                watermarkTraceCode, viewerTokenId, viewerTokenNonce, auditContext(request, accessEventCode));
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(binary.contentType()))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        contentDispositionInline(binary.fileName()))
                .header(org.springframework.http.HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                        PREVIEW_WATERMARK_HEADER + "," + ACCESS_EVENT_CODE_HEADER)
                .header(PREVIEW_WATERMARK_HEADER, encodePreviewWatermark(binary.watermark()))
                .header(ACCESS_EVENT_CODE_HEADER, accessEventCode)
                .body(binary.bytes());
    }

    @GetMapping("/{id:\\d+}/preview-metadata")
    @Operation(summary = "Get controlled file preview metadata")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<DccControlledFilePreviewMetadataRespVO> getPreviewMetadata(@PathVariable("id") Long id,
                                                                                  HttpServletRequest request) {
        return success(queryService.getPreviewMetadata(getLoginUserId(), id, auditContext(request, null)));
    }

    @GetMapping("/{id:\\d+}/onlyoffice-file")
    @TenantIgnore
    @PermitAll
    @Operation(summary = "Read one controlled file for OnlyOffice")
    public ResponseEntity<byte[]> getOnlyOfficePreviewFile(@PathVariable("id") Long id,
                                                           @RequestParam("token") String token,
                                                           HttpServletRequest request) throws Exception {
        var binary = queryService.readOnlyOfficePreviewFile(id, token, auditContext(request, null));
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(binary.contentType()))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        contentDispositionInline(binary.fileName()))
                .body(binary.bytes());
    }

    @GetMapping("/{id:\\d+}/download")
    @Operation(summary = "Download controlled file")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> downloadControlledFile(@PathVariable("id") Long id,
                                                          @RequestParam("nonControlledWarningConfirmed")
                                                          Boolean nonControlledWarningConfirmed,
                                                          @RequestParam("downloadRequestId")
                                                          String downloadRequestId,
                                                          HttpServletRequest request) {
        if (StrUtil.isBlank(downloadRequestId)) {
            throw exception(DCC_DOWNLOAD_REQUEST_ID_REQUIRED);
        }
        var binary = queryService.readDownloadFile(getLoginUserId(), id, nonControlledWarningConfirmed,
                StrUtil.trim(downloadRequestId), auditContext(request, StrUtil.trim(downloadRequestId)));
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(binary.contentType()))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        org.springframework.http.ContentDisposition.attachment()
                                .filename(binary.fileName(), StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .header(org.springframework.http.HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                        org.springframework.http.HttpHeaders.CONTENT_DISPOSITION + ","
                                + ACCESS_EVENT_CODE_HEADER + "," + DOWNLOAD_REQUEST_ID_HEADER + ","
                                + DOWNLOAD_PLAIN_SHA256_HEADER)
                .header(ACCESS_EVENT_CODE_HEADER, binary.accessEventCode())
                .header(DOWNLOAD_REQUEST_ID_HEADER, binary.downloadRequestId())
                .header(DOWNLOAD_PLAIN_SHA256_HEADER, binary.plainSha256())
                .body(binary.bytes());
    }

    @GetMapping("/{id:\\d+}/approval-print/print-html")
    @Operation(summary = "Build DCC approval process print HTML from the active template")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:query')")
    public CommonResult<DccApprovalPrintHtmlRespVO> getApprovalPrintHtml(@PathVariable("id") Long id) {
        return success(approvalPrintTemplateService.getApprovalPrintHtml(getLoginUserId(), id));
    }

    @GetMapping("/{id:\\d+}/approval-print/export-word")
    @Operation(summary = "Export DCC approval process Word from the active template")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:query')")
    public ResponseEntity<byte[]> exportApprovalWord(@PathVariable("id") Long id) {
        DccApprovalPrintRenderedWord word = approvalPrintTemplateService.exportApprovalWord(getLoginUserId(), id);
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(word.contentType()))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        org.springframework.http.ContentDisposition.attachment()
                                .filename(word.fileName(), StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .body(word.bytes());
    }

    @GetMapping("/source-ownership-migration/readiness")
    @Operation(summary = "Inspect tenant-scoped DCC formal source ownership migration readiness")
    @PreAuthorize("@ss.hasRole('doc_control') and @ss.hasPermission('dcc:controlled-file:update')")
    public CommonResult<DccControlledFileSourceMigrationReadinessRespVO> getSourceOwnershipMigrationReadiness() {
        return success(DccControlledFileSourceMigrationReadinessRespVO.from(sourceMigrationService.getReadiness()));
    }

    @PostMapping("/source-ownership-migration/run")
    @Operation(summary = "Migrate one bounded batch of tenant-scoped DCC formal source ownership records")
    @PreAuthorize("@ss.hasRole('doc_control') and @ss.hasPermission('dcc:controlled-file:update')")
    public CommonResult<DccControlledFileSourceMigrationResultRespVO> migrateSourceOwnershipBatch(
            @RequestParam(value = "batchSize", defaultValue = "100") @Min(1) @Max(200) int batchSize) {
        return success(DccControlledFileSourceMigrationResultRespVO.from(
                sourceMigrationService.migrateBatch(getLoginUserId(), batchSize)));
    }

    private String encodePreviewWatermark(DccControlledPreviewWatermarkRespVO watermark) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(JsonUtils.toJsonString(watermark).getBytes(StandardCharsets.UTF_8));
    }

    private String contentDispositionInline(String fileName) {
        return org.springframework.http.ContentDisposition.inline()
                .filename(fileName, StandardCharsets.UTF_8)
                .build()
                .toString();
    }

    private DccRequestAuditContext auditContext(HttpServletRequest request, String explicitRequestId) {
        return DccRequestAuditContext.from(request, explicitRequestId);
    }

    private DccControlledFileUploadTemporaryStatusRespVO toTemporaryStatusRespVO(
            DccUploadTemporaryFileStatus status, Integer cleanedCount) {
        DccControlledFileUploadTemporaryStatusRespVO respVO = new DccControlledFileUploadTemporaryStatusRespVO();
        respVO.setRequestId(status.requestId());
        respVO.setTemporaryFileCount(status.temporaryFileCount());
        respVO.setBindable(status.bindable());
        respVO.setSessionId(status.sessionId());
        respVO.setPurpose(status.purpose());
        respVO.setStatus(status.status());
        respVO.setExpireTime(status.expireTime());
        respVO.setCleanupStatus(status.cleanupStatus());
        respVO.setCleanupReason(status.cleanupReason());
        respVO.setCleanupTime(status.cleanupTime());
        respVO.setCleanedCount(cleanedCount);
        return respVO;
    }

    @PostMapping("/{id:\\d+}/obsolete")
    @Operation(summary = "Obsolete an active controlled file")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:query')")
    public CommonResult<FormInstanceRespVO> obsoleteControlledFile(@PathVariable("id") Long id,
                                                        @Valid @RequestBody DccControlledFileObsoleteReqVO reqVO) {
        return success(obsoleteService.obsoleteControlledFile(getLoginUserId(), id, reqVO));
    }

    @PostMapping("/{id:\\d+}/publish")
    @Operation(summary = "Publish an approved revision candidate")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:query')")
    public CommonResult<FormInstanceRespVO> publishControlledFile(@PathVariable("id") Long id,
                                                                  @Valid @RequestBody DccControlledFilePublishReqVO reqVO) {
        return success(publishService.publishControlledFile(getLoginUserId(), id, reqVO));
    }

    @PostMapping("/{id:\\d+}/training-acknowledge")
    @Operation(summary = "Acknowledge controlled file training")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:query')")
    public CommonResult<Boolean> acknowledgeTraining(@PathVariable("id") Long id) {
        trainingAssignmentAckService.acknowledgeTraining(getLoginUserId(), id);
        return success(true);
    }
}
