package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileLocalFolderImportReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileLocalFolderImportBatchReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileLocalFolderImportChunkReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileLocalFolderImportChunkRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileLocalFolderImportSessionCreateReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileLocalFolderImportUploadStateRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileNasTransferReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileNasTransferRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSubmitReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasOriginalPathSyncReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasUncontrolledImportLocalWriteResultReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasUncontrolledImportSelectedReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccCategoryDirectoryBindingDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDistributionRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryPermissionRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryTrainingRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccDirectoryAccessRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccFileDirectoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileNasSourceDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileLocalFolderUploadChunkDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileNasTransferTaskDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileNasTransferTaskItemDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccNasControlAuditFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccNasControlAuditTaskDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccNasOriginalPathSyncFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteNodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccCategoryDirectoryBindingMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryDistributionRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryPermissionRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryTrainingRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccDirectoryAccessRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccFileDirectoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileNasSourceMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileLocalFolderUploadChunkMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileNasTransferTaskItemMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileNasTransferTaskMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccNasControlAuditFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccNasControlAuditTaskMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccNasOriginalPathSyncFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.route.DccCategoryApprovalRouteMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.route.DccCategoryApprovalRouteNodeMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileChangeTypeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFilePreviewKindEnum;
import cn.iocoder.yudao.module.dcc.enums.DccProjectCodeStatusConstants;
import cn.iocoder.yudao.module.dcc.service.permission.DccNasPermissionSnapshotCaptureService;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasListRespVO;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.infra.service.file.NasAclReadResult;
import cn.iocoder.yudao.module.infra.service.file.NasBrowserService;
import cn.iocoder.yudao.module.infra.service.file.NasSettingsService;
import cn.iocoder.yudao.module.infra.service.file.NasFileReadResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_CODE_DISABLED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_CODE_NOT_EXISTS;

@Service
@Validated
@Slf4j
public class DccControlledFileNasTransferServiceImpl implements DccControlledFileNasTransferService {

    public static final String TASK_STATUS_WAITING = "WAITING";
    public static final String TASK_STATUS_UPLOADING = "UPLOADING";
    public static final String TASK_STATUS_RUNNING = "RUNNING";
    public static final String TASK_STATUS_COMPLETED = "COMPLETED";
    public static final String TASK_STATUS_FAILED = "FAILED";
    public static final String TASK_STATUS_CANCELLING = "CANCELLING";
    public static final String TASK_STATUS_CANCELLED = "CANCELLED";
    public static final String ITEM_STATUS_WAITING = "WAITING";
    public static final String ITEM_STATUS_RUNNING = "RUNNING";
    public static final String ITEM_STATUS_COMPLETED = "COMPLETED";
    public static final String ITEM_STATUS_FAILED = "FAILED";
    public static final String ITEM_STATUS_CANCELLED = "CANCELLED";
    public static final String ITEM_TYPE_DIRECTORY = "DIRECTORY";
    public static final String ITEM_TYPE_FILE = "FILE";
    public static final String SOURCE_TYPE_NAS = "NAS";
    public static final String SOURCE_TYPE_LOCAL_FOLDER = "LOCAL_FOLDER";
    public static final String SOURCE_TYPE_NAS_UNCONTROLLED_IMPORT = "NAS_UNCONTROLLED_IMPORT";
    public static final String SOURCE_TYPE_NAS_ORIGINAL_PATH_SYNC = "NAS_ORIGINAL_PATH_SYNC";
    public static final String AUDIT_FILE_ORIGINAL_PATH_SYNC_STATUS_WAITING = "ORIGINAL_PATH_WAITING";
    public static final String AUDIT_FILE_ORIGINAL_PATH_SYNC_STATUS_RUNNING = "ORIGINAL_PATH_RUNNING";
    public static final String AUDIT_FILE_ORIGINAL_PATH_SYNC_STATUS_ACTIVE = "ORIGINAL_PATH_ACTIVE";
    public static final String AUDIT_FILE_ORIGINAL_PATH_SYNC_STATUS_FAILED = "ORIGINAL_PATH_FAILED";
    public static final String AUDIT_FILE_ORIGINAL_PATH_SYNC_STATUS_DELETED = "ORIGINAL_PATH_DELETED";
    public static final String ORIGINAL_PATH_SYNC_FILE_STATUS_ACTIVE = "ACTIVE";
    public static final String ORIGINAL_PATH_SYNC_FILE_STATUS_DELETED = "DELETED";
    public static final String CHUNK_STATUS_COMPLETED = "COMPLETED";
    public static final String AUDIT_FILE_DOWNLOAD_STATUS_SELECTED = "SELECTED";
    public static final String AUDIT_FILE_DOWNLOAD_STATUS_LOCAL_WRITTEN = "LOCAL_WRITTEN";
    public static final String AUDIT_FILE_DOWNLOAD_STATUS_LOCAL_WRITE_FAILED = "LOCAL_WRITE_FAILED";
    public static final String AUDIT_FILE_ARCHIVE_STATUS_ARCHIVED = "ARCHIVED";
    public static final String AUDIT_FILE_ARCHIVE_STATUS_FAILED = "FAILED";
    public static final String AUDIT_FILE_ARCHIVE_ERROR_CODE_METADATA_REQUIRED = "ARCHIVE_METADATA_REQUIRED";
    static final String OUTCOME_CREATED = "CREATED";
    static final String OUTCOME_REUSED = "REUSED";
    private static final String UNCONTROLLED_IMPORT_SELECTION_SCOPE_EXPLICIT = "EXPLICIT_SELECTED_FILES";
    private static final String ORIGINAL_PATH_SYNC_SELECTION_SCOPE_FIRST = "FIRST_UNSYNCED";
    private static final String ORIGINAL_PATH_SYNC_SELECTION_SCOPE_EXPLICIT = "EXPLICIT_SELECTED_FILES";
    private static final String ORIGINAL_PATH_SYNC_SELECTION_SCOPE_ALL = "ALL_UNSYNCED";
    private static final String IMPORT_LOCAL_WRITE_STATUS_NOT_STARTED = "NOT_STARTED";
    private static final String IMPORT_LOCAL_WRITE_STATUS_LOCAL_WRITTEN = "LOCAL_WRITTEN";
    private static final String IMPORT_LOCAL_WRITE_STATUS_LOCAL_WRITE_FAILED = "LOCAL_WRITE_FAILED";
    private static final Set<String> UNCONTROLLED_IMPORT_ALLOWED_CLASSIFICATION_STATUSES = Set.of(
            DccNasControlAuditServiceImpl.AUDIT_FILE_CLASSIFICATION_STATUS_MATCHED,
            DccNasControlAuditServiceImpl.AUDIT_FILE_CLASSIFICATION_STATUS_UNCLASSIFIED_PENDING,
            DccNasControlAuditServiceImpl.AUDIT_FILE_CLASSIFICATION_STATUS_AMBIGUOUS
    );
    private static final int TASK_RETRY_DELAY_SECONDS = 30;
    private static final String ORIGINAL_DIRECTORY = "dcc/original";
    private static final String ORIGINAL_PATH_SYNC_DIRECTORY = "dcc/nas-original-path-sync";
    private static final String LOCAL_FOLDER_UPLOAD_CHUNK_DIRECTORY = "dcc-local-folder-import-chunks";
    private static final String DIRECTORY_CODE_PREFIX = "NASDIR-";
    private static final int FILE_NUMBER_MAX_LENGTH = 64;
    private static final int FILE_NUMBER_HASH_LENGTH = 12;
    private static final int DATABASE_ERROR_MESSAGE_MAX_LENGTH = 512;
    private static final String DATABASE_ERROR_MESSAGE_TRUNCATED_SUFFIX = "...[truncated]";
    private static final String CANCEL_REASON = "Stopped before deleting DCC directory subtree";
    @Resource
    private NasBrowserService nasBrowserService;
    @Resource
    private FileService fileService;
    @Resource
    private DccControlledFileWorkflowService workflowService;
    @Resource
    private DccFileDirectoryMapper directoryMapper;
    @Resource
    private DccDirectoryAccessRuleMapper directoryAccessRuleMapper;
    @Resource
    private DccFileCategoryMapper categoryMapper;
    @Resource
    private DccCategoryDirectoryBindingMapper categoryDirectoryBindingMapper;
    @Resource
    private DccFileCategoryPermissionRuleMapper permissionRuleMapper;
    @Resource
    private DccFileCategoryDistributionRuleMapper distributionRuleMapper;
    @Resource
    private DccFileCategoryTrainingRuleMapper trainingRuleMapper;
    @Resource
    private DccCategoryApprovalRouteMapper routeMapper;
    @Resource
    private DccCategoryApprovalRouteNodeMapper routeNodeMapper;
    @Resource
    private DccControlledFileNasTransferFailureReportService failureReportService;
    @Resource
    private DccControlledFileNasTransferTaskMapper taskMapper;
    @Resource
    private DccControlledFileNasTransferTaskItemMapper taskItemMapper;
    @Resource
    private DccControlledFileNasSourceMapper nasSourceMapper;
    @Resource
    private DccNasControlAuditFileMapper auditFileMapper;
    @Resource
    private DccNasControlAuditTaskMapper auditTaskMapper;
    @Resource
    private DccNasOriginalPathSyncFileMapper originalPathSyncFileMapper;
    @Resource
    private DccProjectCodeMapper projectCodeMapper;
    @Resource
    private DccControlledFileLocalFolderUploadChunkMapper uploadChunkMapper;
    @Resource
    private DccNasPermissionSnapshotCaptureService snapshotCaptureService;
    @Resource
    private NasSettingsService nasSettingsService;
    @Resource
    private PlatformTransactionManager transactionManager;
    @Value("${spring.servlet.multipart.location:${java.io.tmpdir}}")
    private String multipartLocation;

    private final ReentrantLock schedulerLock = new ReentrantLock();

    @Override
    public DccControlledFileNasTransferRespVO transfer(Long userId, DccControlledFileNasTransferReqVO reqVO) {
        requireSelectedCategoryContext(reqVO.getTemplateCategoryId());
        DccProjectCodeDO projectCode = resolveRequiredProjectCode(reqVO.getDccProjectCodeId());
        if (taskMapper.selectActiveTask() != null) {
            DccControlledFileNasTransferTaskDO activeTask = taskMapper.selectActiveTask();
            throw new IllegalStateException("nas transfer task already active: " + activeTask.getId());
        }
        List<String> collapsedRoots = collapseSelectedRoots(reqVO.getSelectedNasPaths());
        if (collapsedRoots.isEmpty()) {
            throw new IllegalStateException("selected nas paths empty after normalization");
        }
        Long taskId = createTask(userId, reqVO, collapsedRoots, projectCode);
        triggerTaskAsync(TenantContextHolder.getRequiredTenantId());
        return getTask(userId, taskId);
    }

    @Override
    public DccControlledFileNasTransferRespVO importLocalFolder(Long userId,
                                                               DccControlledFileLocalFolderImportReqVO reqVO) {
        List<ValidatedLocalFolderPath> validatedPaths = validateLocalFolderPaths(reqVO);
        requireSelectedCategoryContext(reqVO.getTemplateCategoryId());
        DccProjectCodeDO projectCode = resolveRequiredProjectCode(reqVO.getDccProjectCodeId());
        DccControlledFileNasTransferTaskDO activeTask = taskMapper.selectActiveTask();
        if (activeTask != null) {
            throw new IllegalStateException("nas transfer task already active: " + activeTask.getId());
        }
        List<LocalFolderFileEntry> fileEntries = buildLocalFolderFileEntries(reqVO.getFiles(), validatedPaths);
        Long taskId = createLocalFolderTask(userId, reqVO, fileEntries, projectCode);
        triggerTaskAsync(TenantContextHolder.getRequiredTenantId());
        return getTask(userId, taskId);
    }

    @Override
    public DccControlledFileNasTransferRespVO createLocalFolderImportSession(
            Long userId, DccControlledFileLocalFolderImportSessionCreateReqVO reqVO) {
        requireSelectedCategoryContext(reqVO.getTemplateCategoryId());
        DccProjectCodeDO projectCode = resolveRequiredProjectCode(reqVO.getDccProjectCodeId());
        String rootDirectoryName = requireLocalFolderRootDirectoryName(reqVO.getRootDirectoryName());
        long expectedFileCount = requirePositiveCount(reqVO.getExpectedFileCount(), "expectedFileCount");
        long expectedTotalBytes = requireNonNegativeCount(reqVO.getExpectedTotalBytes(), "expectedTotalBytes");
        DccControlledFileNasTransferTaskDO activeTask = taskMapper.selectActiveTask();
        if (activeTask != null) {
            if (Objects.equals(activeTask.getOperatorUserId(), userId)
                    && SOURCE_TYPE_LOCAL_FOLDER.equals(activeTask.getSourceType())
                    && TASK_STATUS_UPLOADING.equals(activeTask.getStatus())
                    && Objects.equals(activeTask.getDccProjectCodeId(), projectCode.getId())
                    && Objects.equals(JsonUtils.parseArray(activeTask.getSelectedNasPathsJson(), String.class)
                    .stream().findFirst().orElse(null), rootDirectoryName)) {
                return getTask(userId, activeTask.getId());
            }
            throw new IllegalStateException("nas transfer task already active: " + activeTask.getId());
        }
        Long taskId = tx().execute(status -> {
            DccControlledFileNasTransferTaskDO task = DccControlledFileNasTransferTaskDO.builder()
                    .operatorUserId(userId)
                    .templateCategoryId(reqVO.getTemplateCategoryId())
                    .dccProjectCodeId(projectCode.getId())
                    .productMasterId(null)
                    .effectiveDate(reqVO.getEffectiveDate())
                    .selectedNasPathsJson(JsonUtils.toJsonString(List.of(rootDirectoryName)))
                    .sourceType(SOURCE_TYPE_LOCAL_FOLDER)
                    .status(TASK_STATUS_UPLOADING)
                    .expectedFileCount(expectedFileCount)
                    .expectedTotalBytes(expectedTotalBytes)
                    .uploadedFileCount(0L)
                    .uploadedTotalBytes(0L)
                    .build();
            taskMapper.insert(task);
            return task.getId();
        });
        return getTask(userId, taskId);
    }

    @Override
    public DccControlledFileNasTransferRespVO uploadLocalFolderImportBatch(
            Long userId, Long taskId, DccControlledFileLocalFolderImportBatchReqVO reqVO) {
        DccControlledFileNasTransferTaskDO task = requireLocalFolderUploadingTask(userId, taskId);
        String rootDirectoryName = requireSingleLocalFolderRoot(task);
        List<ValidatedLocalFolderPath> validatedPaths = validateLocalFolderPaths(
                reqVO.getFiles(), reqVO.getRelativePaths(), rootDirectoryName);
        MultipartFile[] files = reqVO.getFiles();
        long batchFileCount = files.length;
        long batchTotalBytes = totalFileSize(files);
        tx().executeWithoutResult(status -> {
            DccControlledFileNasTransferTaskDO current = requireLocalFolderUploadingTask(userId, taskId);
            long currentUploadedFileCount =
                    requireNonNegativeCount(defaultLong(current.getUploadedFileCount()), "uploadedFileCount");
            long currentUploadedTotalBytes =
                    requireNonNegativeCount(defaultLong(current.getUploadedTotalBytes()), "uploadedTotalBytes");
            long currentExpectedFileCount =
                    requirePositiveCount(defaultLong(current.getExpectedFileCount()), "expectedFileCount");
            long currentExpectedTotalBytes =
                    requireNonNegativeCount(defaultLong(current.getExpectedTotalBytes()), "expectedTotalBytes");
            if (currentUploadedFileCount + batchFileCount > currentExpectedFileCount) {
                throw new IllegalStateException("local folder uploaded file count exceeds expected count");
            }
            if (currentUploadedTotalBytes + batchTotalBytes > currentExpectedTotalBytes) {
                throw new IllegalStateException("local folder uploaded total bytes exceeds expected total bytes");
            }
            List<DccControlledFileNasTransferTaskItemDO> existingItems = requireExistingTaskItems(taskId);
            assertNoDuplicateLocalFolderPaths(existingItems, validatedPaths);
            List<LocalFolderFileEntry> fileEntries = buildLocalFolderFileEntries(files, validatedPaths);
            insertLocalFolderTaskItems(current.getId(), fileEntries, existingItems);
            current.setUploadedFileCount(currentUploadedFileCount + batchFileCount);
            current.setUploadedTotalBytes(currentUploadedTotalBytes + batchTotalBytes);
            current.setLastFailureMessage(null);
            taskMapper.updateById(current);
        });
        return getTask(userId, taskId);
    }

    @Override
    public DccControlledFileLocalFolderImportUploadStateRespVO getLocalFolderImportUploadState(
            Long userId, Long taskId) {
        DccControlledFileNasTransferTaskDO task = requireLocalFolderUploadingTask(userId, taskId);
        return buildLocalFolderUploadState(task);
    }

    @Override
    public DccControlledFileLocalFolderImportChunkRespVO uploadLocalFolderImportChunk(
            Long userId, Long taskId, DccControlledFileLocalFolderImportChunkReqVO reqVO) {
        DccControlledFileNasTransferTaskDO task = requireLocalFolderUploadingTask(userId, taskId);
        String rootDirectoryName = requireSingleLocalFolderRoot(task);
        ValidatedLocalFolderChunk validatedChunk = validateLocalFolderChunkReq(reqVO, rootDirectoryName);
        if (isLocalFolderPathAlreadyUploaded(taskId, validatedChunk.relativePath())) {
            return buildLocalFolderChunkResponse(userId, taskId, validatedChunk, true);
        }

        StoredChunkFile storedChunk = storeLocalFolderUploadChunkFile(taskId, validatedChunk, reqVO.getChunk());
        boolean fileCompleted;
        try {
            fileCompleted = Boolean.TRUE.equals(tx().execute(status -> {
                DccControlledFileNasTransferTaskDO current = requireLocalFolderUploadingTask(userId, taskId);
                if (isLocalFolderPathAlreadyUploaded(taskId, validatedChunk.relativePath())) {
                    deleteIfExists(storedChunk.tempPath());
                    return true;
                }
                DccControlledFileLocalFolderUploadChunkDO existingChunk =
                        uploadChunkMapper.selectByTaskIdAndRelativePathAndChunkIndex(
                                taskId, validatedChunk.relativePath(), validatedChunk.chunkIndex());
                if (existingChunk != null) {
                    requireSameLocalFolderChunk(existingChunk, validatedChunk);
                    deleteIfExists(storedChunk.tempPath());
                    return isLocalFolderFileChunkSetComplete(taskId, validatedChunk);
                }

                Path finalPath = finalLocalFolderChunkPath(taskId, validatedChunk);
                moveStoredChunkToFinalPath(storedChunk.tempPath(), finalPath);
                uploadChunkMapper.insert(DccControlledFileLocalFolderUploadChunkDO.builder()
                        .taskId(taskId)
                        .relativePath(validatedChunk.relativePath())
                        .fileName(validatedChunk.fileName())
                        .fileSize(validatedChunk.fileSize())
                        .chunkIndex(validatedChunk.chunkIndex())
                        .totalChunks(validatedChunk.totalChunks())
                        .chunkSize(validatedChunk.chunkSize())
                        .chunkSha256(validatedChunk.chunkSha256())
                        .chunkTempPath(finalPath.toString())
                        .status(CHUNK_STATUS_COMPLETED)
                        .build());
                if (!isLocalFolderFileChunkSetComplete(taskId, validatedChunk)) {
                    return false;
                }
                completeLocalFolderFileFromChunks(current, validatedChunk, reqVO.getContentType());
                return true;
            }));
        } catch (RuntimeException exception) {
            deleteIfExists(storedChunk.tempPath());
            throw exception;
        }
        if (fileCompleted) {
            deleteCompletedLocalFolderChunkFiles(taskId, validatedChunk.relativePath());
        }
        return buildLocalFolderChunkResponse(userId, taskId, validatedChunk, fileCompleted);
    }

