package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
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
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRoutePreviewRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRouteSnapshotRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSubmitReqVO;
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
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccCategoryDirectoryBindingMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryPermissionRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccFileDirectoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionRecipientMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileRouteSnapshotMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSignatureMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.enums.DccAccessTypeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccAccessSubjectTypeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileDistributionStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileChangeTypeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileProcessTypeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStageCodeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileMasterStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFilePreviewKindEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccDistributionMediumEnum;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.dcc.enums.DccProjectCodeStatusConstants;
import cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyAdminService;
import cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyPath;
import cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessPermissionService;
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
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import jakarta.annotation.Resource;
import org.flowable.task.api.Task;
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

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_CATEGORY_DISABLED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_DRAWING_PDF_FILE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_DRAWING_PDF_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_DISTRIBUTION_DEPARTMENT_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_DISTRIBUTION_MEDIUM_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ACCESS_DENIED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_FILE_NUMBER_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PRODUCT_CODE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PRODUCT_MASTER_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PROCESS_TYPE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ROUTE_NOT_CONFIGURED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SOURCE_FILE_TYPE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_STAMPED_PDF_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_EVIDENCE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_PERSIST_FAILED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SUBMIT_DIRECTORY_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SUBMIT_DIRECTORY_NOT_LEAF;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_TASK_STAGE_UNSUPPORTED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_TASK_TARGET_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_TRAINING_RECORD_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_UPLOAD_TICKET_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_VERSION_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_VERSION_NOT_GREATER;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_WITHDRAW_NOT_ALLOWED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_WITHDRAWN_ACTION_NOT_ALLOWED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_WORKFLOW_IN_PROGRESS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.EXTERNAL_FILE_REVIEW_ENDPOINT_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_CATEGORY_DIRECTORY_BINDING_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_CATEGORY_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_TYPE_TAXONOMY_LEVEL_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_CODE_DISABLED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_CODE_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.ROUTE_PREVIEW_APPROVER_NOT_FOUND;

@Service
@Validated
public class DccControlledFileWorkflowServiceImpl implements DccControlledFileWorkflowService {

    public static final String BPM_PROCESS_DEFINITION_KEY = "dcc-controlled-file-approval";
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
    private DccControlledFileMasterMapper controlledFileMasterMapper;
    @Resource
    private DccControlledFileRouteSnapshotMapper routeSnapshotMapper;
    @Resource
    private DccControlledFileDistributionMapper distributionMapper;
    @Resource
    private DccControlledFileDistributionRecipientMapper distributionRecipientMapper;
    @Resource
    private DccControlledFileSignatureMapper signatureMapper;
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
    private DccDirectoryAccessPermissionService directoryAccessPermissionService;
    @Resource
    private DccSignatureVerificationService signatureVerificationService;
    @Resource
    private DccControlledFileFinalizationService finalizationService;
    @Resource
    private DccUploadTicketService uploadTicketService;
    @Resource
    private MdmProductApi productApi;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private DccControlledFileCategoryPermissionSupport permissionSupport;
    @Resource
    private DccProjectCodeMapper projectCodeMapper;
    @Resource
    private DccFileTypeTaxonomyAdminService fileTypeTaxonomyAdminService;
    @Resource
    private DccControlledFileQueryService queryService;
    @Resource
    private DccControlledContentAdapter platformAdapter;
    @Resource
    private DccControlledFileApprovalRouteAssigneeResolver approvalRouteAssigneeResolver;

    @Override
    public List<DccControlledFileRoutePreviewRespVO> previewRoute(Long userId, Long categoryId) {
        DccFileCategoryDO category = validateCategory(categoryId);
        validateCategoryUploadPermission(category.getId(), userId);
        return convertList(approvalRouteAssigneeResolver.resolveRoute(category.getId(), userId).nodes(),
                this::toRoutePreviewRespVO);
    }

