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

import java.time.LocalDate;
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
    @Resource private DccApprovalMatrixTokenService approvalMatrixTokens;
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
    @Resource private DccControlledFileLifecycleService lifecycleService;
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
    @Resource private DccApprovalMatrixBaselineService approvalMatrixBaselines;
    @Resource private DccApprovalMatrixSourceService approvalMatrixSources;
    private final DccApplicationCustomMatrixService customMatrices=new DccApplicationCustomMatrixService();
    @Resource private cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationStageSnapshotService relationStageSnapshots;
    @Resource private DccApplicationExecutionContractService executionContracts;
    @Resource private DccLifecycleEffectiveContractResolver effectiveContractResolver;
    @Resource private org.flowable.engine.RuntimeService runtimeService;

    @Override
    @Transactional(rollbackFor=Exception.class)
    public DccApplicationExecutionContractService.Prepared prepareLegacyWorkingApplication(Long actorId,Long fileId,String processDefinitionId){
        var file=requireEditableWorkingApplication(actorId,fileId);
        return executionContracts.prepareLegacy(actorId,file,processDefinitionId);
    }

    @Override
    @Transactional(rollbackFor=Exception.class)
    public DccControlledFileRouteReadinessRespVO previewRoute(Long actorId,
            cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRoutePreviewReqVO request){
        if(request==null)throw exception(CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING);
        validateCategory(request.getCategoryId());rejectManualSignoffUsers(request.getSelectedSignoffUserIds());
        if(!DccApplicationExecutionContractService.isPublicContract(request.getExecutionContract()))
            throw new IllegalArgumentException("EXECUTION_CONTRACT_ORDINARY_REQUIRES_PUBLIC");
        DccControlledFileDO existingFile=null;
        if(request.getControlledFileId()!=null){existingFile=requireWorkingApplication(actorId,request.getControlledFileId(),true);
            if(!Objects.equals(existingFile.getCategoryId(),request.getCategoryId())||!Objects.equals(existingFile.getDccProjectCodeId(),request.getDccProjectCodeId())
                    ||!Objects.equals(existingFile.getChangeType(),request.getActionType()))throw new IllegalArgumentException("APPROVAL_MATRIX_DRAFT_IDENTITY_INVALID");}
        else projectAccessService.assertProjectEditorOrOwner(actorId,request.getDccProjectCodeId());
        if("APPLICATION_CUSTOM".equals(applicationSourceMode(existingFile,request.getCategoryId())))
            return previewCustomRoute(actorId,existingFile,request);
        var eligibility=newActionBaselineEligibility(actorId,existingFile);
        var inheritedPreview=existingFile!=null&&existingFile.getReworkPredecessorControlledFileId()!=null?readNativeReworkRoute(actorId,existingFile,request.getSelectedSignoffDepartmentIds()):null;
        var comparison=inheritedPreview!=null?nativeReworkComparison(actorId,existingFile,inheritedPreview,request.getApplicationContextKey()):Boolean.TRUE.equals(request.getPrepareNewActionBaseline())?approvalMatrixBaselines.previewNewAction(actorId,request):approvalMatrixBaselines.preview(actorId,request);
        var response=DccControlledFileRouteReadinessRespVO.builder().ready(false).nodes(List.of()).blockers(List.of())
                .routeId(comparison.after().baseline().getRouteId()).routeVersionNo(comparison.after().baseline().getRouteVersionNo())
                .matrixDigest(comparison.after().baseline().getMatrixDigest()).approvalMatrix(comparison.after().baseline())
                .matrixBefore(comparison.before()).matrixAfter(comparison.after().baseline()).matrixChangeRequired(comparison.changed())
                .approvalMatrixBaselineToken(comparison.baselineToken()).approvalMatrixConfirmationToken(comparison.confirmationToken())
                .derivationVersion(cn.iocoder.yudao.module.dcc.service.route.DccPublicMatrixNativeDerivation.VERSION)
                .applicationActionType(request.getActionType()).build();
        response.setExecutionContract(DccApplicationExecutionContractService.PUBLIC);
        response.setNewActionBaselineRequired(eligibility.required());response.setNewActionBaselinePreparationAllowed(eligibility.allowed());
        response.setRuntimeCapabilityLimitations(List.of("多个多候选ANY批准组需要独立组BPM执行，当前不可合成为ANY",
                "可选批准规则必须有本次明确选择或正式条件，目前请求未承载选择事实",
                "会签非DEPT主体不可冒充部门义务，需正式主体到义务映射",
                "非100%批准比例缺当前冻结签名聚合与模型合同，保留原比例并明确拒绝"));
        try{
            var prepared=existingFile!=null?executionContracts.requirePrepared(existingFile,request.getExecutionContract(),request.getExpectedProcessDefinitionId(),request.getExpectedDefinitionDigest(),
                    "NEW".equals(request.getActionType())?DccControlledFileProcessDefinitionKeys.UPLOAD:DccControlledFileProcessDefinitionKeys.REVISION):
                    executionContracts.preparePublicPreview("NEW".equals(request.getActionType())?DccControlledFileProcessDefinitionKeys.UPLOAD:
                    "REVISION".equals(request.getActionType())?DccControlledFileProcessDefinitionKeys.REVISION:DccControlledFileProcessDefinitionKeys.OBSOLETE);
            response.setPreparedProcessDefinitionId(prepared.processDefinitionId());response.setPreparedDefinitionDigest(prepared.definitionDigest());
            response.setExecutionContract(prepared.executionContract());
        }catch(IllegalStateException unknownDefinition){
            response.setBlockers(List.of(cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRouteReadinessBlockerRespVO.builder()
                    .reasonCode("EXECUTION_CONTRACT_NEW_DEFINITION_NOT_READY").message(unknownDefinition.getMessage()).build()));
            return response;
        }
        try{
            var nativeSource=inheritedPreview==null?approvalRouteAssigneeResolver.resolvePublicNative(comparison.after(),actorId,request.getActionType(),request.getControlledFileId(),request.getDccProjectCodeId(),request.getApplicationContextKey(),response.getPreparedProcessDefinitionId(),response.getPreparedDefinitionDigest(),request.getSelectedSignoffDepartmentIds()):approvalRouteAssigneeResolver.attachPublicNative(inheritedPreview,actorId,request.getActionType(),request.getControlledFileId(),request.getDccProjectCodeId(),request.getApplicationContextKey(),response.getPreparedProcessDefinitionId(),response.getPreparedDefinitionDigest(),request.getSelectedSignoffDepartmentIds());
            var route=nativeSource.route();response.setSupervisorSourceDigest(nativeSource.sourceDigest());response.setSupervisorSourceToken(nativeSource.sourceToken());
            var evaluation=routeReadinessService.evaluateFrozenRoute(route).response();
            response.setNodes(evaluation.getNodes());response.setBlockers(evaluation.getBlockers());response.setReady(evaluation.getReady());
            response.setRuntimeRuleGroups(route.runtimeGroups());
        }catch(DccUserManagementDirectSupervisorSource.RelationUnavailableException unavailable){
            response.setBlockers(List.of(cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRouteReadinessBlockerRespVO.builder()
                    .reasonCode(unavailable.getReasonCode()).message(unavailable.getMessage()).build()));response.setReady(false);
        }catch(DccControlledFileApprovalRouteAssigneeResolver.NativeSupervisorNotReadyException absent){
            response.setBlockers(List.of(cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRouteReadinessBlockerRespVO.builder().reasonCode("DIRECT_SUPERVISOR_SOURCE_NOT_CONFIGURED").message(absent.getMessage()).build()));response.setReady(false);
        }catch(ServiceException unsupported){
            if(!Objects.equals(unsupported.getCode(),CONTROLLED_FILE_ROUTE_NOT_READY.getCode()))throw unsupported;
            response.setBlockers(List.of(cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRouteReadinessBlockerRespVO.builder().reasonCode("PUBLIC_MATRIX_DERIVATION_UNSUPPORTED")
                    .message(unsupported.getMessage()).build()));
        }
        return response;
    }

    private String applicationSourceMode(DccControlledFileDO file,Long categoryId){
        if(file!=null&&file.getDraftApprovalSourceMode()==null&&file.getPreparedProcessDefinitionId()!=null&&file.getPreparedDefinitionDigest()!=null){
            if(DccApplicationExecutionContractService.LEGACY.equals(file.getPreparedWorkflowContract())){
                executionContracts.requirePrepared(file,file.getPreparedWorkflowContract(),file.getPreparedProcessDefinitionId(),file.getPreparedDefinitionDigest(),"NEW".equals(file.getChangeType())?DccControlledFileProcessDefinitionKeys.UPLOAD:DccControlledFileProcessDefinitionKeys.REVISION);
                return "PUBLIC_MATRIX";
            }
            if(file.getDraftApprovalMatrixJson()!=null){
                var frozen=DccApprovalMatrixSourceService.readCanonical(file.getDraftApprovalMatrixJson());
                if(!Objects.equals(frozen.getTenantId(),file.getTenantId())||!Objects.equals(frozen.getCategoryId(),categoryId)||!Objects.equals(frozen.getFileTypeTaxonomyId(),file.getFileTypeTaxonomyId()))throw new IllegalArgumentException("PUBLIC_SAVED_SOURCE_IDENTITY_INVALID");
                executionContracts.requirePrepared(file,file.getPreparedWorkflowContract(),file.getPreparedProcessDefinitionId(),file.getPreparedDefinitionDigest(),"NEW".equals(file.getChangeType())?DccControlledFileProcessDefinitionKeys.UPLOAD:DccControlledFileProcessDefinitionKeys.REVISION);
                return "PUBLIC_MATRIX";
            }
            if(file.getReworkPredecessorControlledFileId()!=null){
                var original=controlledFileMapper.selectById(file.getReworkPredecessorControlledFileId());
                if(original==null||!Objects.equals(original.getTenantId(),file.getTenantId())||!Objects.equals(original.getMasterId(),file.getMasterId())||!Objects.equals(original.getDccProjectCodeId(),file.getDccProjectCodeId())||!Objects.equals(original.getCategoryId(),categoryId)||!Objects.equals(original.getFileTypeTaxonomyId(),file.getFileTypeTaxonomyId()))throw new IllegalArgumentException("PUBLIC_REWORK_SOURCE_IDENTITY_INVALID");
                if(!"APPLICATION_CUSTOM".equals(original.getDraftApprovalSourceMode())){
                    var exact=executionContracts.resolveBoundExecution(original);if(exact.active())throw new IllegalArgumentException("PUBLIC_REWORK_ENDED_SOURCE_REQUIRED");
                    approvalRouteAssigneeResolver.restoreFrozenRoute(original,routeSnapshotMapper.selectListByControlledFileId(original.getId()));return "PUBLIC_MATRIX";
                }
            }
        }
        var category=validateCategory(categoryId);String current=DccFileCategoryDO.requireApprovalSourceMode(category.getApprovalSourceMode());
        if(file!=null&&file.getDraftApprovalSourceMode()!=null){
            String saved=DccFileCategoryDO.requireApprovalSourceMode(file.getDraftApprovalSourceMode());
            if(!saved.equals(current))throw new IllegalArgumentException("APPLICATION_APPROVAL_SOURCE_CHANGED：本申请保存的审批来源与当前类别不同，请明确调整文件类型后重新匹配，不能自动转换");
            return saved;
        }
        if(file!=null&&file.getDraftApprovalMatrixJson()!=null&&"APPLICATION_CUSTOM".equals(current))throw new IllegalArgumentException("APPLICATION_APPROVAL_SOURCE_CHANGED：原申请为公共矩阵，不能按当前类别自动转自定义");
        return current;
    }
    private DccApplicationCustomMatrixService.PreviewSourceContext customPreviewContext(Long actor,Long project,Long category,Long taxonomy,String action,String key,DccApplicationExecutionContractService.Prepared prepared){
        return new DccApplicationCustomMatrixService.PreviewSourceContext(DccApplicationCustomMatrix.ApprovalSourceMode.APPLICATION_CUSTOM,TenantContextHolder.getRequiredTenantId(),project,category,taxonomy,actor,"NEW".equals(action)?"UPLOAD":"REVISION",key,prepared.processDefinitionId(),prepared.definitionDigest());
    }
    private DccApplicationCustomMatrixService.SourceContext customReservedContext(DccControlledFileDO file,String key){
        return new DccApplicationCustomMatrixService.SourceContext(DccApplicationCustomMatrix.ApprovalSourceMode.APPLICATION_CUSTOM,file.getTenantId(),file.getDccProjectCodeId(),file.getCategoryId(),file.getFileTypeTaxonomyId(),file.getRequesterId(),file.getId(),applicationRoundService.requireDraft(workingApplicationType(file),file.getId()),workingApplicationType(file),key,file.getPreparedProcessDefinitionId(),file.getPreparedDefinitionDigest());
    }
    private DccApplicationCustomMatrixService.SavedSelection readCustomDraft(DccControlledFileDO file,DccApplicationCustomMatrixService.SourceContext context){
        if(file.getDraftApplicationCustomMatrixJson()==null)throw new IllegalArgumentException("CUSTOM_APPLICATION_SAVED_CONFIGURATION_REQUIRED：本次自定义配置尚未保存");
        String hash=DigestUtil.sha256Hex(file.getDraftApplicationCustomMatrixJson());
        return customMatrices.readCanonical(file.getDraftApplicationCustomMatrixJson(),hash,context);
    }
    private DccApplicationCustomMatrixService.CanonicalSelection saveCustomDraft(DccControlledFileDO file,DccApplicationCustomMatrix matrix,List<Long> departments,String key,String digest,String token,String phase){
        requireCustomReworkSelection(file,matrix,departments);
        if(!"RESERVED".equals(phase))throw new IllegalArgumentException("CUSTOM_APPLICATION_RESERVED_PROOF_REQUIRED");
        if(file.getDraftApplicationContextKey()!=null&&!Objects.equals(file.getDraftApplicationContextKey(),key))throw new IllegalArgumentException("CUSTOM_APPLICATION_CONTEXT_CHANGED");
        var context=customReservedContext(file,key);var selection=customMatrices.canonicalize(context,matrix,departments);
        if(!Objects.equals(selection.digest(),digest))throw new IllegalArgumentException("CUSTOM_APPLICATION_CONFIGURATION_EXPECTATION_CHANGED");
        approvalMatrixTokens.requireCustomReserved(context,selection,token);
        persistCustomDraft(file,key,selection);return selection;
    }
    private void persistCustomDraft(DccControlledFileDO file,String key,DccApplicationCustomMatrixService.CanonicalSelection selection){
        if(controlledFileMapper.update(null,new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<DccControlledFileDO>()
                .eq(DccControlledFileDO::getId,file.getId()).eq(DccControlledFileDO::getTenantId,TenantContextHolder.getRequiredTenantId())
                .eq(DccControlledFileDO::getStatus,"WORKING").isNull(DccControlledFileDO::getProcessInstanceId)
                .set(DccControlledFileDO::getDraftApprovalSourceMode,"APPLICATION_CUSTOM")
                .set(DccControlledFileDO::getDraftApplicationCustomMatrixJson,selection.canonical())
                .set(DccControlledFileDO::getDraftApplicationContextKey,key))!=1)throw new IllegalStateException("CUSTOM_APPLICATION_DRAFT_WRITE_FAILED");
        file.setDraftApprovalSourceMode("APPLICATION_CUSTOM");file.setDraftApplicationCustomMatrixJson(selection.canonical());file.setDraftApplicationContextKey(key);
    }
    private DccControlledFileRouteReadinessRespVO previewCustomRoute(Long actor,DccControlledFileDO file,cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRoutePreviewReqVO request){
        if(file!=null)requireCustomReworkSelection(file,request.getApplicationCustomMatrix(),request.getSelectedSignoffDepartmentIds());
        var category=validateCategory(request.getCategoryId());
        var prepared=file==null?executionContracts.preparePublicPreview("NEW".equals(request.getActionType())?DccControlledFileProcessDefinitionKeys.UPLOAD:DccControlledFileProcessDefinitionKeys.REVISION)
                :executionContracts.requirePrepared(file,request.getExecutionContract(),request.getExpectedProcessDefinitionId(),request.getExpectedDefinitionDigest(),"NEW".equals(file.getChangeType())?DccControlledFileProcessDefinitionKeys.UPLOAD:DccControlledFileProcessDefinitionKeys.REVISION);
        DccApplicationCustomMatrixService.CanonicalSelection selection;String token,phase;
        DccApplicationCustomMatrixService.PreviewSourceContext previewContext=null;
        DccApplicationCustomMatrixService.SourceContext reservedContext=null;
        if(file==null){var context=customPreviewContext(actor,request.getDccProjectCodeId(),request.getCategoryId(),category.getFileTypeTaxonomyId(),request.getActionType(),request.getApplicationContextKey(),prepared);
            selection=customMatrices.canonicalizePreview(context,request.getApplicationCustomMatrix(),request.getSelectedSignoffDepartmentIds());token=approvalMatrixTokens.issueCustomPreview(context,selection);phase="PREVIEW";
            previewContext=context;
        }else{String key=file.getDraftApplicationContextKey()==null?request.getApplicationContextKey():file.getDraftApplicationContextKey();
            if(!Objects.equals(key,request.getApplicationContextKey()))throw new IllegalArgumentException("CUSTOM_APPLICATION_CONTEXT_CHANGED");
            var context=customReservedContext(file,key);selection=customMatrices.canonicalize(context,request.getApplicationCustomMatrix(),request.getSelectedSignoffDepartmentIds());
            token=approvalMatrixTokens.issueCustomReserved(context,selection);phase="RESERVED";reservedContext=context;}
        var evaluation=DccControlledFileRouteReadinessRespVO.builder().ready(false).nodes(List.of()).blockers(List.of()).build();
        try{
            var route=previewContext!=null?approvalRouteAssigneeResolver.resolveCustomPreview(actor,previewContext,selection):approvalRouteAssigneeResolver.resolveCustomReserved(actor,reservedContext,selection);
            evaluation=routeReadinessService.evaluateFrozenRoute(route).response();var proof=route.applicationCustomSource().proof();evaluation.setSupervisorSourceDigest(proof.sourceDigest());evaluation.setSupervisorSourceToken(proof.sourceToken());
        }catch(IllegalArgumentException incomplete){
            if(incomplete.getMessage()==null||!Set.of("CUSTOM_MATRIX_SIGNOFF_REQUIRED","CUSTOM_MATRIX_APPROVER_REQUIRED","CUSTOM_MATRIX_DOCUMENT_CONTROL_REQUIRED").contains(incomplete.getMessage().split("：",2)[0]))throw incomplete;
            evaluation.setBlockers(List.of(cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRouteReadinessBlockerRespVO.builder().reasonCode(incomplete.getMessage().split("：",2)[0]).message(incomplete.getMessage()).build()));
        }catch(DccControlledFileApprovalRouteAssigneeResolver.NativeSupervisorNotReadyException absent){
            evaluation.setBlockers(List.of(cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRouteReadinessBlockerRespVO.builder().reasonCode("DIRECT_SUPERVISOR_SOURCE_NOT_CONFIGURED").message(absent.getMessage()).build()));
        }
        evaluation.setApprovalSourceMode("APPLICATION_CUSTOM");evaluation.setApplicationCustomMatrix(request.getApplicationCustomMatrix());evaluation.setApplicationCustomMatrixCanonical(selection.canonical());evaluation.setApplicationCustomMatrixDigest(selection.digest());evaluation.setApplicationCustomMatrixToken(token);evaluation.setApplicationCustomMatrixPhase(phase);
        evaluation.setExecutionContract(prepared.executionContract());evaluation.setPreparedProcessDefinitionId(prepared.processDefinitionId());evaluation.setPreparedDefinitionDigest(prepared.definitionDigest());evaluation.setApplicationActionType(request.getActionType());evaluation.setDerivationVersion(DccApplicationCustomMatrixService.FORMAT);evaluation.setRuntimeRuleGroups(List.of());evaluation.setRuntimeCapabilityLimitations(List.of());return evaluation;
    }
    private void requireCustomReworkSelection(DccControlledFileDO target,DccApplicationCustomMatrix matrix,List<Long> departments){
        if(target.getReworkPredecessorControlledFileId()==null)return;
        var original=controlledFileMapper.selectByIdAndTenantForUpdate(target.getTenantId(),target.getReworkPredecessorControlledFileId());
        if(original==null||!Objects.equals(original.getTenantId(),target.getTenantId())||!Objects.equals(original.getMasterId(),target.getMasterId())||!Objects.equals(original.getDccProjectCodeId(),target.getDccProjectCodeId())||!Objects.equals(original.getCategoryId(),target.getCategoryId())||!Objects.equals(original.getFileTypeTaxonomyId(),target.getFileTypeTaxonomyId())||!Objects.equals(original.getRequesterId(),target.getRequesterId())||!"APPLICATION_CUSTOM".equals(original.getDraftApprovalSourceMode())||!Set.of("REJECTED","WITHDRAWN").contains(original.getStatus()))throw new IllegalArgumentException("CUSTOM_REWORK_FROZEN_SOURCE_IDENTITY_INVALID");
        var bound=executionContracts.resolveBoundExecution(original);if(bound.active())throw new IllegalArgumentException("CUSTOM_REWORK_ENDED_SOURCE_REQUIRED");
        var route=approvalRouteAssigneeResolver.restoreCustomFrozenRoute(original,routeSnapshotMapper.selectListByControlledFileId(original.getId()));
        var saved=customMatrices.readCanonical(route.applicationCustomSource().selection().canonical(),route.applicationCustomSource().selection().digest(),route.applicationCustomSource().reservedContext());
        if(!Objects.equals(saved.applicationCustomMatrix(),matrix)||departments==null||!Objects.equals(saved.selectedSignoffDepartmentIds(),departments.stream().sorted().toList()))throw new IllegalArgumentException("CUSTOM_REWORK_SAVED_CONFIGURATION_CHANGED：驳回重提必须沿用原保存的批准人、文控审核人和会签部门；主动变更类型需明确重新匹配");
    }

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
            nameClaimService.requireNumberAvailableForNewFile(normalizedFileNumber);
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
    @Transactional(rollbackFor=Exception.class)
    public void saveWorkingApplicationAttributes(Long userId,Long fileId,
            cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes actual,
            String effectiveMode,LocalDate effectiveDate,List<Long> selectedDistributionDepartmentIds) {
        if(actual==null) throw exception(CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING);
        var file=requireEditableWorkingApplication(userId,fileId);
        requireDraftEffectiveSelection(effectiveMode,effectiveDate);
        var distribution=normalizeDistributionDepartments(selectedDistributionDepartmentIds);
        projectApplicationSnapshots.saveReservedDraft(userId,file.getDccProjectCodeId(),workingApplicationType(file),fileId,actual);
        persistApplicationSelection(file,effectiveMode,effectiveDate,distribution);
    }

    @Override
    @Transactional(rollbackFor=Exception.class)
    public void saveWorkingApplication(Long actorId,Long fileId,
            cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccWorkingApplicationSaveReqVO request) {
        if(request==null || request.getActual()==null) throw exception(CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING);
        var file=requireEditableWorkingApplication(actorId,fileId);
        requireDraftEffectiveSelection(request.getEffectiveMode(),request.getEffectiveDate());
        if(request.getEffectiveMode()!=null)workflowDatePolicy.requireApplicationEffectiveConfiguration(request.getEffectiveMode(),request.getEffectiveDate(),file.getPreparedWorkflowContract());
        var signoff=normalizeDistributionDepartments(request.getSelectedSignoffDepartmentIds());
        var distribution=normalizeDistributionDepartments(request.getSelectedDistributionDepartmentIds());
        boolean custom="APPLICATION_CUSTOM".equals(applicationSourceMode(file,file.getCategoryId()));
        if(custom){
            executionContracts.requirePrepared(file,request.getExecutionContract(),request.getExpectedProcessDefinitionId(),request.getExpectedDefinitionDigest(),"NEW".equals(file.getChangeType())?DccControlledFileProcessDefinitionKeys.UPLOAD:DccControlledFileProcessDefinitionKeys.REVISION);
            saveCustomDraft(file,request.getApplicationCustomMatrix(),signoff,request.getApplicationContextKey(),request.getExpectedApplicationCustomMatrixDigest(),request.getApplicationCustomMatrixToken(),request.getApplicationCustomMatrixPhase());
        }
        if(!custom&&(request.getExpectedSupervisorSourceDigest()!=null||request.getSupervisorSourceToken()!=null)){
            var inherited=file.getReworkPredecessorControlledFileId()==null?null:readNativeReworkRoute(actorId,file,signoff);
            var prepared=executionContracts.requirePrepared(file,request.getExecutionContract(),request.getExpectedProcessDefinitionId(),request.getExpectedDefinitionDigest(),"NEW".equals(file.getChangeType())?DccControlledFileProcessDefinitionKeys.UPLOAD:DccControlledFileProcessDefinitionKeys.REVISION);
            var savedSource=inherited==null?approvalMatrixBaselines.rejectedSource(actorId,file,file.getCategoryId(),file.getDccProjectCodeId(),file.getChangeType()):null;
            var source=inherited==null?approvalRouteAssigneeResolver.resolvePublicNative(savedSource==null?approvalMatrixSources.readForUpdate(file.getCategoryId()):savedSource,actorId,file.getChangeType(),fileId,file.getDccProjectCodeId(),request.getApplicationContextKey(),prepared.processDefinitionId(),prepared.definitionDigest(),signoff):approvalRouteAssigneeResolver.attachPublicNative(inherited,actorId,file.getChangeType(),fileId,file.getDccProjectCodeId(),request.getApplicationContextKey(),prepared.processDefinitionId(),prepared.definitionDigest(),signoff);
            approvalRouteAssigneeResolver.requireNativeExpectation(source,request.getExpectedSupervisorSourceDigest(),request.getSupervisorSourceToken());
        }
        if(!custom&&Boolean.TRUE.equals(request.getPrepareNewActionBaseline()))
            approvalMatrixBaselines.initializeNewActionDraft(actorId,file,request);
        else if(!custom&&request.getApprovalMatrixBaselineToken()!=null){
            if(file.getDraftApprovalMatrixJson()==null&&DccApplicationExecutionContractService.isPublicContract(file.getPreparedWorkflowContract()))
                throw new IllegalStateException("APPROVAL_MATRIX_DRAFT_BASELINE_REQUIRED");
            approvalMatrixBaselines.initializeDraft(actorId,file,request.getApprovalMatrixBaselineToken(),request.getApplicationContextKey());
        }
        if(request.getChangeDescription()!=null && request.getChangeDescription().length()>1000)
            throw new IllegalArgumentException("申请说明超过正式长度限制");
        projectApplicationSnapshots.saveReservedDraft(actorId,file.getDccProjectCodeId(),workingApplicationType(file),fileId,request.getActual());
        if(controlledFileMapper.update(null,new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<DccControlledFileDO>()
                .eq(DccControlledFileDO::getId,fileId).eq(DccControlledFileDO::getTenantId,TenantContextHolder.getRequiredTenantId())
                .eq(DccControlledFileDO::getStatus,"WORKING").isNull(DccControlledFileDO::getProcessInstanceId)
                .set(DccControlledFileDO::getEffectiveMode,request.getEffectiveMode())
                .set(DccControlledFileDO::getEffectiveDate,request.getEffectiveDate())
                .set(DccControlledFileDO::getSelectedSignoffDepartmentIdsJson,serializeDistributionDepartments(signoff))
                .set(DccControlledFileDO::getDistributionDepartmentIdsJson,serializeDistributionDepartments(distribution))
                .set(DccControlledFileDO::getNeedTraining,request.getNeedTraining())
                .set(DccControlledFileDO::getChangeDescription,request.getChangeDescription()))!=1)
            throw new IllegalStateException("工作申请实际填写值保存失败");
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
        if(unavailable==null) unavailable=workingSelectionUnavailable(file);
        if("APPLICATION_CUSTOM".equals(file.getDraftApprovalSourceMode())){
            var context=customReservedContext(file,file.getDraftApplicationContextKey());
            var encoded=customMatrices.canonicalize(context,readCustomDraft(file,context).applicationCustomMatrix(),readDistributionDepartments(file.getSelectedSignoffDepartmentIdsJson()));
            var selection=customMatrices.readCanonical(file.getDraftApplicationCustomMatrixJson(),encoded.digest(),context);
            try{applicationSourceMode(file,file.getCategoryId());}catch(IllegalArgumentException changed){unavailable=changed.getMessage();}
            return new DccWorkingApplicationAttributes(fileId,file.getDccProjectCodeId(),type,projectAttributesService.readValue(saved.getDefaultSourceJson()),projectAttributesService.readValue(saved.getActualAttributesJson()),unavailable==null,unavailable,file.getEffectiveDate(),file.getNeedTraining(),distributionDepartmentStrings(file.getSelectedSignoffDepartmentIdsJson()),file.getChangeDescription(),file.getEffectiveMode(),distributionDepartmentStrings(file.getDistributionDepartmentIdsJson()),null,false,false,file.getPreparedWorkflowContract(),file.getPreparedProcessDefinitionId(),file.getPreparedDefinitionDigest(),"APPLICATION_CUSTOM",selection.applicationCustomMatrix(),file.getDraftApplicationContextKey(),context.attributeRound(),encoded.digest());
        }
        var eligibility=newActionBaselineEligibility(userId,file);
        if(eligibility.required())unavailable="新业务申请的公共矩阵原基线尚未正式准备";
        return new DccWorkingApplicationAttributes(fileId,file.getDccProjectCodeId(),type,
                projectAttributesService.readValue(saved.getDefaultSourceJson()),
                projectAttributesService.readValue(saved.getActualAttributesJson()),unavailable==null,unavailable,
                file.getEffectiveDate(),file.getNeedTraining(),distributionDepartmentStrings(file.getSelectedSignoffDepartmentIdsJson()),file.getChangeDescription(),
                file.getEffectiveMode(),distributionDepartmentStrings(file.getDistributionDepartmentIdsJson()),
                DccApprovalMatrixSourceService.readCanonical(file.getDraftApprovalMatrixJson()),eligibility.required(),eligibility.allowed(),
                file.getPreparedWorkflowContract(),file.getPreparedProcessDefinitionId(),file.getPreparedDefinitionDigest());
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
        if(unavailable==null) unavailable=workingSelectionUnavailable(file);
        if("APPLICATION_CUSTOM".equals(file.getDraftApprovalSourceMode())){
            var context=customReservedContext(file,file.getDraftApplicationContextKey());
            var encoded=customMatrices.canonicalize(context,readCustomDraft(file,context).applicationCustomMatrix(),readDistributionDepartments(file.getSelectedSignoffDepartmentIdsJson()));
            var selection=customMatrices.readCanonical(file.getDraftApplicationCustomMatrixJson(),encoded.digest(),context);
            try{applicationSourceMode(file,file.getCategoryId());}catch(IllegalArgumentException changed){unavailable=changed.getMessage();}
            return new DccWorkingApplicationAttributes(selectedId,file.getDccProjectCodeId(),"REVISION",
                    projectAttributesService.readValue(saved.getDefaultSourceJson()),projectAttributesService.readValue(saved.getActualAttributesJson()),
                    unavailable==null,unavailable,file.getEffectiveDate(),file.getNeedTraining(),
                    distributionDepartmentStrings(file.getSelectedSignoffDepartmentIdsJson()),file.getChangeDescription(),
                    file.getEffectiveMode(),distributionDepartmentStrings(file.getDistributionDepartmentIdsJson()),null,false,false,
                    file.getPreparedWorkflowContract(),file.getPreparedProcessDefinitionId(),file.getPreparedDefinitionDigest(),
                    "APPLICATION_CUSTOM",selection.applicationCustomMatrix(),file.getDraftApplicationContextKey(),context.attributeRound(),encoded.digest());
        }
        var eligibility=newActionBaselineEligibility(userId,file);
        if(eligibility.required())unavailable="新业务申请的公共矩阵原基线尚未正式准备";
        return new DccWorkingApplicationAttributes(selectedId,file.getDccProjectCodeId(),"REVISION",
                projectAttributesService.readValue(saved.getDefaultSourceJson()),projectAttributesService.readValue(saved.getActualAttributesJson()),
                unavailable==null,unavailable,file.getEffectiveDate(),file.getNeedTraining(),distributionDepartmentStrings(file.getSelectedSignoffDepartmentIdsJson()),file.getChangeDescription(),
                file.getEffectiveMode(),distributionDepartmentStrings(file.getDistributionDepartmentIdsJson()),
                DccApprovalMatrixSourceService.readCanonical(file.getDraftApprovalMatrixJson()),eligibility.required(),eligibility.allowed(),
                file.getPreparedWorkflowContract(),file.getPreparedProcessDefinitionId(),file.getPreparedDefinitionDigest());
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
        reqVO.setSelectedDistributionDepartmentIds(normalizeDistributionDepartments(reqVO.getSelectedDistributionDepartmentIds()));
        reqVO.setSelectedSignoffDepartmentIds(normalizeDistributionDepartments(reqVO.getSelectedSignoffDepartmentIds()));
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
                validateSubmitRequest(reqVO,true,storage==null,false);
                var publicDefinition=executionContracts.requireOrdinaryPublic(reqVO.getExecutionContract(),reqVO.getExpectedProcessDefinitionId(),
                        reqVO.getExpectedDefinitionDigest(),DccControlledFileProcessDefinitionKeys.UPLOAD);
                lockDraftProject(reqVO.getDccProjectCodeId());
                boolean custom="APPLICATION_CUSTOM".equals(applicationSourceMode(null,reqVO.getCategoryId()));
                DccApplicationCustomMatrixService.PreviewSourceContext customPreview=null;
                DccApplicationCustomMatrixService.CanonicalSelection customSelection=null;
                if(custom){
                    customPreview=customPreviewContext(userId,reqVO.getDccProjectCodeId(),reqVO.getCategoryId(),reqVO.getFileTypeTaxonomyId(),reqVO.getChangeType(),reqVO.getApplicationContextKey(),publicDefinition);
                    customSelection=customMatrices.canonicalizePreview(customPreview,reqVO.getApplicationCustomMatrix(),reqVO.getSelectedSignoffDepartmentIds());
                    if(!"PREVIEW".equals(reqVO.getApplicationCustomMatrixPhase())||!Objects.equals(reqVO.getExpectedApplicationCustomMatrixDigest(),customSelection.digest()))throw new IllegalArgumentException("CUSTOM_PREVIEW_EXPECTATION_CHANGED");
                    approvalMatrixTokens.requireCustomPreview(customPreview,customSelection,reqVO.getApplicationCustomMatrixToken());
                }
                String originalMatrix=custom?null:approvalMatrixBaselines.creationBaseline(userId,reqVO.getDccProjectCodeId(),reqVO.getCategoryId(),reqVO.getChangeType(),reqVO.getApprovalMatrixBaselineToken(),reqVO.getApplicationContextKey());
                PreparedSubmitContext context = prepareSubmitContext(userId, reqVO, true, true, null, true,storage,false);
                requireDraftEffectiveSelection(reqVO.getEffectiveMode(),reqVO.getEffectiveDate());
                DccControlledFileDO file = insertControlledFile(context, userId,
                        DccControlledFileStatusEnum.WORKING.getStatus(), null, false, ownershipType,reqVO.getSelectedSignoffDepartmentIds(),originalMatrix);
                persistPreparedDefinition(file,publicDefinition);
                if(custom&&"SUBMISSION".equals(ownershipType))prepareApplicationDraft(userId,file,reqVO.getProjectAttributes());
                if(custom)persistCustomDraft(file,reqVO.getApplicationContextKey(),customMatrices.bindPreviewToReserved(customSelection.canonical(),customSelection.digest(),customPreview,customReservedContext(file,reqVO.getApplicationContextKey())));
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
                    || !Objects.equals(file.getTenantId(), TenantContextHolder.getRequiredTenantId())
                    || !Objects.equals(file.getCategoryId(), category.getId())) {
                throw exception(CONTROLLED_FILE_UPLOAD_TICKET_INVALID);
            }
            if (DccControlledFileStatusEnum.WITHDRAWN.getStatus().equals(file.getStatus())) {
                queryService.assertCancelledRevisionCheckinUpload(userId,file.getId(),category.getId());
            } else if (!Set.of("WORKING", "REJECTED", "PENDING_APPLICANT_REWORK")
                    .contains(StrUtil.trimToEmpty(file.getStatus()))
                    && !DccControlledFileVersionPolicy.hasControlledBodyStatus(StrUtil.trimToEmpty(file.getStatus()))) {
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
        reqVO.setSelectedDistributionDepartmentIds(normalizeDistributionDepartments(reqVO.getSelectedDistributionDepartmentIds()));
        reqVO.setSelectedSignoffDepartmentIds(normalizeDistributionDepartments(reqVO.getSelectedSignoffDepartmentIds()));
        String payloadHash = DigestUtil.sha256Hex(JsonUtils.toJsonString(java.util.Arrays.asList(iterationId,reqVO)));
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        DccControlledFileDO initial = controlledFileMapper.selectById(iterationId);
        if (initial == null || initial.getMasterId() == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        boolean legacyPrepared=DccApplicationExecutionContractService.LEGACY.equals(initial.getPreparedWorkflowContract());
        lockDraftProject(initial.getDccProjectCodeId());
        if(!legacyPrepared&&initial.getPreparedWorkflowContract()!=null&&categoryMapper.selectMatrixCategoryForUpdate(tenantId,initial.getCategoryId())==null)
            throw new IllegalStateException("APPROVAL_MATRIX_CATEGORY_INVALID");
        publicUploadPlacementService.lockSourceLocation(userId,iterationId);
        DccControlledFileMasterDO master = controlledFileMasterMapper.selectByIdForUpdate(initial.getMasterId());
        if (master == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        DccControlledFileDO file = controlledFileMapper.selectByIdAndTenantForUpdate(tenantId, iterationId);
        if (file == null || !Objects.equals(file.getMasterId(), master.getId())) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        var ordinaryManifest=attachmentService.readOrdinaryAttachmentManifest(userId,iterationId);
        if(reqVO.getExpectedOrdinaryAttachmentIds()==null||reqVO.getExpectedOrdinaryAttachmentManifestHash()==null
                ||!reqVO.getExpectedOrdinaryAttachmentManifestHash().matches("[a-f0-9]{64}")||ordinaryManifest==null
                ||!Objects.equals(reqVO.getExpectedOrdinaryAttachmentIds(),ordinaryManifest.attachmentIds())
                ||!Objects.equals(reqVO.getExpectedOrdinaryAttachmentManifestHash(),ordinaryManifest.payloadHash()))
            throw new IllegalArgumentException("ORDINARY_ATTACHMENT_MANIFEST_CHANGED：本次附件清单未完整读取或已变化，请重新核对全部附件后提交");
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
        workflowDatePolicy.requireEffectiveConfiguration(reqVO.getEffectiveMode(),reqVO.getEffectiveDate());
        if(reqVO.getSelectedDistributionDepartmentIds()==null)
            throw new IllegalArgumentException("请明确保存本次下发部门名单，未选择不能推断为会签名单");
        DccControlledFileDO applicantReworkPredecessor = validateWorkingIterationSubmission(userId, tenantId,
                file, master,reqVO.getRevisionChangeType());
        if(!Objects.equals(initial.getPreparedWorkflowContract(),file.getPreparedWorkflowContract())
                ||!Objects.equals(initial.getPreparedProcessDefinitionId(),file.getPreparedProcessDefinitionId())
                ||!Objects.equals(initial.getPreparedDefinitionDigest(),file.getPreparedDefinitionDigest()))
            throw new IllegalStateException("EXECUTION_CONTRACT_LOCKED_IDENTITY_CHANGED");
        if(file.getPreparedWorkflowContract()==null)
            throw new IllegalStateException("EXECUTION_CONTRACT_PREPARED_IDENTITY_REQUIRED：未提交BODY必须由正式入口准备，不因缺矩阵或token推旧");
        executionContracts.requirePrepared(file,file.getPreparedWorkflowContract(),file.getPreparedProcessDefinitionId(),file.getPreparedDefinitionDigest(),
                "NEW".equals(file.getChangeType())?DccControlledFileProcessDefinitionKeys.UPLOAD:DccControlledFileProcessDefinitionKeys.REVISION);
        if(master.getLatestControlledFileId()!=null) {
            var baseline=controlledFileMapper.selectByIdAndTenantForUpdate(tenantId,master.getLatestControlledFileId());
            if(baseline==null || !Objects.equals(baseline.getMasterId(),master.getId()) || baseline.getControlledTime()==null
                    || !versionPolicy.isControlledBaseline(baseline)) throw exception(CONTROLLED_FILE_ITERATION_SUBMIT_NOT_ALLOWED);
            file=revisionService.createRevision(userId,baseline.getId(),iterationId,reqVO);
            if(file==null || Objects.equals(file.getId(),iterationId) || !Objects.equals(file.getMasterId(),master.getId())
                    || !Objects.equals(file.getTenantId(),tenantId) || file.getRevisionChangeType()==null
                    || !Objects.equals(file.getSelectedIterationControlledFileId(),iterationId))
                throw exception(CONTROLLED_FILE_ITERATION_SUBMIT_NOT_ALLOWED);
            attachmentService.requireFrozenOrdinaryAttachmentManifest(userId,iterationId,file.getId(),ordinaryManifest);
        } else if(master.getCurrentActiveControlledFileId()!=null || "REVISION".equals(file.getChangeType())) {
            throw exception(CONTROLLED_FILE_ITERATION_SUBMIT_NOT_ALLOWED);
        } else if(versionPolicy.requireStored(file).isWorkingIteration()) {
            file=revisionService.createInitialCandidate(userId,iterationId,reqVO);
            if(file==null || Objects.equals(file.getId(),iterationId) || !Objects.equals(file.getMasterId(),master.getId())
                    || !Objects.equals(file.getTenantId(),tenantId) || !"INITIAL".equals(file.getRevisionChangeType())
                    || !Objects.equals(file.getSelectedIterationControlledFileId(),iterationId))
                throw exception(CONTROLLED_FILE_ITERATION_SUBMIT_NOT_ALLOWED);
        } else {
            if(!"INITIAL".equals(reqVO.getRevisionChangeType()))
                throw exception(CONTROLLED_FILE_ITERATION_SUBMIT_NOT_ALLOWED);
            if(controlledFileMapper.update(null,new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<DccControlledFileDO>()
                    .eq(DccControlledFileDO::getId,file.getId()).eq(DccControlledFileDO::getTenantId,tenantId)
                    .eq(DccControlledFileDO::getStatus,"WORKING")
                    .set(DccControlledFileDO::getEffectiveMode,reqVO.getEffectiveMode())
                    .set(DccControlledFileDO::getEffectiveDate,reqVO.getEffectiveDate())
                    .set(DccControlledFileDO::getDistributionDepartmentIdsJson,serializeDistributionDepartments(reqVO.getSelectedDistributionDepartmentIds()))
                    .set(DccControlledFileDO::getChangeDescription,reqVO.getChangeDescription()))!=1)
                throw exception(CONTROLLED_FILE_ITERATION_SUBMIT_NOT_ALLOWED);
            file.setEffectiveMode(reqVO.getEffectiveMode());file.setEffectiveDate(reqVO.getEffectiveDate());
            file.setDistributionDepartmentIdsJson(serializeDistributionDepartments(reqVO.getSelectedDistributionDepartmentIds()));
            file.setChangeDescription(reqVO.getChangeDescription());
        }
        var formalCandidateVersion=versionPolicy.parseStored(file);
        if(formalCandidateVersion==null || formalCandidateVersion.isWorkingIteration())
            throw exception(CONTROLLED_FILE_ITERATION_SUBMIT_NOT_ALLOWED);
        persistApplicationSelection(file,reqVO.getEffectiveMode(),reqVO.getEffectiveDate(),reqVO.getSelectedDistributionDepartmentIds());
        String processDefinitionKey = resolveWorkingIterationProcessDefinitionKey(file, master);
        if(!Objects.equals(file.getId(),iterationId)) prepareApplicationDraft(userId,file,reqVO.getProjectAttributes());
        if(file.getReworkPredecessorControlledFileId()!=null)inheritReservedProvenanceRelations(userId,file,processDefinitionKey);
        if(legacyPrepared)persistPreparedDefinition(file,executionContracts.requirePrepared(initial,initial.getPreparedWorkflowContract(),
                initial.getPreparedProcessDefinitionId(),initial.getPreparedDefinitionDigest(),processDefinitionKey));
        workflowDatePolicy.requireEffectiveConfiguration(file.getEffectiveMode(),file.getEffectiveDate());
        DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute resolvedRoute;
        if(!legacyPrepared&&"APPLICATION_CUSTOM".equals(applicationSourceMode(file,file.getCategoryId()))){
            var selection=saveCustomDraft(initial,reqVO.getApplicationCustomMatrix(),reqVO.getSelectedSignoffDepartmentIds(),reqVO.getApplicationContextKey(),reqVO.getExpectedApplicationCustomMatrixDigest(),reqVO.getApplicationCustomMatrixToken(),reqVO.getApplicationCustomMatrixPhase());
            resolvedRoute=approvalRouteAssigneeResolver.resolveCustomReserved(userId,customReservedContext(initial,initial.getDraftApplicationContextKey()),selection);
            approvalRouteAssigneeResolver.requireCustomExpectation(resolvedRoute,reqVO.getExpectedSupervisorSourceDigest(),reqVO.getSupervisorSourceToken());
            routeReadinessService.requireReadyResolvedRoute(resolvedRoute);
            if(!Objects.equals(file.getId(),initial.getId())){
                var reserved=customMatrices.canonicalize(customReservedContext(file,reqVO.getApplicationContextKey()),reqVO.getApplicationCustomMatrix(),reqVO.getSelectedSignoffDepartmentIds());
                persistCustomDraft(file,reqVO.getApplicationContextKey(),reserved);resolvedRoute=approvalRouteAssigneeResolver.resolveCustomReserved(userId,customReservedContext(file,reqVO.getApplicationContextKey()),reserved);
                routeReadinessService.requireReadyResolvedRoute(resolvedRoute);
            }
        }else if(file.getReworkPredecessorControlledFileId()!=null){
            resolvedRoute=resolveIterationApprovalRoute(file,userId,reqVO.getSelectedSignoffDepartmentIds(),processDefinitionKey);
            if(resolvedRoute.sourceApprovalMatrix()!=null){var nativeSource=approvalRouteAssigneeResolver.attachPublicNative(resolvedRoute,userId,file.getChangeType(),initial.getId(),file.getDccProjectCodeId(),reqVO.getApplicationContextKey(),initial.getPreparedProcessDefinitionId(),initial.getPreparedDefinitionDigest(),reqVO.getSelectedSignoffDepartmentIds());approvalRouteAssigneeResolver.requireNativeExpectation(nativeSource,reqVO.getExpectedSupervisorSourceDigest(),reqVO.getSupervisorSourceToken());resolvedRoute=nativeSource.route();}
        }else if(legacyPrepared){
            resolvedRoute=routeReadinessService.evaluateDepartments(file.getCategoryId(),userId,reqVO.getSelectedSignoffDepartmentIds(),
                    DccControlledFileProcessDefinitionKeys.toActionType(processDefinitionKey)).requireReady();
        }else{
            var prepared=executionContracts.requireOrdinaryPublic(reqVO.getExecutionContract(),reqVO.getExpectedProcessDefinitionId(),
                    reqVO.getExpectedDefinitionDigest(),processDefinitionKey);
            if(!Objects.equals(initial.getPreparedWorkflowContract(),prepared.executionContract())
                    ||!Objects.equals(initial.getPreparedProcessDefinitionId(),prepared.processDefinitionId())
                    ||!Objects.equals(initial.getPreparedDefinitionDigest(),prepared.definitionDigest()))
                throw new IllegalStateException("EXECUTION_CONTRACT_DRAFT_PREPARATION_REQUIRED");
            var confirmed=approvalMatrixBaselines.confirm(userId,initial.getDccProjectCodeId(),initial.getCategoryId(),initial.getId(),
                    DccControlledFileProcessDefinitionKeys.toActionType(processDefinitionKey),
                    new DccApprovalMatrixBaselineService.Expectations(reqVO.getExpectedApprovalRouteId(),reqVO.getExpectedApprovalRouteVersionNo(),
                            reqVO.getExpectedApprovalMatrixDigest(),reqVO.getApprovalMatrixBaselineToken(),reqVO.getApprovalMatrixConfirmationToken(),
                            reqVO.getMatrixChangeConfirmed(),reqVO.getApplicationContextKey()));
            resolvedRoute=approvalRouteAssigneeResolver.resolvePublicMatrix(confirmed.source(),userId,
                    DccControlledFileProcessDefinitionKeys.toActionType(processDefinitionKey),reqVO.getSelectedSignoffDepartmentIds());
            var nativeSource=approvalRouteAssigneeResolver.attachPublicNative(resolvedRoute,userId,file.getChangeType(),initial.getId(),file.getDccProjectCodeId(),reqVO.getApplicationContextKey(),prepared.processDefinitionId(),prepared.definitionDigest(),reqVO.getSelectedSignoffDepartmentIds());
            approvalRouteAssigneeResolver.requireNativeExpectation(nativeSource,reqVO.getExpectedSupervisorSourceDigest(),reqVO.getSupervisorSourceToken());resolvedRoute=nativeSource.route();
            routeReadinessService.requireReadyResolvedRoute(resolvedRoute);
            persistPreparedDefinition(file,prepared);
            if(!Objects.equals(file.getDraftApprovalMatrixJson(),confirmed.beforeCanonical())){
                if(controlledFileMapper.updateById(DccControlledFileDO.builder().id(file.getId()).draftApprovalMatrixJson(confirmed.beforeCanonical()).build())!=1)
                    throw new IllegalStateException("APPROVAL_MATRIX_CANDIDATE_BASELINE_WRITE_FAILED");
                file.setDraftApprovalMatrixJson(confirmed.beforeCanonical());
            }
        }
        persistSignoffSelection(file,resolvedRoute);
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
        attachmentService.bindApplicationRound(file.getId(), processInstanceId);
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

    private DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute resolveIterationApprovalRoute(
            DccControlledFileDO file,Long userId,List<Long> selectedDepartments,String processDefinitionKey) {
        Long sourceId=file.getReworkPredecessorControlledFileId();
        if(sourceId==null) return routeReadinessService.evaluateDepartments(file.getCategoryId(),userId,selectedDepartments,
                DccControlledFileProcessDefinitionKeys.toActionType(processDefinitionKey)).requireReady();
        var source=controlledFileMapper.selectByIdAndTenantForUpdate(TenantContextHolder.getRequiredTenantId(),sourceId);
        if(source==null || Objects.equals(source.getId(),file.getId())
                || !Objects.equals(source.getTenantId(),file.getTenantId()) || !Objects.equals(source.getMasterId(),file.getMasterId())
                || !Objects.equals(source.getDccProjectCodeId(),file.getDccProjectCodeId())
                || !Objects.equals(source.getProcessDefinitionKey(),processDefinitionKey)
                || StrUtil.isBlank(source.getProcessInstanceId())
                || !Set.of("REJECTED","WITHDRAWN","PENDING_APPLICANT_REWORK").contains(source.getStatus()))
            throw exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
        if("PENDING_APPLICANT_REWORK".equals(source.getStatus())) {
            var tasks=bpmTaskService.getRunningTaskListByProcessInstanceId(source.getProcessInstanceId(),null,null);
            if(tasks==null||tasks.size()!=1||!"APPLICANT_REWORK".equals(tasks.get(0).getTaskDefinitionKey())
                    ||!Objects.equals(tasks.get(0).getProcessInstanceId(),source.getProcessInstanceId())
                    ||!String.valueOf(source.getRequesterId()).equals(tasks.get(0).getAssignee()))
                throw exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
        }
        if(!Objects.equals(source.getCategoryId(),file.getCategoryId())
                || !Objects.equals(source.getFileTypeTaxonomyId(),file.getFileTypeTaxonomyId()))
            throw new IllegalStateException("失败重提变更类型必须有正式主动选择和确认合同，不能自动重匹配矩阵");
        var restored=approvalRouteAssigneeResolver.restoreFrozenRoute(source,routeSnapshotMapper.selectListByControlledFileId(sourceId));
        var signoff=restored.nodes().stream().filter(node->"MATRIX_REVIEW".equals(node.stageCode())).findFirst()
                .orElseThrow(()->exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH));
        if(selectedDepartments!=null && (selectedDepartments.stream().anyMatch(Objects::isNull)
                || selectedDepartments.stream().distinct().count()!=selectedDepartments.size()
                || !new java.util.HashSet<>(selectedDepartments).equals(new java.util.HashSet<>(signoff.candidateSourceIds()))))
            throw new IllegalStateException("失败重提改变部门名单必须有正式原名单调整和确认合同，不能静默覆盖");
        routeReadinessService.requireReadyResolvedRoute(restored);
        return restored;
    }
    private DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute readNativeReworkRoute(Long actor,DccControlledFileDO target,List<Long> selected){
        var source=controlledFileMapper.selectById(target.getReworkPredecessorControlledFileId());
        if(source==null||Objects.equals(source.getId(),target.getId())||!Objects.equals(source.getTenantId(),target.getTenantId())||!Objects.equals(source.getMasterId(),target.getMasterId())||!Objects.equals(source.getDccProjectCodeId(),target.getDccProjectCodeId())
                ||!Objects.equals(source.getChangeType(),target.getChangeType())||!Objects.equals(source.getCategoryId(),target.getCategoryId())||!Objects.equals(source.getFileTypeTaxonomyId(),target.getFileTypeTaxonomyId())||source.getProcessInstanceId()==null||!Set.of("REJECTED","WITHDRAWN").contains(source.getStatus())||!DccApplicationExecutionContractService.isPublicContract(source.getPreparedWorkflowContract()))throw new IllegalArgumentException("NATIVE_REWORK_SOURCE_IDENTITY_INVALID");
        if(!Objects.equals(target.getPreparedProcessDefinitionId(),source.getPreparedProcessDefinitionId())||!Objects.equals(target.getPreparedDefinitionDigest(),source.getPreparedDefinitionDigest()))throw new IllegalArgumentException("NATIVE_REWORK_EXACT_SOURCE_GRAPH_CHANGED");
        var exact=executionContracts.resolveBoundExecution(source);if(exact.active()||!Objects.equals(exact.processInstanceId(),source.getProcessInstanceId())||!Objects.equals(exact.processDefinitionId(),target.getPreparedProcessDefinitionId())||!Objects.equals(exact.definitionDigest(),target.getPreparedDefinitionDigest()))throw new IllegalArgumentException("NATIVE_REWORK_ENDED_EXACT_SOURCE_REQUIRED");
        var route=approvalRouteAssigneeResolver.restoreFrozenRoute(source,routeSnapshotMapper.selectListByControlledFileId(source.getId()));var department=route.nodes().stream().filter(n->"MATRIX_REVIEW".equals(n.stageCode())).findFirst().orElseThrow();
        if(selected==null||!Objects.equals(selected,department.candidateSourceIds()))throw new IllegalArgumentException("NATIVE_REWORK_DEPARTMENT_SELECTION_CHANGED");return route;
    }
    private DccApprovalMatrixBaselineService.Comparison nativeReworkComparison(Long actor,DccControlledFileDO file,DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute route,String contextKey){
        var baseline=route.sourceApprovalMatrix();String canonical=canonicalPublicMatrix(baseline);var context=new DccApprovalMatrixTokenService.Context(file.getTenantId(),actor,file.getDccProjectCodeId(),file.getCategoryId(),file.getFileTypeTaxonomyId(),file.getChangeType(),file.getId(),contextKey);
        return new DccApprovalMatrixBaselineService.Comparison(context,canonical,baseline,new DccApprovalMatrixSourceService.Source(route.route(),baseline.getNodes().stream().map(node->cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(node),cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteNodeDO.class)).toList(),baseline,canonical),approvalMatrixTokens.issueBaseline(context,canonical),null,false);
    }

    private void inheritRejectedRoundRelations(Long actorId,DccControlledFileDO target,String targetProcessKey){
        var source=controlledFileMapper.selectByIdAndTenantForUpdate(TenantContextHolder.getRequiredTenantId(),target.getReworkPredecessorControlledFileId());
        if(source==null||!Objects.equals(source.getTenantId(),target.getTenantId())||!Objects.equals(source.getMasterId(),target.getMasterId())
                ||!Objects.equals(source.getDccProjectCodeId(),target.getDccProjectCodeId())||StrUtil.isBlank(source.getProcessInstanceId())
                ||!Set.of("REJECTED","WITHDRAWN","PENDING_APPLICANT_REWORK").contains(source.getStatus()))
            throw new IllegalStateException("RELATION_REWORK_SOURCE_IDENTITY_INVALID");
        String sourceType=DccControlledFileProcessDefinitionKeys.UPLOAD.equals(source.getProcessDefinitionKey())?"UPLOAD":
                DccControlledFileProcessDefinitionKeys.REVISION.equals(source.getProcessDefinitionKey())?"REVISION":null;
        String targetType=DccControlledFileProcessDefinitionKeys.UPLOAD.equals(targetProcessKey)?"UPLOAD":
                DccControlledFileProcessDefinitionKeys.REVISION.equals(targetProcessKey)?"REVISION":null;
        if(sourceType==null||!Objects.equals(sourceType,targetType))throw new IllegalStateException("RELATION_REWORK_APPLICATION_TYPE_INVALID");
        int sourceAttributeRound=applicationRoundService.require(sourceType,source.getId(),source.getProcessInstanceId());
        applicationRoundService.assertBoundRound(source.getDccProjectCodeId(),sourceType,source.getId(),sourceAttributeRound);
        int targetAttributeRound=applicationRoundService.requireDraft(targetType,target.getId());
        var evidence=relationStageSnapshots.readRoundEvidence(actorId,source.getId(),sourceType,source.getProcessInstanceId());
        if(evidence==null||!Objects.equals(evidence.tenantId(),target.getTenantId())||!Objects.equals(evidence.fileId(),source.getId())
                ||!Objects.equals(evidence.bpmRound(),source.getProcessInstanceId())||!Objects.equals(evidence.attributeRound(),sourceAttributeRound))
            throw new IllegalStateException("RELATION_REWORK_SOURCE_EVIDENCE_INVALID");
        relationStageSnapshots.inheritRoundSelections(actorId,source.getId(),sourceType,source.getProcessInstanceId(),target.getId(),
                targetType,targetAttributeRound,evidence.relationDigest());
    }
    private void inheritReservedProvenanceRelations(Long actorId,DccControlledFileDO target,String targetProcessKey){
        String type=DccControlledFileProcessDefinitionKeys.UPLOAD.equals(targetProcessKey)?"UPLOAD":
                DccControlledFileProcessDefinitionKeys.REVISION.equals(targetProcessKey)?"REVISION":null;
        if(type==null)throw new IllegalStateException("RELATION_REWORK_APPLICATION_TYPE_INVALID");
        int targetRound=applicationRoundService.requireDraft(type,target.getId());
        var provenance=workingApplicationDraftInitializer.readApplicationProvenance(actorId,target.getId(),targetRound);
        var visited=new java.util.HashSet<String>();
        while(provenance.sourceApplicationId()!=null){
            String identity=provenance.sourceApplicationId()+":"+provenance.sourceAttributeRound();
            if(!visited.add(identity))throw new IllegalStateException("RELATION_REWORK_SOURCE_CYCLE");
            var source=controlledFileMapper.selectByIdAndTenantForUpdate(TenantContextHolder.getRequiredTenantId(),provenance.sourceApplicationId());
            if(source==null||!Objects.equals(source.getTenantId(),target.getTenantId())||!Objects.equals(source.getMasterId(),target.getMasterId())
                    ||!Objects.equals(source.getDccProjectCodeId(),target.getDccProjectCodeId())||!Objects.equals(type,provenance.applicationType()))
                throw new IllegalStateException("RELATION_REWORK_SOURCE_IDENTITY_INVALID");
            if("WORKING".equals(source.getStatus())&&source.getProcessInstanceId()==null){
                provenance=workingApplicationDraftInitializer.readApplicationProvenance(actorId,source.getId(),provenance.sourceAttributeRound());
                continue;
            }
            String sourceBpm=applicationRoundService.resolveBoundBpm(type,source.getId(),provenance.sourceAttributeRound());
            if(!Set.of("REJECTED","WITHDRAWN","PENDING_APPLICANT_REWORK").contains(source.getStatus())
                    ||!Objects.equals(source.getId(),target.getReworkPredecessorControlledFileId()))
                throw new IllegalStateException("RELATION_REWORK_SOURCE_NOT_EXACT_FAILED_ROUND");
            var evidence=relationStageSnapshots.readRoundEvidence(actorId,source.getId(),type,sourceBpm);
            if(evidence==null||!Objects.equals(evidence.fileId(),source.getId())||!Objects.equals(evidence.bpmRound(),sourceBpm)
                    ||!Objects.equals(evidence.attributeRound(),provenance.sourceAttributeRound()))
                throw new IllegalStateException("RELATION_REWORK_SOURCE_EVIDENCE_INVALID");
            relationStageSnapshots.inheritRoundSelections(actorId,source.getId(),type,sourceBpm,target.getId(),type,targetRound,evidence.relationDigest());
            return;
        }
        throw new IllegalStateException("RELATION_REWORK_FAILED_SOURCE_PROVENANCE_REQUIRED");
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
        reqVO.setSelectedDistributionDepartmentIds(normalizeDistributionDepartments(reqVO.getSelectedDistributionDepartmentIds()));
        reqVO.setSelectedSignoffDepartmentIds(normalizeDistributionDepartments(reqVO.getSelectedSignoffDepartmentIds()));
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
        if(DccControlledFileProcessDefinitionKeys.UPLOAD.equals(processDefinitionKey)&&"APPLICATION_CUSTOM".equals(applicationSourceMode(null,reqVO.getCategoryId())))
            return submitNewCustomApplication(userId,reqVO,storage);
        DccApprovalMatrixBaselineService.Confirmed matrix=null;
        if(isThreeWorkflowUploadOrRevision(processDefinitionKey)){
            validateSubmitRequest(reqVO,requireUploadTickets,storage==null);
            if(reqVO.getDccProjectCodeId()==null||reqVO.getFileTypeTaxonomyId()==null)
                throw exception(CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING);
            resolveFileTypeTaxonomy(reqVO.getFileTypeTaxonomyId(),true);
            validateCategory(reqVO.getCategoryId());
            executionContracts.requireOrdinaryPublic(reqVO.getExecutionContract(),reqVO.getExpectedProcessDefinitionId(),
                    reqVO.getExpectedDefinitionDigest(),processDefinitionKey);
            lockDraftProject(reqVO.getDccProjectCodeId());
            matrix=approvalMatrixBaselines.confirm(userId,reqVO.getDccProjectCodeId(),reqVO.getCategoryId(),null,reqVO.getChangeType(),
                    new DccApprovalMatrixBaselineService.Expectations(reqVO.getExpectedApprovalRouteId(),reqVO.getExpectedApprovalRouteVersionNo(),
                            reqVO.getExpectedApprovalMatrixDigest(),reqVO.getApprovalMatrixBaselineToken(),reqVO.getApprovalMatrixConfirmationToken(),
                            reqVO.getMatrixChangeConfirmed(),reqVO.getApplicationContextKey()));
        }
        PreparedSubmitContext context = prepareSubmitContext(userId, reqVO, true, true, ignoredControlledFileId,
                requireUploadTickets,storage);
        DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute resolvedRoute = matrix==null?routeReadinessService
                .evaluateDepartments(context.category().getId(), userId, reqVO.getSelectedSignoffDepartmentIds(),
                        DccControlledFileProcessDefinitionKeys.toActionType(processDefinitionKey))
                .requireReady():approvalRouteAssigneeResolver.resolvePublicMatrix(matrix.source(),userId,reqVO.getChangeType(),reqVO.getSelectedSignoffDepartmentIds());
        if(matrix!=null){var nativeSource=approvalRouteAssigneeResolver.attachPublicNative(resolvedRoute,userId,reqVO.getChangeType(),null,reqVO.getDccProjectCodeId(),reqVO.getApplicationContextKey(),reqVO.getExpectedProcessDefinitionId(),reqVO.getExpectedDefinitionDigest(),reqVO.getSelectedSignoffDepartmentIds());approvalRouteAssigneeResolver.requireNativeExpectation(nativeSource,reqVO.getExpectedSupervisorSourceDigest(),reqVO.getSupervisorSourceToken());resolvedRoute=nativeSource.route();routeReadinessService.requireReadyResolvedRoute(resolvedRoute);}
        if (isThreeWorkflowUploadOrRevision(processDefinitionKey))
            workflowDatePolicy.requireEffectiveConfiguration(reqVO.getEffectiveMode(),reqVO.getEffectiveDate());
        // The initial selected route is frozen by the same INSERT, without a second mutable projection write.
        var frozenSignoff=resolvedRoute.nodes().stream().filter(node->"MATRIX_REVIEW".equals(node.stageCode()))
                .findFirst().orElseThrow(()->exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH)).candidateSourceIds();
        String pendingStatus = toPendingStatus(resolveTaskStage(resolvedRoute.nodes().get(0).stageCode()));
        DccControlledFileDO file = insertControlledFile(context, userId,
                DccControlledFileStatusEnum.WORKING.getStatus(),
                processDefinitionKey, true, "SUBMISSION",frozenSignoff,matrix==null?null:matrix.beforeCanonical());
        if(matrix!=null){var prepared=executionContracts.requireOrdinaryPublic(reqVO.getExecutionContract(),
                reqVO.getExpectedProcessDefinitionId(),reqVO.getExpectedDefinitionDigest(),processDefinitionKey);
            file.setPreparedWorkflowContract(prepared.executionContract());file.setPreparedProcessDefinitionId(prepared.processDefinitionId());file.setPreparedDefinitionDigest(prepared.definitionDigest());}
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
        attachmentService.bindApplicationRound(file.getId(), processInstanceId);
        platformAdapter.recordSubmitted(file, userId, processInstanceId);
        return file.getId();
    }

    private Long submitNewCustomApplication(Long actor,DccControlledFileSubmitReqVO request,DccDerivedUploadStorage storage){
        var prepared=executionContracts.requireOrdinaryPublic(request.getExecutionContract(),request.getExpectedProcessDefinitionId(),request.getExpectedDefinitionDigest(),DccControlledFileProcessDefinitionKeys.UPLOAD);
        var preview=customPreviewContext(actor,request.getDccProjectCodeId(),request.getCategoryId(),request.getFileTypeTaxonomyId(),request.getChangeType(),request.getApplicationContextKey(),prepared);
        var selection=customMatrices.canonicalizePreview(preview,request.getApplicationCustomMatrix(),request.getSelectedSignoffDepartmentIds());
        if(!"PREVIEW".equals(request.getApplicationCustomMatrixPhase())||!Objects.equals(selection.digest(),request.getExpectedApplicationCustomMatrixDigest()))throw new IllegalArgumentException("CUSTOM_PREVIEW_EXPECTATION_CHANGED");
        approvalMatrixTokens.requireCustomPreview(preview,selection,request.getApplicationCustomMatrixToken());
        var route=approvalRouteAssigneeResolver.resolveCustomPreview(actor,preview,selection);approvalRouteAssigneeResolver.requireCustomExpectation(route,request.getExpectedSupervisorSourceDigest(),request.getSupervisorSourceToken());routeReadinessService.requireReadyResolvedRoute(route);
        Long id=createWorkingControlledFileIdempotently(actor,request,"SUBMISSION",storage);
        var file=controlledFileMapper.selectByIdAndTenantForUpdate(TenantContextHolder.getRequiredTenantId(),id);
        var reservedContext=customReservedContext(file,request.getApplicationContextKey());var reserved=customMatrices.canonicalize(reservedContext,request.getApplicationCustomMatrix(),request.getSelectedSignoffDepartmentIds());
        var actualRoute=approvalRouteAssigneeResolver.resolveCustomReserved(actor,reservedContext,reserved);
        var inner=new DccControlledFileSubmitIterationReqVO();inner.setIdempotencyKey(request.getIdempotencyKey());inner.setApplicationCustomMatrix(request.getApplicationCustomMatrix());inner.setApplicationContextKey(request.getApplicationContextKey());inner.setApplicationCustomMatrixPhase("RESERVED");inner.setExpectedApplicationCustomMatrixDigest(reserved.digest());inner.setApplicationCustomMatrixToken(approvalMatrixTokens.issueCustomReserved(reservedContext,reserved));
        inner.setExpectedSupervisorSourceDigest(actualRoute.applicationCustomSource().proof().sourceDigest());inner.setSupervisorSourceToken(actualRoute.applicationCustomSource().proof().sourceToken());inner.setExecutionContract(prepared.executionContract());inner.setExpectedProcessDefinitionId(prepared.processDefinitionId());inner.setExpectedDefinitionDigest(prepared.definitionDigest());inner.setEffectiveMode(request.getEffectiveMode());inner.setEffectiveDate(request.getEffectiveDate());inner.setNeedTraining(request.getNeedTraining());inner.setProjectAttributes(request.getProjectAttributes());inner.setSelectedSignoffDepartmentIds(request.getSelectedSignoffDepartmentIds());inner.setSelectedDistributionDepartmentIds(request.getSelectedDistributionDepartmentIds());inner.setRevisionChangeType("INITIAL");inner.setChangeDescription(request.getRemark());
        var manifest=attachmentService.readOrdinaryAttachmentManifest(actor,id);inner.setExpectedOrdinaryAttachmentIds(manifest.attachmentIds());inner.setExpectedOrdinaryAttachmentManifestHash(manifest.payloadHash());
        Long result=submitWorkingIteration(actor,id,inner);
        if(controlledFileMapper.updateById(DccControlledFileDO.builder().id(result).submitPayloadHash(request.getSubmitPayloadHash()).build())!=1)throw new IllegalStateException("CUSTOM_INITIAL_REQUEST_HASH_WRITE_FAILED");
        return result;
    }

    private void persistApprovalRouteSnapshots(Long controlledFileId,
                                               DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute resolvedRoute) {
        var runtime=new java.util.TreeMap<String,Object>();runtime.put("formatVersion","DCC_RUNTIME_APPROVAL_NODES_V1");runtime.put("nodes",resolvedRoute.nodes());runtime.put("groups",resolvedRoute.runtimeGroups());if(resolvedRoute.nativeSupervisorSource()!=null)runtime.put("nativeSupervisorSource",resolvedRoute.nativeSupervisorSource());
        if(resolvedRoute.applicationCustomSource()!=null){runtime.put("sourceKind","APPLICATION_CUSTOM");runtime.put("applicationCustomSource",resolvedRoute.applicationCustomSource());}
        else if(resolvedRoute.route()==null||resolvedRoute.route().getVersionNo()==null||resolvedRoute.route().getVersionNo()<=0)throw new IllegalArgumentException("PUBLIC_ROUTE_VERSION_REQUIRED");
        for (DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode routeNode : resolvedRoute.nodes()) {
            if(routeSnapshotMapper.insert(DccControlledFileRouteSnapshotDO.builder()
                    .controlledFileId(controlledFileId)
                    .routeVersionNo(resolvedRoute.applicationCustomSource()==null?resolvedRoute.route().getVersionNo():null)
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
                    .sourceApprovalMatrixId(resolvedRoute.sourceApprovalMatrix()==null?null:resolvedRoute.sourceApprovalMatrix().getRouteId())
                    .sourceApprovalMatrixVersion(resolvedRoute.sourceApprovalMatrix()==null?null:resolvedRoute.sourceApprovalMatrix().getRouteVersionNo())
                    .sourceApprovalMatrixDigest(resolvedRoute.sourceApprovalMatrix()==null?null:resolvedRoute.sourceApprovalMatrix().getMatrixDigest())
                    .derivationVersion(resolvedRoute.derivationVersion())
                    .sourceApprovalMatrixJson(resolvedRoute.sourceApprovalMatrix()==null?null:canonicalPublicMatrix(resolvedRoute.sourceApprovalMatrix()))
                    .runtimeApprovalNodesJson(resolvedRoute.sourceApprovalMatrix()==null&&resolvedRoute.applicationCustomSource()==null?null:JsonUtils.toJsonString(
                            runtime))
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
        if(resolvedRoute.sourceApprovalMatrix()!=null&&resolvedRoute.nodes().stream().noneMatch(node->"SUPERVISOR_REVIEW".equals(node.stageCode())))
            throw new IllegalStateException("DIRECT_SUPERVISOR_SOURCE_NOT_CONFIGURED：新公共矩阵申请不能启动旧定义或猜主管");
        if(file.getPreparedWorkflowContract()!=null){
            executionContracts.requirePrepared(file,file.getPreparedWorkflowContract(),file.getPreparedProcessDefinitionId(),
                    file.getPreparedDefinitionDigest(),processDefinitionKey);
        }
        if (Boolean.TRUE.equals(file.getNeedTraining()) && Set.of(DccControlledFileProcessDefinitionKeys.UPLOAD,DccControlledFileProcessDefinitionKeys.REVISION).contains(processDefinitionKey))
            offlineTraining.requireConfiguredRecipients(file);
        Map<String, Object> variables = new HashMap<>();
        variables.put("controlledFileId", file.getId());
        variables.put("categoryId", file.getCategoryId());
        variables.put("directoryId", file.getDirectoryId());
        variables.put("needTraining", Boolean.TRUE.equals(file.getNeedTraining()));
        if(DccApplicationExecutionContractService.PUBLIC_V2.equals(file.getPreparedWorkflowContract())) {
            variables.put("effectiveMode",file.getEffectiveMode());
            variables.put("dccOriginalRequesterId",file.getRequesterId());
            workflowDatePolicy.requireApplicationEffectiveConfiguration(file.getEffectiveMode(),file.getEffectiveDate(),file.getPreparedWorkflowContract());
        }
        variables.put("fileCode",file.getFileNumber());
        if(resolvedRoute.nativeSupervisorSource()!=null)variables.put("DCC_NATIVE_SUPERVISOR_SOURCE",resolvedRoute.nativeSupervisorSource());
        if(resolvedRoute.applicationCustomSource()!=null)variables.put("DCC_APPLICATION_CUSTOM_SOURCE",JsonUtils.parseObject(JsonUtils.toJsonString(resolvedRoute.applicationCustomSource()),Map.class));
        Map<String, List<Long>> startUserSelectAssignees = approvalRouteAssigneeResolver.buildStartUserSelectAssigneeMap(resolvedRoute.nodes());
        if(DccApplicationExecutionContractService.PUBLIC_V2.equals(file.getPreparedWorkflowContract())) {
            startUserSelectAssignees=new HashMap<>(startUserSelectAssignees);
            startUserSelectAssignees.put("DELAYED_EFFECTIVE_CONFIRM",List.of(file.getRequesterId()));
        }
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
                .setProcessDefinitionId(file.getPreparedProcessDefinitionId())
                .setName("DCC受控文件" + (DccControlledFileProcessDefinitionKeys.REVISION.equals(processDefinitionKey) ? "升版审批 " : "上传审批 ")
                        + file.getFileNumber() + " / " + file.getVersionNo())
                .setBusinessKey(String.valueOf(file.getId()))
                .setApprovalAuthority(new cn.iocoder.yudao.module.bpm.api.task.dto.ApprovalProcessAuthorityDTO(cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId(),"DCC_TASK","CONTROLLED_FILE",file.getId().toString(),file.getVersionNo(),"DCC_CONTROLLED_FILE_SUBMIT",file.getId().toString()))
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
        boolean nativeTraining = isThreeWorkflowUploadOrRevision(file);
        if ((!nativeTraining && (!permissionApi.hasAnyPermissions(userId,"dcc:controlled-file:approve")
                    || !permissionApi.hasAnyRoles(userId,"doc_control")
                    || !categoryPermissionSupport.hasCategoryPermission(file.getCategoryId(),userId,DccFileCategoryPermissionActionEnum.APPROVE)))
                || !Boolean.TRUE.equals(file.getNeedTraining())
                || file.getTrainingRecordFileId() != null
                || file.getPublishedFileId() != null
                || !DccControlledFileStatusEnum.PENDING_APPLICANT_TRAINING_RECORD.getStatus().equals(file.getStatus())) {
            throw exception(CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED);
        }
        if (reqVO == null) {
            throw exception(CONTROLLED_FILE_TRAINING_RECORD_REQUIRED);
        }
        if(nativeTraining) requireNativeTrainingContext(userId,file,reqVO.getSessionId());
        cn.iocoder.yudao.module.dcc.service.upload.DccTrainingUploadTicketBoundFile trainingBinding = uploadTicketService.resolveTrainingForBinding(
                new DccUploadTicketResolveCommand(reqVO.getTrainingRecordUploadTicket(), userId, file.getCategoryId(),
                        reqVO.getSessionId(),
                        DccControlledFileUploadTypePolicy.PURPOSE_TRAINING_RECORD));
        DccUploadTicketBoundFile trainingRecord = trainingBinding == null ? null : trainingBinding.file();
        if (trainingRecord == null || trainingRecord.storageFileId() == null) {
            throw exception(CONTROLLED_FILE_UPLOAD_TICKET_INVALID);
        }
        if(nativeTraining) {
            Long trainingAttachmentId=attachmentService.bindTrainingRecord(userId,id,file.getProcessInstanceId(),trainingBinding);
            if(trainingAttachmentId==null || trainingAttachmentId<=0)
                throw new IllegalStateException("培训附件绑定未返回真实附件身份");
        }
        uploadTicketService.markBound(new DccUploadTicketMarkBoundCommand(reqVO.getTrainingRecordUploadTicket(), userId,
                file.getCategoryId(), reqVO.getSessionId(),
                DccControlledFileUploadTypePolicy.PURPOSE_TRAINING_RECORD, id));
        boolean threeWorkflowUploadOrRevision = isThreeWorkflowUploadOrRevision(file);
        boolean trainingAfterDocControl=threeWorkflowUploadOrRevision&&isPublicBoundWorkflow(file);
        if(controlledFileMapper.updateById(DccControlledFileDO.builder()
                .id(id)
                .trainingRecordFileId(trainingRecord.storageFileId())
                .status(trainingAfterDocControl?DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus():threeWorkflowUploadOrRevision
                        ? DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus()
                        : DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL.getStatus())
                .build())!=1) throw new IllegalStateException("培训记录保存失败");
        if (threeWorkflowUploadOrRevision
                && !bpmTaskService.triggerTask(file.getProcessInstanceId(), "TRAINING")) {
            throw new IllegalStateException("DCC training BPM task was not triggered");
        }
        if(trainingAfterDocControl && "DELAYED".equals(file.getEffectiveMode())) {
            var running=bpmTaskService.getRunningTaskListByProcessInstanceId(file.getProcessInstanceId(),null,null);
            if(running.size()!=1 || !"DELAYED_EFFECTIVE_CONFIRM".equals(running.get(0).getTaskDefinitionKey())
                    || !String.valueOf(file.getRequesterId()).equals(running.get(0).getAssignee()))
                throw new IllegalStateException("必要培训完成后未生成原编制人的本轮延迟确认任务");
            updateWorkflowStatusAfterApprove(file,DccDelayedEffectiveConfirmationEvidence.STATUS,false);
        }
    }

    @Override
    public void validateTrainingRecordUpload(Long userId,Long fileId,Long categoryId,String sessionId) {
        var file=fileId==null?null:controlledFileMapper.selectById(fileId);
        if(file==null || !isThreeWorkflowUploadOrRevision(file) || !Objects.equals(file.getId(),fileId)
                || !Objects.equals(file.getTenantId(),TenantContextHolder.getRequiredTenantId())
                || !Objects.equals(file.getCategoryId(),categoryId) || !Boolean.TRUE.equals(file.getNeedTraining())
                || !"PENDING_APPLICANT_TRAINING_RECORD".equals(file.getStatus()) || file.getTrainingRecordFileId()!=null
                || file.getPublishedFileId()!=null)
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
            lifecycleService.requireReviewEffectiveSelection(context.file());
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
        if(isThreeWorkflowUploadOrRevision(context.file()) && context.stageCode()==DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW)
            lifecycleService.recordStandardizationSignatureEffect(userId,id,signature.getSignatureId());
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
        if (context.stageCode()==DccControlledFileStageCodeEnum.MATRIX_APPROVAL
                && Boolean.TRUE.equals(context.file().getNeedTraining()) && isThreeWorkflowUploadOrRevision(context.file())) {
            try { offlineTraining.requireConfiguredRecipients(context.file()); }
            catch (ServiceException missing) {
                if (missing.getCode()!=cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_OFFLINE_TRAINING_CONFIG_REQUIRED.getCode()) throw missing;
                var readiness=new DccControlledFileTaskReadinessRespVO().setReady(false).setFinalApproval(false)
                        .setBlockers(List.of(taskReadinessBlocker("OFFLINE_TRAINING_CONFIG_REQUIRED",missing.getMessage())));
                return new DocControlApprovalReadinessEvaluation(readiness,null);
            }
        }
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
        validateSubmitRequest(reqVO,requireUploadTickets,requireDirectory,true);
    }
    private void validateSubmitRequest(DccControlledFileSubmitReqVO reqVO,boolean requireUploadTickets,boolean requireDirectory,boolean submitting) {
        if (reqVO == null
                || reqVO.getCategoryId() == null
                || StrUtil.isBlank(reqVO.getFileName())
                || StrUtil.isBlank(reqVO.getFileNumber())
                || (requireDirectory && reqVO.getDirectoryId() == null)) {
            throw exception(CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING);
        }
        if("EXTERNAL_REVIEW".equals(reqVO.getProcessType())) {
            workflowDatePolicy.requireReviewDate(reqVO.getEffectiveDate());
        } else {
            if(submitting) workflowDatePolicy.requireEffectiveConfiguration(reqVO.getEffectiveMode(),reqVO.getEffectiveDate());
            else requireDraftEffectiveSelection(reqVO.getEffectiveMode(),reqVO.getEffectiveDate());
            if(submitting && reqVO.getSelectedDistributionDepartmentIds()==null)
                throw new IllegalArgumentException("请明确保存本次下发部门名单，未选择不能推断为会签名单");
        }
        DccControlledFileChangeTypeEnum changeType = validateChangeType(reqVO.getChangeType());
        boolean explicitRevisionSource = changeType == DccControlledFileChangeTypeEnum.REVISION
                && reqVO.getRevisionSourceControlledFileId() != null;
        boolean ordinaryDualVersionUpload = !"EXTERNAL_REVIEW".equalsIgnoreCase(reqVO.getProcessType())
                && !explicitRevisionSource;
        boolean hasUploadTicket = hasAnyUploadTicket(reqVO);
        if (requireUploadTickets && !explicitRevisionSource && hasAnyRawFileId(reqVO)) {
            throw exception(CONTROLLED_FILE_UPLOAD_TICKET_INVALID);
        }
        if ((requireUploadTickets && !explicitRevisionSource) || hasUploadTicket) {
            if (ordinaryDualVersionUpload && StrUtil.isBlank(reqVO.getReadOnlyUploadTicket())) {
                throw exception(CONTROLLED_FILE_UPLOAD_TICKET_INVALID);
            }
            if (!ordinaryDualVersionUpload && StrUtil.isBlank(reqVO.getOriginalUploadTicket())) {
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
        return reqVO != null && (StrUtil.isNotBlank(reqVO.getReadOnlyUploadTicket())
                || StrUtil.isNotBlank(reqVO.getEditableUploadTicket())
                || StrUtil.isNotBlank(reqVO.getOriginalUploadTicket())
                || StrUtil.isNotBlank(reqVO.getSourceUploadTicket())
                || StrUtil.isNotBlank(reqVO.getDrawingPdfUploadTicket()));
    }

    private boolean hasAnyRawFileId(DccControlledFileSubmitReqVO reqVO) {
        return reqVO != null && (reqVO.getOriginalFileId() != null
                || reqVO.getSourceFileId() != null
                || reqVO.getReadOnlyFileId() != null
                || reqVO.getEditableFileId() != null
                || reqVO.getDrawingPdfFileId() != null);
    }

    private ResolvedSubmitFiles resolveSubmitFiles(Long userId, DccControlledFileSubmitReqVO reqVO,
                                                   boolean requireUploadTickets) {
        if (requireUploadTickets || hasAnyUploadTicket(reqVO)) {
            boolean ordinaryDualVersionUpload = !"EXTERNAL_REVIEW".equalsIgnoreCase(reqVO.getProcessType());
            if (StrUtil.isBlank(reqVO.getSessionId())
                    || (ordinaryDualVersionUpload
                    ? StrUtil.isBlank(reqVO.getReadOnlyUploadTicket())
                    : StrUtil.isBlank(reqVO.getOriginalUploadTicket()))) {
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
            DccUploadTicketBoundFile readOnly = ordinaryDualVersionUpload
                    ? resolveUploadTicket(userId, reqVO.getCategoryId(), reqVO.getSessionId(),
                    reqVO.getReadOnlyUploadTicket(), DccControlledFileUploadTypePolicy.PURPOSE_READ_ONLY_VIEW)
                    : resolveUploadTicket(userId, reqVO.getCategoryId(), reqVO.getSessionId(),
                    reqVO.getOriginalUploadTicket(), DccControlledFileUploadTypePolicy.PURPOSE_SOURCE);
            String readOnlyTicket = ordinaryDualVersionUpload
                    ? reqVO.getReadOnlyUploadTicket() : reqVO.getOriginalUploadTicket();
            bindings.add(new SubmitTicketBinding(readOnlyTicket,
                    ordinaryDualVersionUpload ? DccControlledFileUploadTypePolicy.PURPOSE_READ_ONLY_VIEW
                            : DccControlledFileUploadTypePolicy.PURPOSE_SOURCE));
            DccUploadTicketBoundFile editable = null;
            if (ordinaryDualVersionUpload && StrUtil.isNotBlank(reqVO.getEditableUploadTicket())) {
                editable = resolveUploadTicket(userId, reqVO.getCategoryId(), reqVO.getSessionId(),
                        reqVO.getEditableUploadTicket(), DccControlledFileUploadTypePolicy.PURPOSE_EDITABLE_SOURCE);
                bindings.add(new SubmitTicketBinding(reqVO.getEditableUploadTicket(),
                        DccControlledFileUploadTypePolicy.PURPOSE_EDITABLE_SOURCE));
            } else if (!ordinaryDualVersionUpload && StrUtil.isNotBlank(reqVO.getSourceUploadTicket())) {
                editable = resolveUploadTicket(userId, reqVO.getCategoryId(), reqVO.getSessionId(), reqVO.getSourceUploadTicket(),
                        DccControlledFileUploadTypePolicy.PURPOSE_SOURCE);
                bindings.add(new SubmitTicketBinding(reqVO.getSourceUploadTicket(),
                        DccControlledFileUploadTypePolicy.PURPOSE_SOURCE));
            }
            DccUploadTicketBoundFile source = editable == null ? readOnly : editable;
            if (!ordinaryDualVersionUpload && StrUtil.isNotBlank(reqVO.getSourceUploadTicket())
                    && editable == null) {
                source = resolveUploadTicket(userId, reqVO.getCategoryId(), reqVO.getSessionId(), reqVO.getSourceUploadTicket(),
                        DccControlledFileUploadTypePolicy.PURPOSE_SOURCE);
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
            Long readOnlyFileId = ordinaryDualVersionUpload ? readOnly.storageFileId() : null;
            Long editableFileId = ordinaryDualVersionUpload && editable != null ? editable.storageFileId() : null;
            return new ResolvedSubmitFiles(readOnly.storageFileId(), source.storageFileId(), readOnlyFileId,
                    editableFileId, drawingPdf == null ? null : drawingPdf.storageFileId(), attachmentFiles, bindings, source.fileName());
        }
        Long readOnlyFileId = reqVO.getReadOnlyFileId() == null ? reqVO.getOriginalFileId() : reqVO.getReadOnlyFileId();
        Long editableFileId = reqVO.getEditableFileId();
        Long sourceFileId = reqVO.getSourceFileId() != null ? reqVO.getSourceFileId()
                : (editableFileId == null ? readOnlyFileId : editableFileId);
        var stored = sourceFileId == null ? null : fileMapper.selectById(sourceFileId);
        return new ResolvedSubmitFiles(reqVO.getOriginalFileId(), sourceFileId, readOnlyFileId, editableFileId,
                reqVO.getDrawingPdfFileId(), List.of(), List.of(), stored == null ? null : stored.getName());
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
        return prepareSubmitContext(userId,reqVO,requireLeafDirectory,requireScreenshotMetadata,ignoredControlledFileId,requireUploadTickets,storage,true);
    }
    private PreparedSubmitContext prepareSubmitContext(Long userId,DccControlledFileSubmitReqVO reqVO,boolean requireLeafDirectory,boolean requireScreenshotMetadata,Long ignoredControlledFileId,boolean requireUploadTickets,DccDerivedUploadStorage storage,boolean submitting) {
        if(storage!=null)requireDerivedStorage(userId,reqVO,storage);
        validateSubmitRequest(reqVO, requireUploadTickets,storage==null,submitting);
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
        // Native formal revisions retain their target through the normal correction/checkin chain.
        if (DccControlledFileProcessDefinitionKeys.REVISION.equals(file.getProcessDefinitionKey()))
            throw exception(CONTROLLED_FILE_WITHDRAWN_ACTION_NOT_ALLOWED);
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
        reqVO.setReadOnlyFileId(file.getReadOnlyFileId());
        reqVO.setEditableFileId(file.getEditableFileId());
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
        reqVO.setEffectiveMode(file.getEffectiveMode());
        reqVO.setSelectedDistributionDepartmentIds(readDistributionDepartments(file.getDistributionDepartmentIdsJson()));
        reqVO.setSelectedSignoffDepartmentIds(readDistributionDepartments(file.getSelectedSignoffDepartmentIdsJson()));
        reqVO.setRemark(file.getRemark());
        return reqVO;
    }

    private DccControlledFileDO insertControlledFile(PreparedSubmitContext context, Long userId,
                                                       String status, String processDefinitionKey,
                                                       boolean submitted, String ownershipType) {
        return insertControlledFile(context,userId,status,processDefinitionKey,submitted,ownershipType,context.reqVO().getSelectedSignoffDepartmentIds());
    }
    private DccControlledFileDO insertControlledFile(PreparedSubmitContext context,Long userId,String status,String processDefinitionKey,
            boolean submitted,String ownershipType,List<Long> frozenSignoffDepartments) {
        return insertControlledFile(context,userId,status,processDefinitionKey,submitted,ownershipType,frozenSignoffDepartments,null);
    }
    private DccControlledFileDO insertControlledFile(PreparedSubmitContext context,Long userId,String status,String processDefinitionKey,
            boolean submitted,String ownershipType,List<Long> frozenSignoffDepartments,String originalMatrix) {
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
                .readOnlyFileId(context.submitFiles().readOnlyFileId())
                .editableFileId(context.submitFiles().editableFileId())
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
                .needTraining(submitted?Boolean.TRUE.equals(context.reqVO().getNeedTraining()):context.reqVO().getNeedTraining())
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
                .effectiveMode(context.reqVO().getEffectiveMode())
                .distributionDepartmentIdsJson(serializeDistributionDepartments(context.reqVO().getSelectedDistributionDepartmentIds()))
                .selectedSignoffDepartmentIdsJson(serializeDistributionDepartments(frozenSignoffDepartments))
                .draftApprovalMatrixJson(originalMatrix)
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
        if (controlledFileMapper.updateById(DccControlledFileDO.builder().id(file.getId()).status(status)
                .preparedWorkflowContract(file.getPreparedWorkflowContract()).preparedProcessDefinitionId(file.getPreparedProcessDefinitionId())
                .preparedDefinitionDigest(file.getPreparedDefinitionDigest()).build()) != 1) {
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
                submitFiles.readOnlyFileId(), submitFiles.editableFileId(),
                submitFiles.drawingPdfFileId(), submitFiles.attachmentFiles(), submitFiles.ticketBindings(), submitFiles.sourceOriginalFileName());
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
                                       Long readOnlyFileId,
                                       Long editableFileId,
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
        if(DccApplicationExecutionContractService.isPublicContract(file.getPreparedWorkflowContract())){
            var bound=executionContracts.resolveBoundExecution(file);
            if(!bound.active()||!Objects.equals(bound.processDefinitionId(),task.getProcessDefinitionId())
                    ||!Objects.equals(bound.processInstanceId(),task.getProcessInstanceId()))throw exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
        }
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
        if(stageCode==DccControlledFileStageCodeEnum.SUPERVISOR_REVIEW){
            if(!DccApplicationExecutionContractService.isPublicContract(file.getPreparedWorkflowContract())||!Integer.valueOf(0).equals(stageSnapshot.getStageNo())
                    ||!Integer.valueOf(0).equals(stageSnapshot.getStageOrder())||!"USER".equals(stageSnapshot.getCandidateSourceType())
                    ||parseResolvedUserIds(stageSnapshot).size()!=1||!Objects.equals(stageSnapshot.getCandidateSourceId(),userId)
                    ||!String.valueOf(userId).equals(task.getAssignee()))throw exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
        }
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
            case DELAYED_EFFECTIVE_CONFIRM -> throw new IllegalArgumentException("延迟生效确认必须通过选择生效时间的正式电子签名入口办理");
            case SUPERVISOR_REVIEW, DOC_CONTROL_REVIEW, MATRIX_REVIEW -> REVIEW_PERMISSION;
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
        if(isPublicBoundWorkflow(file) && currentStageCode==DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW
                && DccApplicationExecutionContractService.PUBLIC_V2.equals(file.getPreparedWorkflowContract()) && "DELAYED".equals(file.getEffectiveMode())
                && !Boolean.TRUE.equals(file.getNeedTraining())) {
            if(runningTasks.size()!=1 || !"DELAYED_EFFECTIVE_CONFIRM".equals(runningTasks.get(0).getTaskDefinitionKey())
                    || !String.valueOf(file.getRequesterId()).equals(runningTasks.get(0).getAssignee()))
                throw new IllegalStateException("文控完成后未生成原编制人的本轮延迟确认任务");
            updateWorkflowStatusAfterApprove(file,DccDelayedEffectiveConfirmationEvidence.STATUS,false);return DccDelayedEffectiveConfirmationEvidence.STATUS;
        }
        if(isPublicBoundWorkflow(file)&&currentStageCode==DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW&&runningTasks.isEmpty()){
            var executions=runtimeService.createExecutionQuery().processInstanceId(file.getProcessInstanceId()).activityId("TRAINING").list();
            if(Boolean.TRUE.equals(file.getNeedTraining())&&file.getTrainingRecordFileId()==null){
                if(executions.size()!=1)throw new IllegalStateException("DCC_V5_EXACT_TRAINING_EXECUTION_REQUIRED");
                updateWorkflowStatusAfterApprove(file,DccOfflineTrainingRecordService.STATUS,false);return DccOfflineTrainingRecordService.STATUS;
            }
            if(!executions.isEmpty())throw new IllegalStateException("DCC_V5_UNEXPECTED_TRAINING_EXECUTION");
            return resolveLatestStatus(file);
        }
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
            if (!isPublicBoundWorkflow(file)&&Boolean.TRUE.equals(file.getNeedTraining()) && file.getTrainingRecordFileId() == null) {
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
    private boolean isPublicBoundWorkflow(DccControlledFileDO file){
        if(file==null||!DccApplicationExecutionContractService.isPublicContract(file.getPreparedWorkflowContract()))return false;
        return executionContracts.resolveBoundExecution(file).trainingAfterDocumentControl();
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
            case SUPERVISOR_REVIEW -> DccControlledFileStatusEnum.PENDING_SUPERVISOR_REVIEW.getStatus();
            case DELAYED_EFFECTIVE_CONFIRM -> DccControlledFileStatusEnum.PENDING_DELAYED_EFFECTIVE_CONFIRM.getStatus();
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
            case "PENDING_SUPERVISOR_REVIEW" -> DccControlledFileStageCodeEnum.SUPERVISOR_REVIEW;
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
        respVO.setEffectiveMode(file.getEffectiveMode());
        respVO.setEffectiveTime(file.getEffectiveTime());
        respVO.setEffectiveSignatureId(file.getEffectiveSignatureId());
        respVO.setEffectiveProcessInstanceId(file.getEffectiveProcessInstanceId());
        var distributionDepartments=readDistributionDepartments(file.getDistributionDepartmentIdsJson());
        respVO.setSelectedDistributionDepartmentIds(distributionDepartments==null?null:distributionDepartments.stream().map(String::valueOf).toList());
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

    private List<Long> normalizeDistributionDepartments(List<Long> selected) {
        if(selected==null) return null;
        if(selected.stream().anyMatch(id->id==null || id<=0) || selected.stream().distinct().count()!=selected.size())
            throw new IllegalArgumentException("下发部门身份必须为唯一精确正整数");
        var normalized=selected.stream().sorted().toList();
        if(normalized.isEmpty()) return normalized;
        deptApi.validateDeptList(normalized);
        var departments=deptApi.getDeptList(normalized);
        if(departments==null || departments.size()!=normalized.size()
                || departments.stream().anyMatch(dept->dept==null || dept.getId()==null || !Integer.valueOf(0).equals(dept.getStatus()))
                || departments.stream().map(DeptRespDTO::getId).distinct().count()!=departments.size()
                || !new java.util.HashSet<>(normalized).equals(departments.stream().map(DeptRespDTO::getId).collect(Collectors.toSet())))
            throw new IllegalStateException("正式下发部门不存在、停用或目录身份不完整");
        return normalized;
    }

    private String serializeDistributionDepartments(List<Long> selected) {
        if(selected==null) return null;
        return selected.stream().map(id->"\""+id+"\"").collect(Collectors.joining(",","[","]"));
    }

    private void persistApplicationSelection(DccControlledFileDO file,String mode,LocalDate date,List<Long> departments) {
        String distributionJson=serializeDistributionDepartments(departments);
        if(Objects.equals(file.getEffectiveMode(),mode) && Objects.equals(file.getEffectiveDate(),date)
                && Objects.equals(file.getDistributionDepartmentIdsJson(),distributionJson)) return;
        if(controlledFileMapper.update(null,new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<DccControlledFileDO>()
                .eq(DccControlledFileDO::getId,file.getId()).eq(DccControlledFileDO::getTenantId,TenantContextHolder.getRequiredTenantId())
                .eq(DccControlledFileDO::getStatus,"WORKING")
                .set(DccControlledFileDO::getEffectiveMode,mode).set(DccControlledFileDO::getEffectiveDate,date)
                .set(DccControlledFileDO::getDistributionDepartmentIdsJson,distributionJson))!=1)
            throw new IllegalStateException("本次生效选择和下发部门保存失败");
        file.setEffectiveMode(mode);file.setEffectiveDate(date);file.setDistributionDepartmentIdsJson(distributionJson);
    }
    private void persistPreparedDefinition(DccControlledFileDO file,DccApplicationExecutionContractService.Prepared prepared){
        if(file.getEffectiveMode()!=null)workflowDatePolicy.requireApplicationEffectiveConfiguration(file.getEffectiveMode(),file.getEffectiveDate(),prepared.executionContract());
        if(file.getPreparedWorkflowContract()!=null){
            if(!Objects.equals(file.getPreparedWorkflowContract(),prepared.executionContract())||!Objects.equals(file.getPreparedProcessDefinitionId(),prepared.processDefinitionId())
                    ||!Objects.equals(file.getPreparedDefinitionDigest(),prepared.definitionDigest()))throw new IllegalStateException("EXECUTION_CONTRACT_PREPARED_IDENTITY_CONFLICT");
            return;
        }
        if(controlledFileMapper.updateById(DccControlledFileDO.builder().id(file.getId()).preparedWorkflowContract(prepared.executionContract())
                .preparedProcessDefinitionId(prepared.processDefinitionId()).preparedDefinitionDigest(prepared.definitionDigest()).build())!=1)
            throw new IllegalStateException("EXECUTION_CONTRACT_PREPARED_WRITE_FAILED");
        file.setPreparedWorkflowContract(prepared.executionContract());file.setPreparedProcessDefinitionId(prepared.processDefinitionId());file.setPreparedDefinitionDigest(prepared.definitionDigest());
    }

    private void requireDraftEffectiveSelection(String mode,LocalDate date) {
        if(mode==null && date==null) return;
        if(DccApplicationEffectiveSelection.PRESET_DATE.equals(mode) && date==null) return;
        workflowDatePolicy.requireEffectiveConfiguration(mode,date);
    }

    private void persistSignoffSelection(DccControlledFileDO file,DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute route) {
        var selected=route.nodes().stream().filter(node->"MATRIX_REVIEW".equals(node.stageCode())).findFirst()
                .orElseThrow(()->exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH)).candidateSourceIds();
        if(selected==null || selected.isEmpty()) throw exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
        String json=serializeDistributionDepartments(selected);
        if(Objects.equals(json,file.getSelectedSignoffDepartmentIdsJson())) return;
        if(controlledFileMapper.updateById(DccControlledFileDO.builder().id(file.getId()).selectedSignoffDepartmentIdsJson(json).build())!=1)
            throw new IllegalStateException("本次会签部门快照保存失败");
        file.setSelectedSignoffDepartmentIdsJson(json);
    }

    private String workingSelectionUnavailable(DccControlledFileDO file) {
        if(!"APPLICATION_CUSTOM".equals(file.getDraftApprovalSourceMode())&&DccApplicationExecutionContractService.isPublicContract(file.getPreparedWorkflowContract())&&file.getDraftApprovalMatrixJson()==null)
            return "新业务申请的公共矩阵原基线尚未正式准备";
        if(!DccApplicationEffectiveSelection.isComplete(file.getEffectiveMode(),file.getEffectiveDate()))
            return "本次生效方式或日期尚未完整填写";
        if(file.getDistributionDepartmentIdsJson()==null) return "本次下发部门尚未明确保存";
        var signoff=readDistributionDepartments(file.getSelectedSignoffDepartmentIdsJson());
        if(signoff==null || signoff.isEmpty()) return "本次会签部门尚未完整保存";
        if(file.getNeedTraining()==null) return "本次培训选择尚未填写";
        readDistributionDepartments(file.getDistributionDepartmentIdsJson());
        try {workflowDatePolicy.requireEffectiveConfiguration(file.getEffectiveMode(),file.getEffectiveDate());}
        catch(cn.iocoder.yudao.framework.common.exception.ServiceException invalid){return invalid.getMessage();}
        return null;
    }
    private DccApplicationExecutionContractService.BaselinePreparationEligibility newActionBaselineEligibility(Long actor,DccControlledFileDO file){
        if(file==null||"APPLICATION_CUSTOM".equals(file.getDraftApprovalSourceMode())||file.getDraftApprovalMatrixJson()!=null||!"REVISION".equals(file.getChangeType())
                ||!DccApplicationExecutionContractService.isPublicContract(file.getPreparedWorkflowContract()))
            return new DccApplicationExecutionContractService.BaselinePreparationEligibility(false,false);
        return executionContracts.discoverNewActionBaselinePreparation(actor,file);
    }

    private List<String> distributionDepartmentStrings(String json) {
        var ids=readDistributionDepartments(json);
        return ids==null?null:ids.stream().map(String::valueOf).toList();
    }
    private String canonicalPublicMatrix(cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccApprovalMatrixBaselineVO baseline){
        return DccApprovalMatrixCanonicalizer.canonicalizeBaseline(baseline);
    }

    private List<Long> readDistributionDepartments(String json) {
        if(json==null) return null;
        var values=JsonUtils.parseObject(json,List.class);
        if(values==null) throw new IllegalStateException("本次下发部门快照不是正式数组");
        var ids=new ArrayList<Long>();
        for(Object value:values) {
            if(!(value instanceof String text) || !text.matches("[1-9][0-9]*"))
                throw new IllegalStateException("本次下发部门快照不是精确十进制字符串");
            try {ids.add(Long.valueOf(text));}
            catch(NumberFormatException invalid){throw new IllegalStateException("本次下发部门快照超出正式Long身份",invalid);}
        }
        if(ids.stream().distinct().count()!=ids.size() || !Objects.equals(json,serializeDistributionDepartments(ids)))
            throw new IllegalStateException("本次下发部门快照重复或不是规范化载荷");
        return List.copyOf(ids);
    }

    private Map<String, List<Long>> buildStageAssigneeMapFromSnapshots(List<DccControlledFileRouteSnapshotDO> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            throw exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
        }
        Map<String, List<Long>> assignees = new LinkedHashMap<>();
        for (DccControlledFileRouteSnapshotDO snapshot : snapshots) {
            if (snapshot == null || StrUtil.isBlank(snapshot.getStageCode())
                    || StrUtil.isBlank(snapshot.getResolvedUserIds())) {
                throw exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
            }
            // Repeated users represent separate department obligations; membership checks use a different Set parser.
            List<Long> userIds = Arrays.stream(snapshot.getResolvedUserIds().split(",", -1))
                    .map(String::trim)
                    .map(value -> {
                        if (!value.matches("[1-9][0-9]*")) {
                            throw exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
                        }
                        try {
                            return Long.valueOf(value);
                        } catch (NumberFormatException invalidIdentity) {
                            throw exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
                        }
                    }).toList();
            if (assignees.putIfAbsent(snapshot.getStageCode(), userIds) != null) {
                throw exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
            }
        }
        return assignees;
    }
}