    @Override
    public DccControlledFileNasTransferRespVO completeLocalFolderImportSession(Long userId, Long taskId) {
        DccControlledFileNasTransferTaskDO task = requireLocalFolderUploadingTask(userId, taskId);
        long expectedFileCount = requirePositiveCount(defaultLong(task.getExpectedFileCount()), "expectedFileCount");
        long expectedTotalBytes = requireNonNegativeCount(defaultLong(task.getExpectedTotalBytes()), "expectedTotalBytes");
        long uploadedFileCount = requireNonNegativeCount(defaultLong(task.getUploadedFileCount()), "uploadedFileCount");
        long uploadedTotalBytes = requireNonNegativeCount(defaultLong(task.getUploadedTotalBytes()), "uploadedTotalBytes");
        if (uploadedFileCount != expectedFileCount || uploadedTotalBytes != expectedTotalBytes) {
            throw new IllegalStateException("local folder upload progress does not match expected totals");
        }
        if (taskItemMapper.selectPendingItemCountByTaskId(taskId) <= 0) {
            throw new IllegalStateException("local folder upload has no pending task items");
        }
        LocalDateTime now = LocalDateTime.now();
        task.setStatus(TASK_STATUS_WAITING);
        task.setUploadCompletedAt(now);
        task.setNextCheckAt(null);
        task.setLastFailureMessage(null);
        taskMapper.updateById(task);
        triggerTaskAsync(TenantContextHolder.getRequiredTenantId());
        return getTask(userId, taskId);
    }

    @Override
    public DccControlledFileNasTransferRespVO getTask(Long userId, Long taskId) {
        return buildTaskResponse(requireOwnedTask(userId, taskId));
    }

    @Override
    public void recoverInterruptedTasksOnStartup() {
        LocalDateTime nextCheckAt = nextScheduledCheckTime();
        int recoveredTaskCount = taskMapper.recoverRunningTasksToWaiting(nextCheckAt);
        int recoveredItemCount = 0;
        for (DccControlledFileNasTransferTaskDO task : taskMapper.selectUnfinishedTasks()) {
            recoveredItemCount += taskItemMapper.recoverRunningItemsToWaiting(task.getId());
        }
        if (recoveredTaskCount > 0) {
            log.info("[recoverInterruptedTasksOnStartup][recoveredTaskCount({})][nextCheckAt({})]",
                    recoveredTaskCount, nextCheckAt);
        }
        if (recoveredItemCount > 0) {
            log.info("[recoverInterruptedTasksOnStartup][recoveredItemCount({})]", recoveredItemCount);
        }
    }

    @Override
    public void processWaitingTasks() {
        if (!schedulerLock.tryLock()) {
            return;
        }
        try {
            for (DccControlledFileNasTransferTaskDO task : taskMapper.selectWaitingTasks(LocalDateTime.now())) {
                if (isNasUncontrolledImportTask(task)) {
                    continue;
                }
                try {
                    executeTask(task.getId());
                } catch (RuntimeException exception) {
                    log.error("[processWaitingTasks][taskId({}) DCC NAS transfer task execution failed]",
                            task.getId(), exception);
                }
            }
        } finally {
            schedulerLock.unlock();
        }
    }

    @Override
    public DccControlledFileNasTransferRespVO createUncontrolledImportTask(
            Long userId, Long auditTaskId, DccNasUncontrolledImportSelectedReqVO reqVO) {
        requireNonNull(userId, "userId");
        requireNonNull(auditTaskId, "auditTaskId");
        List<SelectedUncontrolledImportFile> selectedFiles = requireSelectedUncontrolledImportFiles(reqVO);
        String requestHash = uncontrolledImportRequestHash(auditTaskId, selectedFiles);
        DccControlledFileNasTransferTaskDO existingTask = selectUncontrolledImportIdempotentTask(
                userId, auditTaskId, reqVO.getIdempotencyKey(), false);
        if (existingTask != null) {
            requireSameUncontrolledImportRequestHash(existingTask, requestHash, reqVO.getIdempotencyKey());
            return getTask(userId, existingTask.getId());
        }

        Long taskId = tx().execute(status -> {
            DccControlledFileNasTransferTaskDO existingTaskInTransaction = selectUncontrolledImportIdempotentTask(
                    userId, auditTaskId, reqVO.getIdempotencyKey(), true);
            if (existingTaskInTransaction != null) {
                requireSameUncontrolledImportRequestHash(
                        existingTaskInTransaction, requestHash, reqVO.getIdempotencyKey());
                return existingTaskInTransaction.getId();
            }
            List<PreparedUncontrolledImportFile> preparedFiles =
                    prepareUncontrolledImportFiles(auditTaskId, selectedFiles);
            DccControlledFileNasTransferTaskDO task = DccControlledFileNasTransferTaskDO.builder()
                    .auditTaskId(auditTaskId)
                    .operatorUserId(userId)
                    .selectedNasPathsJson(JsonUtils.toJsonString(
                            preparedFiles.stream().map(file -> file.auditFile().getNormalizedRelativePath()).toList()))
                    .sourceType(SOURCE_TYPE_NAS_UNCONTROLLED_IMPORT)
                    .idempotencyKey(reqVO.getIdempotencyKey())
                    .requestHash(requestHash)
                    .status(TASK_STATUS_WAITING)
                    .expectedFileCount((long) preparedFiles.size())
                    .expectedTotalBytes(preparedFiles.stream()
                            .mapToLong(file -> defaultLong(file.auditFile().getFileSize()))
                            .sum())
                    .uploadedFileCount(0L)
                    .uploadedTotalBytes(0L)
                    .build();
            taskMapper.insert(task);
            for (PreparedUncontrolledImportFile preparedFile : preparedFiles) {
                DccNasControlAuditFileDO auditFile = preparedFile.auditFile();
                DccControlledFileNasTransferTaskItemDO item = DccControlledFileNasTransferTaskItemDO.builder()
                        .taskId(task.getId())
                        .auditFileId(auditFile.getId())
                        .itemType(ITEM_TYPE_FILE)
                        .nasPath(auditFile.getNormalizedRelativePath())
                        .itemName(auditFile.getFileName())
                        .sourceSignature(auditFile.getSourceSignature())
                        .classificationStatusSnapshot(auditFile.getClassificationStatus())
                        .matchedProjectCodeIdSnapshot(auditFile.getMatchedProjectCodeId())
                        .matchedFileTypeTaxonomyIdSnapshot(auditFile.getMatchedFileTypeTaxonomyId())
                        .matchedFileTypeLevel1Snapshot(auditFile.getMatchedFileTypeLevel1())
                        .matchedFileTypeLevel2Snapshot(auditFile.getMatchedFileTypeLevel2())
                        .matchedFileTypeLevel3Snapshot(auditFile.getMatchedFileTypeLevel3())
                        .matchedFileTypeLevel4Snapshot(auditFile.getMatchedFileTypeLevel4())
                        .matchedFileTypeLevel5Snapshot(auditFile.getMatchedFileTypeLevel5())
                        .classificationReasonSnapshot(auditFile.getClassificationReason())
                        .classificationCandidatesJsonSnapshot(auditFile.getClassificationCandidatesJson())
                        .localRelativePath(preparedFile.selectedFile().localRelativePath())
                        .localWriteStatus(IMPORT_LOCAL_WRITE_STATUS_NOT_STARTED)
                        .archiveStatus(resolveUncontrolledImportInitialArchiveStatus(
                                auditFile.getClassificationStatus()))
                        .status(ITEM_STATUS_WAITING)
                        .attemptCount(0)
                        .build();
                taskItemMapper.insert(item);
                auditFile.setDownloadStatus(AUDIT_FILE_DOWNLOAD_STATUS_SELECTED);
                auditFile.setSelectedImportTaskId(task.getId());
                auditFile.setSelectedImportTaskItemId(item.getId());
                auditFile.setLocalRelativePath(preparedFile.selectedFile().localRelativePath());
                if (isUncontrolledImportManualReviewClassification(auditFile.getClassificationStatus())) {
                    auditFile.setArchiveStatus(
                            DccNasControlAuditServiceImpl.AUDIT_FILE_ARCHIVE_STATUS_PENDING_MANUAL_REVIEW);
                    auditFile.setArchiveErrorCode(null);
                    auditFile.setArchiveError(null);
                }
                auditFileMapper.updateById(auditFile);
            }
            return task.getId();
        });
        return getTask(userId, taskId);
    }

    @Override
    public DccControlledFileNasTransferRespVO createOriginalPathSyncTask(
            Long userId, Long auditTaskId, DccNasOriginalPathSyncReqVO reqVO) {
        requireNonNull(userId, "userId");
        requireNonNull(auditTaskId, "auditTaskId");
        DccNasControlAuditTaskDO auditTask = requireCompletedOriginalPathAuditTask(auditTaskId);
        List<PreparedOriginalPathSyncFile> preparedFiles = prepareOriginalPathSyncFiles(auditTask, reqVO);
        String requestHash = originalPathSyncRequestHash(auditTaskId, preparedFiles);
        DccControlledFileNasTransferTaskDO existingTask = selectOriginalPathSyncIdempotentTask(
                userId, auditTaskId, reqVO.getIdempotencyKey(), false);
        if (existingTask != null) {
            requireSameOriginalPathSyncRequestHash(existingTask, requestHash, reqVO.getIdempotencyKey());
            return getTask(userId, existingTask.getId());
        }

        Long taskId = tx().execute(status -> {
            DccControlledFileNasTransferTaskDO existingTaskInTransaction = selectOriginalPathSyncIdempotentTask(
                    userId, auditTaskId, reqVO.getIdempotencyKey(), true);
            if (existingTaskInTransaction != null) {
                requireSameOriginalPathSyncRequestHash(
                        existingTaskInTransaction, requestHash, reqVO.getIdempotencyKey());
                return existingTaskInTransaction.getId();
            }
            DccControlledFileNasTransferTaskDO task = DccControlledFileNasTransferTaskDO.builder()
                    .auditTaskId(auditTaskId)
                    .operatorUserId(userId)
                    .selectedNasPathsJson(JsonUtils.toJsonString(preparedFiles.stream()
                            .map(file -> file.auditFile().getNormalizedRelativePath())
                            .toList()))
                    .sourceType(SOURCE_TYPE_NAS_ORIGINAL_PATH_SYNC)
                    .idempotencyKey(reqVO.getIdempotencyKey())
                    .requestHash(requestHash)
                    .status(TASK_STATUS_WAITING)
                    .expectedFileCount((long) preparedFiles.size())
                    .expectedTotalBytes(preparedFiles.stream()
                            .mapToLong(file -> defaultLong(file.auditFile().getFileSize()))
                            .sum())
                    .uploadedFileCount(0L)
                    .uploadedTotalBytes(0L)
                    .build();
            taskMapper.insert(task);
            for (PreparedOriginalPathSyncFile preparedFile : preparedFiles) {
                DccNasControlAuditFileDO auditFile = preparedFile.auditFile();
                DccControlledFileNasTransferTaskItemDO item = DccControlledFileNasTransferTaskItemDO.builder()
                        .taskId(task.getId())
                        .auditFileId(auditFile.getId())
                        .itemType(ITEM_TYPE_FILE)
                        .nasPath(auditFile.getNormalizedRelativePath())
                        .itemName(auditFile.getFileName())
                        .sourceSignature(auditFile.getSourceSignature())
                        .classificationStatusSnapshot(auditFile.getClassificationStatus())
                        .localRelativePath(auditFile.getNormalizedRelativePath())
                        .status(ITEM_STATUS_WAITING)
                        .attemptCount(0)
                        .build();
                taskItemMapper.insert(item);
                auditFile.setOriginalPathSyncStatus(AUDIT_FILE_ORIGINAL_PATH_SYNC_STATUS_WAITING);
                auditFile.setOriginalPathSyncFileId(null);
                auditFile.setOriginalPathSyncTaskId(task.getId());
                auditFile.setOriginalPathSyncTaskItemId(item.getId());
                auditFile.setOriginalPathSyncErrorCode(null);
                auditFile.setOriginalPathSyncError(null);
                auditFileMapper.updateById(auditFile);
            }
            return task.getId();
        });
        triggerTaskAsync(TenantContextHolder.getRequiredTenantId());
        return getTask(userId, taskId);
    }

    @Override
    public void deleteOriginalPathSyncFile(Long userId, Long syncFileId) {
        requireNonNull(userId, "userId");
        requireNonNull(syncFileId, "syncFileId");
        tx().executeWithoutResult(status -> {
            DccNasOriginalPathSyncFileDO syncFile = originalPathSyncFileMapper.selectById(syncFileId);
            if (syncFile == null) {
                throw new IllegalStateException("nas original-path sync file not found: " + syncFileId);
            }
            if (!ORIGINAL_PATH_SYNC_FILE_STATUS_ACTIVE.equals(syncFile.getSyncStatus())) {
                throw new IllegalStateException("nas original-path sync file is not active: " + syncFileId);
            }
            if (syncFile.getSourceFileId() == null) {
                throw new IllegalStateException("nas original-path sync source file id missing: " + syncFileId);
            }
            LocalDateTime now = LocalDateTime.now();
            if (originalPathSyncFileMapper.softDeleteActiveById(syncFileId, userId, now) == 0) {
                throw new IllegalStateException("nas original-path sync file delete conflict: " + syncFileId);
            }
            DccNasControlAuditFileDO auditFile = auditFileMapper.selectById(syncFile.getAuditFileId());
            if (auditFile != null && Objects.equals(syncFileId, auditFile.getOriginalPathSyncFileId())) {
                auditFileMapper.markOriginalPathSyncDeleted(auditFile.getId(), syncFileId);
            }
            try {
                fileService.deleteFile(syncFile.getSourceFileId());
            } catch (Exception ex) {
                throw new IllegalStateException("nas original-path sync source file delete failed: " + syncFileId, ex);
            }
        });
    }

    @Override
    public DccControlledFileBinary readUncontrolledImportContent(Long userId,
                                                                 Long importTaskId,
                                                                 Long auditFileId,
                                                                 String sourceSignature,
                                                                 String localRelativePath) {
        requireNonNull(userId, "userId");
        requireNonNull(importTaskId, "importTaskId");
        requireNonNull(auditFileId, "auditFileId");
        if (StrUtil.isBlank(sourceSignature)) {
            throw new IllegalStateException("nas uncontrolled import sourceSignature is required: " + auditFileId);
        }
        if (StrUtil.isBlank(localRelativePath)) {
            throw new IllegalStateException("nas uncontrolled import localRelativePath is required: " + auditFileId);
        }

        DccControlledFileNasTransferTaskDO task = taskMapper.selectById(importTaskId);
        if (task == null
                || !SOURCE_TYPE_NAS_UNCONTROLLED_IMPORT.equals(task.getSourceType())
                || !Objects.equals(task.getOperatorUserId(), userId)) {
            throw new IllegalStateException("nas uncontrolled import task invalid: " + importTaskId);
        }
        DccNasControlAuditFileDO auditFile = auditFileMapper.selectById(auditFileId);
        if (auditFile == null || !Objects.equals(task.getAuditTaskId(), auditFile.getTaskId())) {
            throw new IllegalStateException("nas uncontrolled import audit file task mismatch: " + auditFileId);
        }
        if (!Objects.equals(importTaskId, auditFile.getSelectedImportTaskId())
                || auditFile.getSelectedImportTaskItemId() == null) {
            throw new IllegalStateException("nas uncontrolled import audit file not bound to task: " + auditFileId);
        }
        DccControlledFileNasTransferTaskItemDO item = taskItemMapper.selectById(auditFile.getSelectedImportTaskItemId());
        if (item == null
                || !Objects.equals(item.getTaskId(), importTaskId)
                || !Objects.equals(item.getAuditFileId(), auditFileId)) {
            throw new IllegalStateException("nas uncontrolled import task item mismatch: " + auditFileId);
        }
        requireUncontrolledImportContentSnapshot(auditFileId, sourceSignature, localRelativePath, auditFile, item);

        NasFileReadResult sourceFile = nasBrowserService.readFile(auditFile.getNormalizedRelativePath());
        if (sourceFile == null || sourceFile.bytes() == null) {
            throw new IllegalStateException("nas uncontrolled import content missing: " + auditFileId);
        }
        String fileName = StrUtil.blankToDefault(sourceFile.name(), auditFile.getFileName());
        return new DccControlledFileBinary(fileName, "application/octet-stream", sourceFile.bytes(), null);
    }

    @Override
    public DccControlledFileNasTransferRespVO recordUncontrolledImportLocalWriteResult(
            Long userId, Long importTaskId, Long auditFileId,
            DccNasUncontrolledImportLocalWriteResultReqVO reqVO) {
        requireNonNull(userId, "userId");
        requireNonNull(importTaskId, "importTaskId");
        requireNonNull(auditFileId, "auditFileId");
        requireNonNull(reqVO, "reqVO");
        if (StrUtil.isBlank(reqVO.getSourceSignature())) {
            throw new IllegalStateException("nas uncontrolled import sourceSignature is required: " + auditFileId);
        }
        if (StrUtil.isBlank(reqVO.getLocalRelativePath())) {
            throw new IllegalStateException("nas uncontrolled import localRelativePath is required: " + auditFileId);
        }
        if (!IMPORT_LOCAL_WRITE_STATUS_LOCAL_WRITTEN.equals(reqVO.getLocalWriteStatus())
                && !IMPORT_LOCAL_WRITE_STATUS_LOCAL_WRITE_FAILED.equals(reqVO.getLocalWriteStatus())) {
            throw new IllegalStateException("nas uncontrolled import localWriteStatus invalid: " + auditFileId);
        }

        tx().executeWithoutResult(status -> {
            DccControlledFileNasTransferTaskDO task = requireUncontrolledImportTask(
                    userId, importTaskId);
            DccNasControlAuditFileDO auditFile = requireUncontrolledImportAuditFile(
                    task, importTaskId, auditFileId);
            DccControlledFileNasTransferTaskItemDO item = requireUncontrolledImportTaskItem(
                    importTaskId, auditFileId, auditFile);
            requireUncontrolledImportLocalWriteSnapshot(auditFileId, reqVO, auditFile, item);
            if (isUncontrolledImportLocalWriteSuccessReplay(reqVO, auditFile, item)) {
                return;
            }
            requireUncontrolledImportLocalWriteNotTerminal(auditFileId, auditFile, item);
            if (IMPORT_LOCAL_WRITE_STATUS_LOCAL_WRITTEN.equals(reqVO.getLocalWriteStatus())) {
                markUncontrolledImportLocalWritten(task, auditFile, item);
            } else {
                markUncontrolledImportLocalWriteFailed(auditFile, item, reqVO);
            }
        });
        return getTask(userId, importTaskId);
    }

    private DccControlledFileNasTransferTaskDO requireUncontrolledImportTask(Long userId, Long importTaskId) {
        DccControlledFileNasTransferTaskDO task = taskMapper.selectById(importTaskId);
        if (task == null
                || !SOURCE_TYPE_NAS_UNCONTROLLED_IMPORT.equals(task.getSourceType())
                || !Objects.equals(task.getOperatorUserId(), userId)) {
            throw new IllegalStateException("nas uncontrolled import task invalid: " + importTaskId);
        }
        return task;
    }

    private DccNasControlAuditFileDO requireUncontrolledImportAuditFile(
            DccControlledFileNasTransferTaskDO task, Long importTaskId, Long auditFileId) {
        DccNasControlAuditFileDO auditFile = auditFileMapper.selectById(auditFileId);
        if (auditFile == null || !Objects.equals(task.getAuditTaskId(), auditFile.getTaskId())) {
            throw new IllegalStateException("nas uncontrolled import audit file task mismatch: " + auditFileId);
        }
        if (!Objects.equals(importTaskId, auditFile.getSelectedImportTaskId())
                || auditFile.getSelectedImportTaskItemId() == null) {
            throw new IllegalStateException("nas uncontrolled import audit file not bound to task: " + auditFileId);
        }
        return auditFile;
    }

    private DccControlledFileNasTransferTaskItemDO requireUncontrolledImportTaskItem(
            Long importTaskId, Long auditFileId, DccNasControlAuditFileDO auditFile) {
        DccControlledFileNasTransferTaskItemDO item = taskItemMapper.selectById(auditFile.getSelectedImportTaskItemId());
        if (item == null
                || !Objects.equals(item.getTaskId(), importTaskId)
                || !Objects.equals(item.getAuditFileId(), auditFileId)) {
            throw new IllegalStateException("nas uncontrolled import task item mismatch: " + auditFileId);
        }
        return item;
    }