    @Override
    public DccControlledFileCurrentVersionRespVO getCurrentVersionByFileNumber(Long userId, String fileNumber) {
        String normalizedFileNumber = normalizeFileNumber(fileNumber);
        List<DccControlledFileMasterDO> masters = controlledFileMasterMapper.selectListByFileNumber(normalizedFileNumber);
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitControlledFile(Long userId, DccControlledFileSubmitReqVO reqVO) {
        if (StrUtil.equals(reqVO.getProcessType(), DccControlledFileProcessTypeEnum.EXTERNAL_REVIEW.getCode())) {
            throw exception(EXTERNAL_FILE_REVIEW_ENDPOINT_REQUIRED);
        }
        return submitControlledFile(userId, reqVO, null, BPM_PROCESS_DEFINITION_KEY, true);
    }

    Long submitControlledFileWithProcessDefinitionKey(Long userId, DccControlledFileSubmitReqVO reqVO,
                                                      String processDefinitionKey) {
        return submitControlledFile(userId, reqVO, null, processDefinitionKey, true);
    }

    private Long submitControlledFile(Long userId, DccControlledFileSubmitReqVO reqVO, Long ignoredControlledFileId,
                                      String processDefinitionKey, boolean requireUploadTickets) {
        PreparedSubmitContext context = prepareSubmitContext(userId, reqVO, true, true, ignoredControlledFileId,
                requireUploadTickets);
        DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute resolvedRoute = applySelectedSignoffUsers(
                approvalRouteAssigneeResolver.resolveRoute(context.category().getId(), userId), reqVO.getSelectedSignoffUserIds());
        DccControlledFileDO file = insertControlledFile(context, userId,
                toPendingStatus(resolvedRoute.nodes().get(0).stageNo()), processDefinitionKey);
        bindSubmitTickets(context, userId, file.getId());

        for (DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode routeNode : resolvedRoute.nodes()) {
            routeSnapshotMapper.insert(DccControlledFileRouteSnapshotDO.builder()
                    .controlledFileId(file.getId())
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
                    .build());
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("controlledFileId", file.getId());
        variables.put("categoryId", file.getCategoryId());
        variables.put("directoryId", file.getDirectoryId());
        Map<String, List<Long>> startUserSelectAssignees = approvalRouteAssigneeResolver.buildStartUserSelectAssigneeMap(resolvedRoute.nodes());
        variables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_START_USER_SELECT_ASSIGNEES,
                startUserSelectAssignees);
        variables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_APPROVE_USER_SELECT_ASSIGNEES,
                approvalRouteAssigneeResolver.buildApproveUserSelectAssigneeMap(resolvedRoute.nodes()));
        String processInstanceId = bpmProcessInstanceApi.createProcessInstance(userId, new BpmProcessInstanceCreateReqDTO()
                .setProcessDefinitionKey(processDefinitionKey)
                .setBusinessKey(String.valueOf(file.getId()))
                .setStartUserSelectAssignees(startUserSelectAssignees)
                .setVariables(variables));
        controlledFileMapper.updateById(DccControlledFileDO.builder()
                .id(file.getId())
                .processInstanceId(processInstanceId)
                .build());
        file.setProcessInstanceId(processInstanceId);
        platformAdapter.recordSubmitted(file, userId, processInstanceId);
        return file.getId();
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
        DccControlledFileDO file = insertControlledFile(context, userId,
                splitRevisionApproval
                        ? DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus()
                        : DccControlledFileStatusEnum.FINALIZING.getStatus(),
                null);
        if (normalizedProcessInstanceId != null) {
            controlledFileMapper.updateById(DccControlledFileDO.builder()
                    .id(file.getId())
                    .processInstanceId(normalizedProcessInstanceId)
                    .build());
            file.setProcessInstanceId(normalizedProcessInstanceId);
        }
        bindSubmitTickets(context, userId, file.getId());
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
    public PageResult<DccControlledFileRespVO> getUploadRevisionCandidates(Long userId, Long dccProjectCodeId,
                                                                           Long fileTypeTaxonomyId, String keyword,
                                                                           Integer pageNo, Integer pageSize) {
        validateEnabledProjectCode(dccProjectCodeId, true);
        ResolvedFileTypeTaxonomy taxonomy = resolveFileTypeTaxonomy(fileTypeTaxonomyId, true);
        DccControlledFilePageReqVO reqVO = new DccControlledFilePageReqVO();
        reqVO.setPageNo(pageNo == null ? 1 : pageNo);
        reqVO.setPageSize(pageSize == null ? 10 : pageSize);
        reqVO.setKeyword(StrUtil.trimToNull(keyword));
        reqVO.setDccProjectCodeId(dccProjectCodeId);
        reqVO.setFileTypeTaxonomyIds(taxonomy.activeDescendantIds());
        reqVO.setFileTypeTaxonomyPaths(toFileTypeTaxonomyPathFilters(taxonomy.activeDescendantPaths()));
        reqVO.setStatus(DccControlledFileStatusEnum.ACTIVE.getStatus());
        reqVO.setProcessType(DccControlledFileProcessTypeEnum.CONTROLLED_FILE.getCode());
        reqVO.setLatestVersionOnly(Boolean.TRUE);
        return queryService.getControlledFilePage(userId, reqVO);
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
        if (!userId.equals(file.getRequesterId())
                || !WITHDRAW_ALLOWED_STATUSES.contains(file.getStatus())
                || StrUtil.isBlank(file.getProcessInstanceId())) {
            throw exception(CONTROLLED_FILE_WITHDRAW_NOT_ALLOWED);
        }
        bpmProcessInstanceService.cancelProcessInstanceByStartUser(userId,
                new BpmProcessInstanceCancelReqVO().setId(file.getProcessInstanceId()).setReason(reqVO.getReason()));
        controlledFileMapper.updateById(DccControlledFileDO.builder()
                .id(file.getId())
                .status(DccControlledFileStatusEnum.WITHDRAWN.getStatus())
                .rejectReason(reqVO.getReason())
                .build());
        platformAdapter.recordWithdrawn(file, userId, reqVO.getReason());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWithdrawnControlledFile(Long userId, Long id) {
        DccControlledFileDO file = controlledFileMapper.selectById(id);
        validateWithdrawnApplicantAction(userId, file);
        Set<Long> artifactFileIds = collectWithdrawnArtifactFileIds(file);
        controlledFileMapper.deleteById(id);
        deleteUnreferencedArtifacts(artifactFileIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long resubmitWithdrawnControlledFile(Long userId, Long id) {
        DccControlledFileDO file = controlledFileMapper.selectById(id);
        validateWithdrawnApplicantAction(userId, file);
        Long newFileId = submitControlledFile(userId, toResubmitReqVO(file), id, BPM_PROCESS_DEFINITION_KEY, false);
        controlledFileMapper.updateById(DccControlledFileDO.builder()
                .id(file.getId())
                .supersededByFileId(newFileId)
                .build());
        platformAdapter.recordResubmitted(file, newFileId);
        return newFileId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadTrainingRecord(Long userId, Long id, DccControlledFileTrainingRecordReqVO reqVO) {
        DccControlledFileDO file = controlledFileMapper.selectById(id);
        if (file == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        if (!Objects.equals(userId, file.getRequesterId())
                || !Boolean.TRUE.equals(file.getNeedTraining())
                || file.getTrainingRecordFileId() != null
                || file.getPublishedFileId() != null
                || !DccControlledFileStatusEnum.PENDING_APPLICANT_TRAINING_RECORD.getStatus().equals(file.getStatus())) {
            throw exception(CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED);
        }
        if (reqVO == null) {
            throw exception(CONTROLLED_FILE_TRAINING_RECORD_REQUIRED);
        }
        DccUploadTicketBoundFile trainingRecord = uploadTicketService.resolveForBinding(
                new DccUploadTicketResolveCommand(reqVO.getTrainingRecordUploadTicket(), userId, reqVO.getSessionId(),
                        DccControlledFileUploadTypePolicy.PURPOSE_TRAINING_RECORD));
        if (trainingRecord == null || trainingRecord.storageFileId() == null) {
            throw exception(CONTROLLED_FILE_UPLOAD_TICKET_INVALID);
        }
        uploadTicketService.markBound(new DccUploadTicketMarkBoundCommand(reqVO.getTrainingRecordUploadTicket(), userId,
                reqVO.getSessionId(), DccControlledFileUploadTypePolicy.PURPOSE_TRAINING_RECORD, id));
        controlledFileMapper.updateById(DccControlledFileDO.builder()
                .id(id)
                .trainingRecordFileId(trainingRecord.storageFileId())
                .status(DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL.getStatus())
                .build());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DccSignatureActionRespVO approveTask(Long userId, Long id, DccControlledFileApproveTaskReqVO reqVO) {
        return approveTaskWithProcessDefinitionKey(userId, id, reqVO, BPM_PROCESS_DEFINITION_KEY, true);
    }

    DccSignatureActionRespVO approveTaskWithProcessDefinitionKey(Long userId, Long id, DccControlledFileApproveTaskReqVO reqVO,
                                                                 String processDefinitionKey, boolean collectDocControlArtifacts) {
        ValidatedTaskActionContext context = validateTaskAction(userId, id, reqVO.getTaskId(), processDefinitionKey,
                "APPROVE");
        DocControlApprovalArtifacts docControlArtifacts = collectDocControlApprovalArtifactsIfRequired(userId, context,
                reqVO, collectDocControlArtifacts);
        Set<String> beforeRunningTaskIds = bpmTaskService.getRunningTaskListByProcessInstanceId(
                        context.file().getProcessInstanceId(), null, null)
                .stream()
                .map(Task::getId)
                .collect(Collectors.toSet());
        signatureVerificationService.verifyPasswordAndCreateSignature(userId, id, reqVO.getTaskId(),
                context.stageCode().getCode(), "APPROVE", reqVO.getPassword(), reqVO.getReason());
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
        return buildActionRespVO(requireActionSignature(id, reqVO.getTaskId(), userId, "APPROVE"),
                "APPROVED", nextStatus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DccSignatureActionRespVO rejectTask(Long userId, Long id, DccControlledFileRejectTaskReqVO reqVO) {
        return rejectTaskWithProcessDefinitionKey(userId, id, reqVO, BPM_PROCESS_DEFINITION_KEY);
    }

    DccSignatureActionRespVO rejectTaskWithProcessDefinitionKey(Long userId, Long id, DccControlledFileRejectTaskReqVO reqVO,
                                                                String processDefinitionKey) {
        ValidatedTaskActionContext context = validateTaskAction(userId, id, reqVO.getTaskId(), processDefinitionKey,
                "REJECT");
        signatureVerificationService.verifyPasswordAndCreateSignature(userId, id, reqVO.getTaskId(),
                context.stageCode().getCode(), "REJECT", reqVO.getPassword(), reqVO.getReason());
        bpmTaskService.rejectTask(userId, new BpmTaskRejectReqVO()
                .setId(reqVO.getTaskId())
                .setReason(reqVO.getReason()));
        controlledFileMapper.updateById(DccControlledFileDO.builder()
                .id(id)
                .status(DccControlledFileStatusEnum.REJECTED.getStatus())
                .rejectedTime(LocalDateTime.now())
                .rejectReason(reqVO.getReason())
                .build());
        return buildActionRespVO(requireActionSignature(id, reqVO.getTaskId(), userId, "REJECT"),
                "REJECTED", DccControlledFileStatusEnum.REJECTED.getStatus());
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

    private String shortHash(String hash) {
        if (StrUtil.isBlank(hash) || hash.length() < 12) {
            throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        }
        return hash.substring(0, 12).toLowerCase();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnTask(Long userId, Long id, DccControlledFileReturnTaskReqVO reqVO) {
        returnTaskWithProcessDefinitionKey(userId, id, reqVO, BPM_PROCESS_DEFINITION_KEY);
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
        transferTaskWithProcessDefinitionKey(userId, id, reqVO, BPM_PROCESS_DEFINITION_KEY);
    }

    void transferTaskWithProcessDefinitionKey(Long userId, Long id, DccControlledFileTransferTaskReqVO reqVO,
                                              String processDefinitionKey) {
        ValidatedTaskActionContext context = validateTaskAction(userId, id, reqVO.getTaskId(), processDefinitionKey,
                "TRANSFER");
        requireExistingUsers(List.of(reqVO.getAssigneeUserId()));
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
        createSignTaskWithProcessDefinitionKey(userId, id, reqVO, BPM_PROCESS_DEFINITION_KEY);
    }

    void createSignTaskWithProcessDefinitionKey(Long userId, Long id, DccControlledFileCreateSignTaskReqVO reqVO,
                                                String processDefinitionKey) {
        ValidatedTaskActionContext context = validateTaskAction(userId, id, reqVO.getTaskId(), processDefinitionKey,
                "ADD_SIGN");
        if (BpmTaskSignTypeEnum.of(reqVO.getType()) == null) {
            throw exception(CONTROLLED_FILE_TASK_TARGET_INVALID);
        }
        requireExistingUsers(reqVO.getUserIds());
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

    private DocControlApprovalArtifacts collectDocControlApprovalArtifactsIfRequired(Long userId,
                                                                                    ValidatedTaskActionContext context,
                                                                                    DccControlledFileApproveTaskReqVO reqVO,
                                                                                    boolean collectDocControlArtifacts) {
        if (!collectDocControlArtifacts || context.stageCode() != DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL) {
            return null;
        }
        if (reqVO.getStampedPdfFileId() != null) {
            throw exception(CONTROLLED_FILE_UPLOAD_TICKET_INVALID);
        }
        if (StrUtil.isBlank(reqVO.getSessionId()) || StrUtil.isBlank(reqVO.getStampedPdfUploadTicket())) {
            throw exception(CONTROLLED_FILE_STAMPED_PDF_REQUIRED);
        }
        DccUploadTicketBoundFile stampedPdf = uploadTicketService.resolveForBinding(
                new DccUploadTicketResolveCommand(reqVO.getStampedPdfUploadTicket(), userId, reqVO.getSessionId(),
                        DccControlledFileUploadTypePolicy.PURPOSE_DRAWING_PDF));
        if (stampedPdf == null || stampedPdf.storageFileId() == null) {
            throw exception(CONTROLLED_FILE_UPLOAD_TICKET_INVALID);
        }
        if (DccControlledFilePreviewKindEnum.resolve(stampedPdf.fileName(), stampedPdf.contentType())
                != DccControlledFilePreviewKindEnum.PDF) {
            throw exception(CONTROLLED_FILE_STAMPED_PDF_REQUIRED);
        }
        if (Boolean.TRUE.equals(context.file().getNeedTraining())
                && context.file().getTrainingRecordFileId() == null) {
            throw exception(CONTROLLED_FILE_TRAINING_RECORD_REQUIRED);
        }
        if (reqVO.getTrainingRecordFileId() != null) {
            throw exception(CONTROLLED_FILE_TRAINING_RECORD_REQUIRED);
        }
        Long confirmedDirectoryId = resolveDocControlConfirmedDirectory(context.file(), reqVO.getConfirmedDirectoryId());
        List<ResolvedDistributionPlan> distributionPlans =
                resolveDistributionPlans(reqVO.getSelectedDistributionScopes());
        return new DocControlApprovalArtifacts(stampedPdf.storageFileId(), reqVO.getSessionId(),
                reqVO.getStampedPdfUploadTicket(), confirmedDirectoryId, distributionPlans);
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
                userId, artifacts.sessionId(), DccControlledFileUploadTypePolicy.PURPOSE_DRAWING_PDF,
                context.file().getId()));
        persistSingleFileDistributionPlans(context.file().getId(), artifacts.distributionPlans());
    }

    private Long resolveDocControlConfirmedDirectory(DccControlledFileDO file, Long confirmedDirectoryId) {
        if (confirmedDirectoryId == null) {
            throw exception(CONTROLLED_FILE_SUBMIT_DIRECTORY_INVALID);
        }
        DccCategoryDirectoryBindingDO binding = categoryDirectoryBindingMapper.selectActiveByCategoryId(file.getCategoryId());
        if (binding == null) {
            throw exception(FILE_CATEGORY_DIRECTORY_BINDING_NOT_EXISTS);
        }
        return validateSelectedDirectory(binding.getDirectoryId(), confirmedDirectoryId, true);
    }

    private void persistDocControlConfirmedDirectory(DccControlledFileDO file, Long confirmedDirectoryId) {
        if (file.getMasterId() == null) {
            throw new IllegalStateException("controlled file master id is required for doc control directory confirmation");
        }
        validateConfirmedDirectoryMasterConflict(file, confirmedDirectoryId);
        controlledFileMasterMapper.updateById(DccControlledFileMasterDO.builder()
                .id(file.getMasterId())
                .directoryId(confirmedDirectoryId)
                .build());
    }

    private void validateConfirmedDirectoryMasterConflict(DccControlledFileDO file, Long confirmedDirectoryId) {
        if (Objects.equals(file.getDirectoryId(), confirmedDirectoryId)) {
            return;
        }
        if (StrUtil.isBlank(file.getFileName())) {
            throw new IllegalStateException("controlled file name is required for doc control directory confirmation");
        }
        DccControlledFileMasterDO existingMaster = controlledFileMasterMapper.selectByCategoryIdAndDirectoryIdAndFileName(
                file.getCategoryId(), confirmedDirectoryId, file.getFileName());
        if (existingMaster != null && !Objects.equals(existingMaster.getId(), file.getMasterId())) {
            throw exception(CONTROLLED_FILE_FILE_NUMBER_CONFLICT);
        }
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
        if (reqVO == null
                || reqVO.getCategoryId() == null
                || StrUtil.isBlank(reqVO.getFileName())
                || StrUtil.isBlank(reqVO.getFileNumber())
                || reqVO.getDirectoryId() == null
                || StrUtil.isBlank(reqVO.getVersionNo())
                || reqVO.getEffectiveDate() == null) {
            throw exception(CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING);
        }
        if (reqVO.getProductMasterId() == null && StrUtil.isNotBlank(reqVO.getProductCode())) {
            throw exception(CONTROLLED_FILE_PRODUCT_MASTER_INVALID);
        }
        boolean hasUploadTicket = hasAnyUploadTicket(reqVO);
        if (requireUploadTickets && hasAnyRawFileId(reqVO)) {
            throw exception(CONTROLLED_FILE_UPLOAD_TICKET_INVALID);
        }
        if (requireUploadTickets || hasUploadTicket) {
            if (StrUtil.isBlank(reqVO.getSessionId()) || StrUtil.isBlank(reqVO.getOriginalUploadTicket())) {
                throw exception(CONTROLLED_FILE_UPLOAD_TICKET_INVALID);
            }
        } else if (reqVO.getOriginalFileId() == null) {
            throw exception(CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING);
        }
        normalizeProcessType(reqVO.getProcessType());
        validateChangeType(reqVO.getChangeType());
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

    private void validateScreenshotProductCode(ResolvedDccProduct product) {
        if (product == null || product.id() == null) {
            return;
        }
        if (!isValidProductCode(product.dccProductCode())) {
            throw exception(CONTROLLED_FILE_PRODUCT_CODE_INVALID);
        }
    }

    private void validateScreenshotSourceFiles(ResolvedSubmitFiles submitFiles) {
        FileDO sourceFile = loadSourceFile(submitFiles);
        if (!DccControlledFileUploadTypePolicy.isAllowedEditableSourceName(sourceFile.getName())) {
            throw exception(CONTROLLED_FILE_SOURCE_FILE_TYPE_INVALID);
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
            DccUploadTicketBoundFile original = resolveUploadTicket(userId, reqVO.getSessionId(),
                    reqVO.getOriginalUploadTicket(), DccControlledFileUploadTypePolicy.PURPOSE_SOURCE);
            bindings.add(new SubmitTicketBinding(reqVO.getOriginalUploadTicket(),
                    DccControlledFileUploadTypePolicy.PURPOSE_SOURCE));
            DccUploadTicketBoundFile source = original;
            if (StrUtil.isNotBlank(reqVO.getSourceUploadTicket())) {
                source = resolveUploadTicket(userId, reqVO.getSessionId(), reqVO.getSourceUploadTicket(),
                        DccControlledFileUploadTypePolicy.PURPOSE_SOURCE);
                bindings.add(new SubmitTicketBinding(reqVO.getSourceUploadTicket(),
                        DccControlledFileUploadTypePolicy.PURPOSE_SOURCE));
            }
            DccUploadTicketBoundFile drawingPdf = null;
            if (StrUtil.isNotBlank(reqVO.getDrawingPdfUploadTicket())) {
                drawingPdf = resolveUploadTicket(userId, reqVO.getSessionId(), reqVO.getDrawingPdfUploadTicket(),
                        DccControlledFileUploadTypePolicy.PURPOSE_DRAWING_PDF);
                bindings.add(new SubmitTicketBinding(reqVO.getDrawingPdfUploadTicket(),
                        DccControlledFileUploadTypePolicy.PURPOSE_DRAWING_PDF));
            }
            return new ResolvedSubmitFiles(original.storageFileId(), source.storageFileId(),
                    drawingPdf == null ? null : drawingPdf.storageFileId(), bindings);
        }
        Long sourceFileId = reqVO.getSourceFileId() == null ? reqVO.getOriginalFileId() : reqVO.getSourceFileId();
        return new ResolvedSubmitFiles(reqVO.getOriginalFileId(), sourceFileId, reqVO.getDrawingPdfFileId(), List.of());
    }

    private DccUploadTicketBoundFile resolveUploadTicket(Long userId, String sessionId,
                                                        String uploadTicket, String purpose) {
        DccUploadTicketBoundFile file = uploadTicketService.resolveForBinding(
                new DccUploadTicketResolveCommand(uploadTicket, userId, sessionId, purpose));
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
                    context.reqVO().getSessionId(), binding.purpose(), controlledFileId));
        }
    }

    private PreparedSubmitContext prepareSubmitContext(Long userId, DccControlledFileSubmitReqVO reqVO,
                                                       boolean requireLeafDirectory,
                                                       boolean requireScreenshotMetadata, Long ignoredControlledFileId,
                                                       boolean requireUploadTickets) {
        validateSubmitRequest(reqVO, requireUploadTickets);
        String processType = normalizeProcessType(reqVO.getProcessType());
        boolean controlledUploadSubmit = requireUploadTickets
                && DccControlledFileProcessTypeEnum.CONTROLLED_FILE.getCode().equals(processType);
        DccProjectCodeDO projectCode = validateEnabledProjectCode(reqVO.getDccProjectCodeId(), controlledUploadSubmit);
        ResolvedFileTypeTaxonomy fileTypeTaxonomy = resolveFileTypeTaxonomy(reqVO.getFileTypeTaxonomyId(),
                controlledUploadSubmit);
        DccControlledFileVersion requestedVersion = parseVersion(reqVO.getVersionNo());
        DccFileCategoryDO category = validateCategory(reqVO.getCategoryId());
        validateCategoryUploadPermission(category.getId(), userId);
        boolean projectCodeProductSource = controlledUploadSubmit && isProductBoundCategory(category);
        validateProductBindingPolicy(category, reqVO.getProductMasterId(), projectCode, projectCodeProductSource);
        ResolvedDccProduct dccProduct = projectCodeProductSource
                ? resolveDccProductFromProjectCode(projectCode)
                : resolveDccProduct(reqVO.getProductMasterId());
        if (requireScreenshotMetadata) {
            validateScreenshotProductCode(dccProduct);
        }
        DccCategoryDirectoryBindingDO binding = categoryDirectoryBindingMapper.selectActiveByCategoryId(category.getId());
        if (binding == null) {
            throw exception(FILE_CATEGORY_DIRECTORY_BINDING_NOT_EXISTS);
        }
        Long selectedDirectoryId = validateSelectedDirectory(binding.getDirectoryId(), reqVO.getDirectoryId(), requireLeafDirectory);
        DccControlledFileChangeTypeEnum changeType = validateChangeType(reqVO.getChangeType());
        DccControlledFileMasterDO master = loadOrCreateMaster(reqVO, changeType);
        lockNativeContentMaster(master);
        DccControlledFileDO currentActiveFile = validateChangeTypeAgainstCurrentVersion(changeType, master);
        validateVersionChain(master.getId(), requestedVersion, ignoredControlledFileId, changeType);
        validateRevisionTarget(reqVO, changeType, currentActiveFile, projectCode, fileTypeTaxonomy,
                controlledUploadSubmit);
        ResolvedSubmitFiles submitFiles = applyOriginalVersionFile(
                resolveSubmitFiles(userId, reqVO, requireUploadTickets), changeType, currentActiveFile);
        if (requireScreenshotMetadata) {
            validateScreenshotSourceFiles(submitFiles);
        }
        return new PreparedSubmitContext(category, master, selectedDirectoryId, reqVO, submitFiles, dccProduct,
                projectCode, fileTypeTaxonomy, changeType);
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

    private void validateProductBindingPolicy(DccFileCategoryDO category, Long productMasterId,
                                              DccProjectCodeDO projectCode, boolean projectCodeProductSource) {
        if (!isProductBoundCategory(category)) {
            return;
        }
        if (projectCodeProductSource) {
            if (projectCode == null || StrUtil.isBlank(projectCode.getProjectCode())) {
                throw exception(CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING);
            }
            return;
        }
        if (productMasterId == null) {
            throw exception(CONTROLLED_FILE_PRODUCT_MASTER_INVALID);
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
        DccControlledFileSubmitReqVO reqVO = new DccControlledFileSubmitReqVO();
        reqVO.setCategoryId(file.getCategoryId());
        reqVO.setDirectoryId(file.getDirectoryId());
        reqVO.setOriginalFileId(file.getOriginalFileId());
        reqVO.setSourceFileId(file.getSourceFileId());
        reqVO.setDrawingPdfFileId(file.getDrawingPdfFileId());
        reqVO.setFileName(file.getFileName());
        reqVO.setFileNumber(file.getFileNumber());
        reqVO.setProductMasterId(file.getProductMasterId());
        reqVO.setProductCode(file.getProductCode());
        reqVO.setDccProjectCodeId(file.getDccProjectCodeId());
        reqVO.setFileTypeTaxonomyId(file.getFileTypeTaxonomyId());
        reqVO.setNeedTraining(file.getNeedTraining());
        reqVO.setProcessType(file.getProcessType());
        reqVO.setChangeType(file.getChangeType());
        reqVO.setVersionNo(file.getVersionNo());
        reqVO.setEffectiveDate(file.getEffectiveDate());
        reqVO.setRemark(file.getRemark());
        return reqVO;
    }

    private DccControlledFileDO insertControlledFile(PreparedSubmitContext context, Long userId,
                                                     String status, String processDefinitionKey) {
        String processType = normalizeProcessType(context.reqVO().getProcessType());
        DccControlledFileDO file = DccControlledFileDO.builder()
                .masterId(context.master().getId())
                .categoryId(context.category().getId())
                .directoryId(context.selectedDirectoryId())
                .sourceFileId(context.submitFiles().sourceFileId())
                .originalFileId(context.submitFiles().originalFileId())
                .drawingPdfFileId(context.submitFiles().drawingPdfFileId())
                .fileName(context.reqVO().getFileName())
                .title(context.reqVO().getFileName())
                .fileNumber(context.reqVO().getFileNumber())
                .productMasterId(context.dccProduct().id())
                .productCode(context.dccProduct().dccProductCode())
                .productName(context.dccProduct().nameCn())
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
                .versionNo(context.reqVO().getVersionNo())
                .effectiveDate(context.reqVO().getEffectiveDate())
                .remark(context.reqVO().getRemark())
                .status(status)
                .submitterId(userId)
                .requesterId(userId)
                .processDefinitionKey(processDefinitionKey)
                .submittedTime(LocalDateTime.now())
                .build();
        controlledFileMapper.insert(file);
        return file;
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
                                                        DccControlledFileChangeTypeEnum changeType) {
        String normalizedFileNumber = normalizeFileNumber(reqVO.getFileNumber());
        reqVO.setFileNumber(normalizedFileNumber);
        List<DccControlledFileMasterDO> masters = controlledFileMasterMapper.selectListByFileNumber(normalizedFileNumber);
        if (masters == null || masters.isEmpty()) {
            if (changeType != DccControlledFileChangeTypeEnum.NEW) {
                throw exception(CONTROLLED_FILE_NOT_EXISTS);
            }
            DccControlledFileMasterDO master = DccControlledFileMasterDO.builder()
                    .categoryId(reqVO.getCategoryId())
                    .directoryId(reqVO.getDirectoryId())
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

    private DccControlledFileChangeTypeEnum validateChangeType(String changeType) {
        DccControlledFileChangeTypeEnum resolved = DccControlledFileChangeTypeEnum.of(StrUtil.trim(changeType));
        if (resolved == null) {
            throw exception(CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING);
        }
        return resolved;
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
        Long revisionTargetId = reqVO.getRevisionTargetControlledFileId();
        if (changeType != DccControlledFileChangeTypeEnum.REVISION) {
            if (revisionTargetId != null) {
                throw exception(CONTROLLED_FILE_TASK_TARGET_INVALID);
            }
            return;
        }
        if (revisionTargetId == null || currentActiveFile == null
                || !Objects.equals(revisionTargetId, currentActiveFile.getId())) {
            throw exception(CONTROLLED_FILE_TASK_TARGET_INVALID);
        }
        DccControlledFileDO revisionTarget = controlledFileMapper.selectById(revisionTargetId);
        String targetFileNumber = revisionTarget == null ? null : StrUtil.trim(revisionTarget.getFileNumber());
        if (revisionTarget == null
                || !DccControlledFileStatusEnum.ACTIVE.getStatus().equals(revisionTarget.getStatus())
                || !Objects.equals(revisionTarget.getMasterId(), currentActiveFile.getMasterId())
                || StrUtil.isBlank(targetFileNumber)
                || !Objects.equals(targetFileNumber, reqVO.getFileNumber())) {
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
                submitFiles.drawingPdfFileId(), submitFiles.ticketBindings());
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

    private ResolvedDccProduct resolveDccProduct(Long productMasterId) {
        if (productMasterId == null) {
            return new ResolvedDccProduct(null, null, null);
        }
        MdmProductRespDTO product;
        try {
            product = productApi.getEnabledDccProduct(productMasterId);
        } catch (RuntimeException ex) {
            throw exception(CONTROLLED_FILE_PRODUCT_MASTER_INVALID);
        }
        if (product == null || product.getId() == null || !isValidProductCode(product.getDccProductCode())
                || StrUtil.isBlank(product.getNameCn())) {
            throw exception(CONTROLLED_FILE_PRODUCT_MASTER_INVALID);
        }
        return new ResolvedDccProduct(product.getId(), product.getDccProductCode(), product.getNameCn());
    }

    private ResolvedDccProduct resolveDccProductFromProjectCode(DccProjectCodeDO projectCode) {
        return new ResolvedDccProduct(null,
                StrUtil.trimToNull(projectCode.getProjectCode()),
                StrUtil.trimToNull(projectCode.getProjectName()));
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
        String normalized = StrUtil.trim(fileNumber);
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
                                          ResolvedDccProduct dccProduct,
                                          DccProjectCodeDO projectCode,
                                          ResolvedFileTypeTaxonomy fileTypeTaxonomy,
                                          DccControlledFileChangeTypeEnum changeType) {
    }

    private record ResolvedDccProduct(Long id, String dccProductCode, String nameCn) {
    }

    private record ResolvedFileTypeTaxonomy(DccFileTypeTaxonomyPath path,
                                            List<Long> activeDescendantIds,
                                            List<DccFileTypeTaxonomyPath> activeDescendantPaths) {
    }

    private record ResolvedSubmitFiles(Long originalFileId,
                                       Long sourceFileId,
                                       Long drawingPdfFileId,
                                       List<SubmitTicketBinding> ticketBindings) {
    }

    private record SubmitTicketBinding(String uploadTicket, String purpose) {
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

    private void validateCategoryUploadPermission(Long categoryId, Long userId) {
        if (!permissionSupport.hasCategoryPermission(categoryId, userId, DccFileCategoryPermissionActionEnum.UPLOAD)) {
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }
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

    private DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute applySelectedSignoffUsers(
            DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute resolvedRoute, List<Long> selectedSignoffUserIds) {
        if (selectedSignoffUserIds == null || selectedSignoffUserIds.isEmpty()) {
            return resolvedRoute;
        }
        LinkedHashSet<Long> normalizedUserIds = selectedSignoffUserIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (normalizedUserIds.size() != selectedSignoffUserIds.size()) {
            throw exception(ROUTE_PREVIEW_APPROVER_NOT_FOUND);
        }
        requireExistingUsers(normalizedUserIds);
        List<DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode> updatedNodes = new ArrayList<>();
        boolean replaced = false;
        for (DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode node : resolvedRoute.nodes()) {
            if (DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode().equals(node.stageCode())) {
                updatedNodes.add(new DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode(
                        node.stageNo(),
                        node.stageCode(),
                        node.stageName(),
                        node.stageOrder(),
                        node.candidateSourceType(),
                        node.candidateSourceId(),
                        node.candidateSourceIds(),
                        node.approveMethod(),
                        node.approveRatio(),
                        node.requireAllApprovals(),
                        new ArrayList<>(normalizedUserIds)));
                replaced = true;
                continue;
            }
            updatedNodes.add(node);
        }
        if (!replaced) {
            throw exception(CONTROLLED_FILE_ROUTE_NOT_CONFIGURED);
        }
        return new DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute(resolvedRoute.route(), updatedNodes);
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
        Task task = bpmTaskService.validateTask(userId, taskId);
        if (!StrUtil.equals(file.getProcessInstanceId(), task.getProcessInstanceId())) {
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
            return new ValidatedTaskActionContext(file, stageCode, task.getTaskDefinitionKey(), null);
        }
        DccControlledFileRouteSnapshotDO stageSnapshot = routeSnapshotMapper.selectListByControlledFileId(controlledFileId).stream()
                .filter(snapshot -> StrUtil.equals(snapshot.getStageCode(), stageCode.getCode()))
                .findFirst()
                .orElseThrow(() -> exception(CONTROLLED_FILE_TASK_STAGE_UNSUPPORTED));
        if (!parseResolvedUserIds(stageSnapshot).contains(userId)) {
            throw exception(CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED);
        }
        validateStagePermission(userId, stageCode);
        return new ValidatedTaskActionContext(file, stageCode, task.getTaskDefinitionKey(), stageSnapshot);
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
                controlledFileMapper.updateById(DccControlledFileDO.builder()
                        .id(file.getId())
                        .status(nextStatus)
                        .build());
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
            controlledFileMapper.updateById(DccControlledFileDO.builder()
                    .id(file.getId())
                    .status(nextStatus)
                    .build());
        }
        return nextStatus;
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
        if (currentStageCode == DccControlledFileStageCodeEnum.MATRIX_APPROVAL
                && nextStageCode == DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL
                && Boolean.TRUE.equals(file.getNeedTraining())
                && file.getTrainingRecordFileId() == null) {
            return DccControlledFileStatusEnum.PENDING_APPLICANT_TRAINING_RECORD.getStatus();
        }
        return toPendingStatus(nextStageCode);
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

    private DccControlledFileRoutePreviewRespVO toRoutePreviewRespVO(
            DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode node) {
        DccControlledFileRoutePreviewRespVO respVO = new DccControlledFileRoutePreviewRespVO();
        respVO.setStageNo(node.stageNo());
        respVO.setStageCode(node.stageCode());
        respVO.setStageName(node.stageName());
        respVO.setStageOrder(node.stageOrder());
        respVO.setCandidateSourceType(node.candidateSourceType());
        respVO.setCandidateSourceId(node.candidateSourceId());
        respVO.setCandidateSourceIds(node.candidateSourceIds());
        respVO.setApproveMethod(node.approveMethod());
        respVO.setApproveRatio(node.approveRatio());
        respVO.setRequireAllApprovals(node.requireAllApprovals());
        respVO.setResolvedUserIds(node.resolvedUserIds());
        return respVO;
    }

    private record ValidatedTaskActionContext(DccControlledFileDO file, DccControlledFileStageCodeEnum stageCode,
                                              String taskDefinitionKey,
                                              DccControlledFileRouteSnapshotDO stageSnapshot) {
    }

    private record ValidatedReturnTarget(String taskDefinitionKey, String pendingStatus) {
    }

    private record DocControlApprovalArtifacts(Long stampedPdfFileId, String sessionId,
                                               String stampedPdfUploadTicket,
                                               Long confirmedDirectoryId,
                                               List<ResolvedDistributionPlan> distributionPlans) {
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
