package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileActionProjectionRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePreviewMetadataRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileAccessExplanationRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileUploadNameOptionRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileDistributionRecipientStatusRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileDistributionStatusRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileCheckoutReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileCheckinReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileCancelCheckoutReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRouteSnapshotRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSignatureSummaryRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileTrainingAssignmentRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileTrainingStatusRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileUploadDirectoryTreeRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileVersionHistoryRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccExternalFileReviewRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccCategoryDirectoryBindingDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccFileDirectoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileAccessLogDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileCheckoutDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceOwnershipDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRouteSnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTaskAssigneeSnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSignatureDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTrainingAssignmentDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTrainingDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTrainingProgressDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccExternalFileReviewDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileAccessEventDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileDownloadRecordDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileWatermarkTraceDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccCategoryDirectoryBindingMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccFileDirectoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileAccessLogMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionRecipientMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileCheckoutMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSourceOwnershipMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileRouteSnapshotMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTaskAssigneeSnapshotMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSignatureMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTrainingAssignmentMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTrainingProgressMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTrainingMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccExternalFileReviewMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileAccessEventMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileDownloadRecordMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileWatermarkTraceMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.enums.DccAccessResultEnum;
import cn.iocoder.yudao.module.dcc.enums.DccAccessTypeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileChangeTypeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileProcessTypeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStageCodeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFilePreviewKindEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileTrainingStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.dcc.enums.DccProjectCodeStatusConstants;
import cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessPermissionService;
import cn.iocoder.yudao.module.dcc.service.download.DccDownloadFileBinary;
import cn.iocoder.yudao.module.dcc.service.download.DccDownloadPolicyContext;
import cn.iocoder.yudao.module.dcc.service.download.DccDownloadPolicyDecision;
import cn.iocoder.yudao.module.dcc.service.download.DccDownloadPolicyService;
import cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyAdminService;
import cn.iocoder.yudao.module.dcc.service.audit.DccControlledFileAccessAuditService;
import cn.iocoder.yudao.module.dcc.service.audit.DccLifecycleLogCreateCommand;
import cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyPath;
import cn.iocoder.yudao.module.dcc.service.preview.DccControlledPreviewAccessService;
import cn.iocoder.yudao.module.dcc.service.preview.DccPreviewAccessRequest;
import cn.iocoder.yudao.module.dcc.service.preview.DccPreviewAccessResult;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.dcc.service.token.DccViewerTokenExpectedContext;
import cn.iocoder.yudao.module.dcc.service.token.DccViewerTokenService;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketBoundFile;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketMarkBoundCommand;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketResolveCommand;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketService;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessOperation;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessDeniedException;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessReference;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessRequest;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import jakarta.annotation.Resource;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.task.api.Task;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_UPLOAD_TICKET_INVALID;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ACCESS_DENIED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ALREADY_CHECKED_OUT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_CHECKIN_NOT_OWNER;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_NOT_CHECKED_OUT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_CHECKOUT_REASON_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_CHECKOUT_NOT_ALLOWED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_DRAWING_PDF_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_DRAWING_PDF_FILE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_CHECKIN_REQUEST_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_CHECKIN_NO_CHANGE;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_CHECKIN_NOT_ALLOWED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ALREADY_CHECKED_OUT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_CHECKIN_NOT_OWNER;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_NOT_CHECKED_OUT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_CATEGORY_DISABLED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_DRAWING_PDF_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_DOWNLOAD_WARNING_UNCONFIRMED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ONLYOFFICE_PREVIEW_CONFIG_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_VIEWER_TOKEN_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_WORKFLOW_IN_PROGRESS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_DOWNLOAD_AUDIT_RECORD_FAILED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_DOWNLOAD_REQUEST_ID_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_DOWNLOAD_REQUEST_ID_REUSED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_ACCESS_DENIED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_CATEGORY_DIRECTORY_BINDING_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_CATEGORY_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_DIRECTORY_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_TYPE_TAXONOMY_LEVEL_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_CODE_DISABLED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_CODE_NOT_EXISTS;

@Service
@Validated
public class DccControlledFileQueryServiceImpl implements DccControlledFileQueryService {
    private final ThreadLocal<DccDistributionPreviewContext> distributionPreviewContext = new ThreadLocal<>();
    private final ThreadLocal<DccControlledFileBodyReadContext> bodyReadContext = new ThreadLocal<>();
    private record AttachmentBusinessRead(Long userId, Long controlledFileId, Long storageFileId) {}
    private final ThreadLocal<AttachmentBusinessRead> attachmentBusinessRead = new ThreadLocal<>();
    @Resource private cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMessageJobMapper messageJobMapper;
    @Resource private DccDistributionReceiptService distributionReceiptService;
    @Resource @org.springframework.context.annotation.Lazy
    private cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationStageSnapshotService relationStageSnapshotService;
    @org.springframework.context.annotation.Lazy
    @Resource
    private cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService bpmProcessInstanceService;
    @Resource private DccPublicUploadPlacementService publicUploadPlacementService;

    private static final String PREVIEW_ACCESS_TYPE = "PREVIEW";
    private static final String CONTROLLED_PREVIEW_PURPOSE = "CONTROLLED_PREVIEW";
    private static final String DOWNLOAD_ACCESS_TYPE = "DOWNLOAD";
    private static final String CONTROLLED_DOWNLOAD_PURPOSE = "CONTROLLED_DOWNLOAD";
    private static final String ACCESS_RESULT_SUCCESS = "SUCCESS";
    private static final String OFFICE_READ_ACTION_TYPE = "OFFICE_READ";
    private static final String DOWNLOAD_RECORD_STATUS_REQUESTED = "REQUESTED";
    private static final String DOWNLOAD_RECORD_STATUS_READY = "READY";
    private static final String DOWNLOAD_RECORD_STATUS_FAILED = "FAILED";
    private static final String FAILURE_SOURCE_READ_FAILED = "SOURCE_READ_FAILED";
    private static final String FAILURE_AUDIT_RECORD_FAILED = "AUDIT_RECORD_FAILED";
    private static final String PREVIEW_UNAVAILABLE_CONTENT_TYPE = "application/octet-stream";
    private static final long PREVIEW_VIEWER_TOKEN_TTL_SECONDS = 900L;
    private static final DateTimeFormatter DOWNLOAD_EVENT_CODE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")
            .withZone(ZoneOffset.UTC);
    private static final String ACTION_VIEW = "VIEW";
    private static final String ACTION_PREVIEW = "PREVIEW";
    private static final String ACTION_DOWNLOAD = "DOWNLOAD";
    private static final String ACTION_PRINT = "PRINT";
    private static final String ACTION_WITHDRAW = "WITHDRAW";
    private static final String ACTION_OBSOLETE = "OBSOLETE";
    private static final String ACTION_PUBLISH = "PUBLISH";
    private static final String ACTION_MANUAL_RELEASE = "MANUAL_RELEASE";
    private static final String ACTION_DELETE_WITHDRAWN_FLOW = "DELETE_WITHDRAWN_FLOW";
    private static final String ACTION_RESUBMIT_WITHDRAWN_FLOW = "RESUBMIT_WITHDRAWN_FLOW";
    private static final String ACTION_UPLOAD_TRAINING_RECORD = "UPLOAD_TRAINING_RECORD";
    private static final String ACTION_ACKNOWLEDGE_TRAINING = "ACKNOWLEDGE_TRAINING";
    private static final String ACTION_RETRY_FINALIZATION = "RETRY_FINALIZATION";
    private static final String ACTION_MAJOR_REVISION = "MAJOR_REVISION";
    private static final String APPROVE_PERMISSION = "dcc:controlled-file:approve";

    @Resource
    private DccFileCategoryMapper categoryMapper;
    @Resource
    private DccCategoryDirectoryBindingMapper categoryDirectoryBindingMapper;
    @Resource
    private DccFileDirectoryMapper directoryMapper;
    @Resource
    private DccControlledFileMapper controlledFileMapper;
    @Resource private cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSelectorMapper selectorMapper;
    @Resource private cn.iocoder.yudao.module.dcc.dal.mysql.file.DccProjectBrowserMapper projectBrowserMapper;
    @Resource private cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileApplicationEvidenceMapper applicationEvidenceMapper;
    @Resource
    private DccControlledFileCheckoutMapper checkoutMapper;
    @Resource
    private DccControlledFileMasterMapper controlledFileMasterMapper;
    @Resource
    private DccControlledFileNameClaimService nameClaimService;
    @Resource
    private DccControlledFileRouteSnapshotMapper routeSnapshotMapper;
    @Resource
    private DccControlledFileTaskAssigneeSnapshotMapper taskAssigneeSnapshotMapper;
    @Resource
    private DccControlledFileDistributionMapper distributionMapper;
    @Resource
    private DccControlledFileDistributionRecipientMapper distributionRecipientMapper;
    @Resource
    private DccControlledFileTrainingMapper trainingMapper;
    @Resource
    private DccControlledFileTrainingAssignmentMapper trainingAssignmentMapper;
    @Resource
    private DccControlledFileTrainingProgressMapper trainingProgressMapper;
    @Resource
    private DccControlledFileSignatureMapper signatureMapper;
    @Resource
    private DccControlledFileSignatureBindingService signatureBindingService;
    @Resource
    private DccControlledFileRelatedFileService relatedFileService;
    @Resource
    private DccControlledFileAttachmentService attachmentService;
    @Resource
    private DccControlledFileVersionPolicy versionPolicy = DccControlledFileVersionPolicy.defaultPolicy();
    @Resource
    private DccExternalFileReviewMapper externalReviewMapper;
    @Resource
    private DccControlledFileAccessLogMapper accessLogMapper;
    @Resource
    private DccControlledFileAccessEventMapper accessEventMapper;
    @Resource
    private DccControlledFileDownloadRecordMapper downloadRecordMapper;
    @Resource
    private DccControlledFileWatermarkTraceMapper watermarkTraceMapper;
    @Resource
    private PlatformTransactionManager transactionManager;
    @Resource
    private DccControlledFileCategoryPermissionSupport permissionSupport;
    @Resource
    private DccControlledFileFormActionPendingService formActionPendingService;
    @Resource
    private DccControlledFileViewMatrixAccessService viewMatrixAccessService;
    @Resource
    private DccDirectoryAccessPermissionService directoryAccessPermissionService;
    @Resource
    private FileMapper fileMapper;
    @Resource
    private FileService fileService;
    @Resource
    private DccControlledFileSourceOwnershipMapper sourceOwnershipMapper;
    @Resource
    private DccControlledFileSourceOwnershipService sourceOwnershipService;
    @Resource
    private DccUploadTicketService uploadTicketService;
    @Resource private cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileAttachmentMapper checkinAttachmentMapper;
    @Resource private cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileTemporaryFileMapper checkinTemporaryFileMapper;
    @Resource
    private DccControlledPreviewWatermarkService watermarkService;
    @Resource
    private DccControlledPreviewAccessService previewAccessService;
    @Resource
    private DccViewerTokenService viewerTokenService;
    @Resource
    private DccOnlyOfficePreviewProperties onlyOfficePreviewProperties;
    @Resource
    private DccOnlyOfficePreviewTokenService onlyOfficePreviewTokenService;
    @Resource
    private DccDownloadPolicyService downloadPolicyService;
    @Resource
    private DccControlledFileBrowserSettingsService browserSettingsService;
    @Resource
    private DccControlledFileAssignmentScopeService assignmentScopeService;
    @Resource
    private DccOfflineTrainingRecordService offlineTraining;
    @Resource
    private DccControlledFileDetailAuthorizationGuard detailAuthorizationGuard;
    @Resource
    private DccProjectAccessService projectAccessService;
    @Resource
    private DccProjectCodeMapper projectCodeMapper;
    @Resource private cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectFilePlacementMapper projectFilePlacementMapper;
    @Resource private cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectFolderMapper projectFolderMapper;
    @Resource private cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectFolderStorageMappingMapper projectStorageMappingMapper;

    @Resource
    private DccWorkingApplicationDraftInitializer workingApplicationDraftInitializer;
    @Resource
    private DccApplicationRoundService applicationRoundService;
    @Resource
    private DccApplicationExecutionContractService executionContracts;
    @Resource
    private DccBoundWorkflowContractResolver roundFlowContracts;
    @Resource
    private DccFileTypeTaxonomyAdminService fileTypeTaxonomyAdminService;
    @Resource
    private DccControlledFileAccessAuditService lifecycleAuditService;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private BusinessFileAccessService businessFileAccessService;
    @Resource
    private BpmTaskService bpmTaskService;
    @Resource private DccApplicationHistoryGuard applicationHistoryGuard;