    private void requireUncontrolledImportLocalWriteSnapshot(
            Long auditFileId,
            DccNasUncontrolledImportLocalWriteResultReqVO reqVO,
            DccNasControlAuditFileDO auditFile,
            DccControlledFileNasTransferTaskItemDO item) {
        if (!Objects.equals(reqVO.getSourceSignature(), auditFile.getSourceSignature())
                || !Objects.equals(reqVO.getSourceSignature(), item.getSourceSignature())) {
            throw new IllegalStateException("nas uncontrolled import source signature mismatch: " + auditFileId);
        }
        if (!Objects.equals(reqVO.getLocalRelativePath(), auditFile.getLocalRelativePath())
                || !Objects.equals(reqVO.getLocalRelativePath(), item.getLocalRelativePath())) {
            throw new IllegalStateException("nas uncontrolled import localRelativePath mismatch: " + auditFileId);
        }
        boolean archiveFresh = isUncontrolledImportArchiveStateOpen(auditFile, item);
        if (!archiveFresh && !isUncontrolledImportLocalWriteSuccessReplay(reqVO, auditFile, item)) {
            throw new IllegalStateException("nas uncontrolled import local-write archive state invalid: " + auditFileId);
        }
    }

    private boolean isUncontrolledImportLocalWriteSuccessReplay(
            DccNasUncontrolledImportLocalWriteResultReqVO reqVO,
            DccNasControlAuditFileDO auditFile,
            DccControlledFileNasTransferTaskItemDO item) {
        return IMPORT_LOCAL_WRITE_STATUS_LOCAL_WRITTEN.equals(reqVO.getLocalWriteStatus())
                && AUDIT_FILE_DOWNLOAD_STATUS_LOCAL_WRITTEN.equals(auditFile.getDownloadStatus())
                && IMPORT_LOCAL_WRITE_STATUS_LOCAL_WRITTEN.equals(item.getLocalWriteStatus());
    }

    private void requireUncontrolledImportLocalWriteNotTerminal(
            Long auditFileId,
            DccNasControlAuditFileDO auditFile,
            DccControlledFileNasTransferTaskItemDO item) {
        if (!Objects.equals(AUDIT_FILE_DOWNLOAD_STATUS_SELECTED, auditFile.getDownloadStatus())
                || !Objects.equals(IMPORT_LOCAL_WRITE_STATUS_NOT_STARTED, item.getLocalWriteStatus())) {
            throw new IllegalStateException("nas uncontrolled import local-write terminal conflict: " + auditFileId);
        }
    }

    private void markUncontrolledImportLocalWritten(
            DccControlledFileNasTransferTaskDO task,
            DccNasControlAuditFileDO auditFile,
            DccControlledFileNasTransferTaskItemDO item) {
        auditFile.setDownloadStatus(AUDIT_FILE_DOWNLOAD_STATUS_LOCAL_WRITTEN);
        auditFile.setLocalWriteErrorCode(null);
        auditFile.setLocalWriteError(null);
        item.setLocalWriteStatus(IMPORT_LOCAL_WRITE_STATUS_LOCAL_WRITTEN);
        item.setLocalWriteErrorCode(null);
        item.setLocalWriteError(null);
        archiveUncontrolledImportIfMatched(task, auditFile, item);
        auditFileMapper.updateById(auditFile);
        taskItemMapper.updateById(item);
    }

    private void archiveUncontrolledImportIfMatched(
            DccControlledFileNasTransferTaskDO task,
            DccNasControlAuditFileDO auditFile,
            DccControlledFileNasTransferTaskItemDO item) {
        if (!DccNasControlAuditServiceImpl.AUDIT_FILE_CLASSIFICATION_STATUS_MATCHED.equals(
                item.getClassificationStatusSnapshot())) {
            markUncontrolledImportPendingManualReview(auditFile, item);
            return;
        }
        if (!hasCompleteUncontrolledImportArchiveSnapshot(item)) {
            markUncontrolledImportArchiveMetadataRequired(auditFile, item);
            return;
        }
        archiveUncontrolledImportFromSnapshot(task, auditFile, item);
    }

    private boolean hasCompleteUncontrolledImportArchiveSnapshot(
            DccControlledFileNasTransferTaskItemDO item) {
        return item.getArchiveCategoryIdSnapshot() != null
                && item.getArchiveDirectoryIdSnapshot() != null
                && item.getArchiveDccProjectCodeIdSnapshot() != null
                && item.getArchiveFileTypeTaxonomyIdSnapshot() != null
                && StrUtil.isNotBlank(item.getArchiveChangeTypeSnapshot())
                && StrUtil.isNotBlank(item.getArchiveFileNameSnapshot())
                && StrUtil.isNotBlank(item.getArchiveFileNumberSnapshot())
                && StrUtil.isNotBlank(item.getArchiveVersionNoSnapshot())
                && item.getArchiveEffectiveDateSnapshot() != null;
    }

    private void markUncontrolledImportArchiveMetadataRequired(
            DccNasControlAuditFileDO auditFile,
            DccControlledFileNasTransferTaskItemDO item) {
        String errorMessage = "NAS uncontrolled import requires formal archive metadata before DCC archive";
        LocalDateTime now = LocalDateTime.now();
        auditFile.setArchiveStatus(AUDIT_FILE_ARCHIVE_STATUS_FAILED);
        auditFile.setArchiveErrorCode(AUDIT_FILE_ARCHIVE_ERROR_CODE_METADATA_REQUIRED);
        auditFile.setArchiveError(errorMessage);
        item.setArchiveStatus(AUDIT_FILE_ARCHIVE_STATUS_FAILED);
        item.setArchiveErrorCode(AUDIT_FILE_ARCHIVE_ERROR_CODE_METADATA_REQUIRED);
        item.setArchiveError(errorMessage);
        item.setStatus(ITEM_STATUS_FAILED);
        item.setFailureStage("archive");
        item.setLastError(errorMessage);
        item.setAttemptCount(incrementCount(item.getAttemptCount()));
        item.setLastAttemptAt(now);
        item.setCompletedAt(now);
    }

    private void markUncontrolledImportPendingManualReview(
            DccNasControlAuditFileDO auditFile,
            DccControlledFileNasTransferTaskItemDO item) {
        LocalDateTime now = LocalDateTime.now();
        auditFile.setArchiveStatus(DccNasControlAuditServiceImpl.AUDIT_FILE_ARCHIVE_STATUS_PENDING_MANUAL_REVIEW);
        auditFile.setArchiveErrorCode(null);
        auditFile.setArchiveError(null);
        item.setArchiveStatus(DccNasControlAuditServiceImpl.AUDIT_FILE_ARCHIVE_STATUS_PENDING_MANUAL_REVIEW);
        item.setArchiveErrorCode(null);
        item.setArchiveError(null);
        item.setStatus(ITEM_STATUS_COMPLETED);
        item.setFailureStage(null);
        item.setLastError(null);
        item.setCompletedAt(now);
    }

    private void archiveUncontrolledImportFromSnapshot(
            DccControlledFileNasTransferTaskDO task,
            DccNasControlAuditFileDO auditFile,
            DccControlledFileNasTransferTaskItemDO item) {
        NasFileReadResult sourceFile = nasBrowserService.readFile(auditFile.getNormalizedRelativePath());
        if (sourceFile == null || sourceFile.bytes() == null || StrUtil.isBlank(sourceFile.name())) {
            throw new IllegalStateException("nas uncontrolled import archive source file missing: " + auditFile.getId());
        }
        Long originalFileId = fileService.createFileAndReturnId(
                sourceFile.bytes(), sourceFile.name(), ORIGINAL_DIRECTORY, sourceFile.contentType());
        DccControlledFileSubmitReqVO submitReqVO = new DccControlledFileSubmitReqVO();
        submitReqVO.setCategoryId(item.getArchiveCategoryIdSnapshot());
        submitReqVO.setDirectoryId(item.getArchiveDirectoryIdSnapshot());
        submitReqVO.setProductMasterId(null);
        submitReqVO.setDccProjectCodeId(item.getArchiveDccProjectCodeIdSnapshot());
        submitReqVO.setFileTypeTaxonomyId(item.getArchiveFileTypeTaxonomyIdSnapshot());
        submitReqVO.setOriginalFileId(originalFileId);
        submitReqVO.setChangeType(item.getArchiveChangeTypeSnapshot());
        submitReqVO.setFileName(item.getArchiveFileNameSnapshot());
        submitReqVO.setFileNumber(item.getArchiveFileNumberSnapshot());
        submitReqVO.setVersionNo(item.getArchiveVersionNoSnapshot());
        submitReqVO.setEffectiveDate(item.getArchiveEffectiveDateSnapshot());
        submitReqVO.setRemark(item.getArchiveRemarkSnapshot());
        Long controlledFileId = workflowService.submitControlledFileWithoutApproval(
                task.getOperatorUserId(), submitReqVO);
        String nasShareName = nasSettingsService.getRequiredNasConfig().share();
        String normalizedPath = DccNasPathUtils.normalizeRelativePath(auditFile.getNormalizedRelativePath());
        nasSourceMapper.insert(DccControlledFileNasSourceDO.builder()
                .controlledFileId(controlledFileId)
                .nasShareName(nasShareName)
                .normalizedRelativePath(normalizedPath)
                .pathHash(DccNasPathUtils.pathHash(nasShareName, normalizedPath))
                .sourceType(DccNasControlAuditServiceImpl.SOURCE_TYPE_NAS_TRANSFER)
                .sourceConfidence(DccNasControlAuditServiceImpl.SOURCE_CONFIDENCE_EXACT)
                .tenantId(TenantContextHolder.getRequiredTenantId())
                .build());

        LocalDateTime now = LocalDateTime.now();
        auditFile.setArchiveStatus(AUDIT_FILE_ARCHIVE_STATUS_ARCHIVED);
        auditFile.setArchiveErrorCode(null);
        auditFile.setArchiveError(null);
        auditFile.setControlledFileId(controlledFileId);
        item.setArchiveStatus(AUDIT_FILE_ARCHIVE_STATUS_ARCHIVED);
        item.setArchiveErrorCode(null);
        item.setArchiveError(null);
        item.setStatus(ITEM_STATUS_COMPLETED);
        item.setFailureStage(null);
        item.setLastError(null);
        item.setAttemptCount(incrementCount(item.getAttemptCount()));
        item.setLastAttemptAt(now);
        item.setCompletedAt(now);
    }

    private void markUncontrolledImportLocalWriteFailed(
            DccNasControlAuditFileDO auditFile,
            DccControlledFileNasTransferTaskItemDO item,
            DccNasUncontrolledImportLocalWriteResultReqVO reqVO) {
        auditFile.setDownloadStatus(AUDIT_FILE_DOWNLOAD_STATUS_LOCAL_WRITE_FAILED);
        auditFile.setLocalWriteErrorCode(StrUtil.trimToNull(reqVO.getLocalWriteErrorCode()));
        auditFile.setLocalWriteError(fitDatabaseErrorMessage(reqVO.getLocalWriteError()));
        item.setLocalWriteStatus(IMPORT_LOCAL_WRITE_STATUS_LOCAL_WRITE_FAILED);
        item.setLocalWriteErrorCode(StrUtil.trimToNull(reqVO.getLocalWriteErrorCode()));
        item.setLocalWriteError(fitDatabaseErrorMessage(reqVO.getLocalWriteError()));
        auditFileMapper.updateById(auditFile);
        taskItemMapper.updateById(item);
    }

    private void requireUncontrolledImportContentSnapshot(Long auditFileId,
                                                          String sourceSignature,
                                                          String localRelativePath,
                                                          DccNasControlAuditFileDO auditFile,
                                                          DccControlledFileNasTransferTaskItemDO item) {
        if (!Objects.equals(sourceSignature, auditFile.getSourceSignature())
                || !Objects.equals(sourceSignature, item.getSourceSignature())) {
            throw new IllegalStateException("nas uncontrolled import source signature mismatch: " + auditFileId);
        }
        if (!Objects.equals(localRelativePath, auditFile.getLocalRelativePath())
                || !Objects.equals(localRelativePath, item.getLocalRelativePath())) {
            throw new IllegalStateException("nas uncontrolled import localRelativePath mismatch: " + auditFileId);
        }
        if (!Objects.equals(AUDIT_FILE_DOWNLOAD_STATUS_SELECTED, auditFile.getDownloadStatus())
                || !Objects.equals(IMPORT_LOCAL_WRITE_STATUS_NOT_STARTED, item.getLocalWriteStatus())
                || !isUncontrolledImportArchiveStateOpen(auditFile, item)
                || auditFile.getControlledFileId() != null) {
            throw new IllegalStateException("nas uncontrolled import content state invalid: " + auditFileId);
        }
    }

    private DccControlledFileNasTransferTaskDO selectUncontrolledImportIdempotentTask(
            Long userId, Long auditTaskId, String idempotencyKey, boolean forUpdate) {
        return taskMapper.selectOne(new LambdaQueryWrapperX<DccControlledFileNasTransferTaskDO>()
                .eq(DccControlledFileNasTransferTaskDO::getAuditTaskId, auditTaskId)
                .eq(DccControlledFileNasTransferTaskDO::getOperatorUserId, userId)
                .eq(DccControlledFileNasTransferTaskDO::getSourceType, SOURCE_TYPE_NAS_UNCONTROLLED_IMPORT)
                .eq(DccControlledFileNasTransferTaskDO::getIdempotencyKey, idempotencyKey)
                .orderByDesc(DccControlledFileNasTransferTaskDO::getId)
                .last(forUpdate ? "LIMIT 1 FOR UPDATE" : "LIMIT 1"));
    }

    private void requireSameUncontrolledImportRequestHash(DccControlledFileNasTransferTaskDO existingTask,
                                                          String requestHash,
                                                          String idempotencyKey) {
        if (!Objects.equals(existingTask.getRequestHash(), requestHash)) {
            throw new IllegalStateException("nas uncontrolled import idempotency conflict: " + idempotencyKey);
        }
    }

    private List<SelectedUncontrolledImportFile> requireSelectedUncontrolledImportFiles(
            DccNasUncontrolledImportSelectedReqVO reqVO) {
        requireNonNull(reqVO, "reqVO");
        if (!UNCONTROLLED_IMPORT_SELECTION_SCOPE_EXPLICIT.equals(reqVO.getSelectionScope())) {
            throw new IllegalStateException("nas uncontrolled import selectionScope invalid: "
                    + reqVO.getSelectionScope());
        }
        if (StrUtil.isBlank(reqVO.getIdempotencyKey())) {
            throw new IllegalStateException("nas uncontrolled import idempotencyKey is required");
        }
        List<DccNasUncontrolledImportSelectedReqVO.SelectedFile> selectedFiles = reqVO.getSelectedFiles();
        if (selectedFiles == null || selectedFiles.isEmpty()) {
            throw new IllegalStateException("nas uncontrolled import selectedFiles is required");
        }
        Map<Long, SelectedUncontrolledImportFile> selectedById = new LinkedHashMap<>();
        for (DccNasUncontrolledImportSelectedReqVO.SelectedFile selectedFile : selectedFiles) {
            requireNonNull(selectedFile, "selectedFile");
            Long auditFileId = selectedFile.getAuditFileId();
            requireNonNull(auditFileId, "auditFileId");
            if (selectedById.containsKey(auditFileId)) {
                throw new IllegalStateException("nas uncontrolled import duplicate auditFileId: " + auditFileId);
            }
            if (StrUtil.isBlank(selectedFile.getSourceSignature())) {
                throw new IllegalStateException("nas uncontrolled import sourceSignature is required: " + auditFileId);
            }
            if (StrUtil.isBlank(selectedFile.getLocalRelativePath())) {
                throw new IllegalStateException("nas uncontrolled import localRelativePath is required: " + auditFileId);
            }
            selectedById.put(auditFileId, new SelectedUncontrolledImportFile(
                    auditFileId, selectedFile.getSourceSignature(), selectedFile.getLocalRelativePath()));
        }
        return selectedById.values().stream()
                .sorted(Comparator.comparing(SelectedUncontrolledImportFile::auditFileId))
                .toList();
    }

    private List<PreparedUncontrolledImportFile> prepareUncontrolledImportFiles(
            Long auditTaskId, List<SelectedUncontrolledImportFile> selectedFiles) {
        List<Long> auditFileIds = selectedFiles.stream()
                .map(SelectedUncontrolledImportFile::auditFileId)
                .toList();
        List<DccNasControlAuditFileDO> auditFiles = auditFileMapper.selectBatchIds(auditFileIds);
        Map<Long, DccNasControlAuditFileDO> auditFileById = new LinkedHashMap<>();
        if (auditFiles != null) {
            for (DccNasControlAuditFileDO auditFile : auditFiles) {
                auditFileById.put(auditFile.getId(), auditFile);
            }
        }
        List<PreparedUncontrolledImportFile> preparedFiles = new ArrayList<>();
        for (SelectedUncontrolledImportFile selectedFile : selectedFiles) {
            DccNasControlAuditFileDO auditFile = auditFileById.get(selectedFile.auditFileId());
            if (auditFile == null) {
                throw new IllegalStateException("nas uncontrolled import audit file not found: "
                        + selectedFile.auditFileId());
            }
            requireImportableAuditFile(auditTaskId, selectedFile, auditFile);
            preparedFiles.add(new PreparedUncontrolledImportFile(selectedFile, auditFile));
        }
        return preparedFiles;
    }

    private void requireImportableAuditFile(Long auditTaskId,
                                            SelectedUncontrolledImportFile selectedFile,
                                            DccNasControlAuditFileDO auditFile) {
        if (!Objects.equals(auditTaskId, auditFile.getTaskId())) {
            throw new IllegalStateException("nas uncontrolled import audit file task mismatch: " + auditFile.getId());
        }
        if (!Objects.equals(selectedFile.sourceSignature(), auditFile.getSourceSignature())) {
            throw new IllegalStateException("nas uncontrolled import source signature mismatch: " + auditFile.getId());
        }
        if (!UNCONTROLLED_IMPORT_ALLOWED_CLASSIFICATION_STATUSES.contains(auditFile.getClassificationStatus())) {
            throw new IllegalStateException("nas uncontrolled import classification status invalid: "
                    + auditFile.getId());
        }
        if (!Objects.equals(DccNasControlAuditServiceImpl.AUDIT_FILE_DOWNLOAD_STATUS_NOT_SELECTED,
                auditFile.getDownloadStatus())) {
            throw new IllegalStateException("nas uncontrolled import download status invalid: " + auditFile.getId());
        }
        if (!isUncontrolledImportArchiveStatusSelectable(
                auditFile.getClassificationStatus(), auditFile.getArchiveStatus())) {
            throw new IllegalStateException("nas uncontrolled import archive status invalid: " + auditFile.getId());
        }
        if (auditFile.getSelectedImportTaskId() != null || auditFile.getSelectedImportTaskItemId() != null) {
            throw new IllegalStateException("nas uncontrolled import audit file already selected: " + auditFile.getId());
        }
        if (auditFile.getControlledFileId() != null) {
            throw new IllegalStateException("nas uncontrolled import audit file already archived: " + auditFile.getId());
        }
        if (StrUtil.isBlank(auditFile.getExpectedLocalRelativePath())) {
            throw new IllegalStateException("nas uncontrolled import expectedLocalRelativePath missing: "
                    + auditFile.getId());
        }
        if (!Objects.equals(selectedFile.localRelativePath(), auditFile.getExpectedLocalRelativePath())) {
            throw new IllegalStateException("nas uncontrolled import localRelativePath mismatch: " + auditFile.getId());
        }
    }

    private String resolveUncontrolledImportInitialArchiveStatus(String classificationStatus) {
        if (isUncontrolledImportManualReviewClassification(classificationStatus)) {
            return DccNasControlAuditServiceImpl.AUDIT_FILE_ARCHIVE_STATUS_PENDING_MANUAL_REVIEW;
        }
        return DccNasControlAuditServiceImpl.AUDIT_FILE_ARCHIVE_STATUS_NOT_STARTED;
    }

    private boolean isUncontrolledImportManualReviewClassification(String classificationStatus) {
        return DccNasControlAuditServiceImpl.AUDIT_FILE_CLASSIFICATION_STATUS_UNCLASSIFIED_PENDING.equals(
                classificationStatus)
                || DccNasControlAuditServiceImpl.AUDIT_FILE_CLASSIFICATION_STATUS_AMBIGUOUS.equals(
                classificationStatus);
    }

    private boolean isUncontrolledImportArchiveStatusSelectable(String classificationStatus, String archiveStatus) {
        if (DccNasControlAuditServiceImpl.AUDIT_FILE_CLASSIFICATION_STATUS_MATCHED.equals(classificationStatus)) {
            return DccNasControlAuditServiceImpl.AUDIT_FILE_ARCHIVE_STATUS_NOT_STARTED.equals(archiveStatus);
        }
        if (isUncontrolledImportManualReviewClassification(classificationStatus)) {
            return DccNasControlAuditServiceImpl.AUDIT_FILE_ARCHIVE_STATUS_NOT_STARTED.equals(archiveStatus)
                    || DccNasControlAuditServiceImpl.AUDIT_FILE_ARCHIVE_STATUS_PENDING_MANUAL_REVIEW.equals(
                    archiveStatus);
        }
        return false;
    }

