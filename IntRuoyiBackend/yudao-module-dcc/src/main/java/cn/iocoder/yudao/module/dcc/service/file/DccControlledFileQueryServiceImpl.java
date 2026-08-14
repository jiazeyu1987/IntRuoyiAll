package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileActionProjectionRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePreviewMetadataRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileAccessExplanationRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileUploadNameOptionRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileDistributionRecipientStatusRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileDistributionStatusRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePageReqVO;
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
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRouteSnapshotDO;
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
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileRouteSnapshotMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSignatureMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTrainingAssignmentMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTrainingProgressMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTrainingMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccExternalFileReviewMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileAccessEventMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileDownloadRecordMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileWatermarkTraceMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeAssignmentFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.enums.DccAccessResultEnum;
import cn.iocoder.yudao.module.dcc.enums.DccAccessTypeEnum;
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
import cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyPath;
import cn.iocoder.yudao.module.dcc.service.preview.DccControlledPreviewAccessService;
import cn.iocoder.yudao.module.dcc.service.preview.DccPreviewAccessRequest;
import cn.iocoder.yudao.module.dcc.service.preview.DccPreviewAccessResult;
import cn.iocoder.yudao.module.dcc.service.token.DccViewerTokenExpectedContext;
import cn.iocoder.yudao.module.dcc.service.token.DccViewerTokenService;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ACCESS_DENIED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_CATEGORY_DISABLED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_DOWNLOAD_WARNING_UNCONFIRMED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ONLYOFFICE_PREVIEW_CONFIG_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_VIEWER_TOKEN_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_DOWNLOAD_AUDIT_RECORD_FAILED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_DOWNLOAD_REQUEST_ID_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_DOWNLOAD_REQUEST_ID_REUSED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_CATEGORY_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_DIRECTORY_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_TYPE_TAXONOMY_LEVEL_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_CODE_DISABLED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_CODE_NOT_EXISTS;

@Service
@Validated
public class DccControlledFileQueryServiceImpl implements DccControlledFileQueryService {