    @Override
    public PageResult<DccControlledFileRespVO> getControlledFilePage(Long userId, DccControlledFilePageReqVO reqVO) {
        rejectWorkingBrowserScopeOnOtherEndpoint(reqVO);
        Set<Long> requestedDirectoryIds = resolveRequestedDirectoryIds(reqVO);
        DccControlledFilePageReqVO candidateReqVO = buildCandidateReqForWorkflowSearch(reqVO);
        boolean hasDirectoryManagementPermission = directoryAccessPermissionService.hasDirectoryManagementPermission(userId);
        List<DccControlledFileDO> candidates;
        if (requestedDirectoryIds != null) {
            if (requestedDirectoryIds.isEmpty()) {
                return PageResult.empty(0L);
            }
            candidates = controlledFileMapper.selectWorkflowList(buildPageReqWithoutDirectory(candidateReqVO), requestedDirectoryIds);
        } else {
            candidates = controlledFileMapper.selectWorkflowList(candidateReqVO);
        }
        List<DccControlledFileDO> visibleFiles = candidates.stream()
                .filter(file -> canAccessQuery(userId, file, reqVO, hasDirectoryManagementPermission))
                .toList();
        if (reqVO.getDccProjectCodeId() != null) {
            List<String> blacklistedExtensionPatterns = browserSettingsService.getBlacklistedExtensionPatterns();
            visibleFiles = visibleFiles.stream()
                    .filter(file -> !isBlacklistedBrowserExtension(file, blacklistedExtensionPatterns))
                    .toList();
        }
        if (Boolean.TRUE.equals(reqVO.getLatestVersionOnly())) {
            visibleFiles = StrUtil.isBlank(reqVO.getStatus()) ? aggregateLatestControlledVisibleFiles(visibleFiles)
                    : aggregateLatestVisibleFiles(visibleFiles);
            if (StrUtil.isBlank(reqVO.getStatus())) {
                visibleFiles = visibleFiles.stream().filter(file -> hasReadableProjectScope(userId, file)).toList();
            }
        }
        if (shouldFilterKeywordAfterLatestAggregation(reqVO)) {
            visibleFiles = filterByKeyword(visibleFiles, reqVO.getKeyword());
        }
        long total = visibleFiles.size();
        List<DccControlledFileDO> pageFiles = sliceRows(reqVO, visibleFiles);
        Map<Long, DccFileDirectoryDO> directoryMap = buildEnabledDirectoryMap();
        List<DccControlledFileRespVO> visibleRows = pageFiles.stream()
                .map(file -> toRespVO(userId, file, false, hasDirectoryManagementPermission, directoryMap))
                .toList();
        return new PageResult<>(visibleRows, total);
    }
    @Override
    @Transactional(readOnly=true, isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public PageResult<DccControlledFileRespVO> getControlledFileBrowserPage(Long userId, DccControlledFilePageReqVO reqVO) {
        if (hasWorkingBrowserScope(reqVO) && (reqVO.getBrowserScope()!=null || reqVO.getSelectorScope()!=null
                || reqVO.getProjectFolderId()!=null))
            throw new IllegalArgumentException("exact working navigation requires the normal storage browser");
        if (reqVO.getBrowserScope()!=null) return getProjectBrowserPage(userId,reqVO);
        if (reqVO.getSelectorScope()!=null || reqVO.getProjectFolderId()!=null) {
            if (reqVO.getSelectorScope()==null || !Set.of("GLOBAL","PROJECT_FOLDER").contains(reqVO.getSelectorScope())
                    || "PROJECT_FOLDER".equals(reqVO.getSelectorScope()) && (reqVO.getDccProjectCodeId()==null || reqVO.getProjectFolderId()==null)
                    || "GLOBAL".equals(reqVO.getSelectorScope()) && reqVO.getProjectFolderId()!=null
                    || reqVO.getDirectoryId()!=null || StrUtil.isNotBlank(reqVO.getStatus()))
                throw new IllegalArgumentException("invalid selector scope or mixed NAS/status filter");
            var page = getControlledFileSelectorPage(userId,new DccControlledFileSelectorQuery(reqVO.getDccProjectCodeId(),null,
                    reqVO.getProjectFolderId(),reqVO.getKeyword(),reqVO.getPageNo(),reqVO.getPageSize()));
            return new PageResult<>(page.getList().stream().map(row -> {
                var response=new DccControlledFileRespVO();response.setId(row.controlledFileId());response.setTenantId(row.tenantId());
                response.setMasterId(row.masterId());response.setLatestControlledFileId(row.latestControlledFileId());
                response.setCurrentActiveControlledFileId(row.currentActiveControlledFileId());
                response.setDccProjectCodeId(row.projectId());response.setProjectName(row.projectName());
                response.setProjectFolderId(row.projectFolderId());response.setProjectFolderName(row.projectFolderName());
                response.setFileName(row.fileName());response.setSourceOriginalFileName(row.fileName());response.setFileNumber(row.fileNumber());
                response.setVersionNo(row.versionNo());response.setStatus(row.status());response.setControlled(row.controlled());
                response.setPendingEffect(row.pendingEffect());response.setExecutable(row.executable());
                response.setCanPreview(row.canPreview());response.setCanDownload(false);response.setCanPrint(false);
                return response;
            }).toList(),page.getTotal());
        }
        List<String> blacklistedExtensionPatterns = browserSettingsService.getBlacklistedExtensionPatterns();
        boolean hasDirectoryManagementPermission = directoryAccessPermissionService.hasDirectoryManagementPermission(userId);
        Map<Long, Boolean> currentViewMatrixAccessByCategory = new LinkedHashMap<>();
        Set<Long> activeAssignedControlledFileIds = resolveActiveAssignedControlledFileIds(userId);
        List<DccControlledFileDO> visibleFiles = listControlledFileBrowserCandidates(userId, reqVO,
                blacklistedExtensionPatterns, hasDirectoryManagementPermission, true,
                currentViewMatrixAccessByCategory, activeAssignedControlledFileIds);
        long total = visibleFiles.size();
        Map<Long, DccFileDirectoryDO> directoryMap = buildEnabledDirectoryMap();
        List<DccControlledFileRespVO> visibleRows = sliceRows(reqVO, visibleFiles).stream()
                .map(file -> {
                    var response = toBrowserRespVO(userId, file, hasDirectoryManagementPermission,
                            blacklistedExtensionPatterns, currentViewMatrixAccessByCategory, directoryMap);
                    if (!hasWorkingBrowserScope(reqVO)) restrictDailyBodyProjection(response, file);
                    return response;
                })
                .toList();
        return new PageResult<>(visibleRows, total);
    }

    private PageResult<DccControlledFileRespVO> getProjectBrowserPage(Long userId,DccControlledFilePageReqVO request) {
        if (userId==null) throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        if (!Set.of("GLOBAL","PROJECT_FOLDER").contains(request.getBrowserScope()) || request.getSelectorScope()!=null
                || "PROJECT_FOLDER".equals(request.getBrowserScope()) && (request.getDccProjectCodeId()==null || request.getProjectFolderId()==null)
                || "GLOBAL".equals(request.getBrowserScope()) && request.getProjectFolderId()!=null
                || request.getDirectoryId()!=null || request.getIncludeDescendantDirectories()!=null
                || request.getPageNo()<1 || request.getPageSize()<1 || request.getPageSize()>200
                || request.getDccProjectCodeId()!=null && request.getDccProjectCodeId()<1
                || request.getProjectFolderId()!=null && request.getProjectFolderId()<1
                || request.getFileTypeTaxonomyIds()!=null || request.getFileTypeTaxonomyPaths()!=null
                || request.getQuickFilter()!=null || request.getRecognitionStatus()!=null || request.getBatchRecognitionTaskId()!=null)
            throw new IllegalArgumentException("invalid full browser scope or unsupported mixed filter");
        Long tenant=TenantContextHolder.getRequiredTenantId();
        boolean manager=directoryAccessPermissionService.hasDirectoryManagementPermission(userId);
        var matrix=new LinkedHashMap<Long,Boolean>();
        var blacklist=browserSettingsService.getBlacklistedExtensionPatterns();
        List<Long> authorized=projectBrowserMapper.selectCandidates(tenant,request).stream()
                .filter(file -> Objects.equals(tenant,file.getTenantId()) && isWithinAssignedFileScope(userId,file)
                        && hasReadableProjectScope(userId, file)
                        && canAccessQuery(userId,file,request,manager,matrix) && !isBlacklistedBrowserExtension(file,blacklist))
                .map(DccControlledFileDO::getId).toList();
        if (authorized.isEmpty()) return PageResult.empty(0L);
        long total=projectBrowserMapper.countAuthorized(tenant,request,authorized);
        var directoryMap=buildEnabledDirectoryMap();
        var rows=projectBrowserMapper.selectAuthorizedPage(tenant,request,authorized,request.getPageSize(),
                Math.multiplyExact((long)request.getPageNo()-1,request.getPageSize())).stream().map(file -> {
            if (!authorized.contains(file.getId()) || !Objects.equals(tenant,file.getTenantId()))
                throw new IllegalStateException("full browser returned an unauthorized identity");
            var row=toBrowserRespVO(userId,file,manager,blacklist,matrix,directoryMap);
            row.setTenantId(tenant);row.setLatestControlledFileId(file.getLatestControlledFileId());
            row.setCurrentActiveControlledFileId(file.getCurrentActiveControlledFileId());
            row.setProjectName(file.getProjectName());row.setProjectFolderId(file.getProjectFolderId());row.setProjectFolderName(file.getProjectFolderName());
            row.setSourceOriginalFileName(file.getSourceOriginalFileName());row.setRevisionChangeType(file.getRevisionChangeType());
            row.setControlled(file.getControlledTime()!=null && DccControlledFileVersionPolicy.isCurrentControlledStatus(file.getStatus()));
            row.setPendingEffect("CONTROLLED_PENDING_EFFECTIVE".equals(file.getStatus()));
            row.setExecutable("ACTIVE".equals(file.getStatus()) && file.getActivatedTime()!=null);
            row.setEffectiveDate(file.getEffectiveDate());row.setControlledTime(file.getControlledTime());row.setActivatedTime(file.getActivatedTime());
            restrictDailyBodyProjection(row, file);
            return row;
        }).toList();
        return new PageResult<>(rows,total);
    }

    @Override
    public List<DccControlledFileDO> listControlledFileBrowserCandidates(Long userId, DccControlledFilePageReqVO reqVO) {
        return listControlledFileBrowserCandidates(userId, reqVO,
                browserSettingsService.getBlacklistedExtensionPatterns());
    }

    private List<DccControlledFileDO> listControlledFileBrowserCandidates(Long userId, DccControlledFilePageReqVO reqVO,
                                                                          List<String> blacklistedExtensionPatterns) {
        boolean hasDirectoryManagementPermission = directoryAccessPermissionService.hasDirectoryManagementPermission(userId);
        Set<Long> activeAssignedControlledFileIds = resolveActiveAssignedControlledFileIds(userId);
        return listControlledFileBrowserCandidates(userId, reqVO, blacklistedExtensionPatterns,
                hasDirectoryManagementPermission, false, new LinkedHashMap<>(), activeAssignedControlledFileIds);
    }

    private List<DccControlledFileDO> listControlledFileBrowserCandidates(Long userId, DccControlledFilePageReqVO reqVO,
                                                                           List<String> blacklistedExtensionPatterns,
                                                                           boolean hasDirectoryManagementPermission,
                                                                           boolean browserSummaryQuery,
                                                                           Map<Long, Boolean> currentViewMatrixAccessByCategory,
                                                                           Set<Long> activeAssignedControlledFileIds) {
        Set<Long> requestedDirectoryIds = resolveRequestedDirectoryIds(reqVO);
        DccControlledFilePageReqVO candidateReqVO = buildCandidateReqForWorkflowSearch(reqVO);
        boolean exactOperationView = hasWorkingBrowserScope(reqVO);
        boolean latestControlledView = !exactOperationView && StrUtil.isBlank(reqVO.getStatus())
                && (browserSummaryQuery ? !Boolean.FALSE.equals(reqVO.getLatestVersionOnly())
                : Boolean.TRUE.equals(reqVO.getLatestVersionOnly()));
        if (latestControlledView || exactOperationView) {
            candidateReqVO = copyPageReq(candidateReqVO);
            candidateReqVO.setLatestVersionOnly(false);
            candidateReqVO.setKeyword(null);
        }
        List<DccControlledFileDO> candidates;
        if (hasWorkingBrowserScope(reqVO)) {
            if (!browserSummaryQuery) throw new IllegalArgumentException("exact working navigation is not supported by this endpoint");
            Set<Long> exactIds = resolveWorkingBrowserCandidateIds(reqVO);
            if (activeAssignedControlledFileIds != null && !activeAssignedControlledFileIds.isEmpty()) {
                exactIds.retainAll(activeAssignedControlledFileIds);
                if (exactIds.isEmpty()) return List.of();
            }
            if (requestedDirectoryIds != null && requestedDirectoryIds.isEmpty()) return List.of();
            candidates = controlledFileMapper.selectBrowserSummaryList(
                    requestedDirectoryIds == null ? candidateReqVO : buildPageReqWithoutDirectory(candidateReqVO),
                    requestedDirectoryIds, exactIds);
        } else if (requestedDirectoryIds != null) {
            if (requestedDirectoryIds.isEmpty()) {
                return List.of();
            }
            DccControlledFilePageReqVO directoryReqVO = buildPageReqWithoutDirectory(candidateReqVO);
            candidates = browserSummaryQuery
                    ? activeAssignedControlledFileIds == null || activeAssignedControlledFileIds.isEmpty()
                        ? controlledFileMapper.selectBrowserSummaryList(directoryReqVO, requestedDirectoryIds)
                        : controlledFileMapper.selectBrowserSummaryList(directoryReqVO, requestedDirectoryIds,
                                activeAssignedControlledFileIds)
                    : activeAssignedControlledFileIds == null || activeAssignedControlledFileIds.isEmpty()
                        ? controlledFileMapper.selectWorkflowList(directoryReqVO, requestedDirectoryIds)
                        : controlledFileMapper.selectWorkflowList(directoryReqVO, requestedDirectoryIds,
                                activeAssignedControlledFileIds);
        } else {
            candidates = browserSummaryQuery
                    ? activeAssignedControlledFileIds == null || activeAssignedControlledFileIds.isEmpty()
                        ? controlledFileMapper.selectBrowserSummaryList(candidateReqVO)
                        : controlledFileMapper.selectBrowserSummaryList(candidateReqVO, null,
                                activeAssignedControlledFileIds)
                    : activeAssignedControlledFileIds == null || activeAssignedControlledFileIds.isEmpty()
                        ? controlledFileMapper.selectWorkflowList(candidateReqVO)
                        : controlledFileMapper.selectWorkflowList(candidateReqVO, null,
                                activeAssignedControlledFileIds);
        }
        List<DccControlledFileDO> visibleFiles = candidates.stream()
                .filter(file -> activeAssignedControlledFileIds == null
                        ? canAccessQuery(userId, file, reqVO, hasDirectoryManagementPermission,
                        currentViewMatrixAccessByCategory)
                        : activeAssignedControlledFileIds.isEmpty()
                        ? canAccessQuery(userId, file, reqVO, false, currentViewMatrixAccessByCategory)
                        : isActiveAssignedControlledFile(file, activeAssignedControlledFileIds)
                        && canAccessQuery(userId, file, reqVO, hasDirectoryManagementPermission,
                        currentViewMatrixAccessByCategory))
                .filter(file -> !isBlacklistedBrowserExtension(file, blacklistedExtensionPatterns))
                .toList();
        visibleFiles = latestControlledView ? aggregateLatestControlledVisibleFiles(visibleFiles)
                : aggregateLatestVisibleFiles(visibleFiles);
        if (latestControlledView) {
            visibleFiles = visibleFiles.stream().filter(file -> hasReadableProjectScope(userId, file)).toList();
        }
        visibleFiles = filterByKeyword(visibleFiles, reqVO.getKeyword());
        return visibleFiles;
    }

    private Set<Long> resolveActiveAssignedControlledFileIds(Long userId) {
        return assignmentScopeService.resolveActiveAssignedControlledFileIds(userId);
    }

    private boolean hasWorkingBrowserScope(DccControlledFilePageReqVO request) {
        return request.getWorkingFileId()!=null || request.getWorkingMasterId()!=null;
    }

    private void rejectWorkingBrowserScopeOnOtherEndpoint(DccControlledFilePageReqVO request) {
        if (hasWorkingBrowserScope(request))
            throw new IllegalArgumentException("exact working navigation is only supported by the storage browser page");
    }

    private Set<Long> resolveWorkingBrowserCandidateIds(DccControlledFilePageReqVO request) {
        Long fileId=request.getWorkingFileId(), masterId=request.getWorkingMasterId();
        if (fileId==null || masterId==null || fileId<=0 || masterId<=0)
            throw new IllegalArgumentException("exact working file and Master identity must both be positive");
        Long tenant=TenantContextHolder.getRequiredTenantId();
        var file=controlledFileMapper.selectById(fileId);
        var master=controlledFileMasterMapper.selectById(masterId);
        if (file==null || master==null || Boolean.TRUE.equals(file.getDeleted()) || Boolean.TRUE.equals(master.getDeleted())
                || !Objects.equals(fileId,file.getId()) || !Objects.equals(masterId,master.getId())
                || !Objects.equals(tenant,file.getTenantId()) || !Objects.equals(tenant,master.getTenantId())
                || !Objects.equals(masterId,file.getMasterId())
                || !Objects.equals(master.getDccProjectCodeId(),file.getDccProjectCodeId()))
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        var chain=Objects.requireNonNull(controlledFileMapper.selectListByMasterId(masterId),"exact working version chain");
        var ids=new LinkedHashSet<Long>();
        for (var row:chain) {
            if (row==null || row.getId()==null || !Objects.equals(tenant,row.getTenantId())
                    || !Objects.equals(masterId,row.getMasterId()) || Boolean.TRUE.equals(row.getDeleted()))
                throw new IllegalStateException("exact working version chain identity is invalid");
            if (!ids.add(row.getId())) throw new IllegalStateException("exact working version identity is duplicated");
        }
        if (!ids.contains(fileId)) throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        return ids; // Candidate scope only. Original row authorization and aggregation execute below.
    }

    private boolean isActiveAssignedControlledFile(DccControlledFileDO file, Set<Long> activeAssignedControlledFileIds) {
        return file != null
                && file.getId() != null
                && activeAssignedControlledFileIds != null
                && activeAssignedControlledFileIds.contains(file.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public DccControlledFileRespVO getControlledFile(Long userId, Long id) {
        DccControlledFileDO file = controlledFileMapper.selectById(id);
        if (file == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        if (!canAccessDetail(userId, file)) {
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }
        return toRespVO(userId, file, true);
    }

    @Override
    public DccControlledFileRespVO checkoutControlledFile(Long userId, Long id) {
        throw exception(CONTROLLED_FILE_CHECKOUT_REASON_REQUIRED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DccControlledFileRespVO checkoutControlledFile(Long userId, Long id,
                                                           DccControlledFileCheckoutReqVO reqVO) {
        return executeLifecycleAudit(userId, id, "CHECKOUT", reqVO == null ? null : reqVO.getReason(),
                () -> doCheckoutControlledFile(userId, id, reqVO));
    }

    private DccControlledFileRespVO doCheckoutControlledFile(Long userId, Long id,
                                                              DccControlledFileCheckoutReqVO reqVO) {
        if (reqVO == null || StrUtil.isBlank(reqVO.getReason())) {
            throw exception(CONTROLLED_FILE_CHECKOUT_REASON_REQUIRED);
        }
        DccControlledFileDO file = requireCheckoutAccessibleControlledFile(userId, id);
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        DccControlledFileMasterDO master = file.getMasterId() == null ? null
                : controlledFileMasterMapper.selectByIdForUpdate(file.getMasterId());
        if (master == null || !Objects.equals(tenantId, master.getTenantId()) || !Objects.equals(tenantId, file.getTenantId())) {
            throw exception(CONTROLLED_FILE_CHECKOUT_NOT_ALLOWED);
        }
        file = controlledFileMapper.selectByIdAndTenantForUpdate(tenantId, id);
        if (file == null || !Objects.equals(master.getId(), file.getMasterId())) {
            throw exception(CONTROLLED_FILE_CHECKOUT_NOT_ALLOWED);
        }
        if (!matchesLockedFormalIdentity(file, master) || !canCheckoutLockedVersion(userId, file, master)) {
            throw exception(CONTROLLED_FILE_CHECKOUT_NOT_ALLOWED);
        }
        rejectWhenMasterHasOtherUnfinishedWorkflow(master.getId(), file);
        assertCanMutateControlledFile(userId, file);
        DccControlledFileCheckoutDO active = checkoutMapper.selectActiveByMasterId(tenantId, master.getId());
        if (active != null) {
            if (Objects.equals(active.getActorId(), userId)
                    && Objects.equals(active.getBaseIterationId(), id)) {
                file.setCheckedOutBy(userId);
                file.setCheckedOutTime(active.getCreateTime());
                file.setCheckedOutReason(active.getReason());
                return toBrowserRespVO(userId, file);
            }
            throw exception(CONTROLLED_FILE_ALREADY_CHECKED_OUT, active.getActorId());
        }
        if (file.getCheckedOutBy() != null) {
            throw exception(CONTROLLED_FILE_ALREADY_CHECKED_OUT, file.getCheckedOutBy());
        }
        String baseSourceSha256 = resolveSourceSha256(file);
        DccControlledFileCheckoutDO checkout = DccControlledFileCheckoutDO.builder()
                .masterId(master.getId())
                .baseIterationId(file.getId())
                .actorId(userId)
                .reason(StrUtil.trim(reqVO.getReason()))
                .baseSourceSha256(baseSourceSha256)
                .status("ACTIVE")
                .build();
        checkout.setTenantId(tenantId);
        if (checkoutMapper.insert(checkout) != 1 || checkout.getId() == null) {
            throw exception(CONTROLLED_FILE_CHECKOUT_NOT_ALLOWED);
        }
        int claimed = DccControlledFileStatusEnum.WITHDRAWN.getStatus().equals(file.getStatus())
                ? controlledFileMapper.checkoutCancelledNativeRevisionWhenAvailable(tenantId, id, userId,
                        file.getProcessInstanceId(), checkout.getReason())
                : controlledFileMapper.checkoutByIdAndTenantWhenAvailable(tenantId, id, userId, checkout.getReason());
        if (claimed != 1) {
            throw exception(CONTROLLED_FILE_ALREADY_CHECKED_OUT, userId);
        }
        file.setCheckedOutBy(userId);
        file.setCheckedOutTime(LocalDateTime.now());
        file.setCheckedOutReason(checkout.getReason());
        return toBrowserRespVO(userId, file);
    }

    @Override
    public DccControlledFileRespVO checkinControlledFile(Long userId, Long id) {
        throw exception(CONTROLLED_FILE_CHECKIN_REQUEST_INVALID);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DccControlledFileRespVO checkinControlledFile(Long userId, Long id,
                                                          DccControlledFileCheckinReqVO reqVO) {
        return executeLifecycleAudit(userId, id, "CHECKIN",
                reqVO == null ? null : reqVO.getChangeDescription(),
                () -> doCheckinControlledFile(userId, id, reqVO));
    }

    private DccControlledFileRespVO doCheckinControlledFile(Long userId, Long id,
                                                             DccControlledFileCheckinReqVO reqVO) {
        if (reqVO == null || StrUtil.isBlank(reqVO.getChangeDescription())) {
            throw exception(CONTROLLED_FILE_CHECKIN_REQUEST_INVALID);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        // The initializer and B draft writes lock Project before Master/File. Take that shared lock
        // before lifecycle locks, then revalidate the routing identity under the locked File.
        DccControlledFileDO routingFile = controlledFileMapper.selectById(id);
        if (routingFile == null || !Objects.equals(tenantId, routingFile.getTenantId())
                || routingFile.getDccProjectCodeId() == null || routingFile.getMasterId() == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        DccProjectCodeDO project = projectCodeMapper.selectByIdForUpdate(routingFile.getDccProjectCodeId());
        if (project == null || !Objects.equals(tenantId, project.getTenantId())
                || !"ENABLE".equals(project.getStatus())) {
            throw exception(CONTROLLED_FILE_CHECKIN_NOT_ALLOWED);
        }
        publicUploadPlacementService.lockSourceLocation(userId,id);
        DccControlledFileDO file = requireLockedLifecycleFile(tenantId, id);
        if (!Objects.equals(project.getId(), file.getDccProjectCodeId())
                || !Objects.equals(routingFile.getMasterId(), file.getMasterId())) {
            throw exception(CONTROLLED_FILE_CHECKIN_NOT_ALLOWED);
        }
        DccControlledFileMasterDO identity = controlledFileMasterMapper.selectByIdForUpdate(file.getMasterId());
        if (!matchesLockedFormalIdentity(file, identity)) {
            throw exception(CONTROLLED_FILE_CHECKIN_NOT_ALLOWED);
        }
        assertCanMutateControlledFile(userId, file);
        validateWorkingCheckin(reqVO);
        String checkinPayloadHash = requireCheckinPayloadHash(tenantId, userId, id, reqVO);
        DccControlledFileCheckoutDO checkout = file.getMasterId() == null ? null
                : checkoutMapper.selectActiveByMasterId(tenantId, file.getMasterId());
        if (checkout == null) {
            DccControlledFileCheckoutDO latest = checkoutMapper.selectLatestByBaseIterationId(tenantId, id);
            if (latest != null && isSameCheckinReplay(latest, userId, id, reqVO)) {
                DccControlledFileDO existing = latest.getCheckinIterationId() == null ? null
                        : controlledFileMapper.selectByIdAndTenantForUpdate(tenantId, latest.getCheckinIterationId());
                if (latest.getCheckinPayloadHash() == null || !latest.getCheckinPayloadHash().matches("[a-f0-9]{64}")
                        || !Objects.equals(latest.getCheckinPayloadHash(), checkinPayloadHash))
                    throw exception(CONTROLLED_FILE_CHECKIN_REQUEST_INVALID);
                if (existing != null && matchesCheckinReplayPayload(file, existing, latest, reqVO)) {
                    requireSavedCheckinManifest(userId, file, existing, reqVO);
                    executionContracts.verifyInheritedPrepared(userId, existing.getId(), file.getId(),
                            requirePreparedSourceAttributeRound(file));
                    return toCheckinRespVO(userId, existing);
                }
            }
            throw exception(CONTROLLED_FILE_NOT_CHECKED_OUT);
        }
        if (!Objects.equals(checkout.getActorId(), userId)) {
            throw exception(CONTROLLED_FILE_CHECKIN_NOT_OWNER, checkout.getActorId());
        }
        if (!Objects.equals(checkout.getBaseIterationId(), id)) {
            throw exception(CONTROLLED_FILE_CHECKIN_NOT_ALLOWED);
        }
        rejectWhenMasterHasOtherUnfinishedWorkflow(file.getMasterId(), file);
        if (!isEditableWorkingVersion(file, null) || isFrozenFormalCandidate(file)) {
            throw exception(CONTROLLED_FILE_CHECKIN_NOT_ALLOWED);
        }
        String verifiedSourceName=nameClaimService.requireSourceName(file);
        if(StrUtil.isBlank(verifiedSourceName))throw exception(CONTROLLED_FILE_CHECKIN_NOT_ALLOWED);
        boolean hasUpload = StrUtil.isNotBlank(reqVO.getUploadTicket());
        boolean hasMetadata = hasRemarkChange(file, reqVO.getRemark());
        if (!hasUpload && !hasMetadata) {
            throw exception(CONTROLLED_FILE_CHECKIN_REQUEST_INVALID);
        }
        if (!hasUpload && (DccControlledFileStatusEnum.REJECTED.getStatus().equals(file.getStatus())
                || DccControlledFileStatusEnum.WITHDRAWN.getStatus().equals(file.getStatus())
                || DccControlledFileStatusEnum.PENDING_APPLICANT_REWORK.getStatus().equals(file.getStatus()))) {
            throw exception(CONTROLLED_FILE_CHECKIN_REQUEST_INVALID);
        }
        CheckinVersionSelection versionSelection = resolveCheckinVersion(file,verifiedSourceName);
        if (versionSelection.formalBaselineId() != null
                && !Objects.equals(identity.getLatestControlledFileId(),versionSelection.formalBaselineId())) {
            throw exception(CONTROLLED_FILE_CHECKIN_NOT_ALLOWED);
        }
        nameClaimService.claimExistingVersion(tenantId, verifiedSourceName, identity.getDccProjectCodeId(),
                identity.getFileTypeTaxonomyLeafId(), identity.getNormalizedFileNumber(), identity.getId(),file.getId());
        int sourceAttributeRound = requirePreparedSourceAttributeRound(file);
        DccControlledFilePreparedSource preparedSource = null;
        DccControlledFilePreparedSource preparedDrawingPdf = null;
        Long drawingPdfFileId;
        String drawingPdfSha256 = null;
        String uploadTicket = StrUtil.trimToNull(reqVO.getUploadTicket());
        String sourceOriginalFileName = verifiedSourceName;
        try {
            if (hasUpload) {
                DccSourceUploadSession.require(reqVO.getSessionId(), DccSourceUploadSession.checkinPrefix(id));
                DccUploadTicketBoundFile bound = uploadTicketService.resolveForBinding(
                        new DccUploadTicketResolveCommand(uploadTicket, userId, file.getCategoryId(),
                                reqVO.getSessionId(), "SOURCE"));
                if (!sourceOriginalFileName.equals(bound.fileName())) {
                    throw exception(CONTROLLED_FILE_CHECKIN_REQUEST_INVALID);
                }
                sourceOriginalFileName = bound.fileName();
                drawingPdfFileId = resolveCheckinDrawingPdf(userId, file, reqVO, bound.fileName());
                drawingPdfSha256 = resolveDrawingPdfSha256(drawingPdfFileId);
                preparedSource = sourceOwnershipService.prepareSubmissionSource(bound.storageFileId(), false);
            } else {
                if (StrUtil.isNotBlank(reqVO.getDrawingPdfUploadTicket())) {
                    throw exception(CONTROLLED_FILE_CHECKIN_REQUEST_INVALID);
                }
                drawingPdfFileId = file.getDrawingPdfFileId();
                preparedSource = sourceOwnershipService.prepareSubmissionSource(file.getSourceFileId(), false);
            }
            String baseHash = StrUtil.trimToNull(checkout.getBaseSourceSha256());
            if (baseHash == null) throw exception(CONTROLLED_FILE_CHECKIN_NOT_ALLOWED);
            if (!hasUpload && (!Objects.equals(file.getSourceFileId(), preparedSource.originSourceFileId())
                    || !Objects.equals(baseHash, preparedSource.sourceSha256()))) {
                throw exception(CONTROLLED_FILE_CHECKIN_NOT_ALLOWED);
            }
            boolean drawingPdfChanged = hasUpload && hasDrawingPdfChanged(file, drawingPdfSha256);
            if (Objects.equals(baseHash, preparedSource.sourceSha256()) && !hasMetadata && !drawingPdfChanged) {
                throw exception(CONTROLLED_FILE_CHECKIN_NO_CHANGE);
            }
            if (!hasUpload) {
                preparedDrawingPdf = prepareMetadataOnlyDrawingPdfCopy(file);
                if (preparedDrawingPdf != null) {
                    drawingPdfFileId = preparedDrawingPdf.sourceFileId();
                }
            }
            DccControlledFileVersionPolicy.VersionNumber nextVersion = versionSelection.version();
            DccControlledFileDO next = copyForCheckin(file, userId, nextVersion, preparedSource,
                    baseHash, drawingPdfFileId, versionSelection.formalBaselineId(), reqVO);
            next.setSourceOriginalFileName(sourceOriginalFileName);
            if (controlledFileMapper.insert(next) != 1 || next.getId() == null) {
                throw exception(CONTROLLED_FILE_CHECKIN_NOT_ALLOWED);
            }
            workingApplicationDraftInitializer.initialize(userId, file.getId(), next.getId(), null);
            var inheritedPrepared = executionContracts.inheritPrepared(userId, next.getId(), file.getId(), sourceAttributeRound);
            next.setPreparedWorkflowContract(inheritedPrepared.executionContract());
            next.setPreparedProcessDefinitionId(inheritedPrepared.processDefinitionId());
            next.setPreparedDefinitionDigest(inheritedPrepared.definitionDigest());
            var draftFacts = executionContracts.readInheritedDraftFacts(userId, next.getId(), file.getId(), sourceAttributeRound);
            var customDraft = executionContracts.readInheritedCustomDraft(userId, next.getId(), file.getId(), sourceAttributeRound);
            if ("APPLICATION_CUSTOM".equals(file.getDraftApprovalSourceMode()) && customDraft == null)
                throw new IllegalStateException("CUSTOM_DRAFT_INHERITED_CONFIGURATION_MISSING");
            if (customDraft != null) {
                next.setDraftApprovalSourceMode(customDraft.draftApprovalSourceMode());
                next.setDraftApplicationCustomMatrixJson(customDraft.draftApplicationCustomMatrixJson());
                next.setDraftApplicationContextKey(customDraft.draftApplicationContextKey());
            }
            next.setDraftApprovalMatrixJson(draftFacts.approvalMatrixCanonical());
            next.setEffectiveMode(draftFacts.effectiveMode());
            next.setEffectiveDate(draftFacts.effectiveDate());
            next.setSelectedSignoffDepartmentIdsJson(draftFacts.selectedSignoffDepartmentIdsJson());
            next.setDistributionDepartmentIdsJson(draftFacts.distributionDepartmentIdsJson());
            next.setNeedTraining(reqVO.getNeedTraining() != null ? reqVO.getNeedTraining()
                    : draftFacts.sameApplicationType() ? draftFacts.needTraining() : false);
            if (next.getNeedTraining() == null)
                throw new IllegalArgumentException("DRAFT_FACTS_EXPLICIT_TRAINING_REQUIRED");
            if (controlledFileMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<DccControlledFileDO>()
                    .eq(DccControlledFileDO::getId, next.getId()).eq(DccControlledFileDO::getTenantId, tenantId)
                    .eq(DccControlledFileDO::getStatus, "WORKING").isNull(DccControlledFileDO::getProcessInstanceId)
                    .set(DccControlledFileDO::getDraftApprovalMatrixJson, next.getDraftApprovalMatrixJson())
                    .set(DccControlledFileDO::getEffectiveMode, next.getEffectiveMode())
                    .set(DccControlledFileDO::getEffectiveDate, next.getEffectiveDate())
                    .set(DccControlledFileDO::getSelectedSignoffDepartmentIdsJson, next.getSelectedSignoffDepartmentIdsJson())
                    .set(DccControlledFileDO::getDistributionDepartmentIdsJson, next.getDistributionDepartmentIdsJson())
                    .set(customDraft != null, DccControlledFileDO::getDraftApprovalSourceMode, next.getDraftApprovalSourceMode())
                    .set(customDraft != null, DccControlledFileDO::getDraftApplicationCustomMatrixJson, next.getDraftApplicationCustomMatrixJson())
                    .set(customDraft != null, DccControlledFileDO::getDraftApplicationContextKey, next.getDraftApplicationContextKey())
                    .set(DccControlledFileDO::getNeedTraining, next.getNeedTraining())) != 1)
                throw new IllegalStateException("DRAFT_FACTS_TARGET_WRITE_FAILED");
            publicUploadPlacementService.inherit(userId,file.getId(),next.getId(),reqVO.getChangeDescription());
            sourceOwnershipService.claimSubmissionSource(next.getId(), preparedSource, userId, "CHECKIN");
            relatedFileService.inheritRelatedFiles(file.getId(), next.getId());
            if (hasUpload) {
                uploadTicketService.markBound(new DccUploadTicketMarkBoundCommand(
                        uploadTicket, userId, file.getCategoryId(), reqVO.getSessionId(), "SOURCE", next.getId()));
            }
            if (StrUtil.isNotBlank(reqVO.getDrawingPdfUploadTicket())) {
                uploadTicketService.markBound(new DccUploadTicketMarkBoundCommand(
                        reqVO.getDrawingPdfUploadTicket(), userId, file.getCategoryId(),
                        reqVO.getSessionId(), "DRAWING_PDF", next.getId()));
            }
            attachmentService.bindCheckinAttachments(userId, id, next.getId(), reqVO.getAttachmentUploadTickets());
            requireSavedCheckinManifest(userId, file, next, reqVO);
            if (checkoutMapper.markCheckedIn(tenantId, checkout.getId(), userId, uploadTicket, next.getId(),
                    preparedSource.sourceFileId(), preparedSource.sourceSha256(), checkinPayloadHash) != 1) {
                throw exception(CONTROLLED_FILE_CHECKIN_NOT_ALLOWED);
            }
            if (controlledFileMapper.checkinByIdAndTenantWhenOwner(tenantId, id, userId) != 1) {
                throw exception(CONTROLLED_FILE_CHECKIN_NOT_OWNER, userId);
            }
            DccControlledFileRespVO response = toCheckinRespVO(userId, next);
            DccControlledFileVersionSourceRollback.enlist(sourceOwnershipService, preparedSource, preparedDrawingPdf);
            return response;
        } catch (RuntimeException ex) {
            cleanupPreparedSourceAfterFailure(preparedSource, ex);
            cleanupPreparedSourceAfterFailure(preparedDrawingPdf, ex);
            throw ex;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DccControlledFileRespVO cancelCheckoutControlledFile(Long userId, Long id,
                                                                 DccControlledFileCancelCheckoutReqVO reqVO) {
        return executeLifecycleAudit(userId, id, "CANCEL_CHECKOUT",
                reqVO == null ? null : reqVO.getReason(),
                () -> doCancelCheckoutControlledFile(userId, id, reqVO));
    }

    private DccControlledFileRespVO doCancelCheckoutControlledFile(Long userId, Long id,
                                                                    DccControlledFileCancelCheckoutReqVO reqVO) {
        if (reqVO == null || StrUtil.isBlank(reqVO.getReason())) {
            throw exception(CONTROLLED_FILE_CHECKOUT_REASON_REQUIRED);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        DccControlledFileDO file = requireLockedLifecycleFile(tenantId, id);
        DccControlledFileCheckoutDO checkout = file.getMasterId() == null ? null
                : checkoutMapper.selectActiveByMasterId(tenantId, file.getMasterId());
        if (checkout == null || !Objects.equals(checkout.getBaseIterationId(), id)) {
            throw exception(CONTROLLED_FILE_NOT_CHECKED_OUT);
        }
        if (!Objects.equals(checkout.getActorId(), userId)) {
            throw exception(CONTROLLED_FILE_CHECKIN_NOT_OWNER, checkout.getActorId());
        }
        assertCanMutateControlledFile(userId, file);
        String reason = StrUtil.trim(reqVO.getReason());
        if (checkoutMapper.markCancelled(tenantId, checkout.getId(), userId, reason) != 1) {
            throw exception(CONTROLLED_FILE_CHECKIN_NOT_ALLOWED);
        }
        if (controlledFileMapper.cancelCheckoutByIdAndTenantWhenOwner(tenantId, id, userId) != 1) {
            throw exception(CONTROLLED_FILE_CHECKIN_NOT_OWNER, userId);
        }
        file.setCheckedOutBy(null);
        file.setCheckedOutTime(null);
        file.setCheckedOutReason(null);
        return toBrowserRespVO(userId, file);
    }

    private DccControlledFileDO requireLockedLifecycleFile(Long tenantId, Long id) {
        DccControlledFileDO initial = controlledFileMapper.selectById(id);
        if (initial == null || initial.getMasterId() == null || !Objects.equals(tenantId, initial.getTenantId())) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        DccControlledFileMasterDO master = controlledFileMasterMapper.selectByIdForUpdate(initial.getMasterId());
        if (master == null || !Objects.equals(tenantId, master.getTenantId())) throw exception(CONTROLLED_FILE_CHECKIN_NOT_ALLOWED);
        DccControlledFileDO current = controlledFileMapper.selectByIdAndTenantForUpdate(tenantId, id);
        if (current == null || !Objects.equals(master.getId(), current.getMasterId())) {
            throw exception(CONTROLLED_FILE_CHECKIN_NOT_ALLOWED);
        }
        return current;
    }

    private boolean matchesLockedFormalIdentity(DccControlledFileDO file, DccControlledFileMasterDO master) {
        return file != null && master != null && Objects.equals(file.getMasterId(), master.getId())
                && Objects.equals(file.getTenantId(), master.getTenantId())
                && master.getDccProjectCodeId() != null && master.getFileTypeTaxonomyLeafId() != null
                && StrUtil.isNotBlank(master.getNormalizedFileNumber())
                && Objects.equals(file.getDccProjectCodeId(), master.getDccProjectCodeId())
                && Objects.equals(file.getFileTypeTaxonomyId(), master.getFileTypeTaxonomyLeafId())
                && Objects.equals(file.getFileNumber(), master.getNormalizedFileNumber());
    }

    private DccControlledFileRespVO executeLifecycleAudit(Long userId, Long requestedFileId, String action,
                                                           String reason,
                                                           Supplier<DccControlledFileRespVO> operation) {
        DccControlledFileDO before = controlledFileMapper.selectById(requestedFileId);
        try {
            DccControlledFileRespVO result = operation.get();
            lifecycleAuditService.recordLifecycleLog(new DccLifecycleLogCreateCommand(
                    result.getId(), result.getVersionNo(), userId, action, "SUCCESS", null, reason));
            return result;
        } catch (RuntimeException ex) {
            String failureCode = ex instanceof ServiceException serviceException
                    ? String.valueOf(serviceException.getCode()) : ex.getClass().getSimpleName();
            // Persist the actual cause, not repeated framework SQL wrappers. The original exception
            // (and any failed audit write) remains intact in the propagated exception chain.
            String failureReason = org.springframework.core.NestedExceptionUtils.getMostSpecificCause(ex).getMessage();
            try {
                lifecycleAuditService.recordLifecycleFailureLog(new DccLifecycleLogCreateCommand(
                        requestedFileId, before == null ? null : before.getVersionNo(), userId,
                        action, "FAILED", failureCode, failureReason));
            } catch (RuntimeException auditFailure) {
                if (auditFailure != ex) ex.addSuppressed(auditFailure);
            }
            throw ex;
        }
    }

    private boolean isEditableWorkingVersion(DccControlledFileDO file, DccControlledFileMasterDO master) {
        if (file == null || file.getId() == null || file.getMasterId() == null) {
            return false;
        }
        String status = StrUtil.trimToEmpty(file.getStatus());
        if ("WORKING".equals(status)
                || DccControlledFileStatusEnum.REJECTED.getStatus().equals(status)
                || DccControlledFileStatusEnum.PENDING_APPLICANT_REWORK.getStatus().equals(status)) {
            return true;
        }
        if (DccControlledFileStatusEnum.WITHDRAWN.getStatus().equals(status)) {
            return isEditableWithdrawnNativeRevision(file, master);
        }
        if (versionPolicy.isControlledBaseline(file)) {
            return true;
        }
        return false;
    }

    private boolean canCheckoutLockedVersion(Long userId, DccControlledFileDO file, DccControlledFileMasterDO master) {
        if (!isEditableWorkingVersion(file, master)) {
            return false;
        }
        if (versionPolicy.isControlledBaseline(file)
                && !Objects.equals(master.getLatestControlledFileId(),file.getId())) return false;
        if (isFrozenFormalCandidate(file)) return false;
        if (Objects.equals(userId, file.getRequesterId())) {
            return true;
        }
        return isPublishedRevisionBaseline(file) && canCreateMajorRevision(userId, file);
    }

    private boolean isPublishedRevisionBaseline(DccControlledFileDO file) {
        if (file == null) {
            return false;
        }
        return versionPolicy.isControlledBaseline(file);
    }

    private boolean isEditableWithdrawnNativeRevision(DccControlledFileDO file, DccControlledFileMasterDO lockedMaster) {
        if (!DccRevisionReworkPolicy.failedFormal(file) || !"REVISION".equals(file.getChangeType())
                || !DccControlledFileProcessDefinitionKeys.REVISION.equals(file.getProcessDefinitionKey())
                || file.getSupersededByFileId() != null) return false;
        Long tenant = TenantContextHolder.getRequiredTenantId();
        var master = lockedMaster == null ? controlledFileMasterMapper.selectById(file.getMasterId()) : lockedMaster;
        if (master == null || !Objects.equals(master.getTenantId(), tenant) || !Objects.equals(file.getTenantId(), tenant)
                || !Objects.equals(master.getId(), file.getMasterId())
                || !Objects.equals(master.getLatestControlledFileId(), file.getRevisionSourceControlledFileId())) return false;
        var baseline = controlledFileMapper.selectById(master.getLatestControlledFileId());
        if (baseline == null || baseline.getControlledTime() == null
                || !DccControlledFileVersionPolicy.isCurrentControlledStatus(baseline.getStatus())
                || !DccRevisionReworkPolicy.sameIdentity(baseline, file)) return false;
        var history = bpmProcessInstanceService.getHistoricProcessInstance(file.getProcessInstanceId());
        if (history == null || history.getEndTime() == null
                || bpmProcessInstanceService.getProcessInstance(file.getProcessInstanceId()) != null
                || !Objects.equals(history.getId(), file.getProcessInstanceId())
                || !tenant.toString().equals(history.getTenantId()) || !file.getId().toString().equals(history.getBusinessKey())
                || !Objects.equals(String.valueOf(file.getRequesterId()), history.getStartUserId())
                || !DccControlledFileProcessDefinitionKeys.REVISION.equals(history.getProcessDefinitionKey())
                || history.getProcessVariables() == null
                || !Integer.valueOf(4).equals(history.getProcessVariables().get("PROCESS_STATUS"))) return false;
        try {
            DccRevisionReworkPolicy.requireAttempt(baseline, file, file.getRevisionChangeType(),
                    controlledFileMapper.selectListByMasterId(file.getMasterId()), versionPolicy);
            return true;
        } catch (IllegalArgumentException invalidLineage) {
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void assertCancelledRevisionCheckinUpload(Long userId, Long fileId, Long categoryId) {
        var file = requireCheckoutAccessibleControlledFile(userId, fileId);
        Long tenant = TenantContextHolder.getRequiredTenantId();
        if (!Objects.equals(file.getTenantId(), tenant) || !Objects.equals(file.getId(), fileId)
                || !Objects.equals(file.getCategoryId(), categoryId)
                || !Objects.equals(file.getRequesterId(), userId)
                || !DccControlledFileStatusEnum.WITHDRAWN.getStatus().equals(file.getStatus()))
            throw exception(CONTROLLED_FILE_UPLOAD_TICKET_INVALID);
        var master = controlledFileMasterMapper.selectById(file.getMasterId());
        if (!isEditableWithdrawnNativeRevision(file, master)) throw exception(CONTROLLED_FILE_UPLOAD_TICKET_INVALID);
        assertCanMutateControlledFile(userId, file);
    }

    private void rejectWhenMasterHasOtherUnfinishedWorkflow(Long masterId, DccControlledFileDO currentFile) {
        List<DccControlledFileDO> masterVersions = controlledFileMapper.selectListByMasterIdForUpdate(masterId);
        if (masterVersions == null) {
            return;
        }
        Map<Long, DccControlledFileDO> versionsById = masterVersions.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(DccControlledFileDO::getId, Function.identity(), (left, right) -> left));
        Set<Long> sameChainEditablePredecessors = resolveSameChainEditablePredecessors(currentFile, versionsById);
        boolean hasOtherUnfinishedWorkflow = masterVersions.stream()
                .filter(Objects::nonNull)
                .filter(item -> !Objects.equals(item.getId(), currentFile.getId()))
                .anyMatch(item -> isUnfinishedWorkflowVersion(item)
                        && !sameChainEditablePredecessors.contains(item.getId()));
        if (hasOtherUnfinishedWorkflow) {
            throw exception(CONTROLLED_FILE_WORKFLOW_IN_PROGRESS);
        }
    }

    private Set<Long> resolveSameChainEditablePredecessors(DccControlledFileDO current,
                                                            Map<Long, DccControlledFileDO> versionsById) {
        Set<Long> predecessors = new HashSet<>();
        Set<Long> visited = new HashSet<>();
        Long predecessorId = current == null ? null : current.getPredecessorControlledFileId();
        while (predecessorId != null && visited.add(predecessorId)) {
            DccControlledFileDO predecessor = versionsById.get(predecessorId);
            if (predecessor == null) {
                break;
            }
            if (isEditablePredecessor(predecessor)) {
                predecessors.add(predecessorId);
            }
            predecessorId = predecessor.getPredecessorControlledFileId();
        }
        return predecessors;
    }

    private boolean isEditablePredecessor(DccControlledFileDO file) {
        return file != null && (DccControlledFileStatusEnum.WORKING.getStatus().equals(file.getStatus())
                || DccControlledFileStatusEnum.PENDING_APPLICANT_REWORK.getStatus().equals(file.getStatus()));
    }

    private boolean isUnfinishedWorkflowVersion(DccControlledFileDO file) {
        if (file == null || StrUtil.isBlank(file.getStatus())
                || versionPolicy.isControlledBaseline(file)) {
            return false;
        }
        return !Set.of(
                DccControlledFileStatusEnum.REJECTED.getStatus(),
                DccControlledFileStatusEnum.WITHDRAWN.getStatus(),
                DccControlledFileStatusEnum.OBSOLETE.getStatus(),
                DccControlledFileStatusEnum.SUPERSEDED.getStatus()
        ).contains(file.getStatus());
    }

    private String resolveSourceSha256(DccControlledFileDO file) {
        DccControlledFileSourceOwnershipDO ownership = sourceOwnershipMapper.selectByControlledFileId(
                TenantContextHolder.getRequiredTenantId(), file.getId());
        if (ownership != null && StrUtil.isNotBlank(ownership.getSourceSha256())) {
            return ownership.getSourceSha256();
        }
        if (file.getSourceFileId() == null) {
            throw exception(CONTROLLED_FILE_CHECKOUT_NOT_ALLOWED);
        }
        return sourceOwnershipService.inspectSource(file.getSourceFileId()).sourceSha256();
    }

    private String resolveDrawingPdfSha256(Long drawingPdfFileId) {
        if (drawingPdfFileId == null) {
            return null;
        }
        DccControlledFilePreparedSource preparedPdf = sourceOwnershipService.inspectSource(drawingPdfFileId);
        if (preparedPdf == null || StrUtil.isBlank(preparedPdf.sourceSha256())) {
            throw exception(CONTROLLED_FILE_CHECKIN_NOT_ALLOWED);
        }
        return preparedPdf.sourceSha256();
    }

    private boolean hasDrawingPdfChanged(DccControlledFileDO file, String drawingPdfSha256) {
        if (StrUtil.isBlank(drawingPdfSha256)) {
            return false;
        }
        if (file == null || file.getDrawingPdfFileId() == null) {
            return true;
        }
        String currentPdfSha256 = resolveDrawingPdfSha256(file.getDrawingPdfFileId());
        return !Objects.equals(currentPdfSha256, drawingPdfSha256);
    }

    private boolean hasRemarkChange(DccControlledFileDO file, String remark) {
        return remark != null && !Objects.equals(StrUtil.trimToEmpty(remark), StrUtil.trimToEmpty(file.getRemark()));
    }

    private DccControlledFileDO requireRelationIdentity(Long id) {
        var file=controlledFileMapper.selectById(id);
        if(file==null || !Objects.equals(file.getTenantId(),TenantContextHolder.getRequiredTenantId()))
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        return file;
    }

    @Override
    @Transactional(readOnly=true, isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public List<DccApplicationRoundSummary> listApplicationRounds(Long userId, Long controlledFileId) {
        var file = requireRelationIdentity(controlledFileId);
        if (userId == null || !canAccessDetail(userId, file)) throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        if (file.getDccProjectCodeId() == null) throw new IllegalStateException("文件未登记正式项目身份");
        var rows = Objects.requireNonNull(applicationEvidenceMapper.selectApplicationRounds(
                file.getTenantId(), file.getDccProjectCodeId(), file.getId()), "persisted application rounds");
        var identities = new java.util.HashSet<String>();
        for (var row : rows) {
            if (row == null || !Objects.equals(row.getControlledFileId(), controlledFileId)
                    || row.getApplicationType() == null || !Set.of("UPLOAD", "REVISION", "OBSOLETE").contains(row.getApplicationType())
                    || row.getBpmRound() == null || row.getBpmRound().isBlank() || !row.getBpmRound().equals(row.getBpmRound().trim())
                    || row.getAttributeRound() == null || row.getAttributeRound() < 1
                    || !identities.add(row.getApplicationType() + ":" + row.getAttributeRound()))
                throw new IllegalStateException("已登记申请轮次身份不完整或重复");
            applicationHistoryGuard.require(file,row.getApplicationType(),row.getBpmRound());
        }
        for (var row : rows) {
            var flow = roundFlowContracts.resolveRoundFlowContract(file, row);
            if (flow == null || !Objects.equals(flow.tenantId(), file.getTenantId())
                    || !Objects.equals(flow.controlledFileId(), row.getControlledFileId())
                    || !Objects.equals(flow.applicationType(), row.getApplicationType())
                    || !Objects.equals(flow.attributeRound(), row.getAttributeRound())
                    || !Objects.equals(flow.bpmRound(), row.getBpmRound()))
                throw new IllegalStateException("申请轮次流程合同与真实映射身份不一致");
            row.setFlowContract(flow);
        }
        return List.copyOf(rows);
    }

    @Override
    @Transactional(readOnly=true, isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public DccFileRelationPermissions getRelationPermissions(Long userId, Long controlledFileId) {
        var file = requireRelationIdentity(controlledFileId);
        if (userId == null || !canViewFileName(userId, file)) throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        boolean canEdit;
        try {
            assertCanMutateControlledFile(userId, file);
            canEdit = true;
        } catch (cn.iocoder.yudao.framework.common.exception.ServiceException denied) {
            if (denied.getCode() != CONTROLLED_FILE_ACCESS_DENIED.getCode()
                    && denied.getCode() != DCC_PROJECT_ACCESS_DENIED.getCode()) throw denied;
            canEdit = false;
        }
        boolean canPreview = resolvePreviewArtifactProjection(file).file() != null
                && canReadBinary(userId, file, DccAccessTypeEnum.PREVIEW);
        var master=file.getMasterId()==null?null:controlledFileMasterMapper.selectById(file.getMasterId());
        var project=file.getDccProjectCodeId()==null?null:projectCodeMapper.selectById(file.getDccProjectCodeId());
        if(master==null || project==null || !Objects.equals(file.getTenantId(),master.getTenantId())
                || !Objects.equals(file.getTenantId(),project.getTenantId()) || !Objects.equals(master.getDccProjectCodeId(),project.getId())
                || StrUtil.isBlank(project.getProjectName()) || StrUtil.isBlank(file.getFileName())
                || StrUtil.isBlank(file.getFileNumber()) || StrUtil.isBlank(file.getVersionNo()) || StrUtil.isBlank(file.getStatus()))
            throw new IllegalStateException("relation metadata formal file/Master/project identity is missing or inconsistent");
        var placement=projectFilePlacementMapper.findFile(file.getId());Long folderId=null;String folderName=null;
        if(placement!=null){
            var folder=placement.getProjectFolderId()==null?null:projectFolderMapper.selectById(placement.getProjectFolderId());
            if(!Objects.equals(placement.getTenantId(),file.getTenantId()) || !Objects.equals(placement.getProjectCodeId(),project.getId())
                    || !Objects.equals(placement.getControlledFileId(),file.getId()) || !Objects.equals(placement.getStorageDirectoryId(),file.getDirectoryId())
                    || folder==null || !Objects.equals(folder.getTenantId(),file.getTenantId()) || !Objects.equals(folder.getProjectCodeId(),project.getId())
                    || !Boolean.TRUE.equals(folder.getActive()) || StrUtil.isBlank(folder.getName()))
                throw new IllegalStateException("relation metadata registered project folder identity is inconsistent");
            folderId=folder.getId();folderName=folder.getName();
        }
        boolean controlled=file.getControlledTime()!=null && DccControlledFileVersionPolicy.isCurrentControlledStatus(file.getStatus());
        boolean hasCurrentControlledSource = hasCurrentControlledRelationSource(file, master, project.getId())
                && canViewFileName(userId, controlledFileMapper.selectById(master.getCurrentActiveControlledFileId()));
        return new DccFileRelationPermissions(file.getId(),canEdit,canPreview,file.getTenantId(),file.getMasterId(),project.getId(),project.getProjectName(),
                folderId,folderName,file.getFileNumber(),file.getFileName(),file.getVersionNo(),file.getStatus(),controlled,
                "CONTROLLED_PENDING_EFFECTIVE".equals(file.getStatus()),"ACTIVE".equals(file.getStatus()) && file.getActivatedTime()!=null,
                hasCurrentControlledSource,
                master.getLatestControlledFileId() != null
                        && canViewFileName(userId, controlledFileMapper.selectById(master.getLatestControlledFileId()))
                        ? master.getLatestControlledFileId() : null,
                hasCurrentControlledSource ? master.getCurrentActiveControlledFileId() : null);
    }

    /** Source existence is distinct from selected-version state and its content/edit grants. No reads create relations. */
    private boolean hasCurrentControlledRelationSource(DccControlledFileDO selected, DccControlledFileMasterDO master, Long projectId) {
        Long tenant = TenantContextHolder.getRequiredTenantId();
        if (!Objects.equals(master.getId(), selected.getMasterId()) || !Objects.equals(master.getTenantId(), tenant)
                || !Objects.equals(selected.getTenantId(), tenant) || !Objects.equals(master.getDccProjectCodeId(), projectId)) {
            throw new IllegalStateException("current controlled relation source Master identity is inconsistent");
        }
        Long currentId = master.getCurrentActiveControlledFileId();
        if (currentId == null) return false;
        var current = controlledFileMapper.selectById(currentId);
        var version = current == null ? null : versionPolicy.parseStored(current);
        if (current == null || !Objects.equals(currentId, current.getId()) || !Objects.equals(current.getTenantId(), tenant)
                || !Objects.equals(current.getMasterId(), master.getId()) || !Objects.equals(current.getDccProjectCodeId(), projectId)
                || Boolean.TRUE.equals(current.getDeleted())
                || version == null || version.isWorkingIteration()) {
            throw new IllegalStateException("current controlled relation source pointer or formal control facts are invalid");
        }
        return "ACTIVE".equals(current.getStatus())
                && current.getControlledTime() != null && current.getActivatedTime() != null;
    }

    @Override
    @Transactional(readOnly=true, isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public DccControlledFileApplicationEvidence getApplicationEvidence(Long userId, Long controlledFileId,
                                                                      String applicationType, String bpmRound) {
        var file=requireRelationIdentity(controlledFileId);
        if (userId==null || !canAccessDetail(userId,file)) throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        if (applicationType==null || !Set.of("UPLOAD","REVISION","OBSOLETE").contains(applicationType)
                || bpmRound==null || bpmRound.isBlank() || !bpmRound.equals(bpmRound.trim()) || file.getDccProjectCodeId()==null)
            throw new IllegalArgumentException("explicit formal application type and BPM round are required");
        var signatures=buildSignatureSummaries(file).stream()
                .filter(signature -> Objects.equals(signature.getProcessInstanceId(),bpmRound)).toList();
        var rounds=Objects.requireNonNull(applicationEvidenceMapper.selectBoundRounds(file.getTenantId(),file.getDccProjectCodeId(),
                applicationType,file.getId(),bpmRound),"persisted application rounds");
        if (rounds.size()>1) throw new IllegalStateException("application BPM mapping is ambiguous");
        if (rounds.isEmpty()) return new DccControlledFileApplicationEvidence(file.getId(),file.getVersionNo(),applicationType,bpmRound,
                null,false,"NOT_RECORDED",null,null,signatures);
        applicationHistoryGuard.require(file,applicationType,bpmRound);
        int round=rounds.get(0);
        if (round<1) throw new IllegalStateException("invalid stored application round");
        var snapshot=applicationEvidenceMapper.selectSnapshot(file.getTenantId(),file.getDccProjectCodeId(),applicationType,file.getId(),round);
        if (snapshot==null) return new DccControlledFileApplicationEvidence(file.getId(),file.getVersionNo(),applicationType,bpmRound,
                round,false,"NOT_RECORDED",null,null,signatures);
        if (!Objects.equals(snapshot.getTenantId(),file.getTenantId()) || !Objects.equals(snapshot.getProjectCodeId(),file.getDccProjectCodeId())
                || !Objects.equals(snapshot.getApplicationId(),file.getId()) || !Objects.equals(snapshot.getApplicationType(),applicationType)
                || !Objects.equals(snapshot.getApplicationRound(),round)) throw new IllegalStateException("application snapshot identity changed");
        if (!Boolean.TRUE.equals(snapshot.getSubmitted())) return new DccControlledFileApplicationEvidence(file.getId(),file.getVersionNo(),applicationType,bpmRound,
                round,false,"NOT_FROZEN",null,null,signatures);
        var defaults=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(snapshot.getDefaultSourceJson(),cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes.class);
        var actual=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(snapshot.getActualAttributesJson(),cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes.class);
        if (defaults==null || actual==null) throw new IllegalStateException("frozen application attributes are missing");
        defaults=defaults.validated();actual=actual.validated();
        var relationEvidence=Objects.requireNonNull(relationStageSnapshotService.readRoundEvidence(userId,file.getId(),applicationType,bpmRound),
                "bound application relation evidence must be returned by its formal round reader");
        return new DccControlledFileApplicationEvidence(file.getId(),file.getVersionNo(),applicationType,bpmRound,round,true,null,
                defaults,actual,signatures,relationEvidence,
                DccApplicationNodeRecordProjection.project(file,bpmRound,signatures,
                        bpmTaskService.getTaskListByProcessInstanceId(bpmRound,true),taskAssigneeSnapshotMapper.selectListByControlledFileId(file.getId())));
    }

    @Override
    @Transactional(readOnly=true, isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public DccControlledFileRevisionOptions getRevisionOptions(Long userId, Long controlledBaselineId) {
        var actualBaseline = requireRelationIdentity(controlledBaselineId);
        if (!canViewFileName(userId, actualBaseline)) throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        var baseline = sourceIdentityProjection(actualBaseline);
        var master = controlledFileMasterMapper.selectById(baseline.getMasterId());
        if (!matchesLockedFormalIdentity(baseline, master)
                || !Objects.equals(master.getLatestControlledFileId(), controlledBaselineId)
                || baseline.getControlledTime() == null
                || !DccControlledFileVersionPolicy.isCurrentControlledStatus(baseline.getStatus())
                || versionPolicy.requireStored(baseline).isWorkingIteration()
                || StrUtil.isBlank(baseline.getSourceOriginalFileName())) {
            throw new IllegalArgumentException("revision preparation requires the authoritative latest controlled source");
        }
        var chain = Objects.requireNonNull(controlledFileMapper.selectListByMasterId(master.getId()), "revision chain");
        if (chain.stream().anyMatch(f -> f == null || !Objects.equals(f.getTenantId(), baseline.getTenantId())
                || !Objects.equals(f.getMasterId(), master.getId()))) {
            throw new IllegalStateException("revision chain contains a foreign identity");
        }
        var checkout = readLogicalCheckout(baseline);
        String lockedReason = checkout != null ? "CHECKED_OUT" : chain.stream().anyMatch(f ->
                !versionPolicy.isControlledBaseline(f)
                && (!Set.of("WORKING", "OBSOLETE", "REJECTED", "WITHDRAWN").contains(f.getStatus())
                || "WORKING".equals(f.getStatus()) && f.getRevisionChangeType() != null)
                && !("PENDING_APPLICANT_REWORK".equals(f.getStatus()) && chain.stream().anyMatch(s ->
                    "WORKING".equals(s.getStatus()) && Objects.equals(f,DccRevisionReworkPolicy.predecessor(s,chain,versionPolicy)))))
                ? "FORMAL_REVISION_IN_PROGRESS" : null;
        var partial = revisionTargetProjection(baseline, "PARTIAL", chain);
        var replacement = revisionTargetProjection(baseline, "REPLACEMENT", chain);
        boolean editor = projectAccessService.hasProjectEditorOrOwner(userId, baseline.getDccProjectCodeId());
        boolean owner = editor && projectAccessService.hasProjectOwner(userId, baseline.getDccProjectCodeId());
        boolean upload = permissionSupport.hasCategoryPermission(baseline.getCategoryId(), userId,
                DccFileCategoryPermissionActionEnum.UPLOAD);
        var iterations = chain.stream()
                .filter(f -> "WORKING".equals(f.getStatus()) && f.getRevisionChangeType() == null)
                .filter(f -> Objects.equals(controlledBaselineId, f.getRevisionBaseActiveControlledFileId())
                        && matchesLockedFormalIdentity(f, master)
                        && Objects.equals(baseline.getCategoryId(), f.getCategoryId())
                        && Objects.equals(baseline.getSourceOriginalFileName(), nameClaimService.requireSourceName(f))
                        && canViewFileName(userId, f))
                .filter(f -> {
                    var version = versionPolicy.requireStored(f);
                    return version.isWorkingIteration()
                            && (version.formal().display().equals(versionPolicy.requireStored(baseline).display())
                            || DccRevisionReworkPolicy.correctionMatchesBaseline(baseline,f,chain,versionPolicy));
                })
                .sorted(Comparator.comparing(versionPolicy::requireStored).reversed())
                .map(f -> new DccControlledFileRevisionOptions.Iteration(f.getId(), f.getVersionNo(),
                        lockedReason == null && editor && upload && partial.unavailableReason() == null
                                && Objects.equals(userId, f.getRequesterId()) && DccRevisionReworkPolicy.canAllocate(baseline,f,"PARTIAL",chain,versionPolicy),
                        lockedReason == null && owner && upload && replacement.unavailableReason() == null
                                && DccRevisionReworkPolicy.canAllocate(baseline,f,"REPLACEMENT",chain,versionPolicy),
                        resolvePreviewArtifactProjection(f).file() != null && canReadBinary(userId, f, DccAccessTypeEnum.PREVIEW),
                        f.getCreateTime(), f.getChangeDescription()))
                .toList();
        var actor = checkout == null ? null : adminUserApi.getUser(checkout.getActorId());
        return new DccControlledFileRevisionOptions(baseline.getId(), baseline.getVersionNo(), master.getId(),
                baseline.getSourceOriginalFileName(), partial, replacement,
                checkout == null ? null : checkout.getActorId(), actor == null ? null : actor.getNickname(),
                checkout == null ? null : checkout.getCreateTime(), lockedReason, iterations);
    }

    private boolean isFrozenFormalCandidate(DccControlledFileDO file) {
        return "WORKING".equals(file.getStatus()) && file.getRevisionChangeType() != null
                && (!"INITIAL".equals(file.getRevisionChangeType()) || file.getSelectedIterationControlledFileId() != null);
    }

    private DccControlledFileRevisionOptions.Target revisionTargetProjection(DccControlledFileDO baseline,
                                                                            String intent, List<DccControlledFileDO> chain) {
        String target = versionPolicy.formalTarget(baseline, intent).display();
        return new DccControlledFileRevisionOptions.Target(target,
                chain.stream().anyMatch(f -> target.equals(f.getVersionNo()))
                    && chain.stream().noneMatch(f -> "WORKING".equals(f.getStatus()) && f.getRevisionChangeType()==null
                        && DccRevisionReworkPolicy.canAllocate(baseline,f,intent,chain,versionPolicy)) ? "TARGET_ALREADY_ALLOCATED" : null);
    }

    @Override
    @Transactional(readOnly=true, isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public PageResult<DccControlledFileSelectorRow> getControlledFileSelectorPage(Long userId, DccControlledFileSelectorQuery request) {
        Objects.requireNonNull(request, "selector request").validated();
        if (userId == null) throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        Long tenant = TenantContextHolder.getRequiredTenantId();
        boolean manager = directoryAccessPermissionService.hasDirectoryManagementPermission(userId);
        Map<Long,Boolean> matrix = new LinkedHashMap<>();
        var pageRequest = new DccControlledFilePageReqVO();
        List<String> blacklist = browserSettingsService.getBlacklistedExtensionPatterns();
        var candidates = Objects.requireNonNull(selectorMapper.selectCandidates(tenant,request), "selector candidates");
        Map<Long,String> selectedNames=new LinkedHashMap<>();
        List<Long> authorized = candidates.stream()
                .filter(f -> Objects.equals(tenant,f.getTenantId()) && f.getControlledTime()!=null
                        && f.getActivatedTime()!=null && "ACTIVE".equals(f.getStatus())
                        && Objects.equals(f.getId(),f.getCurrentActiveControlledFileId()))
                .filter(f -> isWithinAssignedFileScope(userId,f)
                        && hasReadableProjectScope(userId, f)
                        && canAccessQuery(userId,f,pageRequest,manager,matrix))
                .filter(f -> {
                    String name=f.getSourceOriginalFileName();
                    if(StrUtil.isBlank(name)) {
                        var master=controlledFileMasterMapper.selectById(f.getMasterId());
                        if(!matchesLockedFormalIdentity(f,master))throw new IllegalStateException("selector historical candidate has incomplete formal identity");
                        name=nameClaimService.requireSourceName(f);
                    }
                    if(StrUtil.isBlank(name))throw new IllegalStateException("selector candidate has no verified source identity");
                    var projection=new DccControlledFileDO();org.springframework.beans.BeanUtils.copyProperties(f,projection);projection.setSourceOriginalFileName(name);
                    if(isBlacklistedBrowserExtension(projection,blacklist))return false;
                    selectedNames.put(f.getId(),name);return true;
                })
                .map(DccControlledFileDO::getId).toList();
        if (authorized.isEmpty()) return PageResult.empty(0L);
        long total = selectorMapper.countAuthorized(tenant,request,authorized);
        var rows = selectorMapper.selectAuthorizedPage(tenant,request,authorized,request.pageSize(),
                Math.multiplyExact((long)request.pageNo()-1,request.pageSize())).stream().map(f -> {
            if (!authorized.contains(f.getId()) || !Objects.equals(tenant,f.getTenantId())
                    || !Objects.equals(f.getId(),f.getCurrentActiveControlledFileId())
                    || !"ACTIVE".equals(f.getStatus()) || f.getControlledTime()==null || f.getActivatedTime()==null
                    || f.getMasterId()==null || f.getDccProjectCodeId()==null) {
                throw new IllegalStateException("selector page returned an unauthorized identity");
            }
            String sourceName=selectedNames.get(f.getId());
            if(StrUtil.isNotBlank(f.getSourceOriginalFileName()) && !Objects.equals(sourceName,f.getSourceOriginalFileName()))throw new IllegalStateException("selector page source identity changed");
            if(StrUtil.isBlank(sourceName))throw new IllegalStateException("selector page has no verified source identity");
            boolean preview = isDailyBodyState(f) && resolvePreviewArtifactProjection(f).file()!=null
                    && canReadBinary(userId,f,DccAccessTypeEnum.PREVIEW,manager,matrix);
            return new DccControlledFileSelectorRow(tenant,f.getId(),f.getLatestControlledFileId(),f.getCurrentActiveControlledFileId(),f.getMasterId(),
                    f.getDccProjectCodeId(),f.getProjectName(),f.getDirectoryId(),f.getSourceDirectoryName(),f.getProjectFolderId(),f.getProjectFolderName(),
                    f.getFileNumber(),sourceName,f.getVersionNo(),f.getStatus(),true,
                    "CONTROLLED_PENDING_EFFECTIVE".equals(f.getStatus()),"ACTIVE".equals(f.getStatus()) && f.getActivatedTime()!=null,preview);
        }).toList();
        return new PageResult<>(rows,total);
    }
    @Override public void assertRelationNameVisible(Long userId,Long id) {
        var file=requireRelationIdentity(id);
        if(!canViewFileName(userId,file))
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
    }
    @Override public void assertRelationContentReadable(Long userId,Long id) {
        if(!canReadBinary(userId,requireRelationIdentity(id),DccAccessTypeEnum.PREVIEW))
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
    }
    @Override public void assertRelationEditable(Long userId,Long id) {
        var file=requireRelationIdentity(id);
        assertRelationNameVisible(userId,id);
        assertCanMutateControlledFile(userId,file);
    }
    private void assertCanMutateControlledFile(Long userId, DccControlledFileDO file) {
        if (userId == null || !isWithinAssignedFileScope(userId, file)) {
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }
        if (file == null || file.getDccProjectCodeId() == null || file.getCategoryId() == null) {
            throw exception(DCC_PROJECT_ACCESS_DENIED);
        }
        projectAccessService.assertProjectEditorOrOwner(userId, file.getDccProjectCodeId());
        if (!permissionSupport.hasCategoryPermission(file.getCategoryId(), userId,
                DccFileCategoryPermissionActionEnum.UPLOAD)) {
            throw exception(DCC_PROJECT_ACCESS_DENIED);
        }
    }

    private String requireCheckinPayloadHash(Long tenantId, Long userId, Long baseId,
                                           DccControlledFileCheckinReqVO request) {
        if (TenantContextHolder.isIgnore() || tenantId == null || userId == null || userId <= 0
                || baseId == null || baseId <= 0 || request.getNeedTraining() == null
                || request.getAttachmentUploadTickets() == null)
            throw exception(CONTROLLED_FILE_CHECKIN_REQUEST_INVALID);
        var tickets = new ArrayList<Object>();
        var seenTickets = new HashSet<String>(); var seenSessions = new HashSet<String>();
        for (var ticket : request.getAttachmentUploadTickets()) {
            if (ticket == null || StrUtil.isBlank(ticket.getUploadTicket()) || StrUtil.isBlank(ticket.getSessionId())
                    || !ticket.getUploadTicket().equals(ticket.getUploadTicket().trim())
                    || !seenTickets.add(ticket.getUploadTicket()) || !seenSessions.add(ticket.getSessionId()))
                throw exception(CONTROLLED_FILE_CHECKIN_REQUEST_INVALID);
            DccSourceUploadSession.require(ticket.getSessionId(), DccSourceUploadSession.checkinPrefix(baseId));
            tickets.add(Arrays.asList(ticket.getUploadTicket(), ticket.getSessionId()));
        }
        String sourceTicket = StrUtil.trimToNull(request.getUploadTicket());
        String diagramTicket = StrUtil.trimToNull(request.getDrawingPdfUploadTicket());
        String sourceSession = StrUtil.trimToNull(request.getSessionId());
        if (sourceTicket != null || diagramTicket != null)
            DccSourceUploadSession.require(sourceSession, DccSourceUploadSession.checkinPrefix(baseId));
        String canonical = cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(Arrays.asList(
                "DCC_CHECKIN_PAYLOAD_V1", tenantId, userId, baseId, request.getVersionChangeType(), request.getRevisionChangeType(),
                sourceTicket, diagramTicket, sourceSession, StrUtil.trim(request.getChangeDescription()),
                request.getRemark() == null ? null : StrUtil.trim(request.getRemark()), request.getNeedTraining(), tickets));
        return DigestUtil.sha256Hex(canonical);
    }

    /** Called only after a complete normal bind or a committed request-hash match. Never resolves AVAILABLE tickets. */
    private void requireSavedCheckinManifest(Long actorId, DccControlledFileDO base, DccControlledFileDO target,
                                             DccControlledFileCheckinReqVO request) {
        Long tenant = TenantContextHolder.getRequiredTenantId();
        if (!Objects.equals(base.getTenantId(), tenant) || !Objects.equals(target.getTenantId(), tenant)
                || Boolean.TRUE.equals(target.getDeleted()) || !Objects.equals(target.getRequesterId(), actorId)
                || !Objects.equals(target.getPredecessorControlledFileId(), base.getId())
                || !Objects.equals(target.getMasterId(), base.getMasterId()))
            throw exception(CONTROLLED_FILE_CHECKIN_REQUEST_INVALID);
        if (StrUtil.isNotBlank(request.getUploadTicket())) {
            var bound = uploadTicketService.resolveBoundFile(new DccUploadTicketResolveCommand(
                    request.getUploadTicket(), actorId, base.getCategoryId(), request.getSessionId(), "SOURCE"), target.getId());
            var source = requireCheckinBoundTicket(actorId, base, target, bound, request.getUploadTicket(), request.getSessionId(), "SOURCE");
            var ownership = sourceOwnershipMapper.selectByControlledFileId(tenant, target.getId());
            if (ownership == null || !Objects.equals(ownership.getSourceFileId(), target.getSourceFileId())
                    || !Objects.equals(ownership.getOriginSourceFileId(), bound.storageFileId())
                    || !Objects.equals(source.getFileSha256(), target.getSourceSha256())
                    || !Objects.equals(sourceOwnershipService.inspectSource(target.getSourceFileId()).sourceSha256(), target.getSourceSha256()))
                throw exception(CONTROLLED_FILE_CHECKIN_REQUEST_INVALID);
        }
        var rows = Objects.requireNonNull(checkinAttachmentMapper.selectListByControlledFileId(target.getId()), "saved checkin attachments");
        if (rows.stream().anyMatch(row -> !Objects.equals(row.getTenantId(), tenant)
                || !Objects.equals(row.getControlledFileId(), target.getId()) || Boolean.TRUE.equals(row.getDeleted())
                || !"ATTACHMENT".equals(row.getAttachmentPurpose())))
            throw exception(CONTROLLED_FILE_CHECKIN_REQUEST_INVALID);
        if (rows.size() != request.getAttachmentUploadTickets().size())
            throw exception(CONTROLLED_FILE_CHECKIN_REQUEST_INVALID);
        for (int i = 0; i < rows.size(); i++) {
            var input = request.getAttachmentUploadTickets().get(i); var row = rows.get(i);
            var bound = uploadTicketService.resolveBoundFile(new DccUploadTicketResolveCommand(
                    input.getUploadTicket(), actorId, base.getCategoryId(), input.getSessionId(), "ATTACHMENT"), target.getId());
            var ticket = requireCheckinBoundTicket(actorId, base, target, bound, input.getUploadTicket(), input.getSessionId(), "ATTACHMENT");
            if (row.getId() == null || row.getId() <= 0 || !Integer.valueOf(i).equals(row.getSortNo())
                    || !Objects.equals(row.getStorageFileId(), bound.storageFileId())
                    || !Objects.equals(row.getOriginalFileName(), bound.fileName()) || !Objects.equals(row.getContentType(), bound.contentType())
                    || !Objects.equals(row.getFileSize(), bound.fileSize()) || !Objects.equals(row.getFileSha256(), ticket.getFileSha256())
                    || !Objects.equals(row.getUploadedBy(), actorId) || !Objects.equals(row.getUploadedAt(), ticket.getCreateTime())
                    || StrUtil.isBlank(row.getUploadedUsernameSnapshot()) || StrUtil.isBlank(row.getUploadedNicknameSnapshot())
                    || row.getApplicationProcessInstanceId() != null)
                throw exception(CONTROLLED_FILE_CHECKIN_REQUEST_INVALID);
        }
    }

    private cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileTemporaryFileDO requireCheckinBoundTicket(
            Long actorId, DccControlledFileDO base, DccControlledFileDO target, DccUploadTicketBoundFile bound,
            String ticketId, String sessionId, String purpose) {
        var ticket = checkinTemporaryFileMapper.selectOne(
                cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileTemporaryFileDO::getUploadTicket, StrUtil.trim(ticketId));
        if (ticket == null || bound == null || !Objects.equals(ticket.getTenantId(), base.getTenantId())
                || Boolean.TRUE.equals(ticket.getDeleted()) || !Objects.equals(ticket.getUploaderId(), actorId)
                || !Objects.equals(ticket.getCategoryId(), base.getCategoryId()) || !Objects.equals(ticket.getPurpose(), purpose)
                || !Objects.equals(ticket.getSessionId(), sessionId) || !"BOUND".equals(ticket.getStatus())
                || !"BOUND".equals(ticket.getCleanupStatus()) || !Objects.equals(ticket.getBoundControlledFileId(), target.getId())
                || !Objects.equals(ticket.getStorageFileId(), bound.storageFileId()) || ticket.getCreateTime() == null
                || !Objects.equals(ticket.getOriginalFileName(), bound.fileName()) || !Objects.equals(ticket.getContentType(), bound.contentType())
                || !Objects.equals(ticket.getFileSize(), bound.fileSize()) || ticket.getFileSha256() == null
                || !ticket.getFileSha256().matches("[a-f0-9]{64}")
                || !Objects.equals(sourceOwnershipService.inspectSource(bound.storageFileId()).sourceSha256(), ticket.getFileSha256()))
            throw exception(CONTROLLED_FILE_CHECKIN_REQUEST_INVALID);
        return ticket;
    }

    private DccControlledFileRespVO toCheckinRespVO(Long actorId, DccControlledFileDO file) {
        var response = toBrowserRespVO(actorId, file);
        if (!canAccessDetail(actorId, file))
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        response.setAttachments(Objects.requireNonNull(attachmentService.listAttachments(file.getId()), "persisted checkin response attachments"));
        response.setOrdinaryAttachmentManifestHash(attachmentService.readOrdinaryAttachmentManifest(actorId, file.getId()).payloadHash());
        var saved = Objects.requireNonNull(checkinAttachmentMapper.selectListByControlledFileId(file.getId()), "saved checkin manifest");
        if (!Objects.equals(response.getId(), file.getId()) || response.getAttachments() == null
                || response.getAttachments().size() != saved.size())
            throw new IllegalStateException("检入结果未返回准确的新工作版本及完整普通附件清单");
        for (int i = 0; i < saved.size(); i++) {
            var fact = saved.get(i); var returned = response.getAttachments().get(i);
            if (!Objects.equals(returned.getControlledFileId(), file.getId()) || !Objects.equals(returned.getAttachmentId(), fact.getId())
                    || !Objects.equals(returned.getSortNo(), fact.getSortNo()) || !Objects.equals(returned.getFileName(), fact.getOriginalFileName())
                    || !Objects.equals(returned.getFileSize(), fact.getFileSize()) || !Objects.equals(returned.getContentType(), fact.getContentType())
                    || !"ATTACHMENT".equals(returned.getAttachmentPurpose()))
                throw new IllegalStateException("检入普通附件响应与实际持久化的版本、身份或顺序不一致");
        }
        return response;
    }

    private boolean isSameCheckinReplay(DccControlledFileCheckoutDO latest, Long userId, Long baseIterationId,
                                        DccControlledFileCheckinReqVO reqVO) {
        return latest != null
                && "CHECKED_IN".equals(latest.getStatus())
                && Objects.equals(latest.getActorId(), userId)
                && Objects.equals(latest.getBaseIterationId(), baseIterationId)
                && StrUtil.equals(latest.getCheckinUploadTicket(), StrUtil.trimToNull(reqVO.getUploadTicket()));
    }

    private boolean matchesCheckinReplayPayload(DccControlledFileDO base, DccControlledFileDO existing,
                                                DccControlledFileCheckoutDO latest,
                                                DccControlledFileCheckinReqVO reqVO) {
        return existing != null
                && Objects.equals(existing.getPredecessorControlledFileId(), latest.getBaseIterationId())
                && matchesCheckinVersionChange(base, existing, reqVO)
                && matchesCheckinDrawingReplay(base, existing, latest, reqVO)
                && Objects.equals(normalizeCheckinReplayPayload(base, existing),
                normalizeCheckinReplayPayload(base, reqVO));
    }

    private boolean matchesCheckinDrawingReplay(DccControlledFileDO base, DccControlledFileDO existing,
                                                DccControlledFileCheckoutDO latest,
                                                DccControlledFileCheckinReqVO reqVO) {
        if (StrUtil.isNotBlank(reqVO.getDrawingPdfUploadTicket())) {
            DccUploadTicketBoundFile bound = uploadTicketService.resolveBoundFile(
                    new DccUploadTicketResolveCommand(reqVO.getDrawingPdfUploadTicket(), latest.getActorId(),
                            base.getCategoryId(), reqVO.getSessionId(), "DRAWING_PDF"), existing.getId());
            return bound != null && Objects.equals(existing.getDrawingPdfFileId(), bound.storageFileId());
        }
        if (StrUtil.isNotBlank(reqVO.getUploadTicket()) || base.getDrawingPdfFileId() == null) {
            return existing.getDrawingPdfFileId() == null;
        }
        if (existing.getDrawingPdfFileId() == null
                || Objects.equals(existing.getDrawingPdfFileId(), base.getDrawingPdfFileId())) {
            return false;
        }
        DccControlledFilePreparedSource originalPdf = sourceOwnershipService.inspectSource(base.getDrawingPdfFileId());
        DccControlledFilePreparedSource copiedPdf = sourceOwnershipService.inspectSource(existing.getDrawingPdfFileId());
        return StrUtil.isNotBlank(originalPdf.sourceSha256())
                && Objects.equals(originalPdf.sourceSha256(), copiedPdf.sourceSha256());
    }

    private Long resolveCheckinDrawingPdf(Long userId, DccControlledFileDO file,
                                          DccControlledFileCheckinReqVO reqVO, String sourceName) {
        if (!DccControlledFileUploadTypePolicy.isDrawingSourceName(sourceName)) {
            if (StrUtil.isNotBlank(reqVO.getDrawingPdfUploadTicket())) {
                throw exception(CONTROLLED_FILE_CHECKIN_REQUEST_INVALID);
            }
            return null;
        }
        if (StrUtil.isBlank(reqVO.getDrawingPdfUploadTicket())) {
            throw exception(CONTROLLED_FILE_DRAWING_PDF_REQUIRED);
        }
        DccUploadTicketBoundFile bound = uploadTicketService.resolveForBinding(new DccUploadTicketResolveCommand(
                reqVO.getDrawingPdfUploadTicket(), userId, file.getCategoryId(), reqVO.getSessionId(), "DRAWING_PDF"));
        FileDO pdf = fileMapper.selectById(bound.storageFileId());
        if (pdf == null) {
            throw exception(CONTROLLED_FILE_DRAWING_PDF_FILE_INVALID);
        }
        byte[] content;
        try {
            content = fileService.getFileContent(pdf.getConfigId(), pdf.getPath());
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to read check-in drawing PDF", ex);
        }
        if (!DccControlledFileUploadTypePolicy.isRealPdfFile(pdf.getName(), content)) {
            throw exception(CONTROLLED_FILE_DRAWING_PDF_FILE_INVALID);
        }
        return bound.storageFileId();
    }

    private DccControlledFilePreparedSource prepareMetadataOnlyDrawingPdfCopy(DccControlledFileDO file) {
        if (file.getSourceFileId() == null || file.getDrawingPdfFileId() == null) {
            return null;
        }
        FileDO source = fileMapper.selectById(file.getSourceFileId());
        if (source == null) {
            throw exception(CONTROLLED_FILE_CHECKIN_NOT_ALLOWED);
        }
        if (!DccControlledFileUploadTypePolicy.isDrawingSourceName(source.getName())) {
            return null;
        }
        return sourceOwnershipService.createVerifiedCopy(file.getDrawingPdfFileId());
    }

    private CheckinReplayPayload normalizeCheckinReplayPayload(DccControlledFileDO base,
                                                               DccControlledFileCheckinReqVO reqVO) {
        String effectiveRemark = reqVO.getRemark() == null
                ? StrUtil.trimToEmpty(base.getRemark()) : StrUtil.trimToEmpty(reqVO.getRemark());
        return new CheckinReplayPayload(StrUtil.trim(reqVO.getChangeDescription()), effectiveRemark,
                reqVO.getNeedTraining() != null ? reqVO.getNeedTraining()
                        : Objects.equals(resolveCheckinChangeType(base), base.getChangeType()) ? base.getNeedTraining() : false);
    }

    private CheckinReplayPayload normalizeCheckinReplayPayload(DccControlledFileDO base, DccControlledFileDO existing) {
        return new CheckinReplayPayload(StrUtil.trim(existing.getChangeDescription()),
                StrUtil.trimToEmpty(existing.getRemark()), existing.getNeedTraining());
    }

    private record CheckinReplayPayload(String changeDescription, String remark, Boolean needTraining) {
    }

    private void validateWorkingCheckin(DccControlledFileCheckinReqVO request) {
        if (!"MINOR".equals(request.getVersionChangeType()) || request.getRevisionChangeType() != null) {
            throw exception(CONTROLLED_FILE_CHECKIN_REQUEST_INVALID);
        }
    }

    private boolean matchesCheckinVersionChange(DccControlledFileDO base, DccControlledFileDO existing,
                                                 DccControlledFileCheckinReqVO request) {
        var previous = versionPolicy.parseStored(base);
        var next = versionPolicy.parseStored(existing);
        return previous != null && next != null && next.isWorkingIteration()
                && previous.formal().display().equals(next.formal().display()) && next.compareTo(previous) > 0;
    }

    private DccControlledFileDO sourceIdentityProjection(DccControlledFileDO actual) {
        var projection=new DccControlledFileDO();
        org.springframework.beans.BeanUtils.copyProperties(actual,projection);
        projection.setSourceOriginalFileName(nameClaimService.requireSourceName(actual));
        return projection;
    }

    private CheckinVersionSelection resolveCheckinVersion(DccControlledFileDO file,String sourceName) {
        List<DccControlledFileDO> chain = Objects.requireNonNull(
                controlledFileMapper.selectListByMasterIdForUpdate(file.getMasterId()), "locked version chain");
        Long formalBaselineId = isPublishedRevisionBaseline(file) ? file.getId() : file.getRevisionBaseActiveControlledFileId();
        if (formalBaselineId == null && "REVISION".equals(file.getChangeType())) {
            throw exception(CONTROLLED_FILE_CHECKIN_NOT_ALLOWED);
        }
        if (formalBaselineId != null) {
            var origin = chain.stream().filter(f -> formalBaselineId.equals(f.getId())).findFirst().orElse(null);
            if (origin == null || !DccControlledFileVersionPolicy.isCurrentControlledStatus(origin.getStatus())
                    || !Objects.equals(file.getTenantId(), origin.getTenantId())
                    || !Objects.equals(file.getMasterId(), origin.getMasterId())
                    || !Objects.equals(file.getDccProjectCodeId(), origin.getDccProjectCodeId())
                    || !Objects.equals(file.getFileTypeTaxonomyId(), origin.getFileTypeTaxonomyId())
                    || !Objects.equals(file.getFileNumber(), origin.getFileNumber())
                    || !Objects.equals(file.getCategoryId(), origin.getCategoryId())
                    || !Objects.equals(sourceName, nameClaimService.requireSourceName(origin))
                    || versionPolicy.requireStored(origin).isWorkingIteration()
                    || !(versionPolicy.requireStored(file).formal().display().equals(versionPolicy.requireStored(origin).display())
                        || DccRevisionReworkPolicy.correctionMatchesBaseline(origin,file,chain,versionPolicy))) {
                throw exception(CONTROLLED_FILE_CHECKIN_NOT_ALLOWED);
            }
        }
        return new CheckinVersionSelection(versionPolicy.nextWorking(file, chain), formalBaselineId);
    }

    private record CheckinVersionSelection(DccControlledFileVersionPolicy.VersionNumber version,
                                           Long formalBaselineId) { }

    private int requirePreparedSourceAttributeRound(DccControlledFileDO source) {
        String type = "NEW".equals(source.getChangeType()) ? "UPLOAD"
                : "REVISION".equals(source.getChangeType()) ? "REVISION" : null;
        if (type == null) throw new IllegalArgumentException("EXECUTION_CONTRACT_SOURCE_ACTION_REQUIRED");
        if ("WORKING".equals(source.getStatus()) && source.getProcessInstanceId() == null)
            return applicationRoundService.requireDraft(type, source.getId());
        if (StrUtil.isBlank(source.getProcessInstanceId()))
            throw new IllegalArgumentException("EXECUTION_CONTRACT_SOURCE_BOUND_BPM_REQUIRED");
        int round = applicationRoundService.require(type, source.getId(), source.getProcessInstanceId());
        applicationRoundService.assertBoundRound(source.getDccProjectCodeId(), type, source.getId(), round);
        return round;
    }

    private DccControlledFileDO copyForCheckin(DccControlledFileDO file, Long userId,
                                               DccControlledFileVersionPolicy.VersionNumber version,
                                               DccControlledFilePreparedSource preparedSource,
                                               String previousHash,
                                               Long drawingPdfFileId,
                                               Long formalBaselineId,
                                               DccControlledFileCheckinReqVO reqVO) {
        String changeType = resolveCheckinChangeType(file);
        return DccControlledFileDO.builder()
                .tenantId(TenantContextHolder.getRequiredTenantId())
                .masterId(file.getMasterId())
                .categoryId(file.getCategoryId())
                .directoryId(file.getDirectoryId())
                .sourceFileId(preparedSource.sourceFileId())
                .originalFileId(file.getOriginalFileId())
                .drawingPdfFileId(drawingPdfFileId)
                .fileName(file.getFileName())
                .title(file.getTitle())
                .fileNumber(file.getFileNumber())
                .productMasterId(file.getProductMasterId())
                .productSource(file.getProductSource()).productCatalogId(file.getProductCatalogId())
                .productRelationId(file.getProductRelationId()).productCreateRequestId(file.getProductCreateRequestId())
                .productCode(file.getProductCode())
                .productName(file.getProductName())
                .dccProjectCodeId(file.getDccProjectCodeId())
                .fileTypeTaxonomyId(file.getFileTypeTaxonomyId())
                .fileTypeLevel1(file.getFileTypeLevel1())
                .fileTypeLevel2(file.getFileTypeLevel2())
                .fileTypeLevel3(file.getFileTypeLevel3())
                .fileTypeLevel4(file.getFileTypeLevel4())
                .fileTypeLevel5(file.getFileTypeLevel5())
                .needTraining(reqVO.getNeedTraining())
                .processType(file.getProcessType())
                .changeType(changeType)
                .versionNo(version.display())
                .revisionCode(version.majorIdentity())
                .iterationNo(version.iterationNo())
                .predecessorControlledFileId(file.getId())
                .revisionBaseActiveControlledFileId(
                        DccControlledFileChangeTypeEnum.REVISION.getCode().equals(changeType)
                                ? formalBaselineId : null)
                .sourceSha256(preparedSource.sourceSha256())
                .previousSourceSha256(previousHash)
                .changeDescription(StrUtil.trim(reqVO.getChangeDescription()))
                .effectiveDate(file.getEffectiveDate())
                .remark(reqVO.getRemark() == null ? file.getRemark() : StrUtil.trim(reqVO.getRemark()))
                .status("WORKING")
                .submitterId(userId)
                .requesterId(userId)
                .build();
    }

    private String resolveCheckinChangeType(DccControlledFileDO file) {
        if (DccControlledFileChangeTypeEnum.NEW.getCode().equals(file.getChangeType())
                && isPublishedRevisionBaseline(file)) {
            return DccControlledFileChangeTypeEnum.REVISION.getCode();
        }
        return file.getChangeType();
    }

    private void cleanupPreparedSourceIfNeeded(DccControlledFilePreparedSource preparedSource) {
        if (preparedSource == null || !preparedSource.isolatedCopy()) {
            return;
        }
        sourceOwnershipService.cleanupPreparedSource(preparedSource);
    }

    private void cleanupPreparedSourceAfterFailure(DccControlledFilePreparedSource preparedSource, RuntimeException failure) {
        try { cleanupPreparedSourceIfNeeded(preparedSource); }
        catch (RuntimeException cleanupFailure) { failure.addSuppressed(cleanupFailure); }
    }

    private DccControlledFileDO requireAccessibleControlledFile(Long userId, Long id) {
        DccControlledFileDO file = controlledFileMapper.selectById(id);
        if (file == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        if (!canAccessDetail(userId, file)) {
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }
        return file;
    }

    private DccControlledFileDO requireCheckoutAccessibleControlledFile(Long userId, Long id) {
        DccControlledFileDO file = controlledFileMapper.selectById(id);
        if (file == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        if (!canViewFileName(userId, file)) {
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }
        if (!Objects.equals(userId, file.getRequesterId()) && !isPublishedRevisionBaseline(file)) {
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }
        return file;
    }

    @Override
    public DccControlledFileAccessExplanationRespVO explainControlledFileAccess(Long userId, Long id) {
        DccControlledFileDO file = controlledFileMapper.selectById(id);
        if (file == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        boolean hasDirectoryManagementPermission = directoryAccessPermissionService.hasDirectoryManagementPermission(userId);
        return buildAccessExplanation(userId, file, hasDirectoryManagementPermission,
                canReadBinary(userId, file, DccAccessTypeEnum.PREVIEW, hasDirectoryManagementPermission),
                decideDownloadBinary(userId, file, hasDirectoryManagementPermission));
    }

    @Override
    public DccControlledFileUploadDirectoryTreeRespVO getUploadDirectoryTree(Long categoryId) {
        validateCategory(categoryId);
        DccCategoryDirectoryBindingDO binding = categoryDirectoryBindingMapper.selectActiveByCategoryId(categoryId);
        if (binding == null || binding.getDirectoryId() == null) {
            throw exception(FILE_CATEGORY_DIRECTORY_BINDING_NOT_EXISTS);
        }
        List<DccFileDirectoryDO> directories = directoryMapper.selectEnabledList();
        Map<Long, DccFileDirectoryDO> directoryMap = directories.stream()
                .collect(Collectors.toMap(DccFileDirectoryDO::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        DccFileDirectoryDO bindingDirectory = directoryMap.get(binding.getDirectoryId());
        if (bindingDirectory == null) {
            throw exception(FILE_DIRECTORY_NOT_EXISTS);
        }
        Map<Long, List<DccFileDirectoryDO>> childrenByParentId = groupChildrenByParentId(directories);
        return DccControlledFileUploadDirectoryTreeRespVO.builder()
                .bindingDirectoryId(bindingDirectory.getId())
                .bindingDirectoryPath(buildDirectoryPath(bindingDirectory.getId(), directoryMap))
                .leafBinding(!childrenByParentId.containsKey(bindingDirectory.getId()))
                .defaultUnclassified(false)
                .children(buildUploadDirectoryChildren(bindingDirectory.getId(), childrenByParentId))
                .build();
    }

    @Override
    public List<DccControlledFileUploadNameOptionRespVO> listUploadNameOptions(Long dccProjectCodeId,
                                                                               Long fileTypeTaxonomyId) {
        validateEnabledUploadNameProjectCode(dccProjectCodeId);
        DccControlledFilePageReqVO reqVO = buildUploadNameOptionReqVO(dccProjectCodeId, fileTypeTaxonomyId);
        return controlledFileMapper.selectWorkflowList(reqVO).stream()
                .filter(file -> StrUtil.isNotBlank(file.getFileName()))
                .sorted(Comparator.comparing(DccControlledFileDO::getFileName,
                                Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(DccControlledFileDO::getFileNumber,
                                Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(file -> DccControlledFileUploadNameOptionRespVO.builder()
                        .fileName(file.getFileName())
                        .currentVersionNo(file.getVersionNo())
                        .controlledFileId(file.getId())
                        .fileNumber(file.getFileNumber())
                        .build())
                .toList();
    }

    private void validateEnabledUploadNameProjectCode(Long dccProjectCodeId) {
        if (dccProjectCodeId == null) {
            throw exception(CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING);
        }
        DccProjectCodeDO projectCode = projectCodeMapper.selectById(dccProjectCodeId);
        if (projectCode == null) {
            throw exception(PROJECT_CODE_NOT_EXISTS);
        }
        if (!DccProjectCodeStatusConstants.ENABLE.equals(projectCode.getStatus())) {
            throw exception(PROJECT_CODE_DISABLED);
        }
    }

    private DccControlledFilePageReqVO buildUploadNameOptionReqVO(Long dccProjectCodeId, Long fileTypeTaxonomyId) {
        if (fileTypeTaxonomyId == null) {
            throw exception(CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING);
        }
        DccFileTypeTaxonomyPath path = fileTypeTaxonomyAdminService.resolveActivePath(fileTypeTaxonomyId);
        if (StrUtil.isBlank(path.level3())) {
            throw exception(FILE_TYPE_TAXONOMY_LEVEL_INVALID);
        }
        DccControlledFilePageReqVO reqVO = new DccControlledFilePageReqVO();
        reqVO.setDccProjectCodeId(dccProjectCodeId);
        reqVO.setFileTypeTaxonomyIds(fileTypeTaxonomyAdminService.listActiveDescendantIds(fileTypeTaxonomyId));
        reqVO.setFileTypeTaxonomyPaths(toFileTypeTaxonomyPathFilters(
                fileTypeTaxonomyAdminService.listActiveDescendantPaths(fileTypeTaxonomyId)));
        reqVO.setStatus(DccControlledFileStatusEnum.ACTIVE.getStatus());
        reqVO.setProcessType(DccControlledFileProcessTypeEnum.CONTROLLED_FILE.getCode());
        return reqVO;
    }

    private List<DccControlledFilePageReqVO.FileTypeTaxonomyPathFilter> toFileTypeTaxonomyPathFilters(
            List<DccFileTypeTaxonomyPath> paths) {
        return paths.stream()
                .map(item -> new DccControlledFilePageReqVO.FileTypeTaxonomyPathFilter(
                        item.level1(), item.level2(), item.level3(), item.level4(), item.level5()))
                .toList();
    }

    @Override
    public DccControlledFilePreviewMetadataRespVO getPreviewMetadata(Long userId, Long id,
                                                                     DccRequestAuditContext auditContext) {
        return getPreviewMetadata(userId, id, DccControlledFileBodyReadContext.daily(id), auditContext);
    }

    @Override
    public DccControlledFilePreviewMetadataRespVO getPreviewMetadata(Long userId, Long id,
            DccControlledFileBodyReadContext context, DccRequestAuditContext auditContext) {
        return withBodyReadContext(userId, id, context, () -> getPreviewMetadataInScope(userId, id, auditContext));
    }

    private DccControlledFilePreviewMetadataRespVO getPreviewMetadataInScope(Long userId, Long id,
            DccRequestAuditContext auditContext) {
        DccRequestAuditContext requiredAuditContext = requireAuditContext(auditContext);
        DccControlledFileDO file = controlledFileMapper.selectById(id);
        if (file == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        DccBoundWorkflowContractResolver.BoundExecution issuingBound = null;
        if (bodyReadContext.get() == null && DccControlledFileStatusEnum.PENDING_SUPERVISOR_REVIEW.getStatus().equals(file.getStatus())) {
            if (resolvePreviewReferenceId(file) == null) throw exception(CONTROLLED_FILE_ACCESS_DENIED);
            issuingBound = detailAuthorizationGuard.requireAuthenticatedCurrentReadBound(userId, file);
        } else if (!canReadBinary(userId, file, DccAccessTypeEnum.PREVIEW)) {
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }
        Long binaryFileId = resolveBinaryFileId(file, DccAccessTypeEnum.PREVIEW);
        BusinessFileAccessReference businessReference = requireBusinessFileAccess(
                BusinessFileAccessOperation.PREVIEW, userId, binaryFileId,
                TenantContextHolder.getRequiredTenantId(), null, requiredAuditContext, null);
        PreviewArtifactProjection previewProjection = resolvePreviewArtifactProjection(file);
        FileDO binaryFile = previewProjection.file();
        String previewFileName = resolvePreviewFileName(file, binaryFile);
        String previewContentType = binaryFile == null ? PREVIEW_UNAVAILABLE_CONTENT_TYPE : binaryFile.getType();
        DccControlledFilePreviewMetadataRespVO respVO = new DccControlledFilePreviewMetadataRespVO();
        DccControlledFilePreviewKindEnum previewKind =
                DccControlledFilePreviewKindEnum.resolve(previewFileName, previewContentType);
        respVO.setPreviewKind(previewKind.getCode());
        respVO.setFileName(previewFileName);
        respVO.setContentType(previewContentType);
        respVO.setWatermark(watermarkService.build(userId, "preview", previewFileName));
        if (previewProjection.unavailableReason() != null) {
            respVO.setPreviewUnavailableReason(previewProjection.unavailableReason());
            return respVO;
        }
        if (previewKind == DccControlledFilePreviewKindEnum.OFFICE && !onlyOfficePreviewProperties.isConfigured()) {
            respVO.setPreviewUnavailableReason(buildOnlyOfficeMissingReason());
            return respVO;
        }
        DccPreviewAccessResult accessResult = previewAccessService.prepareAccess(new DccPreviewAccessRequest(
                TenantContextHolder.getRequiredTenantId(),
                userId,
                file.getId(),
                file.getVersionNo(),
                file.getFileNumber(),
                PREVIEW_ACCESS_TYPE,
                currentPreviewPurpose(),
                PREVIEW_VIEWER_TOKEN_TTL_SECONDS,
                userId == null ? null : String.valueOf(userId),
                userId == null ? null : String.valueOf(userId),
                null,
                null,
                String.valueOf(TenantContextHolder.getRequiredTenantId()),
                "TRACE_CODE_ONLY",
                requiredAuditContext.sourceIp(),
                requiredAuditContext.userAgent(),
                requiredAuditContext.requireRequestId("preview metadata"),
                bodyReadContext.get() != null ? bodyReadContext.get().bpmRound()
                        : issuingBound == null ? null : issuingBound.processInstanceId()));
        requirePreviewAccessResult(accessResult);
        respVO.setViewerToken(accessResult.viewerToken());
        respVO.setViewerTokenId(accessResult.viewerTokenId());
        respVO.setViewerTokenNonce(accessResult.viewerTokenNonce());
        respVO.setAccessEventCode(accessResult.accessEventCode());
        respVO.setPreviewRegisteredAt(Objects.requireNonNull(accessResult.issuedAt(), "verified PREVIEW registration time")
                .atOffset(ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        respVO.setWatermarkTraceCode(accessResult.watermarkTraceCode());
        if (previewKind == DccControlledFilePreviewKindEnum.OFFICE) {
            applyOnlyOfficePreview(respVO, userId, file, binaryFile, businessReference, accessResult);
        }
        return respVO;
    }

    @Override
    public DccControlledFilePreviewMetadataRespVO getDistributionPreviewMetadata(Long userId, Long id, Long distributionId, Long recipientId,
            DccRequestAuditContext auditContext) {
        var context = new DccDistributionPreviewContext(userId, id, distributionId, recipientId);
        return withDistributionPreviewContext(context, () -> {
            var response = getPreviewMetadataInScope(userId, id, auditContext);
            var file = requireDistributionPreviewFile(context);
            response.setControlledFileId(file.getId());
            response.setDistributionId(context.distributionId());
            response.setRecipientId(context.recipientId());
            response.setVersionNo(file.getVersionNo());
            response.setApprovalProcessInstanceId(file.getProcessInstanceId());
            return response;
        });
    }

    @Override
    public DccControlledFileRespVO getDistributionPreviewDetail(Long userId, Long id, Long distributionId, Long recipientId) {
        var context = new DccDistributionPreviewContext(userId, id, distributionId, recipientId);
        return withDistributionPreviewContext(context, () -> {
            var file = requireDistributionPreviewFile(context);
            var response = new DccControlledFileRespVO();
            response.setId(file.getId());response.setTenantId(file.getTenantId());response.setMasterId(file.getMasterId());
            response.setCategoryId(file.getCategoryId());response.setDccProjectCodeId(file.getDccProjectCodeId());
            response.setFileName(file.getFileName());response.setTitle(file.getTitle());response.setFileNumber(file.getFileNumber());
            response.setVersionNo(file.getVersionNo());response.setStatus(file.getStatus());
            response.setBusinessSourceType("DCC_CONTROLLED_FILE");response.setControlled(true);response.setPendingEffect(false);
            response.setExecutable(false);response.setPublishedArtifactAvailable(true);
            response.setEffectiveDate(file.getEffectiveDate());response.setControlledTime(file.getControlledTime());
            response.setActivatedTime(file.getActivatedTime());response.setDistributedTime(file.getDistributedTime());
            fillApplicationSelectionProjection(response, file);
            response.setCanPreview(true);response.setCanDownload(false);response.setCanPrint(false);
            response.setCanPublish(false);response.setCanManualRelease(false);response.setCanObsolete(false);
            var actions = new DccControlledFileActionProjectionRespVO();
            actions.setAllowedActions(List.of(ACTION_PREVIEW));actions.setActionLocked(true);
            actions.setActionLockReason("DISTRIBUTION_READ_ONLY");actions.setCanWithdraw(false);
            response.setActionProjection(actions);
            return response;
        });
    }

    @Override
    public DccControlledFileBinary readDistributionPreviewFile(Long userId, Long id, Long distributionId, Long recipientId,
            String viewerToken, String accessEventCode, String watermarkTraceCode, String viewerTokenId,
            String viewerTokenNonce, DccRequestAuditContext auditContext) {
        var context = new DccDistributionPreviewContext(userId, id, distributionId, recipientId);
        return withDistributionPreviewContext(context, () -> readPreviewBinary(userId, id, viewerToken, accessEventCode,
                watermarkTraceCode, viewerTokenId, viewerTokenNonce, requireAuditContext(auditContext)));
    }

    @Override
    public boolean confirmDistributionOfficeBodyOpened(Long userId, Long id, Long distributionId, Long recipientId,
            String viewerToken, String accessEventCode, String watermarkTraceCode, String viewerTokenId,
            String viewerTokenNonce, DccRequestAuditContext auditContext) {
        Long tenant=TenantContextHolder.getRequiredTenantId();
        var login=cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUser();
        if (TenantContextHolder.isIgnore() || login==null || !Objects.equals(login.getId(),userId)
                || !Objects.equals(login.getTenantId(),tenant) || !Integer.valueOf(2).equals(login.getUserType())) {
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }
        requireAuditContext(auditContext).requireRequestId("authenticated Office body opened confirmation");
        requirePreviewContext(viewerToken,accessEventCode,watermarkTraceCode,viewerTokenId,viewerTokenNonce);
        var context=new DccDistributionPreviewContext(userId,id,distributionId,recipientId);
        return withDistributionPreviewContext(context,()->{
            var file=requireDistributionPreviewFile(context);
            var event=selectAccessEvent(accessEventCode);var trace=selectWatermarkTrace(watermarkTraceCode);
            requireMatchingPreviewEvidence(userId,file,event,trace);
            viewerTokenService.verify(viewerToken,new DccViewerTokenExpectedContext(tenant,userId,id,file.getVersionNo(),event.getId(),
                    context.purpose(),PREVIEW_VIEWER_TOKEN_TTL_SECONDS,viewerTokenNonce,viewerTokenId));
            Long successfulReads=accessLogMapper.selectCount(new LambdaQueryWrapperX<DccControlledFileAccessLogDO>()
                    .eq(DccControlledFileAccessLogDO::getTenantId,tenant)
                    .eq(DccControlledFileAccessLogDO::getControlledFileId,id)
                    .eq(DccControlledFileAccessLogDO::getFileVersionNo,file.getVersionNo())
                    .eq(DccControlledFileAccessLogDO::getUserId,userId)
                    .eq(DccControlledFileAccessLogDO::getAccessEventId,event.getId())
                    .eq(DccControlledFileAccessLogDO::getAccessEventCode,event.getAccessEventCode())
                    .eq(DccControlledFileAccessLogDO::getWatermarkTraceCode,trace.getTraceCode())
                    .eq(DccControlledFileAccessLogDO::getActionType,OFFICE_READ_ACTION_TYPE)
                    .eq(DccControlledFileAccessLogDO::getPurpose,context.purpose())
                    .eq(DccControlledFileAccessLogDO::getResult,DccAccessResultEnum.ALLOWED.name()));
            if (successfulReads==null || successfulReads<1) throw exception(CONTROLLED_FILE_ACCESS_DENIED);
            distributionReceiptService.recordDistributionBodyOpened(userId,id,distributionId,recipientId);
            return true;
        });
    }

    private DccControlledFileDO requireDistributionPreviewFile(DccDistributionPreviewContext context) {
        Long tenant = TenantContextHolder.getRequiredTenantId();
        if (TenantContextHolder.isIgnore()
                || !DccAccessUserIdentity.isEnabledTenantUser(context.userId(), adminUserApi.getUser(context.userId()))) {
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }
        var file = controlledFileMapper.selectById(context.controlledFileId());
        if (file == null || !Objects.equals(file.getId(), context.controlledFileId())
                || !Objects.equals(file.getTenantId(), tenant) || Boolean.TRUE.equals(file.getDeleted())
                || !"ACTIVE".equals(file.getStatus()) || file.getControlledTime() == null || file.getActivatedTime() == null
                || file.getDistributedTime() == null || file.getPublishedFileId() == null || StrUtil.isBlank(file.getVersionNo())) {
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }
        var distribution = distributionMapper.selectById(context.distributionId());
        if (distribution == null || !Objects.equals(distribution.getId(), context.distributionId())
                || !Objects.equals(distribution.getTenantId(), tenant) || Boolean.TRUE.equals(distribution.getDeleted())
                || !Objects.equals(distribution.getControlledFileId(), file.getId())
                || !"PUBLIC_FOLDER".equals(distribution.getDistributionMedium()) || distribution.getRecoveredAt() != null
                || !Set.of("PENDING", "SENT", "READ", "ACKNOWLEDGED").contains(distribution.getStatus())) {
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }
        var recipient = distributionRecipientMapper.selectById(context.recipientId());
        if (recipient == null || !Objects.equals(recipient.getId(), context.recipientId())
                || !Objects.equals(recipient.getTenantId(), tenant) || Boolean.TRUE.equals(recipient.getDeleted())
                || !Objects.equals(recipient.getDistributionId(), distribution.getId())
                || !Objects.equals(recipient.getUserId(), context.userId()) || recipient.getMessageJobId() == null) {
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }
        var job = messageJobMapper.selectById(recipient.getMessageJobId());
        if (job == null || !Objects.equals(job.getId(), recipient.getMessageJobId())
                || !Objects.equals(job.getTenantId(), tenant) || Boolean.TRUE.equals(job.getDeleted())
                || !"DISTRIBUTION".equals(job.getBusinessType()) || !Objects.equals(job.getBusinessId(), distribution.getId())
                || !Objects.equals(job.getRecipientUserId(), context.userId()) || !"SENT".equals(job.getStatus())
                || job.getSentAt() == null) {
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }
        return file;
    }

    private <T> T withDistributionPreviewContext(DccDistributionPreviewContext context, Supplier<T> action) {
        if (distributionPreviewContext.get() != null) throw new IllegalStateException("distribution preview scope is already bound");
        requireDistributionPreviewFile(context);
        distributionPreviewContext.set(context);
        try { return action.get(); }
        finally { distributionPreviewContext.remove(); }
    }

    private String currentPreviewPurpose() {
        if (bodyReadContext.get() != null) return bodyReadContext.get().purpose();
        var context = distributionPreviewContext.get();
        return context == null ? CONTROLLED_PREVIEW_PURPOSE : context.purpose();
    }

    private void requireNoDistributionPreviewContext() {
        if (distributionPreviewContext.get() != null) throw exception(CONTROLLED_FILE_ACCESS_DENIED);
    }

    private <T> T withBodyReadContext(Long actor, Long id, DccControlledFileBodyReadContext context, Supplier<T> read) {
        if (bodyReadContext.get() != null || distributionPreviewContext.get() != null || attachmentBusinessRead.get() != null)
            throw new IllegalStateException("BODY_READ_SCOPE_ALREADY_BOUND");
        Objects.requireNonNull(context, "exact body read context");
        var file = controlledFileMapper.selectById(id);
        if (file == null) throw exception(CONTROLLED_FILE_NOT_EXISTS);
        detailAuthorizationGuard.requireBodyRead(actor, file, context);
        bodyReadContext.set(context);
        try { return read.get(); }
        finally { bodyReadContext.remove(); }
    }

    @Override
    public DccControlledFilePreviewMetadataRespVO getAttachmentPreviewMetadata(Long userId, Long id, Long attachmentId,
                                                                               DccRequestAuditContext auditContext) {
        requireNoDistributionPreviewContext();
        DccRequestAuditContext requiredAuditContext = requireAuditContext(auditContext);
        DccControlledFileDO file = controlledFileMapper.selectById(id);
        if (file == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        if (DccControlledFileStatusEnum.PENDING_SUPERVISOR_REVIEW.getStatus().equals(file.getStatus()))
            if (!detailAuthorizationGuard.hasAuthenticatedCompilerAccess(userId, file)) throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        if (!canReadBinary(userId, file, DccAccessTypeEnum.PREVIEW)) {
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }
        DccControlledFileAttachmentBinary attachment = attachmentService.getAttachment(id, attachmentId);
        FileDO binaryFile = fileMapper.selectById(attachment.storageFileId());
        if (binaryFile == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        BusinessFileAccessReference businessReference = requireAttachmentBusinessFileAccess(
                userId, id, binaryFile.getId(), requiredAuditContext);
        String previewFileName = StrUtil.blankToDefault(attachment.fileName(), binaryFile.getName());
        String previewContentType = StrUtil.blankToDefault(attachment.contentType(), binaryFile.getType());
        DccControlledFilePreviewMetadataRespVO respVO = new DccControlledFilePreviewMetadataRespVO();
        DccControlledFilePreviewKindEnum previewKind =
                DccControlledFilePreviewKindEnum.resolve(previewFileName, previewContentType);
        respVO.setPreviewKind(previewKind.getCode());
        respVO.setFileName(previewFileName);
        respVO.setContentType(previewContentType);
        respVO.setWatermark(watermarkService.build(userId, "preview", previewFileName));
        if (previewKind == DccControlledFilePreviewKindEnum.OFFICE) {
            respVO.setPreviewUnavailableReason("普通附件暂不支持 Office 在线预览，请上传 PDF 或图片附件后查看。");
            return respVO;
        }
        DccPreviewAccessResult accessResult = previewAccessService.prepareAccess(new DccPreviewAccessRequest(
                TenantContextHolder.getRequiredTenantId(),
                userId,
                file.getId(),
                file.getVersionNo(),
                file.getFileNumber(),
                PREVIEW_ACCESS_TYPE,
                CONTROLLED_PREVIEW_PURPOSE,
                PREVIEW_VIEWER_TOKEN_TTL_SECONDS,
                userId == null ? null : String.valueOf(userId),
                userId == null ? null : String.valueOf(userId),
                null,
                null,
                String.valueOf(TenantContextHolder.getRequiredTenantId()),
                "TRACE_CODE_ONLY",
                requiredAuditContext.sourceIp(),
                requiredAuditContext.userAgent(),
                requiredAuditContext.requireRequestId("attachment preview metadata")));
        requirePreviewAccessResult(accessResult);
        respVO.setViewerToken(accessResult.viewerToken());
        respVO.setViewerTokenId(accessResult.viewerTokenId());
        respVO.setViewerTokenNonce(accessResult.viewerTokenNonce());
        respVO.setAccessEventCode(accessResult.accessEventCode());
        respVO.setPreviewRegisteredAt(Objects.requireNonNull(accessResult.issuedAt(), "verified attachment PREVIEW registration time")
                .atOffset(ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        respVO.setWatermarkTraceCode(accessResult.watermarkTraceCode());
        return respVO;
    }

    @Override
    public DccControlledFileBinary readPreviewFile(Long userId, Long id, String viewerToken,
                                                   String accessEventCode, String watermarkTraceCode,
                                                   String viewerTokenId, String viewerTokenNonce,
                                                   DccRequestAuditContext auditContext) {
        return readPreviewFile(userId, id, DccControlledFileBodyReadContext.daily(id), viewerToken, accessEventCode,
                watermarkTraceCode, viewerTokenId, viewerTokenNonce, auditContext);
    }

    @Override
    public DccControlledFileBinary readPreviewFile(Long userId, Long id, DccControlledFileBodyReadContext context,
            String viewerToken, String accessEventCode, String watermarkTraceCode, String viewerTokenId,
            String viewerTokenNonce, DccRequestAuditContext auditContext) {
        return withBodyReadContext(userId, id, context, () -> readPreviewBinary(userId, id, viewerToken,
                accessEventCode, watermarkTraceCode, viewerTokenId, viewerTokenNonce, requireAuditContext(auditContext)));
    }

    @Override
    public DccControlledFileBinary readAttachmentPreviewFile(Long userId, Long id, Long attachmentId,
                                                             String viewerToken, String accessEventCode,
                                                             String watermarkTraceCode, String viewerTokenId,
                                                             String viewerTokenNonce,
                                                             DccRequestAuditContext auditContext) {
        return readAttachmentPreviewBinary(userId, id, attachmentId, viewerToken, accessEventCode,
                watermarkTraceCode, viewerTokenId, viewerTokenNonce, requireAuditContext(auditContext));
    }

    @Override
    public DccDownloadFileBinary readDownloadFile(Long userId, Long id, Boolean nonControlledWarningConfirmed,
                                                  String downloadRequestId, DccRequestAuditContext auditContext) {
        String normalizedDownloadRequestId = requireDownloadRequestId(downloadRequestId);
        DccRequestAuditContext requiredAuditContext = requireAuditContext(auditContext)
                .withRequestId(normalizedDownloadRequestId);
        requireDownloadRequestUnused(normalizedDownloadRequestId);
        return readDownloadBinary(userId, id, nonControlledWarningConfirmed, normalizedDownloadRequestId,
                requiredAuditContext);
    }

    @Override
    public DccDownloadFileBinary readAttachmentDownloadFile(Long userId, Long id, Long attachmentId,
            Boolean nonControlledWarningConfirmed, String downloadRequestId, DccRequestAuditContext auditContext) {
        requireNoDistributionPreviewContext();
        String requestId = requireDownloadRequestId(downloadRequestId);
        var audit = requireAuditContext(auditContext).withRequestId(requestId);
        requireDownloadRequestUnused(requestId);
        var file = controlledFileMapper.selectById(id);
        if (file == null || !Objects.equals(file.getId(), id) || !Objects.equals(file.getTenantId(), TenantContextHolder.getRequiredTenantId())
                || Boolean.TRUE.equals(file.getDeleted())) throw exception(CONTROLLED_FILE_NOT_EXISTS);
        if (!Boolean.TRUE.equals(nonControlledWarningConfirmed)) {
            recordAccess(id,userId,DccAccessTypeEnum.DOWNLOAD,false,"WARNING_UNCONFIRMED",audit);
            throw exception(CONTROLLED_FILE_DOWNLOAD_WARNING_UNCONFIRMED);
        }
        var policy = decideDownloadBinary(userId,file);
        if (!policy.allowed()) {
            recordAccess(id,userId,DccAccessTypeEnum.DOWNLOAD,false,"ATTACHMENT_ACCESS_DENIED",audit);
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }
        var attachment = attachmentService.getAttachment(id, attachmentId);
        requireBusinessFileAccess(BusinessFileAccessOperation.DOWNLOAD,userId,attachment.storageFileId(),
                TenantContextHolder.getRequiredTenantId(),null,audit,null);
        var physical = fileMapper.selectById(attachment.storageFileId());
        if (physical == null || !Objects.equals(physical.getId(), attachment.storageFileId()) || Boolean.TRUE.equals(physical.getDeleted())
                || !Objects.equals(physical.getName(),attachment.fileName()) || !Objects.equals(physical.getType(),attachment.contentType())
                || !Objects.equals(physical.getSize(),attachment.fileSize()) || physical.getConfigId() == null || StrUtil.isBlank(physical.getPath()))
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        var requestedAt = LocalDateTime.now(ZoneOffset.UTC);
        var event = createDownloadAccessEvent(file,userId,requestId,requestedAt,audit,"ATTACHMENT_DOWNLOAD:"+attachmentId);
        var record = createDownloadRecord(file,userId,requestId,event,policy.policyVersion(),requestedAt);
        return readOpenableDownloadBinary(userId,file,physical,event,record,requestId);
    }

    @Override
    public DccControlledFileBinary readOnlyOfficePreviewFile(Long id, String token,
                                                            DccRequestAuditContext auditContext) throws Exception {
        DccRequestAuditContext requiredAuditContext = requireAuditContext(auditContext);
        requireOnlyOfficeConfigured();
        DccOnlyOfficePreviewTokenService.PreviewTokenPayload tokenPayload =
                onlyOfficePreviewTokenService.verifyControlledFile(token, id);
        Long oldTenantId = TenantContextHolder.getTenantId();
        boolean oldIgnore = TenantContextHolder.isIgnore();
        if (!oldIgnore && oldTenantId != null && !Objects.equals(oldTenantId, tokenPayload.getTenantId())) {
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
        }
        try {
            TenantContextHolder.setTenantId(tokenPayload.getTenantId());
            TenantContextHolder.setIgnore(false);
            return readOnlyOfficePreviewBinary(id, tokenPayload, requiredAuditContext);
        } finally {
            restoreTenantContext(oldTenantId, oldIgnore);
        }
    }

    private DccControlledFileBinary readOnlyOfficePreviewBinary(Long id,
                                                               DccOnlyOfficePreviewTokenService.PreviewTokenPayload tokenPayload,
                                                               DccRequestAuditContext auditContext)
            throws Exception {
        if (bodyReadContext.get() == null && tokenPayload.getBodyReadContext() != null) {
            if (distributionPreviewContext.get() != null) throw exception(CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
            var context = DccControlledFileBodyReadContext.fromWire(id, tokenPayload.getBodyReadContext());
            if (!Objects.equals(context.purpose(), tokenPayload.getPurpose())) throw exception(CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
            bodyReadContext.set(context);
            try { return readOnlyOfficePreviewBinary(id, tokenPayload, auditContext); }
            finally { bodyReadContext.remove(); }
        }
        if (distributionPreviewContext.get() == null && tokenPayload.getPurpose() != null
                && tokenPayload.getPurpose().startsWith(DccDistributionPreviewContext.PURPOSE_PREFIX)) {
            var context = DccDistributionPreviewContext.fromPurpose(tokenPayload.getUserId(), id, tokenPayload.getPurpose());
            return readOnlyOfficeInDistributionScope(context, id, tokenPayload, auditContext);
        }
        DccRequestAuditContext officeAuditContext = auditContext.withRequestId(
                auditContext.requestIdOr(tokenPayload.getTokenId()));
        DccControlledFileDO file = controlledFileMapper.selectById(id);
        if (file == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        requireMatchingOnlyOfficeToken(tokenPayload, file);
        if (!Objects.equals(tokenPayload.getInfraFileId(), resolveBinaryFileId(file, DccAccessTypeEnum.PREVIEW)))
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
        if (distributionPreviewContext.get() != null && !Objects.equals(tokenPayload.getInfraFileId(), file.getPublishedFileId())) {
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
        }
        DccControlledFileAccessEventDO accessEvent = selectAccessEvent(tokenPayload.getAccessEventId());
        DccControlledFileWatermarkTraceDO watermarkTrace = selectWatermarkTraceByAccessEventId(accessEvent.getId());
        requireMatchingOnlyOfficeEvidence(tokenPayload, file, accessEvent, watermarkTrace);
        if (bodyReadContext.get() != null) {
            if (!Objects.equals(tokenPayload.getTenantId(), accessEvent.getTenantId())
                    || !Objects.equals(tokenPayload.getTenantId(), file.getTenantId())) throw exception(CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
            var subject = new DccVerifiedOfficeReadActor(tokenPayload.getTenantId(), tokenPayload.getUserId(), file.getId(),
                    file.getVersionNo(), bodyReadContext.get().bpmRound(), tokenPayload.getPurpose(), accessEvent.getId(),
                    accessEvent.getAccessEventCode(), watermarkTrace.getTraceCode(), bodyReadContext.get());
            return detailAuthorizationGuard.withVerifiedOfficeReadActor(subject,
                    () -> readValidatedOnlyOfficePreviewBinary(file, tokenPayload, accessEvent, watermarkTrace, officeAuditContext));
        }
        if (distributionPreviewContext.get() == null && !"ACTIVE".equals(file.getStatus()))
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        if (distributionPreviewContext.get() == null) {
            if (!Objects.equals(tokenPayload.getTenantId(), file.getTenantId())
                    || !Objects.equals(accessEvent.getTenantId(), file.getTenantId())) throw exception(CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
            var subject = new DccVerifiedOfficeReadActor(tokenPayload.getTenantId(), tokenPayload.getUserId(), file.getId(),
                    file.getVersionNo(), file.getProcessInstanceId(), tokenPayload.getPurpose(), accessEvent.getId(),
                    accessEvent.getAccessEventCode(), watermarkTrace.getTraceCode());
            return detailAuthorizationGuard.withVerifiedOfficeReadActor(subject, () -> {
                detailAuthorizationGuard.requireBodyRead(tokenPayload.getUserId(), file, DccControlledFileBodyReadContext.daily(id));
                return readValidatedOnlyOfficePreviewBinary(file, tokenPayload, accessEvent, watermarkTrace, officeAuditContext);
            });
        }
        if (DccControlledFileStatusEnum.PENDING_SUPERVISOR_REVIEW.getStatus().equals(file.getStatus())
                && CONTROLLED_PREVIEW_PURPOSE.equals(tokenPayload.getPurpose())) {
            requireSupervisorPreviewRoundEvidence(file, accessEvent);
            if (!Objects.equals(tokenPayload.getTenantId(), accessEvent.getTenantId()))
                throw exception(CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
            var subject = new DccVerifiedOfficeReadActor(tokenPayload.getTenantId(), tokenPayload.getUserId(), file.getId(),
                    file.getVersionNo(), accessEvent.getProcessInstanceId(), tokenPayload.getPurpose(), accessEvent.getId(),
                    accessEvent.getAccessEventCode(), watermarkTrace.getTraceCode());
            return detailAuthorizationGuard.withVerifiedOfficeReadActor(subject,
                    () -> readValidatedOnlyOfficePreviewBinary(file, tokenPayload, accessEvent, watermarkTrace, officeAuditContext));
        }
        return readValidatedOnlyOfficePreviewBinary(file, tokenPayload, accessEvent, watermarkTrace, officeAuditContext);
    }

    private DccControlledFileBinary readValidatedOnlyOfficePreviewBinary(DccControlledFileDO file,
            DccOnlyOfficePreviewTokenService.PreviewTokenPayload tokenPayload, DccControlledFileAccessEventDO accessEvent,
            DccControlledFileWatermarkTraceDO watermarkTrace, DccRequestAuditContext officeAuditContext) throws Exception {
        requireBusinessFileTokenAccess(BusinessFileAccessOperation.ONLYOFFICE_PREVIEW,
                tokenPayload.getUserId(), tokenPayload.getInfraFileId(), tokenPayload.getTenantId(),
                tokenPayload.getServiceIdentity(), officeAuditContext, tokenPayload.toBusinessFileReference());
        if (!canReadBinary(tokenPayload.getUserId(), file, DccAccessTypeEnum.PREVIEW)) {
            recordOnlyOfficeReadAccess(file, tokenPayload.getUserId(), accessEvent, watermarkTrace, false,
                    "ACCESS_DENIED", officeAuditContext);
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }
        FileDO binaryFile = resolveBinaryFileRecord(file, DccAccessTypeEnum.PREVIEW);
        byte[] content;
        try {
            content = fileService.getFileContent(binaryFile.getConfigId(), binaryFile.getPath());
            recordOnlyOfficeReadAccess(file, tokenPayload.getUserId(), accessEvent, watermarkTrace, true,
                    "OK", officeAuditContext);
        } catch (Exception ex) {
            recordOnlyOfficeReadAccess(file, tokenPayload.getUserId(), accessEvent, watermarkTrace, false,
                    StrUtil.blankToDefault(ex.getMessage(), "READ_FAILED"), officeAuditContext);
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }
        return new DccControlledFileBinary(binaryFile.getName(), binaryFile.getType(), content, null);
    }

    private DccControlledFileBinary readOnlyOfficeInDistributionScope(DccDistributionPreviewContext context, Long id,
            DccOnlyOfficePreviewTokenService.PreviewTokenPayload tokenPayload, DccRequestAuditContext auditContext) throws Exception {
        if (distributionPreviewContext.get() != null) throw new IllegalStateException("distribution preview scope is already bound");
        requireDistributionPreviewFile(context);
        distributionPreviewContext.set(context);
        try { return readOnlyOfficePreviewBinary(id, tokenPayload, auditContext); }
        finally { distributionPreviewContext.remove(); }
    }

    @Override
    public DccControlledFileScope identifyControlledFileScope(Long infraFileId) {
        if (infraFileId == null) {
            throw new IllegalArgumentException("infraFileId is required");
        }
        List<DccControlledFileArtifactReference> references = new ArrayList<>();
        List<DccControlledFileDO> controlledFiles = Objects.requireNonNull(
                controlledFileMapper.selectList(new LambdaQueryWrapperX<DccControlledFileDO>()
                        .and(query -> query.eq(DccControlledFileDO::getSourceFileId, infraFileId)
                                .or().eq(DccControlledFileDO::getOriginalFileId, infraFileId)
                                .or().eq(DccControlledFileDO::getDrawingPdfFileId, infraFileId)
                                .or().eq(DccControlledFileDO::getTrainingRecordFileId, infraFileId)
                                .or().eq(DccControlledFileDO::getPublishedFileId, infraFileId)
                                .or().eq(DccControlledFileDO::getStampedFileId, infraFileId))),
                "controlledFiles");
        for (DccControlledFileDO file : controlledFiles) {
            Long controlledFileId = Objects.requireNonNull(file.getId(), "controlledFileId");
            Long tenantId = Objects.requireNonNull(file.getTenantId(), "controlledFileTenantId");
            appendArtifactReference(references, infraFileId, controlledFileId,
                    tenantId, file.getSourceFileId(), DccControlledFileArtifactRole.SOURCE);
            appendArtifactReference(references, infraFileId, controlledFileId,
                    tenantId, file.getOriginalFileId(), DccControlledFileArtifactRole.ORIGINAL);
            appendArtifactReference(references, infraFileId, controlledFileId,
                    tenantId, file.getDrawingPdfFileId(), DccControlledFileArtifactRole.DRAWING_PDF);
            appendArtifactReference(references, infraFileId, controlledFileId,
                    tenantId, file.getTrainingRecordFileId(), DccControlledFileArtifactRole.TRAINING_RECORD);
            appendArtifactReference(references, infraFileId, controlledFileId,
                    tenantId, file.getPublishedFileId(), DccControlledFileArtifactRole.PUBLISHED);
            appendArtifactReference(references, infraFileId, controlledFileId,
                    tenantId, file.getStampedFileId(), DccControlledFileArtifactRole.STAMPED);
        }
        List<DccExternalFileReviewDO> externalReviews = Objects.requireNonNull(
                externalReviewMapper.selectList(new LambdaQueryWrapperX<DccExternalFileReviewDO>()
                        .eq(DccExternalFileReviewDO::getOutputFileId, infraFileId)),
                "externalReviews");
        for (DccExternalFileReviewDO review : externalReviews) {
            appendArtifactReference(references, infraFileId,
                    Objects.requireNonNull(review.getControlledFileId(), "externalReviewControlledFileId"),
                    Objects.requireNonNull(review.getTenantId(), "externalReviewTenantId"),
                    review.getOutputFileId(), DccControlledFileArtifactRole.EXTERNAL_REVIEW_OUTPUT);
        }
        for (Long controlledFileId : attachmentService.listControlledFileIdsByStorageFileId(infraFileId)) {
            DccControlledFileDO file = controlledFileMapper.selectById(controlledFileId);
            if (file != null) {
                references.add(new DccControlledFileArtifactReference(controlledFileId,
                        Objects.requireNonNull(file.getTenantId(), "attachmentControlledFileTenantId"),
                        DccControlledFileArtifactRole.ATTACHMENT));
            }
        }
        return new DccControlledFileScope(infraFileId, references);
    }

    @Override
    public void assertBusinessFileAccess(Long userId, Long controlledFileId,
                                         BusinessFileAccessOperation operation,
                                         DccRequestAuditContext auditContext) {
        if (operation == null) {
            throw new IllegalArgumentException("business file access operation is required");
        }
        DccRequestAuditContext requiredAuditContext = requireAuditContext(auditContext);
        DccControlledFileDO file = controlledFileMapper.selectById(controlledFileId);
        if (file == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        DccAccessTypeEnum accessType = operation == BusinessFileAccessOperation.DOWNLOAD
                ? DccAccessTypeEnum.DOWNLOAD : DccAccessTypeEnum.PREVIEW;
        boolean allowed = switch (operation) {
            case PREVIEW, ONLYOFFICE_PREVIEW ->
                    canReadBusinessBody(userId, file);
            case PRINT -> canPrintControlledFile(userId, file);
            case CONVERT -> false;
            case DOWNLOAD -> decideDownloadBinary(userId, file).allowed();
            case DIRECT_LINK -> false;
        };
        if (!allowed) {
            recordAccess(file.getId(), userId, accessType, false,
                    "BUSINESS_FILE_GATE_DENIED:" + operation.name(), requiredAuditContext);
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }
    }

    private boolean canReadBusinessBody(Long actor, DccControlledFileDO file) {
        if (distributionPreviewContext.get() != null || bodyReadContext.get() != null)
            return canReadBinary(actor, file, DccAccessTypeEnum.PREVIEW);
        var attachment = attachmentBusinessRead.get();
        if (attachment != null && Objects.equals(attachment.userId(), actor)
                && Objects.equals(attachment.controlledFileId(), file.getId())
                && attachmentService.listControlledFileIdsByStorageFileId(attachment.storageFileId()).contains(file.getId()))
            return canReadBinary(actor, file, DccAccessTypeEnum.PREVIEW);
        detailAuthorizationGuard.requireBodyRead(actor, file, DccControlledFileBodyReadContext.daily(file.getId()));
        return canReadBinary(actor, file, DccAccessTypeEnum.PREVIEW);
    }

    private boolean isDailyBodyState(DccControlledFileDO file) {
        return file != null && "ACTIVE".equals(file.getStatus()) && file.getActivatedTime() != null;
    }

    private void restrictDailyBodyProjection(DccControlledFileRespVO response, DccControlledFileDO file) {
        response.setCanPreview(isDailyBodyState(file) && Boolean.TRUE.equals(response.getCanPreview()));
        if (response.getVersionHistory() == null) return;
        var chain = file.getMasterId() == null ? List.of(file) : controlledFileMapper.selectListByMasterId(file.getMasterId());
        var identities = chain.stream().collect(Collectors.toMap(DccControlledFileDO::getId, Function.identity()));
        response.getVersionHistory().forEach(history -> history.setCanPreview(
                isDailyBodyState(identities.get(history.getId())) && Boolean.TRUE.equals(history.getCanPreview())));
    }

    private BusinessFileAccessReference requireAttachmentBusinessFileAccess(Long actor, Long id, Long storageFileId,
            DccRequestAuditContext audit) {
        if (attachmentBusinessRead.get() != null || bodyReadContext.get() != null || distributionPreviewContext.get() != null)
            throw new IllegalStateException("ATTACHMENT_READ_SCOPE_ALREADY_BOUND");
        attachmentBusinessRead.set(new AttachmentBusinessRead(actor, id, storageFileId));
        try { return requireBusinessFileAccess(BusinessFileAccessOperation.PREVIEW, actor, storageFileId,
                TenantContextHolder.getRequiredTenantId(), null, audit, null); }
        finally { attachmentBusinessRead.remove(); }
    }

    private void appendArtifactReference(List<DccControlledFileArtifactReference> references, Long infraFileId,
                                         Long controlledFileId, Long tenantId, Long artifactFileId,
                                         DccControlledFileArtifactRole role) {
        if (Objects.equals(infraFileId, artifactFileId)) {
            references.add(new DccControlledFileArtifactReference(controlledFileId, tenantId, role));
        }
    }

    private DccControlledFileBinary readPreviewBinary(Long userId, Long id, String viewerToken,
                                                      String accessEventCode, String watermarkTraceCode,
                                                      String viewerTokenId, String viewerTokenNonce,
                                                      DccRequestAuditContext auditContext) {
        requirePreviewContext(viewerToken, accessEventCode, watermarkTraceCode, viewerTokenId, viewerTokenNonce);
        DccControlledFileAccessEventDO accessEvent = selectAccessEvent(accessEventCode);
        if (distributionPreviewContext.get() == null && bodyReadContext.get() == null && accessEvent.getPurpose() != null
                && accessEvent.getPurpose().startsWith(DccDistributionPreviewContext.PURPOSE_PREFIX)) {
            var context = DccDistributionPreviewContext.fromPurpose(userId, id, accessEvent.getPurpose());
            return withDistributionPreviewContext(context, () -> readPreviewBinary(userId, id, viewerToken, accessEventCode,
                    watermarkTraceCode, viewerTokenId, viewerTokenNonce, auditContext));
        }
        DccControlledFileDO file = controlledFileMapper.selectById(id);
        if (file == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        if (DccControlledFileStatusEnum.PENDING_SUPERVISOR_REVIEW.getStatus().equals(file.getStatus())
                && !detailAuthorizationGuard.hasAuthenticatedReadActor(userId, file)) throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        if (!canReadBinary(userId, file, DccAccessTypeEnum.PREVIEW)) {
            recordAccess(file.getId(), userId, DccAccessTypeEnum.PREVIEW, false, "ACCESS_DENIED",
                    auditContext.withRequestId(auditContext.requestIdOr(accessEventCode)));
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }
        Long binaryFileId = resolveBinaryFileId(file, DccAccessTypeEnum.PREVIEW);
        requireBusinessFileAccess(BusinessFileAccessOperation.PREVIEW, userId, binaryFileId,
                TenantContextHolder.getRequiredTenantId(), null,
                auditContext.withRequestId(auditContext.requestIdOr(accessEventCode)), null);
        DccControlledFileWatermarkTraceDO watermarkTrace = selectWatermarkTrace(watermarkTraceCode);
        requireMatchingPreviewEvidence(userId, file, accessEvent, watermarkTrace);
        viewerTokenService.verify(viewerToken, new DccViewerTokenExpectedContext(
                TenantContextHolder.getRequiredTenantId(),
                userId,
                file.getId(),
                file.getVersionNo(),
                accessEvent.getId(),
                currentPreviewPurpose(),
                PREVIEW_VIEWER_TOKEN_TTL_SECONDS,
                viewerTokenNonce,
                viewerTokenId));

        FileDO binaryFile;
        try {
            binaryFile = resolveBinaryFileRecord(file, DccAccessTypeEnum.PREVIEW);
        } catch (ServiceException ex) {
            recordPreviewAccess(file, userId, accessEvent, watermarkTrace, false,
                    "PUBLISHED_FILE_MISSING", auditContext.withRequestId(auditContext.requestIdOr(accessEventCode)));
            throw ex;
        }
        byte[] content;
        try {
            content = fileService.getFileContent(binaryFile.getConfigId(), binaryFile.getPath());
            recordPreviewAccess(file, userId, accessEvent, watermarkTrace, true,
                    "OK", auditContext.withRequestId(auditContext.requestIdOr(accessEventCode)));
        } catch (Exception ex) {
            recordPreviewAccess(file, userId, accessEvent, watermarkTrace, false,
                    StrUtil.blankToDefault(ex.getMessage(), "READ_FAILED"),
                    auditContext.withRequestId(auditContext.requestIdOr(accessEventCode)));
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }
        recordDistributionBodyOpenedIfScoped(userId, file);
        return new DccControlledFileBinary(binaryFile.getName(), binaryFile.getType(), content,
                watermarkService.build(userId, "preview", binaryFile.getName()));
    }

    private void recordDistributionBodyOpenedIfScoped(Long userId, DccControlledFileDO file) {
        var context = distributionPreviewContext.get();
        if (context == null) return;
        if (!Objects.equals(context.userId(), userId) || !Objects.equals(context.controlledFileId(), file.getId())) {
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }
        distributionReceiptService.recordDistributionBodyOpened(userId, file.getId(), context.distributionId(), context.recipientId());
    }

    private DccDownloadFileBinary readDownloadBinary(Long userId, Long id, Boolean nonControlledWarningConfirmed,
                                                     String downloadRequestId,
                                                     DccRequestAuditContext auditContext) {
        DccControlledFileDO file = controlledFileMapper.selectById(id);
        if (file == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        if (!Boolean.TRUE.equals(nonControlledWarningConfirmed)) {
            recordAccess(file.getId(), userId, DccAccessTypeEnum.DOWNLOAD, false,
                    "WARNING_UNCONFIRMED", auditContext);
            throw exception(CONTROLLED_FILE_DOWNLOAD_WARNING_UNCONFIRMED);
        }
        Long sourceFileId = resolveBinaryFileId(file, DccAccessTypeEnum.DOWNLOAD);
        requireBusinessFileAccess(BusinessFileAccessOperation.DOWNLOAD, userId, sourceFileId,
                TenantContextHolder.getRequiredTenantId(), null, auditContext, null);
        DccDownloadPolicyDecision policyDecision = decideDownloadBinary(userId, file);
        if (!policyDecision.allowed()) {
            recordAccess(file.getId(), userId, DccAccessTypeEnum.DOWNLOAD, false,
                    "ACCESS_DENIED", auditContext);
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }
        FileDO sourceFile;
        try {
            sourceFile = resolveBinaryFileRecord(file, DccAccessTypeEnum.DOWNLOAD);
        } catch (ServiceException ex) {
            recordAccess(file.getId(), userId, DccAccessTypeEnum.DOWNLOAD, false,
                    "PUBLISHED_FILE_MISSING", auditContext);
            throw ex;
        }
        LocalDateTime requestedAt = LocalDateTime.now(ZoneOffset.UTC);
        DccControlledFileAccessEventDO accessEvent = createDownloadAccessEvent(file, userId, downloadRequestId,
                requestedAt, auditContext);
        DccControlledFileDownloadRecordDO downloadRecord = createDownloadRecord(file, userId, downloadRequestId,
                accessEvent, policyDecision.policyVersion(), requestedAt);
        return readOpenableDownloadBinary(userId, file, sourceFile, accessEvent, downloadRecord, downloadRequestId);
    }

    private DccDownloadFileBinary readOpenableDownloadBinary(Long userId, DccControlledFileDO file,
                                                            FileDO sourceFile,
                                                            DccControlledFileAccessEventDO accessEvent,
                                                            DccControlledFileDownloadRecordDO downloadRecord,
                                                            String downloadRequestId) {
        try {
            byte[] content = Objects.requireNonNull(
                    fileService.getFileContent(sourceFile.getConfigId(), sourceFile.getPath()),
                    "download file content");
            String plainSha256 = sha256Hex(content);
            LocalDateTime returnedAt = LocalDateTime.now(ZoneOffset.UTC);
            recordSuccessfulDownloadAudit(downloadRecord, plainSha256, returnedAt, file, userId, accessEvent);
            return new DccDownloadFileBinary(
                    sourceFile.getName(),
                    sourceFile.getType(),
                    content,
                    downloadRequestId,
                    accessEvent.getAccessEventCode(),
                    plainSha256);
        } catch (ServiceException ex) {
            String failureCode = downloadFailureCode(ex);
            markDownloadRecordFailed(downloadRecord, failureCode,
                    StrUtil.blankToDefault(ex.getMessage(), failureCode));
            recordDownloadAccess(file, userId, accessEvent, false, failureCode);
            throw ex;
        } catch (Exception ex) {
            markDownloadRecordFailed(downloadRecord, FAILURE_SOURCE_READ_FAILED,
                    StrUtil.blankToDefault(ex.getMessage(), FAILURE_SOURCE_READ_FAILED));
            recordDownloadAccess(file, userId, accessEvent, false, FAILURE_SOURCE_READ_FAILED);
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }
    }

    private void recordSuccessfulDownloadAudit(DccControlledFileDownloadRecordDO downloadRecord,
                                               String plainSha256,
                                               LocalDateTime returnedAt,
                                               DccControlledFileDO file,
                                               Long userId,
                                               DccControlledFileAccessEventDO accessEvent) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            markDownloadRecordReady(downloadRecord, plainSha256, returnedAt);
            recordDownloadAccess(file, userId, accessEvent, true, "OK");
        });
    }

    private String requireDownloadRequestId(String downloadRequestId) {
        if (StrUtil.isBlank(downloadRequestId)) {
            throw exception(DCC_DOWNLOAD_REQUEST_ID_REQUIRED);
        }
        return StrUtil.trim(downloadRequestId);
    }

    private void requireDownloadRequestUnused(String downloadRequestId) {
        DccControlledFileDownloadRecordDO existingRecord = downloadRecordMapper.selectOne(
                new LambdaQueryWrapperX<DccControlledFileDownloadRecordDO>()
                        .eq(DccControlledFileDownloadRecordDO::getDownloadRequestId, downloadRequestId));
        if (existingRecord != null) {
            throw exception(DCC_DOWNLOAD_REQUEST_ID_REUSED);
        }
    }

    private DccControlledFileAccessEventDO createDownloadAccessEvent(DccControlledFileDO file, Long userId,
                                                                     String downloadRequestId,
                                                                     LocalDateTime occurredAt,
                                                                     DccRequestAuditContext auditContext) {
        return createDownloadAccessEvent(file,userId,downloadRequestId,occurredAt,auditContext,CONTROLLED_DOWNLOAD_PURPOSE);
    }

    private DccControlledFileAccessEventDO createDownloadAccessEvent(DccControlledFileDO file, Long userId,
            String downloadRequestId, LocalDateTime occurredAt, DccRequestAuditContext auditContext, String purpose) {
        DccControlledFileAccessEventDO accessEvent = DccControlledFileAccessEventDO.builder()
                .accessEventCode(newAccessEventCode())
                .controlledFileId(file.getId())
                .fileVersionNo(StrUtil.trim(file.getVersionNo()))
                .userId(userId)
                .accessType(DOWNLOAD_ACCESS_TYPE)
                .purpose(purpose)
                .result(ACCESS_RESULT_SUCCESS)
                .sourceIp(auditContext.sourceIp())
                .userAgent(auditContext.userAgent())
                .requestId(downloadRequestId)
                .occurredAt(occurredAt)
                .build();
        int insertedRows = accessEventMapper.insert(accessEvent);
        if (insertedRows <= 0 || accessEvent.getId() == null) {
            throw exception(DCC_DOWNLOAD_AUDIT_RECORD_FAILED);
        }
        return accessEvent;
    }

    private DccControlledFileDownloadRecordDO createDownloadRecord(DccControlledFileDO file, Long userId,
                                                                   String downloadRequestId,
                                                                   DccControlledFileAccessEventDO accessEvent,
                                                                   String policyVersion,
                                                                   LocalDateTime requestedAt) {
        DccControlledFileDownloadRecordDO downloadRecord = DccControlledFileDownloadRecordDO.builder()
                .downloadRequestId(downloadRequestId)
                .accessEventId(accessEvent.getId())
                .accessEventCode(accessEvent.getAccessEventCode())
                .controlledFileId(file.getId())
                .fileVersionNo(StrUtil.trim(file.getVersionNo()))
                .userId(userId)
                .policyVersion(policyVersion)
                .downloadStatus(DOWNLOAD_RECORD_STATUS_REQUESTED)
                .requestedAt(requestedAt)
                .build();
        int insertedRows;
        try {
            insertedRows = downloadRecordMapper.insert(downloadRecord);
        } catch (DuplicateKeyException ex) {
            throw exception(DCC_DOWNLOAD_REQUEST_ID_REUSED);
        }
        if (insertedRows <= 0 || downloadRecord.getId() == null) {
            throw exception(DCC_DOWNLOAD_AUDIT_RECORD_FAILED);
        }
        return downloadRecord;
    }

    private void markDownloadRecordReady(DccControlledFileDownloadRecordDO downloadRecord,
                                         String plainSha256,
                                         LocalDateTime returnedAt) {
        int updatedRows = downloadRecordMapper.updateById(DccControlledFileDownloadRecordDO.builder()
                .id(downloadRecord.getId())
                .downloadStatus(DOWNLOAD_RECORD_STATUS_READY)
                .plainSha256(plainSha256)
                .returnedAt(returnedAt)
                .build());
        if (updatedRows <= 0) {
            throw exception(DCC_DOWNLOAD_AUDIT_RECORD_FAILED);
        }
    }

    private void markDownloadRecordFailed(DccControlledFileDownloadRecordDO downloadRecord,
                                          String failureCode,
                                          String failureReason) {
        int updatedRows = downloadRecordMapper.update(null, new UpdateWrapper<DccControlledFileDownloadRecordDO>()
                .eq("id", downloadRecord.getId())
                .set("download_status", DOWNLOAD_RECORD_STATUS_FAILED)
                .set("plain_sha256", null)
                .set("failure_code", failureCode)
                .set("failure_reason", StrUtil.trim(failureReason))
                .set("returned_at", null));
        if (updatedRows <= 0) {
            throw exception(DCC_DOWNLOAD_AUDIT_RECORD_FAILED);
        }
    }

    private String downloadFailureCode(ServiceException ex) {
        if (Objects.equals(ex.getCode(), DCC_DOWNLOAD_AUDIT_RECORD_FAILED.getCode())) {
            return FAILURE_AUDIT_RECORD_FAILED;
        }
        return FAILURE_SOURCE_READ_FAILED;
    }

    private String sha256Hex(byte[] bytes) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is unavailable", ex);
        }
    }

    private String newAccessEventCode() {
        return "AE-" + DOWNLOAD_EVENT_CODE_DATE_FORMATTER.format(LocalDateTime.now(ZoneOffset.UTC)) + "-"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
    }

    @Override
    public boolean canViewFileName(Long userId, DccControlledFileDO file) {
        return userId != null && file != null && isWithinAssignedFileScope(userId, file)
                && canAccessQuery(userId, file, new DccControlledFilePageReqVO(),
                directoryAccessPermissionService.hasDirectoryManagementPermission(userId));
    }

    private boolean canAccessQuery(Long userId, DccControlledFileDO file, DccControlledFilePageReqVO reqVO,
                                   boolean hasDirectoryManagementPermission) {
        return canAccessQuery(userId, file, reqVO, hasDirectoryManagementPermission, null);
    }

    private boolean canAccessQuery(Long userId, DccControlledFileDO file, DccControlledFilePageReqVO reqVO,
                                   boolean hasDirectoryManagementPermission,
                                   Map<Long, Boolean> currentViewMatrixAccessByCategory) {
        if (DccOfflineTrainingRecordService.isNativeWaitingFile(file) && offlineTraining.canUpload(userId, file)) {
            return true;
        }
        if (reqVO.getRequesterId() != null && reqVO.getRequesterId().equals(userId) && userId.equals(file.getRequesterId())) {
            return true;
        }
        if (hasDirectoryManagementPermission) {
            return !DccControlledFileStatusEnum.OBSOLETE.getStatus().equals(file.getStatus()) || canSeeObsolete(userId, file);
        }
        if (canManageNonActiveLifecycle(userId, file)) {
            return true;
        }
        if (!canAccessBrowseScope(userId, file, currentViewMatrixAccessByCategory)
                && !(isWithinAssignedFileScope(userId, file)
                && (hasAuthorizedDirectoryAccess(userId, file, DccAccessTypeEnum.QUERY)
                    || hasAuthorizedDirectoryAccess(userId, file, DccAccessTypeEnum.PREVIEW)))) {
            return false;
        }
        return !DccControlledFileStatusEnum.OBSOLETE.getStatus().equals(file.getStatus()) || canSeeObsolete(userId, file);
    }

    private boolean canAccessDetail(Long userId, DccControlledFileDO file) {
        return canAccessDetail(userId, file,
                directoryAccessPermissionService.hasDirectoryManagementPermission(userId), null);
    }

    private boolean canAccessDetail(Long userId, DccControlledFileDO file,
                                    boolean directoryManager, Map<Long, Boolean> viewMatrixCache) {
        return detailAuthorizationGuard.isAllowed(userId, file, directoryManager, viewMatrixCache);
    }

    private boolean canReadBinary(Long userId, DccControlledFileDO file, DccAccessTypeEnum accessType) {
        return canReadBinary(userId, file, accessType,
                directoryAccessPermissionService.hasDirectoryManagementPermission(userId));
    }

    private boolean canReadBinary(Long userId, DccControlledFileDO file, DccAccessTypeEnum accessType,
                                  boolean hasDirectoryManagementPermission) {
        return canReadBinary(userId, file, accessType, hasDirectoryManagementPermission, null);
    }

    private boolean canReadBinary(Long userId, DccControlledFileDO file, DccAccessTypeEnum accessType,
                                  boolean hasDirectoryManagementPermission,
                                  Map<Long, Boolean> currentViewMatrixAccessByCategory) {
        var context = bodyReadContext.get();
        if (context != null && accessType == DccAccessTypeEnum.PREVIEW) {
            detailAuthorizationGuard.requireBodyRead(userId, file, context);
            if (context.intent() != DccControlledFileBodyReadContext.Intent.DAILY
                    && context.intent() != DccControlledFileBodyReadContext.Intent.VERSION_HISTORY)
                return resolvePreviewReferenceId(file) != null;
        }
        if (userId == null || file == null || "CONTROLLED_PENDING_EFFECTIVE".equals(file.getStatus())) {
            return false;
        }
        if (DccControlledFileStatusEnum.PENDING_SUPERVISOR_REVIEW.getStatus().equals(file.getStatus())) {
            return accessType == DccAccessTypeEnum.PREVIEW && resolvePreviewReferenceId(file) != null
                    && (hasCurrentRunningApprovalTask(userId, file) || detailAuthorizationGuard.hasCurrentCompilerAccess(userId, file));
        }
        var distributionContext = distributionPreviewContext.get();
        if (distributionContext != null) {
            if (accessType != DccAccessTypeEnum.PREVIEW || !Objects.equals(distributionContext.userId(), userId)
                    || !Objects.equals(distributionContext.controlledFileId(), file.getId())) return false;
            var issued = requireDistributionPreviewFile(distributionContext);
            return Objects.equals(issued.getPublishedFileId(), file.getPublishedFileId())
                    && Objects.equals(issued.getVersionNo(), file.getVersionNo());
        }
        Long previewBinaryFileId = accessType == DccAccessTypeEnum.PREVIEW
                ? resolvePreviewReferenceId(file) : null;
        if (accessType == DccAccessTypeEnum.PREVIEW && DccOfflineTrainingRecordService.isNativeWaitingFile(file)) {
            return previewBinaryFileId != null && offlineTraining.canReadTrainingBody(userId, file);
        }
        if (accessType == DccAccessTypeEnum.PREVIEW
                && (isPendingPreviewStatus(file.getStatus())
                || DccControlledFileStatusEnum.FINALIZATION_FAILED.getStatus().equals(file.getStatus()))
                && previewBinaryFileId != null
                && hasCurrentRunningApprovalTask(userId, file)) {
            return true;
        }
        if (!isWithinAssignedFileScope(userId, file)) {
            return false;
        }
        if (!hasReadableProjectScope(userId, file)) {
            return false;
        }
        if (DccControlledFileStatusEnum.OBSOLETE.getStatus().equals(file.getStatus()) && !canSeeObsolete(userId, file)) {
            return false;
        }
        boolean allowed;
        if (accessType == DccAccessTypeEnum.PREVIEW) {
            if (DccControlledFileVersionPolicy.hasRetainedControlledBodyStatus(file.getStatus())
                    && file.getPublishedFileId() != null) {
                allowed = hasAuthorizedDirectoryAccess(userId, file, DccAccessTypeEnum.PREVIEW)
                        || canAccessCurrentViewMatrix(userId, file, currentViewMatrixAccessByCategory);
                return allowed;
            }
            if (isPendingPreviewStatus(file.getStatus()) && previewBinaryFileId != null) {
                if (userId != null && userId.equals(file.getRequesterId())) {
                    return true;
                }
                allowed = hasCurrentRunningApprovalTask(userId, file);
                return allowed;
            }
            if ((DccControlledFileStatusEnum.WORKING.getStatus().equals(file.getStatus())
                    || DccControlledFileStatusEnum.REJECTED.getStatus().equals(file.getStatus())
                    || DccControlledFileStatusEnum.FINALIZATION_FAILED.getStatus().equals(file.getStatus()))
                    && previewBinaryFileId != null) {
                return userId != null && userId.equals(file.getRequesterId());
            }
            if (DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus().equals(file.getStatus())
                    && previewBinaryFileId != null) {
                return userId != null && userId.equals(file.getRequesterId())
                        || hasCurrentRunningApprovalTask(userId, file);
            }
            return false;
        }
        return canDownloadBinary(userId, file, hasDirectoryManagementPermission);
    }

    private boolean canAccessBrowseScope(Long userId, DccControlledFileDO file) {
        return canAccessBrowseScope(userId, file, null);
    }

    private boolean canAccessBrowseScope(Long userId, DccControlledFileDO file,
                                         Map<Long, Boolean> currentViewMatrixAccessByCategory) {
        if (userId == null || file == null) {
            return false;
        }
        if (userId.equals(file.getRequesterId())) {
            return true;
        }
        if (isPendingPreviewStatus(file.getStatus())) {
            return isCurrentRouteSnapshotParticipant(userId, file)
                    || hasCurrentRunningApprovalTask(userId, file);
        }
        if (!DccControlledFileVersionPolicy.isCurrentControlledStatus(file.getStatus())) {
            return false;
        }
        return canAccessCurrentViewMatrix(userId, file, currentViewMatrixAccessByCategory)
                || hasActiveElectronicDistributionAccess(userId, file);
    }

    private boolean canManageNonActiveLifecycle(Long userId, DccControlledFileDO file) {
        if (userId == null || file == null || file.getCategoryId() == null) {
            return false;
        }
        String status = file.getStatus();
        if (DccControlledFileStatusEnum.PENDING_MANUAL_DISTRIBUTION.getStatus().equals(status)
                || DccControlledFileStatusEnum.TRAINING_IN_PROGRESS.getStatus().equals(status)) {
            return permissionSupport.hasCategoryPermission(file.getCategoryId(), userId,
                    DccFileCategoryPermissionActionEnum.DISTRIBUTE);
        }
        if (DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus().equals(status)
                || DccControlledFileStatusEnum.FINALIZING.getStatus().equals(status)
                || DccControlledFileStatusEnum.FINALIZATION_FAILED.getStatus().equals(status)) {
            return permissionSupport.hasCategoryPermission(file.getCategoryId(), userId,
                    DccFileCategoryPermissionActionEnum.APPROVE);
        }
        if (DccControlledFileStatusEnum.WORKING.getStatus().equals(status)
                || DccControlledFileStatusEnum.REJECTED.getStatus().equals(status)) {
            return permissionSupport.hasCategoryPermission(file.getCategoryId(), userId,
                    DccFileCategoryPermissionActionEnum.UPLOAD);
        }
        if (DccControlledFileStatusEnum.SUPERSEDED.getStatus().equals(status)
                || DccControlledFileStatusEnum.OBSOLETE.getStatus().equals(status)) {
            return permissionSupport.hasCategoryPermission(file.getCategoryId(), userId,
                    DccFileCategoryPermissionActionEnum.OBSOLETE);
        }
        return false;
    }

    private boolean hasHistoryManagementPermission(Long userId, Long categoryId) {
        if (userId == null || categoryId == null) {
            return false;
        }
        return permissionSupport.hasCategoryPermission(categoryId, userId,
                DccFileCategoryPermissionActionEnum.UPLOAD)
                || permissionSupport.hasCategoryPermission(categoryId, userId,
                DccFileCategoryPermissionActionEnum.REVIEW)
                || permissionSupport.hasCategoryPermission(categoryId, userId,
                DccFileCategoryPermissionActionEnum.APPROVE)
                || permissionSupport.hasCategoryPermission(categoryId, userId,
                DccFileCategoryPermissionActionEnum.DISTRIBUTE)
                || permissionSupport.hasCategoryPermission(categoryId, userId,
                DccFileCategoryPermissionActionEnum.OBSOLETE);
    }

    private boolean canAccessCurrentViewMatrix(Long userId, DccControlledFileDO file,
                                               Map<Long, Boolean> currentViewMatrixAccessByCategory) {
        if (userId == null || file == null || file.getCategoryId() == null) {
            return false;
        }
        if (currentViewMatrixAccessByCategory == null) {
            return viewMatrixAccessService.canAccessCurrentViewMatrix(userId, file);
        }
        return currentViewMatrixAccessByCategory.computeIfAbsent(file.getCategoryId(),
                ignored -> viewMatrixAccessService.canAccessCurrentViewMatrix(userId, file));
    }

    private boolean canDownloadBinary(Long userId, DccControlledFileDO file) {
        return decideDownloadBinary(userId, file,
                directoryAccessPermissionService.hasDirectoryManagementPermission(userId)).allowed();
    }

    private boolean canDownloadBinary(Long userId, DccControlledFileDO file,
                                      boolean hasDirectoryManagementPermission) {
        return decideDownloadBinary(userId, file, hasDirectoryManagementPermission).allowed();
    }

    private DccDownloadPolicyDecision decideDownloadBinary(Long userId, DccControlledFileDO file) {
        return decideDownloadBinary(userId, file,
                directoryAccessPermissionService.hasDirectoryManagementPermission(userId));
    }

    private DccDownloadPolicyDecision decideDownloadBinary(Long userId, DccControlledFileDO file,
                                                           boolean hasDirectoryManagementPermission) {
        if (!isWithinAssignedFileScope(userId, file) || !hasReadableProjectScope(userId, file)) {
            return downloadPolicyService.decide(new DccDownloadPolicyContext(
                    file.getId(), file.getStatus(), file.getPublishedFileId(), false, false));
        }
        boolean candidate = file.getPublishedFileId() != null
                && DccControlledFileStatusEnum.ACTIVE.getStatus().equals(file.getStatus());
        boolean categoryDownloadAllowed = candidate
                && permissionSupport.hasCategoryPermission(file.getCategoryId(), userId,
                DccFileCategoryPermissionActionEnum.DOWNLOAD);
        boolean directoryDownloadAllowed = categoryDownloadAllowed
                && hasAuthorizedDirectoryAccess(userId, file, DccAccessTypeEnum.DOWNLOAD);
        return downloadPolicyService.decide(new DccDownloadPolicyContext(
                file.getId(),
                file.getStatus(),
                file.getPublishedFileId(),
                categoryDownloadAllowed,
                directoryDownloadAllowed));
    }

    private boolean hasActiveElectronicDistributionAccess(Long userId, DccControlledFileDO file) {
        return userId != null
                && file != null
                && file.getId() != null
                && DccControlledFileStatusEnum.ACTIVE.getStatus().equals(file.getStatus())
                && distributionRecipientMapper.countActiveElectronicRecipientAccess(
                TenantContextHolder.getRequiredTenantId(), file.getId(), userId) > 0;
    }

    private boolean isWithinAssignedFileScope(Long userId, DccControlledFileDO file) {
        return assignmentScopeService.isWithinAssignedFileScope(userId, file == null ? null : file.getId());
    }

    private boolean hasReadableProjectScope(Long userId, DccControlledFileDO file) {
        return userId != null && file != null && file.getDccProjectCodeId() != null
                && projectAccessService.listReadableProjectIds(userId).contains(file.getDccProjectCodeId());
    }

    private boolean hasDirectoryAccess(Long userId, DccControlledFileDO file, DccAccessTypeEnum accessType) {
        return hasDirectoryAccess(userId, file, accessType,
                directoryAccessPermissionService.hasDirectoryManagementPermission(userId));
    }

    private boolean hasDirectoryAccess(Long userId, DccControlledFileDO file, DccAccessTypeEnum accessType,
                                       boolean hasDirectoryManagementPermission) {
        if (hasDirectoryManagementPermission) {
            return true;
        }
        return hasAuthorizedDirectoryAccess(userId, file, accessType);
    }

    private boolean hasAuthorizedDirectoryAccess(Long userId, DccControlledFileDO file, DccAccessTypeEnum accessType) {
        if (file.getDirectoryId() == null) {
            return false;
        }
        Set<Long> authorizedDirectoryIds = Objects.requireNonNull(
                directoryAccessPermissionService.getAuthorizedDirectoryIds(userId, accessType),
                "authorizedDirectoryIds");
        return authorizedDirectoryIds.contains(file.getDirectoryId());
    }

    private Long resolveBinaryFileId(DccControlledFileDO file, DccAccessTypeEnum accessType) {
        if (accessType == DccAccessTypeEnum.PREVIEW) {
            Long referenceId = resolvePreviewReferenceId(file);
            if (DccControlledFileVersionPolicy.hasRetainedControlledBodyStatus(file.getStatus())
                    || DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus().equals(file.getStatus())) {
                return referenceId;
            }
            return resolveWorkingPreviewFileId(file, referenceId);
        }
        return file.getPublishedFileId();
    }

    private Long resolvePreviewReferenceId(DccControlledFileDO file) {
        if (DccControlledFileStatusEnum.PENDING_DELAYED_EFFECTIVE_CONFIRM.getStatus().equals(file.getStatus())) {
            var context = bodyReadContext.get();
            if (context == null || !Set.of(DccControlledFileBodyReadContext.Intent.AUTHORING,
                    DccControlledFileBodyReadContext.Intent.APPROVAL_TASK,
                    DccControlledFileBodyReadContext.Intent.APPROVAL_HISTORY).contains(context.intent())) return null;
            context.requireFile(file.getId(), file.getVersionNo());
            if (!Objects.equals(context.bpmRound(), file.getProcessInstanceId()))
                throw exception(CONTROLLED_FILE_ACCESS_DENIED);
            if (file.getSourceFileId() == null)
                throw new IllegalStateException("delayed confirmation preview requires the exact saved source body");
            return file.getSourceFileId();
        }
        if (DccOfflineTrainingRecordService.isNativeWaitingFile(file)) {
            if (file.getSourceFileId() == null) {
                throw new IllegalStateException("training preview requires this round's approved source body");
            }
            return file.getSourceFileId();
        }
        String status = file.getStatus();
        if (DccControlledFileVersionPolicy.hasRetainedControlledBodyStatus(status)) {
            return file.getPublishedFileId();
        }
        if (DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus().equals(status)) {
            return file.getStampedFileId();
        }
        if (DccControlledFileStatusEnum.WORKING.getStatus().equals(status)) {
            return file.getSourceFileId();
        }
        if (DccControlledFileStatusEnum.REJECTED.getStatus().equals(status)
                || DccControlledFileStatusEnum.FINALIZATION_FAILED.getStatus().equals(status)
                || isPendingPreviewStatus(status)
                || DccControlledFileStatusEnum.PENDING_APPLICANT_REWORK.getStatus().equals(status)) {
            return file.getSourceFileId() == null ? file.getOriginalFileId() : file.getSourceFileId();
        }
        return null;
    }

    private Long resolveWorkingPreviewFileId(DccControlledFileDO file, Long sourceFileId) {
        FileDO source = sourceFileId == null ? null : fileMapper.selectById(sourceFileId);
        if (source != null && DccControlledFileUploadTypePolicy.isDrawingSourceName(source.getName())) {
            if (file.getDrawingPdfFileId() == null) {
                throw exception(CONTROLLED_FILE_DRAWING_PDF_REQUIRED);
            }
            return file.getDrawingPdfFileId();
        }
        return sourceFileId;
    }

    private FileDO resolveBinaryFileRecord(DccControlledFileDO file, DccAccessTypeEnum accessType) {
        Long binaryFileId = resolveBinaryFileId(file, accessType);
        FileDO binaryFile = binaryFileId != null ? fileMapper.selectById(binaryFileId) : null;
        if (binaryFile == null) {
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }
        return binaryFile;
    }

    private boolean isPendingPreviewStatus(String status) {
        return DccControlledFileStatusEnum.PENDING_SUPERVISOR_REVIEW.getStatus().equals(status)
                || DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus().equals(status)
                || DccControlledFileStatusEnum.PENDING_MATRIX_REVIEW.getStatus().equals(status)
                || DccControlledFileStatusEnum.PENDING_MATRIX_APPROVAL.getStatus().equals(status)
                || DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL.getStatus().equals(status)
                || DccControlledFileStatusEnum.PENDING_APPLICANT_REWORK.getStatus().equals(status);
    }

    private boolean isCurrentRouteSnapshotParticipant(Long userId, DccControlledFileDO file) {
        if (userId == null || file == null || file.getId() == null) {
            return false;
        }
        String stageCode = resolvePendingStageCode(file.getStatus());
        if (StrUtil.isBlank(stageCode)) {
            return false;
        }
        return routeSnapshotMapper.selectListByControlledFileId(file.getId()).stream()
                .filter(snapshot -> StrUtil.equals(snapshot.getStageCode(), stageCode))
                .anyMatch(snapshot -> parseResolvedUserIds(snapshot).contains(userId));
    }

    private boolean hasCurrentRunningApprovalTask(Long userId, DccControlledFileDO file) {
        if (file != null && DccControlledFileStatusEnum.PENDING_SUPERVISOR_REVIEW.getStatus().equals(file.getStatus()))
            return detailAuthorizationGuard.hasCurrentSupervisorTask(userId, file);
        if (userId == null || file == null || StrUtil.isBlank(file.getProcessInstanceId())
                || (!isPendingPreviewStatus(file.getStatus())
                && !DccControlledFileStatusEnum.FINALIZATION_FAILED.getStatus().equals(file.getStatus()))) {
            return false;
        }
        String stageCode = resolvePendingStageCode(file.getStatus());
        if (StrUtil.isBlank(stageCode)) {
            return false;
        }
        List<Task> runningTasks = Objects.requireNonNull(
                bpmTaskService.getRunningTaskListByProcessInstanceId(file.getProcessInstanceId(), null, null),
                "runningTasks");
        return runningTasks.stream()
                .filter(Objects::nonNull)
                .anyMatch(task -> StrUtil.equals(task.getTaskDefinitionKey(), stageCode)
                        && StrUtil.equals(task.getAssignee(), String.valueOf(userId)));
    }

    private String resolvePendingStageCode(String status) {
        if (DccControlledFileStatusEnum.PENDING_SUPERVISOR_REVIEW.getStatus().equals(status))
            return DccControlledFileStageCodeEnum.SUPERVISOR_REVIEW.getCode();
        if (DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus().equals(status)) {
            return DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode();
        }
        if (DccControlledFileStatusEnum.PENDING_MATRIX_REVIEW.getStatus().equals(status)) {
            return DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode();
        }
        if (DccControlledFileStatusEnum.PENDING_MATRIX_APPROVAL.getStatus().equals(status)) {
            return DccControlledFileStageCodeEnum.MATRIX_APPROVAL.getCode();
        }
        if (DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL.getStatus().equals(status)) {
            return DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL.getCode();
        }
        if (DccControlledFileStatusEnum.FINALIZATION_FAILED.getStatus().equals(status)) {
            return DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode();
        }
        if (DccControlledFileStatusEnum.PENDING_APPLICANT_REWORK.getStatus().equals(status)) {
            return DccControlledFileStageCodeEnum.APPLICANT_REWORK.getCode();
        }
        return null;
    }

    private Set<Long> parseResolvedUserIds(DccControlledFileRouteSnapshotDO snapshot) {
        if (snapshot == null || StrUtil.isBlank(snapshot.getResolvedUserIds())) {
            return Set.of();
        }
        return Arrays.stream(snapshot.getResolvedUserIds().split(","))
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .map(Long::valueOf)
                .collect(Collectors.toSet());
    }

    private boolean canSeeObsolete(Long userId, DccControlledFileDO file) {
        return userId != null && userId.equals(file.getRequesterId())
                || permissionSupport.hasCategoryPermission(file.getCategoryId(), userId,
                DccFileCategoryPermissionActionEnum.OBSOLETE);
    }

    private Set<Long> resolveRequestedDirectoryIds(DccControlledFilePageReqVO reqVO) {
        if (reqVO.getDirectoryId() == null) {
            return null;
        }
        if (!Boolean.TRUE.equals(reqVO.getIncludeDescendantDirectories())) {
            return Set.of(reqVO.getDirectoryId());
        }
        Map<Long, List<DccFileDirectoryDO>> childrenByParentId = groupChildrenByParentId(directoryMapper.selectEnabledList());
        if (!childrenByParentId.containsKey(reqVO.getDirectoryId())
                && childrenByParentId.values().stream().flatMap(List::stream).noneMatch(item -> Objects.equals(item.getId(), reqVO.getDirectoryId()))) {
            return Set.of();
        }
        java.util.LinkedHashSet<Long> directoryIds = new java.util.LinkedHashSet<>();
        collectDirectoryIds(reqVO.getDirectoryId(), childrenByParentId, directoryIds);
        return directoryIds;
    }

    private void collectDirectoryIds(Long directoryId,
                                     Map<Long, List<DccFileDirectoryDO>> childrenByParentId,
                                     java.util.LinkedHashSet<Long> container) {
        if (directoryId == null || !container.add(directoryId)) {
            return;
        }
        for (DccFileDirectoryDO child : childrenByParentId.getOrDefault(directoryId, List.of())) {
            collectDirectoryIds(child.getId(), childrenByParentId, container);
        }
    }

    private DccControlledFilePageReqVO buildCandidateReqForWorkflowSearch(DccControlledFilePageReqVO reqVO) {
        if (!shouldFilterKeywordAfterLatestAggregation(reqVO)) {
            return reqVO;
        }
        DccControlledFilePageReqVO candidateReqVO = copyPageReq(reqVO);
        candidateReqVO.setKeyword(null);
        return candidateReqVO;
    }

    private boolean shouldFilterKeywordAfterLatestAggregation(DccControlledFilePageReqVO reqVO) {
        return Boolean.TRUE.equals(reqVO.getLatestVersionOnly()) && normalizeKeyword(reqVO.getKeyword()) != null;
    }

    private DccControlledFilePageReqVO buildPageReqWithoutDirectory(DccControlledFilePageReqVO reqVO) {
        DccControlledFilePageReqVO sanitizedReqVO = copyPageReq(reqVO);
        sanitizedReqVO.setDirectoryId(null);
        return sanitizedReqVO;
    }

    private DccControlledFilePageReqVO copyPageReq(DccControlledFilePageReqVO reqVO) {
        DccControlledFilePageReqVO copiedReqVO = new DccControlledFilePageReqVO();
        copiedReqVO.setPageNo(reqVO.getPageNo());
        copiedReqVO.setPageSize(reqVO.getPageSize());
        copiedReqVO.setCategoryId(reqVO.getCategoryId());
        copiedReqVO.setDirectoryId(reqVO.getDirectoryId());
        copiedReqVO.setRequesterId(reqVO.getRequesterId());
        copiedReqVO.setStatus(reqVO.getStatus());
        copiedReqVO.setProcessType(reqVO.getProcessType());
        copiedReqVO.setKeyword(reqVO.getKeyword());
        copiedReqVO.setIncludeDescendantDirectories(reqVO.getIncludeDescendantDirectories());
        copiedReqVO.setLatestVersionOnly(reqVO.getLatestVersionOnly());
        copiedReqVO.setDccProjectCodeId(reqVO.getDccProjectCodeId());
        copiedReqVO.setFileTypeTaxonomyId(reqVO.getFileTypeTaxonomyId());
        copiedReqVO.setFileTypeTaxonomyIds(reqVO.getFileTypeTaxonomyIds());
        copiedReqVO.setRecognitionStatus(reqVO.getRecognitionStatus());
        copiedReqVO.setBatchRecognitionTaskId(reqVO.getBatchRecognitionTaskId());
        copiedReqVO.setQuickFilter(reqVO.getQuickFilter());
        return copiedReqVO;
    }

    private List<DccControlledFileDO> filterByKeyword(List<DccControlledFileDO> files, String keyword) {
        String normalizedKeyword = normalizeKeyword(keyword);
        if (normalizedKeyword == null) {
            return files;
        }
        String lowerKeyword = normalizedKeyword.toLowerCase(Locale.ROOT);
        return files.stream()
                .filter(file -> containsLowerKeyword(file.getTitle(), lowerKeyword)
                        || containsLowerKeyword(file.getSourceOriginalFileName(), lowerKeyword)
                        || containsLowerKeyword(file.getFileName(), lowerKeyword)
                        || containsLowerKeyword(file.getFileNumber(), lowerKeyword))
                .toList();
    }

    private boolean containsLowerKeyword(String value, String lowerKeyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(lowerKeyword);
    }

    private boolean isBlacklistedBrowserExtension(DccControlledFileDO file, List<String> blacklistedExtensionPatterns) {
        if (file == null || blacklistedExtensionPatterns == null || blacklistedExtensionPatterns.isEmpty()) {
            return false;
        }
        String fileName = file.getSourceOriginalFileName() != null ? file.getSourceOriginalFileName()
                : StrUtil.blankToDefault(file.getFileName(), file.getTitle());
        String extension = resolveFileExtension(fileName);
        return extension != null && blacklistedExtensionPatterns.stream()
                .map(this::normalizeExtensionPatternForMatch)
                .anyMatch(extension::equals);
    }

    private String resolveFileExtension(String fileName) {
        if (StrUtil.isBlank(fileName)) {
            return null;
        }
        String normalizedFileName = fileName.trim().toLowerCase(Locale.ROOT);
        int dotIndex = normalizedFileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == normalizedFileName.length() - 1) {
            return null;
        }
        return normalizedFileName.substring(dotIndex);
    }

    private String normalizeExtensionPatternForMatch(String pattern) {
        if (pattern == null) {
            return "";
        }
        String normalized = pattern.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("*")) {
            normalized = normalized.substring(1);
        }
        return normalized.startsWith(".") ? normalized : "." + normalized;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String normalized = keyword.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private List<DccControlledFileDO> aggregateLatestVisibleFiles(List<DccControlledFileDO> visibleFiles) {
        if (visibleFiles.isEmpty()) {
            return List.of();
        }
        Map<String, DccControlledFileDO> latestByGroup = new LinkedHashMap<>();
        for (DccControlledFileDO file : visibleFiles) {
            String groupKey = resolveVersionGroupKey(file);
            DccControlledFileDO current = latestByGroup.get(groupKey);
            if (current == null || compareVersionPriority(file, current) > 0) {
                latestByGroup.put(groupKey, file);
            }
        }
        Set<Long> latestIds = latestByGroup.values().stream()
                .map(DccControlledFileDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<String> appendedGroups = new HashSet<>();
        return visibleFiles.stream()
                .filter(file -> file.getId() != null && latestIds.contains(file.getId()))
                .filter(file -> appendedGroups.add(resolveVersionGroupKey(file)))
                .toList();
    }

    private List<DccControlledFileDO> aggregateLatestControlledVisibleFiles(List<DccControlledFileDO> visibleFiles) {
        Map<String, List<DccControlledFileDO>> groups = new LinkedHashMap<>();
        visibleFiles.forEach(file -> groups.computeIfAbsent(resolveVersionGroupKey(file), ignored -> new ArrayList<>()).add(file));
        List<DccControlledFileDO> result = new ArrayList<>();
        for (var group : groups.values()) {
            var first = group.get(0);
            var master = first.getMasterId() == null ? null : controlledFileMasterMapper.selectById(first.getMasterId());
            if (master == null || !Objects.equals(master.getTenantId(), TenantContextHolder.getRequiredTenantId())) {
                throw new IllegalStateException("daily browser row lacks its authoritative tenant master");
            }
            group.stream().filter(file -> Objects.equals(master.getCurrentActiveControlledFileId(), file.getId())
                    && Objects.equals(file.getMasterId(), master.getId())
                    && Objects.equals(file.getTenantId(), master.getTenantId())
                    && file.getControlledTime() != null && file.getActivatedTime() != null
                    && DccControlledFileStatusEnum.ACTIVE.getStatus().equals(file.getStatus()))
                    .findFirst().ifPresent(result::add);
        }
        return result;
    }

    private String resolveVersionGroupKey(DccControlledFileDO file) {
        return file.getMasterId() != null ? "MASTER:" + file.getMasterId() : "FILE:" + file.getId();
    }

    private int compareVersionPriority(DccControlledFileDO left, DccControlledFileDO right) {
        Comparator<DccControlledFileDO> comparator = Comparator
                .comparing((DccControlledFileDO file) -> parseSortableVersion(file.getVersionNo()),
                        Comparator.nullsLast(DccControlledFileVersion::compareTo))
                .thenComparing(DccControlledFileDO::getId, Comparator.nullsLast(Long::compareTo));
        return comparator.compare(left, right);
    }

    private DccControlledFileVersion parseSortableVersion(String versionNo) {
        DccControlledFileVersion parsed = DccControlledFileVersion.parse(versionNo);
        if (parsed != null) {
            return parsed;
        }
        return null;
    }

    private DccControlledFileBinary readAttachmentPreviewBinary(Long userId, Long id, Long attachmentId,
                                                                String viewerToken, String accessEventCode,
                                                                String watermarkTraceCode, String viewerTokenId,
                                                                String viewerTokenNonce,
                                                                DccRequestAuditContext auditContext) {
        requireNoDistributionPreviewContext();
        requirePreviewContext(viewerToken, accessEventCode, watermarkTraceCode, viewerTokenId, viewerTokenNonce);
        DccControlledFileDO file = controlledFileMapper.selectById(id);
        if (file == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        if (DccControlledFileStatusEnum.PENDING_SUPERVISOR_REVIEW.getStatus().equals(file.getStatus()))
            if (!detailAuthorizationGuard.hasAuthenticatedCompilerAccess(userId, file)) throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        if (!canReadBinary(userId, file, DccAccessTypeEnum.PREVIEW)) {
            recordAccess(file.getId(), userId, DccAccessTypeEnum.PREVIEW, false, "ATTACHMENT_ACCESS_DENIED",
                    auditContext.withRequestId(auditContext.requestIdOr(accessEventCode)));
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }
        DccControlledFileAttachmentBinary attachment = attachmentService.getAttachment(id, attachmentId);
        requireAttachmentBusinessFileAccess(userId, id, attachment.storageFileId(),
                auditContext.withRequestId(auditContext.requestIdOr(accessEventCode)));
        DccControlledFileAccessEventDO accessEvent = selectAccessEvent(accessEventCode);
        DccControlledFileWatermarkTraceDO watermarkTrace = selectWatermarkTrace(watermarkTraceCode);
        requireMatchingPreviewEvidence(userId, file, accessEvent, watermarkTrace);
        viewerTokenService.verify(viewerToken, new DccViewerTokenExpectedContext(
                TenantContextHolder.getRequiredTenantId(),
                userId,
                file.getId(),
                file.getVersionNo(),
                accessEvent.getId(),
                CONTROLLED_PREVIEW_PURPOSE,
                PREVIEW_VIEWER_TOKEN_TTL_SECONDS,
                viewerTokenNonce,
                viewerTokenId));
        FileDO binaryFile = fileMapper.selectById(attachment.storageFileId());
        if (binaryFile == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        try {
            byte[] content = fileService.getFileContent(binaryFile.getConfigId(), binaryFile.getPath());
            recordPreviewAccess(file, userId, accessEvent, watermarkTrace, true,
                    "ATTACHMENT_OK", auditContext.withRequestId(auditContext.requestIdOr(accessEventCode)));
            String fileName = StrUtil.blankToDefault(attachment.fileName(), binaryFile.getName());
            return new DccControlledFileBinary(fileName,
                    StrUtil.blankToDefault(attachment.contentType(), binaryFile.getType()), content,
                    watermarkService.build(userId, "preview", fileName));
        } catch (Exception ex) {
            recordPreviewAccess(file, userId, accessEvent, watermarkTrace, false,
                    StrUtil.blankToDefault(ex.getMessage(), "ATTACHMENT_READ_FAILED"),
                    auditContext.withRequestId(auditContext.requestIdOr(accessEventCode)));
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }
    }

    private String buildDirectoryPath(Long directoryId, Map<Long, DccFileDirectoryDO> directoryMap) {
        java.util.LinkedList<String> segments = new java.util.LinkedList<>();
        DccFileDirectoryDO current = directoryMap.get(directoryId);
        while (current != null) {
            segments.addFirst(current.getName());
            current = current.getParentId() == null ? null : directoryMap.get(current.getParentId());
        }
        return String.join("/", segments);
    }

    private Map<Long, DccFileDirectoryDO> buildEnabledDirectoryMap() {
        return directoryMapper.selectEnabledList().stream()
                .collect(Collectors.toMap(DccFileDirectoryDO::getId, Function.identity(), (left, right) -> left,
                        LinkedHashMap::new));
    }

    private String resolveDirectoryPath(Long directoryId, Map<Long, DccFileDirectoryDO> directoryMap) {
        if (directoryId == null || directoryMap == null || directoryMap.isEmpty()) {
            return null;
        }
        String directoryPath = buildDirectoryPath(directoryId, directoryMap);
        return StrUtil.isBlank(directoryPath) ? null : directoryPath;
    }

    private List<DccControlledFileUploadDirectoryTreeRespVO.DirectoryNode> buildUploadDirectoryChildren(
            Long parentId, Map<Long, List<DccFileDirectoryDO>> childrenByParentId) {
        return childrenByParentId.getOrDefault(parentId, List.of()).stream()
                .map(child -> {
                    List<DccControlledFileUploadDirectoryTreeRespVO.DirectoryNode> children =
                            buildUploadDirectoryChildren(child.getId(), childrenByParentId);
                    return DccControlledFileUploadDirectoryTreeRespVO.DirectoryNode.builder()
                            .id(child.getId())
                            .name(child.getName())
                            .leaf(children.isEmpty())
                            .children(children)
                            .build();
                })
                .toList();
    }

    private Map<Long, List<DccFileDirectoryDO>> groupChildrenByParentId(List<DccFileDirectoryDO> directories) {
        Map<Long, List<DccFileDirectoryDO>> childrenByParentId = new LinkedHashMap<>();
        for (DccFileDirectoryDO directory : directories) {
            childrenByParentId.computeIfAbsent(directory.getParentId(), key -> new java.util.ArrayList<>())
                    .add(directory);
        }
        return childrenByParentId;
    }

    private DccControlledFileRespVO toRespVO(Long userId, DccControlledFileDO file, boolean includeRouteSnapshots) {
        return toRespVO(userId, file, includeRouteSnapshots,
                directoryAccessPermissionService.hasDirectoryManagementPermission(userId));
    }

    private DccControlledFileRespVO toRespVO(Long userId, DccControlledFileDO file, boolean includeRouteSnapshots,
                                             boolean hasDirectoryManagementPermission) {
        return toRespVO(userId, file, includeRouteSnapshots, hasDirectoryManagementPermission,
                buildEnabledDirectoryMap());
    }

    private DccControlledFileRespVO toRespVO(Long userId, DccControlledFileDO file, boolean includeRouteSnapshots,
                                             boolean hasDirectoryManagementPermission,
                                             Map<Long, DccFileDirectoryDO> directoryMap) {
        if (!canAccessDetail(userId, file, hasDirectoryManagementPermission, null)) {
            return toNameOnlyResponseWithHistory(userId, file, hasDirectoryManagementPermission, List.of(), null);
        }
        DccControlledFileRespVO respVO = new DccControlledFileRespVO();
        respVO.setCanReadTrainingApplicationProgress(offlineTraining.canReadProgress(userId,file));
        fillMasterVersionPointers(respVO, file);
        respVO.setId(file.getId());
        respVO.setMasterId(file.getMasterId());
        respVO.setCategoryId(file.getCategoryId());
        respVO.setDirectoryId(file.getDirectoryId());
        respVO.setDirectoryPath(resolveDirectoryPath(file.getDirectoryId(), directoryMap));
        respVO.setTitle(file.getTitle());
        respVO.setFileName(file.getFileName());
        PreviewArtifactProjection previewProjection = resolvePreviewArtifactProjection(file);
        FileDO previewFile = previewProjection.file();
        respVO.setContentType(previewFile == null ? null : previewFile.getType());
        respVO.setPreviewKind(previewFile == null ? null
                : DccControlledFilePreviewKindEnum.resolve(previewFile.getName(), previewFile.getType()).getCode());
        respVO.setPreviewUnavailableReason(previewProjection.unavailableReason());
        respVO.setFileNumber(file.getFileNumber());
        respVO.setPublishedArtifactAvailable(hasFileRecord(file.getPublishedFileId()));
        respVO.setStampedArtifactAvailable(hasFileRecord(file.getStampedFileId()));
        boolean nativeTrainingRecordAvailable = populateNativeWorkflowExecutionProjection(respVO, file);
        respVO.setProductMasterId(file.getProductMasterId());
        respVO.setProductSource(file.getProductSource());respVO.setProductCatalogId(file.getProductCatalogId());
        respVO.setProductRelationId(file.getProductRelationId());respVO.setProductCreateRequestId(file.getProductCreateRequestId());
        respVO.setProductCode(file.getProductCode());
        respVO.setProductName(file.getProductName());
        respVO.setDccProjectCodeId(file.getDccProjectCodeId());
        if (includeRouteSnapshots) {
            populateDetailProjectIdentity(respVO, file);
            populateDetailProjectStorageLocation(respVO, file);
        }
        respVO.setProjectCodeRecognitionType(file.getProjectCodeRecognitionType());
        respVO.setProjectCodeRecognitionText(file.getProjectCodeRecognitionText());
        respVO.setProjectCodeRecognizedBy(file.getProjectCodeRecognizedBy());
        respVO.setProjectCodeRecognizedTime(file.getProjectCodeRecognizedTime());
        respVO.setFileTypeTaxonomyId(resolveResponseFileTypeTaxonomyId(file));
        respVO.setFileTypeLevel1(file.getFileTypeLevel1());
        respVO.setFileTypeLevel2(file.getFileTypeLevel2());
        respVO.setFileTypeLevel3(file.getFileTypeLevel3());
        respVO.setFileTypeLevel4(file.getFileTypeLevel4());
        respVO.setFileTypeLevel5(file.getFileTypeLevel5());
        respVO.setNeedTraining(file.getNeedTraining());
        respVO.setProcessType(file.getProcessType());
        respVO.setChangeType(file.getChangeType());
        respVO.setVersionNo(file.getVersionNo());
        respVO.setRevisionChangeType(file.getRevisionChangeType());
        respVO.setRevisionAttemptNo(file.getRevisionAttemptNo());respVO.setReworkPredecessorControlledFileId(file.getReworkPredecessorControlledFileId());
        respVO.setFileOwnerUserId(file.getFileOwnerUserId());respVO.setFileOwnerUsernameSnapshot(file.getFileOwnerUsernameSnapshot());respVO.setFileOwnerNicknameSnapshot(file.getFileOwnerNicknameSnapshot());
        respVO.setFileOwnerSignatureId(file.getFileOwnerSignatureId());respVO.setFileOwnerApprovalTaskId(file.getFileOwnerApprovalTaskId());respVO.setFileOwnerProcessInstanceId(file.getFileOwnerProcessInstanceId());respVO.setFileOwnerSelectedTime(file.getFileOwnerSelectedTime());
        respVO.setRevisionSourceControlledFileId(file.getRevisionSourceControlledFileId());
        respVO.setRevisionSourceVersionNo(file.getRevisionSourceVersionNo());
        respVO.setSelectedIterationControlledFileId(file.getSelectedIterationControlledFileId());
        respVO.setSelectedIterationVersionNo(file.getSelectedIterationVersionNo());
        respVO.setSourceOriginalFileName(file.getSourceOriginalFileName());

        respVO.setRevisionCode(file.getRevisionCode());
        respVO.setIterationNo(file.getIterationNo());
        respVO.setPredecessorControlledFileId(file.getPredecessorControlledFileId());
        respVO.setRevisionBaseActiveControlledFileId(file.getRevisionBaseActiveControlledFileId());
        respVO.setSourceSha256(file.getSourceSha256());
        respVO.setPreviousSourceSha256(file.getPreviousSourceSha256());
        respVO.setChangeDescription(file.getChangeDescription());
        respVO.setEffectiveDate(file.getEffectiveDate());
        fillApplicationSelectionProjection(respVO, file);
        respVO.setRemark(file.getRemark());
        respVO.setAttachments(attachmentService.listAttachments(file.getId()));
        if ("WORKING".equals(file.getStatus()) && file.getProcessInstanceId() == null && file.getDccProjectCodeId() != null
                && (Objects.equals(userId, file.getRequesterId()) || projectAccessService.hasProjectOwner(userId, file.getDccProjectCodeId()))
                && projectAccessService.hasProjectEditorOrOwner(userId, file.getDccProjectCodeId())
                && permissionSupport.hasCategoryPermission(file.getCategoryId(), userId, DccFileCategoryPermissionActionEnum.UPLOAD)) {
            var manifest = Objects.requireNonNull(attachmentService.readOrdinaryAttachmentManifest(userId, file.getId()),
                    "exact saved ordinary attachment manifest");
            respVO.setOrdinaryAttachmentManifestHash(manifest.payloadHash());
        }
        respVO.setStatus(file.getStatus());
        respVO.setRequesterId(file.getRequesterId());
        respVO.setProcessInstanceId(file.getProcessInstanceId());
        respVO.setProcessDefinitionKey(file.getProcessDefinitionKey());
        respVO.setSubmittedTime(file.getSubmittedTime());
        respVO.setApprovedTime(file.getApprovedTime());
        respVO.setPublishedTime(file.getPublishedTime());
        respVO.setControlledTime(file.getControlledTime());
        respVO.setActivatedTime(file.getActivatedTime());
        respVO.setDistributedTime(file.getDistributedTime());
        respVO.setRejectedTime(file.getRejectedTime());
        respVO.setStampedTime(file.getStampedTime());
        respVO.setObsoletedBy(file.getObsoletedBy());
        respVO.setObsoletedTime(file.getObsoletedTime());
        respVO.setObsoleteReason(file.getObsoleteReason());
        respVO.setSupersededByFileId(file.getSupersededByFileId());
        respVO.setRejectReason(file.getRejectReason());
        respVO.setFinalizationError(file.getFinalizationError());
        fillCheckoutProjection(respVO, file);
        respVO.setCanPreview(previewFile != null && canReadBinary(userId, file, DccAccessTypeEnum.PREVIEW,
                hasDirectoryManagementPermission));
        DccDownloadPolicyDecision downloadDecision = decideDownloadBinary(userId, file, hasDirectoryManagementPermission);
        respVO.setCanDownload(downloadDecision.allowed());
        respVO.setCanPrint(canPrintControlledFile(userId, file));
        respVO.setAccessExplanation(buildAccessExplanation(userId, file, hasDirectoryManagementPermission,
                respVO.getCanPreview(), downloadDecision));
        respVO.setSystemRecordDownloadOpen(Boolean.FALSE);
        List<DccControlledFileDO> chainFiles = file.getMasterId() == null
                ? List.of()
                : controlledFileMapper.selectListByMasterId(file.getMasterId());
        respVO.setCurrentActiveVersionNo(resolveCurrentActiveVersionNo(file, chainFiles));
        respVO.setModifying(isActiveFileBeingModified(file, chainFiles));
        respVO.setCanObsolete(isObsoleteControlledStage(file)
                && permissionSupport.hasCategoryPermission(file.getCategoryId(), userId,
                DccFileCategoryPermissionActionEnum.OBSOLETE));
        respVO.setCanPublish(DccControlledFileProcessTypeEnum.EXTERNAL_REVIEW.getCode().equals(file.getProcessType())
                && DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus().equals(file.getStatus())
                && permissionApi.hasAnyPermissions(userId, APPROVE_PERMISSION));
        respVO.setVersionHistory(buildVersionHistory(userId, file, chainFiles, hasDirectoryManagementPermission));
        respVO.setDistributionStatuses(buildDistributionStatuses(file.getId()));
        List<DccControlledFileTrainingStatusRespVO> trainingStatuses = buildTrainingStatuses(file.getId());
        respVO.setTrainingStatuses(trainingStatuses);
        respVO.setCanManualRelease(canDistributeControlledFile(userId,file) ||
                (isThreeWorkflowUploadOrRevision(file)
                        || !DccControlledFileProcessTypeEnum.CONTROLLED_FILE.getCode().equals(file.getProcessType()))
                        && DccControlledFileStatusEnum.PENDING_MANUAL_DISTRIBUTION.getStatus().equals(file.getStatus())
                        && permissionSupport.hasCategoryPermission(file.getCategoryId(), userId,
                        DccFileCategoryPermissionActionEnum.DISTRIBUTE)
                        && isTrainingSatisfiedForManualDistribution(file, trainingStatuses,
                        nativeTrainingRecordAvailable));
        respVO.setSignatureSummaries(buildSignatureSummaries(file));
        respVO.setExternalReview(toExternalReviewRespVO(externalReviewMapper.selectByControlledFileId(file.getId())));
        respVO.setHasPendingTrainingAcknowledgement(respVO.getTrainingStatuses().stream()
                .flatMap(status -> status.getAssignments().stream())
                .anyMatch(assignment -> userId != null
                        && userId.equals(assignment.getUserId())
                        && !DccControlledFileTrainingStatusEnum.ACKNOWLEDGED.getCode().equals(assignment.getStatus())
                        && Boolean.TRUE.equals(assignment.getEligibleToAcknowledge())));
        respVO.setActionProjection(buildActionProjection(userId, file, respVO.getCanPreview(), respVO.getCanDownload(),
                respVO.getCanPrint(), respVO.getCanObsolete(), respVO.getCanPublish(), respVO.getCanManualRelease(),
                respVO.getHasPendingTrainingAcknowledgement()));
        List<DccControlledFileRouteSnapshotDO> routeSnapshotDOs = includeRouteSnapshots
                ? routeSnapshotMapper.selectListByControlledFileId(file.getId())
                : List.of();
        List<DccControlledFileTaskAssigneeSnapshotDO> taskAssigneeSnapshotDOs = routeSnapshotDOs.isEmpty()
                ? List.of()
                : taskAssigneeSnapshotMapper.selectListByControlledFileId(file.getId());
        respVO.setRouteSnapshots(routeSnapshotDOs.stream()
                .map(snapshot -> toSnapshotRespVO(snapshot, taskAssigneeSnapshotDOs))
                .toList());
        return respVO;
    }

    /** Current project facts come only from the authorized file's formal identity; frozen product facts stay separate. */
    private void populateDetailProjectIdentity(DccControlledFileRespVO response, DccControlledFileDO file) {
        if (file.getDccProjectCodeId() == null) return; // Explicit unbound history remains readable.
        Long tenant = TenantContextHolder.getRequiredTenantId();
        var project = projectCodeMapper.selectById(file.getDccProjectCodeId());
        if (!Objects.equals(file.getTenantId(), tenant) || project == null
                || !Objects.equals(project.getId(), file.getDccProjectCodeId())
                || !Objects.equals(project.getTenantId(), tenant) || Boolean.TRUE.equals(project.getDeleted())) {
            throw exception(PROJECT_CODE_NOT_EXISTS);
        }
        response.setProjectName(project.getProjectName());
        response.setProjectCode(project.getProjectCode());
    }

    /** Resolve only this file's registered location; never borrow another version's or a reference folder. */
    private void populateDetailProjectStorageLocation(DccControlledFileRespVO response, DccControlledFileDO file) {
        Long tenant = TenantContextHolder.getRequiredTenantId();
        var placement = projectFilePlacementMapper.findFile(file.getId());
        var derived = file.getDirectoryId() == null ? null : projectStorageMappingMapper.findLeaf(tenant, file.getDirectoryId());
        if (placement == null) {
            if (derived != null) throw new IllegalStateException("DCC_FILE_PROJECT_LOCATION_MISSING");
            return;
        }
        var folder = placement.getProjectFolderId() == null ? null : projectFolderMapper.selectById(placement.getProjectFolderId());
        if (!Objects.equals(file.getTenantId(), tenant) || !Objects.equals(placement.getTenantId(), tenant)
                || Boolean.TRUE.equals(placement.getDeleted())
                || !Objects.equals(placement.getControlledFileId(), file.getId())
                || file.getDccProjectCodeId() == null || !Objects.equals(placement.getProjectCodeId(), file.getDccProjectCodeId())
                || file.getDirectoryId() == null || !Objects.equals(placement.getStorageDirectoryId(), file.getDirectoryId())
                || folder == null || !Objects.equals(folder.getId(), placement.getProjectFolderId())
                || !Objects.equals(folder.getTenantId(), tenant) || !Objects.equals(folder.getProjectCodeId(), file.getDccProjectCodeId())
                || Boolean.TRUE.equals(folder.getDeleted()) || !Boolean.TRUE.equals(folder.getActive()) || StrUtil.isBlank(folder.getName())) {
            throw new IllegalStateException("DCC_FILE_PROJECT_LOCATION_INVALID");
        }
        if (derived != null && (!Objects.equals(derived.getTenantId(), tenant) || Boolean.TRUE.equals(derived.getDeleted())
                || !Objects.equals(derived.getProjectCodeId(), file.getDccProjectCodeId())
                || !Objects.equals(derived.getProjectFolderId(), placement.getProjectFolderId())
                || !Objects.equals(derived.getStorageDirectoryId(), file.getDirectoryId())
                || !Objects.equals(derived.getCategoryId(), file.getCategoryId()))) {
            throw new IllegalStateException("DCC_FILE_PROJECT_STORAGE_MAPPING_INVALID");
        }
        response.setHasProjectStorageMapping(true);
        response.setProjectFolderId(folder.getId());
        response.setProjectFolderName(folder.getName());
    }

    private DccControlledFileRespVO toBrowserRespVO(Long userId, DccControlledFileDO file) {
        return toBrowserRespVO(userId, file,
                directoryAccessPermissionService.hasDirectoryManagementPermission(userId));
    }

    private DccControlledFileRespVO toBrowserRespVO(Long userId, DccControlledFileDO file,
                                                    boolean hasDirectoryManagementPermission) {
        return toBrowserRespVO(userId, file, hasDirectoryManagementPermission,
                browserSettingsService.getBlacklistedExtensionPatterns());
    }

    private DccControlledFileRespVO toBrowserRespVO(Long userId, DccControlledFileDO file,
                                                    boolean hasDirectoryManagementPermission,
                                                    List<String> blacklistedExtensionPatterns) {
        return toBrowserRespVO(userId, file, hasDirectoryManagementPermission, blacklistedExtensionPatterns, null);
    }

    private DccControlledFileRespVO toBrowserRespVO(Long userId, DccControlledFileDO file,
                                                    boolean hasDirectoryManagementPermission,
                                                    List<String> blacklistedExtensionPatterns,
                                                    Map<Long, Boolean> currentViewMatrixAccessByCategory) {
        return toBrowserRespVO(userId, file, hasDirectoryManagementPermission, blacklistedExtensionPatterns,
                currentViewMatrixAccessByCategory, buildEnabledDirectoryMap());
    }

    private DccControlledFileRespVO toBrowserRespVO(Long userId, DccControlledFileDO file,
                                                    boolean hasDirectoryManagementPermission,
                                                    List<String> blacklistedExtensionPatterns,
                                                    Map<Long, Boolean> currentViewMatrixAccessByCategory,
                                                    Map<Long, DccFileDirectoryDO> directoryMap) {
        if (!canAccessDetail(userId, file, hasDirectoryManagementPermission, currentViewMatrixAccessByCategory)) {
            return toNameOnlyResponseWithHistory(userId, file, hasDirectoryManagementPermission,
                    blacklistedExtensionPatterns, currentViewMatrixAccessByCategory);
        }
        DccControlledFileRespVO respVO = new DccControlledFileRespVO();
        respVO.setId(file.getId());
        respVO.setMasterId(file.getMasterId());
        fillMasterVersionPointers(respVO, file);
        respVO.setCategoryId(file.getCategoryId());
        respVO.setDirectoryId(file.getDirectoryId());
        respVO.setDirectoryPath(resolveDirectoryPath(file.getDirectoryId(), directoryMap));
        respVO.setTitle(file.getTitle());
        respVO.setFileName(file.getFileName());
        respVO.setFileNumber(file.getFileNumber());
        respVO.setPublishedArtifactAvailable(hasFileRecord(file.getPublishedFileId()));
        respVO.setStampedArtifactAvailable(hasFileRecord(file.getStampedFileId()));
        respVO.setProductCode(file.getProductCode());
        respVO.setProductName(file.getProductName());
        respVO.setDccProjectCodeId(file.getDccProjectCodeId());
        respVO.setProjectCodeRecognitionType(file.getProjectCodeRecognitionType());
        respVO.setProjectCodeRecognitionText(file.getProjectCodeRecognitionText());
        respVO.setProjectCodeRecognizedBy(file.getProjectCodeRecognizedBy());
        respVO.setProjectCodeRecognizedTime(file.getProjectCodeRecognizedTime());
        respVO.setFileTypeTaxonomyId(resolveResponseFileTypeTaxonomyId(file));
        respVO.setFileTypeLevel1(file.getFileTypeLevel1());
        respVO.setFileTypeLevel2(file.getFileTypeLevel2());
        respVO.setFileTypeLevel3(file.getFileTypeLevel3());
        respVO.setFileTypeLevel4(file.getFileTypeLevel4());
        respVO.setFileTypeLevel5(file.getFileTypeLevel5());
        respVO.setNeedTraining(file.getNeedTraining());
        respVO.setProcessType(file.getProcessType());
        respVO.setChangeType(file.getChangeType());
        respVO.setVersionNo(file.getVersionNo());
        respVO.setRevisionChangeType(file.getRevisionChangeType());
        respVO.setRevisionAttemptNo(file.getRevisionAttemptNo());respVO.setReworkPredecessorControlledFileId(file.getReworkPredecessorControlledFileId());
        respVO.setFileOwnerUserId(file.getFileOwnerUserId());respVO.setFileOwnerUsernameSnapshot(file.getFileOwnerUsernameSnapshot());respVO.setFileOwnerNicknameSnapshot(file.getFileOwnerNicknameSnapshot());
        respVO.setFileOwnerSignatureId(file.getFileOwnerSignatureId());respVO.setFileOwnerApprovalTaskId(file.getFileOwnerApprovalTaskId());respVO.setFileOwnerProcessInstanceId(file.getFileOwnerProcessInstanceId());respVO.setFileOwnerSelectedTime(file.getFileOwnerSelectedTime());
        respVO.setRevisionSourceControlledFileId(file.getRevisionSourceControlledFileId());
        respVO.setRevisionSourceVersionNo(file.getRevisionSourceVersionNo());
        respVO.setSelectedIterationControlledFileId(file.getSelectedIterationControlledFileId());
        respVO.setSelectedIterationVersionNo(file.getSelectedIterationVersionNo());
        respVO.setSourceOriginalFileName(file.getSourceOriginalFileName());

        respVO.setEffectiveDate(file.getEffectiveDate());
        fillApplicationSelectionProjection(respVO, file);
        respVO.setRevisionBaseActiveControlledFileId(file.getRevisionBaseActiveControlledFileId());
        respVO.setRemark(file.getRemark());
        respVO.setStatus(file.getStatus());
        respVO.setRequesterId(file.getRequesterId());
        respVO.setPublishedTime(file.getPublishedTime());
        respVO.setControlledTime(file.getControlledTime());
        respVO.setActivatedTime(file.getActivatedTime());
        respVO.setDistributedTime(file.getDistributedTime());
        respVO.setObsoletedTime(file.getObsoletedTime());
        respVO.setSupersededByFileId(file.getSupersededByFileId());
        fillCheckoutProjection(respVO, file);
        PreviewArtifactProjection previewProjection = resolvePreviewArtifactProjection(file);
        respVO.setPreviewUnavailableReason(previewProjection.unavailableReason());
        respVO.setCanPreview(previewProjection.file() != null && canReadBinary(userId, file, DccAccessTypeEnum.PREVIEW,
                hasDirectoryManagementPermission, currentViewMatrixAccessByCategory));
        DccDownloadPolicyDecision downloadDecision = decideDownloadBinary(userId, file, hasDirectoryManagementPermission);
        respVO.setCanDownload(downloadDecision.allowed());
        respVO.setCanPrint(canPrintControlledFile(userId, file));
        respVO.setAccessExplanation(buildAccessExplanation(userId, file, hasDirectoryManagementPermission,
                respVO.getCanPreview(), downloadDecision, currentViewMatrixAccessByCategory));
        respVO.setSystemRecordDownloadOpen(Boolean.FALSE);
        List<DccControlledFileDO> chainFiles = file.getMasterId() == null
                ? List.of()
                : controlledFileMapper.selectListByMasterId(file.getMasterId()).stream()
                .filter(history -> !isBlacklistedBrowserExtension(history, blacklistedExtensionPatterns))
                .toList();
        respVO.setCurrentActiveVersionNo(resolveCurrentActiveVersionNo(file, chainFiles));
        respVO.setModifying(isActiveFileBeingModified(file, chainFiles));
        respVO.setCanObsolete(isObsoleteControlledStage(file)
                && permissionSupport.hasCategoryPermission(file.getCategoryId(), userId,
                DccFileCategoryPermissionActionEnum.OBSOLETE));
        respVO.setCanPublish(DccControlledFileProcessTypeEnum.EXTERNAL_REVIEW.getCode().equals(file.getProcessType())
                && DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus().equals(file.getStatus())
                && permissionApi.hasAnyPermissions(userId, APPROVE_PERMISSION));
        respVO.setCanManualRelease(canDistributeControlledFile(userId,file));
        respVO.setVersionHistory(buildVersionHistory(userId, file, chainFiles, hasDirectoryManagementPermission,
                currentViewMatrixAccessByCategory));
        respVO.setActionProjection(buildActionProjection(userId, file, respVO.getCanPreview(), respVO.getCanDownload(),
                respVO.getCanPrint(), respVO.getCanObsolete(), respVO.getCanPublish(), respVO.getCanManualRelease(),
                respVO.getHasPendingTrainingAcknowledgement()));
        return respVO;
    }

    private DccControlledFileRespVO toNameOnlyResponseWithHistory(Long userId, DccControlledFileDO file,
            boolean directoryManager, List<String> blacklistedExtensions, Map<Long, Boolean> viewMatrixCache) {
        DccControlledFileRespVO response = toNameOnlyResponse(file);
        fillCheckoutProjection(response, file);
        response.setCheckedOutReason(null);
        List<DccControlledFileDO> chain = file.getMasterId() == null ? List.of()
                : controlledFileMapper.selectListByMasterId(file.getMasterId()).stream()
                .filter(history -> !isBlacklistedBrowserExtension(history, blacklistedExtensions)).toList();
        response.setVersionHistory(buildVersionHistory(userId, file, chain, directoryManager, viewMatrixCache));
        return response;
    }

    private DccControlledFileRespVO toNameOnlyResponse(DccControlledFileDO file) {
        DccControlledFileRespVO response = new DccControlledFileRespVO();
        fillMasterVersionPointers(response, file);
        response.setId(file.getId());
        response.setMasterId(file.getMasterId());
        response.setCategoryId(file.getCategoryId());
        response.setDirectoryId(file.getDirectoryId());
        response.setDccProjectCodeId(file.getDccProjectCodeId());
        response.setTitle(file.getTitle());
        response.setFileName(file.getFileName());
        response.setFileNumber(file.getFileNumber());
        response.setVersionNo(file.getVersionNo());
        response.setChangeType(file.getChangeType());
        response.setControlledTime(file.getControlledTime());
        response.setStatus(file.getStatus());
        response.setBusinessSourceType("DCC_CONTROLLED_FILE");
        response.setCanPreview(false);
        response.setCanDownload(false);
        response.setCanPrint(false);
        response.setCanPublish(false);
        response.setCanManualRelease(false);
        response.setCanObsolete(false);
        response.setVersionHistory(List.of());
        DccControlledFileActionProjectionRespVO actions = new DccControlledFileActionProjectionRespVO();
        actions.setActionLocked(false);
        actions.setAllowedActions(List.of());
        actions.setCanWithdraw(false);
        response.setActionProjection(actions);
        return response;
    }

    private void fillMasterVersionPointers(DccControlledFileRespVO response, DccControlledFileDO file) {
        if (file.getMasterId() == null) return;
        var master = controlledFileMasterMapper.selectById(file.getMasterId());
        if (master == null || !Objects.equals(master.getId(), file.getMasterId())
                || !Objects.equals(master.getTenantId(), TenantContextHolder.getRequiredTenantId())
                || !Objects.equals(master.getTenantId(), file.getTenantId())) {
            throw new IllegalStateException("version pointer projection lacks its authoritative tenant master");
        }
        response.setLatestControlledFileId(master.getLatestControlledFileId());
        response.setCurrentActiveControlledFileId(master.getCurrentActiveControlledFileId());
    }

    private void fillApplicationSelectionProjection(DccControlledFileRespVO response, DccControlledFileDO file) {
        response.setEffectiveMode(file.getEffectiveMode());
        response.setDelayedEffectiveTime(file.getDelayedEffectiveTime());
        response.setDelayedEffectiveSignatureId(file.getDelayedEffectiveSignatureId());
        response.setDelayedEffectiveProcessInstanceId(file.getDelayedEffectiveProcessInstanceId());
        response.setDelayedEffectiveConfirmedAt(file.getDelayedEffectiveConfirmedAt());
        response.setEffectiveTime(file.getEffectiveTime());
        response.setEffectiveSignatureId(file.getEffectiveSignatureId());
        response.setEffectiveProcessInstanceId(file.getEffectiveProcessInstanceId());
        response.setSelectedDistributionDepartmentIds(readDistributionDepartmentIds(file.getDistributionDepartmentIdsJson()));
    }

    private List<String> readDistributionDepartmentIds(String json) {
        if (json == null) return null;
        var snapshot = cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(json);
        if (snapshot == null || !snapshot.isArray()) {
            throw new IllegalStateException("distribution department snapshot must be a recorded JSON array");
        }
        List<String> ids = new ArrayList<>();
        Set<String> distinct = new HashSet<>();
        for (var item : snapshot) {
            if (!item.isTextual() && !item.isIntegralNumber()) {
                throw new IllegalStateException("distribution department snapshot contains a non-integer identity");
            }
            String id = item.asText();
            if (!id.matches("[1-9][0-9]*") || new java.math.BigInteger(id).compareTo(
                    java.math.BigInteger.valueOf(Long.MAX_VALUE)) > 0 || !distinct.add(id)) {
                throw new IllegalStateException("distribution department snapshot contains an invalid or duplicate identity");
            }
            ids.add(id);
        }
        return List.copyOf(ids);
    }

    private DccControlledFileCheckoutDO readLogicalCheckout(DccControlledFileDO file) {
        if (file.getMasterId() == null) return null;
        var lock = checkoutMapper.selectActiveByMasterIdForRead(TenantContextHolder.getRequiredTenantId(), file.getMasterId());
        if (lock != null && (lock.getActorId() == null || lock.getCreateTime() == null
                || !Objects.equals(lock.getMasterId(), file.getMasterId()))) {
            throw new IllegalStateException("logical checkout record lacks authoritative actor/time/master");
        }
        return lock;
    }

    private void fillCheckoutProjection(DccControlledFileRespVO response, DccControlledFileDO file) {
        var lock = readLogicalCheckout(file);
        response.setCheckedOut(lock != null);
        if (lock == null) return;
        response.setCheckedOutBy(lock.getActorId()); response.setCheckedOutTime(lock.getCreateTime());
        response.setCheckedOutReason(lock.getReason());
        AdminUserRespDTO user = adminUserApi.getUser(lock.getActorId());
        response.setCheckedOutByName(user == null ? null : user.getNickname());
    }

    private void fillCheckoutProjection(DccControlledFileVersionHistoryRespVO response, DccControlledFileCheckoutDO lock) {
        response.setCheckedOut(lock != null);
        if (lock == null) return;
        response.setCheckedOutBy(lock.getActorId()); response.setCheckedOutTime(lock.getCreateTime());
        response.setCheckedOutReason(lock.getReason());
        AdminUserRespDTO user = adminUserApi.getUser(lock.getActorId());
        response.setCheckedOutByName(user == null ? null : user.getNickname());
    }

    private DccControlledFileActionProjectionRespVO buildActionProjection(Long userId, DccControlledFileDO file,
                                                                          Boolean canPreview,
                                                                          Boolean canDownload,
                                                                          Boolean canPrint,
                                                                          Boolean canObsolete,
                                                                          Boolean canPublish,
                                                                          Boolean canManualRelease,
                                                                          Boolean hasPendingTrainingAcknowledgement) {
        requireActionProjectionIdentity(file);
        List<String> allowedActions = new ArrayList<>();
        allowedActions.add(ACTION_VIEW);
        String status = file.getStatus();
        boolean requester = userId != null && userId.equals(file.getRequesterId());
        boolean active = DccControlledFileStatusEnum.ACTIVE.getStatus().equals(status);
        boolean obsoleteControlled = isObsoleteControlledStage(file);
        boolean superseded = DccControlledFileStatusEnum.SUPERSEDED.getStatus().equals(status);
        boolean working = DccControlledFileStatusEnum.WORKING.getStatus().equals(status);
        boolean readyToPublish = DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus().equals(status);
        boolean pendingWithdrawable = isWithdrawableWorkflowStatus(status);
        boolean pendingOpenCandidate = isOpenCandidateActionStatus(status);
        FormActionInstanceDO obsoleteFormPending = obsoleteControlled ? formActionPendingService.findOpenObsoleteAction(file.getId()) : null;
        boolean obsoleteApprovalPending = obsoleteFormPending != null;
        requirePendingRequestIdentity(file, pendingOpenCandidate);

        addBinaryActions(allowedActions, canPreview, canDownload);
        if (active && Boolean.TRUE.equals(canPrint)) {
            allowedActions.add(ACTION_PRINT);
        }
        if (obsoleteControlled && Boolean.TRUE.equals(canObsolete) && !obsoleteApprovalPending) {
            allowedActions.add(ACTION_OBSOLETE);
        }
        if ((active || superseded) && canCreateMajorRevision(userId, file)) {
            allowedActions.add(ACTION_MAJOR_REVISION);
        }
        if (DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus().equals(status)
                && Boolean.TRUE.equals(canPublish)) {
            allowedActions.add(ACTION_PUBLISH);
        }
        if (pendingWithdrawable && requester) {
            allowedActions.add(ACTION_WITHDRAW);
        }
        if (DccControlledFileStatusEnum.PENDING_APPLICANT_TRAINING_RECORD.getStatus().equals(status)
                && (DccOfflineTrainingRecordService.isNativeWaitingFile(file) ? offlineTraining.canUpload(userId, file)
                    : permissionApi.hasAnyRoles(userId,"doc_control") && Boolean.TRUE.equals(file.getNeedTraining())
                    && permissionSupport.hasCategoryPermission(file.getCategoryId(),userId,DccFileCategoryPermissionActionEnum.APPROVE))) {
            allowedActions.add(ACTION_UPLOAD_TRAINING_RECORD);
        } else if (DccControlledFileStatusEnum.WITHDRAWN.getStatus().equals(status)
                && requester && file.getSupersededByFileId() == null
                && !DccControlledFileProcessDefinitionKeys.REVISION.equals(file.getProcessDefinitionKey())) {
            allowedActions.add(ACTION_DELETE_WITHDRAWN_FLOW);
            allowedActions.add(ACTION_RESUBMIT_WITHDRAWN_FLOW);
        } else if ((DccControlledFileStatusEnum.PENDING_MANUAL_DISTRIBUTION.getStatus().equals(status)
                || DccControlledFileVersionPolicy.isCurrentControlledStatus(status)) && Boolean.TRUE.equals(canManualRelease)) {
            allowedActions.add(ACTION_MANUAL_RELEASE);
        } else if (DccControlledFileStatusEnum.TRAINING_IN_PROGRESS.getStatus().equals(status)
                && Boolean.TRUE.equals(hasPendingTrainingAcknowledgement)) {
            allowedActions.add(ACTION_ACKNOWLEDGE_TRAINING);
        } else if (DccControlledFileStatusEnum.FINALIZATION_FAILED.getStatus().equals(status)) {
            allowedActions.add(ACTION_RETRY_FINALIZATION);
        }

        DccControlledFileActionProjectionRespVO projection = new DccControlledFileActionProjectionRespVO();
        projection.setActionLocked(!active || obsoleteApprovalPending);
        projection.setActionLockReason(obsoleteApprovalPending ? "OBSOLETE_APPROVAL_PENDING"
                : active ? null : resolveActionLockReason(status));
        projection.setAllowedActions(allowedActions);
        projection.setCanWithdraw((pendingWithdrawable && requester)
                || (obsoleteApprovalPending && userId != null && userId.equals(obsoleteFormPending.getApplicantUserId())));
        projection.setPendingRequestId(obsoleteApprovalPending ? obsoleteFormPending.getId()
                : (pendingOpenCandidate ? file.getId() : null));
        projection.setPendingVersionNo((pendingOpenCandidate || obsoleteApprovalPending) ? file.getVersionNo() : null);
        return projection;
    }

    private boolean canCreateMajorRevision(Long userId, DccControlledFileDO file) {
        return userId != null
                && file != null
                && file.getId() != null
                && file.getMasterId() != null
                && file.getDccProjectCodeId() != null
                && projectAccessService.hasProjectOwner(userId, file.getDccProjectCodeId());
    }

    /** Only the obsolete action accepts a genuinely controlled version before its execution date. */
    private boolean isObsoleteControlledStage(DccControlledFileDO file) {
        if (file == null) return false;
        if (DccControlledFileStatusEnum.ACTIVE.getStatus().equals(file.getStatus())) return true;
        if (!DccControlledFileStatusEnum.CONTROLLED_PENDING_EFFECTIVE.getStatus().equals(file.getStatus())
                || !Objects.equals(file.getTenantId(), TenantContextHolder.getRequiredTenantId())
                || Boolean.TRUE.equals(file.getDeleted()) || !isThreeWorkflowUploadOrRevision(file)
                || file.getControlledTime() == null || file.getPublishedFileId() == null || file.getPublishedFileId() <= 0
                || file.getStampedFileId() == null || file.getStampedFileId() <= 0
                || StrUtil.isBlank(file.getProcessInstanceId()) || file.getMasterId() == null) return false;
        var version = versionPolicy.parseStored(file);
        if (version == null || version.isWorkingIteration()) return false;
        var master = controlledFileMasterMapper.selectById(file.getMasterId());
        return matchesLockedFormalIdentity(file, master) && !Boolean.TRUE.equals(master.getDeleted());
    }

    private void requireActionProjectionIdentity(DccControlledFileDO file) {
        if (file == null) {
            throw new IllegalStateException("DCC controlled file action projection requires file");
        }
        if (file.getId() == null) {
            throw new IllegalStateException("DCC controlled file action projection requires controlledFileId");
        }
        if (StrUtil.isBlank(file.getStatus())) {
            throw new IllegalStateException("DCC controlled file action projection requires status");
        }
    }

    private void requirePendingRequestIdentity(DccControlledFileDO file, boolean pendingOpenCandidate) {
        if (!pendingOpenCandidate || !requiresWorkflowPendingProcess(file.getStatus())) {
            return;
        }
        if (StrUtil.isBlank(file.getProcessInstanceId())) {
            throw new IllegalStateException("DCC controlled file action projection requires processInstanceId");
        }
    }

    private void addBinaryActions(List<String> allowedActions, Boolean canPreview, Boolean canDownload) {
        if (Boolean.TRUE.equals(canPreview)) {
            allowedActions.add(ACTION_PREVIEW);
        }
        if (Boolean.TRUE.equals(canDownload)) {
            allowedActions.add(ACTION_DOWNLOAD);
        }
    }

    private boolean canDistributeControlledFile(Long userId,DccControlledFileDO file) {
        return userId != null && isThreeWorkflowUploadOrRevision(file)
                && file.getControlledTime() != null && file.getDistributedTime() == null
                && DccControlledFileVersionPolicy.isCurrentControlledStatus(file.getStatus())
                && permissionApi.hasAnyRoles(userId,"doc_control")
                && permissionSupport.hasCategoryPermission(file.getCategoryId(),userId,DccFileCategoryPermissionActionEnum.DISTRIBUTE)
                && (!Boolean.TRUE.equals(file.getNeedTraining()) || file.getTrainingRecordFileId() != null);
    }

    private boolean canPrintControlledFile(Long userId, DccControlledFileDO file) {
        if (file == null || userId == null) {
            return false;
        }
        if (!DccControlledFileStatusEnum.ACTIVE.getStatus().equals(file.getStatus())
                || file.getControlledTime() == null || file.getActivatedTime() == null
                || !isWithinAssignedFileScope(userId, file) || !hasReadableProjectScope(userId, file)) {
            return false;
        }
        if (!permissionSupport.hasCategoryPermission(file.getCategoryId(), userId,
                DccFileCategoryPermissionActionEnum.PRINT)) {
            return false;
        }
        DccControlledFileMasterDO master = file.getMasterId() == null ? null
                : controlledFileMasterMapper.selectById(file.getMasterId());
        return master != null && Objects.equals(master.getId(), file.getMasterId())
                && Objects.equals(master.getTenantId(), TenantContextHolder.getRequiredTenantId())
                && Objects.equals(master.getTenantId(), file.getTenantId())
                && Objects.equals(master.getCurrentActiveControlledFileId(), file.getId());
    }

    private boolean isOpenCandidateActionStatus(String status) {
        return isWithdrawableWorkflowStatus(status)
                || DccControlledFileStatusEnum.PENDING_APPLICANT_REWORK.getStatus().equals(status)
                || DccControlledFileStatusEnum.FINALIZING.getStatus().equals(status)
                || DccControlledFileStatusEnum.TRAINING_IN_PROGRESS.getStatus().equals(status)
                || DccControlledFileStatusEnum.PENDING_MANUAL_DISTRIBUTION.getStatus().equals(status);
    }

    private boolean isWithdrawableWorkflowStatus(String status) {
        return isPendingPreviewStatus(status)
                || DccControlledFileStatusEnum.PENDING_APPLICANT_TRAINING_RECORD.getStatus().equals(status);
    }

    private boolean requiresWorkflowPendingProcess(String status) {
        return isWithdrawableWorkflowStatus(status)
                || DccControlledFileStatusEnum.PENDING_APPLICANT_REWORK.getStatus().equals(status);
    }

    private String resolveActionLockReason(String status) {
        if (isOpenCandidateActionStatus(status)) {
            return "Controlled file candidate is in workflow";
        }
        if (DccControlledFileStatusEnum.WITHDRAWN.getStatus().equals(status)) {
            return "Controlled file workflow has been withdrawn";
        }
        if (DccControlledFileStatusEnum.OBSOLETE.getStatus().equals(status)
                || DccControlledFileStatusEnum.SUPERSEDED.getStatus().equals(status)
                || DccControlledFileStatusEnum.REJECTED.getStatus().equals(status)) {
            return "Controlled file version is terminal";
        }
        if (DccControlledFileStatusEnum.FINALIZATION_FAILED.getStatus().equals(status)) {
            return "Controlled file finalization failed";
        }
        return "Controlled file is not active";
    }

    private DccExternalFileReviewRespVO toExternalReviewRespVO(DccExternalFileReviewDO review) {
        if (review == null) {
            return null;
        }
        DccExternalFileReviewRespVO respVO = new DccExternalFileReviewRespVO();
        respVO.setControlledFileId(review.getControlledFileId());
        respVO.setExternalSource(review.getExternalSource());
        respVO.setExternalOwner(review.getExternalOwner());
        respVO.setReviewReason(review.getReviewReason());
        respVO.setParticipantUserIds(StrUtil.isBlank(review.getParticipantUserIds())
                ? List.of()
                : Arrays.stream(review.getParticipantUserIds().split(","))
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .map(Long::valueOf)
                .toList());
        respVO.setReviewConclusion(review.getReviewConclusion());
        respVO.setConclusionComment(review.getConclusionComment());
        respVO.setClosedTime(review.getClosedTime());
        return respVO;
    }

    private DccControlledFileAccessExplanationRespVO buildAccessExplanation(Long userId, DccControlledFileDO file,
                                                                            boolean hasDirectoryManagementPermission,
                                                                            Boolean canPreview,
                                                                            DccDownloadPolicyDecision downloadDecision) {
        return buildAccessExplanation(userId, file, hasDirectoryManagementPermission, canPreview, downloadDecision,
                null);
    }

    private DccControlledFileAccessExplanationRespVO buildAccessExplanation(Long userId, DccControlledFileDO file,
                                                                            boolean hasDirectoryManagementPermission,
                                                                            Boolean canPreview,
                                                                            DccDownloadPolicyDecision downloadDecision,
                                                                            Map<Long, Boolean> currentViewMatrixAccessByCategory) {
        DccControlledFileAccessExplanationRespVO respVO = new DccControlledFileAccessExplanationRespVO();
        AccessReason detailReason = resolveDetailAccessReason(userId, file, hasDirectoryManagementPermission,
                currentViewMatrixAccessByCategory);
        respVO.setDetailSource(detailReason.source());
        respVO.setDetailReason(detailReason.reason());
        respVO.setDetailDeniedReason(detailReason.allowed() ? null : detailReason.reason());
        if (Boolean.TRUE.equals(canPreview)) {
            if (DccControlledFileVersionPolicy.hasRetainedControlledBodyStatus(file.getStatus())
                    && file.getPublishedFileId() != null) {
                AccessReason previewReason = resolvePublishedPreviewAccessReason(userId, file,
                        hasDirectoryManagementPermission, currentViewMatrixAccessByCategory);
                respVO.setPublishedPreviewSource(previewReason.source());
                respVO.setPublishedPreviewReason(previewReason.reason());
            } else if (isPendingPreviewStatus(file.getStatus()) && file.getOriginalFileId() != null) {
                AccessReason pendingReason = resolvePendingPreviewAccessReason(userId, file,
                        hasDirectoryManagementPermission);
                respVO.setPendingPreviewSource(pendingReason.source());
                respVO.setPendingPreviewReason(pendingReason.reason());
            }
        }
        respVO.setDownloadSource("DOWNLOAD_POLICY");
        if (downloadDecision != null && downloadDecision.allowed()) {
            respVO.setDownloadReason("下载权限由独立下载策略服务放行");
        } else if (downloadDecision != null) {
            respVO.setDownloadDeniedReason(downloadDecision.reason());
        }
        return respVO;
    }

    private AccessReason resolveDetailAccessReason(Long userId, DccControlledFileDO file,
                                                   boolean hasDirectoryManagementPermission) {
        return resolveDetailAccessReason(userId, file, hasDirectoryManagementPermission, null);
    }

    private AccessReason resolveDetailAccessReason(Long userId, DccControlledFileDO file,
                                                   boolean hasDirectoryManagementPermission,
                                                   Map<Long, Boolean> currentViewMatrixAccessByCategory) {
        if (userId != null && userId.equals(file.getRequesterId())) {
            return new AccessReason(true, "REQUESTER_SELF", "申请人自查");
        }
        if (hasDirectoryManagementPermission) {
            return new AccessReason(true, "DIRECTORY_ADMIN", "目录管理员");
        }
        if (hasActiveElectronicDistributionAccess(userId, file)) {
            return new AccessReason(true, "ELECTRONIC_DISTRIBUTION_RECIPIENT", "有效电子分发收件人");
        }
        if (isPendingPreviewStatus(file.getStatus())) {
            boolean participant = isCurrentRouteSnapshotParticipant(userId, file);
            if (participant) {
                return new AccessReason(true, "CURRENT_ROUTE_STAGE", "当前阶段 route snapshot 参与人");
            }
            boolean currentAssignee = hasCurrentRunningApprovalTask(userId, file);
            return new AccessReason(currentAssignee, currentAssignee ? "CURRENT_APPROVAL_TASK" : "DENIED",
                    currentAssignee ? "当前运行审批任务处理人" : "不在当前文件当前阶段 route snapshot 参与人内，且不是当前运行审批任务处理人");
        }
        boolean matrixParticipant = canAccessCurrentViewMatrix(userId, file, currentViewMatrixAccessByCategory);
        return new AccessReason(matrixParticipant, matrixParticipant ? "CURRENT_VIEW_MATRIX" : "DENIED",
                matrixParticipant ? "当前查看矩阵参与人" : "不在当前文件类型查看矩阵解析主体内");
    }

    private AccessReason resolvePublishedPreviewAccessReason(Long userId, DccControlledFileDO file,
                                                             boolean hasDirectoryManagementPermission) {
        return resolvePublishedPreviewAccessReason(userId, file, hasDirectoryManagementPermission, null);
    }

    private AccessReason resolvePublishedPreviewAccessReason(Long userId, DccControlledFileDO file,
                                                             boolean hasDirectoryManagementPermission,
                                                             Map<Long, Boolean> currentViewMatrixAccessByCategory) {
        if (hasAuthorizedDirectoryAccess(userId, file, DccAccessTypeEnum.PREVIEW)) {
            return new AccessReason(true, "DIRECTORY_PREVIEW", "目录正文预览授权");
        }
        var context = distributionPreviewContext.get();
        if (context != null && Objects.equals(context.userId(), userId)
                && Objects.equals(context.controlledFileId(), file.getId())) {
            requireDistributionPreviewFile(context);
            return new AccessReason(true, "EXACT_DISTRIBUTION_TASK", "本次精确下发任务正文查阅");
        }
        boolean matrixParticipant = canAccessCurrentViewMatrix(userId, file, currentViewMatrixAccessByCategory);
        return new AccessReason(matrixParticipant, matrixParticipant ? "CURRENT_VIEW_MATRIX" : "DENIED",
                matrixParticipant ? "当前查看矩阵参与人" : "不在当前文件类型查看矩阵解析主体内");
    }

    private AccessReason resolvePendingPreviewAccessReason(Long userId, DccControlledFileDO file,
                                                           boolean hasDirectoryManagementPermission) {
        if (userId != null && userId.equals(file.getRequesterId())) {
            return new AccessReason(true, "REQUESTER_SELF", "申请人自查");
        }
        boolean currentAssignee = hasCurrentRunningApprovalTask(userId, file);
        return new AccessReason(currentAssignee, currentAssignee ? "CURRENT_APPROVAL_TASK" : "DENIED",
                currentAssignee ? "当前运行审批任务处理人" : "不是当前运行审批任务处理人");
    }

    private String resolveCurrentActiveVersionNo(DccControlledFileDO file, List<DccControlledFileDO> chainFiles) {
        if (file == null || file.getMasterId() == null) return null;
        var master = controlledFileMasterMapper.selectById(file.getMasterId());
        if (master == null || !Objects.equals(master.getTenantId(), TenantContextHolder.getRequiredTenantId())
                || !Objects.equals(master.getTenantId(), file.getTenantId())) {
            throw new IllegalStateException("current execution projection lacks its authoritative tenant master");
        }
        if (master.getCurrentActiveControlledFileId() == null) return null;
        return chainFiles.stream()
                .filter(history -> Objects.equals(history.getId(), master.getCurrentActiveControlledFileId())
                        && Objects.equals(history.getMasterId(), master.getId())
                        && Objects.equals(history.getTenantId(), master.getTenantId())
                        && history.getControlledTime() != null && history.getActivatedTime() != null
                        && DccControlledFileStatusEnum.ACTIVE.getStatus().equals(history.getStatus()))
                .map(DccControlledFileDO::getVersionNo)
                .findFirst()
                .orElse(null);
    }

    private boolean isActiveFileBeingModified(DccControlledFileDO file, List<DccControlledFileDO> chainFiles) {
        return DccControlledFileStatusEnum.ACTIVE.getStatus().equals(file.getStatus())
                && file.getId() != null
                && chainFiles.stream()
                        .anyMatch(history -> !file.getId().equals(history.getId())
                        && (isPendingPreviewStatus(history.getStatus())
                        || DccControlledFileStatusEnum.WORKING.getStatus().equals(history.getStatus())));
    }

    private List<DccControlledFileVersionHistoryRespVO> buildVersionHistory(Long userId, DccControlledFileDO file,
                                                                            List<DccControlledFileDO> chainFiles) {
        return buildVersionHistory(userId, file, chainFiles,
                directoryAccessPermissionService.hasDirectoryManagementPermission(userId));
    }

    private List<DccControlledFileVersionHistoryRespVO> buildVersionHistory(Long userId, DccControlledFileDO file,
                                                                            List<DccControlledFileDO> chainFiles,
                                                                            boolean hasDirectoryManagementPermission) {
        return buildVersionHistory(userId, file, chainFiles, hasDirectoryManagementPermission, null);
    }

    private List<DccControlledFileVersionHistoryRespVO> buildVersionHistory(Long userId, DccControlledFileDO file,
                                                                            List<DccControlledFileDO> chainFiles,
                                                                            boolean hasDirectoryManagementPermission,
                                                                            Map<Long, Boolean> currentViewMatrixAccessByCategory) {
        if (file.getMasterId() == null) {
            return List.of();
        }
        String currentActiveVersionNo = resolveCurrentActiveVersionNo(file, chainFiles);
        DccControlledFileCheckoutDO logicalCheckout = readLogicalCheckout(file);
        return chainFiles.stream()
                .filter(history -> hasCurrentRunningApprovalTask(userId, history)
                        || isWithinAssignedFileScope(userId, history)
                        && canAccessQuery(userId, history, new DccControlledFilePageReqVO(),
                        hasDirectoryManagementPermission, currentViewMatrixAccessByCategory))
                .filter(history -> !DccControlledFileStatusEnum.OBSOLETE.getStatus().equals(history.getStatus()) || canSeeObsolete(userId, history))
                .sorted(Comparator.comparing((DccControlledFileDO history) -> parseSortableVersion(history.getVersionNo()),
                                Comparator.nullsLast(DccControlledFileVersion::compareTo))
                        .reversed()
                        .thenComparing(DccControlledFileDO::getId,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .map(history -> {
                    DccControlledFileVersionHistoryRespVO respVO = new DccControlledFileVersionHistoryRespVO();
                    respVO.setId(history.getId());
                    respVO.setTitle(history.getTitle());
                    respVO.setFileNumber(history.getFileNumber());
                    respVO.setVersionNo(history.getVersionNo());
                    respVO.setRevisionCode(history.getRevisionCode());
                    respVO.setIterationNo(history.getIterationNo());
                    respVO.setStatus(history.getStatus());
                    respVO.setCurrentActiveVersionNo(currentActiveVersionNo);
                    if (!canAccessDetail(userId, history, hasDirectoryManagementPermission,
                            currentViewMatrixAccessByCategory)) {
                        fillCheckoutProjection(respVO, logicalCheckout);
                        respVO.setCheckedOutReason(null);
                        respVO.setCanPreview(false);
                        respVO.setCanDownload(false);
                        respVO.setCanPrint(false);
                        respVO.setActionProjection(toNameOnlyResponse(history).getActionProjection());
                        return respVO;
                    }
                    respVO.setRevisionChangeType(history.getRevisionChangeType());
                    respVO.setChangeType(history.getChangeType());
                    respVO.setControlledTime(history.getControlledTime());
                    respVO.setRevisionAttemptNo(history.getRevisionAttemptNo());respVO.setReworkPredecessorControlledFileId(history.getReworkPredecessorControlledFileId());
                    respVO.setFileOwnerUserId(history.getFileOwnerUserId());respVO.setFileOwnerUsernameSnapshot(history.getFileOwnerUsernameSnapshot());respVO.setFileOwnerNicknameSnapshot(history.getFileOwnerNicknameSnapshot());
                    respVO.setFileOwnerSignatureId(history.getFileOwnerSignatureId());respVO.setFileOwnerApprovalTaskId(history.getFileOwnerApprovalTaskId());respVO.setFileOwnerProcessInstanceId(history.getFileOwnerProcessInstanceId());respVO.setFileOwnerSelectedTime(history.getFileOwnerSelectedTime());
                    respVO.setRevisionSourceControlledFileId(history.getRevisionSourceControlledFileId());
                    respVO.setRevisionSourceVersionNo(history.getRevisionSourceVersionNo());
                    respVO.setSelectedIterationControlledFileId(history.getSelectedIterationControlledFileId());
                    respVO.setSelectedIterationVersionNo(history.getSelectedIterationVersionNo());
                    respVO.setSourceOriginalFileName(history.getSourceOriginalFileName());
                    respVO.setPredecessorControlledFileId(history.getPredecessorControlledFileId());
                    respVO.setRevisionBaseActiveControlledFileId(history.getRevisionBaseActiveControlledFileId());
                    respVO.setSourceSha256(history.getSourceSha256());
                    respVO.setPreviousSourceSha256(history.getPreviousSourceSha256());
                    respVO.setChangeDescription(history.getChangeDescription());
                    respVO.setSubmitterId(history.getSubmitterId());
                    respVO.setRequesterId(history.getRequesterId());
                    respVO.setProcessInstanceId(history.getProcessInstanceId());
                    respVO.setSubmittedTime(history.getSubmittedTime());
                    respVO.setCreateTime(history.getCreateTime());
                    respVO.setApprovedTime(history.getApprovedTime());
                    respVO.setRejectedTime(history.getRejectedTime());
                    respVO.setRejectReason(history.getRejectReason());
                    respVO.setFinalizationError(history.getFinalizationError());
                    respVO.setStatus(history.getStatus());
                    respVO.setNeedTraining(history.getNeedTraining());
                    respVO.setCurrentActiveVersionNo(currentActiveVersionNo);
                    respVO.setPublishedArtifactAvailable(hasFileRecord(history.getPublishedFileId()));
                    respVO.setStampedArtifactAvailable(hasFileRecord(history.getStampedFileId()));
                    respVO.setEffectiveDate(history.getEffectiveDate());
                    respVO.setEffectiveMode(history.getEffectiveMode());
                    respVO.setEffectiveTime(history.getEffectiveTime());
                    respVO.setEffectiveSignatureId(history.getEffectiveSignatureId());
                    respVO.setEffectiveProcessInstanceId(history.getEffectiveProcessInstanceId());
                    respVO.setSelectedDistributionDepartmentIds(readDistributionDepartmentIds(history.getDistributionDepartmentIdsJson()));
                    respVO.setPublishedTime(history.getPublishedTime());
                    respVO.setObsoletedTime(history.getObsoletedTime());
                    respVO.setSupersededByFileId(history.getSupersededByFileId());
                    respVO.setRemark(history.getRemark());
                    PreviewArtifactProjection previewProjection = resolvePreviewArtifactProjection(history);
                    respVO.setCanPreview(previewProjection.file() != null && canReadBinary(userId, history, DccAccessTypeEnum.PREVIEW,
                            hasDirectoryManagementPermission, currentViewMatrixAccessByCategory));
                    respVO.setPreviewUnavailableReason(previewProjection.unavailableReason());
                    respVO.setCanDownload(canReadBinary(userId, history, DccAccessTypeEnum.DOWNLOAD,
                            hasDirectoryManagementPermission));
                    fillCheckoutProjection(respVO, logicalCheckout);
                    respVO.setCanPrint(canPrintControlledFile(userId, history));
                    boolean canObsolete = isObsoleteControlledStage(history)
                            && permissionSupport.hasCategoryPermission(history.getCategoryId(), userId,
                            DccFileCategoryPermissionActionEnum.OBSOLETE);
                    boolean canPublish = DccControlledFileProcessTypeEnum.EXTERNAL_REVIEW.getCode().equals(history.getProcessType())
                            && DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus().equals(history.getStatus())
                            && permissionApi.hasAnyPermissions(userId, APPROVE_PERMISSION);
                    respVO.setActionProjection(buildActionProjection(userId, history, respVO.getCanPreview(),
                            respVO.getCanDownload(), respVO.getCanPrint(), canObsolete, canPublish, false, false));
                    return respVO;
                })
                .toList();
    }

    private List<DccControlledFileDistributionStatusRespVO> buildDistributionStatuses(Long controlledFileId) {
        return distributionMapper.selectListByControlledFileId(controlledFileId).stream()
                .map(distribution -> {
                    List<DccControlledFileDistributionRecipientStatusRespVO> recipients =
                            distributionRecipientMapper.selectListByDistributionId(distribution.getId()).stream()
                                    .map(recipient -> {
                                        DccControlledFileDistributionRecipientStatusRespVO recipientRespVO =
                                                new DccControlledFileDistributionRecipientStatusRespVO();
                                        recipientRespVO.setId(recipient.getId());
                                        recipientRespVO.setUserId(recipient.getUserId());
                                        recipientRespVO.setReadAt(recipient.getReadAt());
                                        recipientRespVO.setAcknowledgedAt(recipient.getAcknowledgedAt());
                                        recipientRespVO.setAckComment(recipient.getAckComment());
                                        return recipientRespVO;
                                    })
                                    .toList();
                    DccControlledFileDistributionStatusRespVO respVO = new DccControlledFileDistributionStatusRespVO();
                    respVO.setId(distribution.getId());
                    respVO.setDepartmentId(distribution.getDepartmentId());
                    respVO.setDistributionMedium(distribution.getDistributionMedium());
                    respVO.setStatus(distribution.getStatus());
                    respVO.setAcknowledgedBy(distribution.getAcknowledgedBy());
                    respVO.setAcknowledgedAt(distribution.getAcknowledgedAt());
                    respVO.setRecoveredBy(distribution.getRecoveredBy());
                    respVO.setRecoveredAt(distribution.getRecoveredAt());
                    respVO.setRecipientUserIds(recipients.stream()
                            .map(DccControlledFileDistributionRecipientStatusRespVO::getUserId)
                            .toList());
                    respVO.setRecipients(recipients);
                    return respVO;
                })
                .toList();
    }

    private List<DccControlledFileTrainingStatusRespVO> buildTrainingStatuses(Long controlledFileId) {
        return trainingMapper.selectListByControlledFileId(controlledFileId).stream()
                .map(training -> {
                    DccControlledFileTrainingStatusRespVO respVO = new DccControlledFileTrainingStatusRespVO();
                    respVO.setId(training.getId());
                    respVO.setDepartmentId(training.getDepartmentId());
                    respVO.setStatus(training.getStatus());
                    respVO.setAssignments(trainingAssignmentMapper.selectListByTrainingId(training.getId()).stream()
                            .map(assignment -> toTrainingAssignmentRespVO(training.getControlledFileId(), assignment))
                            .toList());
                    return respVO;
                })
                .toList();
    }

    private boolean allTrainingStatusesAcknowledged(List<DccControlledFileTrainingStatusRespVO> trainingStatuses) {
        return trainingStatuses != null
                && !trainingStatuses.isEmpty()
                && trainingStatuses.stream()
                .allMatch(status -> DccControlledFileTrainingStatusEnum.ACKNOWLEDGED.getCode().equals(status.getStatus()));
    }

    private boolean isTrainingSatisfiedForManualDistribution(
            DccControlledFileDO file,
            List<DccControlledFileTrainingStatusRespVO> trainingStatuses,
            Boolean nativeTrainingRecordAvailable) {
        if (isThreeWorkflowUploadOrRevision(file)) {
            return !Boolean.TRUE.equals(file.getNeedTraining()) || Boolean.TRUE.equals(nativeTrainingRecordAvailable);
        }
        return allTrainingStatusesAcknowledged(trainingStatuses);
    }

    private boolean isThreeWorkflowUploadOrRevision(DccControlledFileDO file) {
        return file != null && (DccControlledFileProcessDefinitionKeys.UPLOAD.equals(file.getProcessDefinitionKey())
                || DccControlledFileProcessDefinitionKeys.REVISION.equals(file.getProcessDefinitionKey()));
    }

    private boolean populateNativeWorkflowExecutionProjection(DccControlledFileRespVO respVO,
                                                               DccControlledFileDO file) {
        respVO.setTrainingRecordAvailable(Boolean.FALSE);
        respVO.setTrainingRecordFileName(null);
        respVO.setDistributionCompleted(Boolean.FALSE);
        if (!isThreeWorkflowUploadOrRevision(file)) {
            return false;
        }
        if (Boolean.TRUE.equals(file.getNeedTraining()) && file.getTrainingRecordFileId() != null) {
            FileDO trainingRecord = fileMapper.selectById(file.getTrainingRecordFileId());
            if (trainingRecord != null) {
                respVO.setTrainingRecordAvailable(Boolean.TRUE);
                respVO.setTrainingRecordFileName(trainingRecord.getName());
            }
        }
        respVO.setDistributionCompleted(isNativeWorkflowDistributionCompleted(file));
        return Boolean.TRUE.equals(respVO.getTrainingRecordAvailable());
    }

    private boolean isNativeWorkflowDistributionCompleted(DccControlledFileDO file) {
        if (file.getControlledTime() != null) return file.getDistributedTime() != null;
        if (!isThreeWorkflowUploadOrRevision(file) || StrUtil.isBlank(file.getProcessInstanceId())) {
            return false;
        }
        List<HistoricActivityInstance> activities =
                bpmTaskService.getActivityListByProcessInstanceId(file.getProcessInstanceId());
        return activities != null && activities.stream()
                .filter(Objects::nonNull)
                .anyMatch(activity -> "DISTRIBUTION".equals(activity.getActivityId())
                        && activity.getEndTime() != null);
    }

    private List<DccControlledFileSignatureSummaryRespVO> buildSignatureSummaries(DccControlledFileDO file) {
        if (file == null || file.getId() == null) {
            return List.of();
        }
        return signatureMapper.selectListByControlledFileId(file.getId()).stream()
                .sorted(Comparator.comparing(DccControlledFileSignatureDO::getSignedAt, Comparator.nullsLast(LocalDateTimeComparator.INSTANCE)))
                .map(signature -> {
                    DccControlledFileSignatureSummaryRespVO respVO = new DccControlledFileSignatureSummaryRespVO();
                    respVO.setId(signature.getId());
                    respVO.setTaskId(signature.getTaskId());
                    respVO.setProcessInstanceId(signature.getProcessInstanceId());
                    respVO.setFileNumberSnapshot(signature.getFileNumberSnapshot());
                    respVO.setActorId(signature.getActorId());
                    respVO.setActionType(signature.getActionType());
                    respVO.setTaskActionResult(normalizeSignatureTaskActionResult(signature.getActionType()));
                    respVO.setRevisionId(signature.getRevisionId());
                    respVO.setVersionNo(signature.getVersionNo());
                    respVO.setMeaningCode(signature.getMeaningCode());
                    projectSignatureBindingStatus(respVO, signature, file);
                    respVO.setEvidenceHashShort(shortSignatureHash(signature.getEvidenceHash()));
                    respVO.setActorUsernameSnapshot(signature.getActorUsernameSnapshot());
                    respVO.setActorNicknameSnapshot(signature.getActorNicknameSnapshot());
                    respVO.setActorDeptIdSnapshot(signature.getActorDeptIdSnapshot());
                    respVO.setActorDeptNameSnapshot(signature.getActorDeptNameSnapshot());
                    respVO.setActorPostNamesSnapshot(signature.getActorPostNamesSnapshot());
                    respVO.setActorRoleNamesSnapshot(signature.getActorRoleNamesSnapshot());
                    respVO.setSignaturePurpose(signature.getSignaturePurpose());
                    respVO.setAuthorizationBasis(signature.getAuthorizationBasis());
                    respVO.setAuthenticationMethod(signature.getAuthenticationMethod());
                    respVO.setRecordVersionSnapshot(signature.getRecordVersionSnapshot());
                    respVO.setRecordHashSnapshot(signature.getRecordHashSnapshot());
                    respVO.setClientIpSnapshot(signature.getClientIpSnapshot());
                    respVO.setUserAgentSnapshot(signature.getUserAgentSnapshot());
                    respVO.setSnapshotStatus(signature.getSnapshotStatus());
                    respVO.setSignatureMode(signature.getSignatureMode());
                    respVO.setComment(signature.getComment());
                    respVO.setSignedAt(signature.getSignedAt());
                    return respVO;
                })
                .toList();
    }

    private void projectSignatureBindingStatus(DccControlledFileSignatureSummaryRespVO respVO,
                                               DccControlledFileSignatureDO signature,
                                               DccControlledFileDO file) {
        respVO.setControlledCopyHashStatus(signature.getControlledCopyHashStatus());
        respVO.setEvidenceStatus(signature.getEvidenceStatus());
        if (file.getPublishedFileId() == null) {
            return;
        }
        DccControlledFileSignatureBindingVerification verification =
                signatureBindingService.verifyPublishedCopyBinding(signature, file);
        if (verification == null || verification.valid()) {
            if (verification != null && verification.binding() != null) {
                respVO.setControlledCopyHashStatus("BOUND");
            }
            return;
        }
        respVO.setControlledCopyHashStatus("INVALID");
        respVO.setEvidenceStatus("INVALID");
    }

    private String normalizeSignatureTaskActionResult(String actionType) {
        if (actionType == null) {
            return null;
        }
        return switch (actionType) {
            case "APPROVE" -> "APPROVED";
            case "REJECT" -> "REJECTED";
            case "RETURN" -> "RETURNED";
            case "TRANSFER" -> "TRANSFERRED";
            case "ADD_SIGN" -> "SIGN_ADDED";
            case "DISTRIBUTION_ACK" -> "DISTRIBUTION_ACK";
            case "DISTRIBUTION_SIGN" -> "DISTRIBUTION_SIGN";
            default -> actionType;
        };
    }

    private String shortSignatureHash(String hash) {
        if (hash == null || hash.isBlank()) {
            return null;
        }
        return hash.length() <= 12 ? hash.toLowerCase(Locale.ROOT) : hash.substring(0, 12).toLowerCase(Locale.ROOT);
    }

    private DccControlledFileTrainingAssignmentRespVO toTrainingAssignmentRespVO(Long controlledFileId,
                                                                                 DccControlledFileTrainingAssignmentDO assignment) {
        DccControlledFileTrainingAssignmentRespVO respVO = new DccControlledFileTrainingAssignmentRespVO();
        respVO.setId(assignment.getId());
        respVO.setUserId(assignment.getUserId());
        respVO.setStatus(assignment.getStatus());
        respVO.setAcknowledgedAt(assignment.getAcknowledgedAt());
        if (assignment.getUserId() != null) {
            DccControlledFileTrainingProgressDO progress =
                    trainingProgressMapper.selectByControlledFileIdAndUserId(controlledFileId, assignment.getUserId());
            if (progress != null) {
                int requiredViewSeconds = progress.getRequiredViewSeconds() == null ? 600 : progress.getRequiredViewSeconds();
                int accumulatedViewSeconds = progress.getAccumulatedViewSeconds() == null ? 0 : progress.getAccumulatedViewSeconds();
                respVO.setRequiredViewSeconds(requiredViewSeconds);
                respVO.setAccumulatedViewSeconds(accumulatedViewSeconds);
                respVO.setEligibleToAcknowledge(
                        progress.getAcknowledgedAt() == null && accumulatedViewSeconds >= requiredViewSeconds);
            }
        }
        return respVO;
    }

    private FileDO resolvePreviewFileForSummary(DccControlledFileDO file) {
        return resolvePreviewArtifactProjection(file).file();
    }

    private PreviewArtifactProjection resolvePreviewArtifactProjection(DccControlledFileDO file) {
        if (!expectsPreviewArtifact(file)) {
            return new PreviewArtifactProjection(null, null);
        }
        Long previewFileId = resolveBinaryFileId(file, DccAccessTypeEnum.PREVIEW);
        FileDO previewFile = previewFileId == null ? null : fileMapper.selectById(previewFileId);
        return new PreviewArtifactProjection(previewFile,
                previewFile == null ? buildPreviewArtifactMissingReason(file) : null);
    }

    private boolean hasFileRecord(Long fileId) {
        return fileId != null && fileMapper.selectById(fileId) != null;
    }

    private boolean expectsPreviewArtifact(DccControlledFileDO file) {
        if (file == null) {
            return false;
        }
        return DccControlledFileVersionPolicy.hasRetainedControlledBodyStatus(file.getStatus())
                || DccControlledFileStatusEnum.WORKING.getStatus().equals(file.getStatus())
                || DccControlledFileStatusEnum.REJECTED.getStatus().equals(file.getStatus())
                || DccControlledFileStatusEnum.FINALIZATION_FAILED.getStatus().equals(file.getStatus())
                || DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus().equals(file.getStatus())
                || DccControlledFileStatusEnum.PENDING_DELAYED_EFFECTIVE_CONFIRM.getStatus().equals(file.getStatus())
                || isPendingPreviewStatus(file.getStatus());
    }

    private String resolvePreviewFileName(DccControlledFileDO file, FileDO binaryFile) {
        if (binaryFile != null && StrUtil.isNotBlank(binaryFile.getName())) {
            return binaryFile.getName();
        }
        String controlledFileName = StrUtil.blankToDefault(file.getFileName(), file.getTitle());
        return StrUtil.blankToDefault(controlledFileName, "controlled-file-" + file.getId());
    }

    private String buildPreviewArtifactMissingReason(DccControlledFileDO file) {
        if (DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus().equals(file.getStatus())) {
            return "待生效盖章预览文件不存在或已被删除";
        }
        if (DccControlledFileStatusEnum.WORKING.getStatus().equals(file.getStatus())) {
            return "待提交受控文件源文件不存在或已被删除";
        }
        return isPendingPreviewStatus(file.getStatus())
                || DccControlledFileStatusEnum.PENDING_DELAYED_EFFECTIVE_CONFIRM.getStatus().equals(file.getStatus())
                || DccControlledFileStatusEnum.REJECTED.getStatus().equals(file.getStatus())
                || DccControlledFileStatusEnum.FINALIZATION_FAILED.getStatus().equals(file.getStatus())
                ? "受审源文件不存在或已被删除"
                : "正式预览文件不存在或已被删除";
    }

    private void applyOnlyOfficePreview(DccControlledFilePreviewMetadataRespVO respVO, Long userId,
                                        DccControlledFileDO file, FileDO binaryFile,
                                        BusinessFileAccessReference businessReference,
                                        DccPreviewAccessResult accessResult) {
        if (!onlyOfficePreviewProperties.isConfigured()) {
            respVO.setPreviewUnavailableReason(buildOnlyOfficeMissingReason());
            return;
        }
        respVO.setOnlyofficeBaseUrl(trimTrailingSlash(onlyOfficePreviewProperties.getBaseUrl()));
        DccOnlyOfficePreviewTokenService.IssuedPreviewToken issuedToken =
                onlyOfficePreviewTokenService.issueControlledFile(TenantContextHolder.getRequiredTenantId(),
                        userId, file.getId(), file.getVersionNo(), accessResult.accessEventId(),
                        currentPreviewPurpose(),
                        PREVIEW_VIEWER_TOKEN_TTL_SECONDS, binaryFile.getId(), businessReference, bodyReadContext.get());
        respVO.setOnlyofficeDocumentUrl(trimTrailingSlash(onlyOfficePreviewProperties.getPublicFileBaseUrl())
                + "/admin-api/dcc/controlled-files/" + file.getId() + "/onlyoffice-file?token=" + issuedToken.token());
    }

    private BusinessFileAccessReference requireBusinessFileAccess(BusinessFileAccessOperation operation,
                                                                  Long userId, FileDO binaryFile,
                                                                  DccRequestAuditContext auditContext,
                                                                  BusinessFileAccessReference claim) {
        if (binaryFile == null || binaryFile.getId() == null) {
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }
        return requireBusinessFileAccess(operation, userId, binaryFile.getId(),
                TenantContextHolder.getRequiredTenantId(), null, auditContext, claim);
    }

    private BusinessFileAccessReference requireBusinessFileAccess(BusinessFileAccessOperation operation,
                                                                  Long userId, Long infraFileId, Long tenantId,
                                                                  String serviceIdentity,
                                                                  DccRequestAuditContext auditContext,
                                                                  BusinessFileAccessReference claim) {
        try {
            return businessFileAccessService.assertAllowed(new BusinessFileAccessRequest(
                            operation, infraFileId, tenantId, userId, serviceIdentity,
                            auditContext.requireRequestId("controlled file business access"), claim,
                            auditContext.sourceIp(), auditContext.userAgent()))
                    .orElseThrow(() -> exception(CONTROLLED_FILE_ACCESS_DENIED));
        } catch (BusinessFileAccessDeniedException ex) {
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }
    }

    private BusinessFileAccessReference requireBusinessFileTokenAccess(BusinessFileAccessOperation operation,
                                                                        Long userId, Long infraFileId, Long tenantId,
                                                                        String serviceIdentity,
                                                                        DccRequestAuditContext auditContext,
                                                                        BusinessFileAccessReference claim) {
        try {
            return businessFileAccessService.assertAllowed(BusinessFileAccessRequest.tokenCallback(
                            operation, infraFileId, tenantId, userId, serviceIdentity,
                            auditContext.requireRequestId("controlled file business token access"), claim,
                            auditContext.sourceIp(), auditContext.userAgent()))
                    .orElseThrow(() -> exception(CONTROLLED_FILE_ACCESS_DENIED));
        } catch (BusinessFileAccessDeniedException ex) {
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }
    }

    private void requireOnlyOfficeConfigured() {
        if (!onlyOfficePreviewProperties.isConfigured()) {
            throw exception(CONTROLLED_FILE_ONLYOFFICE_PREVIEW_CONFIG_MISSING,
                    onlyOfficePreviewProperties.missingReason());
        }
    }

    private void requirePreviewAccessResult(DccPreviewAccessResult accessResult) {
        if (accessResult == null
                || accessResult.issuedAt() == null
                || accessResult.accessEventId() == null
                || StrUtil.isBlank(accessResult.accessEventCode())
                || accessResult.watermarkTraceId() == null
                || StrUtil.isBlank(accessResult.watermarkTraceCode())
                || StrUtil.isBlank(accessResult.viewerToken())
                || StrUtil.isBlank(accessResult.viewerTokenId())
                || StrUtil.isBlank(accessResult.viewerTokenNonce())) {
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_INVALID);
        }
    }

    private void requirePreviewContext(String viewerToken, String accessEventCode, String watermarkTraceCode,
                                       String viewerTokenId, String viewerTokenNonce) {
        if (StrUtil.isBlank(viewerToken)
                || StrUtil.isBlank(accessEventCode)
                || StrUtil.isBlank(watermarkTraceCode)
                || StrUtil.isBlank(viewerTokenId)
                || StrUtil.isBlank(viewerTokenNonce)) {
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_INVALID);
        }
    }

    private DccControlledFileAccessEventDO selectAccessEvent(String accessEventCode) {
        DccControlledFileAccessEventDO accessEvent = accessEventMapper.selectOne(
                new LambdaQueryWrapperX<DccControlledFileAccessEventDO>()
                        .eq(DccControlledFileAccessEventDO::getAccessEventCode, accessEventCode));
        if (accessEvent == null) {
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_INVALID);
        }
        return accessEvent;
    }

    private DccControlledFileAccessEventDO selectAccessEvent(Long accessEventId) {
        if (accessEventId == null) {
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_INVALID);
        }
        DccControlledFileAccessEventDO accessEvent = accessEventMapper.selectById(accessEventId);
        if (accessEvent == null) {
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_INVALID);
        }
        return accessEvent;
    }

    private DccControlledFileWatermarkTraceDO selectWatermarkTrace(String watermarkTraceCode) {
        DccControlledFileWatermarkTraceDO watermarkTrace = watermarkTraceMapper.selectOne(
                new LambdaQueryWrapperX<DccControlledFileWatermarkTraceDO>()
                        .eq(DccControlledFileWatermarkTraceDO::getTraceCode, watermarkTraceCode));
        if (watermarkTrace == null) {
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_INVALID);
        }
        return watermarkTrace;
    }

    private DccControlledFileWatermarkTraceDO selectWatermarkTraceByAccessEventId(Long accessEventId) {
        if (accessEventId == null) {
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_INVALID);
        }
        DccControlledFileWatermarkTraceDO watermarkTrace = watermarkTraceMapper.selectOne(
                new LambdaQueryWrapperX<DccControlledFileWatermarkTraceDO>()
                        .eq(DccControlledFileWatermarkTraceDO::getAccessEventId, accessEventId));
        if (watermarkTrace == null) {
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_INVALID);
        }
        return watermarkTrace;
    }

    private void requireMatchingOnlyOfficeToken(DccOnlyOfficePreviewTokenService.PreviewTokenPayload tokenPayload,
                                                DccControlledFileDO file) {
        if (!Objects.equals(tokenPayload.getFileId(), file.getId())
                || !Objects.equals(tokenPayload.getVersionId(), StrUtil.trim(file.getVersionNo()))
                || !currentPreviewPurpose().equals(tokenPayload.getPurpose())) {
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
        }
    }

    private void requireMatchingPreviewEvidence(Long userId, DccControlledFileDO file,
                                                DccControlledFileAccessEventDO accessEvent,
                                                DccControlledFileWatermarkTraceDO watermarkTrace) {
        if (bodyReadContext.get() != null && !Objects.equals(accessEvent.getTenantId(), file.getTenantId()))
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
        if (!Objects.equals(accessEvent.getControlledFileId(), file.getId())
                || !Objects.equals(accessEvent.getUserId(), userId)
                || !Objects.equals(accessEvent.getFileVersionNo(), file.getVersionNo())
                || !PREVIEW_ACCESS_TYPE.equals(accessEvent.getAccessType())
                || !currentPreviewPurpose().equals(accessEvent.getPurpose())
                || !ACCESS_RESULT_SUCCESS.equals(accessEvent.getResult())
                || !Objects.equals(watermarkTrace.getAccessEventId(), accessEvent.getId())
                || !Objects.equals(watermarkTrace.getAccessEventCode(), accessEvent.getAccessEventCode())
                || !Objects.equals(watermarkTrace.getControlledFileId(), file.getId())
                || !Objects.equals(watermarkTrace.getUserId(), userId)
                || !Objects.equals(watermarkTrace.getFileVersionNo(), file.getVersionNo())) {
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_INVALID);
        }
        requireSupervisorPreviewRoundEvidence(file, accessEvent);
    }

    private void requireSupervisorPreviewRoundEvidence(DccControlledFileDO file, DccControlledFileAccessEventDO event) {
        if (bodyReadContext.get() != null) {
            if (!Objects.equals(bodyReadContext.get().bpmRound(), event.getProcessInstanceId())
                    || !Objects.equals(event.getTenantId(), file.getTenantId()))
                throw exception(CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
            return;
        }
        if (!DccControlledFileStatusEnum.PENDING_SUPERVISOR_REVIEW.getStatus().equals(file.getStatus())) return;
        if (StrUtil.isBlank(event.getProcessInstanceId()) || !Objects.equals(event.getProcessInstanceId(), file.getProcessInstanceId())
                || !Objects.equals(event.getTenantId(), file.getTenantId()))
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
    }

    private void requireMatchingOnlyOfficeEvidence(DccOnlyOfficePreviewTokenService.PreviewTokenPayload tokenPayload,
                                                   DccControlledFileDO file,
                                                   DccControlledFileAccessEventDO accessEvent,
                                                   DccControlledFileWatermarkTraceDO watermarkTrace) {
        if (bodyReadContext.get() != null && !Objects.equals(accessEvent.getTenantId(), tokenPayload.getTenantId()))
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
        if (bodyReadContext.get() != null) requireSupervisorPreviewRoundEvidence(file, accessEvent);
        if (!Objects.equals(accessEvent.getId(), tokenPayload.getAccessEventId())
                || !Objects.equals(accessEvent.getControlledFileId(), tokenPayload.getFileId())
                || !Objects.equals(accessEvent.getControlledFileId(), file.getId())
                || !Objects.equals(accessEvent.getUserId(), tokenPayload.getUserId())
                || !Objects.equals(accessEvent.getFileVersionNo(), tokenPayload.getVersionId())
                || !PREVIEW_ACCESS_TYPE.equals(accessEvent.getAccessType())
                || !currentPreviewPurpose().equals(accessEvent.getPurpose())
                || !Objects.equals(accessEvent.getPurpose(), tokenPayload.getPurpose())
                || !ACCESS_RESULT_SUCCESS.equals(accessEvent.getResult())
                || !Objects.equals(watermarkTrace.getAccessEventId(), accessEvent.getId())
                || !Objects.equals(watermarkTrace.getAccessEventCode(), accessEvent.getAccessEventCode())
                || !Objects.equals(watermarkTrace.getControlledFileId(), file.getId())
                || !Objects.equals(watermarkTrace.getUserId(), tokenPayload.getUserId())
                || !Objects.equals(watermarkTrace.getFileVersionNo(), tokenPayload.getVersionId())) {
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
        }
    }

    private void recordDownloadAccess(DccControlledFileDO file, Long userId,
                                      DccControlledFileAccessEventDO accessEvent,
                                      boolean allowed, String reason) {
        int insertedRows = accessLogMapper.insert(DccControlledFileAccessLogDO.builder()
                .controlledFileId(file.getId())
                .accessEventId(accessEvent.getId())
                .accessEventCode(accessEvent.getAccessEventCode())
                .fileVersionNo(file.getVersionNo())
                .userId(userId)
                .actionType(DOWNLOAD_ACCESS_TYPE)
                .purpose(CONTROLLED_DOWNLOAD_PURPOSE)
                .result(allowed ? DccAccessResultEnum.ALLOWED.name() : DccAccessResultEnum.DENIED.name())
                .failureCode(allowed ? null : reason)
                .reason(reason)
                .sourceIp(accessEvent.getSourceIp())
                .userAgent(accessEvent.getUserAgent())
                .requestId(accessEvent.getRequestId())
                .build());
        if (insertedRows <= 0) {
            throw exception(DCC_DOWNLOAD_AUDIT_RECORD_FAILED);
        }
    }

    private void recordOnlyOfficeReadAccess(DccControlledFileDO file, Long userId,
                                            DccControlledFileAccessEventDO accessEvent,
                                            DccControlledFileWatermarkTraceDO watermarkTrace,
                                            boolean allowed, String reason,
                                            DccRequestAuditContext auditContext) {
        int inserted=accessLogMapper.insert(DccControlledFileAccessLogDO.builder()
                .tenantId(TenantContextHolder.getRequiredTenantId())
                .controlledFileId(file.getId())
                .accessEventId(accessEvent.getId())
                .accessEventCode(accessEvent.getAccessEventCode())
                .watermarkTraceCode(watermarkTrace.getTraceCode())
                .fileVersionNo(file.getVersionNo())
                .userId(userId)
                .actionType(OFFICE_READ_ACTION_TYPE)
                .purpose(accessEvent.getPurpose())
                .result(allowed ? DccAccessResultEnum.ALLOWED.name() : DccAccessResultEnum.DENIED.name())
                .reason(reason)
                .sourceIp(auditContext.sourceIp())
                .userAgent(auditContext.userAgent())
                .requestId(auditContext.requireRequestId("onlyoffice preview file"))
                .build());
        if (inserted!=1) throw new IllegalStateException("Office body read evidence was not persisted");
    }

    private void recordPreviewAccess(DccControlledFileDO file, Long userId,
                                     DccControlledFileAccessEventDO accessEvent,
                                     DccControlledFileWatermarkTraceDO watermarkTrace,
                                     boolean allowed, String reason,
                                     DccRequestAuditContext auditContext) {
        accessLogMapper.insert(DccControlledFileAccessLogDO.builder()
                .controlledFileId(file.getId())
                .accessEventId(accessEvent.getId())
                .accessEventCode(accessEvent.getAccessEventCode())
                .watermarkTraceCode(watermarkTrace.getTraceCode())
                .fileVersionNo(file.getVersionNo())
                .userId(userId)
                .actionType(PREVIEW_ACCESS_TYPE)
                .purpose(accessEvent.getPurpose())
                .result(allowed ? DccAccessResultEnum.ALLOWED.name() : DccAccessResultEnum.DENIED.name())
                .reason(reason)
                .sourceIp(auditContext.sourceIp())
                .userAgent(auditContext.userAgent())
                .requestId(auditContext.requireRequestId("preview file"))
                .build());
    }

    private void restoreTenantContext(Long tenantId, boolean ignore) {
        TenantContextHolder.clear();
        if (tenantId != null) {
            TenantContextHolder.setTenantId(tenantId);
        }
        if (ignore) {
            TenantContextHolder.setIgnore(true);
        }
    }

    private String buildOnlyOfficeMissingReason() {
        return "OnlyOffice preview config is missing: " + onlyOfficePreviewProperties.missingReason();
    }

    private String trimTrailingSlash(String value) {
        String normalized = StrUtil.trimToEmpty(value);
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private DccControlledFileRouteSnapshotRespVO toSnapshotRespVO(
            DccControlledFileRouteSnapshotDO snapshot,
            List<DccControlledFileTaskAssigneeSnapshotDO> taskAssigneeSnapshots) {
        DccControlledFileRouteSnapshotRespVO respVO = new DccControlledFileRouteSnapshotRespVO();
        respVO.setId(snapshot.getId());
        respVO.setRouteVersionNo(snapshot.getRouteVersionNo());
        respVO.setStageNo(snapshot.getStageNo());
        respVO.setStageCode(snapshot.getStageCode());
        respVO.setStageName(snapshot.getStageName());
        respVO.setStageOrder(snapshot.getStageOrder());
        respVO.setCandidateSourceType(snapshot.getCandidateSourceType());
        respVO.setCandidateSourceId(snapshot.getCandidateSourceId());
        List<Long> candidateSourceIds = resolveSnapshotCandidateSourceIds(snapshot, taskAssigneeSnapshots);
        respVO.setCandidateSourceIds(candidateSourceIds);
        Map<Long, String> departmentNames = resolveHistoricalDepartmentNames(candidateSourceIds, taskAssigneeSnapshots);
        respVO.setCandidateSourceNames(candidateSourceIds.stream()
                .map(id -> StrUtil.blankToDefault(departmentNames.get(id), "部门#" + id))
                .toList());
        respVO.setApproveMethod(snapshot.getApproveMethod());
        respVO.setApproveRatio(snapshot.getApproveRatio());
        respVO.setRequireAllApprovals(snapshot.getRequireAllApprovals());
        respVO.setResolvedUserIds(StrUtil.isBlank(snapshot.getResolvedUserIds())
                ? List.of()
                : Arrays.stream(snapshot.getResolvedUserIds().split(",")).map(Long::valueOf).toList());
        respVO.setDepartmentObligations(taskAssigneeSnapshots.stream()
                .filter(item -> Objects.equals(item.getStageCode(), snapshot.getStageCode()))
                .filter(item -> Objects.equals(item.getStageNo(), snapshot.getStageNo()))
                .filter(item -> candidateSourceIds.contains(item.getDepartmentId()))
                .map(item -> toDepartmentObligationRespVO(item, departmentNames))
                .toList());
        return respVO;
    }

    private List<Long> parseSnapshotIds(String ids, Long fallbackId) {
        if (StrUtil.isNotBlank(ids)) {
            return Arrays.stream(ids.split(","))
                    .map(String::trim)
                    .filter(StrUtil::isNotBlank)
                    .map(Long::valueOf)
                    .toList();
        }
        return fallbackId == null ? List.of() : List.of(fallbackId);
    }

    private List<Long> resolveSnapshotCandidateSourceIds(
            DccControlledFileRouteSnapshotDO snapshot,
            List<DccControlledFileTaskAssigneeSnapshotDO> taskAssigneeSnapshots) {
        if (StrUtil.isNotBlank(snapshot.getCandidateSourceIds())) {
            return parseSnapshotIds(snapshot.getCandidateSourceIds(), snapshot.getCandidateSourceId());
        }
        if (!"DEPT".equalsIgnoreCase(snapshot.getCandidateSourceType())) {
            return parseSnapshotIds(null, snapshot.getCandidateSourceId());
        }
        List<Long> taskDepartmentIds = taskAssigneeSnapshots.stream()
                .filter(item -> Objects.equals(item.getStageCode(), snapshot.getStageCode()))
                .filter(item -> Objects.equals(item.getStageNo(), snapshot.getStageNo()))
                .map(DccControlledFileTaskAssigneeSnapshotDO::getDepartmentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (!taskDepartmentIds.isEmpty()) {
            return taskDepartmentIds;
        }
        return parseSnapshotIds(null, snapshot.getCandidateSourceId());
    }

    private Map<Long, String> resolveHistoricalDepartmentNames(
            List<Long> candidateSourceIds,
            List<DccControlledFileTaskAssigneeSnapshotDO> taskAssigneeSnapshots) {
        if (candidateSourceIds.isEmpty()) {
            return Map.of();
        }
        return taskAssigneeSnapshots.stream()
                .filter(Objects::nonNull)
                .filter(snapshot -> snapshot.getDepartmentId() != null)
                .filter(snapshot -> candidateSourceIds.contains(snapshot.getDepartmentId()))
                .filter(snapshot -> StrUtil.isNotBlank(snapshot.getDepartmentName()))
                .collect(Collectors.toMap(DccControlledFileTaskAssigneeSnapshotDO::getDepartmentId,
                        snapshot -> StrUtil.trim(snapshot.getDepartmentName()), (left, right) -> left,
                        LinkedHashMap::new));
    }

    private DccControlledFileRouteSnapshotRespVO.DepartmentObligationRespVO toDepartmentObligationRespVO(
            DccControlledFileTaskAssigneeSnapshotDO snapshot, Map<Long, String> departmentNames) {
        DccControlledFileRouteSnapshotRespVO.DepartmentObligationRespVO respVO =
                new DccControlledFileRouteSnapshotRespVO.DepartmentObligationRespVO();
        respVO.setSnapshotId(snapshot.getId());
        respVO.setDepartmentId(snapshot.getDepartmentId());
        respVO.setDepartmentName(StrUtil.blankToDefault(snapshot.getDepartmentName(),
                "部门#" + snapshot.getDepartmentId()));
        respVO.setAssigneeUserId(snapshot.getAssigneeUserId());
        respVO.setAssigneeName(snapshot.getAssigneeName());
        respVO.setLeaderConfigDigest(snapshot.getLeaderConfigDigest());
        respVO.setObligationId(snapshot.getObligationId());
        respVO.setBpmTaskId(snapshot.getBpmTaskId());
        return respVO;
    }

    private DccFileCategoryDO validateCategory(Long categoryId) {
        DccFileCategoryDO category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw exception(FILE_CATEGORY_NOT_EXISTS);
        }
        if (!Boolean.TRUE.equals(category.getActive())) {
            throw exception(CONTROLLED_FILE_CATEGORY_DISABLED);
        }
        return category;
    }

    private Long resolveResponseFileTypeTaxonomyId(DccControlledFileDO file) {
        if (file.getFileTypeTaxonomyId() != null) {
            return file.getFileTypeTaxonomyId();
        }
        if (StrUtil.isBlank(file.getFileTypeLevel1())
                || StrUtil.isBlank(file.getFileTypeLevel2())
                || StrUtil.isBlank(file.getFileTypeLevel3())) {
            return null;
        }
        return fileTypeTaxonomyAdminService.resolveActiveIdByPath(
                file.getFileTypeLevel1(),
                file.getFileTypeLevel2(),
                file.getFileTypeLevel3(),
                file.getFileTypeLevel4(),
                file.getFileTypeLevel5());
    }

    private void recordAccess(Long fileId, Long userId, DccAccessTypeEnum accessType, boolean allowed, String reason) {
        accessLogMapper.insert(DccControlledFileAccessLogDO.builder()
                .controlledFileId(fileId)
                .userId(userId)
                .actionType(accessType.name())
                .result(allowed ? DccAccessResultEnum.ALLOWED.name() : DccAccessResultEnum.DENIED.name())
                .reason(reason)
                .build());
    }

    private void recordAccess(Long fileId, Long userId, DccAccessTypeEnum accessType, boolean allowed,
                              String reason, DccRequestAuditContext auditContext) {
        accessLogMapper.insert(DccControlledFileAccessLogDO.builder()
                .controlledFileId(fileId)
                .userId(userId)
                .actionType(accessType.name())
                .result(allowed ? DccAccessResultEnum.ALLOWED.name() : DccAccessResultEnum.DENIED.name())
                .reason(reason)
                .sourceIp(auditContext.sourceIp())
                .userAgent(auditContext.userAgent())
                .requestId(auditContext.requireRequestId(accessType.name()))
                .build());
    }

    private DccRequestAuditContext requireAuditContext(DccRequestAuditContext auditContext) {
        if (auditContext == null) {
            throw new IllegalArgumentException("DCC request audit context is required");
        }
        return auditContext;
    }

    private <T> List<T> sliceRows(DccControlledFilePageReqVO reqVO, List<T> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        if (PageParam.PAGE_SIZE_NONE.equals(reqVO.getPageSize())) {
            return rows;
        }
        int pageNo = Math.max(reqVO.getPageNo(), 1);
        int pageSize = Math.max(reqVO.getPageSize(), 1);
        int fromIndex = Math.min((pageNo - 1) * pageSize, rows.size());
        int toIndex = Math.min(fromIndex + pageSize, rows.size());
        return rows.subList(fromIndex, toIndex);
    }

    private static final class LocalDateTimeComparator implements Comparator<java.time.LocalDateTime> {
        private static final LocalDateTimeComparator INSTANCE = new LocalDateTimeComparator();

        @Override
        public int compare(java.time.LocalDateTime left, java.time.LocalDateTime right) {
            return left.compareTo(right);
        }
    }

    private record AccessReason(boolean allowed, String source, String reason) {
    }

    private record PreviewArtifactProjection(FileDO file, String unavailableReason) {
    }

}