    private boolean isUncontrolledImportArchiveStateOpen(DccNasControlAuditFileDO auditFile,
                                                         DccControlledFileNasTransferTaskItemDO item) {
        return auditFile.getControlledFileId() == null
                && isUncontrolledImportArchiveStatusSelectable(
                item.getClassificationStatusSnapshot(), auditFile.getArchiveStatus())
                && isUncontrolledImportArchiveStatusSelectable(
                item.getClassificationStatusSnapshot(), item.getArchiveStatus());
    }

    private String uncontrolledImportRequestHash(Long auditTaskId, List<SelectedUncontrolledImportFile> selectedFiles) {
        StringBuilder raw = new StringBuilder("DCC_NAS_UNCONTROLLED_IMPORT");
        appendLengthPrefixed(raw, String.valueOf(auditTaskId));
        for (SelectedUncontrolledImportFile selectedFile : selectedFiles) {
            appendLengthPrefixed(raw, String.valueOf(selectedFile.auditFileId()));
            appendLengthPrefixed(raw, selectedFile.sourceSignature());
            appendLengthPrefixed(raw, selectedFile.localRelativePath());
        }
        return sha256Hex(raw.toString());
    }

    private DccNasControlAuditTaskDO requireCompletedOriginalPathAuditTask(Long auditTaskId) {
        DccNasControlAuditTaskDO auditTask = auditTaskMapper.selectById(auditTaskId);
        if (auditTask == null) {
            throw new IllegalStateException("nas original-path sync audit task not found: " + auditTaskId);
        }
        if (!DccNasControlAuditServiceImpl.STATUS_COMPLETED.equals(auditTask.getStatus())) {
            throw new IllegalStateException("nas original-path sync audit task not completed: " + auditTaskId);
        }
        if (StrUtil.isBlank(auditTask.getNasShareName())) {
            throw new IllegalStateException("nas original-path sync audit task share missing: " + auditTaskId);
        }
        return auditTask;
    }

    private List<PreparedOriginalPathSyncFile> prepareOriginalPathSyncFiles(
            DccNasControlAuditTaskDO auditTask, DccNasOriginalPathSyncReqVO reqVO) {
        requireNonNull(reqVO, "reqVO");
        String selectionScope = StrUtil.trimToEmpty(reqVO.getSelectionScope());
        if (StrUtil.isBlank(reqVO.getIdempotencyKey())) {
            throw new IllegalStateException("nas original-path sync idempotencyKey is required");
        }
        if (ORIGINAL_PATH_SYNC_SELECTION_SCOPE_EXPLICIT.equals(selectionScope)) {
            return prepareExplicitOriginalPathSyncFiles(auditTask, reqVO);
        }
        if (!ORIGINAL_PATH_SYNC_SELECTION_SCOPE_FIRST.equals(selectionScope)
                && !ORIGINAL_PATH_SYNC_SELECTION_SCOPE_ALL.equals(selectionScope)) {
            throw new IllegalStateException("nas original-path sync selectionScope invalid: "
                    + reqVO.getSelectionScope());
        }
        List<DccNasControlAuditFileDO> auditFiles = safeList(auditFileMapper.selectListByTaskId(auditTask.getId()));
        Map<String, DccNasOriginalPathSyncFileDO> activeSyncByHash =
                selectActiveOriginalPathSyncByHash(auditTask.getNasShareName(), pathHashesOf(auditFiles));
        List<PreparedOriginalPathSyncFile> preparedFiles = auditFiles.stream()
                .filter(file -> isOriginalPathSyncCandidate(auditTask.getId(), file))
                .filter(file -> !activeSyncByHash.containsKey(file.getPathHash()))
                .map(PreparedOriginalPathSyncFile::new)
                .toList();
        if (ORIGINAL_PATH_SYNC_SELECTION_SCOPE_FIRST.equals(selectionScope) && !preparedFiles.isEmpty()) {
            preparedFiles = List.of(preparedFiles.get(0));
        }
        if (preparedFiles.isEmpty()) {
            throw new IllegalStateException("nas original-path sync has no selectable files: " + auditTask.getId());
        }
        return preparedFiles;
    }

    private List<PreparedOriginalPathSyncFile> prepareExplicitOriginalPathSyncFiles(
            DccNasControlAuditTaskDO auditTask, DccNasOriginalPathSyncReqVO reqVO) {
        List<SelectedOriginalPathSyncFile> selectedFiles = requireSelectedOriginalPathSyncFiles(reqVO);
        List<DccNasControlAuditFileDO> auditFiles = safeList(auditFileMapper.selectBatchIds(selectedFiles.stream()
                .map(SelectedOriginalPathSyncFile::auditFileId)
                .toList()));
        Map<Long, DccNasControlAuditFileDO> auditFileById = auditFiles.stream()
                .collect(Collectors.toMap(DccNasControlAuditFileDO::getId, file -> file,
                        (left, right) -> left, LinkedHashMap::new));
        Map<String, DccNasOriginalPathSyncFileDO> activeSyncByHash =
                selectActiveOriginalPathSyncByHash(auditTask.getNasShareName(), pathHashesOf(auditFiles));
        List<PreparedOriginalPathSyncFile> preparedFiles = new ArrayList<>();
        for (SelectedOriginalPathSyncFile selectedFile : selectedFiles) {
            DccNasControlAuditFileDO auditFile = auditFileById.get(selectedFile.auditFileId());
            if (auditFile == null) {
                throw new IllegalStateException("nas original-path sync audit file not found: "
                        + selectedFile.auditFileId());
            }
            requireOriginalPathSyncCandidate(auditTask.getId(), auditFile);
            if (!Objects.equals(selectedFile.sourceSignature(), auditFile.getSourceSignature())) {
                throw new IllegalStateException("nas original-path sync source signature mismatch: "
                        + auditFile.getId());
            }
            if (activeSyncByHash.containsKey(auditFile.getPathHash())) {
                throw new IllegalStateException("nas original-path sync file already active: " + auditFile.getId());
            }
            preparedFiles.add(new PreparedOriginalPathSyncFile(auditFile));
        }
        return preparedFiles;
    }

    private List<SelectedOriginalPathSyncFile> requireSelectedOriginalPathSyncFiles(
            DccNasOriginalPathSyncReqVO reqVO) {
        List<DccNasOriginalPathSyncReqVO.SelectedFile> selectedFiles = reqVO.getSelectedFiles();
        if (selectedFiles == null || selectedFiles.isEmpty()) {
            throw new IllegalStateException("nas original-path sync selectedFiles is required");
        }
        Map<Long, SelectedOriginalPathSyncFile> selectedById = new LinkedHashMap<>();
        for (DccNasOriginalPathSyncReqVO.SelectedFile selectedFile : selectedFiles) {
            requireNonNull(selectedFile, "selectedFile");
            Long auditFileId = selectedFile.getAuditFileId();
            requireNonNull(auditFileId, "auditFileId");
            if (selectedById.containsKey(auditFileId)) {
                throw new IllegalStateException("nas original-path sync duplicate auditFileId: " + auditFileId);
            }
            if (StrUtil.isBlank(selectedFile.getSourceSignature())) {
                throw new IllegalStateException("nas original-path sync sourceSignature is required: " + auditFileId);
            }
            selectedById.put(auditFileId, new SelectedOriginalPathSyncFile(
                    auditFileId, selectedFile.getSourceSignature()));
        }
        return selectedById.values().stream()
                .sorted(Comparator.comparing(SelectedOriginalPathSyncFile::auditFileId))
                .toList();
    }

    private boolean isOriginalPathSyncCandidate(Long auditTaskId, DccNasControlAuditFileDO auditFile) {
        try {
            requireOriginalPathSyncCandidate(auditTaskId, auditFile);
            return true;
        } catch (IllegalStateException exception) {
            return false;
        }
    }

    private void requireOriginalPathSyncCandidate(Long auditTaskId, DccNasControlAuditFileDO auditFile) {
        requireOriginalPathSyncBaseSnapshot(auditTaskId, auditFile);
        if (AUDIT_FILE_ORIGINAL_PATH_SYNC_STATUS_WAITING.equals(auditFile.getOriginalPathSyncStatus())
                || AUDIT_FILE_ORIGINAL_PATH_SYNC_STATUS_RUNNING.equals(auditFile.getOriginalPathSyncStatus())
                || AUDIT_FILE_ORIGINAL_PATH_SYNC_STATUS_ACTIVE.equals(auditFile.getOriginalPathSyncStatus())
                || auditFile.getOriginalPathSyncFileId() != null) {
            throw new IllegalStateException("nas original-path sync audit file already selected: "
                    + auditFile.getId());
        }
    }

    private void requireOriginalPathSyncBaseSnapshot(Long auditTaskId, DccNasControlAuditFileDO auditFile) {
        if (auditFile == null) {
            throw new IllegalStateException("nas original-path sync audit file is required");
        }
        if (!Objects.equals(auditTaskId, auditFile.getTaskId())) {
            throw new IllegalStateException("nas original-path sync audit file task mismatch: " + auditFile.getId());
        }
        if (!DccNasControlAuditServiceImpl.AUDIT_FILE_CONTROL_STATUS_NOT_CONTROLLED.equals(
                auditFile.getControlStatus())) {
            throw new IllegalStateException("nas original-path sync control status invalid: " + auditFile.getId());
        }
        if (auditFile.getControlledFileId() != null) {
            throw new IllegalStateException("nas original-path sync audit file already archived: "
                    + auditFile.getId());
        }
        if (StrUtil.isBlank(auditFile.getNasShareName())
                || StrUtil.isBlank(auditFile.getRootPath())
                || StrUtil.isBlank(auditFile.getNormalizedRelativePath())
                || StrUtil.isBlank(auditFile.getPathHash())
                || StrUtil.isBlank(auditFile.getFileName())
                || StrUtil.isBlank(auditFile.getSourceSignature())
                || auditFile.getFileSize() == null
                || auditFile.getModifiedAt() == null) {
            throw new IllegalStateException("nas original-path sync audit file snapshot incomplete: "
                    + auditFile.getId());
        }
    }

    private Map<String, DccNasOriginalPathSyncFileDO> selectActiveOriginalPathSyncByHash(
            String nasShareName, Collection<String> pathHashes) {
        Map<String, DccNasOriginalPathSyncFileDO> activeSyncByHash = new LinkedHashMap<>();
        List<DccNasOriginalPathSyncFileDO> activeRows =
                originalPathSyncFileMapper.selectActiveByPathHashes(nasShareName, pathHashes);
        for (DccNasOriginalPathSyncFileDO activeRow : safeList(activeRows)) {
            DccNasOriginalPathSyncFileDO previous = activeSyncByHash.putIfAbsent(
                    activeRow.getPathHash(), activeRow);
            if (previous != null) {
                throw new IllegalStateException("nas original-path sync duplicate active path hash: "
                        + activeRow.getPathHash());
            }
        }
        return activeSyncByHash;
    }

    private List<String> pathHashesOf(List<DccNasControlAuditFileDO> auditFiles) {
        return safeList(auditFiles).stream()
                .map(DccNasControlAuditFileDO::getPathHash)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toList();
    }

    private DccControlledFileNasTransferTaskDO selectOriginalPathSyncIdempotentTask(
            Long userId, Long auditTaskId, String idempotencyKey, boolean forUpdate) {
        return taskMapper.selectOne(new LambdaQueryWrapperX<DccControlledFileNasTransferTaskDO>()
                .eq(DccControlledFileNasTransferTaskDO::getAuditTaskId, auditTaskId)
                .eq(DccControlledFileNasTransferTaskDO::getOperatorUserId, userId)
                .eq(DccControlledFileNasTransferTaskDO::getSourceType, SOURCE_TYPE_NAS_ORIGINAL_PATH_SYNC)
                .eq(DccControlledFileNasTransferTaskDO::getIdempotencyKey, idempotencyKey)
                .orderByDesc(DccControlledFileNasTransferTaskDO::getId)
                .last(forUpdate ? "LIMIT 1 FOR UPDATE" : "LIMIT 1"));
    }

    private void requireSameOriginalPathSyncRequestHash(DccControlledFileNasTransferTaskDO existingTask,
                                                        String requestHash,
                                                        String idempotencyKey) {
        if (!Objects.equals(existingTask.getRequestHash(), requestHash)) {
            throw new IllegalStateException("nas original-path sync idempotency conflict: " + idempotencyKey);
        }
    }

    private String originalPathSyncRequestHash(Long auditTaskId, List<PreparedOriginalPathSyncFile> preparedFiles) {
        StringBuilder raw = new StringBuilder("DCC_NAS_ORIGINAL_PATH_SYNC");
        appendLengthPrefixed(raw, String.valueOf(auditTaskId));
        for (PreparedOriginalPathSyncFile preparedFile : preparedFiles) {
            DccNasControlAuditFileDO auditFile = preparedFile.auditFile();
            appendLengthPrefixed(raw, String.valueOf(auditFile.getId()));
            appendLengthPrefixed(raw, auditFile.getSourceSignature());
            appendLengthPrefixed(raw, auditFile.getNormalizedRelativePath());
        }
        return sha256Hex(raw.toString());
    }

    private String sourceSignature(String pathHash, Long fileSize, Long modifiedAtUtcEpochMillis) {
        return sha256Hex(pathHash + "|" + fileSize + "|" + modifiedAtUtcEpochMillis);
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private void appendLengthPrefixed(StringBuilder builder, String value) {
        builder.append('|').append(value.length()).append(':').append(value);
    }

    private String sha256Hex(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalStateException("nas uncontrolled import " + fieldName + " is required");
        }
    }

    private Long createTask(Long userId, DccControlledFileNasTransferReqVO reqVO, List<String> collapsedRoots,
                            DccProjectCodeDO projectCode) {
        return tx().execute(status -> {
            DccControlledFileNasTransferTaskDO task = DccControlledFileNasTransferTaskDO.builder()
                    .operatorUserId(userId)
                    .templateCategoryId(reqVO.getTemplateCategoryId())
                    .dccProjectCodeId(projectCode.getId())
                    .productMasterId(null)
                    .effectiveDate(reqVO.getEffectiveDate())
                    .selectedNasPathsJson(JsonUtils.toJsonString(collapsedRoots))
                    .sourceType(SOURCE_TYPE_NAS)
                    .status(TASK_STATUS_WAITING)
                    .build();
            taskMapper.insert(task);
            Map<String, Long> insertedItemIds = new LinkedHashMap<>();
            for (String rootPath : collapsedRoots) {
                Long parentItemId = null;
                StringBuilder builtPath = new StringBuilder();
                for (String segment : rootPath.split("/")) {
                    if (builtPath.length() > 0) {
                        builtPath.append('/');
                    }
                    builtPath.append(segment);
                    String currentPath = builtPath.toString();
                    Long existingItemId = insertedItemIds.get(currentPath);
                    if (existingItemId != null) {
                        parentItemId = existingItemId;
                        continue;
                    }
                    DccControlledFileNasTransferTaskItemDO item = DccControlledFileNasTransferTaskItemDO.builder()
                            .taskId(task.getId())
                            .parentItemId(parentItemId)
                            .itemType(ITEM_TYPE_DIRECTORY)
                            .nasPath(currentPath)
                            .itemName(segment)
                            .status(ITEM_STATUS_WAITING)
                            .attemptCount(0)
                            .previewDownloadOnly(Boolean.FALSE)
                            .build();
                    taskItemMapper.insert(item);
                    insertedItemIds.put(currentPath, item.getId());
                    parentItemId = item.getId();
                }
            }
            return task.getId();
        });
    }

    private Long createLocalFolderTask(Long userId,
                                       DccControlledFileLocalFolderImportReqVO reqVO,
                                       List<LocalFolderFileEntry> fileEntries,
                                       DccProjectCodeDO projectCode) {
        String rootDirectoryName = requireLocalFolderRootDirectoryName(reqVO.getRootDirectoryName());
        return tx().execute(status -> {
            DccControlledFileNasTransferTaskDO task = DccControlledFileNasTransferTaskDO.builder()
                    .operatorUserId(userId)
                    .templateCategoryId(reqVO.getTemplateCategoryId())
                    .dccProjectCodeId(projectCode.getId())
                    .productMasterId(null)
                    .effectiveDate(reqVO.getEffectiveDate())
                    .selectedNasPathsJson(JsonUtils.toJsonString(List.of(rootDirectoryName)))
                    .sourceType(SOURCE_TYPE_LOCAL_FOLDER)
                    .status(TASK_STATUS_WAITING)
                    .expectedFileCount((long) fileEntries.size())
                    .expectedTotalBytes(fileEntries.stream().mapToLong(LocalFolderFileEntry::fileSize).sum())
                    .uploadedFileCount((long) fileEntries.size())
                    .uploadedTotalBytes(fileEntries.stream().mapToLong(LocalFolderFileEntry::fileSize).sum())
                    .uploadCompletedAt(LocalDateTime.now())
                    .build();
            taskMapper.insert(task);
            insertLocalFolderTaskItems(task.getId(), fileEntries, List.of());
            return task.getId();
        });
    }

    private void insertLocalFolderTaskItems(Long taskId,
                                            List<LocalFolderFileEntry> fileEntries,
                                            List<DccControlledFileNasTransferTaskItemDO> existingItems) {
        Map<String, Long> insertedDirectoryItemIds = existingItems.stream()
                .filter(item -> ITEM_TYPE_DIRECTORY.equals(item.getItemType()))
                .filter(item -> StrUtil.isNotBlank(item.getNasPath()))
                .collect(Collectors.toMap(DccControlledFileNasTransferTaskItemDO::getNasPath,
                        DccControlledFileNasTransferTaskItemDO::getId, (left, right) -> left, LinkedHashMap::new));
        for (LocalFolderFileEntry fileEntry : fileEntries) {
            Long parentItemId = null;
            StringBuilder builtPath = new StringBuilder();
            String[] segments = fileEntry.relativePath().split("/");
            for (int index = 0; index < segments.length - 1; index++) {
                String segment = segments[index];
                if (builtPath.length() > 0) {
                    builtPath.append('/');
                }
                builtPath.append(segment);
                String currentPath = builtPath.toString();
                Long existingItemId = insertedDirectoryItemIds.get(currentPath);
                if (existingItemId != null) {
                    parentItemId = existingItemId;
                    continue;
                }
                DccControlledFileNasTransferTaskItemDO directoryItem =
                        DccControlledFileNasTransferTaskItemDO.builder()
                                .taskId(taskId)
                                .parentItemId(parentItemId)
                                .itemType(ITEM_TYPE_DIRECTORY)
                                .nasPath(currentPath)
                                .itemName(segment)
                                .status(ITEM_STATUS_WAITING)
                                .attemptCount(0)
                                .previewDownloadOnly(Boolean.FALSE)
                                .build();
                taskItemMapper.insert(directoryItem);
                insertedDirectoryItemIds.put(currentPath, directoryItem.getId());
                parentItemId = directoryItem.getId();
            }

            DccControlledFileNasTransferTaskItemDO fileItem =
                    DccControlledFileNasTransferTaskItemDO.builder()
                            .taskId(taskId)
                            .parentItemId(parentItemId)
                            .itemType(ITEM_TYPE_FILE)
                            .nasPath(fileEntry.relativePath())
                            .itemName(fileEntry.fileName())
                            .sourceFileId(fileEntry.sourceFileId())
                            .status(ITEM_STATUS_WAITING)
                            .attemptCount(0)
                            .previewDownloadOnly(Boolean.FALSE)
                            .build();
            taskItemMapper.insert(fileItem);
        }
    }

    private List<ValidatedLocalFolderPath> validateLocalFolderPaths(
            DccControlledFileLocalFolderImportReqVO reqVO) {
        return validateLocalFolderPaths(reqVO.getFiles(), reqVO.getRelativePaths(), reqVO.getRootDirectoryName());
    }

    private List<ValidatedLocalFolderPath> validateLocalFolderPaths(
            MultipartFile[] files,
            List<String> relativePaths,
            String rawRootDirectoryName) {
        if (files == null || files.length == 0) {
            throw new IllegalStateException("local folder files required");
        }
        if (relativePaths == null || relativePaths.isEmpty()) {
            throw new IllegalStateException("local folder relativePaths required");
        }
        if (files.length != relativePaths.size()) {
            throw new IllegalStateException("local folder files and relative paths count mismatch");
        }

        String rootDirectoryName = requireLocalFolderRootDirectoryName(rawRootDirectoryName);
        Set<String> seenRelativePaths = new HashSet<>();
        List<ValidatedLocalFolderPath> validatedPaths = new ArrayList<>();
        for (int index = 0; index < relativePaths.size(); index++) {
            MultipartFile file = files[index];
            if (file == null) {
                throw new IllegalStateException("local folder file missing at index: " + index);
            }
            String relativePath = requireLocalFolderRelativePath(relativePaths.get(index), rootDirectoryName);
            if (!seenRelativePaths.add(relativePath)) {
                throw new IllegalStateException("duplicate local folder relative path: " + relativePath);
            }
            validatedPaths.add(new ValidatedLocalFolderPath(relativePath, lastPathSegment(relativePath)));
        }
        return validatedPaths;
    }