    private static final String PREVIEW_ACCESS_TYPE = "PREVIEW";
    private static final String CONTROLLED_PREVIEW_PURPOSE = "CONTROLLED_PREVIEW";
    private static final String DOWNLOAD_ACCESS_TYPE = "DOWNLOAD";
    private static final String CONTROLLED_DOWNLOAD_PURPOSE = "CONTROLLED_DOWNLOAD";
    private static final String ACCESS_RESULT_SUCCESS = "SUCCESS";
    private static final String OFFICE_READ_ACTION_TYPE = "OFFICE_READ";
    private static final String DOWNLOAD_ENCRYPTION_STATUS_REQUESTED = "REQUESTED";
    private static final String DOWNLOAD_ENCRYPTION_STATUS_READY = "READY";
    private static final String DOWNLOAD_ENCRYPTION_STATUS_FAILED = "FAILED";
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
    private static final String ACTION_MANUAL_RELEASE = "MANUAL_RELEASE";
    private static final String ACTION_DELETE_WITHDRAWN_FLOW = "DELETE_WITHDRAWN_FLOW";
    private static final String ACTION_RESUBMIT_WITHDRAWN_FLOW = "RESUBMIT_WITHDRAWN_FLOW";
    private static final String ACTION_UPLOAD_TRAINING_RECORD = "UPLOAD_TRAINING_RECORD";
    private static final String ACTION_ACKNOWLEDGE_TRAINING = "ACKNOWLEDGE_TRAINING";
    private static final String ACTION_RETRY_FINALIZATION = "RETRY_FINALIZATION";
    private static final String FULL_FILE_SCOPE_PERMISSION = "dcc:controlled-file:scope:all";
    private static final String ASSIGNMENT_EXECUTE_PERMISSION = "dcc:project-code-assignment:execute";

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
    private DccProjectCodeAssignmentFileMapper projectCodeAssignmentFileMapper;
    @Resource
    private DccProjectCodeMapper projectCodeMapper;
    @Resource
    private DccFileTypeTaxonomyAdminService fileTypeTaxonomyAdminService;
    @Resource
    private PermissionApi permissionApi;
    @Override
    public PageResult<DccControlledFileRespVO> getControlledFilePage(Long userId, DccControlledFilePageReqVO reqVO) {
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
            visibleFiles = aggregateLatestVisibleFiles(visibleFiles);
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
    public PageResult<DccControlledFileRespVO> getControlledFileBrowserPage(Long userId, DccControlledFilePageReqVO reqVO) {
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
                .map(file -> toBrowserRespVO(userId, file, hasDirectoryManagementPermission,
                        blacklistedExtensionPatterns, currentViewMatrixAccessByCategory, directoryMap))
                .toList();
        return new PageResult<>(visibleRows, total);
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
        if (activeAssignedControlledFileIds != null && activeAssignedControlledFileIds.isEmpty()) {
            return List.of();
        }
        Set<Long> requestedDirectoryIds = resolveRequestedDirectoryIds(reqVO);
        DccControlledFilePageReqVO candidateReqVO = buildCandidateReqForWorkflowSearch(reqVO);
        List<DccControlledFileDO> candidates;
        if (requestedDirectoryIds != null) {
            if (requestedDirectoryIds.isEmpty()) {
                return List.of();
            }
            DccControlledFilePageReqVO directoryReqVO = buildPageReqWithoutDirectory(candidateReqVO);
            if (activeAssignedControlledFileIds == null) {
                candidates = browserSummaryQuery
                        ? controlledFileMapper.selectBrowserSummaryList(directoryReqVO, requestedDirectoryIds)
                        : controlledFileMapper.selectWorkflowList(directoryReqVO, requestedDirectoryIds);
            } else {
                candidates = browserSummaryQuery
                        ? controlledFileMapper.selectBrowserSummaryList(directoryReqVO, requestedDirectoryIds,
                        activeAssignedControlledFileIds)
                        : controlledFileMapper.selectWorkflowList(directoryReqVO, requestedDirectoryIds,
                        activeAssignedControlledFileIds);
            }
        } else {
            if (activeAssignedControlledFileIds == null) {
                candidates = browserSummaryQuery
                        ? controlledFileMapper.selectBrowserSummaryList(candidateReqVO)
                        : controlledFileMapper.selectWorkflowList(candidateReqVO);
            } else {
                candidates = browserSummaryQuery
                        ? controlledFileMapper.selectBrowserSummaryList(candidateReqVO, null, activeAssignedControlledFileIds)
                        : controlledFileMapper.selectWorkflowList(candidateReqVO, null, activeAssignedControlledFileIds);
            }
        }
        List<DccControlledFileDO> visibleFiles = candidates.stream()
                .filter(file -> isActiveAssignedControlledFile(file, activeAssignedControlledFileIds)
                        || canAccessQuery(userId, file, reqVO, hasDirectoryManagementPermission,
                        currentViewMatrixAccessByCategory))
                .filter(file -> !isBlacklistedBrowserExtension(file, blacklistedExtensionPatterns))
                .toList();
        visibleFiles = aggregateLatestVisibleFiles(visibleFiles);
        visibleFiles = filterByKeyword(visibleFiles, reqVO.getKeyword());
        return visibleFiles;
    }

    private Set<Long> resolveActiveAssignedControlledFileIds(Long userId) {
        if (userId == null) {
            return null;
        }
        if (permissionApi.hasAnyPermissions(userId, FULL_FILE_SCOPE_PERMISSION)) {
            return null;
        }
        if (!permissionApi.hasAnyPermissions(userId, ASSIGNMENT_EXECUTE_PERMISSION)) {
            return null;
        }
        Set<Long> scopedFileIds = new HashSet<>(projectCodeAssignmentFileMapper
                .selectActiveControlledFileIdsByAssigneeUserId(userId, LocalDateTime.now()));
        scopedFileIds.addAll(distributionRecipientMapper.selectActiveElectronicControlledFileIdsByUserId(
                TenantContextHolder.getRequiredTenantId(), userId));
        return scopedFileIds;
    }

    private boolean isActiveAssignedControlledFile(DccControlledFileDO file, Set<Long> activeAssignedControlledFileIds) {
        return file != null
                && file.getId() != null
                && activeAssignedControlledFileIds != null
                && activeAssignedControlledFileIds.contains(file.getId());
    }

    @Override
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
        List<DccFileDirectoryDO> directories = directoryMapper.selectEnabledList();
        Map<Long, DccFileDirectoryDO> directoryMap = directories.stream()
                .collect(Collectors.toMap(DccFileDirectoryDO::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        boolean defaultUnclassified = binding == null;
        DccFileDirectoryDO bindingDirectory = defaultUnclassified
                ? DccUploadDirectoryResolver.resolveUnclassifiedUploadDirectory(directories)
                : directoryMap.get(binding.getDirectoryId());
        if (bindingDirectory == null) {
            throw exception(FILE_DIRECTORY_NOT_EXISTS);
        }
        Map<Long, List<DccFileDirectoryDO>> childrenByParentId = groupChildrenByParentId(directories);
        return DccControlledFileUploadDirectoryTreeRespVO.builder()
                .bindingDirectoryId(bindingDirectory.getId())
                .bindingDirectoryPath(buildDirectoryPath(bindingDirectory.getId(), directoryMap))
                .leafBinding(!childrenByParentId.containsKey(bindingDirectory.getId()))
                .defaultUnclassified(defaultUnclassified)
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
        DccRequestAuditContext requiredAuditContext = requireAuditContext(auditContext);
        DccControlledFileDO file = controlledFileMapper.selectById(id);
        if (file == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        if (!canReadBinary(userId, file, DccAccessTypeEnum.PREVIEW)) {
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }
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
                requiredAuditContext.requireRequestId("preview metadata")));
        requirePreviewAccessResult(accessResult);
        respVO.setViewerToken(accessResult.viewerToken());
        respVO.setViewerTokenId(accessResult.viewerTokenId());
        respVO.setViewerTokenNonce(accessResult.viewerTokenNonce());
        respVO.setAccessEventCode(accessResult.accessEventCode());
        respVO.setWatermarkTraceCode(accessResult.watermarkTraceCode());
        if (previewKind == DccControlledFilePreviewKindEnum.OFFICE) {
            applyOnlyOfficePreview(respVO, userId, file, accessResult);
        }
        return respVO;
    }

    @Override
    public DccControlledFileBinary readPreviewFile(Long userId, Long id, String viewerToken,
                                                   String accessEventCode, String watermarkTraceCode,
                                                   String viewerTokenId, String viewerTokenNonce,
                                                   DccRequestAuditContext auditContext) {
        return readPreviewBinary(userId, id, viewerToken, accessEventCode, watermarkTraceCode,
                viewerTokenId, viewerTokenNonce, requireAuditContext(auditContext));
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
        DccRequestAuditContext officeAuditContext = auditContext.withRequestId(
                auditContext.requestIdOr(tokenPayload.getTokenId()));
        DccControlledFileDO file = controlledFileMapper.selectById(id);
        if (file == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        requireMatchingOnlyOfficeToken(tokenPayload, file);
        DccControlledFileAccessEventDO accessEvent = selectAccessEvent(tokenPayload.getAccessEventId());
        DccControlledFileWatermarkTraceDO watermarkTrace = selectWatermarkTraceByAccessEventId(accessEvent.getId());
        requireMatchingOnlyOfficeEvidence(tokenPayload, file, accessEvent, watermarkTrace);
        if (!canReadBinary(tokenPayload.getUserId(), file, DccAccessTypeEnum.PREVIEW)) {
            recordOnlyOfficeReadAccess(file, tokenPayload.getUserId(), accessEvent, watermarkTrace, false,
                    "ACCESS_DENIED", officeAuditContext);
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }
        FileDO binaryFile = resolveBinaryFileRecord(file, DccAccessTypeEnum.PREVIEW);
        try {
            byte[] content = fileService.getFileContent(binaryFile.getConfigId(), binaryFile.getPath());
            recordOnlyOfficeReadAccess(file, tokenPayload.getUserId(), accessEvent, watermarkTrace, true,
                    "OK", officeAuditContext);
            return new DccControlledFileBinary(binaryFile.getName(), binaryFile.getType(), content, null);
        } catch (Exception ex) {
            recordOnlyOfficeReadAccess(file, tokenPayload.getUserId(), accessEvent, watermarkTrace, false,
                    StrUtil.blankToDefault(ex.getMessage(), "READ_FAILED"), officeAuditContext);
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }
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
        return new DccControlledFileScope(infraFileId, references);
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
        DccControlledFileDO file = controlledFileMapper.selectById(id);
        if (file == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        if (!canReadBinary(userId, file, DccAccessTypeEnum.PREVIEW)) {
            recordAccess(file.getId(), userId, DccAccessTypeEnum.PREVIEW, false, "ACCESS_DENIED",
                    auditContext.withRequestId(auditContext.requestIdOr(accessEventCode)));
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }
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

        FileDO binaryFile;
        try {
            binaryFile = resolveBinaryFileRecord(file, DccAccessTypeEnum.PREVIEW);
        } catch (ServiceException ex) {
            recordPreviewAccess(file, userId, accessEvent, watermarkTrace, false,
                    "PUBLISHED_FILE_MISSING", auditContext.withRequestId(auditContext.requestIdOr(accessEventCode)));
            throw ex;
        }
        try {
            byte[] content = fileService.getFileContent(binaryFile.getConfigId(), binaryFile.getPath());
            recordPreviewAccess(file, userId, accessEvent, watermarkTrace, true,
                    "OK", auditContext.withRequestId(auditContext.requestIdOr(accessEventCode)));
            return new DccControlledFileBinary(binaryFile.getName(), binaryFile.getType(), content,
                    watermarkService.build(userId, "preview", binaryFile.getName()));
        } catch (Exception ex) {
            recordPreviewAccess(file, userId, accessEvent, watermarkTrace, false,
                    StrUtil.blankToDefault(ex.getMessage(), "READ_FAILED"),
                    auditContext.withRequestId(auditContext.requestIdOr(accessEventCode)));
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }
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
                    null,
                    null,
                    plainSha256,
                    null);
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
        DccControlledFileAccessEventDO accessEvent = DccControlledFileAccessEventDO.builder()
                .accessEventCode(newAccessEventCode())
                .controlledFileId(file.getId())
                .fileVersionNo(StrUtil.trim(file.getVersionNo()))
                .userId(userId)
                .accessType(DOWNLOAD_ACCESS_TYPE)
                .purpose(CONTROLLED_DOWNLOAD_PURPOSE)
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
                .encryptionStatus(DOWNLOAD_ENCRYPTION_STATUS_REQUESTED)
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
                .encryptionStatus(DOWNLOAD_ENCRYPTION_STATUS_READY)
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
                .set("encryption_status", DOWNLOAD_ENCRYPTION_STATUS_FAILED)
                .set("encryption_policy_version", null)
                .set("artifact_id", null)
                .set("cipher_file_ref", null)
                .set("plain_sha256", null)
                .set("cipher_sha256", null)
                .set("failure_code", failureCode)
                .set("failure_reason", StrUtil.trim(failureReason))
                .set("encrypted_at", LocalDateTime.now(ZoneOffset.UTC))
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

    private boolean canAccessQuery(Long userId, DccControlledFileDO file, DccControlledFilePageReqVO reqVO,
                                   boolean hasDirectoryManagementPermission) {
        return canAccessQuery(userId, file, reqVO, hasDirectoryManagementPermission, null);
    }

    private boolean canAccessQuery(Long userId, DccControlledFileDO file, DccControlledFilePageReqVO reqVO,
                                   boolean hasDirectoryManagementPermission,
                                   Map<Long, Boolean> currentViewMatrixAccessByCategory) {
        if (reqVO.getRequesterId() != null && reqVO.getRequesterId().equals(userId) && userId.equals(file.getRequesterId())) {
            return true;
        }
        if (hasDirectoryManagementPermission) {
            return !DccControlledFileStatusEnum.OBSOLETE.getStatus().equals(file.getStatus()) || canSeeObsolete(userId, file);
        }
        if (!canAccessBrowseScope(userId, file, currentViewMatrixAccessByCategory)) {
            return false;
        }
        return !DccControlledFileStatusEnum.OBSOLETE.getStatus().equals(file.getStatus()) || canSeeObsolete(userId, file);
    }

    private boolean canAccessDetail(Long userId, DccControlledFileDO file) {
        if (!isWithinAssignedFileScope(userId, file)) {
            return false;
        }
        if (userId != null && userId.equals(file.getRequesterId())) {
            return true;
        }
        if (directoryAccessPermissionService.hasDirectoryManagementPermission(userId)) {
            return !DccControlledFileStatusEnum.OBSOLETE.getStatus().equals(file.getStatus()) || canSeeObsolete(userId, file);
        }
        if (!canAccessBrowseScope(userId, file)) {
            return false;
        }
        return !DccControlledFileStatusEnum.OBSOLETE.getStatus().equals(file.getStatus()) || canSeeObsolete(userId, file);
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
        if (!isWithinAssignedFileScope(userId, file)) {
            return false;
        }
        boolean allowed;
        if (accessType == DccAccessTypeEnum.PREVIEW) {
            if ((DccControlledFileStatusEnum.ACTIVE.getStatus().equals(file.getStatus())
                    || DccControlledFileStatusEnum.SUPERSEDED.getStatus().equals(file.getStatus()))
                    && file.getPublishedFileId() != null) {
                allowed = userId != null && userId.equals(file.getRequesterId())
                        || hasDirectoryManagementPermission
                        || canAccessCurrentViewMatrix(userId, file, currentViewMatrixAccessByCategory)
                        || hasActiveElectronicDistributionAccess(userId, file);
                return allowed;
            }
            if (isPendingPreviewStatus(file.getStatus()) && file.getOriginalFileId() != null) {
                if (userId != null && userId.equals(file.getRequesterId())) {
                    return true;
                }
                allowed = hasDirectoryManagementPermission
                        || isCurrentRouteSnapshotParticipant(userId, file);
                return allowed;
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
            return isCurrentRouteSnapshotParticipant(userId, file);
        }
        return canAccessCurrentViewMatrix(userId, file, currentViewMatrixAccessByCategory)
                || hasActiveElectronicDistributionAccess(userId, file);
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
        if (!isWithinAssignedFileScope(userId, file)) {
            return downloadPolicyService.decide(new DccDownloadPolicyContext(
                    file.getId(), file.getStatus(), file.getPublishedFileId(), false, false));
        }
        boolean candidate = file.getPublishedFileId() != null
                && DccControlledFileStatusEnum.ACTIVE.getStatus().equals(file.getStatus());
        boolean recipientAllowed = candidate && hasActiveElectronicDistributionAccess(userId, file);
        boolean categoryDownloadAllowed = candidate
                && (recipientAllowed
                || hasDirectoryManagementPermission
                || permissionSupport.hasCategoryPermission(file.getCategoryId(), userId,
                DccFileCategoryPermissionActionEnum.DOWNLOAD));
        boolean directoryDownloadAllowed = categoryDownloadAllowed
                && (recipientAllowed
                || hasDirectoryAccess(userId, file, DccAccessTypeEnum.DOWNLOAD, hasDirectoryManagementPermission));
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
        Set<Long> assignedFileIds = resolveActiveAssignedControlledFileIds(userId);
        return assignedFileIds == null || file != null && assignedFileIds.contains(file.getId());
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
        if (accessType == DccAccessTypeEnum.PREVIEW && isPendingPreviewStatus(file.getStatus())) {
            return file.getOriginalFileId();
        }
        return file.getPublishedFileId();
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
        return DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus().equals(status)
                || DccControlledFileStatusEnum.PENDING_MATRIX_REVIEW.getStatus().equals(status)
                || DccControlledFileStatusEnum.PENDING_MATRIX_APPROVAL.getStatus().equals(status)
                || DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL.getStatus().equals(status);
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

    private String resolvePendingStageCode(String status) {
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
        String fileName = StrUtil.blankToDefault(file.getFileName(), file.getTitle());
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

    private String resolveVersionGroupKey(DccControlledFileDO file) {
        return file.getMasterId() != null ? "MASTER:" + file.getMasterId() : "FILE:" + file.getId();
    }

    private int compareVersionPriority(DccControlledFileDO left, DccControlledFileDO right) {
        Comparator<DccControlledFileDO> comparator = Comparator
                .comparing((DccControlledFileDO file) -> DccControlledFileVersion.parse(file.getVersionNo()),
                        Comparator.nullsLast(DccControlledFileVersion::compareTo))
                .thenComparing(DccControlledFileDO::getId, Comparator.nullsLast(Long::compareTo));
        return comparator.compare(left, right);
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
        DccControlledFileRespVO respVO = new DccControlledFileRespVO();
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
        respVO.setProductMasterId(file.getProductMasterId());
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
        respVO.setVersionNo(file.getVersionNo());
        respVO.setEffectiveDate(file.getEffectiveDate());
        respVO.setRemark(file.getRemark());
        respVO.setStatus(file.getStatus());
        respVO.setRequesterId(file.getRequesterId());
        respVO.setProcessInstanceId(file.getProcessInstanceId());
        respVO.setProcessDefinitionKey(file.getProcessDefinitionKey());
        respVO.setSubmittedTime(file.getSubmittedTime());
        respVO.setApprovedTime(file.getApprovedTime());
        respVO.setPublishedTime(file.getPublishedTime());
        respVO.setRejectedTime(file.getRejectedTime());
        respVO.setStampedTime(file.getStampedTime());
        respVO.setObsoletedBy(file.getObsoletedBy());
        respVO.setObsoletedTime(file.getObsoletedTime());
        respVO.setObsoleteReason(file.getObsoleteReason());
        respVO.setSupersededByFileId(file.getSupersededByFileId());
        respVO.setRejectReason(file.getRejectReason());
        respVO.setFinalizationError(file.getFinalizationError());
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
        respVO.setCanObsolete(DccControlledFileStatusEnum.ACTIVE.getStatus().equals(file.getStatus())
                && permissionSupport.hasCategoryPermission(file.getCategoryId(), userId,
                DccFileCategoryPermissionActionEnum.OBSOLETE));
        respVO.setCanPublish(DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus().equals(file.getStatus())
                && permissionSupport.hasCategoryPermission(file.getCategoryId(), userId,
                DccFileCategoryPermissionActionEnum.APPROVE));
        respVO.setVersionHistory(buildVersionHistory(userId, file, chainFiles, hasDirectoryManagementPermission));
        respVO.setDistributionStatuses(buildDistributionStatuses(file.getId()));
        List<DccControlledFileTrainingStatusRespVO> trainingStatuses = buildTrainingStatuses(file.getId());
        respVO.setTrainingStatuses(trainingStatuses);
        respVO.setCanManualRelease(
                DccControlledFileStatusEnum.PENDING_MANUAL_DISTRIBUTION.getStatus().equals(file.getStatus())
                        && permissionSupport.hasCategoryPermission(file.getCategoryId(), userId,
                        DccFileCategoryPermissionActionEnum.DISTRIBUTE)
                        && allTrainingStatusesAcknowledged(trainingStatuses));
        respVO.setSignatureSummaries(buildSignatureSummaries(file));
        respVO.setExternalReview(toExternalReviewRespVO(externalReviewMapper.selectByControlledFileId(file.getId())));
        respVO.setHasPendingTrainingAcknowledgement(respVO.getTrainingStatuses().stream()
                .flatMap(status -> status.getAssignments().stream())
                .anyMatch(assignment -> userId != null
                        && userId.equals(assignment.getUserId())
                        && !DccControlledFileTrainingStatusEnum.ACKNOWLEDGED.getCode().equals(assignment.getStatus())
                        && Boolean.TRUE.equals(assignment.getEligibleToAcknowledge())));
        respVO.setActionProjection(buildActionProjection(userId, file, respVO.getCanPreview(), respVO.getCanDownload(),
                respVO.getCanPrint(), respVO.getCanObsolete(), respVO.getCanManualRelease(),
                respVO.getHasPendingTrainingAcknowledgement()));
        respVO.setRouteSnapshots(includeRouteSnapshots
                ? convertList(routeSnapshotMapper.selectListByControlledFileId(file.getId()), this::toSnapshotRespVO)
                : List.of());
        return respVO;
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
        DccControlledFileRespVO respVO = new DccControlledFileRespVO();
        respVO.setId(file.getId());
        respVO.setMasterId(file.getMasterId());
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
        respVO.setVersionNo(file.getVersionNo());
        respVO.setEffectiveDate(file.getEffectiveDate());
        respVO.setRemark(file.getRemark());
        respVO.setStatus(file.getStatus());
        respVO.setRequesterId(file.getRequesterId());
        respVO.setPublishedTime(file.getPublishedTime());
        respVO.setObsoletedTime(file.getObsoletedTime());
        respVO.setSupersededByFileId(file.getSupersededByFileId());
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
        respVO.setCanObsolete(DccControlledFileStatusEnum.ACTIVE.getStatus().equals(file.getStatus())
                && permissionSupport.hasCategoryPermission(file.getCategoryId(), userId,
                DccFileCategoryPermissionActionEnum.OBSOLETE));
        respVO.setCanManualRelease(Boolean.FALSE);
        respVO.setVersionHistory(buildVersionHistory(userId, file, chainFiles, hasDirectoryManagementPermission,
                currentViewMatrixAccessByCategory));
        respVO.setActionProjection(buildActionProjection(userId, file, respVO.getCanPreview(), respVO.getCanDownload(),
                respVO.getCanPrint(), respVO.getCanObsolete(), respVO.getCanManualRelease(),
                respVO.getHasPendingTrainingAcknowledgement()));
        return respVO;
    }

    private DccControlledFileActionProjectionRespVO buildActionProjection(Long userId, DccControlledFileDO file,
                                                                          Boolean canPreview,
                                                                          Boolean canDownload,
                                                                          Boolean canPrint,
                                                                          Boolean canObsolete,
                                                                          Boolean canManualRelease,
                                                                          Boolean hasPendingTrainingAcknowledgement) {
        requireActionProjectionIdentity(file);
        List<String> allowedActions = new ArrayList<>();
        allowedActions.add(ACTION_VIEW);
        String status = file.getStatus();
        boolean requester = userId != null && userId.equals(file.getRequesterId());
        boolean active = DccControlledFileStatusEnum.ACTIVE.getStatus().equals(status);
        boolean superseded = DccControlledFileStatusEnum.SUPERSEDED.getStatus().equals(status);
        boolean pendingWithdrawable = isWithdrawableWorkflowStatus(status);
        boolean pendingOpenCandidate = isOpenCandidateActionStatus(status);
        FormActionInstanceDO obsoleteFormPending = active ? formActionPendingService.findOpenObsoleteAction(file.getId()) : null;
        boolean obsoleteApprovalPending = obsoleteFormPending != null;
        requirePendingRequestIdentity(file, pendingOpenCandidate);

        if (active || superseded) {
            addBinaryActions(allowedActions, canPreview, canDownload);
        }
        if (active && Boolean.TRUE.equals(canPrint)) {
            allowedActions.add(ACTION_PRINT);
        }
        if (active && Boolean.TRUE.equals(canObsolete) && !obsoleteApprovalPending) {
            allowedActions.add(ACTION_OBSOLETE);
        }
        if (pendingWithdrawable && requester) {
            allowedActions.add(ACTION_WITHDRAW);
        }
        if (DccControlledFileStatusEnum.PENDING_APPLICANT_TRAINING_RECORD.getStatus().equals(status)
                && requester && Boolean.TRUE.equals(file.getNeedTraining())) {
            allowedActions.add(ACTION_UPLOAD_TRAINING_RECORD);
        } else if (DccControlledFileStatusEnum.WITHDRAWN.getStatus().equals(status)
                && requester && file.getSupersededByFileId() == null) {
            allowedActions.add(ACTION_DELETE_WITHDRAWN_FLOW);
            allowedActions.add(ACTION_RESUBMIT_WITHDRAWN_FLOW);
        } else if (DccControlledFileStatusEnum.PENDING_MANUAL_DISTRIBUTION.getStatus().equals(status)
                && Boolean.TRUE.equals(canManualRelease)) {
            allowedActions.add(ACTION_MANUAL_RELEASE);
        } else if (DccControlledFileStatusEnum.TRAINING_IN_PROGRESS.getStatus().equals(status)
                && Boolean.TRUE.equals(hasPendingTrainingAcknowledgement)) {
            allowedActions.add(ACTION_ACKNOWLEDGE_TRAINING);
        } else if (DccControlledFileStatusEnum.FINALIZATION_FAILED.getStatus().equals(status)) {
            allowedActions.add(ACTION_RETRY_FINALIZATION);
        }

        DccControlledFileActionProjectionRespVO projection = new DccControlledFileActionProjectionRespVO();
        projection.setActionLocked(!active || obsoleteApprovalPending);
        projection.setActionLockReason(active
                ? (obsoleteApprovalPending ? "OBSOLETE_APPROVAL_PENDING" : null)
                : resolveActionLockReason(status));
        projection.setAllowedActions(allowedActions);
        projection.setCanWithdraw((pendingWithdrawable && requester)
                || (obsoleteApprovalPending && userId != null && userId.equals(obsoleteFormPending.getApplicantUserId())));
        projection.setPendingRequestId(obsoleteApprovalPending ? obsoleteFormPending.getId()
                : (pendingOpenCandidate ? file.getId() : null));
        projection.setPendingVersionNo((pendingOpenCandidate || obsoleteApprovalPending) ? file.getVersionNo() : null);
        return projection;
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

    private boolean canPrintControlledFile(Long userId, DccControlledFileDO file) {
        if (file == null || userId == null) {
            return false;
        }
        if (!DccControlledFileStatusEnum.ACTIVE.getStatus().equals(file.getStatus())) {
            return false;
        }
        if (!permissionSupport.hasCategoryPermission(file.getCategoryId(), userId,
                DccFileCategoryPermissionActionEnum.PRINT)) {
            return false;
        }
        DccControlledFileMasterDO master = file.getMasterId() == null ? null
                : controlledFileMasterMapper.selectById(file.getMasterId());
        return master != null && Objects.equals(master.getCurrentActiveControlledFileId(), file.getId());
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
            if ((DccControlledFileStatusEnum.ACTIVE.getStatus().equals(file.getStatus())
                    || DccControlledFileStatusEnum.SUPERSEDED.getStatus().equals(file.getStatus()))
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
            return new AccessReason(participant, participant ? "CURRENT_ROUTE_STAGE" : "DENIED",
                    participant ? "当前阶段 route snapshot 参与人" : "不在当前文件当前阶段 route snapshot 参与人内");
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
        if (userId != null && userId.equals(file.getRequesterId())) {
            return new AccessReason(true, "REQUESTER_SELF", "申请人自查");
        }
        if (hasDirectoryManagementPermission) {
            return new AccessReason(true, "DIRECTORY_ADMIN", "目录管理员");
        }
        if (hasActiveElectronicDistributionAccess(userId, file)) {
            return new AccessReason(true, "ELECTRONIC_DISTRIBUTION_RECIPIENT", "有效电子分发收件人");
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
        if (hasDirectoryManagementPermission) {
            return new AccessReason(true, "DIRECTORY_ADMIN", "目录管理员");
        }
        boolean participant = isCurrentRouteSnapshotParticipant(userId, file);
        return new AccessReason(participant, participant ? "CURRENT_ROUTE_STAGE" : "DENIED",
                participant ? "当前阶段 route snapshot 参与人" : "不在当前文件当前阶段 route snapshot 参与人内");
    }

    private String resolveCurrentActiveVersionNo(DccControlledFileDO file, List<DccControlledFileDO> chainFiles) {
        if (DccControlledFileStatusEnum.ACTIVE.getStatus().equals(file.getStatus())) {
            return file.getVersionNo();
        }
        return chainFiles.stream()
                .filter(history -> DccControlledFileStatusEnum.ACTIVE.getStatus().equals(history.getStatus()))
                .map(DccControlledFileDO::getVersionNo)
                .findFirst()
                .orElse(null);
    }

    private boolean isActiveFileBeingModified(DccControlledFileDO file, List<DccControlledFileDO> chainFiles) {
        return DccControlledFileStatusEnum.ACTIVE.getStatus().equals(file.getStatus())
                && file.getId() != null
                && chainFiles.stream()
                .anyMatch(history -> !file.getId().equals(history.getId())
                        && isPendingPreviewStatus(history.getStatus()));
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
        return chainFiles.stream()
                .filter(history -> !DccControlledFileStatusEnum.OBSOLETE.getStatus().equals(history.getStatus()) || canSeeObsolete(userId, history))
                .sorted(Comparator.comparing((DccControlledFileDO history) -> DccControlledFileVersion.parse(history.getVersionNo()),
                                Comparator.nullsLast(DccControlledFileVersion::compareTo))
                        .reversed()
                        .thenComparing(DccControlledFileDO::getId, Comparator.nullsLast(Long::compareTo)).reversed())
                .map(history -> {
                    DccControlledFileVersionHistoryRespVO respVO = new DccControlledFileVersionHistoryRespVO();
                    respVO.setId(history.getId());
                    respVO.setTitle(history.getTitle());
                    respVO.setFileNumber(history.getFileNumber());
                    respVO.setVersionNo(history.getVersionNo());
                    respVO.setStatus(history.getStatus());
                    respVO.setCurrentActiveVersionNo(currentActiveVersionNo);
                    respVO.setPublishedArtifactAvailable(hasFileRecord(history.getPublishedFileId()));
                    respVO.setStampedArtifactAvailable(hasFileRecord(history.getStampedFileId()));
                    respVO.setEffectiveDate(history.getEffectiveDate());
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
        return DccControlledFileStatusEnum.ACTIVE.getStatus().equals(file.getStatus())
                || DccControlledFileStatusEnum.SUPERSEDED.getStatus().equals(file.getStatus())
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
        return isPendingPreviewStatus(file.getStatus())
                ? "待审批源文件不存在或已被删除"
                : "正式预览文件不存在或已被删除";
    }

    private void applyOnlyOfficePreview(DccControlledFilePreviewMetadataRespVO respVO, Long userId,
                                        DccControlledFileDO file, DccPreviewAccessResult accessResult) {
        if (!onlyOfficePreviewProperties.isConfigured()) {
            respVO.setPreviewUnavailableReason(buildOnlyOfficeMissingReason());
            return;
        }
        respVO.setOnlyofficeBaseUrl(trimTrailingSlash(onlyOfficePreviewProperties.getBaseUrl()));
        DccOnlyOfficePreviewTokenService.IssuedPreviewToken issuedToken =
                onlyOfficePreviewTokenService.issueControlledFile(TenantContextHolder.getRequiredTenantId(),
                        userId, file.getId(), file.getVersionNo(), accessResult.accessEventId(),
                        DccOnlyOfficePreviewTokenService.PURPOSE_CONTROLLED_PREVIEW,
                        PREVIEW_VIEWER_TOKEN_TTL_SECONDS);
        respVO.setOnlyofficeDocumentUrl(trimTrailingSlash(onlyOfficePreviewProperties.getPublicFileBaseUrl())
                + "/admin-api/dcc/controlled-files/" + file.getId() + "/onlyoffice-file?token=" + issuedToken.token());
    }

    private void requireOnlyOfficeConfigured() {
        if (!onlyOfficePreviewProperties.isConfigured()) {
            throw exception(CONTROLLED_FILE_ONLYOFFICE_PREVIEW_CONFIG_MISSING,
                    onlyOfficePreviewProperties.missingReason());
        }
    }

    private void requirePreviewAccessResult(DccPreviewAccessResult accessResult) {
        if (accessResult == null
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
                || !DccOnlyOfficePreviewTokenService.PURPOSE_CONTROLLED_PREVIEW.equals(tokenPayload.getPurpose())) {
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
        }
    }

    private void requireMatchingPreviewEvidence(Long userId, DccControlledFileDO file,
                                                DccControlledFileAccessEventDO accessEvent,
                                                DccControlledFileWatermarkTraceDO watermarkTrace) {
        if (!Objects.equals(accessEvent.getControlledFileId(), file.getId())
                || !Objects.equals(accessEvent.getUserId(), userId)
                || !Objects.equals(accessEvent.getFileVersionNo(), file.getVersionNo())
                || !PREVIEW_ACCESS_TYPE.equals(accessEvent.getAccessType())
                || !CONTROLLED_PREVIEW_PURPOSE.equals(accessEvent.getPurpose())
                || !ACCESS_RESULT_SUCCESS.equals(accessEvent.getResult())
                || !Objects.equals(watermarkTrace.getAccessEventId(), accessEvent.getId())
                || !Objects.equals(watermarkTrace.getAccessEventCode(), accessEvent.getAccessEventCode())
                || !Objects.equals(watermarkTrace.getControlledFileId(), file.getId())
                || !Objects.equals(watermarkTrace.getUserId(), userId)
                || !Objects.equals(watermarkTrace.getFileVersionNo(), file.getVersionNo())) {
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_INVALID);
        }
    }

    private void requireMatchingOnlyOfficeEvidence(DccOnlyOfficePreviewTokenService.PreviewTokenPayload tokenPayload,
                                                   DccControlledFileDO file,
                                                   DccControlledFileAccessEventDO accessEvent,
                                                   DccControlledFileWatermarkTraceDO watermarkTrace) {
        if (!Objects.equals(accessEvent.getId(), tokenPayload.getAccessEventId())
                || !Objects.equals(accessEvent.getControlledFileId(), tokenPayload.getFileId())
                || !Objects.equals(accessEvent.getControlledFileId(), file.getId())
                || !Objects.equals(accessEvent.getUserId(), tokenPayload.getUserId())
                || !Objects.equals(accessEvent.getFileVersionNo(), tokenPayload.getVersionId())
                || !PREVIEW_ACCESS_TYPE.equals(accessEvent.getAccessType())
                || !DccOnlyOfficePreviewTokenService.PURPOSE_CONTROLLED_PREVIEW.equals(accessEvent.getPurpose())
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
        accessLogMapper.insert(DccControlledFileAccessLogDO.builder()
                .controlledFileId(file.getId())
                .accessEventId(accessEvent.getId())
                .accessEventCode(accessEvent.getAccessEventCode())
                .watermarkTraceCode(watermarkTrace.getTraceCode())
                .fileVersionNo(file.getVersionNo())
                .userId(userId)
                .actionType(OFFICE_READ_ACTION_TYPE)
                .purpose(CONTROLLED_PREVIEW_PURPOSE)
                .result(allowed ? DccAccessResultEnum.ALLOWED.name() : DccAccessResultEnum.DENIED.name())
                .reason(reason)
                .sourceIp(auditContext.sourceIp())
                .userAgent(auditContext.userAgent())
                .requestId(auditContext.requireRequestId("onlyoffice preview file"))
                .build());
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
                .purpose(CONTROLLED_PREVIEW_PURPOSE)
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
        respVO.setApproveMethod(snapshot.getApproveMethod());
        respVO.setApproveRatio(snapshot.getApproveRatio());
        respVO.setRequireAllApprovals(snapshot.getRequireAllApprovals());
        respVO.setResolvedUserIds(StrUtil.isBlank(snapshot.getResolvedUserIds())
                ? List.of()
                : Arrays.stream(snapshot.getResolvedUserIds().split(",")).map(Long::valueOf).toList());
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
