package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccProjectProductRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileUploadPreviewReqVO;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.BpmProcessInstanceCancelReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskApproveReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskRejectReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskReturnReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskSignCreateReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskTransferReqVO;
import cn.iocoder.yudao.module.bpm.enums.task.BpmTaskSignTypeEnum;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileApproveTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileCreateSignTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRejectTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileReturnTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileCurrentVersionRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRouteReadinessRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRouteSnapshotRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSubmitReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSubmitIterationReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileTaskReadinessBlockerRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileTaskReadinessReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileTaskReadinessRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileTrainingRecordReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileTransferTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileWithdrawReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccSignatureActionRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccCategoryDirectoryBindingDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccFileDirectoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDistributionDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDistributionRecipientDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRouteSnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSignatureDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTaskAssigneeSnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccCategoryDirectoryBindingMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryPermissionRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccFileDirectoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionRecipientMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileCheckoutMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileRouteSnapshotMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSignatureMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTaskAssigneeSnapshotMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.enums.DccAccessTypeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccAccessSubjectTypeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileDistributionStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileChangeTypeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileProcessTypeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStageCodeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileMasterStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFilePreviewKindEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccDistributionMediumEnum;
import cn.iocoder.yudao.module.dcc.enums.DccProjectCodeStatusConstants;
import cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyAdminService;
import cn.iocoder.yudao.module.dcc.service.audit.DccControlledFileAccessAuditService;
import cn.iocoder.yudao.module.dcc.service.audit.DccLifecycleLogCreateCommand;
import cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyPath;
import cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessPermissionService;
import cn.iocoder.yudao.module.dcc.service.projectcode.DccProjectFileTemplateService;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketBoundFile;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketMarkBoundCommand;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketResolveCommand;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketService;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.mdm.api.product.MdmProductApi;
import cn.iocoder.yudao.module.mdm.api.product.dto.MdmProductRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.DccProjectProductIdentityResolver;
import cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.DccProjectProductIdentityResolver.Product;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccApprovedProductIdentityMapper;
import jakarta.annotation.Resource;
import org.flowable.task.api.Task;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.function.Supplier;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_CATEGORY_DISABLED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_DRAWING_PDF_FILE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_DRAWING_PDF_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_DISTRIBUTION_DEPARTMENT_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_DISTRIBUTION_MEDIUM_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_FILE_NUMBER_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_FINAL_APPROVAL_NOT_READY;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_RELATED_FILE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PRODUCT_CODE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PROCESS_TYPE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ROUTE_NOT_CONFIGURED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ROUTE_NOT_READY;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SOURCE_FILE_TYPE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_STAMPED_PDF_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_EVIDENCE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_PERSIST_FAILED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SUBMIT_DIRECTORY_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SUBMIT_DIRECTORY_NOT_LEAF;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SUBMIT_IDEMPOTENCY_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ITERATION_SUBMIT_NOT_ALLOWED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_CATEGORY_DIRECTORY_BINDING_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_TASK_STAGE_UNSUPPORTED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_TASK_TARGET_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_TRAINING_RECORD_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_UPLOAD_TICKET_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_VERSION_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_VERSION_NOT_GREATER;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_WITHDRAW_NOT_ALLOWED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_WITHDRAWN_ACTION_NOT_ALLOWED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_WORKFLOW_IN_PROGRESS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_MAJOR_REVISION_NOT_ALLOWED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_ACCESS_DENIED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.EXTERNAL_FILE_REVIEW_ENDPOINT_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_CATEGORY_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_TYPE_TAXONOMY_LEVEL_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_CODE_DISABLED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_CODE_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_FILE_TEMPLATE_SELECTION_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.ROUTE_PREVIEW_APPROVER_NOT_FOUND;

@Service
@Validated
public class DccControlledFileWorkflowServiceImpl implements DccControlledFileWorkflowService {
    @Resource private DccApprovedProductIdentityMapper approvedProductIdentityMapper;
    @Resource private DccPublicUploadPlacementService publicUploadPlacementService;

    public static final String BPM_PROCESS_DEFINITION_KEY = DccControlledFileProcessDefinitionKeys.LEGACY_APPROVAL;
    static final String APPLICANT_REWORK_TASK_DEFINITION_KEY = "APPLICANT_REWORK";
    private static final String SUBMIT_PERMISSION = "dcc:controlled-file:submit";
    private static final String REVIEW_PERMISSION = "dcc:controlled-file:review";
    private static final String APPROVE_PERMISSION = "dcc:controlled-file:approve";

    private static final Set<String> WITHDRAW_ALLOWED_STATUSES = Set.of(
            DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus(),
            DccControlledFileStatusEnum.PENDING_MATRIX_REVIEW.getStatus(),
            DccControlledFileStatusEnum.PENDING_MATRIX_APPROVAL.getStatus(),
            DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL.getStatus(),
            DccControlledFileStatusEnum.PENDING_APPLICANT_TRAINING_RECORD.getStatus()
    );

    @Resource
    private DccFileCategoryMapper categoryMapper;
    @Resource
    private DccCategoryDirectoryBindingMapper categoryDirectoryBindingMapper;
    @Resource
    private DccFileDirectoryMapper directoryMapper;
    @Resource
    private DccControlledFileMapper controlledFileMapper;
    @Resource
    private DccControlledFileCheckoutMapper checkoutMapper;
    @Resource
    private DccControlledFileMasterMapper controlledFileMasterMapper;
    @Resource
    private DccControlledFileRouteSnapshotMapper routeSnapshotMapper;
    @Resource
    private DccControlledFileTaskAssigneeSnapshotMapper taskAssigneeSnapshotMapper;
    @Resource
    private DccWorkflowSignoffAssignmentService signoffAssignmentService;
    @Resource private DccApprovalFileOwnerSelectionService fileOwnerSelection;
    @Resource
    private DccWorkflowDatePolicy workflowDatePolicy;
    @Resource
    private DccControlledFileDistributionMapper distributionMapper;
    @Resource
    private DccControlledFileDistributionRecipientMapper distributionRecipientMapper;
    @Resource
    private DccControlledFileSignatureMapper signatureMapper;
    @Resource
    private DccControlledFileSignatureBindingService signatureBindingService;
    @Resource
    private DccControlledFileSourceOwnershipService sourceOwnershipService;
    @Resource
    private FileMapper fileMapper;
    @Resource
    private FileService fileService;
    @Resource
    private BpmProcessInstanceApi bpmProcessInstanceApi;
    @Resource
    private BpmProcessInstanceService bpmProcessInstanceService;
    @Resource
    private BpmTaskService bpmTaskService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private DccDirectoryAccessPermissionService directoryAccessPermissionService;
    @Resource
    private DccSignatureVerificationService signatureVerificationService;
    @Resource
    private DccControlledFileFinalizationService finalizationService;
    @Resource
    private DccUploadTicketService uploadTicketService;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private DccOfflineTrainingRecordService offlineTraining;
    @Resource
    private DccProjectCodeMapper projectCodeMapper;
    @Resource
    private DccFileTypeTaxonomyAdminService fileTypeTaxonomyAdminService;
    @Resource
    private DccControlledFileQueryService queryService;
    @Resource
    private DccControlledFileRelatedFileService relatedFileService;
    @Resource
    private DccControlledFileAttachmentService attachmentService;
    @Resource
    private DccControlledFileVersionPolicy versionPolicy = DccControlledFileVersionPolicy.defaultPolicy();
    @Resource
    private DccControlledContentAdapter platformAdapter;
    @Resource
    private MdmProductApi mdmProductApi;
    @Resource
    private DccControlledFileApprovalRouteAssigneeResolver approvalRouteAssigneeResolver;
    @Resource
    private DccApprovalParticipantPostValidator approvalParticipantPostValidator;
    @Resource
    private DccControlledFileRouteReadinessService routeReadinessService;
    @Resource
    private DccControlledFileAccessAuditService lifecycleAuditService;
    @Resource
    private DccProjectFileTemplateService projectFileTemplateService;
    @Resource
    private DccProjectAccessService projectAccessService;
    @Resource
    private DccControlledFileCategoryPermissionSupport categoryPermissionSupport;
    @Resource
    private DccControlledFileSubmitMutex submitMutex;
    @Resource
    private DccControlledFileNameClaimService nameClaimService;
    @Resource private DccControlledFileRevisionService revisionService;
    @Resource private DccApplicationRoundService applicationRoundService;
    @Resource private cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributesService projectAttributesService;
    @Resource private cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectApplicationSnapshotService projectApplicationSnapshots;
    @Resource private DccWorkingApplicationDraftInitializer workingApplicationDraftInitializer;
    @Resource private DccControlledFileAssignmentScopeService assignmentScopeService;

    @Override
    public DccControlledFileRouteReadinessRespVO previewRoute(Long userId, Long categoryId,
                                                              List<Long> selectedSignoffUserIds,
                                                              List<Long> selectedSignoffDepartmentIds, String actionType) {
        DccFileCategoryDO category = validateCategory(categoryId);
        if (selectedSignoffDepartmentIds != null) {
            rejectManualSignoffUsers(selectedSignoffUserIds);
            return routeReadinessService.evaluateDepartments(category.getId(), userId,
                    selectedSignoffDepartmentIds, actionType).response();
        }
        return routeReadinessService.evaluate(category.getId(), userId, selectedSignoffUserIds, actionType).response();
    }

    @Override
    public DccControlledFileCurrentVersionRespVO getCurrentVersionByFileNumber(Long userId, String fileNumber) {
        return getCurrentVersionByFileNumberInternal(userId, fileNumber, null, null);
    }

    @Override
    public DccControlledFileCurrentVersionRespVO getCurrentVersionByFileNumber(Long userId, String fileNumber,
                                                                                 Long dccProjectCodeId,
                                                                                 Long fileTypeTaxonomyId) {
        if (dccProjectCodeId == null || fileTypeTaxonomyId == null) {
            throw exception(CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING);
        }
        validateEnabledProjectCode(dccProjectCodeId, true);
        ResolvedFileTypeTaxonomy taxonomy = resolveFileTypeTaxonomy(fileTypeTaxonomyId, true);
        return getCurrentVersionByFileNumberInternal(userId, fileNumber, dccProjectCodeId, taxonomy.path().id());
    }

    private DccControlledFileCurrentVersionRespVO getCurrentVersionByFileNumberInternal(Long userId, String fileNumber,
                                                                                         Long dccProjectCodeId,
                                                                                         Long fileTypeTaxonomyLeafId) {
        String normalizedFileNumber = normalizeFileNumber(fileNumber);
        List<DccControlledFileMasterDO> masters = dccProjectCodeId == null
                ? controlledFileMasterMapper.selectListByFileNumber(normalizedFileNumber)
                : controlledFileMasterMapper.selectListByLogicalIdentity(dccProjectCodeId,
                fileTypeTaxonomyLeafId, normalizedFileNumber);
        if ((masters == null || masters.isEmpty()) && dccProjectCodeId != null) {
            DccControlledFileMasterDO legacyMaster = resolveUniqueLegacyMaster(dccProjectCodeId,
                    fileTypeTaxonomyLeafId, normalizedFileNumber);
            masters = legacyMaster == null ? List.of() : List.of(legacyMaster);
        }
        if (masters == null || masters.isEmpty()) {
            return DccControlledFileCurrentVersionRespVO.builder()
                    .fileNumber(normalizedFileNumber)
                    .matched(Boolean.FALSE)
                    .modifying(Boolean.FALSE)
                    .build();
        }
        List<DccControlledFileMasterDO> activeMasters = masters.stream()
                .filter(master -> master.getCurrentActiveControlledFileId() != null)
                .toList();
        if (activeMasters.size() != 1) {
            throw exception(CONTROLLED_FILE_FILE_NUMBER_CONFLICT);
        }
        DccControlledFileMasterDO master = activeMasters.get(0);
        DccControlledFileDO activeFile = controlledFileMapper.selectById(master.getCurrentActiveControlledFileId());
        if (activeFile == null || !DccControlledFileStatusEnum.ACTIVE.getStatus().equals(activeFile.getStatus())) {
            throw exception(CONTROLLED_FILE_FILE_NUMBER_CONFLICT);
        }
        assertCurrentVersionIdentityConsistent(normalizedFileNumber, dccProjectCodeId,
                fileTypeTaxonomyLeafId, master, activeFile);
        List<DccControlledFileDO> chainFiles = controlledFileMapper.selectListByMasterId(master.getId());
        if (chainFiles == null) {
            chainFiles = List.of(activeFile);
        }
        Long originalFileId = resolveOriginalFileId(activeFile);
        FileTrace originalFileTrace = resolveFileTrace(originalFileId);
        FileTrace sourceFileTrace = resolveFileTrace(activeFile.getSourceFileId());
        FileTrace publishedFileTrace = resolveFileTrace(activeFile.getPublishedFileId());
        FileTrace stampedFileTrace = resolveFileTrace(activeFile.getStampedFileId());
        DccControlledFileRespVO projectedDetail = queryService.getControlledFile(userId, activeFile.getId());
        return DccControlledFileCurrentVersionRespVO.builder()
                .fileNumber(activeFile.getFileNumber())
                .matched(Boolean.TRUE)
                .currentControlledFileId(activeFile.getId())
                .masterId(master.getId())
                .fileName(activeFile.getFileName())
                .currentVersionNo(activeFile.getVersionNo())
                .status(activeFile.getStatus())
                .categoryId(activeFile.getCategoryId())
                .directoryId(activeFile.getDirectoryId())
                .originalFileId(originalFileId)
                .originalFileName(originalFileTrace.name())
                .originalFilePath(originalFileTrace.path())
                .sourceFileId(activeFile.getSourceFileId())
                .sourceFileName(sourceFileTrace.name())
                .sourceFilePath(sourceFileTrace.path())
                .publishedFileId(activeFile.getPublishedFileId())
                .publishedFileName(publishedFileTrace.name())
                .publishedFilePath(publishedFileTrace.path())
                .stampedFileId(activeFile.getStampedFileId())
                .stampedFileName(stampedFileTrace.name())
                .stampedFilePath(stampedFileTrace.path())
                .productMasterId(activeFile.getProductMasterId())
                .productSource(activeFile.getProductSource()).productCatalogId(activeFile.getProductCatalogId())
                .productRelationId(activeFile.getProductRelationId()).productCreateRequestId(activeFile.getProductCreateRequestId())
                .productCode(activeFile.getProductCode())
                .productName(activeFile.getProductName())
                .dccProjectCodeId(activeFile.getDccProjectCodeId())
                .fileTypeTaxonomyId(activeFile.getFileTypeTaxonomyId())
                .fileTypeLevel1(activeFile.getFileTypeLevel1())
                .fileTypeLevel2(activeFile.getFileTypeLevel2())
                .fileTypeLevel3(activeFile.getFileTypeLevel3())
                .fileTypeLevel4(activeFile.getFileTypeLevel4())
                .fileTypeLevel5(activeFile.getFileTypeLevel5())
                .modifying(chainFiles.stream().anyMatch(this::isUnfinishedWorkflowVersion))
                .actionProjection(projectedDetail.getActionProjection())
                .build();
    }

    private void assertCurrentVersionIdentityConsistent(String requestedNormalizedFileNumber,
                                                        Long requestedProjectCodeId,
                                                        Long requestedFileTypeTaxonomyLeafId,
                                                        DccControlledFileMasterDO master,
                                                        DccControlledFileDO activeFile) {
        String activeNormalizedFileNumber = normalizeStoredFileNumber(activeFile.getFileNumber());
        if (!Objects.equals(activeNormalizedFileNumber, requestedNormalizedFileNumber)) {
            throw exception(CONTROLLED_FILE_FILE_NUMBER_CONFLICT);
        }
        String masterNormalizedFileNumber = normalizeStoredFileNumberOrNull(master.getNormalizedFileNumber());
        if (masterNormalizedFileNumber != null
                && !Objects.equals(masterNormalizedFileNumber, requestedNormalizedFileNumber)) {
            throw exception(CONTROLLED_FILE_FILE_NUMBER_CONFLICT);
        }

        Long expectedProjectCodeId = requestedProjectCodeId == null
                ? master.getDccProjectCodeId() : requestedProjectCodeId;
        if (expectedProjectCodeId != null && !Objects.equals(activeFile.getDccProjectCodeId(), expectedProjectCodeId)) {
            throw exception(CONTROLLED_FILE_FILE_NUMBER_CONFLICT);
        }
        if (requestedProjectCodeId != null && master.getDccProjectCodeId() != null
                && !Objects.equals(master.getDccProjectCodeId(), requestedProjectCodeId)) {
            throw exception(CONTROLLED_FILE_FILE_NUMBER_CONFLICT);
        }

        Long activeTaxonomyLeafId = resolveControlledFileTypeTaxonomyId(activeFile);
        Long expectedTaxonomyLeafId = requestedFileTypeTaxonomyLeafId == null
                ? master.getFileTypeTaxonomyLeafId() : requestedFileTypeTaxonomyLeafId;
        if (expectedTaxonomyLeafId != null && !Objects.equals(activeTaxonomyLeafId, expectedTaxonomyLeafId)) {
            throw exception(CONTROLLED_FILE_FILE_NUMBER_CONFLICT);
        }
        if (requestedFileTypeTaxonomyLeafId != null && master.getFileTypeTaxonomyLeafId() != null
                && !Objects.equals(master.getFileTypeTaxonomyLeafId(), requestedFileTypeTaxonomyLeafId)) {
            throw exception(CONTROLLED_FILE_FILE_NUMBER_CONFLICT);
        }
    }

    private String normalizeStoredFileNumber(String fileNumber) {
        String normalized = normalizeStoredFileNumberOrNull(fileNumber);
        if (normalized == null) {
            throw exception(CONTROLLED_FILE_FILE_NUMBER_CONFLICT);
        }
        return normalized;
    }