    private List<LocalFolderFileEntry> buildLocalFolderFileEntries(
            MultipartFile[] files,
            List<ValidatedLocalFolderPath> validatedPaths) {
        List<LocalFolderFileEntry> fileEntries = new ArrayList<>();
        for (int index = 0; index < files.length; index++) {
            MultipartFile file = files[index];
            ValidatedLocalFolderPath validatedPath = validatedPaths.get(index);
            try {
                Long sourceFileId = fileService.createFileAndReturnId(
                        file.getBytes(),
                        validatedPath.fileName(),
                        ORIGINAL_DIRECTORY,
                        file.getContentType()
                );
                fileEntries.add(new LocalFolderFileEntry(
                        validatedPath.relativePath(),
                        validatedPath.fileName(),
                        sourceFileId,
                        file.getSize()
                ));
            } catch (IOException exception) {
                throw new IllegalStateException("local folder file read failed: "
                        + validatedPath.relativePath(), exception);
            }
        }
        return fileEntries;
    }

    private DccControlledFileLocalFolderImportUploadStateRespVO buildLocalFolderUploadState(
            DccControlledFileNasTransferTaskDO task) {
        String rootDirectoryName = requireSingleLocalFolderRoot(task);
        List<DccControlledFileNasTransferTaskItemDO> existingItems = requireExistingTaskItems(task.getId());
        Set<String> uploadedRelativePathSet = existingItems.stream()
                .filter(item -> ITEM_TYPE_FILE.equals(item.getItemType()))
                .filter(item -> item.getSourceFileId() != null)
                .map(DccControlledFileNasTransferTaskItemDO::getNasPath)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toCollection(HashSet::new));
        List<String> uploadedRelativePaths = existingItems.stream()
                .filter(item -> ITEM_TYPE_FILE.equals(item.getItemType()))
                .filter(item -> item.getSourceFileId() != null)
                .map(DccControlledFileNasTransferTaskItemDO::getNasPath)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toList();
        Map<String, List<DccControlledFileLocalFolderUploadChunkDO>> chunksByRelativePath =
                uploadChunkMapper.selectListByTaskId(task.getId()).stream()
                        .collect(Collectors.groupingBy(DccControlledFileLocalFolderUploadChunkDO::getRelativePath,
                                LinkedHashMap::new, Collectors.toList()));
        Map<String, DccControlledFileLocalFolderImportUploadStateRespVO.FileState> fileStates =
                new LinkedHashMap<>();
        for (Map.Entry<String, List<DccControlledFileLocalFolderUploadChunkDO>> entry : chunksByRelativePath.entrySet()) {
            List<DccControlledFileLocalFolderUploadChunkDO> chunks = entry.getValue();
            DccControlledFileLocalFolderUploadChunkDO firstChunk = chunks.get(0);
            fileStates.put(entry.getKey(), DccControlledFileLocalFolderImportUploadStateRespVO.FileState.builder()
                    .relativePath(entry.getKey())
                    .fileSize(firstChunk.getFileSize())
                    .totalChunks(firstChunk.getTotalChunks())
                    .uploadedChunkIndexes(chunks.stream()
                            .map(DccControlledFileLocalFolderUploadChunkDO::getChunkIndex)
                            .sorted()
                            .toList())
                    .completed(uploadedRelativePathSet.contains(entry.getKey()))
                    .build());
        }
        for (String uploadedRelativePath : uploadedRelativePaths) {
            fileStates.computeIfAbsent(uploadedRelativePath,
                    relativePath -> DccControlledFileLocalFolderImportUploadStateRespVO.FileState.builder()
                            .relativePath(relativePath)
                            .uploadedChunkIndexes(List.of())
                            .completed(true)
                            .build());
        }
        return DccControlledFileLocalFolderImportUploadStateRespVO.builder()
                .taskId(task.getId())
                .rootDirectoryName(rootDirectoryName)
                .status(task.getStatus())
                .expectedFileCount(defaultLong(task.getExpectedFileCount()))
                .expectedTotalBytes(defaultLong(task.getExpectedTotalBytes()))
                .uploadedFileCount(defaultLong(task.getUploadedFileCount()))
                .uploadedTotalBytes(defaultLong(task.getUploadedTotalBytes()))
                .uploadedRelativePaths(uploadedRelativePaths)
                .files(new ArrayList<>(fileStates.values()))
                .build();
    }

    private DccControlledFileLocalFolderImportChunkRespVO buildLocalFolderChunkResponse(
            Long userId, Long taskId, ValidatedLocalFolderChunk validatedChunk, boolean fileCompleted) {
        int uploadedChunkCount = (int) uploadChunkMapper
                .selectListByTaskIdAndRelativePath(taskId, validatedChunk.relativePath()).stream()
                .map(DccControlledFileLocalFolderUploadChunkDO::getChunkIndex)
                .distinct()
                .count();
        return DccControlledFileLocalFolderImportChunkRespVO.builder()
                .taskId(taskId)
                .relativePath(validatedChunk.relativePath())
                .uploadedChunkCount(fileCompleted ? validatedChunk.totalChunks() : uploadedChunkCount)
                .totalChunks(validatedChunk.totalChunks())
                .fileCompleted(fileCompleted)
                .task(getTask(userId, taskId))
                .build();
    }

    private ValidatedLocalFolderChunk validateLocalFolderChunkReq(
            DccControlledFileLocalFolderImportChunkReqVO reqVO, String rootDirectoryName) {
        String relativePath = requireLocalFolderRelativePath(reqVO.getRelativePath(), rootDirectoryName);
        String fileName = StrUtil.trimToEmpty(reqVO.getFileName());
        if (StrUtil.isBlank(fileName) || !fileName.equals(lastPathSegment(relativePath))) {
            throw new IllegalStateException("local folder chunk fileName mismatch: " + relativePath);
        }
        long fileSize = requireNonNegativeCount(reqVO.getFileSize(), "fileSize");
        int chunkIndex = reqVO.getChunkIndex() == null ? -1 : reqVO.getChunkIndex();
        int totalChunks = reqVO.getTotalChunks() == null ? 0 : reqVO.getTotalChunks();
        if (chunkIndex < 0 || totalChunks <= 0 || chunkIndex >= totalChunks) {
            throw new IllegalStateException("local folder chunk index invalid: " + relativePath);
        }
        MultipartFile chunk = reqVO.getChunk();
        if (chunk == null) {
            throw new IllegalStateException("local folder chunk file required: " + relativePath);
        }
        long chunkSize = chunk.getSize();
        if (fileSize == 0) {
            if (chunkIndex != 0 || totalChunks != 1 || chunkSize != 0) {
                throw new IllegalStateException("local folder empty file chunk invalid: " + relativePath);
            }
        } else {
            if (chunkSize <= 0 || chunkSize > fileSize) {
                throw new IllegalStateException("local folder chunk size invalid: " + relativePath);
            }
            if (totalChunks == 1 && chunkSize != fileSize) {
                throw new IllegalStateException("local folder single chunk size mismatch: " + relativePath);
            }
        }
        String chunkSha256 = StrUtil.trimToEmpty(reqVO.getChunkSha256()).toLowerCase();
        if (!chunkSha256.matches("^[0-9a-f]{64}$")) {
            throw new IllegalStateException("local folder chunk sha256 invalid: " + relativePath);
        }
        return new ValidatedLocalFolderChunk(relativePath, fileName, fileSize,
                chunkIndex, totalChunks, chunkSize, chunkSha256);
    }

    private StoredChunkFile storeLocalFolderUploadChunkFile(
            Long taskId, ValidatedLocalFolderChunk validatedChunk, MultipartFile chunk) {
        Path tempPath = localFolderChunkDirectory(taskId, validatedChunk.relativePath())
                .resolve(validatedChunk.chunkIndex() + "." + System.nanoTime() + ".uploading");
        try {
            Files.createDirectories(tempPath.getParent());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long copied = 0L;
            byte[] buffer = new byte[1024 * 1024];
            try (InputStream inputStream = chunk.getInputStream();
                 OutputStream outputStream = Files.newOutputStream(tempPath)) {
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                    outputStream.write(buffer, 0, read);
                    copied += read;
                }
            }
            if (copied != validatedChunk.chunkSize()) {
                deleteIfExists(tempPath);
                throw new IllegalStateException("local folder chunk copied size mismatch: "
                        + validatedChunk.relativePath());
            }
            String actualSha256 = HexFormat.of().formatHex(digest.digest());
            if (!actualSha256.equals(validatedChunk.chunkSha256())) {
                deleteIfExists(tempPath);
                throw new IllegalStateException("local folder chunk sha256 mismatch: "
                        + validatedChunk.relativePath());
            }
            return new StoredChunkFile(tempPath);
        } catch (IOException | NoSuchAlgorithmException exception) {
            deleteIfExists(tempPath);
            throw new IllegalStateException("local folder chunk persist failed: "
                    + validatedChunk.relativePath(), exception);
        }
    }

    private void requireSameLocalFolderChunk(DccControlledFileLocalFolderUploadChunkDO existingChunk,
                                             ValidatedLocalFolderChunk validatedChunk) {
        if (!Objects.equals(existingChunk.getFileName(), validatedChunk.fileName())
                || !Objects.equals(existingChunk.getFileSize(), validatedChunk.fileSize())
                || !Objects.equals(existingChunk.getTotalChunks(), validatedChunk.totalChunks())
                || !Objects.equals(existingChunk.getChunkSize(), validatedChunk.chunkSize())
                || !Objects.equals(existingChunk.getChunkSha256(), validatedChunk.chunkSha256())) {
            throw new IllegalStateException("local folder chunk conflicts with persisted state: "
                    + validatedChunk.relativePath() + "#" + validatedChunk.chunkIndex());
        }
    }

    private boolean isLocalFolderFileChunkSetComplete(Long taskId, ValidatedLocalFolderChunk validatedChunk) {
        List<DccControlledFileLocalFolderUploadChunkDO> chunks =
                uploadChunkMapper.selectListByTaskIdAndRelativePath(taskId, validatedChunk.relativePath());
        if (chunks.size() < validatedChunk.totalChunks()) {
            return false;
        }
        Set<Integer> uploadedIndexes = chunks.stream()
                .map(DccControlledFileLocalFolderUploadChunkDO::getChunkIndex)
                .collect(Collectors.toSet());
        for (int index = 0; index < validatedChunk.totalChunks(); index++) {
            if (!uploadedIndexes.contains(index)) {
                return false;
            }
        }
        return true;
    }

    private void completeLocalFolderFileFromChunks(DccControlledFileNasTransferTaskDO current,
                                                   ValidatedLocalFolderChunk validatedChunk,
                                                   String contentType) {
        List<DccControlledFileNasTransferTaskItemDO> existingItems = requireExistingTaskItems(current.getId());
        assertNoDuplicateLocalFolderPaths(existingItems, List.of(
                new ValidatedLocalFolderPath(validatedChunk.relativePath(), validatedChunk.fileName())));
        long currentUploadedFileCount =
                requireNonNegativeCount(defaultLong(current.getUploadedFileCount()), "uploadedFileCount");
        long currentUploadedTotalBytes =
                requireNonNegativeCount(defaultLong(current.getUploadedTotalBytes()), "uploadedTotalBytes");
        long currentExpectedFileCount =
                requirePositiveCount(defaultLong(current.getExpectedFileCount()), "expectedFileCount");
        long currentExpectedTotalBytes =
                requireNonNegativeCount(defaultLong(current.getExpectedTotalBytes()), "expectedTotalBytes");
        if (currentUploadedFileCount + 1 > currentExpectedFileCount) {
            throw new IllegalStateException("local folder uploaded file count exceeds expected count");
        }
        if (currentUploadedTotalBytes + validatedChunk.fileSize() > currentExpectedTotalBytes) {
            throw new IllegalStateException("local folder uploaded total bytes exceeds expected total bytes");
        }

        Long sourceFileId = mergeLocalFolderChunksAndCreateFile(current.getId(), validatedChunk, contentType);
        insertLocalFolderTaskItems(current.getId(), List.of(new LocalFolderFileEntry(
                validatedChunk.relativePath(),
                validatedChunk.fileName(),
                sourceFileId,
                validatedChunk.fileSize()
        )), existingItems);
        current.setUploadedFileCount(currentUploadedFileCount + 1);
        current.setUploadedTotalBytes(currentUploadedTotalBytes + validatedChunk.fileSize());
        current.setLastFailureMessage(null);
        taskMapper.updateById(current);
    }

    private Long mergeLocalFolderChunksAndCreateFile(Long taskId,
                                                     ValidatedLocalFolderChunk validatedChunk,
                                                     String contentType) {
        List<DccControlledFileLocalFolderUploadChunkDO> chunks =
                new ArrayList<>(uploadChunkMapper.selectListByTaskIdAndRelativePath(
                        taskId, validatedChunk.relativePath()));
        chunks.sort(Comparator.comparing(DccControlledFileLocalFolderUploadChunkDO::getChunkIndex));
        if (chunks.size() != validatedChunk.totalChunks()) {
            throw new IllegalStateException("local folder chunk set incomplete: " + validatedChunk.relativePath());
        }
        Path mergedPath = localFolderChunkDirectory(taskId, validatedChunk.relativePath())
                .resolve("merged-" + System.nanoTime() + ".tmp");
        try {
            long mergedSize = 0L;
            Files.createDirectories(mergedPath.getParent());
            try (OutputStream outputStream = Files.newOutputStream(mergedPath)) {
                for (int index = 0; index < validatedChunk.totalChunks(); index++) {
                    DccControlledFileLocalFolderUploadChunkDO chunk = chunks.get(index);
                    if (!Objects.equals(index, chunk.getChunkIndex())) {
                        throw new IllegalStateException("local folder chunk index gap: "
                                + validatedChunk.relativePath());
                    }
                    Path chunkPath = Path.of(chunk.getChunkTempPath());
                    if (!Files.exists(chunkPath)) {
                        throw new IllegalStateException("local folder chunk file missing: " + chunkPath);
                    }
                    Files.copy(chunkPath, outputStream);
                    mergedSize += Files.size(chunkPath);
                }
            }
            if (mergedSize != validatedChunk.fileSize()) {
                throw new IllegalStateException("local folder merged file size mismatch: "
                        + validatedChunk.relativePath());
            }
            return fileService.createFileAndReturnId(mergedPath, validatedChunk.fileSize(),
                    validatedChunk.fileName(), ORIGINAL_DIRECTORY, contentType);
        } catch (IOException exception) {
            throw new IllegalStateException("local folder chunk merge failed: "
                    + validatedChunk.relativePath(), exception);
        } finally {
            deleteIfExists(mergedPath);
        }
    }

    private void deleteCompletedLocalFolderChunkFiles(Long taskId, String relativePath) {
        for (DccControlledFileLocalFolderUploadChunkDO chunk :
                uploadChunkMapper.selectListByTaskIdAndRelativePath(taskId, relativePath)) {
            deleteIfExists(Path.of(chunk.getChunkTempPath()));
        }
    }

    private boolean isLocalFolderPathAlreadyUploaded(Long taskId, String relativePath) {
        return requireExistingTaskItems(taskId).stream()
                .anyMatch(item -> ITEM_TYPE_FILE.equals(item.getItemType())
                        && Objects.equals(item.getNasPath(), relativePath)
                        && item.getSourceFileId() != null);
    }

    private Path localFolderChunkDirectory(Long taskId, String relativePath) {
        String root = StrUtil.blankToDefault(multipartLocation, System.getProperty("java.io.tmpdir"));
        return Path.of(root, LOCAL_FOLDER_UPLOAD_CHUNK_DIRECTORY, String.valueOf(taskId), sha1(relativePath));
    }

    private Path finalLocalFolderChunkPath(Long taskId, ValidatedLocalFolderChunk validatedChunk) {
        return localFolderChunkDirectory(taskId, validatedChunk.relativePath())
                .resolve(validatedChunk.chunkIndex() + ".part");
    }

    private void moveStoredChunkToFinalPath(Path tempPath, Path finalPath) {
        try {
            Files.createDirectories(finalPath.getParent());
            Files.move(tempPath, finalPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalStateException("local folder chunk finalization failed: " + finalPath, exception);
        }
    }

    private void deleteIfExists(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            log.warn("[deleteIfExists][path({}) temporary file deletion failed]", path, exception);
        }
    }

    private DccControlledFileNasTransferTaskDO requireLocalFolderUploadingTask(Long userId, Long taskId) {
        DccControlledFileNasTransferTaskDO task = requireOwnedTask(userId, taskId);
        if (!SOURCE_TYPE_LOCAL_FOLDER.equals(sourceTypeOf(task))) {
            throw new IllegalStateException("nas transfer task is not local folder import: " + taskId);
        }
        if (!TASK_STATUS_UPLOADING.equals(task.getStatus())) {
            throw new IllegalStateException("local folder import session is not uploading: " + taskId);
        }
        return task;
    }

    private String requireSingleLocalFolderRoot(DccControlledFileNasTransferTaskDO task) {
        List<String> roots = JsonUtils.parseArray(
                StrUtil.blankToDefault(task.getSelectedNasPathsJson(), "[]"), String.class);
        if (roots.size() != 1) {
            throw new IllegalStateException("local folder import session root count invalid: " + task.getId());
        }
        return requireLocalFolderRootDirectoryName(roots.get(0));
    }

    private List<DccControlledFileNasTransferTaskItemDO> requireExistingTaskItems(Long taskId) {
        List<DccControlledFileNasTransferTaskItemDO> existingItems = taskItemMapper.selectListByTaskId(taskId);
        if (existingItems == null) {
            throw new IllegalStateException("local folder task items query returned null: " + taskId);
        }
        return existingItems;
    }

    private void assertNoDuplicateLocalFolderPaths(List<DccControlledFileNasTransferTaskItemDO> existingItems,
                                                   List<ValidatedLocalFolderPath> validatedPaths) {
        Set<String> existingPaths = existingItems.stream()
                .map(DccControlledFileNasTransferTaskItemDO::getNasPath)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
        for (ValidatedLocalFolderPath validatedPath : validatedPaths) {
            if (existingPaths.contains(validatedPath.relativePath())) {
                throw new IllegalStateException("duplicate local folder relative path: "
                        + validatedPath.relativePath());
            }
        }
    }

    private long totalFileSize(MultipartFile[] files) {
        long total = 0L;
        for (MultipartFile file : files) {
            if (file.getSize() < 0) {
                throw new IllegalStateException("local folder file size invalid: " + file.getOriginalFilename());
            }
            total = Math.addExact(total, file.getSize());
        }
        return total;
    }

    private long requirePositiveCount(Long value, String fieldName) {
        long count = defaultLong(value);
        if (count <= 0) {
            throw new IllegalStateException("local folder " + fieldName + " must be positive");
        }
        return count;
    }

    private long requireNonNegativeCount(Long value, String fieldName) {
        long count = defaultLong(value);
        if (count < 0) {
            throw new IllegalStateException("local folder " + fieldName + " must be non-negative");
        }
        return count;
    }

    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private String requireLocalFolderRootDirectoryName(String rootDirectoryName) {
        String root = StrUtil.trimToEmpty(rootDirectoryName);
        if (StrUtil.isBlank(root) || root.contains("/") || root.contains("\\")
                || ".".equals(root) || "..".equals(root) || isAbsoluteLocalFolderPath(root)) {
            throw new IllegalStateException("invalid local folder rootDirectoryName: " + rootDirectoryName);
        }
        return root;
    }

    private String requireLocalFolderRelativePath(String rawRelativePath, String rootDirectoryName) {
        String relativePath = StrUtil.trimToEmpty(rawRelativePath);
        if (StrUtil.isBlank(relativePath) || relativePath.contains("\\")
                || relativePath.startsWith("/") || isAbsoluteLocalFolderPath(relativePath)
                || relativePath.endsWith("/")) {
            throw new IllegalStateException("unsafe local folder relative path: " + rawRelativePath);
        }
        String[] segments = relativePath.split("/", -1);
        if (segments.length < 2) {
            throw new IllegalStateException("unsafe local folder relative path: " + rawRelativePath);
        }
        for (String segment : segments) {
            if (StrUtil.isBlank(segment) || ".".equals(segment) || "..".equals(segment)) {
                throw new IllegalStateException("unsafe local folder relative path: " + rawRelativePath);
            }
        }
        if (!rootDirectoryName.equals(segments[0])) {
            throw new IllegalStateException("local folder relative path root mismatch: " + rawRelativePath);
        }
        return relativePath;
    }

    private boolean isAbsoluteLocalFolderPath(String path) {
        return StrUtil.isNotBlank(path) && path.matches("^[A-Za-z]:.*");
    }

    private void triggerTaskAsync(Long tenantId) {
        CompletableFuture.runAsync(() -> TenantUtils.execute(tenantId, () -> {
            try {
                processWaitingTasks();
            } catch (RuntimeException exception) {
                log.error("[triggerTaskAsync][tenantId({}) DCC NAS transfer async execution failed]", tenantId, exception);
            }
        }));
    }

    private void executeTask(Long taskId) {
        DccControlledFileNasTransferTaskDO task = taskMapper.selectById(taskId);
        if (task == null || TASK_STATUS_COMPLETED.equals(task.getStatus())
                || TASK_STATUS_FAILED.equals(task.getStatus())
                || TASK_STATUS_CANCELLED.equals(task.getStatus())) {
            return;
        }
        if (taskMapper.claimWaitingTask(taskId, LocalDateTime.now()) == 0) {
            return;
        }
        if (isOriginalPathSyncTask(task)) {
            executeOriginalPathSyncTask(task);
            return;
        }

        Snapshot snapshot = Snapshot.load(
                directoryMapper.selectList(),
                directoryAccessRuleMapper.selectList(),
                categoryMapper.selectList(),
                categoryDirectoryBindingMapper.selectList(),
                permissionRuleMapper.selectList(),
                distributionRuleMapper.selectList(),
                trainingRuleMapper.selectList(),
                routeMapper.selectList(),
                routeNodeMapper.selectList()
        );
        TaskRuntime runtime = TaskRuntime.fromItems(taskItemMapper.selectListByTaskId(taskId));
        SelectedCategoryContext selectedCategory;
        try {
            selectedCategory = requireSelectedCategoryContext(task.getTemplateCategoryId(), snapshot);
        } catch (RuntimeException exception) {
            log.error("[executeTask][taskId({}) DCC NAS transfer selected category validation failed]",
                    taskId, exception);
            markTaskFailed(taskId, resolveThrowableMessage(exception));
            return;
        }

        try {
            boolean localFolderTask = isLocalFolderTask(task);
            while (true) {
                if (isTaskCancelling(taskId)) {
                    markTaskCancelled(taskId, CANCEL_REASON);
                    return;
                }
                DccControlledFileNasTransferTaskItemDO nextItem = taskItemMapper.selectFirstWaitingItemByTaskId(taskId);
                if (nextItem == null) {
                    if (isTaskCancelling(taskId)) {
                        markTaskCancelled(taskId, CANCEL_REASON);
                    } else {
                        finalizeTask(taskId);
                    }
                    return;
                }
                if (ITEM_TYPE_DIRECTORY.equals(nextItem.getItemType())) {
                    if (localFolderTask) {
                        processLocalFolderDirectoryItem(nextItem, selectedCategory, snapshot);
                    } else {
                        processDirectoryItem(nextItem, selectedCategory, snapshot, runtime);
                    }
                } else {
                    if (localFolderTask) {
                        processLocalFolderFileItem(task, nextItem, selectedCategory, snapshot);
                    } else {
                        processFileItem(task, nextItem, selectedCategory, snapshot);
                    }
                }
            }
        } catch (RuntimeException exception) {
            log.error("[executeTask][taskId({}) DCC NAS transfer task failed]", taskId, exception);
            markTaskFailed(taskId, resolveThrowableMessage(exception));
        }
    }

    private void executeOriginalPathSyncTask(DccControlledFileNasTransferTaskDO task) {
        Map<String, List<FileNasListRespVO.Item>> listingByParentPath = new HashMap<>();
        try {
            while (true) {
                if (isTaskCancelling(task.getId())) {
                    markTaskCancelled(task.getId(), CANCEL_REASON);
                    return;
                }
                DccControlledFileNasTransferTaskItemDO nextItem =
                        taskItemMapper.selectFirstWaitingItemByTaskId(task.getId());
                if (nextItem == null) {
                    if (isTaskCancelling(task.getId())) {
                        markTaskCancelled(task.getId(), CANCEL_REASON);
                    } else {
                        finalizeTask(task.getId());
                    }
                    return;
                }
                processOriginalPathSyncFileItem(task, nextItem, listingByParentPath);
            }
        } catch (RuntimeException exception) {
            log.error("[executeOriginalPathSyncTask][taskId({}) original-path sync task failed]",
                    task.getId(), exception);
            markTaskFailed(task.getId(), resolveThrowableMessage(exception));
        }
    }

    private void processOriginalPathSyncFileItem(DccControlledFileNasTransferTaskDO task,
                                                 DccControlledFileNasTransferTaskItemDO item,
                                                 Map<String, List<FileNasListRespVO.Item>> listingByParentPath) {
        if (taskItemMapper.claimWaitingItem(item.getId()) == 0) {
            return;
        }
        try {
            DccNasControlAuditFileDO auditFile = requireOriginalPathSyncItemSnapshot(task, item);
            FileNasListRespVO.Item currentNasFile =
                    requireCurrentOriginalPathNasFile(auditFile, listingByParentPath);
            DccNasOriginalPathSyncFileDO activeSync = originalPathSyncFileMapper.selectActiveByPathHash(
                    auditFile.getNasShareName(), auditFile.getPathHash());
            if (activeSync != null) {
                throw new IllegalStateException("nas original-path sync file already active: " + auditFile.getId());
            }
            NasFileReadResult sourceFile = nasBrowserService.readFile(auditFile.getNormalizedRelativePath());
            if (sourceFile == null || sourceFile.bytes() == null) {
                throw new IllegalStateException("nas original-path sync source file missing: " + auditFile.getId());
            }
            if (!Objects.equals((long) sourceFile.bytes().length, currentNasFile.getSize())) {
                throw new IllegalStateException("nas original-path sync source file size changed: "
                        + auditFile.getId());
            }
            String fileName = StrUtil.blankToDefault(sourceFile.name(), auditFile.getFileName());
            if (StrUtil.isBlank(fileName)) {
                throw new IllegalStateException("nas original-path sync source file name missing: "
                        + auditFile.getId());
            }
            persistOriginalPathSyncFile(task, item, auditFile, currentNasFile, sourceFile, fileName);
        } catch (RuntimeException exception) {
            markOriginalPathSyncItemFailed(item, resolveThrowableMessage(exception));
        }
    }

    private DccNasControlAuditFileDO requireOriginalPathSyncItemSnapshot(
            DccControlledFileNasTransferTaskDO task, DccControlledFileNasTransferTaskItemDO item) {
        if (!ITEM_TYPE_FILE.equals(item.getItemType())) {
            throw new IllegalStateException("nas original-path sync item type invalid: " + item.getId());
        }
        if (item.getAuditFileId() == null) {
            throw new IllegalStateException("nas original-path sync auditFileId missing: " + item.getId());
        }
        DccNasControlAuditFileDO auditFile = auditFileMapper.selectById(item.getAuditFileId());
        if (auditFile == null || !Objects.equals(task.getAuditTaskId(), auditFile.getTaskId())) {
            throw new IllegalStateException("nas original-path sync audit file task mismatch: "
                    + item.getAuditFileId());
        }
        if (!Objects.equals(task.getId(), auditFile.getOriginalPathSyncTaskId())
                || !Objects.equals(item.getId(), auditFile.getOriginalPathSyncTaskItemId())) {
            throw new IllegalStateException("nas original-path sync audit file not bound to item: "
                    + auditFile.getId());
        }
        requireOriginalPathSyncBaseSnapshot(task.getAuditTaskId(), auditFile);
        if (auditFile.getOriginalPathSyncFileId() != null) {
            throw new IllegalStateException("nas original-path sync audit file already active: "
                    + auditFile.getId());
        }
        if (!AUDIT_FILE_ORIGINAL_PATH_SYNC_STATUS_WAITING.equals(auditFile.getOriginalPathSyncStatus())
                && !AUDIT_FILE_ORIGINAL_PATH_SYNC_STATUS_RUNNING.equals(auditFile.getOriginalPathSyncStatus())) {
            throw new IllegalStateException("nas original-path sync audit file status invalid: "
                    + auditFile.getId());
        }
        if (!Objects.equals(item.getSourceSignature(), auditFile.getSourceSignature())) {
            throw new IllegalStateException("nas original-path sync source signature mismatch: "
                    + auditFile.getId());
        }
        if (!Objects.equals(item.getNasPath(), auditFile.getNormalizedRelativePath())
                || !Objects.equals(item.getLocalRelativePath(), auditFile.getNormalizedRelativePath())) {
            throw new IllegalStateException("nas original-path sync path snapshot mismatch: " + auditFile.getId());
        }
        return auditFile;
    }

    private FileNasListRespVO.Item requireCurrentOriginalPathNasFile(
            DccNasControlAuditFileDO auditFile,
            Map<String, List<FileNasListRespVO.Item>> listingByParentPath) {
        String parentPath = parentPathOf(auditFile.getNormalizedRelativePath());
        List<FileNasListRespVO.Item> items = listingByParentPath.computeIfAbsent(parentPath, path -> {
            FileNasListRespVO listing = nasBrowserService.listFiles(path);
            if (listing == null || listing.getItems() == null) {
                throw new IllegalStateException("nas original-path sync listing missing: " + path);
            }
            return listing.getItems();
        });
        for (FileNasListRespVO.Item item : items) {
            String itemPath = DccNasPathUtils.normalizeRelativePath(item.getPath());
            if (Boolean.TRUE.equals(item.getDir())
                    || !Objects.equals(itemPath, auditFile.getNormalizedRelativePath())) {
                continue;
            }
            if (item.getSize() == null || item.getModifiedAt() == null) {
                throw new IllegalStateException("nas original-path sync current snapshot incomplete: "
                        + auditFile.getId());
            }
            String currentSignature = sourceSignature(auditFile.getPathHash(), item.getSize(), item.getModifiedAt());
            if (!Objects.equals(currentSignature, auditFile.getSourceSignature())) {
                throw new IllegalStateException("nas original-path sync source signature changed: "
                        + auditFile.getId());
            }
            return item;
        }
        throw new IllegalStateException("nas original-path sync source path missing: " + auditFile.getId());
    }

    private void persistOriginalPathSyncFile(DccControlledFileNasTransferTaskDO task,
                                             DccControlledFileNasTransferTaskItemDO item,
                                             DccNasControlAuditFileDO auditFile,
                                             FileNasListRespVO.Item currentNasFile,
                                             NasFileReadResult sourceFile,
                                             String fileName) {
        LocalDateTime now = LocalDateTime.now();
        tx().executeWithoutResult(status -> {
            DccControlledFileNasTransferTaskItemDO currentItem = taskItemMapper.selectById(item.getId());
            if (currentItem == null) {
                throw new IllegalStateException("nas original-path sync item running state missing: " + item.getId());
            }
            DccNasControlAuditFileDO currentAuditFile = requireOriginalPathSyncItemSnapshot(task, currentItem);
            DccNasOriginalPathSyncFileDO activeSync = originalPathSyncFileMapper.selectActiveByPathHash(
                    currentAuditFile.getNasShareName(), currentAuditFile.getPathHash());
            if (activeSync != null) {
                throw new IllegalStateException("nas original-path sync file already active: "
                        + currentAuditFile.getId());
            }
            Long sourceFileId = fileService.createFileAndReturnId(
                    sourceFile.bytes(),
                    fileName,
                    originalPathSyncStorageDirectory(currentAuditFile.getNormalizedRelativePath()),
                    sourceFile.contentType()
            );
            if (sourceFileId == null) {
                throw new IllegalStateException("nas original-path sync source file id missing: "
                        + currentAuditFile.getId());
            }
            DccNasOriginalPathSyncFileDO syncFile = DccNasOriginalPathSyncFileDO.builder()
                    .auditTaskId(currentAuditFile.getTaskId())
                    .auditFileId(currentAuditFile.getId())
                    .transferTaskId(task.getId())
                    .transferTaskItemId(currentItem.getId())
                    .sourceFileId(sourceFileId)
                    .nasShareName(currentAuditFile.getNasShareName())
                    .rootPath(currentAuditFile.getRootPath())
                    .normalizedRelativePath(currentAuditFile.getNormalizedRelativePath())
                    .pathHash(currentAuditFile.getPathHash())
                    .fileName(fileName)
                    .fileSize(currentNasFile.getSize())
                    .modifiedAt(LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(currentNasFile.getModifiedAt()), java.time.ZoneOffset.UTC))
                    .sourceSignature(currentAuditFile.getSourceSignature())
                    .syncStatus(ORIGINAL_PATH_SYNC_FILE_STATUS_ACTIVE)
                    .syncedByUserId(task.getOperatorUserId())
                    .syncedAt(now)
                    .tenantId(TenantContextHolder.getRequiredTenantId())
                    .build();
            originalPathSyncFileMapper.insert(syncFile);
            if (syncFile.getId() == null) {
                throw new IllegalStateException("nas original-path sync persisted id missing: "
                        + currentAuditFile.getId());
            }

            currentAuditFile.setOriginalPathSyncStatus(AUDIT_FILE_ORIGINAL_PATH_SYNC_STATUS_ACTIVE);
            currentAuditFile.setOriginalPathSyncFileId(syncFile.getId());
            currentAuditFile.setOriginalPathSyncTaskId(task.getId());
            currentAuditFile.setOriginalPathSyncTaskItemId(currentItem.getId());
            currentAuditFile.setOriginalPathSyncErrorCode(null);
            currentAuditFile.setOriginalPathSyncError(null);
            auditFileMapper.updateById(currentAuditFile);

            currentItem.setSourceFileId(sourceFileId);
            currentItem.setStatus(ITEM_STATUS_COMPLETED);
            currentItem.setAttemptCount(incrementCount(currentItem.getAttemptCount()));
            currentItem.setLastAttemptAt(now);
            currentItem.setCompletedAt(now);
            currentItem.setFailureStage(null);
            currentItem.setLastError(null);
            taskItemMapper.updateById(currentItem);
        });
    }

    private void markOriginalPathSyncItemFailed(DccControlledFileNasTransferTaskItemDO item, String reason) {
        markItemFailed(item.getId(), "original-path-sync", reason);
        DccNasControlAuditFileDO auditFile = item.getAuditFileId() == null
                ? null : auditFileMapper.selectById(item.getAuditFileId());
        if (auditFile == null || !Objects.equals(item.getId(), auditFile.getOriginalPathSyncTaskItemId())) {
            return;
        }
        auditFile.setOriginalPathSyncStatus(AUDIT_FILE_ORIGINAL_PATH_SYNC_STATUS_FAILED);
        auditFile.setOriginalPathSyncErrorCode("ORIGINAL_PATH_SYNC_FAILED");
        auditFile.setOriginalPathSyncError(fitDatabaseErrorMessage(reason));
        auditFileMapper.updateById(auditFile);
    }

    private String originalPathSyncStorageDirectory(String normalizedRelativePath) {
        String parentPath = parentPathOf(normalizedRelativePath);
        if (StrUtil.isBlank(parentPath)) {
            return ORIGINAL_PATH_SYNC_DIRECTORY;
        }
        return ORIGINAL_PATH_SYNC_DIRECTORY + "/" + parentPath;
    }

    private String parentPathOf(String normalizedRelativePath) {
        String path = DccNasPathUtils.normalizeRelativePath(normalizedRelativePath);
        int index = path.lastIndexOf('/');
        return index < 0 ? "" : path.substring(0, index);
    }

    private void processDirectoryItem(DccControlledFileNasTransferTaskItemDO item,
                                      SelectedCategoryContext selectedCategory,
                                      Snapshot snapshot,
                                      TaskRuntime runtime) {
        if (taskItemMapper.claimWaitingItem(item.getId()) == 0) {
            return;
        }
        NasAclReadResult acl;
        try {
            acl = nasBrowserService.readDirectoryAcl(item.getNasPath());
        } catch (Exception exception) {
            markItemFailed(item.getId(), "acl", resolveThrowableMessage(exception));
            return;
        }

        FileNasListRespVO listing;
        try {
            listing = nasBrowserService.listFiles(item.getNasPath());
        } catch (Exception exception) {
            markItemFailed(item.getId(), "list", resolveThrowableMessage(exception));
            return;
        }

        List<PendingChildItem> childItems = new ArrayList<>();
        Set<String> knownOrQueuedNasPaths = new HashSet<>(runtime.knownNasPaths());
        for (FileNasListRespVO.Item child : listing.getItems()) {
            String childPath = normalizePath(child.getPath());
            if (StrUtil.isBlank(childPath) || !knownOrQueuedNasPaths.add(childPath)) {
                continue;
            }
            childItems.add(new PendingChildItem(
                    Boolean.TRUE.equals(child.getDir()) ? ITEM_TYPE_DIRECTORY : ITEM_TYPE_FILE,
                    childPath,
                    StrUtil.blankToDefault(StrUtil.trimToEmpty(child.getName()), lastPathSegment(childPath))
            ));
        }

        List<String> insertedPaths = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        try {
            tx().executeWithoutResult(status -> {
                DccControlledFileNasTransferTaskItemDO current = taskItemMapper.selectById(item.getId());
                DirectoryResolution directoryResolution = resolveDirectoryForItem(current, snapshot,
                        selectedCategory.nasRootParentDirectoryId(), "Created from NAS transfer task");
                current.setResolvedDirectoryId(directoryResolution.directory().getId());
                current.setDirectoryOutcome(directoryResolution.outcome());
                snapshotCaptureService.captureDirectorySnapshot(current.getTaskId(), current.getId(),
                        current.getNasPath(), directoryResolution.directory().getId(), acl);
                current.setStatus(ITEM_STATUS_COMPLETED);
                current.setAttemptCount(incrementCount(current.getAttemptCount()));
                current.setLastAttemptAt(now);
                current.setCompletedAt(now);
                current.setFailureStage(null);
                current.setLastError(null);
                taskItemMapper.updateById(current);
                for (PendingChildItem childItem : childItems) {
                    DccControlledFileNasTransferTaskItemDO child = DccControlledFileNasTransferTaskItemDO.builder()
                            .taskId(current.getTaskId())
                            .parentItemId(current.getId())
                            .itemType(childItem.itemType())
                            .nasPath(childItem.nasPath())
                            .itemName(childItem.itemName())
                            .status(ITEM_STATUS_WAITING)
                            .attemptCount(0)
                            .previewDownloadOnly(Boolean.FALSE)
                            .build();
                    taskItemMapper.insert(child);
                    insertedPaths.add(childItem.nasPath());
                }
            });
        } catch (RuntimeException exception) {
            markItemFailed(item.getId(), "directory", resolveThrowableMessage(exception));
            return;
        }

        runtime.knownNasPaths().addAll(insertedPaths);
    }

    private void processLocalFolderDirectoryItem(DccControlledFileNasTransferTaskItemDO item,
                                                 SelectedCategoryContext selectedCategory,
                                                 Snapshot snapshot) {
        if (taskItemMapper.claimWaitingItem(item.getId()) == 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        try {
            tx().executeWithoutResult(status -> {
                DccControlledFileNasTransferTaskItemDO current = taskItemMapper.selectById(item.getId());
                DirectoryResolution directoryResolution = resolveLocalFolderDirectoryForItem(current,
                        selectedCategory, snapshot);
                current.setResolvedDirectoryId(directoryResolution.directory().getId());
                current.setDirectoryOutcome(directoryResolution.outcome());
                current.setStatus(ITEM_STATUS_COMPLETED);
                current.setAttemptCount(incrementCount(current.getAttemptCount()));
                current.setLastAttemptAt(now);
                current.setCompletedAt(now);
                current.setFailureStage(null);
                current.setLastError(null);
                taskItemMapper.updateById(current);
            });
        } catch (RuntimeException exception) {
            markItemFailed(item.getId(), "directory", resolveThrowableMessage(exception));
        }
    }

    private void processFileItem(DccControlledFileNasTransferTaskDO task,
                                 DccControlledFileNasTransferTaskItemDO item,
                                 SelectedCategoryContext selectedCategory,
                                 Snapshot snapshot) {
        if (taskItemMapper.claimWaitingItem(item.getId()) == 0) {
            return;
        }
        DccControlledFileNasTransferTaskItemDO parentDirectoryItem = taskItemMapper.selectById(item.getParentItemId());
        if (parentDirectoryItem == null || parentDirectoryItem.getResolvedDirectoryId() == null) {
            markItemFailed(item.getId(), "directory",
                    "parent directory item unresolved: " + item.getParentItemId());
            return;
        }

        try {
            assignSelectedCategoryForFileItem(item, selectedCategory, snapshot);
        } catch (RuntimeException exception) {
            markItemFailed(item.getId(), "category", resolveThrowableMessage(exception));
            return;
        }

        NasFileReadResult sourceFile;
        try {
            sourceFile = nasBrowserService.readFile(item.getNasPath());
        } catch (Exception exception) {
            markItemFailed(item.getId(), "read", resolveThrowableMessage(exception));
            return;
        }

        DccControlledFilePreviewKindEnum previewKind = DccControlledFilePreviewKindEnum.resolve(
                sourceFile.name(), sourceFile.contentType());
        String nasShareName = nasSettingsService.getRequiredNasConfig().share();
        LocalDateTime now = LocalDateTime.now();
        try {
            tx().executeWithoutResult(status -> {
                DccControlledFileNasTransferTaskItemDO current = taskItemMapper.selectById(item.getId());
                DccControlledFileNasTransferTaskItemDO latestParent = taskItemMapper.selectById(item.getParentItemId());
                if (latestParent == null || latestParent.getResolvedDirectoryId() == null) {
                    throw new IllegalStateException("parent directory item unresolved: " + item.getParentItemId());
                }
                if (latestParent.getResolvedCategoryId() == null) {
                    throw new IllegalStateException("parent category item unresolved: " + item.getParentItemId());
                }
                if (!snapshot.directoriesById().containsKey(latestParent.getResolvedDirectoryId())) {
                    throw new IllegalStateException("dcc directory missing: " + latestParent.getResolvedDirectoryId());
                }
                Long originalFileId = fileService.createFileAndReturnId(
                        sourceFile.bytes(),
                        sourceFile.name(),
                        ORIGINAL_DIRECTORY,
                        sourceFile.contentType()
                );
                DccControlledFileSubmitReqVO submitReqVO = new DccControlledFileSubmitReqVO();
                submitReqVO.setCategoryId(latestParent.getResolvedCategoryId());
                submitReqVO.setDirectoryId(latestParent.getResolvedDirectoryId());
                submitReqVO.setProductMasterId(null);
                submitReqVO.setDccProjectCodeId(task.getDccProjectCodeId());
                submitReqVO.setOriginalFileId(originalFileId);
                submitReqVO.setChangeType(DccControlledFileChangeTypeEnum.NEW.getCode());
                submitReqVO.setFileName(sourceFile.name());
                submitReqVO.setFileNumber(fileNumberOf(sourceFile.name(), item.getNasPath()));
                submitReqVO.setVersionNo("V1.0");
                submitReqVO.setEffectiveDate(task.getEffectiveDate());
                submitReqVO.setRemark("NAS transfer source: " + item.getNasPath());
                Long controlledFileId = workflowService.submitControlledFileWithoutApproval(
                        task.getOperatorUserId(), submitReqVO);
                String normalizedPath = DccNasPathUtils.normalizeRelativePath(item.getNasPath());
                nasSourceMapper.insert(DccControlledFileNasSourceDO.builder()
                        .controlledFileId(controlledFileId)
                        .nasShareName(nasShareName)
                        .normalizedRelativePath(normalizedPath)
                        .pathHash(DccNasPathUtils.pathHash(nasShareName, normalizedPath))
                        .sourceType(DccNasControlAuditServiceImpl.SOURCE_TYPE_NAS_TRANSFER)
                        .sourceConfidence(DccNasControlAuditServiceImpl.SOURCE_CONFIDENCE_EXACT)
                        .tenantId(TenantContextHolder.getRequiredTenantId())
                        .build());

                current.setStatus(ITEM_STATUS_COMPLETED);
                current.setAttemptCount(incrementCount(current.getAttemptCount()));
                current.setLastAttemptAt(now);
                current.setCompletedAt(now);
                current.setFailureStage(null);
                current.setLastError(null);
                current.setPreviewDownloadOnly(previewKind == DccControlledFilePreviewKindEnum.DOWNLOAD_ONLY);
                taskItemMapper.updateById(current);
            });
        } catch (ServiceException exception) {
            markItemFailed(item.getId(), "submit", resolveThrowableMessage(exception));
        } catch (RuntimeException exception) {
            markItemFailed(item.getId(), "submit", resolveThrowableMessage(exception));
        }
    }

    private void processLocalFolderFileItem(DccControlledFileNasTransferTaskDO task,
                                            DccControlledFileNasTransferTaskItemDO item,
                                            SelectedCategoryContext selectedCategory,
                                            Snapshot snapshot) {
        if (taskItemMapper.claimWaitingItem(item.getId()) == 0) {
            return;
        }
        DccControlledFileNasTransferTaskItemDO parentDirectoryItem = taskItemMapper.selectById(item.getParentItemId());
        if (parentDirectoryItem == null || parentDirectoryItem.getResolvedDirectoryId() == null) {
            markItemFailed(item.getId(), "directory",
                    "parent directory item unresolved: " + item.getParentItemId());
            return;
        }
        if (item.getSourceFileId() == null) {
            markItemFailed(item.getId(), "source-file",
                    "local folder source file missing: " + item.getNasPath());
            return;
        }

        try {
            assignSelectedCategoryForFileItem(item, selectedCategory, snapshot);
        } catch (RuntimeException exception) {
            markItemFailed(item.getId(), "category", resolveThrowableMessage(exception));
            return;
        }

        String fileName = StrUtil.blankToDefault(item.getItemName(), lastPathSegment(item.getNasPath()));
        DccControlledFilePreviewKindEnum previewKind =
                DccControlledFilePreviewKindEnum.resolve(fileName, null);
        LocalDateTime now = LocalDateTime.now();
        try {
            tx().executeWithoutResult(status -> {
                DccControlledFileNasTransferTaskItemDO current = taskItemMapper.selectById(item.getId());
                DccControlledFileNasTransferTaskItemDO latestParent = taskItemMapper.selectById(item.getParentItemId());
                if (latestParent == null || latestParent.getResolvedDirectoryId() == null) {
                    throw new IllegalStateException("parent directory item unresolved: " + item.getParentItemId());
                }
                if (latestParent.getResolvedCategoryId() == null) {
                    throw new IllegalStateException("parent category item unresolved: " + item.getParentItemId());
                }
                if (!snapshot.directoriesById().containsKey(latestParent.getResolvedDirectoryId())) {
                    throw new IllegalStateException("dcc directory missing: " + latestParent.getResolvedDirectoryId());
                }
                if (current.getSourceFileId() == null) {
                    throw new IllegalStateException("local folder source file missing: " + item.getNasPath());
                }
                DccControlledFileSubmitReqVO submitReqVO = new DccControlledFileSubmitReqVO();
                submitReqVO.setCategoryId(latestParent.getResolvedCategoryId());
                submitReqVO.setDirectoryId(latestParent.getResolvedDirectoryId());
                submitReqVO.setProductMasterId(null);
                submitReqVO.setDccProjectCodeId(task.getDccProjectCodeId());
                submitReqVO.setOriginalFileId(current.getSourceFileId());
                submitReqVO.setChangeType(DccControlledFileChangeTypeEnum.NEW.getCode());
                submitReqVO.setFileName(fileName);
                submitReqVO.setFileNumber(fileNumberOf(fileName, item.getNasPath()));
                submitReqVO.setVersionNo("V1.0");
                submitReqVO.setEffectiveDate(task.getEffectiveDate());
                submitReqVO.setRemark("Local folder import source: " + item.getNasPath());
                workflowService.submitControlledFileWithoutApproval(task.getOperatorUserId(), submitReqVO);

                current.setStatus(ITEM_STATUS_COMPLETED);
                current.setAttemptCount(incrementCount(current.getAttemptCount()));
                current.setLastAttemptAt(now);
                current.setCompletedAt(now);
                current.setFailureStage(null);
                current.setLastError(null);
                current.setPreviewDownloadOnly(previewKind == DccControlledFilePreviewKindEnum.DOWNLOAD_ONLY);
                taskItemMapper.updateById(current);
            });
        } catch (ServiceException exception) {
            markItemFailed(item.getId(), "submit", resolveThrowableMessage(exception));
        } catch (RuntimeException exception) {
            markItemFailed(item.getId(), "submit", resolveThrowableMessage(exception));
        }
    }

    private Long assignSelectedCategoryForFileItem(DccControlledFileNasTransferTaskItemDO item,
                                                   SelectedCategoryContext selectedCategory,
                                                   Snapshot snapshot) {
        Long categoryId = tx().execute(status -> {
            DccControlledFileNasTransferTaskItemDO latestParent = taskItemMapper.selectById(item.getParentItemId());
            if (latestParent == null || latestParent.getResolvedDirectoryId() == null) {
                throw new IllegalStateException("parent directory item unresolved: " + item.getParentItemId());
            }
            DccFileDirectoryDO directory = snapshot.directoriesById().get(latestParent.getResolvedDirectoryId());
            if (directory == null) {
                throw new IllegalStateException("dcc directory missing: " + latestParent.getResolvedDirectoryId());
            }
            if (!isDirectoryCoveredByBinding(directory.getId(), selectedCategory.bindingDirectoryId(), snapshot)) {
                throw new IllegalStateException("selected category directory binding does not cover target directory: "
                        + "categoryId=" + selectedCategory.category().getId()
                        + ", bindingDirectoryId=" + selectedCategory.bindingDirectoryId()
                        + ", targetDirectoryId=" + directory.getId());
            }
            if (!selectedCategory.category().getId().equals(latestParent.getResolvedCategoryId())) {
                latestParent.setResolvedCategoryId(selectedCategory.category().getId());
                latestParent.setCategoryOutcome(OUTCOME_REUSED);
                if (taskItemMapper.updateById(latestParent) != 1) {
                    throw new IllegalStateException("parent category item update failed: " + item.getParentItemId());
                }
            }
            return selectedCategory.category().getId();
        });
        if (categoryId == null) {
            throw new IllegalStateException("parent category item unresolved: " + item.getParentItemId());
        }
        return categoryId;
    }

    private void finalizeTask(Long taskId) {
        DccControlledFileNasTransferTaskDO current = taskMapper.selectById(taskId);
        if (current == null) {
            return;
        }
        if (!isLocalFolderTask(current) && !isOriginalPathSyncTask(current)) {
            snapshotCaptureService.completeSnapshotForTask(taskId);
        }
        current.setStatus(TASK_STATUS_COMPLETED);
        current.setCompletedAt(LocalDateTime.now());
        current.setNextCheckAt(null);
        DccControlledFileNasTransferRespVO response = buildTaskResponse(current);
        refreshFailureReport(current, response);
        taskMapper.updateById(current);
    }

    private boolean isTaskCancelling(Long taskId) {
        DccControlledFileNasTransferTaskDO current = taskMapper.selectById(taskId);
        return current != null && TASK_STATUS_CANCELLING.equals(current.getStatus());
    }

    private void markTaskCancelled(Long taskId, String reason) {
        DccControlledFileNasTransferTaskDO current = taskMapper.selectById(taskId);
        if (current == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        taskItemMapper.cancelWaitingItemsByTaskId(taskId, now);
        current.setStatus(TASK_STATUS_CANCELLED);
        current.setCompletedAt(now);
        current.setNextCheckAt(null);
        current.setLastFailureMessage(fitDatabaseErrorMessage(reason));
        DccControlledFileNasTransferRespVO response = buildTaskResponse(current);
        refreshFailureReport(current, response);
        taskMapper.updateById(current);
    }

    private void markTaskFailed(Long taskId, String reason) {
        DccControlledFileNasTransferTaskDO current = taskMapper.selectById(taskId);
        if (current == null) {
            return;
        }
        current.setStatus(TASK_STATUS_FAILED);
        current.setCompletedAt(LocalDateTime.now());
        current.setNextCheckAt(null);
        current.setLastFailureMessage(fitDatabaseErrorMessage(reason));
        DccControlledFileNasTransferRespVO response = buildTaskResponse(current);
        refreshFailureReport(current, response);
        taskMapper.updateById(current);
    }

    private void refreshFailureReport(DccControlledFileNasTransferTaskDO task,
                                      DccControlledFileNasTransferRespVO response) {
        if (response.getFailures().isEmpty()) {
            task.setFailureReportPath(null);
            task.setFailureReportGeneratedAt(null);
            task.setFailureReportError(null);
            return;
        }
        try {
            DccControlledFileNasTransferFailureReportService.FailureReport report =
                    failureReportService.write(toReqVO(task), response);
            task.setFailureReportPath(report.path());
            task.setFailureReportGeneratedAt(report.generatedAt());
            task.setFailureReportError(null);
        } catch (Exception exception) {
            task.setFailureReportPath(null);
            task.setFailureReportGeneratedAt(null);
            task.setFailureReportError(fitDatabaseErrorMessage(resolveThrowableMessage(exception)));
        }
    }

    private void markItemFailed(Long itemId, String stage, String reason) {
        LocalDateTime now = LocalDateTime.now();
        tx().executeWithoutResult(status -> {
            DccControlledFileNasTransferTaskItemDO item = taskItemMapper.selectById(itemId);
            if (item == null) {
                return;
            }
            item.setStatus(ITEM_STATUS_FAILED);
            item.setAttemptCount(incrementCount(item.getAttemptCount()));
            item.setFailureStage(stage);
            item.setLastError(fitDatabaseErrorMessage(reason));
            item.setLastAttemptAt(now);
            item.setCompletedAt(now);
            taskItemMapper.updateById(item);
        });
    }

    private DccControlledFileNasTransferRespVO buildTaskResponse(DccControlledFileNasTransferTaskDO task) {
        DccControlledFileNasTransferRespVO response = new DccControlledFileNasTransferRespVO();
        response.setTaskId(task.getId());
        response.setStatus(StrUtil.blankToDefault(task.getStatus(), TASK_STATUS_WAITING));
        response.setSourceType(sourceTypeOf(task));
        response.setSelectedNasPaths(JsonUtils.parseArray(
                StrUtil.blankToDefault(task.getSelectedNasPathsJson(), "[]"), String.class));
        response.setExpectedFileCount(defaultLong(task.getExpectedFileCount()));
        response.setExpectedTotalBytes(defaultLong(task.getExpectedTotalBytes()));
        response.setUploadedFileCount(defaultLong(task.getUploadedFileCount()));
        response.setUploadedTotalBytes(defaultLong(task.getUploadedTotalBytes()));
        response.setUploadCompletedAt(task.getUploadCompletedAt() == null ? null : task.getUploadCompletedAt().toString());
        response.setLastFailureMessage(task.getLastFailureMessage());
        response.setCompletedAt(task.getCompletedAt() == null ? null : task.getCompletedAt().toString());
        response.setFailureReportPath(task.getFailureReportPath());
        response.setFailureReportGeneratedAt(task.getFailureReportGeneratedAt());
        response.setFailureReportError(task.getFailureReportError());

        Long taskId = task.getId();
        response.setCreatedDirectoryCount(toIntegerCount(
                taskItemMapper.selectCountByTaskIdAndItemTypeAndDirectoryOutcome(taskId, ITEM_TYPE_DIRECTORY, OUTCOME_CREATED),
                "createdDirectoryCount"));
        response.setReusedDirectoryCount(toIntegerCount(
                taskItemMapper.selectCountByTaskIdAndItemTypeAndDirectoryOutcome(taskId, ITEM_TYPE_DIRECTORY, OUTCOME_REUSED),
                "reusedDirectoryCount"));
        response.setCreatedCategoryCount(toIntegerCount(
                taskItemMapper.selectCountByTaskIdAndItemTypeAndCategoryOutcome(taskId, ITEM_TYPE_DIRECTORY, OUTCOME_CREATED),
                "createdCategoryCount"));
        response.setReusedCategoryCount(toIntegerCount(
                taskItemMapper.selectCountByTaskIdAndItemTypeAndCategoryOutcome(taskId, ITEM_TYPE_DIRECTORY, OUTCOME_REUSED),
                "reusedCategoryCount"));
        response.setCreatedFileCount(toIntegerCount(
                taskItemMapper.selectCompletedFileCountByTaskId(taskId), "createdFileCount"));
        response.setSkippedPreviewOnlyCount(toIntegerCount(
                taskItemMapper.selectPreviewDownloadOnlyCompletedFileCountByTaskId(taskId), "skippedPreviewOnlyCount"));
        response.setRemainingPendingCount(toIntegerCount(
                taskItemMapper.selectPendingItemCountByTaskId(taskId), "remainingPendingCount"));

        List<DccControlledFileNasTransferTaskItemDO> failedItems = taskItemMapper.selectFailedItemsByTaskId(taskId);
        for (DccControlledFileNasTransferTaskItemDO item : failedItems) {
            DccControlledFileNasTransferRespVO.FailureItem failureItem =
                    new DccControlledFileNasTransferRespVO.FailureItem();
            failureItem.setNasPath(item.getNasPath());
            failureItem.setStage(StrUtil.blankToDefault(item.getFailureStage(), "task"));
            failureItem.setReason(StrUtil.blankToDefault(item.getLastError(), "unknown error"));
            response.getFailures().add(failureItem);
        }
        response.setFailedFileCount(toIntegerCount(failedItems.size(), "failedFileCount"));
        return response;
    }

    private boolean isLocalFolderTask(DccControlledFileNasTransferTaskDO task) {
        return SOURCE_TYPE_LOCAL_FOLDER.equals(sourceTypeOf(task));
    }

    private boolean isNasUncontrolledImportTask(DccControlledFileNasTransferTaskDO task) {
        return SOURCE_TYPE_NAS_UNCONTROLLED_IMPORT.equals(sourceTypeOf(task));
    }

    private boolean isOriginalPathSyncTask(DccControlledFileNasTransferTaskDO task) {
        return SOURCE_TYPE_NAS_ORIGINAL_PATH_SYNC.equals(sourceTypeOf(task));
    }

    private String sourceTypeOf(DccControlledFileNasTransferTaskDO task) {
        return StrUtil.blankToDefault(task.getSourceType(), SOURCE_TYPE_NAS);
    }

    private Integer toIntegerCount(long value, String fieldName) {
        try {
            return Math.toIntExact(value);
        } catch (ArithmeticException exception) {
            throw new IllegalStateException("nas transfer " + fieldName + " exceeds integer range: " + value,
                    exception);
        }
    }

    private DccControlledFileNasTransferTaskDO requireOwnedTask(Long userId, Long taskId) {
        DccControlledFileNasTransferTaskDO task = taskMapper.selectById(taskId);
        if (task == null || !Objects.equals(task.getOperatorUserId(), userId)) {
            throw new IllegalStateException("nas transfer task not found: " + taskId);
        }
        return task;
    }

    private DccControlledFileNasTransferReqVO toReqVO(DccControlledFileNasTransferTaskDO task) {
        DccControlledFileNasTransferReqVO reqVO = new DccControlledFileNasTransferReqVO();
        reqVO.setSelectedNasPaths(JsonUtils.parseArray(
                StrUtil.blankToDefault(task.getSelectedNasPathsJson(), "[]"), String.class));
        reqVO.setTemplateCategoryId(task.getTemplateCategoryId());
        reqVO.setDccProjectCodeId(task.getDccProjectCodeId());
        reqVO.setProductMasterId(null);
        reqVO.setEffectiveDate(task.getEffectiveDate());
        return reqVO;
    }

    private DccProjectCodeDO resolveRequiredProjectCode(Long projectCodeId) {
        if (projectCodeId == null) {
            throw exception(CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING);
        }
        DccProjectCodeDO projectCode = projectCodeMapper.selectById(projectCodeId);
        if (projectCode == null) {
            throw exception(PROJECT_CODE_NOT_EXISTS);
        }
        if (!DccProjectCodeStatusConstants.ENABLE.equals(projectCode.getStatus())) {
            throw exception(PROJECT_CODE_DISABLED);
        }
        if (StrUtil.isBlank(projectCode.getProjectCode()) || StrUtil.isBlank(projectCode.getProjectName())) {
            throw exception(CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING);
        }
        return projectCode;
    }

    private DccFileCategoryDO requireSelectedCategory(Long selectedCategoryId) {
        DccFileCategoryDO category = categoryMapper.selectById(selectedCategoryId);
        if (category == null || !Boolean.TRUE.equals(category.getActive())) {
            throw new IllegalStateException("selected category missing or inactive: " + selectedCategoryId);
        }
        return category;
    }

    private SelectedCategoryContext requireSelectedCategoryContext(Long selectedCategoryId) {
        DccFileCategoryDO category = requireSelectedCategory(selectedCategoryId);
        DccCategoryDirectoryBindingDO binding = categoryDirectoryBindingMapper.selectActiveByCategoryId(selectedCategoryId);
        if (binding != null && binding.getDirectoryId() != null) {
            return new SelectedCategoryContext(category, binding.getDirectoryId(), false);
        }
        DccFileDirectoryDO unclassifiedDirectory =
                DccUploadDirectoryResolver.resolveUnclassifiedUploadDirectory(directoryMapper.selectEnabledList());
        return new SelectedCategoryContext(category, unclassifiedDirectory.getId(), true);
    }

    private SelectedCategoryContext requireSelectedCategoryContext(Long selectedCategoryId, Snapshot snapshot) {
        DccFileCategoryDO category = requireSelectedCategory(selectedCategoryId);
        Long bindingDirectoryId = snapshot.categoryBindingDirectoryId().get(selectedCategoryId);
        boolean unclassifiedDirectory = false;
        if (bindingDirectoryId == null) {
            DccFileDirectoryDO directory = DccUploadDirectoryResolver.resolveUnclassifiedUploadDirectory(
                    new ArrayList<>(snapshot.directoriesById().values()));
            bindingDirectoryId = directory.getId();
            unclassifiedDirectory = true;
        }
        DccFileDirectoryDO bindingDirectory = snapshot.directoriesById().get(bindingDirectoryId);
        if (bindingDirectory == null || !Boolean.TRUE.equals(bindingDirectory.getActive())) {
            throw new IllegalStateException("selected category bound directory missing or inactive: "
                    + bindingDirectoryId);
        }
        return new SelectedCategoryContext(category, bindingDirectoryId, unclassifiedDirectory);
    }

    private boolean isDirectoryCoveredByBinding(Long directoryId, Long bindingDirectoryId, Snapshot snapshot) {
        Long currentDirectoryId = directoryId;
        while (currentDirectoryId != null) {
            if (currentDirectoryId.equals(bindingDirectoryId)) {
                return true;
            }
            DccFileDirectoryDO currentDirectory = snapshot.directoriesById().get(currentDirectoryId);
            if (currentDirectory == null) {
                return false;
            }
            currentDirectoryId = currentDirectory.getParentId();
        }
        return false;
    }

    private DirectoryResolution resolveDirectoryForItem(DccControlledFileNasTransferTaskItemDO item,
                                                        Snapshot snapshot) {
        return resolveDirectoryForItem(item, snapshot, null, "Created from NAS transfer task");
    }

    private DirectoryResolution resolveLocalFolderDirectoryForItem(DccControlledFileNasTransferTaskItemDO item,
                                                                   SelectedCategoryContext selectedCategory,
                                                                   Snapshot snapshot) {
        DccFileDirectoryDO bindingDirectory = snapshot.directoriesById().get(selectedCategory.bindingDirectoryId());
        if (bindingDirectory == null || !Boolean.TRUE.equals(bindingDirectory.getActive())) {
            throw new IllegalStateException("selected category bound directory missing or inactive: "
                    + selectedCategory.bindingDirectoryId());
        }
        if (item.getResolvedDirectoryId() == null && item.getParentItemId() == null
                && Objects.equals(bindingDirectory.getName(), item.getItemName())) {
            return new DirectoryResolution(bindingDirectory, OUTCOME_REUSED);
        }
        return resolveDirectoryForItem(item, snapshot, selectedCategory.bindingDirectoryId(),
                "Created from local folder import task");
    }

    private DirectoryResolution resolveDirectoryForItem(DccControlledFileNasTransferTaskItemDO item,
                                                        Snapshot snapshot,
                                                        Long rootParentDirectoryId,
                                                        String createdRemark) {
        if (item.getResolvedDirectoryId() != null) {
            DccFileDirectoryDO existing = snapshot.directoriesById().get(item.getResolvedDirectoryId());
            if (existing != null) {
                return new DirectoryResolution(existing,
                        StrUtil.blankToDefault(item.getDirectoryOutcome(), OUTCOME_REUSED));
            }
        }
        Long parentDirectoryId = rootParentDirectoryId;
        if (item.getParentItemId() != null) {
            DccControlledFileNasTransferTaskItemDO parentItem = taskItemMapper.selectById(item.getParentItemId());
            if (parentItem == null || parentItem.getResolvedDirectoryId() == null) {
                throw new IllegalStateException("parent directory item unresolved: " + item.getParentItemId());
            }
            parentDirectoryId = parentItem.getResolvedDirectoryId();
        }
        List<DccFileDirectoryDO> candidates = snapshot.directoriesByParentAndName()
                .getOrDefault(directoryKey(parentDirectoryId, item.getItemName()), List.of());
        if (candidates.size() > 1) {
            throw new IllegalStateException("duplicate dcc directory: " + item.getNasPath());
        }
        if (candidates.size() == 1) {
            return new DirectoryResolution(candidates.get(0), OUTCOME_REUSED);
        }
        DccFileDirectoryDO created = DccFileDirectoryDO.builder()
                .parentId(parentDirectoryId)
                .code(DIRECTORY_CODE_PREFIX + sha1(item.getNasPath()))
                .name(item.getItemName())
                .active(Boolean.TRUE)
                .sort(snapshot.nextDirectorySort(parentDirectoryId))
                .remark(createdRemark)
                .accessRuleManuallyBound(Boolean.FALSE)
                .build();
        directoryMapper.insert(created);
        cloneDirectoryAccessRules(parentDirectoryId, created.getId(), snapshot);
        snapshot.addDirectory(created);
        return new DirectoryResolution(created, OUTCOME_CREATED);
    }

    private void cloneDirectoryAccessRules(Long parentId, Long newDirectoryId, Snapshot snapshot) {
        if (parentId == null) {
            return;
        }
        List<DccDirectoryAccessRuleDO> parentRules = snapshot.directoryAccessRulesByDirectoryId()
                .getOrDefault(parentId, List.of());
        for (DccDirectoryAccessRuleDO parentRule : parentRules) {
            DccDirectoryAccessRuleDO cloned = DccDirectoryAccessRuleDO.builder()
                    .directoryId(newDirectoryId)
                    .subjectType(parentRule.getSubjectType())
                    .subjectId(parentRule.getSubjectId())
                    .canQuery(parentRule.getCanQuery())
                    .canPreview(parentRule.getCanPreview())
                    .canDownload(parentRule.getCanDownload())
                    .active(parentRule.getActive())
                    .changeReason(parentRule.getChangeReason())
                    .build();
            directoryAccessRuleMapper.insert(cloned);
            snapshot.addDirectoryAccessRule(cloned);
        }
    }

    private List<String> collapseSelectedRoots(List<String> selectedNasPaths) {
        List<String> normalized = selectedNasPaths.stream()
                .map(this::normalizePath)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .sorted(Comparator.comparingInt(String::length))
                .toList();
        List<String> collapsed = new ArrayList<>();
        for (String candidate : normalized) {
            boolean covered = collapsed.stream()
                    .anyMatch(parent -> candidate.equals(parent) || candidate.startsWith(parent + "/"));
            if (!covered) {
                collapsed.add(candidate);
            }
        }
        return collapsed;
    }

    private String normalizePath(String rawPath) {
        String normalized = StrUtil.trimToEmpty(rawPath).replace("\\", "/");
        List<String> parts = new ArrayList<>();
        for (String token : normalized.split("/")) {
            String clean = StrUtil.trimToEmpty(token);
            if (StrUtil.isBlank(clean) || ".".equals(clean)) {
                continue;
            }
            if ("..".equals(clean)) {
                if (!parts.isEmpty()) {
                    parts.remove(parts.size() - 1);
                }
                continue;
            }
            parts.add(clean);
        }
        return String.join("/", parts);
    }

    private String lastPathSegment(String path) {
        int index = StrUtil.nullToEmpty(path).lastIndexOf('/');
        return index >= 0 ? path.substring(index + 1) : path;
    }

    private String fileNumberOf(String fileName, String nasPath) {
        String cleanName = StrUtil.nullToEmpty(fileName).trim();
        int index = cleanName.lastIndexOf('.');
        String stem = index > 0 ? cleanName.substring(0, index) : cleanName;
        if (stem.length() <= FILE_NUMBER_MAX_LENGTH) {
            return stem;
        }
        String suffix = "-" + sha1(StrUtil.blankToDefault(nasPath, cleanName))
                .substring(0, FILE_NUMBER_HASH_LENGTH);
        int prefixLength = FILE_NUMBER_MAX_LENGTH - suffix.length();
        return stem.substring(0, prefixLength).trim() + suffix;
    }

    private String directoryKey(Long parentId, String name) {
        return String.valueOf(parentId) + "::" + StrUtil.nullToEmpty(name).trim();
    }

    private String categoryKey(Long directoryId, String name) {
        return String.valueOf(directoryId) + "::" + StrUtil.nullToEmpty(name).trim();
    }

    private String categoryCodeKey(String code) {
        return StrUtil.nullToEmpty(code).trim();
    }

    private String resolveThrowableMessage(Throwable throwable) {
        return StrUtil.blankToDefault(throwable.getMessage(), throwable.getClass().getSimpleName());
    }

    private String fitDatabaseErrorMessage(String message) {
        String normalizedMessage = StrUtil.blankToDefault(message, "unknown error");
        if (normalizedMessage.length() <= DATABASE_ERROR_MESSAGE_MAX_LENGTH) {
            return normalizedMessage;
        }
        int contentLength = DATABASE_ERROR_MESSAGE_MAX_LENGTH - DATABASE_ERROR_MESSAGE_TRUNCATED_SUFFIX.length();
        return normalizedMessage.substring(0, contentLength) + DATABASE_ERROR_MESSAGE_TRUNCATED_SUFFIX;
    }

    private String sha1(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8))).toUpperCase();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-1 unavailable", ex);
        }
    }

    private int incrementCount(Integer count) {
        return count == null ? 1 : count + 1;
    }

    private LocalDateTime nextScheduledCheckTime() {
        return LocalDateTime.now().plusSeconds(TASK_RETRY_DELAY_SECONDS);
    }

    private TransactionTemplate tx() {
        return new TransactionTemplate(transactionManager);
    }

    private record PendingChildItem(String itemType, String nasPath, String itemName) {
    }

    private record ValidatedLocalFolderPath(String relativePath, String fileName) {
    }

    private record ValidatedLocalFolderChunk(String relativePath, String fileName, Long fileSize,
                                             Integer chunkIndex, Integer totalChunks, Long chunkSize,
                                             String chunkSha256) {
    }

    private record StoredChunkFile(Path tempPath) {
    }

    private record LocalFolderFileEntry(String relativePath, String fileName, Long sourceFileId, Long fileSize) {
    }

    private record SelectedUncontrolledImportFile(Long auditFileId,
                                                  String sourceSignature,
                                                  String localRelativePath) {
    }

    private record PreparedUncontrolledImportFile(SelectedUncontrolledImportFile selectedFile,
                                                  DccNasControlAuditFileDO auditFile) {
    }

    private record SelectedOriginalPathSyncFile(Long auditFileId,
                                                String sourceSignature) {
    }

    private record PreparedOriginalPathSyncFile(DccNasControlAuditFileDO auditFile) {
    }

    private record DirectoryResolution(DccFileDirectoryDO directory, String outcome) {
    }

    private record SelectedCategoryContext(DccFileCategoryDO category, Long bindingDirectoryId,
                                           boolean unclassifiedDirectory) {

        Long nasRootParentDirectoryId() {
            return unclassifiedDirectory ? bindingDirectoryId : null;
        }
    }

    static final class TaskRuntime {
        private final Set<String> knownNasPaths;

        private TaskRuntime(Set<String> knownNasPaths) {
            this.knownNasPaths = knownNasPaths;
        }

        static TaskRuntime fromItems(List<DccControlledFileNasTransferTaskItemDO> items) {
            return new TaskRuntime(items.stream()
                    .map(DccControlledFileNasTransferTaskItemDO::getNasPath)
                    .filter(StrUtil::isNotBlank)
                    .collect(Collectors.toCollection(HashSet::new)));
        }

        Set<String> knownNasPaths() {
            return knownNasPaths;
        }
    }

    static final class Snapshot {
        private final Map<String, List<DccFileDirectoryDO>> directoriesByParentAndName = new LinkedHashMap<>();
        private final Map<Long, DccFileDirectoryDO> directoriesById = new LinkedHashMap<>();
        private final Map<Long, List<DccDirectoryAccessRuleDO>> directoryAccessRulesByDirectoryId = new LinkedHashMap<>();
        private final Map<String, List<DccFileCategoryDO>> categoriesByDirectoryAndName = new LinkedHashMap<>();
        private final Map<Long, DccFileCategoryDO> categoriesById = new LinkedHashMap<>();
        private final Map<String, List<DccFileCategoryDO>> categoriesByCode = new LinkedHashMap<>();
        private final Map<Long, Integer> nextDirectorySortByParent = new HashMap<>();
        private final Map<Long, Long> categoryBindingDirectoryId = new HashMap<>();
        private final Map<Long, List<DccFileCategoryPermissionRuleDO>> permissionRulesByCategoryId = new LinkedHashMap<>();
        private final Map<Long, List<DccFileCategoryDistributionRuleDO>> distributionRulesByCategoryId = new LinkedHashMap<>();
        private final Map<Long, List<DccFileCategoryTrainingRuleDO>> trainingRulesByCategoryId = new LinkedHashMap<>();
        private final Map<Long, List<DccCategoryApprovalRouteDO>> routesByCategoryId = new LinkedHashMap<>();
        private final Map<Long, List<DccCategoryApprovalRouteNodeDO>> routeNodesByRouteId = new LinkedHashMap<>();
        private int nextCategorySort;

        static Snapshot load(List<DccFileDirectoryDO> directories,
                             List<DccDirectoryAccessRuleDO> directoryAccessRules,
                             List<DccFileCategoryDO> categories,
                             List<DccCategoryDirectoryBindingDO> bindings,
                             List<DccFileCategoryPermissionRuleDO> permissionRules,
                             List<DccFileCategoryDistributionRuleDO> distributionRules,
                             List<DccFileCategoryTrainingRuleDO> trainingRules,
                             List<DccCategoryApprovalRouteDO> routes,
                             List<DccCategoryApprovalRouteNodeDO> routeNodes) {
            Snapshot snapshot = new Snapshot();
            for (DccFileDirectoryDO directory : directories) {
                snapshot.addDirectory(directory);
            }
            for (DccDirectoryAccessRuleDO rule : directoryAccessRules) {
                snapshot.addDirectoryAccessRule(rule);
            }
            snapshot.nextCategorySort = categories.stream()
                    .map(DccFileCategoryDO::getSort)
                    .filter(Objects::nonNull)
                    .max(Integer::compareTo)
                    .orElse(0);
            Map<Long, Long> bindingMap = bindings.stream()
                    .filter(item -> Boolean.TRUE.equals(item.getActive()))
                    .collect(Collectors.toMap(DccCategoryDirectoryBindingDO::getCategoryId,
                            DccCategoryDirectoryBindingDO::getDirectoryId, (left, right) -> left));
            snapshot.categoryBindingDirectoryId.putAll(bindingMap);
            for (DccFileCategoryDO category : categories) {
                Long directoryId = bindingMap.get(category.getId());
                snapshot.categoriesById.put(category.getId(), category);
                snapshot.categoriesByCode
                        .computeIfAbsent(snapshot.categoryCodeKey(category.getCode()), key -> new ArrayList<>())
                        .add(category);
                if (directoryId == null) {
                    continue;
                }
                snapshot.categoriesByDirectoryAndName
                        .computeIfAbsent(snapshot.categoryKey(directoryId, category.getName()), key -> new ArrayList<>())
                        .add(category);
            }
            for (DccFileCategoryPermissionRuleDO rule : permissionRules) {
                snapshot.permissionRulesByCategoryId.computeIfAbsent(rule.getCategoryId(), key -> new ArrayList<>()).add(rule);
            }
            for (DccFileCategoryDistributionRuleDO rule : distributionRules) {
                snapshot.distributionRulesByCategoryId.computeIfAbsent(rule.getCategoryId(), key -> new ArrayList<>()).add(rule);
            }
            for (DccFileCategoryTrainingRuleDO rule : trainingRules) {
                snapshot.trainingRulesByCategoryId.computeIfAbsent(rule.getCategoryId(), key -> new ArrayList<>()).add(rule);
            }
            for (DccCategoryApprovalRouteDO route : routes) {
                snapshot.routesByCategoryId.computeIfAbsent(route.getCategoryId(), key -> new ArrayList<>()).add(route);
            }
            for (DccCategoryApprovalRouteNodeDO node : routeNodes) {
                snapshot.routeNodesByRouteId.computeIfAbsent(node.getRouteId(), key -> new ArrayList<>()).add(node);
            }
            return snapshot;
        }

        void addDirectory(DccFileDirectoryDO directory) {
            directoriesById.put(directory.getId(), directory);
            directoriesByParentAndName
                    .computeIfAbsent(directoryKey(directory.getParentId(), directory.getName()), key -> new ArrayList<>())
                    .add(directory);
            int nextSort = directory.getSort() == null ? 0 : directory.getSort();
            nextDirectorySortByParent.merge(directory.getParentId(), nextSort, Math::max);
        }

        void addDirectoryAccessRule(DccDirectoryAccessRuleDO rule) {
            directoryAccessRulesByDirectoryId.computeIfAbsent(rule.getDirectoryId(), key -> new ArrayList<>()).add(rule);
        }

        void addCategory(DccFileCategoryDO category, DccCategoryDirectoryBindingDO binding) {
            if (!categoriesById.containsKey(category.getId())) {
                categoriesById.put(category.getId(), category);
                categoriesByCode
                        .computeIfAbsent(categoryCodeKey(category.getCode()), key -> new ArrayList<>())
                        .add(category);
            }
            categoryBindingDirectoryId.put(category.getId(), binding.getDirectoryId());
            categoriesByDirectoryAndName
                    .computeIfAbsent(categoryKey(binding.getDirectoryId(), category.getName()), key -> new ArrayList<>())
                    .add(category);
        }

        int nextDirectorySort(Long parentId) {
            int next = nextDirectorySortByParent.getOrDefault(parentId, 0) + 1;
            nextDirectorySortByParent.put(parentId, next);
            return next;
        }

        int nextCategorySort() {
            nextCategorySort += 1;
            return nextCategorySort;
        }

        Map<String, List<DccFileDirectoryDO>> directoriesByParentAndName() {
            return directoriesByParentAndName;
        }

        Map<Long, DccFileDirectoryDO> directoriesById() {
            return directoriesById;
        }

        Map<Long, List<DccDirectoryAccessRuleDO>> directoryAccessRulesByDirectoryId() {
            return directoryAccessRulesByDirectoryId;
        }

        Map<String, List<DccFileCategoryDO>> categoriesByDirectoryAndName() {
            return categoriesByDirectoryAndName;
        }

        Map<String, List<DccFileCategoryDO>> categoriesByCode() {
            return categoriesByCode;
        }

        Map<Long, Long> categoryBindingDirectoryId() {
            return categoryBindingDirectoryId;
        }

        Map<Long, List<DccFileCategoryPermissionRuleDO>> permissionRulesByCategoryId() {
            return permissionRulesByCategoryId;
        }

        Map<Long, List<DccFileCategoryDistributionRuleDO>> distributionRulesByCategoryId() {
            return distributionRulesByCategoryId;
        }

        Map<Long, List<DccFileCategoryTrainingRuleDO>> trainingRulesByCategoryId() {
            return trainingRulesByCategoryId;
        }

        Map<Long, List<DccCategoryApprovalRouteDO>> routesByCategoryId() {
            return routesByCategoryId;
        }

        Map<Long, List<DccCategoryApprovalRouteNodeDO>> routeNodesByRouteId() {
            return routeNodesByRouteId;
        }

        private String directoryKey(Long parentId, String name) {
            return String.valueOf(parentId) + "::" + StrUtil.nullToEmpty(name).trim();
        }

        private String categoryKey(Long directoryId, String name) {
            return String.valueOf(directoryId) + "::" + StrUtil.nullToEmpty(name).trim();
        }

        private String categoryCodeKey(String code) {
            return StrUtil.nullToEmpty(code).trim();
        }
    }
}