    private String normalizeStoredFileNumberOrNull(String fileNumber) {
        String normalized = StrUtil.trim(fileNumber);
        if (StrUtil.isBlank(normalized)) {
            return null;
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitControlledFile(Long userId, DccControlledFileSubmitReqVO reqVO) {
        if (StrUtil.equals(reqVO.getProcessType(), DccControlledFileProcessTypeEnum.EXTERNAL_REVIEW.getCode())) {
            throw exception(EXTERNAL_FILE_REVIEW_ENDPOINT_REQUIRED);
        }
        requireOrdinaryNewUpload(reqVO);
        rejectManualSignoffUsersForThreeWorkflow(DccControlledFileProcessDefinitionKeys.UPLOAD,
                reqVO.getSelectedSignoffUserIds());
        return publicUploadPlacementService.create(userId,reqVO,
                storage->submitNewWithDerivedStorage(userId,reqVO,storage));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createWorkingControlledFile(Long userId, DccControlledFileSubmitReqVO reqVO) {
        if (reqVO == null || StrUtil.equals(reqVO.getProcessType(),
                DccControlledFileProcessTypeEnum.EXTERNAL_REVIEW.getCode())) {
            throw exception(EXTERNAL_FILE_REVIEW_ENDPOINT_REQUIRED);
        }
        requireOrdinaryNewUpload(reqVO);
        return publicUploadPlacementService.create(userId,reqVO,
                storage->createWorkingWithDerivedStorage(userId,reqVO,storage));
    }

    @Override
    @Transactional(rollbackFor=Exception.class)
    public Long submitNewWithDerivedStorage(Long userId,DccControlledFileSubmitReqVO request,DccDerivedUploadStorage storage) {
        requireDerivedStorage(userId,request,storage);
        requireOrdinaryNewUpload(request);
        return submitControlledFileIdempotently(userId,request,DccControlledFileProcessDefinitionKeys.UPLOAD,storage);
    }
    @Override
    @Transactional(rollbackFor=Exception.class)
    public Long createWorkingWithDerivedStorage(Long userId,DccControlledFileSubmitReqVO request,DccDerivedUploadStorage storage) {
        requireDerivedStorage(userId,request,storage);
        requireOrdinaryNewUpload(request);
        return createWorkingControlledFileIdempotently(userId,request,"WORKING_CREATE",storage);
    }
    private void requireDerivedStorage(Long actor,DccControlledFileSubmitReqVO request,DccDerivedUploadStorage storage) {
        if(storage==null || request==null
          || !Objects.equals(storage.tenantId,TenantContextHolder.getRequiredTenantId())
          || !Objects.equals(actor,storage.actorId) || !Objects.equals(request.getDccProjectCodeId(),storage.projectId)
          || !Objects.equals(request.getProjectFolderId(),storage.folderId) || !Objects.equals(request.getCategoryId(),storage.categoryId))
            throw cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributeErrors.fail(
                    cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributeErrors.FOLDER_INVALID);
    }

    private void requireOrdinaryNewUpload(DccControlledFileSubmitReqVO request) {
        if (request == null || !DccControlledFileChangeTypeEnum.NEW.getCode().equals(StrUtil.trim(request.getChangeType()))
                || request.getRevisionTargetControlledFileId() != null
                || request.getRevisionSourceControlledFileId() != null
                || StrUtil.isNotBlank(request.getRevisionSourceReason())) {
            throw exception(CONTROLLED_FILE_MAJOR_REVISION_NOT_ALLOWED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveWorkingApplicationAttributes(Long userId,Long fileId,
            cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes actual) {
        if(actual==null) throw exception(CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING);
        var file=requireEditableWorkingApplication(userId,fileId);
        projectApplicationSnapshots.saveReservedDraft(userId,file.getDccProjectCodeId(),workingApplicationType(file),fileId,actual);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DccWorkingApplicationAttributes readWorkingApplicationAttributes(Long userId,Long fileId) {
        var file=requireWorkingApplication(userId,fileId,true);
        String type=workingApplicationType(file);
        var saved=projectApplicationSnapshots.readReservedDraft(userId,file.getDccProjectCodeId(),type,fileId);
        String unavailable=null;
        try {
            validateWorkingIterationSubmission(userId,TenantContextHolder.getRequiredTenantId(),file,
                    controlledFileMasterMapper.selectByIdForUpdate(file.getMasterId()));
        } catch (cn.iocoder.yudao.framework.common.exception.ServiceException denied) {
            unavailable=denied.getMessage(); // Explicit business ineligibility; unexpected failures propagate.
        }
        return new DccWorkingApplicationAttributes(fileId,file.getDccProjectCodeId(),type,
                projectAttributesService.readValue(saved.getDefaultSourceJson()),
                projectAttributesService.readValue(saved.getActualAttributesJson()),unavailable==null,unavailable,
                file.getEffectiveDate(),file.getNeedTraining(),null,file.getChangeDescription());
    }

    @Override
    @Transactional(rollbackFor=Exception.class)
    public DccWorkingApplicationAttributes readReplacementApplicationAttributes(Long userId,Long selectedId,Long baselineId) {
        var file=requireWorkingApplication(userId,selectedId,true,true);
        var master=controlledFileMasterMapper.selectByIdForUpdate(file.getMasterId());
        var baseline=baselineId==null?null:controlledFileMapper.selectByIdAndTenantForUpdate(file.getTenantId(),baselineId);
        if(master==null || baseline==null || !Objects.equals(master.getLatestControlledFileId(),baselineId)
                || !DccRevisionReworkPolicy.sameIdentity(baseline,file) || baseline.getControlledTime()==null
                || !DccControlledFileVersionPolicy.isCurrentControlledStatus(baseline.getStatus())
                || !Objects.equals(baselineId,file.getRevisionBaseActiveControlledFileId())
                || !"REVISION".equals(workingApplicationType(file)) || file.getRevisionChangeType()!=null)
            throw exception(CONTROLLED_FILE_ITERATION_SUBMIT_NOT_ALLOWED);
        var chain=controlledFileMapper.selectListByMasterIdForUpdate(master.getId());
        var version=versionPolicy.requireStored(file);
        if(!version.isWorkingIteration() || !(version.formal().display().equals(baseline.getVersionNo())
                || DccRevisionReworkPolicy.correctionMatchesBaseline(baseline,file,chain,versionPolicy)))
            throw exception(CONTROLLED_FILE_ITERATION_SUBMIT_NOT_ALLOWED);
        var saved=projectApplicationSnapshots.readReservedDraft(userId,file.getDccProjectCodeId(),"REVISION",selectedId);
        String unavailable=null;
        try {validateWorkingIterationSubmission(userId,file.getTenantId(),file,master,"REPLACEMENT");
            DccRevisionReworkPolicy.requireAttempt(baseline,file,"REPLACEMENT",chain,versionPolicy);
        }catch(cn.iocoder.yudao.framework.common.exception.ServiceException denied){unavailable=denied.getMessage();}
        catch(IllegalArgumentException denied){unavailable=denied.getMessage();}
        return new DccWorkingApplicationAttributes(selectedId,file.getDccProjectCodeId(),"REVISION",
                projectAttributesService.readValue(saved.getDefaultSourceJson()),projectAttributesService.readValue(saved.getActualAttributesJson()),
                unavailable==null,unavailable,file.getEffectiveDate(),file.getNeedTraining(),null,file.getChangeDescription());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreWorkingApplicationAttributes(Long userId,Long fileId) {
        var file=requireEditableWorkingApplication(userId,fileId);
        String type=workingApplicationType(file);
        var saved=projectApplicationSnapshots.readReservedDraft(userId,file.getDccProjectCodeId(),type,fileId);
        projectApplicationSnapshots.saveReservedDraft(userId,file.getDccProjectCodeId(),type,fileId,
                projectAttributesService.readValue(saved.getDefaultSourceJson()));
    }

    private String workingApplicationType(DccControlledFileDO file) {
        if("NEW".equals(file.getChangeType())) return "UPLOAD";
        if("REVISION".equals(file.getChangeType())) return "REVISION";
        throw exception(CONTROLLED_FILE_ITERATION_SUBMIT_NOT_ALLOWED);
    }

    private DccControlledFileDO requireEditableWorkingApplication(Long userId,Long fileId) {
        return requireWorkingApplication(userId,fileId,false);
    }

    private DccControlledFileDO requireWorkingApplication(Long userId,Long fileId,boolean allowCheckedOutRead) {
        return requireWorkingApplication(userId,fileId,allowCheckedOutRead,false);
    }
    private DccControlledFileDO requireWorkingApplication(Long userId,Long fileId,boolean allowCheckedOutRead,boolean replacementContext) {
        Long tenant=TenantContextHolder.getRequiredTenantId();
        var identity=controlledFileMapper.selectById(fileId);
        if(identity==null || !Objects.equals(identity.getTenantId(),tenant)) throw exception(CONTROLLED_FILE_NOT_EXISTS);
        lockDraftProject(identity.getDccProjectCodeId());
        var master=controlledFileMasterMapper.selectByIdForUpdate(identity.getMasterId());
        var file=controlledFileMapper.selectByIdAndTenantForUpdate(tenant,fileId);
        if(master==null || file==null || !Objects.equals(master.getTenantId(),tenant) || !Objects.equals(file.getTenantId(),tenant)
                || !Objects.equals(file.getMasterId(),master.getId()) || userId==null
                || !(Objects.equals(userId,file.getRequesterId()) || replacementContext && projectAccessService.hasProjectOwner(userId,file.getDccProjectCodeId()))
                || !Objects.equals(file.getDccProjectCodeId(),master.getDccProjectCodeId())
                || file.getCategoryId()==null || !Objects.equals(file.getCategoryId(),master.getCategoryId())
                || file.getFileTypeTaxonomyId()==null || !Objects.equals(file.getFileTypeTaxonomyId(),master.getFileTypeTaxonomyLeafId())
                || StrUtil.isBlank(file.getFileNumber()) || !Objects.equals(file.getFileNumber(),master.getNormalizedFileNumber())
                || !assignmentScopeService.isWithinAssignedFileScope(userId,fileId)
                || !"WORKING".equals(file.getStatus()) || StrUtil.isNotBlank(file.getProcessInstanceId())
                || file.getControlledTime()!=null || !allowCheckedOutRead && file.getCheckedOutBy()!=null)
            throw exception(CONTROLLED_FILE_ITERATION_SUBMIT_NOT_ALLOWED);
        projectAccessService.assertProjectEditorOrOwner(userId,file.getDccProjectCodeId());
        if(replacementContext)projectAccessService.assertProjectOwner(userId,file.getDccProjectCodeId());
        if(!categoryPermissionSupport.hasCategoryPermission(file.getCategoryId(),userId,DccFileCategoryPermissionActionEnum.UPLOAD))
            throw exception(DCC_PROJECT_ACCESS_DENIED);
        workingApplicationType(file);
        return file;
    }

    private Long createWorkingControlledFileIdempotently(Long userId, DccControlledFileSubmitReqVO reqVO,
                                                          String ownershipType) {
        return createWorkingControlledFileIdempotently(userId,reqVO,ownershipType,null);
    }
    private Long createWorkingControlledFileIdempotently(Long userId,DccControlledFileSubmitReqVO reqVO,String ownershipType,DccDerivedUploadStorage storage) {
        String idempotencyKey = StrUtil.trim(reqVO.getIdempotencyKey());
        if (StrUtil.isBlank(idempotencyKey)) {
            throw exception(CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING);
        }
        reqVO.setIdempotencyKey(idempotencyKey);
        String payloadHash = DigestUtil.sha256Hex("CREATE_WORKING:" + ownershipType + ":"
                + JsonUtils.toJsonString(reqVO));
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        return submitMutex.execute("create:" + tenantId + ":" + userId + ":" + idempotencyKey, () -> {
            DccControlledFileDO existing = controlledFileMapper.selectByCreationIdempotency(
                    tenantId, userId, idempotencyKey);
            if (existing != null) {
                return requireMatchingCreationPayload(existing, payloadHash);
            }
            reqVO.setCreationPayloadHash(payloadHash);
            try {
                PreparedSubmitContext context = prepareSubmitContext(userId, reqVO, true, true, null, true,storage);
                workflowDatePolicy.requireReviewDate(reqVO.getEffectiveDate());
                DccControlledFileDO file = insertControlledFile(context, userId,
                        DccControlledFileStatusEnum.WORKING.getStatus(), null, false, ownershipType);
                validateAndBindRelatedFiles(userId, file.getId(),
                        context.projectCode().getId(), reqVO.getRelatedControlledFileIds());
                bindSubmitTickets(context, userId, file.getId());
                bindSubmitAttachments(context, file.getId());
                return file.getId();
            } catch (ControlledFileInsertConflict ex) {
                DccControlledFileDO winner = controlledFileMapper.selectByCreationIdempotencyForUpdate(
                        tenantId, userId, idempotencyKey);
                if (winner != null) {
                    return requireMatchingCreationPayload(winner, payloadHash);
                }
                throw ex.failure;
            }
        });
    }

    @Override
    public DccProjectProductRespVO previewProjectProduct(
            Long userId, Long projectCodeId) {
        DccProjectCodeDO project = validateEnabledProjectCode(projectCodeId, true);
        projectAccessService.assertProjectEditorOrOwner(userId, projectCodeId);
        Product product = resolveDccProductFromProjectCode(project);
        validateScreenshotProductCode(product);
        return DccProjectProductRespVO.builder()
                .projectCodeId(projectCodeId).productMasterId(product.masterId())
                .productCode(product.code()).productName(product.name())
                .source(product.source()).productCatalogId(product.catalogId())
                .productRelationId(product.relationId()).productCreateRequestId(product.requestId()).build();
    }

    @Override
    public void validateApprovalPdfUpload(Long userId, Long fileId, String taskId, Long categoryId, String sessionId) {
        if (fileId == null || StrUtil.isBlank(taskId)) {
            throw exception(CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED);
        }
        ValidatedTaskActionContext context = validateTaskAction(userId, fileId, taskId,
                resolveNativeProcessDefinitionKey(fileId), "APPROVE");
        DccControlledFileStageCodeEnum finalStage=isThreeWorkflowUploadOrRevision(context.file())
                ?DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW:DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL;
        if (context.stageCode() != finalStage
                || !Objects.equals(context.file().getCategoryId(), categoryId)) {
            throw exception(CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED);
        }
        requireApprovalUploadSession(fileId, taskId, sessionId);
        queryService.getControlledFile(userId, fileId);
    }

    @Override
    public void validateSourceUploadContext(Long userId, DccControlledFileUploadPreviewReqVO request) {
        DccFileCategoryDO category = validateCategory(request.getCategoryId());
        if (!categoryPermissionSupport.hasCategoryPermission(category.getId(), userId,
                DccFileCategoryPermissionActionEnum.UPLOAD)) {
            throw exception(DCC_PROJECT_ACCESS_DENIED);
        }
        if ("EXTERNAL_REVIEW".equals(request.getUploadContext())) {
            return;
        }
        if ("CHECKIN".equals(request.getUploadContext())) {
            DccControlledFileDO file = request.getControlledFileId() == null ? null
                    : controlledFileMapper.selectById(request.getControlledFileId());
            if (file == null || file.getMasterId() == null
                    || !Objects.equals(file.getCategoryId(), category.getId())
                    || !Set.of("WORKING", "REJECTED", "PENDING_APPLICANT_REWORK", "ACTIVE", "SUPERSEDED")
                    .contains(StrUtil.trimToEmpty(file.getStatus()))) {
                throw exception(CONTROLLED_FILE_UPLOAD_TICKET_INVALID);
            }
            var checkout = checkoutMapper.selectActiveByMasterId(TenantContextHolder.getRequiredTenantId(), file.getMasterId());
            if (checkout == null || !Objects.equals(checkout.getActorId(), userId)
                    || !Objects.equals(checkout.getBaseIterationId(), file.getId())) {
                throw exception(CONTROLLED_FILE_UPLOAD_TICKET_INVALID);
            }
            projectAccessService.assertProjectEditorOrOwner(userId, file.getDccProjectCodeId());
            return;
        }
        if (!"NEW_UPLOAD".equals(request.getUploadContext()) || request.getDccProjectCodeId() == null
                || request.getFileTypeTaxonomyId() == null || StrUtil.isBlank(request.getFileName())
                || !Objects.equals(category.getFileTypeTaxonomyId(), request.getFileTypeTaxonomyId())) {
            throw exception(PROJECT_FILE_TEMPLATE_SELECTION_INVALID);
        }
        DccProjectCodeDO project = validateEnabledProjectCode(request.getDccProjectCodeId(), true);
        projectAccessService.assertProjectEditorOrOwner(userId, project.getId());
        if (!Objects.equals(request.getCategoryId(),fileTypeTaxonomyAdminService.resolveActiveCategoryId(request.getFileTypeTaxonomyId()))) {
            throw exception(PROJECT_FILE_TEMPLATE_SELECTION_INVALID);
        }
        validateNewFileTaxonomyLeaf(DccControlledFileChangeTypeEnum.NEW, true,
                resolveFileTypeTaxonomy(request.getFileTypeTaxonomyId(), true));
    }

    private void requireApprovalUploadSession(Long fileId, String taskId, String sessionId) {
        String prefix = "dcc-approval:" + fileId + ":" + taskId.length() + ":" + taskId + ":";
        if (sessionId == null || !sessionId.startsWith(prefix) || sessionId.length() <= prefix.length()) {
            throw exception(CONTROLLED_FILE_UPLOAD_TICKET_INVALID);
        }
    }

    private void validateAndBindRelatedFiles(Long userId, Long fileId, Long projectCodeId,
                                             List<Long> relatedFileIds) {
        if (relatedFileIds != null && !relatedFileIds.isEmpty()) {
            projectAccessService.assertProjectEditorOrOwner(userId, projectCodeId);
            for (Long relatedFileId : relatedFileIds) {
                DccControlledFileDO target = relatedFileId == null ? null
                        : controlledFileMapper.selectById(relatedFileId);
                if (target == null || !queryService.canViewFileName(userId, target)) {
                    throw exception(CONTROLLED_FILE_RELATED_FILE_INVALID);
                }
            }
        }
        relatedFileService.validateAndBindRelatedFiles(fileId, projectCodeId, relatedFileIds);
    }

    private Long requireMatchingCreationPayload(DccControlledFileDO existing, String payloadHash) {
        if (!Objects.equals(existing.getCreationPayloadHash(), payloadHash)) {
            throw exception(CONTROLLED_FILE_SUBMIT_IDEMPOTENCY_CONFLICT);
        }
        return existing.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitWorkingIteration(Long userId, Long iterationId,
                                       DccControlledFileSubmitIterationReqVO reqVO) {
        if (iterationId == null || reqVO == null || StrUtil.isBlank(reqVO.getIdempotencyKey())) {
            throw exception(CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING);
        }
        String idempotencyKey = StrUtil.trim(reqVO.getIdempotencyKey());
        boolean needTraining = Boolean.TRUE.equals(reqVO.getNeedTraining());
        rejectManualSignoffUsers(reqVO.getSelectedSignoffUserIds());
        List<Long> selectedSignoffUserIds = List.of();
        reqVO.setIdempotencyKey(idempotencyKey);
        String payloadHash = DigestUtil.sha256Hex(JsonUtils.toJsonString(java.util.Arrays.asList(iterationId,reqVO)));
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        DccControlledFileDO initial = controlledFileMapper.selectById(iterationId);
        if (initial == null || initial.getMasterId() == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        lockDraftProject(initial.getDccProjectCodeId());
        publicUploadPlacementService.lockSourceLocation(userId,iterationId);
        DccControlledFileMasterDO master = controlledFileMasterMapper.selectByIdForUpdate(initial.getMasterId());
        if (master == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        DccControlledFileDO file = controlledFileMapper.selectByIdAndTenantForUpdate(tenantId, iterationId);
        if (file == null || !Objects.equals(file.getMasterId(), master.getId())) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        DccControlledFileDO idempotentResult = controlledFileMapper.selectBySubmitIdempotencyForUpdate(
                tenantId, userId, idempotencyKey);
        if (idempotentResult != null) {
            if (!(Objects.equals(idempotentResult.getId(), iterationId)
                    || Objects.equals(idempotentResult.getSelectedIterationControlledFileId(),iterationId))
                    || !Objects.equals(idempotentResult.getMasterId(),master.getId())
                    || !Objects.equals(idempotentResult.getSubmitPayloadHash(), payloadHash)
                    || StrUtil.isBlank(idempotentResult.getProcessInstanceId())) {
                throw exception(CONTROLLED_FILE_SUBMIT_IDEMPOTENCY_CONFLICT);
            }
            return idempotentResult.getId();
        }
        DccControlledFileDO applicantReworkPredecessor = validateWorkingIterationSubmission(userId, tenantId,
                file, master,reqVO.getRevisionChangeType());
        if(master.getLatestControlledFileId()!=null) {
            var baseline=controlledFileMapper.selectByIdAndTenantForUpdate(tenantId,master.getLatestControlledFileId());
            if(baseline==null || !Objects.equals(baseline.getMasterId(),master.getId()) || baseline.getControlledTime()==null
                    || !versionPolicy.isControlledBaseline(baseline)) throw exception(CONTROLLED_FILE_ITERATION_SUBMIT_NOT_ALLOWED);
            file=revisionService.createRevision(userId,baseline.getId(),iterationId,reqVO);
            if(file==null || Objects.equals(file.getId(),iterationId) || !Objects.equals(file.getMasterId(),master.getId())
                    || !Objects.equals(file.getTenantId(),tenantId) || file.getRevisionChangeType()==null
                    || !Objects.equals(file.getSelectedIterationControlledFileId(),iterationId))
                throw exception(CONTROLLED_FILE_ITERATION_SUBMIT_NOT_ALLOWED);
        } else if(master.getCurrentActiveControlledFileId()!=null || "REVISION".equals(file.getChangeType())) {
            throw exception(CONTROLLED_FILE_ITERATION_SUBMIT_NOT_ALLOWED);
        } else if(versionPolicy.requireStored(file).isWorkingIteration()) {
            file=revisionService.createInitialCandidate(userId,iterationId,reqVO);
            if(file==null || Objects.equals(file.getId(),iterationId) || !Objects.equals(file.getMasterId(),master.getId())
                    || !Objects.equals(file.getTenantId(),tenantId) || !"INITIAL".equals(file.getRevisionChangeType())
                    || !Objects.equals(file.getSelectedIterationControlledFileId(),iterationId))
                throw exception(CONTROLLED_FILE_ITERATION_SUBMIT_NOT_ALLOWED);
        } else {
            if(!"INITIAL".equals(reqVO.getRevisionChangeType()) || reqVO.getEffectiveDate()==null)
                throw exception(CONTROLLED_FILE_ITERATION_SUBMIT_NOT_ALLOWED);
            workflowDatePolicy.requireReviewDate(reqVO.getEffectiveDate());
            if(controlledFileMapper.updateById(DccControlledFileDO.builder().id(file.getId())
                    .effectiveDate(reqVO.getEffectiveDate()).changeDescription(reqVO.getChangeDescription()).build())!=1)
                throw exception(CONTROLLED_FILE_ITERATION_SUBMIT_NOT_ALLOWED);
            file.setEffectiveDate(reqVO.getEffectiveDate());file.setChangeDescription(reqVO.getChangeDescription());
        }
        var formalCandidateVersion=versionPolicy.parseStored(file);
        if(formalCandidateVersion==null || formalCandidateVersion.isWorkingIteration())
            throw exception(CONTROLLED_FILE_ITERATION_SUBMIT_NOT_ALLOWED);
        String processDefinitionKey = resolveWorkingIterationProcessDefinitionKey(file, master);
        if(!Objects.equals(file.getId(),iterationId)) prepareApplicationDraft(userId,file,reqVO.getProjectAttributes());
        workflowDatePolicy.requireReviewDate(file.getEffectiveDate());
        DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute resolvedRoute = routeReadinessService
                .evaluateDepartments(file.getCategoryId(), userId, reqVO.getSelectedSignoffDepartmentIds(),
                        DccControlledFileProcessDefinitionKeys.toActionType(processDefinitionKey))
                .requireReady();
        String pendingStatus = toPendingStatus(resolveTaskStage(resolvedRoute.nodes().get(0).stageCode()));
        if (StrUtil.isBlank(pendingStatus)) {
            throw exception(CONTROLLED_FILE_ITERATION_SUBMIT_NOT_ALLOWED);
        }
        LocalDateTime submittedTime = LocalDateTime.now();
        try {
            if (controlledFileMapper.claimWorkingIterationSubmission(tenantId, userId, DccControlledFileDO.builder()
                    .id(file.getId())
                    .status(pendingStatus)
                    .submitterId(userId)
                    .submitIdempotencyKey(idempotencyKey)
                    .submitPayloadHash(payloadHash)
                    .needTraining(needTraining)
                    .processDefinitionKey(processDefinitionKey)
                    .submittedTime(submittedTime)
                    .build()) != 1) {
                throw exception(CONTROLLED_FILE_ITERATION_SUBMIT_NOT_ALLOWED);
            }
        } catch (DuplicateKeyException ex) {
            throw exception(CONTROLLED_FILE_SUBMIT_IDEMPOTENCY_CONFLICT);
        }
        file.setStatus(pendingStatus);
        file.setSubmitterId(userId);
        file.setSubmitIdempotencyKey(idempotencyKey);
        file.setSubmitPayloadHash(payloadHash);
        file.setNeedTraining(needTraining);
        file.setProcessDefinitionKey(processDefinitionKey);
        file.setSubmittedTime(submittedTime);
        persistApprovalRouteSnapshots(file.getId(), resolvedRoute);
        persistDepartmentTaskAssigneeSnapshots(file.getId(), resolvedRoute);
        String processInstanceId = createApprovalProcess(userId, file, resolvedRoute, processDefinitionKey);
        if (controlledFileMapper.updateById(DccControlledFileDO.builder()
                .id(file.getId())
                .processInstanceId(processInstanceId)
                .build()) != 1) {
            throw exception(CONTROLLED_FILE_ITERATION_SUBMIT_NOT_ALLOWED);
        }
        file.setProcessInstanceId(processInstanceId);
        closeApplicantReworkPredecessorForResubmission(userId, applicantReworkPredecessor, file, processInstanceId);
        freezeApplicationAttributes(userId,file,reqVO.getProjectAttributes());
        platformAdapter.recordSubmitted(file, userId, processInstanceId);
        if (applicantReworkPredecessor != null) {
            platformAdapter.recordResubmitted(applicantReworkPredecessor, file.getId());
        } else if (file.getReworkPredecessorControlledFileId()!=null) {
            var failed=controlledFileMapper.selectByIdAndTenantForUpdate(tenantId,file.getReworkPredecessorControlledFileId());
            if(failed==null)throw exception(CONTROLLED_FILE_ITERATION_SUBMIT_NOT_ALLOWED);
            platformAdapter.recordResubmitted(failed,file.getId());
        }
        return file.getId();
    }

    private String resolveWorkingIterationProcessDefinitionKey(DccControlledFileDO file,
                                                               DccControlledFileMasterDO master) {
        if (DccControlledFileChangeTypeEnum.REVISION.getCode().equals(file.getChangeType())
                || isWorkingIterationBasedOnFormalActive(file, master)) {
            return DccControlledFileProcessDefinitionKeys.REVISION;
        }
        return DccControlledFileProcessDefinitionKeys.UPLOAD;
    }

    private boolean isWorkingIterationBasedOnFormalActive(DccControlledFileDO file,
                                                          DccControlledFileMasterDO master) {
        return DccControlledFileChangeTypeEnum.NEW.getCode().equals(file.getChangeType())
                && (file.getRevisionBaseActiveControlledFileId() != null
                || (master != null && master.getCurrentActiveControlledFileId() != null));
    }

    private DccControlledFileDO validateWorkingIterationSubmission(Long userId, Long tenantId, DccControlledFileDO file,
                                                                   DccControlledFileMasterDO master) {
        return validateWorkingIterationSubmission(userId,tenantId,file,master,null);
    }
    private DccControlledFileDO validateWorkingIterationSubmission(Long userId,Long tenantId,DccControlledFileDO file,
                DccControlledFileMasterDO master,String formalIntent) {
        if (!DccControlledFileStatusEnum.WORKING.getStatus().equals(file.getStatus())
                || file.getCategoryId() == null || file.getDccProjectCodeId() == null
                || file.getMasterId() == null || file.getCheckedOutBy() != null
                || !(Objects.equals(file.getRequesterId(),userId) || "REPLACEMENT".equals(formalIntent)
                    && master!=null && master.getLatestControlledFileId()!=null && "REVISION".equals(file.getChangeType())
                    && projectAccessService.hasProjectOwner(userId,file.getDccProjectCodeId())
                    && assignmentScopeService.isWithinAssignedFileScope(userId,file.getId()))
                || !(DccControlledFileChangeTypeEnum.NEW.getCode().equals(file.getChangeType())
                || DccControlledFileChangeTypeEnum.REVISION.getCode().equals(file.getChangeType()))) {
            throw exception(CONTROLLED_FILE_ITERATION_SUBMIT_NOT_ALLOWED);
        }
        DccControlledFileVersionPolicy.VersionNumber targetVersion = resolveConfiguredVersion(file);
        List<DccControlledFileDO> masterVersions = controlledFileMapper.selectListByMasterIdForUpdate(file.getMasterId())
                .stream()
                .filter(Objects::nonNull)
                .toList();
        // The applicant selects the approval body. C verifies its exact formal baseline and freezes that version.
        assertIterationAdvancesCurrentActive(master, targetVersion, masterVersions);
        if (checkoutMapper.selectActiveByMasterId(tenantId, file.getMasterId()) != null) {
            throw exception(CONTROLLED_FILE_ITERATION_SUBMIT_NOT_ALLOWED);
        }
        DccControlledFileDO applicantReworkPredecessor = resolveApplicantReworkPredecessorForResubmission(file,
                masterVersions);
        boolean anotherOpenCandidate = masterVersions.stream()
                .filter(Objects::nonNull)
                .filter(item -> !Objects.equals(item.getId(), file.getId()))
                .filter(item -> applicantReworkPredecessor == null
                        || !Objects.equals(item.getId(), applicantReworkPredecessor.getId()))
                .anyMatch(item -> isUnfinishedWorkflowVersion(item)
                        && !DccControlledFileStatusEnum.WORKING.getStatus().equals(item.getStatus()));
        if (anotherOpenCandidate) {
            throw exception(CONTROLLED_FILE_WORKFLOW_IN_PROGRESS);
        }
        projectAccessService.assertProjectEditorOrOwner(userId, file.getDccProjectCodeId());
        if (!categoryPermissionSupport.hasCategoryPermission(file.getCategoryId(), userId,
                DccFileCategoryPermissionActionEnum.UPLOAD)) {
            throw exception(DCC_PROJECT_ACCESS_DENIED);
        }
        return applicantReworkPredecessor;
    }

    private DccControlledFileDO resolveApplicantReworkPredecessorForResubmission(DccControlledFileDO file,
                                                                                List<DccControlledFileDO> masterVersions) {
        if (file == null || file.getPredecessorControlledFileId() == null) {
            return null;
        }
        Map<Long, DccControlledFileDO> versionsById = masterVersions.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(DccControlledFileDO::getId, item -> item,
                        (left, right) -> left, LinkedHashMap::new));
        Set<Long> visitedIds = new LinkedHashSet<>();
        DccControlledFileDO candidate = versionsById.get(file.getPredecessorControlledFileId());
        while (candidate != null) {
            if (!visitedIds.add(candidate.getId())) {
                throw exception(CONTROLLED_FILE_ITERATION_SUBMIT_NOT_ALLOWED);
            }
            if (isApplicantReworkPredecessorForResubmission(file, candidate)) {
                return candidate;
            }
            if (!DccControlledFileStatusEnum.WORKING.getStatus().equals(candidate.getStatus())
                    || !isSameWorkingCorrectionInReworkChain(file, candidate)) {
                return null;
            }
            candidate = versionsById.get(candidate.getPredecessorControlledFileId());
        }
        return null;
    }

    private boolean isApplicantReworkPredecessorForResubmission(DccControlledFileDO file,
                                                               DccControlledFileDO predecessor) {
        if (file == null || predecessor == null) {
            return false;
        }
        if (!DccControlledFileStatusEnum.WORKING.getStatus().equals(file.getStatus())
                || !DccControlledFileStatusEnum.PENDING_APPLICANT_REWORK.getStatus().equals(predecessor.getStatus())
                || StrUtil.isBlank(predecessor.getProcessInstanceId())
                || !Objects.equals(file.getMasterId(), predecessor.getMasterId())
                || !Objects.equals(file.getRequesterId(), predecessor.getRequesterId())) {
            return false;
        }
        DccControlledFileVersionPolicy.VersionNumber fileVersion = resolveConfiguredVersion(file);
        DccControlledFileVersionPolicy.VersionNumber predecessorVersion = resolveConfiguredVersion(predecessor);
        return fileVersion.sameMajorIdentity(predecessorVersion) && fileVersion.compareTo(predecessorVersion) > 0;
    }

    private boolean isSameWorkingCorrectionInReworkChain(DccControlledFileDO file,
                                                        DccControlledFileDO candidate) {
        if (file == null || candidate == null || candidate.getPredecessorControlledFileId() == null
                || !Objects.equals(file.getMasterId(), candidate.getMasterId())
                || !Objects.equals(file.getRequesterId(), candidate.getRequesterId())) {
            return false;
        }
        DccControlledFileVersionPolicy.VersionNumber fileVersion = resolveConfiguredVersion(file);
        DccControlledFileVersionPolicy.VersionNumber candidateVersion = resolveConfiguredVersion(candidate);
        return fileVersion.sameMajorIdentity(candidateVersion) && fileVersion.compareTo(candidateVersion) > 0;
    }

    private void closeApplicantReworkPredecessorForResubmission(Long userId,
                                                               DccControlledFileDO predecessor,
                                                               DccControlledFileDO successor,
                                                               String successorProcessInstanceId) {
        if (predecessor == null) {
            return;
        }
        String reason = "申请人返工已生成修正版本 " + successor.getVersionNo()
                + "（ID " + successor.getId() + "）并重新送审，原退回流程终结；新流程 "
                + successorProcessInstanceId;
        bpmProcessInstanceService.cancelProcessInstanceByStartUser(userId,
                new BpmProcessInstanceCancelReqVO().setId(predecessor.getProcessInstanceId()).setReason(reason));
        if (controlledFileMapper.updateById(DccControlledFileDO.builder()
                .id(predecessor.getId())
                .status(DccControlledFileStatusEnum.WITHDRAWN.getStatus())
                .build()) != 1) {
            throw exception(CONTROLLED_FILE_ITERATION_SUBMIT_NOT_ALLOWED);
        }
        predecessor.setStatus(DccControlledFileStatusEnum.WITHDRAWN.getStatus());
        platformAdapter.recordWithdrawn(predecessor, userId, reason);
    }

    private void assertIterationAdvancesCurrentActive(DccControlledFileMasterDO master,
                                                      DccControlledFileVersionPolicy.VersionNumber targetVersion,
                                                      List<DccControlledFileDO> masterVersions) {
        Long currentActiveId = master == null ? null : master.getCurrentActiveControlledFileId();
        if (currentActiveId == null) {
            return;
        }
        DccControlledFileDO currentActive = masterVersions.stream()
                .filter(item -> Objects.equals(item.getId(), currentActiveId))
                .findFirst()
                .orElseThrow(() -> exception(CONTROLLED_FILE_ITERATION_SUBMIT_NOT_ALLOWED));
        DccControlledFileVersionPolicy.VersionNumber currentVersion = resolveConfiguredVersion(currentActive);
        if (targetVersion.compareTo(currentVersion) <= 0) {
            throw exception(CONTROLLED_FILE_ITERATION_SUBMIT_NOT_ALLOWED);
        }
    }

    private DccControlledFileVersionPolicy.VersionNumber resolveConfiguredVersion(DccControlledFileDO file) {
        DccControlledFileVersionPolicy.VersionNumber version = versionPolicy.parseStored(file);
        if (version == null) {
            throw exception(CONTROLLED_FILE_VERSION_INVALID);
        }
        return version;
    }

    private Long submitControlledFileIdempotently(Long userId, DccControlledFileSubmitReqVO reqVO,
                                                   String processDefinitionKey) {
        return submitControlledFileIdempotently(userId,reqVO,processDefinitionKey,null);
    }
    private Long submitControlledFileIdempotently(Long userId,DccControlledFileSubmitReqVO reqVO,String processDefinitionKey,DccDerivedUploadStorage storage) {
        String idempotencyKey = reqVO == null ? null : StrUtil.trim(reqVO.getIdempotencyKey());
        if (StrUtil.isBlank(idempotencyKey)) {
            throw exception(CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING);
        }
        reqVO.setIdempotencyKey(idempotencyKey);
        String payloadHash = DigestUtil.sha256Hex(JsonUtils.toJsonString(reqVO));
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        return submitMutex.execute(tenantId + ":" + userId + ":" + idempotencyKey, () -> {
            DccControlledFileDO existing = controlledFileMapper.selectBySubmitIdempotency(
                    tenantId, userId, idempotencyKey);
            if (existing != null) {
                return requireMatchingIdempotencyPayload(existing, payloadHash);
            }
            reqVO.setSubmitPayloadHash(payloadHash);
            try {
                return submitControlledFile(userId, reqVO, null, processDefinitionKey, true,storage);
            } catch (ControlledFileInsertConflict ex) {
                DccControlledFileDO winner = controlledFileMapper.selectBySubmitIdempotencyForUpdate(
                        tenantId, userId, idempotencyKey);
                if (winner != null) {
                    return requireMatchingIdempotencyPayload(winner, payloadHash);
                }
                throw ex.failure;
            }
        });
    }

    private Long requireMatchingIdempotencyPayload(DccControlledFileDO existing, String payloadHash) {
        if (!Objects.equals(existing.getSubmitPayloadHash(), payloadHash)) {
            throw exception(CONTROLLED_FILE_SUBMIT_IDEMPOTENCY_CONFLICT);
        }
        return existing.getId();
    }

    Long submitControlledFileWithProcessDefinitionKey(Long userId, DccControlledFileSubmitReqVO reqVO,
                                                      String processDefinitionKey) {
        return submitControlledFileIdempotently(userId, reqVO, processDefinitionKey);
    }

    private Long submitControlledFile(Long userId, DccControlledFileSubmitReqVO reqVO, Long ignoredControlledFileId,
                                      String processDefinitionKey, boolean requireUploadTickets) {
        return submitControlledFile(userId,reqVO,ignoredControlledFileId,processDefinitionKey,requireUploadTickets,null);
    }
    private Long submitControlledFile(Long userId,DccControlledFileSubmitReqVO reqVO,Long ignoredControlledFileId,String processDefinitionKey,boolean requireUploadTickets,DccDerivedUploadStorage storage) {
        rejectManualSignoffUsersForThreeWorkflow(processDefinitionKey, reqVO.getSelectedSignoffUserIds());
        PreparedSubmitContext context = prepareSubmitContext(userId, reqVO, true, true, ignoredControlledFileId,
                requireUploadTickets,storage);
        DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute resolvedRoute = routeReadinessService
                .evaluateDepartments(context.category().getId(), userId, reqVO.getSelectedSignoffDepartmentIds(),
                        DccControlledFileProcessDefinitionKeys.toActionType(processDefinitionKey))
                .requireReady();
        if (isThreeWorkflowUploadOrRevision(processDefinitionKey))
            workflowDatePolicy.requireReviewDate(reqVO.getEffectiveDate());
        String pendingStatus = toPendingStatus(resolveTaskStage(resolvedRoute.nodes().get(0).stageCode()));
        DccControlledFileDO file = insertControlledFile(context, userId,
                DccControlledFileStatusEnum.WORKING.getStatus(),
                processDefinitionKey, true, "SUBMISSION");
        validateAndBindRelatedFiles(userId, file.getId(),
                context.projectCode() == null ? null : context.projectCode().getId(),
                reqVO.getRelatedControlledFileIds());
        bindSubmitTickets(context, userId, file.getId());
        bindSubmitAttachments(context, file.getId());

        transitionNewCandidateStatus(file, pendingStatus);

        persistApprovalRouteSnapshots(file.getId(), resolvedRoute);
        persistDepartmentTaskAssigneeSnapshots(file.getId(), resolvedRoute);
        String processInstanceId = createApprovalProcess(userId, file, resolvedRoute, processDefinitionKey);
        if (controlledFileMapper.updateById(DccControlledFileDO.builder()
                .id(file.getId())
                .processInstanceId(processInstanceId)
                .build()) != 1) {
            throw exception(CONTROLLED_FILE_ITERATION_SUBMIT_NOT_ALLOWED);
        }
        file.setProcessInstanceId(processInstanceId);
        freezeApplicationAttributes(userId,file,reqVO.getProjectAttributes());
        platformAdapter.recordSubmitted(file, userId, processInstanceId);
        return file.getId();
    }

    private void persistApprovalRouteSnapshots(Long controlledFileId,
                                               DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute resolvedRoute) {
        for (DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode routeNode : resolvedRoute.nodes()) {
            if(routeSnapshotMapper.insert(DccControlledFileRouteSnapshotDO.builder()
                    .controlledFileId(controlledFileId)
                    .routeVersionNo(resolvedRoute.route().getVersionNo())
                    .stageNo(routeNode.stageNo())
                    .stageCode(routeNode.stageCode())
                    .stageName(routeNode.stageName())
                    .stageOrder(routeNode.stageOrder())
                    .candidateSourceType(routeNode.candidateSourceType())
                    .candidateSourceId(routeNode.candidateSourceId())
                    .candidateSourceIds(joinIds(routeNode.candidateSourceIds()))
                    .resolvedUserIds(routeNode.resolvedUserIds().stream().map(String::valueOf).collect(Collectors.joining(",")))
                    .approveMethod(routeNode.approveMethod())
                    .approveRatio(routeNode.approveRatio())
                    .requireAllApprovals(routeNode.requireAllApprovals())
                    .build())!=1) throw new IllegalStateException("审批路线冻结保存失败");
        }
    }

    void freezeApplicationAttributes(Long actorId,DccControlledFileDO file,
            cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes actual) {
        if(!isThreeWorkflowUploadOrRevision(file)) return;
        String type=DccControlledFileProcessDefinitionKeys.UPLOAD.equals(file.getProcessDefinitionKey())?"UPLOAD":"REVISION";
        var saved=projectApplicationSnapshots.readReservedDraft(actorId,file.getDccProjectCodeId(),type,file.getId());
        var actualValue=actual==null?projectAttributesService.readValue(saved.getActualAttributesJson()):actual;
        projectApplicationSnapshots.submitReservedDraft(actorId,file.getDccProjectCodeId(),type,file.getId(),
                saved.getApplicationRound(),file.getProcessInstanceId(),actualValue);
    }

    void prepareApplicationDraft(Long actorId,DccControlledFileDO file,
            cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes actual) {
        workingApplicationDraftInitializer.initialize(actorId,file.getPredecessorControlledFileId(),file.getId(),actual);
    }

    private void lockDraftProject(Long projectId) {
        var project=projectId==null?null:projectCodeMapper.selectByIdForUpdate(projectId);
        if(project==null || !Objects.equals(project.getTenantId(),TenantContextHolder.getRequiredTenantId())
                || !"ENABLE".equals(project.getStatus())) throw exception(CONTROLLED_FILE_ITERATION_SUBMIT_NOT_ALLOWED);
    }

    private void persistDepartmentTaskAssigneeSnapshots(Long controlledFileId,
                                                        DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute resolvedRoute) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        List<Long> departmentIds = resolvedRoute.nodes().stream()
                .filter(node -> "DEPT".equalsIgnoreCase(node.candidateSourceType()))
                .flatMap(node -> node.candidateSourceIds().stream())
                .distinct()
                .toList();
        List<Long> allAssigneeUserIds = resolvedRoute.nodes().stream()
                .filter(node -> "DEPT".equalsIgnoreCase(node.candidateSourceType()))
                .flatMap(node -> node.resolvedUserIds().stream())
                .distinct()
                .toList();
        Map<Long, String> departmentNames = resolveDepartmentSnapshotNames(departmentIds);
        Map<Long, String> assigneeNames = resolveAssigneeSnapshotNames(allAssigneeUserIds);
        for (DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode routeNode : resolvedRoute.nodes()) {
            if (!"DEPT".equalsIgnoreCase(routeNode.candidateSourceType())) {
                continue;
            }
            List<Long> routeDepartmentIds = routeNode.candidateSourceIds();
            List<Long> assigneeUserIds = routeNode.resolvedUserIds();
            if (routeDepartmentIds.size() != assigneeUserIds.size()) {
                throw exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
            }
            for (int index = 0; index < routeDepartmentIds.size(); index++) {
                Long departmentId = routeDepartmentIds.get(index);
                if(taskAssigneeSnapshotMapper.insert(DccControlledFileTaskAssigneeSnapshotDO.builder()
                        .controlledFileId(controlledFileId)
                        .stageCode(routeNode.stageCode())
                        .stageNo(routeNode.stageNo())
                        .departmentId(departmentId)
                        .departmentName(departmentNames.get(departmentId))
                        .assigneeUserId(assigneeUserIds.get(index))
                        .leaderUserId(assigneeUserIds.get(index))
                        .assigneeName(assigneeNames.get(assigneeUserIds.get(index)))
                        .leaderConfigDigest("leaderUserId@" + departmentId + "=" + assigneeUserIds.get(index))
                        .obligationId(controlledFileId + ":" + routeNode.stageCode() + ":" + departmentId)
                        .tenantId(tenantId)
                        .build())!=1) throw new IllegalStateException("会签部门义务冻结保存失败");
            }
        }
    }

    private Map<Long, String> resolveDepartmentSnapshotNames(List<Long> departmentIds) {
        if (departmentIds.isEmpty()) {
            return Map.of();
        }
        List<DeptRespDTO> departments = deptApi.getDeptList(departmentIds);
        if (departments == null) {
            throw exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
        }
        Map<Long, String> names = departments.stream()
                .filter(Objects::nonNull)
                .filter(department -> department.getId() != null)
                .filter(department -> StrUtil.isNotBlank(department.getName()))
                .collect(Collectors.toMap(DeptRespDTO::getId, department -> StrUtil.trim(department.getName()),
                        (left, right) -> left, LinkedHashMap::new));
        if (names.size() != departmentIds.size() || departmentIds.stream().anyMatch(id -> !names.containsKey(id))) {
            throw exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
        }
        return names;
    }

    private Map<Long, String> resolveAssigneeSnapshotNames(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        List<AdminUserRespDTO> users = adminUserApi.getUserList(userIds);
        if (users == null) {
            throw exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
        }
        Map<Long, String> names = users.stream()
                .filter(Objects::nonNull)
                .filter(user -> user.getId() != null)
                .filter(user -> StrUtil.isNotBlank(user.getNickname()))
                .collect(Collectors.toMap(AdminUserRespDTO::getId, user -> StrUtil.trim(user.getNickname()),
                        (left, right) -> left, LinkedHashMap::new));
        if (names.size() != userIds.size() || userIds.stream().anyMatch(id -> !names.containsKey(id))) {
            throw exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
        }
        return names;
    }

    private String createApprovalProcess(Long userId, DccControlledFileDO file,
                                         DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute resolvedRoute,
                                         String processDefinitionKey) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("controlledFileId", file.getId());
        variables.put("categoryId", file.getCategoryId());
        variables.put("directoryId", file.getDirectoryId());
        variables.put("needTraining", Boolean.TRUE.equals(file.getNeedTraining()));
        Map<String, List<Long>> startUserSelectAssignees = approvalRouteAssigneeResolver.buildStartUserSelectAssigneeMap(resolvedRoute.nodes());
        variables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_START_USER_SELECT_ASSIGNEES,
                startUserSelectAssignees);
        variables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_APPROVE_USER_SELECT_ASSIGNEES,
                approvalRouteAssigneeResolver.buildApproveUserSelectAssigneeMap(resolvedRoute.nodes()));
        Map<String, List<String>> dccTaskObligationIds = buildDccTaskObligationIdMap(file.getId(), resolvedRoute);
        if (!dccTaskObligationIds.isEmpty()) {
            variables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_DCC_TASK_OBLIGATION_IDS,
                    dccTaskObligationIds);
        }
        return bpmProcessInstanceApi.createProcessInstance(userId, new BpmProcessInstanceCreateReqDTO()
                .setProcessDefinitionKey(processDefinitionKey)
                .setBusinessKey(String.valueOf(file.getId()))
                .setStartUserSelectAssignees(startUserSelectAssignees)
                .setVariables(variables));
    }

    private Map<String, List<String>> buildDccTaskObligationIdMap(Long controlledFileId,
            DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute resolvedRoute) {
        Map<String, List<String>> obligationIdsByStage = new LinkedHashMap<>();
        for (DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode routeNode : resolvedRoute.nodes()) {
            if (!"DEPT".equalsIgnoreCase(routeNode.candidateSourceType())) {
                continue;
            }
            List<Long> departmentIds = routeNode.candidateSourceIds();
            if (departmentIds.size() != routeNode.resolvedUserIds().size()) {
                throw exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
            }
            obligationIdsByStage.put(routeNode.stageCode(), departmentIds.stream()
                    .map(departmentId -> controlledFileId + ":" + routeNode.stageCode() + ":" + departmentId)
                    .toList());
        }
        return obligationIdsByStage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitControlledFileWithoutApproval(Long userId, DccControlledFileSubmitReqVO reqVO) {
        return submitControlledFileWithoutApprovalInternal(userId, reqVO, null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitControlledFileWithoutApproval(Long userId, DccControlledFileSubmitReqVO reqVO,
                                                    String approvalProcessInstanceId, String platformEventKey) {
        return submitControlledFileWithoutApprovalInternal(userId, reqVO, approvalProcessInstanceId, platformEventKey);
    }

    private Long submitControlledFileWithoutApprovalInternal(Long userId, DccControlledFileSubmitReqVO reqVO,
                                                            String approvalProcessInstanceId,
                                                            String platformEventKey) {
        replaceExistingVersionOneForNasTransfer(reqVO);
        PreparedSubmitContext context = prepareSubmitContext(userId, reqVO, false, false, null, false);
        String normalizedProcessInstanceId = StrUtil.trim(approvalProcessInstanceId);
        if (StrUtil.isBlank(normalizedProcessInstanceId)) {
            normalizedProcessInstanceId = null;
        }
        boolean splitRevisionApproval = normalizedProcessInstanceId != null
                && context.changeType() == DccControlledFileChangeTypeEnum.REVISION;
        String finalizationStatus = splitRevisionApproval
                        ? DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus()
                        : DccControlledFileStatusEnum.FINALIZING.getStatus();
        DccControlledFileDO file = insertControlledFile(context, userId,
                DccControlledFileStatusEnum.WORKING.getStatus(),
                null, true, "SUBMISSION");
        validateAndBindRelatedFiles(userId, file.getId(),
                context.projectCode() == null ? null : context.projectCode().getId(),
                reqVO.getRelatedControlledFileIds());
        if (normalizedProcessInstanceId != null) {
            controlledFileMapper.updateById(DccControlledFileDO.builder()
                    .id(file.getId())
                    .processInstanceId(normalizedProcessInstanceId)
                    .build());
            file.setProcessInstanceId(normalizedProcessInstanceId);
        }
        bindSubmitTickets(context, userId, file.getId());
        bindSubmitAttachments(context, file.getId());
        transitionNewCandidateStatus(file, finalizationStatus);
        String normalizedEventKey = StrUtil.blankToDefault(platformEventKey,
                "dcc-upload-without-approval:" + file.getId());
        if (splitRevisionApproval) {
            platformAdapter.recordApprovedUploadReadyToPublish(file, userId, normalizedProcessInstanceId,
                    normalizedEventKey);
            return file.getId();
        }
        platformAdapter.recordApprovedUploadFinalizationStarted(file, userId, normalizedProcessInstanceId,
                normalizedEventKey);
        finalizationService.activateWithoutApproval(file.getId(), true);
        return file.getId();
    }

    @Override
    public PageResult<DccControlledFileRespVO> getControlledFilePage(Long userId, DccControlledFilePageReqVO reqVO) {
        Set<Long> requestedDirectoryIds = resolveRequestedDirectoryIds(reqVO);
        PageResult<DccControlledFileDO> pageResult;
        if (directoryAccessPermissionService.hasDirectoryManagementPermission(userId)
                || (reqVO.getRequesterId() != null && reqVO.getRequesterId().equals(userId))) {
            if (requestedDirectoryIds != null) {
                if (requestedDirectoryIds.isEmpty()) {
                    return PageResult.empty(0L);
                }
                pageResult = controlledFileMapper.selectWorkflowPage(buildPageReqWithoutDirectory(reqVO), requestedDirectoryIds);
            } else {
                pageResult = controlledFileMapper.selectWorkflowPage(reqVO);
            }
        } else {
            java.util.Set<Long> visibleDirectoryIds = directoryAccessPermissionService.getAuthorizedDirectoryIds(userId, DccAccessTypeEnum.QUERY);
            if (visibleDirectoryIds.isEmpty()) {
                return PageResult.empty(0L);
            }
            if (requestedDirectoryIds != null) {
                Set<Long> effectiveDirectoryIds = requestedDirectoryIds.stream()
                        .filter(visibleDirectoryIds::contains)
                        .collect(Collectors.toSet());
                if (effectiveDirectoryIds.isEmpty()) {
                    return PageResult.empty(0L);
                }
                pageResult = controlledFileMapper.selectWorkflowPage(buildPageReqWithoutDirectory(reqVO), effectiveDirectoryIds);
            } else {
                if (reqVO.getDirectoryId() != null && !visibleDirectoryIds.contains(reqVO.getDirectoryId())) {
                    return PageResult.empty(0L);
                }
                pageResult = controlledFileMapper.selectWorkflowPage(reqVO, visibleDirectoryIds);
            }
        }
        return new PageResult<>(convertList(pageResult.getList(), this::toRespVO), pageResult.getTotal());
    }

    @Override
    public DccControlledFileRespVO getControlledFile(Long id) {
        DccControlledFileDO file = controlledFileMapper.selectById(id);
        if (file == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        DccControlledFileRespVO respVO = toRespVO(file);
        respVO.setRouteSnapshots(convertList(routeSnapshotMapper.selectListByControlledFileId(id), this::toSnapshotRespVO));
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawControlledFile(Long userId, Long id, DccControlledFileWithdrawReqVO reqVO) {
        DccControlledFileDO file = controlledFileMapper.selectById(id);
        if (file == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        file=lockNativeApplicantFile(file,id);
        if (!userId.equals(file.getRequesterId())
                || !WITHDRAW_ALLOWED_STATUSES.contains(file.getStatus())
                || StrUtil.isBlank(file.getProcessInstanceId())) {
            throw exception(CONTROLLED_FILE_WITHDRAW_NOT_ALLOWED);
        }
        bpmProcessInstanceService.cancelProcessInstanceByStartUser(userId,
                new BpmProcessInstanceCancelReqVO().setId(file.getProcessInstanceId()).setReason(reqVO.getReason()));
        if(controlledFileMapper.updateById(DccControlledFileDO.builder()
                .id(file.getId())
                .status(DccControlledFileStatusEnum.WITHDRAWN.getStatus())
                .rejectReason(reqVO.getReason())
                .build())!=1) throw new IllegalStateException("撤回状态保存失败");
        platformAdapter.recordWithdrawn(file, userId, reqVO.getReason());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWithdrawnControlledFile(Long userId, Long id) {
        DccControlledFileDO file = controlledFileMapper.selectById(id);
        file=lockNativeApplicantFile(file,id);
        validateWithdrawnApplicantAction(userId, file);
        List<DccControlledFileDO> successors = controlledFileMapper.selectListByPredecessorControlledFileId(id);
        if (successors != null && !successors.isEmpty()) {
            throw exception(CONTROLLED_FILE_WITHDRAWN_ACTION_NOT_ALLOWED);
        }
        controlledFileMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long resubmitWithdrawnControlledFile(Long userId, Long id) {
        DccControlledFileDO file = controlledFileMapper.selectById(id);
        validateWithdrawnApplicantAction(userId,file);
        if(file!=null) lockDraftProject(file.getDccProjectCodeId());
        file=lockNativeApplicantFile(file,id);
        validateWithdrawnApplicantAction(userId, file);
        projectAccessService.assertProjectEditorOrOwner(userId, file.getDccProjectCodeId());
        if (!categoryPermissionSupport.hasCategoryPermission(file.getCategoryId(), userId,
                DccFileCategoryPermissionActionEnum.UPLOAD)) {
            throw exception(DCC_PROJECT_ACCESS_DENIED);
        }
        String processKey = isThreeWorkflowUploadOrRevision(file) ? file.getProcessDefinitionKey() : BPM_PROCESS_DEFINITION_KEY;
        Long newFileId = submitControlledFile(userId, toResubmitReqVO(file), id, processKey, false);
        controlledFileMapper.updateById(DccControlledFileDO.builder()
                .id(file.getId())
                .supersededByFileId(newFileId)
                .build());
        platformAdapter.recordResubmitted(file, newFileId);
        return newFileId;
    }

    private DccControlledFileDO lockNativeApplicantFile(DccControlledFileDO identity,Long id) {
        if(!isThreeWorkflowUploadOrRevision(identity)) return identity;
        Long tenant=TenantContextHolder.getRequiredTenantId();
        if(!Objects.equals(identity.getId(),id) || !Objects.equals(identity.getTenantId(),tenant) || identity.getMasterId()==null)
            throw exception(CONTROLLED_FILE_WITHDRAW_NOT_ALLOWED);
        var master=controlledFileMasterMapper.selectByIdForUpdate(identity.getMasterId());
        var locked=controlledFileMapper.selectByIdAndTenantForUpdate(tenant,id);
        if(master==null || !Objects.equals(master.getTenantId(),tenant) || locked==null
                || !Objects.equals(locked.getId(),id) || !Objects.equals(locked.getTenantId(),tenant)
                || !Objects.equals(locked.getMasterId(),master.getId())
                || !Objects.equals(locked.getProcessDefinitionKey(),identity.getProcessDefinitionKey()))
            throw exception(CONTROLLED_FILE_WITHDRAW_NOT_ALLOWED);
        return locked;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadTrainingRecord(Long userId, Long id, DccControlledFileTrainingRecordReqVO reqVO) {
        DccControlledFileDO identity=controlledFileMapper.selectById(id);
        DccControlledFileDO file=isThreeWorkflowUploadOrRevision(identity)?lockNativeApplicantFile(identity,id)
                :controlledFileMapper.selectByIdAndTenantForUpdate(TenantContextHolder.getRequiredTenantId(),id);
        if (file == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        if (!permissionApi.hasAnyRoles(userId,"doc_control")
                || !categoryPermissionSupport.hasCategoryPermission(file.getCategoryId(),userId,DccFileCategoryPermissionActionEnum.APPROVE)
                || !Boolean.TRUE.equals(file.getNeedTraining())
                || file.getTrainingRecordFileId() != null
                || file.getPublishedFileId() != null
                || !DccControlledFileStatusEnum.PENDING_APPLICANT_TRAINING_RECORD.getStatus().equals(file.getStatus())) {
            throw exception(CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED);
        }
        if (reqVO == null) {
            throw exception(CONTROLLED_FILE_TRAINING_RECORD_REQUIRED);
        }
        if(isThreeWorkflowUploadOrRevision(file)) requireNativeTrainingContext(userId,file,reqVO.getSessionId());
        DccUploadTicketBoundFile trainingRecord = uploadTicketService.resolveForBinding(
                new DccUploadTicketResolveCommand(reqVO.getTrainingRecordUploadTicket(), userId, file.getCategoryId(),
                        reqVO.getSessionId(),
                        DccControlledFileUploadTypePolicy.PURPOSE_TRAINING_RECORD));
        if (trainingRecord == null || trainingRecord.storageFileId() == null) {
            throw exception(CONTROLLED_FILE_UPLOAD_TICKET_INVALID);
        }
        uploadTicketService.markBound(new DccUploadTicketMarkBoundCommand(reqVO.getTrainingRecordUploadTicket(), userId,
                file.getCategoryId(), reqVO.getSessionId(),
                DccControlledFileUploadTypePolicy.PURPOSE_TRAINING_RECORD, id));
        boolean threeWorkflowUploadOrRevision = isThreeWorkflowUploadOrRevision(file);
        if(controlledFileMapper.updateById(DccControlledFileDO.builder()
                .id(id)
                .trainingRecordFileId(trainingRecord.storageFileId())
                .status(threeWorkflowUploadOrRevision
                        ? DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus()
                        : DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL.getStatus())
                .build())!=1) throw new IllegalStateException("培训记录保存失败");
        if (threeWorkflowUploadOrRevision
                && !bpmTaskService.triggerTask(file.getProcessInstanceId(), "TRAINING")) {
            throw new IllegalStateException("DCC training BPM task was not triggered");
        }
    }

    @Override
    public void validateTrainingRecordUpload(Long userId,Long fileId,Long categoryId,String sessionId) {
        var file=fileId==null?null:controlledFileMapper.selectById(fileId);
        if(file==null || !isThreeWorkflowUploadOrRevision(file) || !Objects.equals(file.getId(),fileId)
                || !Objects.equals(file.getTenantId(),TenantContextHolder.getRequiredTenantId())
                || !Objects.equals(file.getCategoryId(),categoryId) || !Boolean.TRUE.equals(file.getNeedTraining())
                || !"PENDING_APPLICANT_TRAINING_RECORD".equals(file.getStatus()) || file.getTrainingRecordFileId()!=null
                || file.getPublishedFileId()!=null || !permissionApi.hasAnyRoles(userId,"doc_control")
                || !categoryPermissionSupport.hasCategoryPermission(categoryId,userId,DccFileCategoryPermissionActionEnum.APPROVE))
            throw exception(CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED);
        requireNativeTrainingContext(userId,file,sessionId);
    }

    private void requireNativeTrainingContext(Long userId,DccControlledFileDO file,String sessionId) {
        offlineTraining.requireUpload(userId,file);
        String round=file.getProcessInstanceId();
        if(StrUtil.isBlank(round) || versionPolicy.parseStored(file)==null)
            throw exception(CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED);
        String prefix="dcc-training:"+file.getId()+":"+round.length()+":"+round+":";
        if(sessionId==null || sessionId.length()>128 || !sessionId.equals(sessionId.trim())
                || !sessionId.startsWith(prefix) || StrUtil.isBlank(sessionId.substring(prefix.length())))
            throw exception(CONTROLLED_FILE_UPLOAD_TICKET_INVALID);
        var process=bpmProcessInstanceService.getProcessInstance(round);
        var variables=process==null?null:process.getProcessVariables();
        if(process==null || !round.equals(process.getId())
                || !String.valueOf(TenantContextHolder.getRequiredTenantId()).equals(process.getTenantId())
                || !Objects.equals(file.getProcessDefinitionKey(),process.getProcessDefinitionKey())
                || !String.valueOf(file.getId()).equals(process.getBusinessKey()) || variables==null
                || !String.valueOf(file.getId()).equals(String.valueOf(variables.get("controlledFileId"))))
            throw exception(CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DccSignatureActionRespVO approveTask(Long userId, Long id, DccControlledFileApproveTaskReqVO reqVO) {
        Task actual = bpmTaskService.getTask(reqVO.getTaskId());
        if (actual != null && signoffAssignmentService.isObsoleteProcessForFile(actual,id)) {
            if(reqVO.getFileOwnerUserId()!=null) throw new IllegalArgumentException("独立作废流程不选择文件负责人");
            var signature = signoffAssignmentService.reviewObsolete(userId,id,reqVO.getTaskId(),
                    reqVO.getPassword(),reqVO.getReason(),true);
            return buildActionRespVO(signature,"APPROVED",resolveLatestStatus(controlledFileMapper.selectById(id)));
        }
        return approveTaskWithProcessDefinitionKey(userId, id, reqVO,
                resolveNativeProcessDefinitionKey(id), true);
    }

    @Override
    public DccControlledFileTaskReadinessRespVO getTaskActionReadiness(Long userId, Long id,
                                                                      DccControlledFileTaskReadinessReqVO reqVO) {
        Task actual = bpmTaskService.getTask(reqVO.getTaskId());
        if (actual != null && signoffAssignmentService.isObsoleteProcessForFile(actual, id)) {
            signoffAssignmentService.requireObsoleteReviewReady(userId, id, reqVO.getTaskId());
            return new DccControlledFileTaskReadinessRespVO().setReady(true)
                    .setFinalApproval(false).setBlockers(List.of());
        }
        ValidatedTaskActionContext context = validateTaskAction(userId, id, reqVO.getTaskId(),
                resolveNativeProcessDefinitionKey(id), "APPROVE");
        return evaluateDocControlApprovalReadiness(userId, context, reqVO.getSessionId(),
                reqVO.getStampedPdfUploadTicket(), reqVO.getConfirmedDirectoryId(),
                reqVO.getSelectedDistributionScopes(), null, null, true).readiness();
    }

    DccSignatureActionRespVO approveTaskWithProcessDefinitionKey(Long userId, Long id, DccControlledFileApproveTaskReqVO reqVO,
                                                                 String processDefinitionKey, boolean collectDocControlArtifacts) {
        ValidatedTaskActionContext context = validateTaskAction(userId, id, reqVO.getTaskId(), processDefinitionKey,
                "APPROVE");
        boolean selectsFileOwner=isThreeWorkflowUploadOrRevision(context.file())
                && context.stageCode()==DccControlledFileStageCodeEnum.MATRIX_APPROVAL;
        if(!selectsFileOwner && reqVO.getFileOwnerUserId()!=null)
            throw new IllegalArgumentException("只有上传、升版批准节点可选择文件负责人");
        DccApprovalFileOwnerSelectionService.Selection selectedFileOwner=selectsFileOwner
                ? Objects.requireNonNull(fileOwnerSelection.prepare(context.file(),context.stageCode(),reqVO.getFileOwnerUserId()),"required file owner selection") : null;
        String signedReason=selectedFileOwner==null ? reqVO.getReason() : fileOwnerSelection.signedReason(reqVO.getReason(),selectedFileOwner);
        if (isThreeWorkflowUploadOrRevision(context.file())
                && context.stageCode() == DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW) {
            workflowDatePolicy.requireReviewDate(context.file().getEffectiveDate());
            if (Boolean.TRUE.equals(context.file().getNeedTraining()) && context.file().getTrainingRecordFileId() == null)
                throw exception(CONTROLLED_FILE_TRAINING_RECORD_REQUIRED);
        }
        DocControlApprovalReadinessEvaluation readinessEvaluation = evaluateDocControlApprovalReadiness(userId, context,
                reqVO.getSessionId(), reqVO.getStampedPdfUploadTicket(), reqVO.getConfirmedDirectoryId(),
                reqVO.getSelectedDistributionScopes(), reqVO.getStampedPdfFileId(), reqVO.getTrainingRecordFileId(),
                collectDocControlArtifacts);
        readinessEvaluation.requireReady();
        DocControlApprovalArtifacts docControlArtifacts = readinessEvaluation.artifacts();
        Set<String> beforeRunningTaskIds = bpmTaskService.getRunningTaskListByProcessInstanceId(
                        context.file().getProcessInstanceId(), null, null)
                .stream()
                .map(Task::getId)
                .collect(Collectors.toSet());
        DccUnifiedSignatureResult signature = isThreeWorkflowUploadOrRevision(context.file())
                ? signatureVerificationService.verifyPasswordAndCreateWorkflowSignature(userId,id,reqVO.getTaskId(),
                    context.file().getProcessInstanceId(),context.stageCode().getCode(),"APPROVE",reqVO.getPassword(),signedReason)
                : signatureVerificationService.verifyPasswordAndCreateSignature(userId,id,reqVO.getTaskId(),
                    context.stageCode().getCode(),"APPROVE",reqVO.getPassword(),signedReason);
        if(selectedFileOwner!=null)fileOwnerSelection.bind(context.file(),reqVO.getTaskId(),userId,signature.getSignatureId(),selectedFileOwner,signedReason);
        if (isThreeWorkflowUploadOrRevision(context.file()))
            signoffAssignmentService.authorizeSignedAction(reqVO.getTaskId(),userId,"APPROVE",signature);
        if (docControlArtifacts != null) {
            signatureBindingService.bindPublishedCopy(context.file(), docControlArtifacts.stampedPdfFileId(), userId,
                    "dcc-final-approval:" + id + ":" + reqVO.getTaskId());
        }
        if (collectDocControlArtifacts) {
            persistDocControlApprovalArtifacts(userId, context, docControlArtifacts);
        }
        List<DccControlledFileRouteSnapshotDO> routeSnapshots = routeSnapshotMapper.selectListByControlledFileId(id);
        bpmTaskService.approveTask(userId, new BpmTaskApproveReqVO()
                .setId(reqVO.getTaskId())
                .setReason(reqVO.getReason())
                .setNextAssignees(buildStageAssigneeMapFromSnapshots(routeSnapshots)));
        String nextStatus = syncStatusAfterApprove(context.file(), context.stageCode(), beforeRunningTaskIds,
                reqVO.getTaskId(), context.taskDefinitionKey());
        return buildActionRespVO(signature, "APPROVED", nextStatus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DccSignatureActionRespVO rejectTask(Long userId, Long id, DccControlledFileRejectTaskReqVO reqVO) {
        Task actual = bpmTaskService.getTask(reqVO.getTaskId());
        if (actual != null && signoffAssignmentService.isObsoleteProcessForFile(actual,id)) {
            var signature = signoffAssignmentService.reviewObsolete(userId,id,reqVO.getTaskId(),
                    reqVO.getPassword(),reqVO.getReason(),false);
            return buildActionRespVO(signature,"REJECTED",controlledFileMapper.selectById(id).getStatus());
        }
        return rejectTaskWithProcessDefinitionKey(userId, id, reqVO,
                resolveNativeProcessDefinitionKey(id));
    }

    DccSignatureActionRespVO rejectTaskWithProcessDefinitionKey(Long userId, Long id, DccControlledFileRejectTaskReqVO reqVO,
                                                                String processDefinitionKey) {
        ValidatedTaskActionContext context = validateTaskAction(userId, id, reqVO.getTaskId(), processDefinitionKey,
                "REJECT");
        DccUnifiedSignatureResult signature = isThreeWorkflowUploadOrRevision(context.file())
                ? signatureVerificationService.verifyPasswordAndCreateWorkflowSignature(userId,id,reqVO.getTaskId(),
                    context.file().getProcessInstanceId(),context.stageCode().getCode(),"REJECT",reqVO.getPassword(),reqVO.getReason())
                : signatureVerificationService.verifyPasswordAndCreateSignature(userId,id,reqVO.getTaskId(),
                    context.stageCode().getCode(),"REJECT",reqVO.getPassword(),reqVO.getReason());
        if (isThreeWorkflowUploadOrRevision(context.file()))
            signoffAssignmentService.authorizeSignedAction(reqVO.getTaskId(),userId,"REJECT",signature);
        bpmTaskService.rejectTask(userId, new BpmTaskRejectReqVO()
                .setId(reqVO.getTaskId())
                .setReason(reqVO.getReason()));
        controlledFileMapper.updateById(DccControlledFileDO.builder()
                .id(id)
                .status(DccControlledFileStatusEnum.REJECTED.getStatus())
                .rejectedTime(LocalDateTime.now())
                .rejectReason(reqVO.getReason())
                .build());
        return buildActionRespVO(signature, "REJECTED", DccControlledFileStatusEnum.REJECTED.getStatus());
    }

    private DccControlledFileSignatureDO requireActionSignature(Long controlledFileId, String taskId,
                                                                Long actorId, String actionType) {
        DccControlledFileSignatureDO signature =
                signatureMapper.selectActionSignature(controlledFileId, taskId, actorId, actionType);
        if (signature == null) {
            throw exception(CONTROLLED_FILE_SIGNATURE_PERSIST_FAILED);
        }
        return signature;
    }

    private DccSignatureActionRespVO buildActionRespVO(DccControlledFileSignatureDO signature,
                                                       String taskActionResult,
                                                       String nextStatus) {
        if (signature.getId() == null
                || signature.getControlledFileId() == null
                || signature.getRevisionId() == null
                || StrUtil.isBlank(signature.getVersionNo())
                || StrUtil.isBlank(signature.getMeaningCode())
                || StrUtil.isBlank(signature.getControlledCopyHashStatus())
                || StrUtil.isBlank(signature.getEvidenceHash())
                || signature.getSignedAt() == null
                || StrUtil.isBlank(nextStatus)) {
            throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        }
        if (!"VALID".equals(signature.getEvidenceStatus())) {
            throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_INVALID);
        }
        DccSignatureActionRespVO respVO = new DccSignatureActionRespVO();
        respVO.setTaskActionResult(taskActionResult);
        respVO.setSignatureId(signature.getId());
        respVO.setControlledFileId(signature.getControlledFileId());
        respVO.setRevisionId(signature.getRevisionId());
        respVO.setVersionNo(signature.getVersionNo());
        respVO.setMeaningCode(signature.getMeaningCode());
        respVO.setControlledCopyHashStatus(signature.getControlledCopyHashStatus());
        respVO.setEvidenceStatus(signature.getEvidenceStatus());
        respVO.setEvidenceHashShort(shortHash(signature.getEvidenceHash()));
        respVO.setSignedAt(signature.getSignedAt());
        respVO.setNextStatus(nextStatus);
        return respVO;
    }

    private DccSignatureActionRespVO buildActionRespVO(DccUnifiedSignatureResult signature,
                                                       String taskActionResult,
                                                       String nextStatus) {
        if (signature.getSignatureId() == null
                || signature.getControlledFileId() == null
                || signature.getRevisionId() == null
                || StrUtil.isBlank(signature.getVersionNo())
                || StrUtil.isBlank(signature.getMeaningCode())
                || StrUtil.isBlank(signature.getControlledCopyHashStatus())
                || StrUtil.isBlank(signature.getEvidenceHash())
                || signature.getSignedAt() == null
                || StrUtil.isBlank(nextStatus)) {
            throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        }
        if (!"VALID".equals(signature.getEvidenceStatus())) {
            throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_INVALID);
        }
        DccSignatureActionRespVO respVO = new DccSignatureActionRespVO();
        respVO.setTaskActionResult(taskActionResult);
        respVO.setSignatureId(signature.getSignatureId());
        respVO.setControlledFileId(signature.getControlledFileId());
        respVO.setRevisionId(signature.getRevisionId());
        respVO.setVersionNo(signature.getVersionNo());
        respVO.setMeaningCode(signature.getMeaningCode());
        respVO.setControlledCopyHashStatus(signature.getControlledCopyHashStatus());
        respVO.setEvidenceStatus(signature.getEvidenceStatus());
        respVO.setEvidenceHashShort(shortHash(signature.getEvidenceHash()));
        respVO.setSignedAt(signature.getSignedAt());
        respVO.setNextStatus(nextStatus);
        return respVO;
    }

    private String shortHash(String hash) {
        if (StrUtil.isBlank(hash) || hash.length() < 12) {
            throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        }
        return hash.substring(0, 12).toLowerCase();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnTask(Long userId, Long id, DccControlledFileReturnTaskReqVO reqVO) {
        throw exception(CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED);
    }

    void returnTaskWithProcessDefinitionKey(Long userId, Long id, DccControlledFileReturnTaskReqVO reqVO,
                                            String processDefinitionKey) {
        ValidatedTaskActionContext context = validateTaskAction(userId, id, reqVO.getTaskId(), processDefinitionKey,
                "RETURN");
        ValidatedReturnTarget target = validateReturnTarget(context, reqVO.getTargetTaskDefinitionKey());
        signatureVerificationService.verifyPasswordAndCreateSignature(userId, id, reqVO.getTaskId(),
                context.stageCode().getCode(), "RETURN", reqVO.getPassword(), reqVO.getReason());
        bpmTaskService.returnTask(userId, new BpmTaskReturnReqVO()
                .setId(reqVO.getTaskId())
                .setTargetTaskDefinitionKey(target.taskDefinitionKey())
                .setReason(reqVO.getReason()));
        controlledFileMapper.updateById(DccControlledFileDO.builder()
                .id(id)
                .status(target.pendingStatus())
                .rejectReason("有流程回退，需处理：" + reqVO.getReason())
                .build());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferTask(Long userId, Long id, DccControlledFileTransferTaskReqVO reqVO) {
        throw exception(CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED);
    }

    void transferTaskWithProcessDefinitionKey(Long userId, Long id, DccControlledFileTransferTaskReqVO reqVO,
                                              String processDefinitionKey) {
        ValidatedTaskActionContext context = validateTaskAction(userId, id, reqVO.getTaskId(), processDefinitionKey,
                "TRANSFER");
        requireExistingUsers(List.of(reqVO.getAssigneeUserId()));
        routeReadinessService.requireReadyParticipants(context.stageCode().getCode(), List.of(reqVO.getAssigneeUserId()));
        signatureVerificationService.verifyPasswordAndCreateSignature(userId, id, reqVO.getTaskId(),
                context.stageCode().getCode(), "TRANSFER", reqVO.getPassword(), reqVO.getReason());
        bpmTaskService.transferTask(userId, new BpmTaskTransferReqVO()
                .setId(reqVO.getTaskId())
                .setAssigneeUserId(reqVO.getAssigneeUserId())
                .setReason(reqVO.getReason()));
        updateStageResolvedUsers(context.stageSnapshot(), replaceCurrentUserWithAssignee(
                parseResolvedUserIdsInOrder(context.stageSnapshot()), userId, reqVO.getAssigneeUserId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createSignTask(Long userId, Long id, DccControlledFileCreateSignTaskReqVO reqVO) {
        throw exception(CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED);
    }

    @Override
    public void assignSignoff(Long userId, Long id, cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccSignoffAssignmentReqVO request) {
        signoffAssignmentService.assign(userId,id,request);
    }

    void createSignTaskWithProcessDefinitionKey(Long userId, Long id, DccControlledFileCreateSignTaskReqVO reqVO,
                                                String processDefinitionKey) {
        ValidatedTaskActionContext context = validateTaskAction(userId, id, reqVO.getTaskId(), processDefinitionKey,
                "ADD_SIGN");
        if (BpmTaskSignTypeEnum.of(reqVO.getType()) == null) {
            throw exception(CONTROLLED_FILE_TASK_TARGET_INVALID);
        }
        requireExistingUsers(reqVO.getUserIds());
        routeReadinessService.requireReadyParticipants(context.stageCode().getCode(), new ArrayList<>(reqVO.getUserIds()));
        signatureVerificationService.verifyPasswordAndCreateSignature(userId, id, reqVO.getTaskId(),
                context.stageCode().getCode(), "ADD_SIGN", reqVO.getPassword(), reqVO.getReason());
        bpmTaskService.createSignTask(userId, new BpmTaskSignCreateReqVO()
                .setId(reqVO.getTaskId())
                .setUserIds(reqVO.getUserIds())
                .setType(reqVO.getType())
                .setReason(reqVO.getReason()));
        LinkedHashSet<Long> resolvedUserIds = parseResolvedUserIdsInOrder(context.stageSnapshot());
        resolvedUserIds.addAll(reqVO.getUserIds());
        updateStageResolvedUsers(context.stageSnapshot(), resolvedUserIds);
    }

    private DocControlApprovalReadinessEvaluation evaluateDocControlApprovalReadiness(
            Long userId, ValidatedTaskActionContext context, String sessionId, String stampedPdfUploadTicket,
            Long confirmedDirectoryId, List<DccControlledFileApproveTaskReqVO.DistributionScope> distributionScopes,
            Long legacyStampedPdfFileId, Long legacyTrainingRecordFileId, boolean collectDocControlArtifacts) {
        if (!collectDocControlArtifacts || context.stageCode() != DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL) {
            return readyTaskActionEvaluation(false, null);
        }

        List<DccControlledFileTaskReadinessBlockerRespVO> blockers = new ArrayList<>();
        DccUploadTicketBoundFile stampedPdf = null;
        if (legacyStampedPdfFileId != null) {
            blockers.add(taskReadinessBlocker("STAMPED_PDF_INVALID", "盖章 PDF 必须通过当前上传会话提交"));
        } else if (StrUtil.isBlank(sessionId) || StrUtil.isBlank(stampedPdfUploadTicket)) {
            blockers.add(taskReadinessBlocker("STAMPED_PDF_REQUIRED", "请上传盖章 PDF"));
        } else {
            try {
                requireApprovalUploadSession(context.file().getId(), context.taskId(), sessionId);
                stampedPdf = uploadTicketService.resolveForBinding(
                        new DccUploadTicketResolveCommand(stampedPdfUploadTicket, userId,
                                context.file().getCategoryId(), sessionId,
                                DccControlledFileUploadTypePolicy.PURPOSE_APPROVAL_PDF));
                if (stampedPdf == null || stampedPdf.storageFileId() == null
                        || DccControlledFilePreviewKindEnum.resolve(stampedPdf.fileName(), stampedPdf.contentType())
                        != DccControlledFilePreviewKindEnum.PDF) {
                    blockers.add(taskReadinessBlocker("STAMPED_PDF_INVALID", "盖章 PDF 上传凭证无效"));
                    stampedPdf = null;
                }
            } catch (ServiceException ex) {
                blockers.add(taskReadinessBlocker("STAMPED_PDF_INVALID", "盖章 PDF 上传凭证无效"));
            }
        }

        Long resolvedDirectoryId = null;
        try {
            resolvedDirectoryId = resolveDocControlDefaultDirectory(context.file());
        } catch (ServiceException ex) {
            blockers.add(taskReadinessBlocker("DEFAULT_DIRECTORY_INVALID", "正式默认目录未配置或已失效，请先修正类别目录绑定"));
        }

        DccControlledFileTaskReadinessRespVO readiness = new DccControlledFileTaskReadinessRespVO();
        readiness.setReady(blockers.isEmpty());
        readiness.setFinalApproval(true);
        readiness.setBlockers(List.copyOf(blockers));
        if (!blockers.isEmpty()) {
            return new DocControlApprovalReadinessEvaluation(readiness, null);
        }
        DocControlApprovalArtifacts artifacts = new DocControlApprovalArtifacts(stampedPdf.storageFileId(), sessionId,
                stampedPdfUploadTicket, resolvedDirectoryId, List.of());
        return new DocControlApprovalReadinessEvaluation(readiness, artifacts);
    }

    private DocControlApprovalReadinessEvaluation readyTaskActionEvaluation(
            boolean finalApproval, DocControlApprovalArtifacts artifacts) {
        DccControlledFileTaskReadinessRespVO readiness = new DccControlledFileTaskReadinessRespVO();
        readiness.setReady(true);
        readiness.setFinalApproval(finalApproval);
        readiness.setBlockers(List.of());
        return new DocControlApprovalReadinessEvaluation(readiness, artifacts);
    }

    private DccControlledFileTaskReadinessBlockerRespVO taskReadinessBlocker(String reasonCode, String message) {
        return new DccControlledFileTaskReadinessBlockerRespVO(reasonCode, message);
    }

    private void persistDocControlApprovalArtifacts(Long userId,
                                                    ValidatedTaskActionContext context,
                                                    DocControlApprovalArtifacts artifacts) {
        if (artifacts == null) {
            return;
        }
        controlledFileMapper.updateById(DccControlledFileDO.builder()
                .id(context.file().getId())
                .directoryId(artifacts.confirmedDirectoryId())
                .publishedFileId(artifacts.stampedPdfFileId())
                .stampedFileId(artifacts.stampedPdfFileId())
                .stampedTime(LocalDateTime.now())
                .build());
        persistDocControlConfirmedDirectory(context.file(), artifacts.confirmedDirectoryId());
        uploadTicketService.markBound(new DccUploadTicketMarkBoundCommand(artifacts.stampedPdfUploadTicket(),
                userId, context.file().getCategoryId(), artifacts.sessionId(),
                DccControlledFileUploadTypePolicy.PURPOSE_APPROVAL_PDF,
                context.file().getId()));
    }

    private Long resolveDocControlDefaultDirectory(DccControlledFileDO file) {
        DccCategoryDirectoryBindingDO binding = categoryDirectoryBindingMapper.selectActiveByCategoryId(file.getCategoryId());
        if (binding == null || binding.getDirectoryId() == null) {
            throw exception(FILE_CATEGORY_DIRECTORY_BINDING_NOT_EXISTS);
        }
        return validateSelectedDirectory(binding.getDirectoryId(), binding.getDirectoryId(), false);
    }

    private void persistDocControlConfirmedDirectory(DccControlledFileDO file, Long confirmedDirectoryId) {
        if (file.getMasterId() == null) {
            throw new IllegalStateException("controlled file master id is required for doc control directory confirmation");
        }
        // Relocating the existing Master does not change its project/taxonomy/file-number identity.
        controlledFileMasterMapper.updateById(DccControlledFileMasterDO.builder()
                .id(file.getMasterId())
                .directoryId(confirmedDirectoryId)
                .build());
    }

    private List<ResolvedDistributionPlan> resolveDistributionPlans(
            List<DccControlledFileApproveTaskReqVO.DistributionScope> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            throw exception(CONTROLLED_FILE_DISTRIBUTION_DEPARTMENT_REQUIRED);
        }
        LinkedHashSet<Long> uniqueDepartmentIds = new LinkedHashSet<>();
        List<ResolvedDistributionPlan> plans = new ArrayList<>();
        for (DccControlledFileApproveTaskReqVO.DistributionScope scope : scopes) {
            if (scope == null || scope.getDepartmentId() == null) {
                throw exception(CONTROLLED_FILE_DISTRIBUTION_DEPARTMENT_REQUIRED);
            }
            if (!uniqueDepartmentIds.add(scope.getDepartmentId())) {
                throw exception(CONTROLLED_FILE_DISTRIBUTION_DEPARTMENT_REQUIRED);
            }
            String distributionMedium = StrUtil.trim(scope.getDistributionMedium());
            if (StrUtil.isBlank(distributionMedium) || !DccDistributionMediumEnum.isValid(distributionMedium)) {
                throw exception(CONTROLLED_FILE_DISTRIBUTION_MEDIUM_INVALID);
            }
            List<Long> recipientUserIds = DccDistributionMediumEnum.PUBLIC_FOLDER.getCode().equals(distributionMedium)
                    ? resolveElectronicDistributionRecipientsByDept(scope.getDepartmentId()) : List.of();
            plans.add(new ResolvedDistributionPlan(scope.getDepartmentId(), distributionMedium, recipientUserIds));
        }
        return plans;
    }

    private List<Long> resolveElectronicDistributionRecipientsByDept(Long departmentId) {
        List<AdminUserRespDTO> users = adminUserApi.getUserListByDeptIds(List.of(departmentId));
        if (users == null || users.isEmpty()) {
            throw exception(ROUTE_PREVIEW_APPROVER_NOT_FOUND);
        }
        List<Long> recipientUserIds = users.stream()
                .filter(Objects::nonNull)
                .filter(user -> CommonStatusEnum.ENABLE.getStatus().equals(user.getStatus()))
                .map(AdminUserRespDTO::getId)
                .filter(Objects::nonNull)
                .toList();
        if (recipientUserIds.isEmpty()) {
            throw exception(ROUTE_PREVIEW_APPROVER_NOT_FOUND);
        }
        return recipientUserIds;
    }

    private void persistSingleFileDistributionPlans(Long controlledFileId,
                                                    List<ResolvedDistributionPlan> distributionPlans) {
        if (distributionPlans == null || distributionPlans.isEmpty()) {
            return;
        }
        for (ResolvedDistributionPlan distributionPlan : distributionPlans) {
            DccControlledFileDistributionDO distribution = DccControlledFileDistributionDO.builder()
                    .controlledFileId(controlledFileId)
                    .departmentId(distributionPlan.departmentId())
                    .distributionMedium(distributionPlan.distributionMedium())
                    .status(DccControlledFileDistributionStatusEnum.PENDING.getCode())
                    .build();
            distributionMapper.insert(distribution);
            for (Long recipientUserId : distributionPlan.recipientUserIds()) {
                distributionRecipientMapper.insert(DccControlledFileDistributionRecipientDO.builder()
                        .distributionId(distribution.getId())
                        .userId(recipientUserId)
                        .build());
            }
        }
    }

    private FileDO validateFileExists(Long fileId, ErrorCode errorCode) {
        if (fileId == null) {
            throw exception(errorCode);
        }
        FileDO file = fileMapper.selectById(fileId);
        if (file == null) {
            throw exception(errorCode);
        }
        return file;
    }

    private ValidatedReturnTarget validateReturnTarget(ValidatedTaskActionContext context,
                                                       String targetTaskDefinitionKey) {
        if (StrUtil.equals(targetTaskDefinitionKey, APPLICANT_REWORK_TASK_DEFINITION_KEY)) {
            return new ValidatedReturnTarget(APPLICANT_REWORK_TASK_DEFINITION_KEY,
                    DccControlledFileStatusEnum.PENDING_APPLICANT_REWORK.getStatus());
        }
        DccControlledFileStageCodeEnum targetStage = Arrays.stream(DccControlledFileStageCodeEnum.values())
                .filter(stageCode -> StrUtil.equals(stageCode.getCode(), targetTaskDefinitionKey))
                .findFirst()
                .orElseThrow(() -> exception(CONTROLLED_FILE_TASK_TARGET_INVALID));
        DccControlledFileRouteSnapshotDO targetSnapshot = routeSnapshotMapper.selectListByControlledFileId(context.file().getId()).stream()
                .filter(snapshot -> StrUtil.equals(snapshot.getStageCode(), targetStage.getCode()))
                .findFirst()
                .orElseThrow(() -> exception(CONTROLLED_FILE_TASK_TARGET_INVALID));
        Integer currentOrder = context.stageSnapshot().getStageOrder();
        Integer targetOrder = targetSnapshot.getStageOrder();
        if (currentOrder != null && targetOrder != null && targetOrder >= currentOrder) {
            throw exception(CONTROLLED_FILE_TASK_TARGET_INVALID);
        }
        return new ValidatedReturnTarget(targetSnapshot.getStageCode(), toPendingStatus(targetStage));
    }

    private void requireExistingUsers(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            throw exception(ROUTE_PREVIEW_APPROVER_NOT_FOUND);
        }
        List<Long> normalizedUserIds = userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (normalizedUserIds.size() != userIds.size()) {
            throw exception(ROUTE_PREVIEW_APPROVER_NOT_FOUND);
        }
        try {
            adminUserApi.validateUserList(normalizedUserIds);
        } catch (ServiceException ex) {
            throw exception(ROUTE_PREVIEW_APPROVER_NOT_FOUND);
        }
        approvalParticipantPostValidator.requireConfiguredPosts(normalizedUserIds);
    }

    private LinkedHashSet<Long> replaceCurrentUserWithAssignee(LinkedHashSet<Long> resolvedUserIds,
                                                               Long currentUserId, Long assigneeUserId) {
        LinkedHashSet<Long> updatedUserIds = new LinkedHashSet<>();
        updatedUserIds.add(assigneeUserId);
        resolvedUserIds.stream()
                .filter(userId -> !Objects.equals(userId, currentUserId))
                .forEach(updatedUserIds::add);
        return updatedUserIds;
    }

    private void updateStageResolvedUsers(DccControlledFileRouteSnapshotDO snapshot, LinkedHashSet<Long> resolvedUserIds) {
        routeSnapshotMapper.updateById(DccControlledFileRouteSnapshotDO.builder()
                .id(snapshot.getId())
                .resolvedUserIds(joinIds(new ArrayList<>(resolvedUserIds)))
                .build());
    }

    private void validateSubmitRequest(DccControlledFileSubmitReqVO reqVO, boolean requireUploadTickets) {
        validateSubmitRequest(reqVO,requireUploadTickets,true);
    }
    private void validateSubmitRequest(DccControlledFileSubmitReqVO reqVO,boolean requireUploadTickets,boolean requireDirectory) {
        if (reqVO == null
                || reqVO.getCategoryId() == null
                || StrUtil.isBlank(reqVO.getFileName())
                || StrUtil.isBlank(reqVO.getFileNumber())
                || (requireDirectory && reqVO.getDirectoryId() == null)
                || reqVO.getEffectiveDate() == null) {
            throw exception(CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING);
        }
        DccControlledFileChangeTypeEnum changeType = validateChangeType(reqVO.getChangeType());
        boolean explicitRevisionSource = changeType == DccControlledFileChangeTypeEnum.REVISION
                && reqVO.getRevisionSourceControlledFileId() != null;
        boolean hasUploadTicket = hasAnyUploadTicket(reqVO);
        if (requireUploadTickets && !explicitRevisionSource && hasAnyRawFileId(reqVO)) {
            throw exception(CONTROLLED_FILE_UPLOAD_TICKET_INVALID);
        }
        if ((requireUploadTickets && !explicitRevisionSource) || hasUploadTicket) {
            if (StrUtil.isBlank(reqVO.getSessionId()) || StrUtil.isBlank(reqVO.getOriginalUploadTicket())) {
                throw exception(CONTROLLED_FILE_UPLOAD_TICKET_INVALID);
            }
        } else if (!explicitRevisionSource && reqVO.getOriginalFileId() == null) {
            throw exception(CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING);
        }
        normalizeProcessType(reqVO.getProcessType());
        if (changeType != DccControlledFileChangeTypeEnum.NEW && !explicitRevisionSource
                && StrUtil.isBlank(reqVO.getVersionNo())) {
            throw exception(CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING);
        }
    }

    private DccProjectCodeDO validateEnabledProjectCode(Long projectCodeId, boolean required) {
        if (projectCodeId == null) {
            if (required) {
                throw exception(CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING);
            }
            return null;
        }
        DccProjectCodeDO projectCode = projectCodeMapper.selectById(projectCodeId);
        if (projectCode == null) {
            throw exception(PROJECT_CODE_NOT_EXISTS);
        }
        if (!DccProjectCodeStatusConstants.ENABLE.equals(projectCode.getStatus())) {
            throw exception(PROJECT_CODE_DISABLED);
        }
        return projectCode;
    }

    private ResolvedFileTypeTaxonomy resolveFileTypeTaxonomy(Long taxonomyId, boolean required) {
        if (taxonomyId == null) {
            if (required) {
                throw exception(CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING);
            }
            return null;
        }
        DccFileTypeTaxonomyPath path = fileTypeTaxonomyAdminService.resolveActivePath(taxonomyId);
        if (StrUtil.isBlank(path.level3())) {
            throw exception(FILE_TYPE_TAXONOMY_LEVEL_INVALID);
        }
        return new ResolvedFileTypeTaxonomy(path,
                fileTypeTaxonomyAdminService.listActiveDescendantIds(taxonomyId),
                fileTypeTaxonomyAdminService.listActiveDescendantPaths(taxonomyId));
    }

    private List<DccControlledFilePageReqVO.FileTypeTaxonomyPathFilter> toFileTypeTaxonomyPathFilters(
            List<DccFileTypeTaxonomyPath> paths) {
        return paths.stream()
                .map(path -> new DccControlledFilePageReqVO.FileTypeTaxonomyPathFilter(
                        path.level1(), path.level2(), path.level3(), path.level4(), path.level5()))
                .toList();
    }

    private void validateScreenshotProductCode(Product product) {
        if (product == null || !DccProjectProductIdentityResolver.MDM_MASTER.equals(product.source())) {
            return;
        }
        if (!isValidProductCode(product.code())) {
            throw exception(CONTROLLED_FILE_PRODUCT_CODE_INVALID);
        }
    }

    private void validateScreenshotSourceFiles(ResolvedSubmitFiles submitFiles) {
        FileDO sourceFile = loadSourceFile(submitFiles);
        if (!DccControlledFileUploadTypePolicy.isAllowedEditableSourceName(sourceFile.getName())) {
            throw exception(CONTROLLED_FILE_SOURCE_FILE_TYPE_INVALID);
        }
        if (DccControlledFileUploadTypePolicy.isPdfName(sourceFile.getName())) {
            return;
        }
        if (DccControlledFileUploadTypePolicy.isDrawingSourceName(sourceFile.getName()) && submitFiles.drawingPdfFileId() == null) {
            throw exception(CONTROLLED_FILE_DRAWING_PDF_REQUIRED);
        }
        if (DccControlledFileUploadTypePolicy.isDrawingSourceName(sourceFile.getName())) {
            validateDrawingPdfFile(submitFiles.drawingPdfFileId());
        }
    }

    private Long validateSelectedDirectory(Long bindingDirectoryId, Long selectedDirectoryId, boolean requireLeaf) {
        List<DccFileDirectoryDO> directories = directoryMapper.selectEnabledList();
        boolean bindingExists = directories.stream().anyMatch(item -> Objects.equals(item.getId(), bindingDirectoryId));
        if (!bindingExists) {
            throw exception(CONTROLLED_FILE_SUBMIT_DIRECTORY_INVALID);
        }
        Map<Long, List<DccFileDirectoryDO>> childrenByParentId = groupChildrenByParentId(directories);
        java.util.LinkedHashSet<Long> subtreeIds = new java.util.LinkedHashSet<>();
        collectDirectoryIds(bindingDirectoryId, childrenByParentId, subtreeIds);
        if (subtreeIds.isEmpty() || !subtreeIds.contains(selectedDirectoryId)) {
            throw exception(CONTROLLED_FILE_SUBMIT_DIRECTORY_INVALID);
        }
        if (requireLeaf && childrenByParentId.containsKey(selectedDirectoryId)) {
            throw exception(CONTROLLED_FILE_SUBMIT_DIRECTORY_NOT_LEAF);
        }
        return selectedDirectoryId;
    }

    private boolean hasAnyUploadTicket(DccControlledFileSubmitReqVO reqVO) {
        return reqVO != null && (StrUtil.isNotBlank(reqVO.getOriginalUploadTicket())
                || StrUtil.isNotBlank(reqVO.getSourceUploadTicket())
                || StrUtil.isNotBlank(reqVO.getDrawingPdfUploadTicket()));
    }

    private boolean hasAnyRawFileId(DccControlledFileSubmitReqVO reqVO) {
        return reqVO != null && (reqVO.getOriginalFileId() != null
                || reqVO.getSourceFileId() != null
                || reqVO.getDrawingPdfFileId() != null);
    }

    private ResolvedSubmitFiles resolveSubmitFiles(Long userId, DccControlledFileSubmitReqVO reqVO,
                                                   boolean requireUploadTickets) {
        if (requireUploadTickets || hasAnyUploadTicket(reqVO)) {
            if (StrUtil.isBlank(reqVO.getSessionId()) || StrUtil.isBlank(reqVO.getOriginalUploadTicket())) {
                throw exception(CONTROLLED_FILE_UPLOAD_TICKET_INVALID);
            }
            List<SubmitTicketBinding> bindings = new ArrayList<>();
            List<DccUploadTicketBoundFile> attachmentFiles = new ArrayList<>();
            String expectedPrefix = DccControlledFileProcessTypeEnum.EXTERNAL_REVIEW.getCode()
                    .equals(normalizeProcessType(reqVO.getProcessType()))
                    ? DccSourceUploadSession.externalPrefix()
                    : DccSourceUploadSession.newUploadPrefix(reqVO.getDccProjectCodeId(),
                            reqVO.getFileTypeTaxonomyId(), reqVO.getFileName());
            DccSourceUploadSession.require(reqVO.getSessionId(), expectedPrefix);
            DccUploadTicketBoundFile original = resolveUploadTicket(userId, reqVO.getCategoryId(), reqVO.getSessionId(),
                    reqVO.getOriginalUploadTicket(), DccControlledFileUploadTypePolicy.PURPOSE_SOURCE);
            bindings.add(new SubmitTicketBinding(reqVO.getOriginalUploadTicket(),
                    DccControlledFileUploadTypePolicy.PURPOSE_SOURCE));
            DccUploadTicketBoundFile source = original;
            if (StrUtil.isNotBlank(reqVO.getSourceUploadTicket())) {
                source = resolveUploadTicket(userId, reqVO.getCategoryId(), reqVO.getSessionId(), reqVO.getSourceUploadTicket(),
                        DccControlledFileUploadTypePolicy.PURPOSE_SOURCE);
                bindings.add(new SubmitTicketBinding(reqVO.getSourceUploadTicket(),
                        DccControlledFileUploadTypePolicy.PURPOSE_SOURCE));
            }
            DccUploadTicketBoundFile drawingPdf = null;
            if (StrUtil.isNotBlank(reqVO.getDrawingPdfUploadTicket())) {
                drawingPdf = resolveUploadTicket(userId, reqVO.getCategoryId(), reqVO.getSessionId(), reqVO.getDrawingPdfUploadTicket(),
                        DccControlledFileUploadTypePolicy.PURPOSE_DRAWING_PDF);
                bindings.add(new SubmitTicketBinding(reqVO.getDrawingPdfUploadTicket(),
                        DccControlledFileUploadTypePolicy.PURPOSE_DRAWING_PDF));
            }
            if (reqVO.getAttachmentUploadTickets() != null) {
                for (var attachmentTicket : reqVO.getAttachmentUploadTickets()) {
                    if (attachmentTicket == null || StrUtil.isBlank(attachmentTicket.getUploadTicket())
                            || StrUtil.isBlank(attachmentTicket.getSessionId())) {
                        throw exception(CONTROLLED_FILE_UPLOAD_TICKET_INVALID);
                    }
                    DccSourceUploadSession.require(attachmentTicket.getSessionId(), expectedPrefix);
                    DccUploadTicketBoundFile attachment = resolveUploadTicket(userId, reqVO.getCategoryId(),
                            attachmentTicket.getSessionId(), attachmentTicket.getUploadTicket(),
                            DccControlledFileUploadTypePolicy.PURPOSE_ATTACHMENT);
                    attachmentFiles.add(attachment);
                    bindings.add(new SubmitTicketBinding(attachmentTicket.getUploadTicket(),
                            DccControlledFileUploadTypePolicy.PURPOSE_ATTACHMENT, attachmentTicket.getSessionId()));
                }
            }
            return new ResolvedSubmitFiles(original.storageFileId(), source.storageFileId(),
                    drawingPdf == null ? null : drawingPdf.storageFileId(), attachmentFiles, bindings,source.fileName());
        }
        Long sourceFileId = reqVO.getSourceFileId() == null ? reqVO.getOriginalFileId() : reqVO.getSourceFileId();
        var stored=sourceFileId==null?null:fileMapper.selectById(sourceFileId);
        return new ResolvedSubmitFiles(reqVO.getOriginalFileId(), sourceFileId, reqVO.getDrawingPdfFileId(),
                List.of(), List.of(),stored==null?null:stored.getName());
    }

    private DccUploadTicketBoundFile resolveUploadTicket(Long userId, Long categoryId, String sessionId,
                                                        String uploadTicket, String purpose) {
        DccUploadTicketBoundFile file = uploadTicketService.resolveForBinding(
                new DccUploadTicketResolveCommand(uploadTicket, userId, categoryId, sessionId, purpose));
        if (file == null || file.storageFileId() == null) {
            throw exception(CONTROLLED_FILE_UPLOAD_TICKET_INVALID);
        }
        return file;
    }

    private void bindSubmitTickets(PreparedSubmitContext context, Long userId, Long controlledFileId) {
        if (context.submitFiles().ticketBindings().isEmpty()) {
            return;
        }
        Map<String, SubmitTicketBinding> distinctBindings = new LinkedHashMap<>();
        for (SubmitTicketBinding binding : context.submitFiles().ticketBindings()) {
            distinctBindings.put(binding.purpose() + "\u0000" + StrUtil.trim(binding.uploadTicket()), binding);
        }
        for (SubmitTicketBinding binding : distinctBindings.values()) {
            uploadTicketService.markBound(new DccUploadTicketMarkBoundCommand(binding.uploadTicket(), userId,
                    context.category().getId(), binding.sessionId() == null ? context.reqVO().getSessionId() : binding.sessionId(),
                    binding.purpose(), controlledFileId));
        }
    }

    private void bindSubmitAttachments(PreparedSubmitContext context, Long controlledFileId) {
        attachmentService.bindAttachments(controlledFileId, context.submitFiles().attachmentFiles());
    }

    private PreparedSubmitContext prepareSubmitContext(Long userId, DccControlledFileSubmitReqVO reqVO,
                                                       boolean requireLeafDirectory,
                                                       boolean requireScreenshotMetadata, Long ignoredControlledFileId,
                                                       boolean requireUploadTickets) {
        return prepareSubmitContext(userId,reqVO,requireLeafDirectory,requireScreenshotMetadata,ignoredControlledFileId,requireUploadTickets,null);
    }
    private PreparedSubmitContext prepareSubmitContext(Long userId,DccControlledFileSubmitReqVO reqVO,boolean requireLeafDirectory,boolean requireScreenshotMetadata,Long ignoredControlledFileId,boolean requireUploadTickets,DccDerivedUploadStorage storage) {
        if(storage!=null)requireDerivedStorage(userId,reqVO,storage);
        validateSubmitRequest(reqVO, requireUploadTickets,storage==null);
        String processType = normalizeProcessType(reqVO.getProcessType());
        boolean controlledUploadSubmit = requireUploadTickets
                && DccControlledFileProcessTypeEnum.CONTROLLED_FILE.getCode().equals(processType);
        DccProjectCodeDO projectCode = validateEnabledProjectCode(reqVO.getDccProjectCodeId(), true);
        lockDraftProject(projectCode.getId());
        ResolvedFileTypeTaxonomy fileTypeTaxonomy = resolveFileTypeTaxonomy(reqVO.getFileTypeTaxonomyId(),
                controlledUploadSubmit);
        boolean existingRevisionSubmit = DccControlledFileChangeTypeEnum.REVISION.getCode()
                .equals(StrUtil.trim(reqVO.getChangeType()))
                && reqVO.getRevisionSourceControlledFileId() != null;
        DccFileCategoryDO category = validateCategory(reqVO.getCategoryId());
        if (controlledUploadSubmit) {
            if (existingRevisionSubmit) {
                projectAccessService.assertProjectOwner(userId, projectCode.getId());
            } else {
                projectAccessService.assertProjectEditorOrOwner(userId, projectCode.getId());
            }
            if (!categoryPermissionSupport.hasCategoryPermission(category.getId(), userId,
                    DccFileCategoryPermissionActionEnum.UPLOAD)) {
                throw exception(DCC_PROJECT_ACCESS_DENIED);
            }
        }
        if (controlledUploadSubmit
                && !Objects.equals(category.getFileTypeTaxonomyId(), reqVO.getFileTypeTaxonomyId())) {
            throw exception(PROJECT_FILE_TEMPLATE_SELECTION_INVALID);
        }
        if (controlledUploadSubmit && !Objects.equals(category.getId(),
                fileTypeTaxonomyAdminService.resolveActiveCategoryId(reqVO.getFileTypeTaxonomyId()))) {
            throw exception(PROJECT_FILE_TEMPLATE_SELECTION_INVALID);
        }
        Product dccProduct = resolveDccProductFromProjectCode(projectCode);
        if (isProductBoundCategory(category) && !dccProduct.bound()) {
            throw exception(CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING);
        }
        if (requireScreenshotMetadata) {
            validateScreenshotProductCode(dccProduct);
        }
        DccCategoryDirectoryBindingDO binding = storage==null ? categoryDirectoryBindingMapper.selectActiveByCategoryId(category.getId()) : null;
        Long selectedDirectoryId = validateSelectedDirectory(storage==null?resolveUploadBindingDirectoryId(binding):storage.baseDirectoryId,
                storage==null?reqVO.getDirectoryId():storage.storageDirectoryId, requireLeafDirectory);
        DccControlledFileChangeTypeEnum changeType = validateChangeType(reqVO.getChangeType());
        validateNewFileTaxonomyLeaf(changeType, controlledUploadSubmit, fileTypeTaxonomy);
        DccControlledFileMasterDO master = loadOrCreateMaster(reqVO, changeType, controlledUploadSubmit,selectedDirectoryId);
        lockNativeContentMaster(master);
        DccControlledFileDO currentActiveFile = validateChangeTypeAgainstCurrentVersion(changeType, master);
        if (changeType != DccControlledFileChangeTypeEnum.NEW
                && !(changeType == DccControlledFileChangeTypeEnum.REVISION
                && reqVO.getRevisionSourceControlledFileId() != null)) {
            validateVersionChain(master.getId(), parseVersion(reqVO.getVersionNo()), ignoredControlledFileId, changeType);
        }
        validateRevisionTarget(reqVO, changeType, currentActiveFile, projectCode, fileTypeTaxonomy,
                controlledUploadSubmit);
        DccControlledFileDO selectedRevisionSource = resolveExplicitRevisionSource(reqVO, changeType, master,
                currentActiveFile, controlledUploadSubmit);
        boolean explicitRevisionSource = selectedRevisionSource != null;
        if (explicitRevisionSource) {
            reqVO.setOriginalFileId(selectedRevisionSource.getOriginalFileId() == null
                    ? selectedRevisionSource.getSourceFileId() : selectedRevisionSource.getOriginalFileId());
            reqVO.setSourceFileId(selectedRevisionSource.getSourceFileId());
        }
        ResolvedSubmitFiles submitFiles = applyOriginalVersionFile(
                resolveSubmitFiles(userId, reqVO, requireUploadTickets && !explicitRevisionSource), changeType,
                currentActiveFile);
        if (requireScreenshotMetadata) {
            validateScreenshotSourceFiles(submitFiles);
        }
        if(changeType==DccControlledFileChangeTypeEnum.NEW && controlledUploadSubmit) {
            if(StrUtil.isBlank(submitFiles.sourceOriginalFileName())) throw exception(CONTROLLED_FILE_UPLOAD_TICKET_INVALID);
            nameClaimService.claimNewSubmission(TenantContextHolder.getRequiredTenantId(),submitFiles.sourceOriginalFileName(),
                    projectCode.getId(),fileTypeTaxonomy.path().id(),master.getNormalizedFileNumber(),master.getId(),userId,submitFiles.sourceFileId());
        }
        return new PreparedSubmitContext(category, master, selectedDirectoryId, reqVO, submitFiles, dccProduct,
                projectCode, fileTypeTaxonomy, changeType, controlledUploadSubmit,
                currentActiveFile == null ? null : currentActiveFile.getId());
    }

    private Long resolveUploadBindingDirectoryId(DccCategoryDirectoryBindingDO binding) {
        if (binding == null || binding.getDirectoryId() == null) {
            throw exception(FILE_CATEGORY_DIRECTORY_BINDING_NOT_EXISTS);
        }
        return binding.getDirectoryId();
    }

    private void lockNativeContentMaster(DccControlledFileMasterDO master) {
        if (master == null || master.getId() == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        DccControlledFileMasterDO lockedMaster = controlledFileMasterMapper.selectByIdForUpdate(master.getId());
        if (lockedMaster == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
    }

    private boolean isProductBoundCategory(DccFileCategoryDO category) {
        String categoryCode = StrUtil.trimToEmpty(category.getCode()).toUpperCase(Locale.ROOT);
        return categoryCode.startsWith("DCC_FVM_DHF_") || categoryCode.startsWith("DCC_FVM_DMR_");
    }

    private void validateWithdrawnApplicantAction(Long userId, DccControlledFileDO file) {
        if (file == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        if (!userId.equals(file.getRequesterId())
                || !DccControlledFileStatusEnum.WITHDRAWN.getStatus().equals(file.getStatus())
                || StrUtil.isBlank(file.getProcessInstanceId())
                || file.getSupersededByFileId() != null) {
            throw exception(CONTROLLED_FILE_WITHDRAWN_ACTION_NOT_ALLOWED);
        }
    }

    private Set<Long> collectWithdrawnArtifactFileIds(DccControlledFileDO file) {
        Set<Long> fileIds = new LinkedHashSet<>();
        if (file == null) {
            return fileIds;
        }
        if (file.getSourceFileId() != null) {
            fileIds.add(file.getSourceFileId());
        }
        if (file.getOriginalFileId() != null) {
            fileIds.add(file.getOriginalFileId());
        }
        if (file.getDrawingPdfFileId() != null) {
            fileIds.add(file.getDrawingPdfFileId());
        }
        return fileIds;
    }

    private void deleteUnreferencedArtifacts(Set<Long> artifactFileIds) {
        if (artifactFileIds == null || artifactFileIds.isEmpty()) {
            return;
        }
        List<Long> orphanedIds = artifactFileIds.stream()
                .filter(Objects::nonNull)
                .filter(fileId -> controlledFileMapper.selectCountByReferencedFileId(fileId) == 0)
                .toList();
        if (orphanedIds.isEmpty()) {
            return;
        }
        try {
            fileService.deleteFileList(orphanedIds);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to delete orphaned withdrawn controlled file artifacts", ex);
        }
    }

    private DccControlledFileSubmitReqVO toResubmitReqVO(DccControlledFileDO file) {
        DccControlledFileVersionPolicy.VersionNumber withdrawnVersion = versionPolicy.parseStoredInitial(file);
        if (withdrawnVersion == null) {
            throw exception(CONTROLLED_FILE_VERSION_INVALID);
        }
        DccControlledFileSubmitReqVO reqVO = new DccControlledFileSubmitReqVO();
        reqVO.setCategoryId(file.getCategoryId());
        reqVO.setDirectoryId(file.getDirectoryId());
        reqVO.setOriginalFileId(file.getOriginalFileId());
        reqVO.setSourceFileId(file.getSourceFileId());
        reqVO.setDrawingPdfFileId(file.getDrawingPdfFileId());
        reqVO.setFileName(file.getFileName());
        reqVO.setFileNumber(file.getFileNumber());
        reqVO.setProductMasterId(null);
        reqVO.setProductCode(file.getProductCode());
        reqVO.setDccProjectCodeId(file.getDccProjectCodeId());
        reqVO.setFileTypeTaxonomyId(file.getFileTypeTaxonomyId());
        reqVO.setNeedTraining(file.getNeedTraining());
        reqVO.setProcessType(file.getProcessType());
        reqVO.setChangeType(file.getChangeType());
        reqVO.setVersionNo(withdrawnVersion.nextMinor().display());
        reqVO.setRevisionSourceControlledFileId(file.getId());
        reqVO.setRevisionSourceReason("主动撤回后重新提交");
        reqVO.setEffectiveDate(file.getEffectiveDate());
        reqVO.setRemark(file.getRemark());
        return reqVO;
    }

    private DccControlledFileDO insertControlledFile(PreparedSubmitContext context, Long userId,
                                                       String status, String processDefinitionKey,
                                                       boolean submitted, String ownershipType) {
        String processType = normalizeProcessType(context.reqVO().getProcessType());
        DccControlledFilePreparedSource preparedSource = sourceOwnershipService.prepareSubmissionSource(
                context.submitFiles().sourceFileId(), context.submitFiles().ticketBindings().isEmpty());
        if(preparedSource.isolatedCopy()) DccControlledFileVersionSourceRollback.enlist(sourceOwnershipService,preparedSource);
        String generatedVersionNo = resolveServerVersionNo(context);
        DccControlledFileVersionPolicy.VersionNumber configuredVersion = versionPolicy.parse(generatedVersionNo);
        DccControlledFileDO file = DccControlledFileDO.builder()
                .tenantId(TenantContextHolder.getRequiredTenantId())
                .masterId(context.master().getId())
                .categoryId(context.category().getId())
                .directoryId(context.selectedDirectoryId())
                .sourceFileId(preparedSource.sourceFileId())
                .originalFileId(context.submitFiles().originalFileId())
                .drawingPdfFileId(context.submitFiles().drawingPdfFileId())
                .fileName(context.reqVO().getFileName())
                .title(context.reqVO().getFileName())
                .fileNumber(context.reqVO().getFileNumber())
                .productMasterId(context.dccProduct().masterId())
                .productSource(context.dccProduct().source())
                .productCatalogId(context.dccProduct().catalogId())
                .productRelationId(context.dccProduct().relationId())
                .productCreateRequestId(context.dccProduct().requestId())
                .productCode(context.dccProduct().code())
                .productName(context.dccProduct().name())
                .dccProjectCodeId(context.projectCode() == null ? null : context.projectCode().getId())
                .fileTypeTaxonomyId(context.fileTypeTaxonomy() == null ? null : context.fileTypeTaxonomy().path().id())
                .fileTypeLevel1(context.fileTypeTaxonomy() == null ? null : context.fileTypeTaxonomy().path().level1())
                .fileTypeLevel2(context.fileTypeTaxonomy() == null ? null : context.fileTypeTaxonomy().path().level2())
                .fileTypeLevel3(context.fileTypeTaxonomy() == null ? null : context.fileTypeTaxonomy().path().level3())
                .fileTypeLevel4(context.fileTypeTaxonomy() == null ? null : context.fileTypeTaxonomy().path().level4())
                .fileTypeLevel5(context.fileTypeTaxonomy() == null ? null : context.fileTypeTaxonomy().path().level5())
                .needTraining(Boolean.TRUE.equals(context.reqVO().getNeedTraining()))
                .processType(processType)
                .changeType(context.changeType().getCode())
                .versionNo(generatedVersionNo)
                .revisionCode(configuredVersion == null ? null : configuredVersion.majorIdentity())
                .iterationNo(configuredVersion == null ? null : configuredVersion.iterationNo())
                .predecessorControlledFileId(context.reqVO().getRevisionSourceControlledFileId())
                .revisionBaseActiveControlledFileId(context.changeType() == DccControlledFileChangeTypeEnum.REVISION
                        ? context.revisionBaseActiveControlledFileId() : null)
                .changeDescription(StrUtil.isBlank(context.reqVO().getRevisionSourceReason())
                        ? context.reqVO().getRemark() : context.reqVO().getRevisionSourceReason())
                .sourceSha256(preparedSource.sourceSha256())
                .sourceOriginalFileName(context.submitFiles().sourceOriginalFileName())
                .effectiveDate(context.reqVO().getEffectiveDate())
                .remark(context.reqVO().getRemark())
                .status(isThreeWorkflowUploadOrRevision(processDefinitionKey) ? "WORKING" : status)
                .submitterId(userId)
                .requesterId(userId)
                .processDefinitionKey(submitted ? processDefinitionKey : null)
                .creationIdempotencyKey(submitted ? null : context.reqVO().getIdempotencyKey())
                .creationPayloadHash(submitted ? null : context.reqVO().getCreationPayloadHash())
                .submitIdempotencyKey(submitted ? context.reqVO().getIdempotencyKey() : null)
                .submitPayloadHash(submitted ? context.reqVO().getSubmitPayloadHash() : null)
                .submittedTime(submitted ? LocalDateTime.now() : null)
                .build();
        if(context.changeType()==DccControlledFileChangeTypeEnum.NEW) revisionService.recordInitialIntent(file);
        try {
            controlledFileMapper.insert(file);
        } catch (DuplicateKeyException ex) {
            // Only the root row insert can identify a concurrent submission winner.
            // Uniqueness failures in ownership, relations or ticket binding must roll back.
            throw new ControlledFileInsertConflict(ex);
        }
        sourceOwnershipService.claimSubmissionSource(file.getId(), preparedSource, userId, ownershipType);
        if("WORKING_CREATE".equals(ownershipType) || isThreeWorkflowUploadOrRevision(file))
            prepareApplicationDraft(userId,file,context.reqVO().getProjectAttributes());
        if(!Objects.equals(file.getStatus(),status)) {
            if(controlledFileMapper.updateById(DccControlledFileDO.builder().id(file.getId()).status(status).build())!=1)
                throw exception(CONTROLLED_FILE_ITERATION_SUBMIT_NOT_ALLOWED);
            file.setStatus(status);
        }
        return file;
    }

    /** Freeze this transaction's new snapshot only after its selected links and upload bindings have succeeded. */
    private void transitionNewCandidateStatus(DccControlledFileDO file, String status) {
        if (!DccControlledFileStatusEnum.WORKING.getStatus().equals(file.getStatus())
                || file.getControlledTime() != null || file.getActivatedTime() != null) {
            throw exception(CONTROLLED_FILE_ITERATION_SUBMIT_NOT_ALLOWED);
        }
        if (controlledFileMapper.updateById(DccControlledFileDO.builder().id(file.getId()).status(status).build()) != 1) {
            throw exception(CONTROLLED_FILE_ITERATION_SUBMIT_NOT_ALLOWED);
        }
        file.setStatus(status);
    }

    private static final class ControlledFileInsertConflict extends RuntimeException {
        private final DuplicateKeyException failure;

        private ControlledFileInsertConflict(DuplicateKeyException failure) {
            super(failure);
            this.failure = failure;
        }
    }

    private String resolveServerVersionNo(PreparedSubmitContext context) {
        if (context.changeType() == DccControlledFileChangeTypeEnum.NEW && context.serverOwnedVersion()) {
            try {
                return versionPolicy.initialForNewFile(context.reqVO().getVersionNo());
            } catch (IllegalArgumentException ex) {
                throw exception(CONTROLLED_FILE_VERSION_INVALID);
            }
        }
        if (context.changeType() == DccControlledFileChangeTypeEnum.REVISION
                && context.reqVO().getRevisionSourceControlledFileId() != null) {
            List<DccControlledFileDO> chain = controlledFileMapper.selectListByMasterId(context.master().getId());
            DccControlledFileVersionPolicy.VersionNumber max = versionPolicy.initial();
            if (chain != null) {
                for (DccControlledFileDO item : chain) {
                    DccControlledFileVersionPolicy.VersionNumber candidate = versionPolicy.parseStored(item);
                    if (candidate != null && candidate.compareMajorIdentityTo(max) > 0) {
                        max = candidate;
                    }
                }
            }
            return max.nextMajor().display();
        }
        return context.reqVO().getVersionNo();
    }

    private String normalizeProcessType(String processType) {
        String normalizedProcessType = StrUtil.blankToDefault(processType,
                DccControlledFileProcessTypeEnum.CONTROLLED_FILE.getCode());
        if (!DccControlledFileProcessTypeEnum.isValid(normalizedProcessType)) {
            throw exception(CONTROLLED_FILE_PROCESS_TYPE_INVALID);
        }
        return normalizedProcessType;
    }

    private DccControlledFileMasterDO loadOrCreateMaster(DccControlledFileSubmitReqVO reqVO,
                                                         DccControlledFileChangeTypeEnum changeType,
                                                         boolean useNewLogicalIdentity,Long selectedDirectoryId) {
        String normalizedFileNumber = normalizeFileNumber(reqVO.getFileNumber());
        reqVO.setFileNumber(normalizedFileNumber);
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        if (useNewLogicalIdentity && changeType == DccControlledFileChangeTypeEnum.NEW) {
            Long projectCodeId = reqVO.getDccProjectCodeId();
            Long taxonomyLeafId = reqVO.getFileTypeTaxonomyId();
            if (projectCodeId == null || taxonomyLeafId == null) {
                throw exception(CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING);
            }
            DccControlledFileMasterDO existing = controlledFileMasterMapper.selectByNewLogicalIdentity(
                    tenantId, projectCodeId, taxonomyLeafId, normalizedFileNumber);
            if (existing == null) {
                existing = resolveUniqueLegacyMaster(projectCodeId, taxonomyLeafId, normalizedFileNumber);
            }
            if (existing != null) {
                throw exception(CONTROLLED_FILE_FILE_NUMBER_CONFLICT);
            }
            DccControlledFileMasterDO master = DccControlledFileMasterDO.builder()
                    .tenantId(tenantId)
                    .dccProjectCodeId(projectCodeId)
                    .fileTypeTaxonomyLeafId(taxonomyLeafId)
                    .normalizedFileNumber(normalizedFileNumber)
                    .categoryId(reqVO.getCategoryId())
                    .directoryId(selectedDirectoryId)
                    .fileName(reqVO.getFileName())
                    .fileNumber(normalizedFileNumber)
                    .status(DccControlledFileMasterStatusEnum.ACTIVE_CHAIN.getCode())
                    .build();
            try {
                controlledFileMasterMapper.insert(master);
                return master;
            } catch (DuplicateKeyException ex) {
                throw exception(CONTROLLED_FILE_FILE_NUMBER_CONFLICT);
            }
        }
        if (useNewLogicalIdentity && changeType == DccControlledFileChangeTypeEnum.REVISION) {
            Long selectedRevisionFileId = reqVO.getRevisionSourceControlledFileId() == null
                    ? reqVO.getRevisionTargetControlledFileId() : reqVO.getRevisionSourceControlledFileId();
            if (selectedRevisionFileId == null) {
                throw exception(CONTROLLED_FILE_TASK_TARGET_INVALID);
            }
            DccControlledFileDO source = controlledFileMapper.selectById(selectedRevisionFileId);
            DccControlledFileMasterDO master = source == null || source.getMasterId() == null ? null
                    : controlledFileMasterMapper.selectByIdForUpdate(source.getMasterId());
            if (master == null) {
                throw exception(CONTROLLED_FILE_NOT_EXISTS);
            }
            return master;
        }
        List<DccControlledFileMasterDO> masters = controlledFileMasterMapper.selectListByFileNumber(normalizedFileNumber);
        if (masters == null || masters.isEmpty()) {
            if (changeType != DccControlledFileChangeTypeEnum.NEW) {
                throw exception(CONTROLLED_FILE_NOT_EXISTS);
            }
            DccControlledFileMasterDO master = DccControlledFileMasterDO.builder()
                    .categoryId(reqVO.getCategoryId())
                    .directoryId(selectedDirectoryId)
                    .fileName(reqVO.getFileName())
                    .fileNumber(normalizedFileNumber)
                    .status(DccControlledFileMasterStatusEnum.ACTIVE_CHAIN.getCode())
                    .build();
            controlledFileMasterMapper.insert(master);
            return master;
        }
        if (masters.size() != 1) {
            throw exception(CONTROLLED_FILE_FILE_NUMBER_CONFLICT);
        }
        return masters.get(0);
    }

    private DccControlledFileMasterDO resolveUniqueLegacyMaster(Long projectCodeId, Long taxonomyLeafId,
                                                                 String normalizedFileNumber) {
        List<DccControlledFileDO> legacyCandidates = controlledFileMapper
                .selectActiveByLegacyProjectAndFileNumber(projectCodeId, normalizedFileNumber);
        List<DccControlledFileDO> legacyActiveFiles = (legacyCandidates == null ? List.<DccControlledFileDO>of()
                : legacyCandidates).stream()
                .filter(file -> Objects.equals(resolveControlledFileTypeTaxonomyId(file), taxonomyLeafId))
                .toList();
        if (legacyActiveFiles == null || legacyActiveFiles.isEmpty()) {
            return null;
        }
        List<Long> masterIds = legacyActiveFiles.stream().map(DccControlledFileDO::getMasterId)
                .filter(Objects::nonNull).distinct().toList();
        if (masterIds.size() != 1 || legacyActiveFiles.size() != 1) {
            throw exception(CONTROLLED_FILE_FILE_NUMBER_CONFLICT);
        }
        DccControlledFileMasterDO master = controlledFileMasterMapper.selectById(masterIds.get(0));
        if (master == null || !Objects.equals(master.getCurrentActiveControlledFileId(), legacyActiveFiles.get(0).getId())) {
            throw exception(CONTROLLED_FILE_FILE_NUMBER_CONFLICT);
        }
        return master;
    }

    private DccControlledFileChangeTypeEnum validateChangeType(String changeType) {
        DccControlledFileChangeTypeEnum resolved = DccControlledFileChangeTypeEnum.of(StrUtil.trim(changeType));
        if (resolved == null) {
            throw exception(CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING);
        }
        return resolved;
    }

    private void validateNewFileTaxonomyLeaf(DccControlledFileChangeTypeEnum changeType,
                                             boolean controlledUploadSubmit,
                                             ResolvedFileTypeTaxonomy taxonomy) {
        if (!controlledUploadSubmit || changeType != DccControlledFileChangeTypeEnum.NEW) {
            return;
        }
        if (taxonomy == null || taxonomy.path() == null || taxonomy.path().id() == null
                || taxonomy.activeDescendantIds() == null
                || taxonomy.activeDescendantIds().size() != 1
                || !taxonomy.activeDescendantIds().contains(taxonomy.path().id())) {
            throw exception(FILE_TYPE_TAXONOMY_LEVEL_INVALID);
        }
    }

    private DccControlledFileDO validateChangeTypeAgainstCurrentVersion(DccControlledFileChangeTypeEnum changeType,
                                                                       DccControlledFileMasterDO master) {
        DccControlledFileDO currentActiveFile = loadCurrentActiveFile(master);
        if (changeType == DccControlledFileChangeTypeEnum.NEW) {
            if (currentActiveFile != null) {
                throw exception(CONTROLLED_FILE_FILE_NUMBER_CONFLICT);
            }
            return null;
        }
        if (currentActiveFile == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        return currentActiveFile;
    }

    private void validateRevisionTarget(DccControlledFileSubmitReqVO reqVO,
                                        DccControlledFileChangeTypeEnum changeType,
                                        DccControlledFileDO currentActiveFile,
                                        DccProjectCodeDO projectCode,
                                        ResolvedFileTypeTaxonomy fileTypeTaxonomy,
                                        boolean controlledUploadSubmit) {
        if (!controlledUploadSubmit) {
            return;
        }
        Long revisionTargetId = reqVO.getRevisionSourceControlledFileId() != null
                ? reqVO.getRevisionSourceControlledFileId() : reqVO.getRevisionTargetControlledFileId();
        if (changeType != DccControlledFileChangeTypeEnum.REVISION) {
            if (revisionTargetId != null) {
                throw exception(CONTROLLED_FILE_TASK_TARGET_INVALID);
            }
            return;
        }
        if (revisionTargetId == null || currentActiveFile == null) {
            throw exception(CONTROLLED_FILE_TASK_TARGET_INVALID);
        }
        DccControlledFileDO revisionTarget = controlledFileMapper.selectById(revisionTargetId);
        String targetFileNumber = revisionTarget == null ? null : StrUtil.trim(revisionTarget.getFileNumber());
        if (revisionTarget == null
                || !Objects.equals(revisionTarget.getMasterId(), currentActiveFile.getMasterId())
                || !Objects.equals(revisionTarget.getCategoryId(), reqVO.getCategoryId())
                || !Objects.equals(revisionTarget.getDirectoryId(), reqVO.getDirectoryId())
                || !Objects.equals(StrUtil.trim(revisionTarget.getFileName()), StrUtil.trim(reqVO.getFileName()))
                || StrUtil.isBlank(targetFileNumber)
                || !Objects.equals(targetFileNumber, reqVO.getFileNumber())) {
            throw exception(CONTROLLED_FILE_TASK_TARGET_INVALID);
        }
        boolean explicitSource = reqVO.getRevisionSourceControlledFileId() != null;
        if (!explicitSource && (!Objects.equals(revisionTargetId, currentActiveFile.getId())
                || !DccControlledFileStatusEnum.ACTIVE.getStatus().equals(revisionTarget.getStatus()))) {
            throw exception(CONTROLLED_FILE_TASK_TARGET_INVALID);
        }
        if (explicitSource && !isRevisionSourceStatusAllowed(revisionTarget.getStatus())) {
            throw exception(CONTROLLED_FILE_TASK_TARGET_INVALID);
        }
        if (projectCode == null || projectCode.getId() == null
                || !targetMatchesProject(revisionTargetId, projectCode.getId())) {
            throw exception(CONTROLLED_FILE_TASK_TARGET_INVALID);
        }
        Long revisionTargetTaxonomyId = resolveControlledFileTypeTaxonomyId(revisionTarget);
        if (fileTypeTaxonomy == null
                || revisionTargetTaxonomyId == null
                || !fileTypeTaxonomy.activeDescendantIds().contains(revisionTargetTaxonomyId)) {
            throw exception(CONTROLLED_FILE_TASK_TARGET_INVALID);
        }
    }

    private boolean isRevisionSourceStatusAllowed(String status) {
        return DccControlledFileStatusEnum.WORKING.getStatus().equals(status)
                || DccControlledFileStatusEnum.ACTIVE.getStatus().equals(status)
                || DccControlledFileStatusEnum.SUPERSEDED.getStatus().equals(status)
                || DccControlledFileStatusEnum.REJECTED.getStatus().equals(status);
    }

    private DccControlledFileDO resolveExplicitRevisionSource(DccControlledFileSubmitReqVO reqVO,
                                                               DccControlledFileChangeTypeEnum changeType,
                                                               DccControlledFileMasterDO master,
                                                               DccControlledFileDO currentActiveFile,
                                                               boolean controlledUploadSubmit) {
        if (!controlledUploadSubmit || changeType != DccControlledFileChangeTypeEnum.REVISION
                || reqVO.getRevisionSourceControlledFileId() == null) {
            return null;
        }
        if (currentActiveFile != null
                && !Objects.equals(reqVO.getRevisionSourceControlledFileId(), currentActiveFile.getId())
                && StrUtil.isBlank(reqVO.getRevisionSourceReason())) {
            throw exception(CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING);
        }
        List<DccControlledFileDO> chain = controlledFileMapper.selectListByMasterId(master.getId());
        if (chain == null || chain.stream().noneMatch(item -> Objects.equals(item.getId(),
                reqVO.getRevisionSourceControlledFileId()))) {
            throw exception(CONTROLLED_FILE_TASK_TARGET_INVALID);
        }
        boolean openRevision = chain.stream()
                .filter(item -> DccControlledFileChangeTypeEnum.REVISION.getCode().equals(item.getChangeType()))
                .filter(item -> !Objects.equals(item.getId(), reqVO.getRevisionSourceControlledFileId()))
                .anyMatch(item -> isUnfinishedWorkflowVersion(item));
        if (openRevision) {
            throw exception(CONTROLLED_FILE_WORKFLOW_IN_PROGRESS);
        }
        DccControlledFileDO source = controlledFileMapper.selectById(reqVO.getRevisionSourceControlledFileId());
        if (source == null || !Objects.equals(source.getMasterId(), master.getId())
                || !isRevisionSourceStatusAllowed(source.getStatus())) {
            throw exception(CONTROLLED_FILE_TASK_TARGET_INVALID);
        }
        if (currentActiveFile != null && !Objects.equals(source.getId(), currentActiveFile.getId())
                && StrUtil.isBlank(reqVO.getRevisionSourceReason())) {
            throw exception(CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING);
        }
        return source;
    }

    private Long resolveControlledFileTypeTaxonomyId(DccControlledFileDO file) {
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

    private boolean targetMatchesProject(Long revisionTargetId, Long projectCodeId) {
        List<DccControlledFileDO> associatedFiles = controlledFileMapper.selectAssociatedFilesByProjectCodeId(
                projectCodeId, List.of(revisionTargetId));
        return associatedFiles != null && associatedFiles.stream()
                .anyMatch(file -> Objects.equals(file.getId(), revisionTargetId));
    }

    private DccControlledFileDO loadCurrentActiveFile(DccControlledFileMasterDO master) {
        if (master == null || master.getCurrentActiveControlledFileId() == null) {
            return null;
        }
        DccControlledFileDO currentActiveFile = controlledFileMapper.selectById(master.getCurrentActiveControlledFileId());
        if (currentActiveFile == null
                || !DccControlledFileStatusEnum.ACTIVE.getStatus().equals(currentActiveFile.getStatus())) {
            throw exception(CONTROLLED_FILE_FILE_NUMBER_CONFLICT);
        }
        return currentActiveFile;
    }

    private ResolvedSubmitFiles applyOriginalVersionFile(ResolvedSubmitFiles submitFiles,
                                                        DccControlledFileChangeTypeEnum changeType,
                                                        DccControlledFileDO currentActiveFile) {
        if (changeType == DccControlledFileChangeTypeEnum.NEW) {
            return submitFiles;
        }
        return new ResolvedSubmitFiles(resolveOriginalFileId(currentActiveFile), submitFiles.sourceFileId(),
                submitFiles.drawingPdfFileId(), submitFiles.attachmentFiles(), submitFiles.ticketBindings(),submitFiles.sourceOriginalFileName());
    }

    private Long resolveOriginalFileId(DccControlledFileDO currentActiveFile) {
        if (currentActiveFile == null) {
            return null;
        }
        return currentActiveFile.getOriginalFileId() == null
                ? currentActiveFile.getSourceFileId()
                : currentActiveFile.getOriginalFileId();
    }

    private FileTrace resolveFileTrace(Long fileId) {
        if (fileId == null) {
            return FileTrace.empty();
        }
        FileDO file = fileMapper.selectById(fileId);
        if (file == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        return new FileTrace(file.getName(), file.getPath());
    }

    private Product resolveDccProductFromProjectCode(DccProjectCodeDO projectCode) {
        return DccProjectProductIdentityResolver.resolve(projectCode,mdmProductApi,approvedProductIdentityMapper);
    }

    private boolean isValidProductCode(String productCode) {
        return StrUtil.isNotBlank(productCode) && productCode.matches("[A-Za-z0-9]{14}");
    }

    private FileDO loadSourceFile(ResolvedSubmitFiles submitFiles) {
        FileDO sourceFile = fileMapper.selectById(submitFiles.sourceFileId());
        if (sourceFile == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        return sourceFile;
    }

    private void validateDrawingPdfFile(Long drawingPdfFileId) {
        FileDO drawingPdfFile = fileMapper.selectById(drawingPdfFileId);
        if (drawingPdfFile == null || !DccControlledFileUploadTypePolicy.isRealPdfFile(
                drawingPdfFile.getName(), readFileContent(drawingPdfFile))) {
            throw exception(CONTROLLED_FILE_DRAWING_PDF_FILE_INVALID);
        }
    }

    private byte[] readFileContent(FileDO file) {
        try {
            return fileService.getFileContent(file.getConfigId(), file.getPath());
        } catch (Exception ex) {
            throw exception(CONTROLLED_FILE_DRAWING_PDF_FILE_INVALID);
        }
    }

    private void replaceExistingVersionOneForNasTransfer(DccControlledFileSubmitReqVO reqVO) {
        if (!StrUtil.equalsIgnoreCase(reqVO.getVersionNo(), "V1.0")) {
            return;
        }
        DccControlledFileMasterDO deletedMaster =
                controlledFileMasterMapper.selectDeletedByCategoryIdAndDirectoryIdAndFileName(
                        reqVO.getCategoryId(), reqVO.getDirectoryId(), reqVO.getFileName());
        if (deletedMaster != null) {
            if (!StrUtil.equals(deletedMaster.getFileNumber(), reqVO.getFileNumber())) {
                throw exception(CONTROLLED_FILE_FILE_NUMBER_CONFLICT);
            }
            int restored = controlledFileMasterMapper.restoreDeletedNasMaster(
                    deletedMaster.getId(), reqVO.getCategoryId(), reqVO.getDirectoryId(),
                    reqVO.getFileName(), reqVO.getFileNumber(),
                    DccControlledFileMasterStatusEnum.ACTIVE_CHAIN.getCode());
            if (restored != 1) {
                throw new IllegalStateException("deleted dcc controlled file master restore failed: "
                        + deletedMaster.getId());
            }
            return;
        }
        DccControlledFileMasterDO master = controlledFileMasterMapper.selectByCategoryIdAndDirectoryIdAndFileName(
                reqVO.getCategoryId(), reqVO.getDirectoryId(), reqVO.getFileName());
        if (master == null || !StrUtil.equals(master.getFileNumber(), reqVO.getFileNumber())) {
            return;
        }
        List<DccControlledFileDO> chainFiles = controlledFileMapper.selectListByMasterId(master.getId());
        if (chainFiles.size() != 1) {
            return;
        }
        DccControlledFileDO existing = chainFiles.get(0);
        DccControlledFileVersion existingVersion = DccControlledFileVersion.parse(existing.getVersionNo());
        DccControlledFileVersion targetVersion = DccControlledFileVersion.parse("V1.0");
        if (existingVersion == null || targetVersion == null || existingVersion.compareTo(targetVersion) != 0) {
            return;
        }
        routeSnapshotMapper.delete(DccControlledFileRouteSnapshotDO::getControlledFileId, existing.getId());
        controlledFileMapper.deleteById(existing.getId());
        master.setCurrentActiveControlledFileId(null);
        controlledFileMasterMapper.update(null, new UpdateWrapper<DccControlledFileMasterDO>()
                .eq("id", master.getId())
                .set("current_active_controlled_file_id", null));
    }

    private void validateVersionChain(Long masterId, DccControlledFileVersion requestedVersion, Long ignoredControlledFileId,
                                      DccControlledFileChangeTypeEnum changeType) {
        List<DccControlledFileDO> chainFiles = controlledFileMapper.selectList(DccControlledFileDO::getMasterId, masterId);
        if (chainFiles == null) {
            chainFiles = List.of();
        }
        boolean hasUnfinishedWorkflow = chainFiles.stream()
                .filter(file -> ignoredControlledFileId == null || !Objects.equals(file.getId(), ignoredControlledFileId))
                .anyMatch(this::isUnfinishedWorkflowVersion);
        if (hasUnfinishedWorkflow) {
            throw exception(CONTROLLED_FILE_WORKFLOW_IN_PROGRESS);
        }
        if (changeType == DccControlledFileChangeTypeEnum.OBSOLETE) {
            return;
        }
        DccControlledFileVersion maxVersion = chainFiles.stream()
                .filter(file -> ignoredControlledFileId == null || !Objects.equals(file.getId(), ignoredControlledFileId))
                .map(DccControlledFileDO::getVersionNo)
                .map(this::parseVersion)
                .max(DccControlledFileVersion::compareTo)
                .orElse(null);
        if (maxVersion != null && requestedVersion.compareTo(maxVersion) <= 0) {
            throw exception(CONTROLLED_FILE_VERSION_NOT_GREATER);
        }
    }

    private DccControlledFileVersion parseVersion(String rawVersion) {
        DccControlledFileVersion version = DccControlledFileVersion.parse(rawVersion);
        if (version == null) {
            throw exception(CONTROLLED_FILE_VERSION_INVALID);
        }
        return version;
    }

    private String normalizeFileNumber(String fileNumber) {
        String normalized = StrUtil.trim(fileNumber).toUpperCase(Locale.ROOT);
        if (StrUtil.isBlank(normalized)) {
            throw exception(CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING);
        }
        return normalized;
    }

    private boolean isUnfinishedWorkflowVersion(DccControlledFileDO file) {
        if (file == null || StrUtil.isBlank(file.getStatus())
                || DccControlledFileStatusEnum.ACTIVE.getStatus().equals(file.getStatus())) {
            return false;
        }
        if(DccControlledFileStatusEnum.CONTROLLED_PENDING_EFFECTIVE.getStatus().equals(file.getStatus())) {
            return false;
        }
        return !Set.of(
                DccControlledFileStatusEnum.REJECTED.getStatus(),
                DccControlledFileStatusEnum.WITHDRAWN.getStatus(),
                DccControlledFileStatusEnum.OBSOLETE.getStatus(),
                DccControlledFileStatusEnum.SUPERSEDED.getStatus()
        ).contains(file.getStatus());
    }

    private record PreparedSubmitContext(DccFileCategoryDO category,
                                          DccControlledFileMasterDO master,
                                          Long selectedDirectoryId,
                                          DccControlledFileSubmitReqVO reqVO,
                                          ResolvedSubmitFiles submitFiles,
                                          Product dccProduct,
                                          DccProjectCodeDO projectCode,
                                          ResolvedFileTypeTaxonomy fileTypeTaxonomy,
                                          DccControlledFileChangeTypeEnum changeType,
                                          boolean serverOwnedVersion,
                                          Long revisionBaseActiveControlledFileId) {
    }

    private record WorkingIterationSubmissionPayload(Long iterationId, boolean needTraining,
                                                     List<Long> selectedSignoffUserIds) {
    }

    private record ResolvedFileTypeTaxonomy(DccFileTypeTaxonomyPath path,
                                            List<Long> activeDescendantIds,
                                            List<DccFileTypeTaxonomyPath> activeDescendantPaths) {
    }

    private record ResolvedSubmitFiles(Long originalFileId,
                                       Long sourceFileId,
                                       Long drawingPdfFileId,
                                       List<DccUploadTicketBoundFile> attachmentFiles,
                                       List<SubmitTicketBinding> ticketBindings,String sourceOriginalFileName) {
    }

    private record SubmitTicketBinding(String uploadTicket, String purpose, String sessionId) {
        private SubmitTicketBinding(String uploadTicket, String purpose) {
            this(uploadTicket, purpose, null);
        }
    }

    private record FileTrace(String name, String path) {

        private static FileTrace empty() {
            return new FileTrace(null, null);
        }
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

    private Set<Long> resolveRequestedDirectoryIds(DccControlledFilePageReqVO reqVO) {
        if (!Boolean.TRUE.equals(reqVO.getIncludeDescendantDirectories()) || reqVO.getDirectoryId() == null) {
            return null;
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

    private DccControlledFilePageReqVO buildPageReqWithoutDirectory(DccControlledFilePageReqVO reqVO) {
        DccControlledFilePageReqVO sanitizedReqVO = new DccControlledFilePageReqVO();
        sanitizedReqVO.setPageNo(reqVO.getPageNo());
        sanitizedReqVO.setPageSize(reqVO.getPageSize());
        sanitizedReqVO.setCategoryId(reqVO.getCategoryId());
        sanitizedReqVO.setRequesterId(reqVO.getRequesterId());
        sanitizedReqVO.setStatus(reqVO.getStatus());
        sanitizedReqVO.setProcessType(reqVO.getProcessType());
        sanitizedReqVO.setKeyword(reqVO.getKeyword());
        sanitizedReqVO.setIncludeDescendantDirectories(reqVO.getIncludeDescendantDirectories());
        sanitizedReqVO.setLatestVersionOnly(reqVO.getLatestVersionOnly());
        sanitizedReqVO.setDccProjectCodeId(reqVO.getDccProjectCodeId());
        sanitizedReqVO.setFileTypeTaxonomyId(reqVO.getFileTypeTaxonomyId());
        sanitizedReqVO.setFileTypeTaxonomyIds(reqVO.getFileTypeTaxonomyIds());
        sanitizedReqVO.setRecognitionStatus(reqVO.getRecognitionStatus());
        sanitizedReqVO.setBatchRecognitionTaskId(reqVO.getBatchRecognitionTaskId());
        sanitizedReqVO.setQuickFilter(reqVO.getQuickFilter());
        return sanitizedReqVO;
    }

    private Map<Long, List<DccFileDirectoryDO>> groupChildrenByParentId(List<DccFileDirectoryDO> directories) {
        Map<Long, List<DccFileDirectoryDO>> childrenByParentId = new java.util.LinkedHashMap<>();
        for (DccFileDirectoryDO directory : directories) {
            childrenByParentId.computeIfAbsent(directory.getParentId(), key -> new java.util.ArrayList<>())
                    .add(directory);
        }
        return childrenByParentId;
    }

    private ValidatedTaskActionContext validateTaskAction(Long userId, Long controlledFileId, String taskId,
                                                          String processDefinitionKey, String actionType) {
        DccControlledFileDO file = controlledFileMapper.selectById(controlledFileId);
        if (file == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        if (!StrUtil.equals(file.getProcessDefinitionKey(), processDefinitionKey)
                || StrUtil.isBlank(file.getProcessInstanceId())) {
            throw exception(CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED);
        }
        boolean nativeWorkflow=isThreeWorkflowUploadOrRevision(file);
        if(nativeWorkflow) {
            Long tenantId=TenantContextHolder.getRequiredTenantId();
            if(!Objects.equals(file.getId(),controlledFileId) || !Objects.equals(file.getTenantId(),tenantId)
                    || file.getMasterId()==null) throw exception(CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED);
            var master=controlledFileMasterMapper.selectByIdForUpdate(file.getMasterId());
            if(master==null) throw exception(CONTROLLED_FILE_NOT_EXISTS);
            if(!Objects.equals(master.getId(),file.getMasterId()) || !Objects.equals(master.getTenantId(),tenantId))
                throw exception(CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED);
            file=controlledFileMapper.selectByIdAndTenantForUpdate(tenantId,controlledFileId);
            if(file==null) throw exception(CONTROLLED_FILE_NOT_EXISTS);
            if(!Objects.equals(file.getId(),controlledFileId) || !Objects.equals(file.getTenantId(),tenantId)
                    || !Objects.equals(file.getMasterId(),master.getId())
                    || !StrUtil.equals(file.getProcessDefinitionKey(),processDefinitionKey)
                    || StrUtil.isBlank(file.getProcessInstanceId()))
                throw exception(CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED);
        }
        Task task;
        try {
            task = bpmTaskService.validateTask(userId, taskId);
        } catch (ServiceException ex) {
            Task runtimeTask = bpmTaskService.getTask(taskId);
            if (hasRouteRuntimeMismatch(file, userId, runtimeTask)) {
                throw exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
            }
            throw ex;
        }
        if (task==null || (nativeWorkflow && (!StrUtil.equals(taskId,task.getId())
                || !String.valueOf(TenantContextHolder.getRequiredTenantId()).equals(task.getTenantId())))
                || !StrUtil.equals(file.getProcessInstanceId(), task.getProcessInstanceId())) {
            throw exception(CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED);
        }
        DccControlledFileStageCodeEnum stageCode = resolveCurrentTaskStage(task.getTaskDefinitionKey(), file.getStatus());
        if (!StrUtil.equals(file.getStatus(), toPendingStatus(stageCode))) {
            throw exception(CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED);
        }
        if (stageCode == DccControlledFileStageCodeEnum.APPLICANT_REWORK) {
            if (!StrUtil.equals(actionType, "APPROVE")) {
                throw exception(CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED);
            }
            if (!Objects.equals(file.getRequesterId(), userId)) {
                throw exception(CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED);
            }
            validateStagePermission(userId, stageCode);
            return new ValidatedTaskActionContext(file, stageCode, task.getTaskDefinitionKey(), null, taskId);
        }
        DccControlledFileRouteSnapshotDO stageSnapshot = routeSnapshotMapper.selectListByControlledFileId(controlledFileId).stream()
                .filter(snapshot -> StrUtil.equals(snapshot.getStageCode(), stageCode.getCode()))
                .findFirst()
                .orElseThrow(() -> exception(CONTROLLED_FILE_TASK_STAGE_UNSUPPORTED));
        boolean departmentSignoff = isThreeWorkflowUploadOrRevision(file)
                && stageCode == DccControlledFileStageCodeEnum.MATRIX_REVIEW;
        if (!departmentSignoff && !parseResolvedUserIds(stageSnapshot).contains(userId)) {
            if (StrUtil.equals(task.getAssignee(), String.valueOf(userId))) {
                throw exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
            }
            throw exception(CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED);
        }
        validateStagePermission(userId, stageCode);
        bindTaskAssigneeSnapshotIfPresent(controlledFileId, stageCode, task);
        if (departmentSignoff) signoffAssignmentService.requireAssigned(userId,controlledFileId,task);
        return new ValidatedTaskActionContext(file, stageCode, task.getTaskDefinitionKey(), stageSnapshot, taskId);
    }

    private void bindTaskAssigneeSnapshotIfPresent(Long controlledFileId, DccControlledFileStageCodeEnum stageCode,
                                                   Task task) {
        Map<String, Object> taskLocalVariables = task.getTaskLocalVariables();
        if (taskLocalVariables == null || taskLocalVariables.isEmpty()) {
            return;
        }
        String obligationId = (String) taskLocalVariables.get(BpmnVariableConstants.TASK_VARIABLE_DCC_OBLIGATION_ID);
        if (StrUtil.isBlank(obligationId)) {
            return;
        }
        int updatedRows = taskAssigneeSnapshotMapper.bindBpmTaskByObligationId(controlledFileId,
                stageCode.getCode(), obligationId, task.getId(), task.getExecutionId());
        if (updatedRows != 1) {
            throw exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
        }
    }

    private String resolveNativeProcessDefinitionKey(Long controlledFileId) {
        DccControlledFileDO file = controlledFileMapper.selectById(controlledFileId);
        if (file == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        String processDefinitionKey = file.getProcessDefinitionKey();
        if (StrUtil.isNotBlank(processDefinitionKey)
                && DccControlledFileProcessDefinitionKeys.NATIVE_FINALIZATION_KEYS.contains(processDefinitionKey)) {
            return processDefinitionKey;
        }
        return BPM_PROCESS_DEFINITION_KEY;
    }

    private boolean hasRouteRuntimeMismatch(DccControlledFileDO file, Long userId, Task runtimeTask) {
        if (runtimeTask == null || !StrUtil.equals(file.getProcessInstanceId(), runtimeTask.getProcessInstanceId())
                || StrUtil.isBlank(runtimeTask.getAssignee())
                || StrUtil.equals(runtimeTask.getAssignee(), String.valueOf(userId))) {
            return false;
        }
        DccControlledFileStageCodeEnum stageCode = resolveCurrentTaskStage(runtimeTask.getTaskDefinitionKey(),
                file.getStatus());
        if (!StrUtil.equals(file.getStatus(), toPendingStatus(stageCode))) {
            return false;
        }
        DccControlledFileRouteSnapshotDO stageSnapshot = routeSnapshotMapper.selectListByControlledFileId(file.getId())
                .stream()
                .filter(snapshot -> StrUtil.equals(snapshot.getStageCode(), stageCode.getCode()))
                .findFirst()
                .orElse(null);
        if (stageSnapshot == null) {
            return false;
        }
        Set<Long> resolvedUserIds = parseResolvedUserIds(stageSnapshot);
        if (!resolvedUserIds.contains(userId)) {
            return false;
        }
        try {
            return !resolvedUserIds.contains(Long.valueOf(runtimeTask.getAssignee()));
        } catch (NumberFormatException ex) {
            return true;
        }
    }

    private void validateStagePermission(Long userId, DccControlledFileStageCodeEnum stageCode) {
        String requiredPermission = switch (stageCode) {
            case APPLICANT_REWORK -> SUBMIT_PERMISSION;
            case DOC_CONTROL_REVIEW, MATRIX_REVIEW -> REVIEW_PERMISSION;
            case MATRIX_APPROVAL, DOC_CONTROL_APPROVAL -> APPROVE_PERMISSION;
        };
        if (!permissionApi.hasAnyPermissions(userId, requiredPermission)) {
            throw exception(CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED);
        }
    }

    private String syncStatusAfterApprove(DccControlledFileDO file, DccControlledFileStageCodeEnum currentStageCode,
                                          Set<String> beforeRunningTaskIds, String currentTaskId,
                                          String currentTaskDefinitionKey) {
        List<DccControlledFileStageCodeEnum> configuredStageCodes = routeSnapshotMapper.selectListByControlledFileId(file.getId()).stream()
                .sorted(Comparator.comparing(DccControlledFileRouteSnapshotDO::getStageOrder,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(DccControlledFileRouteSnapshotDO::getStageNo, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(DccControlledFileRouteSnapshotDO::getId, Comparator.nullsLast(Long::compareTo)))
                .map(DccControlledFileRouteSnapshotDO::getStageCode)
                .filter(StrUtil::isNotBlank)
                .map(this::resolveTaskStage)
                .distinct()
                .toList();
        int currentStageIndex = configuredStageCodes.indexOf(currentStageCode);
        if (currentStageCode == DccControlledFileStageCodeEnum.APPLICANT_REWORK) {
            return syncStatusAfterApplicantReworkApprove(file, configuredStageCodes);
        }
        if (currentStageIndex < 0) {
            throw exception(CONTROLLED_FILE_TASK_STAGE_UNSUPPORTED);
        }
        List<Task> runningTasks = bpmTaskService.getRunningTaskListByProcessInstanceId(file.getProcessInstanceId(), null, null);
        if (runningTasks.isEmpty()) {
            if (isThreeWorkflowUploadOrRevision(file)
                    && currentStageCode == DccControlledFileStageCodeEnum.MATRIX_APPROVAL) {
                DccControlledFileStageCodeEnum nextStageCode =
                        resolveNextConfiguredStage(configuredStageCodes, currentStageIndex);
                String nextStatus = resolveNextStatusAfterApprove(file, currentStageCode, nextStageCode);
                if (nextStatus == null) {
                    throw exception(CONTROLLED_FILE_TASK_STAGE_UNSUPPORTED);
                }
                if (!StrUtil.equals(nextStatus, file.getStatus())) {
                    updateWorkflowStatusAfterApprove(file, nextStatus, true);
                }
                return nextStatus;
            }
            if (currentStageIndex != configuredStageCodes.size() - 1) {
                throw exception(CONTROLLED_FILE_TASK_STAGE_UNSUPPORTED);
            }
            // The publish listener owns the terminal status transition. Persisting FINALIZING here
            // can win the commit race and overwrite the listener's later ACTIVE update.
            return resolveLatestStatus(file);
        }
        boolean explicitStageTasks = runningTasks.stream()
                .map(Task::getTaskDefinitionKey)
                .allMatch(this::isExplicitStageTaskDefinitionKey);
        boolean genericCurrentTask = StrUtil.equalsIgnoreCase(currentTaskDefinitionKey, "approveTask");
        if (explicitStageTasks && !genericCurrentTask) {
            LinkedHashSet<DccControlledFileStageCodeEnum> runningStageCodes = runningTasks.stream()
                    .map(task -> resolveTaskStage(task.getTaskDefinitionKey()))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (runningStageCodes.size() != 1) {
                throw exception(CONTROLLED_FILE_TASK_STAGE_UNSUPPORTED);
            }
            DccControlledFileStageCodeEnum runningStageCode = runningStageCodes.iterator().next();
            DccControlledFileStageCodeEnum expectedStageCode = runningStageCode == currentStageCode
                    ? currentStageCode
                    : resolveNextConfiguredStage(configuredStageCodes, currentStageIndex);
            if (runningStageCode != expectedStageCode) {
                throw exception(CONTROLLED_FILE_TASK_STAGE_UNSUPPORTED);
            }
            String nextStatus = resolveNextStatusAfterApprove(file, currentStageCode, runningStageCode);
            if (nextStatus == null) {
                throw exception(CONTROLLED_FILE_TASK_STAGE_UNSUPPORTED);
            }
            if (!StrUtil.equals(nextStatus, file.getStatus()) || runningStageCode != currentStageCode) {
                updateWorkflowStatusAfterApprove(file, nextStatus,
                        currentStageCode == DccControlledFileStageCodeEnum.MATRIX_APPROVAL
                                && runningStageCode != currentStageCode);
            }
            return nextStatus;
        }
        Set<String> previousSiblingTaskIds = beforeRunningTaskIds.stream()
                .filter(taskId -> !StrUtil.equals(taskId, currentTaskId))
                .collect(Collectors.toSet());
        boolean stillInCurrentStage = runningTasks.stream()
                .map(Task::getId)
                .anyMatch(previousSiblingTaskIds::contains);
        if (stillInCurrentStage) {
            return file.getStatus();
        }
        DccControlledFileStageCodeEnum nextStageCode = resolveNextConfiguredStage(configuredStageCodes, currentStageIndex);
        String nextStatus = resolveNextStatusAfterApprove(file, currentStageCode, nextStageCode);
        if (nextStatus == null) {
            throw exception(CONTROLLED_FILE_TASK_STAGE_UNSUPPORTED);
        }
        if (!StrUtil.equals(nextStatus, file.getStatus())) {
            updateWorkflowStatusAfterApprove(file, nextStatus,
                    currentStageCode == DccControlledFileStageCodeEnum.MATRIX_APPROVAL);
        }
        return nextStatus;
    }

    private void updateWorkflowStatusAfterApprove(DccControlledFileDO file, String nextStatus,
                                                  boolean contentApprovalCompleted) {
        DccControlledFileDO update = DccControlledFileDO.builder()
                .id(file.getId())
                .status(nextStatus)
                .build();
        if (contentApprovalCompleted && file.getApprovedTime() == null) {
            LocalDateTime approvedTime = LocalDateTime.now();
            update.setApprovedTime(approvedTime);
            file.setApprovedTime(approvedTime);
        }
        if (controlledFileMapper.updateById(update) != 1) {
            throw exception(CONTROLLED_FILE_TASK_STAGE_UNSUPPORTED);
        }
        file.setStatus(nextStatus);
        if (DccOfflineTrainingRecordService.STATUS.equals(nextStatus) && isThreeWorkflowUploadOrRevision(file)) {
            offlineTraining.notifyWaiting(file);
        }
    }

    private String resolveLatestStatus(DccControlledFileDO file) {
        DccControlledFileDO latest = controlledFileMapper.selectById(file.getId());
        if (latest == null || StrUtil.isBlank(latest.getStatus())) {
            throw exception(CONTROLLED_FILE_TASK_STAGE_UNSUPPORTED);
        }
        return latest.getStatus();
    }

    private String syncStatusAfterApplicantReworkApprove(DccControlledFileDO file,
                                                         List<DccControlledFileStageCodeEnum> configuredStageCodes) {
        if (configuredStageCodes.isEmpty()) {
            throw exception(CONTROLLED_FILE_TASK_STAGE_UNSUPPORTED);
        }
        List<Task> runningTasks = bpmTaskService.getRunningTaskListByProcessInstanceId(
                file.getProcessInstanceId(), null, null);
        if (runningTasks.isEmpty()) {
            throw exception(CONTROLLED_FILE_TASK_STAGE_UNSUPPORTED);
        }
        DccControlledFileStageCodeEnum nextStageCode = resolveApplicantReworkNextStage(
                runningTasks, configuredStageCodes.get(0));
        String nextStatus = toPendingStatus(nextStageCode);
        if (nextStatus == null) {
            throw exception(CONTROLLED_FILE_TASK_STAGE_UNSUPPORTED);
        }
        if (!StrUtil.equals(nextStatus, file.getStatus())) {
            controlledFileMapper.updateById(DccControlledFileDO.builder()
                    .id(file.getId())
                    .status(nextStatus)
                    .build());
        }
        return nextStatus;
    }

    private DccControlledFileStageCodeEnum resolveApplicantReworkNextStage(List<Task> runningTasks,
                                                                           DccControlledFileStageCodeEnum firstConfiguredStage) {
        LinkedHashSet<DccControlledFileStageCodeEnum> runningStageCodes = runningTasks.stream()
                .map(Task::getTaskDefinitionKey)
                .filter(this::isApprovalStageTaskDefinitionKey)
                .map(this::resolveTaskStage)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (runningStageCodes.size() > 1) {
            throw exception(CONTROLLED_FILE_TASK_STAGE_UNSUPPORTED);
        }
        if (runningStageCodes.size() == 1) {
            return runningStageCodes.iterator().next();
        }
        boolean genericApproveTasks = runningTasks.stream()
                .allMatch(task -> StrUtil.equalsIgnoreCase(task.getTaskDefinitionKey(), "approveTask"));
        if (genericApproveTasks) {
            return firstConfiguredStage;
        }
        throw exception(CONTROLLED_FILE_TASK_STAGE_UNSUPPORTED);
    }

    private String resolveNextStatusAfterApprove(DccControlledFileDO file,
                                                 DccControlledFileStageCodeEnum currentStageCode,
                                                 DccControlledFileStageCodeEnum nextStageCode) {
        if (isThreeWorkflowUploadOrRevision(file)
                && currentStageCode == DccControlledFileStageCodeEnum.MATRIX_APPROVAL
                && nextStageCode == DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW) {
            if (Boolean.TRUE.equals(file.getNeedTraining()) && file.getTrainingRecordFileId() == null) {
                return DccControlledFileStatusEnum.PENDING_APPLICANT_TRAINING_RECORD.getStatus();
            }
            return DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus();
        }
        if (currentStageCode == DccControlledFileStageCodeEnum.MATRIX_APPROVAL
                && nextStageCode == DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL
                && !BPM_PROCESS_DEFINITION_KEY.equals(file.getProcessDefinitionKey())
                && Boolean.TRUE.equals(file.getNeedTraining())
                && file.getTrainingRecordFileId() == null) {
            return DccControlledFileStatusEnum.PENDING_APPLICANT_TRAINING_RECORD.getStatus();
        }
        return toPendingStatus(nextStageCode);
    }

    private boolean isThreeWorkflowUploadOrRevision(DccControlledFileDO file) {
        return file != null && (DccControlledFileProcessDefinitionKeys.UPLOAD.equals(file.getProcessDefinitionKey())
                || DccControlledFileProcessDefinitionKeys.REVISION.equals(file.getProcessDefinitionKey()));
    }

    private boolean isThreeWorkflowUploadOrRevision(String processDefinitionKey) {
        return DccControlledFileProcessDefinitionKeys.UPLOAD.equals(processDefinitionKey)
                || DccControlledFileProcessDefinitionKeys.REVISION.equals(processDefinitionKey);
    }

    private void rejectManualSignoffUsersForThreeWorkflow(String processDefinitionKey,
                                                          List<Long> selectedSignoffUserIds) {
        if (isThreeWorkflowUploadOrRevision(processDefinitionKey)) {
            rejectManualSignoffUsers(selectedSignoffUserIds);
        }
    }

    private void rejectManualSignoffUsers(List<Long> selectedSignoffUserIds) {
        if (selectedSignoffUserIds != null && !selectedSignoffUserIds.isEmpty()) {
            throw exception(CONTROLLED_FILE_ROUTE_NOT_READY, "会签人由部门矩阵负责人配置，提交请求不得手选会签人");
        }
    }

    private DccControlledFileStageCodeEnum resolveNextConfiguredStage(List<DccControlledFileStageCodeEnum> configuredStageCodes,
                                                                      int currentStageIndex) {
        if (currentStageIndex < 0 || currentStageIndex >= configuredStageCodes.size() - 1) {
            throw exception(CONTROLLED_FILE_TASK_STAGE_UNSUPPORTED);
        }
        return configuredStageCodes.get(currentStageIndex + 1);
    }

    private String toPendingStatus(Integer stageNo) {
        if (stageNo == null) {
            return null;
        }
        return switch (stageNo) {
            case 1 -> DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus();
            case 2 -> DccControlledFileStatusEnum.PENDING_MATRIX_REVIEW.getStatus();
            case 3 -> DccControlledFileStatusEnum.PENDING_MATRIX_APPROVAL.getStatus();
            case 4 -> DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL.getStatus();
            default -> null;
        };
    }

    private String toPendingStatus(DccControlledFileStageCodeEnum stageCode) {
        if (stageCode == null) {
            return null;
        }
        return switch (stageCode) {
            case APPLICANT_REWORK -> DccControlledFileStatusEnum.PENDING_APPLICANT_REWORK.getStatus();
            case DOC_CONTROL_REVIEW -> DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus();
            case MATRIX_REVIEW -> DccControlledFileStatusEnum.PENDING_MATRIX_REVIEW.getStatus();
            case MATRIX_APPROVAL -> DccControlledFileStatusEnum.PENDING_MATRIX_APPROVAL.getStatus();
            case DOC_CONTROL_APPROVAL -> DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL.getStatus();
        };
    }

    private DccControlledFileStageCodeEnum resolveTaskStage(String taskDefinitionKey) {
        return Arrays.stream(DccControlledFileStageCodeEnum.values())
                .filter(stageCode -> StrUtil.equals(stageCode.getCode(), taskDefinitionKey))
                .findFirst()
                .orElseThrow(() -> exception(CONTROLLED_FILE_TASK_STAGE_UNSUPPORTED));
    }

    private boolean isExplicitStageTaskDefinitionKey(String taskDefinitionKey) {
        return Arrays.stream(DccControlledFileStageCodeEnum.values())
                .anyMatch(stageCode -> StrUtil.equals(stageCode.getCode(), taskDefinitionKey));
    }

    private boolean isApprovalStageTaskDefinitionKey(String taskDefinitionKey) {
        return Arrays.stream(DccControlledFileStageCodeEnum.values())
                .filter(stageCode -> stageCode != DccControlledFileStageCodeEnum.APPLICANT_REWORK)
                .anyMatch(stageCode -> StrUtil.equals(stageCode.getCode(), taskDefinitionKey));
    }

    private DccControlledFileStageCodeEnum resolveCurrentTaskStage(String taskDefinitionKey, String currentFileStatus) {
        if (Arrays.stream(DccControlledFileStageCodeEnum.values()).anyMatch(stageCode -> StrUtil.equals(stageCode.getCode(), taskDefinitionKey))) {
            return resolveTaskStage(taskDefinitionKey);
        }
        DccControlledFileStageCodeEnum stageFromStatus = resolveStageCodeByStatus(currentFileStatus);
        if (stageFromStatus != null && StrUtil.equalsIgnoreCase(taskDefinitionKey, "approveTask")) {
            return stageFromStatus;
        }
        throw exception(CONTROLLED_FILE_TASK_STAGE_UNSUPPORTED);
    }

    private DccControlledFileStageCodeEnum resolveStageCodeByStatus(String status) {
        if (!StrUtil.isNotBlank(status)) {
            return null;
        }
        return switch (status) {
            case "PENDING_APPLICANT_REWORK" -> DccControlledFileStageCodeEnum.APPLICANT_REWORK;
            case "PENDING_DOC_CONTROL_REVIEW" -> DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW;
            case "PENDING_MATRIX_REVIEW" -> DccControlledFileStageCodeEnum.MATRIX_REVIEW;
            case "PENDING_MATRIX_APPROVAL" -> DccControlledFileStageCodeEnum.MATRIX_APPROVAL;
            case "PENDING_DOC_CONTROL_APPROVAL" -> DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL;
            default -> null;
        };
    }

    private Set<Long> parseResolvedUserIds(DccControlledFileRouteSnapshotDO snapshot) {
        return parseResolvedUserIdsInOrder(snapshot);
    }

    private LinkedHashSet<Long> parseResolvedUserIdsInOrder(DccControlledFileRouteSnapshotDO snapshot) {
        if (snapshot == null || StrUtil.isBlank(snapshot.getResolvedUserIds())) {
            return new LinkedHashSet<>();
        }
        return Arrays.stream(snapshot.getResolvedUserIds().split(","))
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .map(Long::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private DccControlledFileRespVO toRespVO(DccControlledFileDO file) {
        DccControlledFileRespVO respVO = new DccControlledFileRespVO();
        respVO.setId(file.getId());
        respVO.setMasterId(file.getMasterId());
        respVO.setCategoryId(file.getCategoryId());
        respVO.setDirectoryId(file.getDirectoryId());
        respVO.setTitle(file.getTitle());
        respVO.setFileName(file.getFileName());
        respVO.setFileNumber(file.getFileNumber());
        respVO.setProductMasterId(file.getProductMasterId());
        respVO.setVersionNo(file.getVersionNo());
        respVO.setProductCode(file.getProductCode());
        respVO.setProductName(file.getProductName());
        respVO.setDccProjectCodeId(file.getDccProjectCodeId());
        respVO.setFileTypeTaxonomyId(file.getFileTypeTaxonomyId());
        respVO.setFileTypeLevel1(file.getFileTypeLevel1());
        respVO.setFileTypeLevel2(file.getFileTypeLevel2());
        respVO.setFileTypeLevel3(file.getFileTypeLevel3());
        respVO.setFileTypeLevel4(file.getFileTypeLevel4());
        respVO.setFileTypeLevel5(file.getFileTypeLevel5());
        respVO.setNeedTraining(file.getNeedTraining());
        respVO.setProcessType(file.getProcessType());
        respVO.setEffectiveDate(file.getEffectiveDate());
        respVO.setRemark(file.getRemark());
        respVO.setStatus(file.getStatus());
        respVO.setRequesterId(file.getRequesterId());
        respVO.setProcessInstanceId(file.getProcessInstanceId());
        respVO.setProcessDefinitionKey(file.getProcessDefinitionKey());
        respVO.setSubmittedTime(file.getSubmittedTime());
        respVO.setApprovedTime(file.getApprovedTime());
        respVO.setRejectedTime(file.getRejectedTime());
        respVO.setStampedTime(file.getStampedTime());
        respVO.setRejectReason(file.getRejectReason());
        return respVO;
    }

    private DccControlledFileRouteSnapshotRespVO toSnapshotRespVO(DccControlledFileRouteSnapshotDO snapshot) {
        DccControlledFileRouteSnapshotRespVO respVO = new DccControlledFileRouteSnapshotRespVO();
        respVO.setId(snapshot.getId());
        respVO.setRouteVersionNo(snapshot.getRouteVersionNo());
        respVO.setStageNo(snapshot.getStageNo());
        respVO.setStageCode(snapshot.getStageCode());
        respVO.setStageName(snapshot.getStageName());
        respVO.setStageOrder(snapshot.getStageOrder());
        respVO.setCandidateSourceType(snapshot.getCandidateSourceType());
        respVO.setCandidateSourceId(snapshot.getCandidateSourceId());
        respVO.setCandidateSourceIds(readCandidateSourceIds(snapshot.getCandidateSourceIds(), snapshot.getCandidateSourceId()));
        respVO.setApproveMethod(snapshot.getApproveMethod());
        respVO.setApproveRatio(snapshot.getApproveRatio());
        respVO.setRequireAllApprovals(snapshot.getRequireAllApprovals());
        respVO.setResolvedUserIds(StrUtil.isBlank(snapshot.getResolvedUserIds())
                ? List.of()
                : Arrays.stream(snapshot.getResolvedUserIds().split(",")).map(Long::valueOf).toList());
        return respVO;
    }

    private record ValidatedTaskActionContext(DccControlledFileDO file, DccControlledFileStageCodeEnum stageCode,
                                              String taskDefinitionKey,
                                              DccControlledFileRouteSnapshotDO stageSnapshot, String taskId) {
    }

    private record ValidatedReturnTarget(String taskDefinitionKey, String pendingStatus) {
    }

    private record DocControlApprovalArtifacts(Long stampedPdfFileId, String sessionId,
                                               String stampedPdfUploadTicket,
                                               Long confirmedDirectoryId,
                                               List<ResolvedDistributionPlan> distributionPlans) {
    }

    private record DocControlApprovalReadinessEvaluation(DccControlledFileTaskReadinessRespVO readiness,
                                                         DocControlApprovalArtifacts artifacts) {

        private void requireReady() {
            if (Boolean.TRUE.equals(readiness.getReady())) {
                return;
            }
            String blockerMessages = readiness.getBlockers().stream()
                    .map(DccControlledFileTaskReadinessBlockerRespVO::getMessage)
                    .collect(Collectors.joining("；"));
            throw exception(CONTROLLED_FILE_FINAL_APPROVAL_NOT_READY, blockerMessages);
        }

    }

    private record ResolvedDistributionPlan(Long departmentId, String distributionMedium,
                                            List<Long> recipientUserIds) {
    }

    private List<Long> readCandidateSourceIds(String candidateSourceIds, Long fallbackId) {
        if (StrUtil.isNotBlank(candidateSourceIds)) {
            return Arrays.stream(candidateSourceIds.split(","))
                    .map(String::trim)
                    .filter(StrUtil::isNotBlank)
                    .map(Long::valueOf)
                    .toList();
        }
        return fallbackId == null ? List.of() : List.of(fallbackId);
    }

    private String joinIds(List<Long> ids) {
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private Map<String, List<Long>> buildStageAssigneeMapFromSnapshots(List<DccControlledFileRouteSnapshotDO> snapshots) {
        return snapshots.stream()
                .filter(snapshot -> StrUtil.isNotBlank(snapshot.getStageCode()))
                .collect(Collectors.toMap(DccControlledFileRouteSnapshotDO::getStageCode,
                        snapshot -> List.copyOf(parseResolvedUserIds(snapshot)), (left, right) -> left, HashMap::new));
    }
}
