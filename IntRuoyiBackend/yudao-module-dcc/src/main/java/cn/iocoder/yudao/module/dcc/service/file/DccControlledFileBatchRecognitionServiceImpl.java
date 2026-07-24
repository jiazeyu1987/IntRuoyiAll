package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileBatchRecognitionCreateReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileBatchRecognitionTaskRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileProjectCodeRecognitionRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodeAssociatedFileAiCategoryRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccFileDirectoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileBatchRecognitionTaskDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRecognitionClaimDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRecognitionFailureSummaryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRecognitionRecordDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccFileDirectoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileBatchRecognitionTaskMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileRecognitionClaimMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileRecognitionRecordMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.service.projectcode.DccProjectCodeService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_CONFIG_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_CODE_ASSOCIATED_FILE_ALREADY_CATEGORIZED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_CODE_ASSOCIATED_FILE_CONCURRENT_MODIFICATION;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_CODE_ASSOCIATED_FILE_NOT_EXISTS;

@Service
@Validated
@Slf4j
public class DccControlledFileBatchRecognitionServiceImpl implements DccControlledFileBatchRecognitionService {

    static final String TASK_STATUS_WAITING = "WAITING";
    static final String TASK_STATUS_RUNNING = "RUNNING";
    static final String TASK_STATUS_COMPLETED = "COMPLETED";
    static final String TASK_STATUS_FAILED = "FAILED";
    static final String TASK_STATUS_STOPPED = "STOPPED";
    static final String SCOPE_CURRENT = "CURRENT";
    static final String SCOPE_GLOBAL = "GLOBAL";
    static final String RECOGNITION_TYPE_BASIC_INFO = "BASIC_INFO";
    static final String RECOGNITION_TYPE_FILE_CATEGORY = "FILE_CATEGORY";
    static final String RECOGNITION_TYPE_FILE_NUMBER = "FILE_NUMBER";
    static final String FILE_CATEGORY_RECOGNITION_VERSION = "file-category-v1";
    static final String FILE_NUMBER_RECOGNITION_VERSION = "file-number-from-project-code-or-name-contains-v2";
    static final String RECOGNITION_METHOD_BATCH_PROJECT_CODE = "BATCH_PROJECT_CODE";
    static final String RECOGNITION_METHOD_BATCH_FILE_CATEGORY = "BATCH_FILE_CATEGORY";
    static final String RECOGNITION_METHOD_BATCH_FILE_NUMBER = "BATCH_FILE_NUMBER_FROM_PROJECT_NAME";
    static final String FILE_CATEGORY_STATUS_UNCLASSIFIED = "UNCLASSIFIED";
    static final String FILE_CATEGORY_STATUS_AMBIGUOUS = "AMBIGUOUS";
    static final String FILE_CATEGORY_STATUS_CONFLICT = "CONFLICT";
    static final String EXISTING_RECORD_POLICY_SKIP_ALL_EXISTING = "SKIP_ALL_EXISTING";
    static final String EXISTING_RECORD_POLICY_RETRY_FAILED = "RETRY_FAILED";
    static final String EXISTING_RECORD_POLICY_OVERWRITE_ALL = "OVERWRITE_ALL";
    private static final String USER_STOPPED_REASON = "Stopped by user";
    private static final int MAX_LAST_FAILURE_MESSAGE_LENGTH = 2048;
    private static final int FAILURE_SUMMARY_LIMIT = 3;
    private static final String ACTIVE_TASK_UNIQUE_CONSTRAINT =
            "uk_dcc_batch_recognition_task_active_type";

    @Resource
    private DccControlledFileBatchRecognitionTaskMapper taskMapper;
    @Resource
    private DccControlledFileMapper controlledFileMapper;
    @Resource
    private DccControlledFileMasterMapper controlledFileMasterMapper;
    @Resource
    private DccFileDirectoryMapper directoryMapper;
    @Resource
    private DccControlledFileQueryService queryService;
    @Resource
    private DccControlledFileProjectCodeRecognitionService projectCodeRecognitionService;
    @Resource
    private DccProjectCodeMapper projectCodeMapper;
    @Resource
    private DccProjectCodeService projectCodeService;
    @Resource
    private DccControlledFileRecognitionRecordMapper recognitionRecordMapper;
    @Resource
    private DccControlledFileRecognitionClaimMapper recognitionClaimMapper;
    @Resource
    private PlatformTransactionManager transactionManager;
    @Resource
    private DccProjectCodeRecognitionProperties recognitionProperties;

    @Override
    public DccControlledFileBatchRecognitionTaskRespVO createTask(
            Long userId, DccControlledFileBatchRecognitionCreateReqVO reqVO) {
        String recognitionType = normalizeRecognitionType(reqVO.getRecognitionType());
        DccControlledFileBatchRecognitionTaskDO activeTask = taskMapper.selectActiveTask(recognitionType);
        if (activeTask != null) {
            return toRespVO(activeTask);
        }
        String scope = normalizeScope(reqVO.getScope());
        if (RECOGNITION_TYPE_FILE_CATEGORY.equals(recognitionType) && !SCOPE_GLOBAL.equals(scope)) {
            throw new IllegalStateException("FILE_CATEGORY batch recognition only supports GLOBAL scope");
        }
        String recognitionVersion = resolveRecognitionVersion(recognitionType);
        Long directoryId = resolveDirectoryId(scope, reqVO.getDirectoryId());
        String existingRecordPolicy = normalizeExistingRecordPolicy(reqVO);
        String directoryPathSnapshot = directoryId == null ? null : buildDirectoryPathSnapshot(directoryId);
        List<Long> candidateIds = listCandidateIds(userId, reqVO, recognitionType, scope, directoryId);
        int workerCountSnapshot = RECOGNITION_TYPE_FILE_NUMBER.equals(recognitionType)
                ? 1 : resolveWorkerCountSnapshot(reqVO.getWorkerCount());
        Long taskId;
        try {
            taskId = tx().execute(status -> {
                DccControlledFileBatchRecognitionTaskDO task = DccControlledFileBatchRecognitionTaskDO.builder()
                        .operatorUserId(userId)
                        .recognitionType(recognitionType)
                        .scopeType(scope)
                        .recognitionVersionSnapshot(recognitionVersion)
                        .directoryId(directoryId)
                        .directoryPathSnapshot(directoryPathSnapshot)
                        .keyword(normalizeKeyword(reqVO.getKeyword()))
                        .statusFilter(normalizeKeyword(reqVO.getStatus()))
                        .categoryId(reqVO.getCategoryId())
                        .overwriteExisting(EXISTING_RECORD_POLICY_OVERWRITE_ALL.equals(existingRecordPolicy))
                        .existingRecordPolicy(existingRecordPolicy)
                        .syncFileNameTitle(!Boolean.FALSE.equals(reqVO.getSyncFileNameTitle()))
                        .workerCount(workerCountSnapshot)
                        .candidateIdsJson(JsonUtils.toJsonString(candidateIds))
                        .status(candidateIds.isEmpty() ? TASK_STATUS_COMPLETED : TASK_STATUS_WAITING)
                        .totalCount((long) candidateIds.size())
                        .processedCount(0L)
                        .successCount(0L)
                        .failedCount(0L)
                        .skippedExistingCount(0L)
                        .unclassifiedCount(0L)
                        .ambiguousCount(0L)
                        .conflictCount(0L)
                        .remainingCount((long) candidateIds.size())
                        .completedAt(candidateIds.isEmpty() ? LocalDateTime.now() : null)
                        .build();
                taskMapper.insert(task);
                return task.getId();
            });
        } catch (DuplicateKeyException exception) {
            if (!isActiveTaskUniqueConflict(exception)) {
                throw exception;
            }
            DccControlledFileBatchRecognitionTaskDO concurrentActiveTask =
                    taskMapper.selectActiveTask(recognitionType);
            if (concurrentActiveTask == null) {
                throw exception;
            }
            return toRespVO(concurrentActiveTask);
        }
        if (!candidateIds.isEmpty()) {
            triggerTaskAsync(TenantContextHolder.getRequiredTenantId());
        }
        return getTask(userId, taskId);
    }

    private boolean isActiveTaskUniqueConflict(DuplicateKeyException exception) {
        Throwable current = exception;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains(ACTIVE_TASK_UNIQUE_CONSTRAINT)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    @Override
    public DccControlledFileBatchRecognitionTaskRespVO getLatestTask(Long userId, String recognitionType) {
        String normalizedRecognitionType = normalizeRecognitionType(recognitionType);
        DccControlledFileBatchRecognitionTaskDO task = taskMapper.selectActiveTask(normalizedRecognitionType);
        if (task == null) {
            task = RECOGNITION_TYPE_FILE_CATEGORY.equals(normalizedRecognitionType)
                    ? taskMapper.selectLatestTask(normalizedRecognitionType)
                    : taskMapper.selectLatestTask(userId, normalizedRecognitionType);
        }
        return task == null ? null : toRespVO(task);
    }

    @Override
    public DccControlledFileBatchRecognitionTaskRespVO getTask(Long userId, Long taskId) {
        DccControlledFileBatchRecognitionTaskDO task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalStateException("dcc batch recognition task not found: " + taskId);
        }
        return toRespVO(task);
    }

    @Override
    public DccControlledFileBatchRecognitionTaskRespVO stopTask(Long userId, Long taskId) {
        DccControlledFileBatchRecognitionTaskDO task = taskMapper.selectById(taskId);
        if (task == null || !Objects.equals(task.getOperatorUserId(), userId)) {
            throw new IllegalStateException("dcc batch recognition task not found: " + taskId);
        }
        if (isTerminalStatus(task.getStatus())) {
            return toRespVO(task);
        }
        taskMapper.stopActiveTask(taskId, userId, LocalDateTime.now(), USER_STOPPED_REASON);
        recognitionClaimMapper.releaseClaimsByTaskId(taskId);
        return getTask(userId, taskId);
    }

    @Override
    public void processWaitingTasks() {
        for (DccControlledFileBatchRecognitionTaskDO task : taskMapper.selectWaitingTasks()) {
            try {
                executeTask(task.getId());
            } catch (RuntimeException exception) {
                log.error("[processWaitingTasks][taskId({}) DCC batch recognition task execution failed]",
                        task.getId(), exception);
            }
        }
    }

    @Override
    public void recoverInterruptedTasksOnStartup() {
        int releasedTerminalClaimCount = recognitionClaimMapper.releaseClaimsOwnedByTerminalTasks();
        if (releasedTerminalClaimCount > 0) {
            log.info("[recoverInterruptedTasksOnStartup][releasedTerminalClaimCount({})]", releasedTerminalClaimCount);
        }
        List<DccControlledFileBatchRecognitionTaskDO> runningTasks = taskMapper.selectRunningTasks();
        for (DccControlledFileBatchRecognitionTaskDO task : runningTasks) {
            recognitionClaimMapper.releaseClaimsByTaskId(task.getId());
        }
        int recoveredCount = taskMapper.requeueRunningTasksOnStartup();
        if (recoveredCount > 0) {
            log.info("[recoverInterruptedTasksOnStartup][recoveredTaskCount({})]", recoveredCount);
        }
    }

    private void triggerTaskAsync(Long tenantId) {
        CompletableFuture.runAsync(() -> TenantUtils.execute(tenantId, () -> {
            try {
                processWaitingTasks();
            } catch (RuntimeException exception) {
                log.error("[triggerTaskAsync][tenantId({}) DCC batch recognition async execution failed]",
                        tenantId, exception);
            }
        }));
    }

    private void executeTask(Long taskId) {
        DccControlledFileBatchRecognitionTaskDO task = taskMapper.selectById(taskId);
        if (task == null || isTerminalStatus(task.getStatus())) {
            return;
        }
        if (taskMapper.claimWaitingTask(taskId, LocalDateTime.now()) == 0) {
            return;
        }
        try {
            List<Long> candidateIds = parseCandidateIds(task.getCandidateIdsJson());
            if (candidateIds.isEmpty()) {
                markTaskCompleted(taskId, task.getTotalCount(), 0L, 0L, 0L, 0L, 0L, 0L);
                return;
            }
            TaskProgress progress = TaskProgress.from(task);
            processCandidates(task, candidateIds, progress);
            if (isStopRequested(taskId)) {
                return;
            }
            TaskProgress.ProgressSnapshot snapshot = progress.snapshot();
            markTaskCompleted(taskId, snapshot.processedCount(), snapshot.successCount(),
                    snapshot.failedCount(), snapshot.skippedExistingCount(), snapshot.unclassifiedCount(),
                    snapshot.ambiguousCount(), snapshot.conflictCount());
        } catch (RuntimeException exception) {
            log.error("[executeTask][taskId({}) DCC batch recognition failed]", taskId, exception);
            markTaskFailed(taskId, resolveThrowableMessage(exception));
        }
    }

    private void processCandidates(DccControlledFileBatchRecognitionTaskDO task, List<Long> candidateIds,
                                   TaskProgress progress) {
        if (isFileNumberTask(task)) {
            processFileNumberCandidates(task, candidateIds, progress);
            return;
        }
        int workerCount = resolveWorkerCount(task.getWorkerCount(), candidateIds.size());
        if (workerCount == 1) {
            for (Long fileId : candidateIds) {
                if (isStopRequested(task.getId())) {
                    return;
                }
                CandidateOutcome candidateOutcome = processOneCandidate(task, fileId);
                updateProgressAfterCandidate(task, progress, candidateOutcome.outcome(),
                        candidateOutcome.lastFailureMessage());
            }
            return;
        }
        ExecutorService executor = Executors.newFixedThreadPool(workerCount);
        try {
            CompletionService<CandidateOutcome> completionService = new ExecutorCompletionService<>(executor);
            int submittedCount = 0;
            for (Long fileId : candidateIds) {
                if (isStopRequested(task.getId())) {
                    break;
                }
                submittedCount++;
                completionService.submit(() -> {
                    if (isStopRequested(task.getId())) {
                        return CandidateOutcome.skipped();
                    }
                    return processOneCandidate(task, fileId);
                });
            }
            for (int i = 0; i < submittedCount; i++) {
                CandidateOutcome candidateOutcome = takeCandidateOutcome(completionService);
                updateProgressAfterCandidate(task, progress, candidateOutcome.outcome(),
                        candidateOutcome.lastFailureMessage());
            }
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                executor.shutdownNow();
                throw new IllegalStateException("dcc batch recognition worker shutdown interrupted", exception);
            }
        }
    }

    private CandidateOutcome takeCandidateOutcome(CompletionService<CandidateOutcome> completionService) {
        try {
            Future<CandidateOutcome> future = completionService.take();
            return future.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("dcc batch recognition worker interrupted", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("dcc batch recognition worker failed", exception.getCause());
        }
    }

    private void processFileNumberCandidates(DccControlledFileBatchRecognitionTaskDO task, List<Long> candidateIds,
                                             TaskProgress progress) {
        List<FileNumberProjectCodeCandidate> projectCodeCandidates = buildFileNumberProjectCodeCandidates();
        for (List<Long> chunk : partition(candidateIds, 500)) {
            if (isStopRequested(task.getId())) {
                return;
            }
            List<DccControlledFileDO> files = controlledFileMapper.selectBatchIds(chunk);
            Map<Long, DccControlledFileDO> fileById = files.stream()
                    .filter(file -> file != null && file.getId() != null)
                    .collect(Collectors.toMap(DccControlledFileDO::getId, file -> file, (left, right) -> left));
            List<DccControlledFileDO> fileUpdates = new ArrayList<>();
            List<DccControlledFileMasterDO> masterUpdates = new ArrayList<>();
            List<CandidateOutcome> outcomes = new ArrayList<>();
            for (Long fileId : chunk) {
                DccControlledFileDO file = fileById.get(fileId);
                if (file == null) {
                    outcomes.add(new CandidateOutcome(ProgressOutcome.FAILED,
                            "controlled file not found: " + fileId));
                    continue;
                }
                if (shouldSkipExistingFileNumber(task, file)) {
                    outcomes.add(CandidateOutcome.skipped());
                    continue;
                }
                String projectNameKey = normalizeFileNameBase(file.getFileName());
                String fileNumber = resolveFileNumber(projectNameKey, projectCodeCandidates);
                fileUpdates.add(DccControlledFileDO.builder()
                        .id(file.getId())
                        .fileNumber(fileNumber)
                        .build());
                if (file.getMasterId() != null) {
                    masterUpdates.add(DccControlledFileMasterDO.builder()
                            .id(file.getMasterId())
                            .fileNumber(fileNumber)
                            .build());
                }
                outcomes.add(CandidateOutcome.success());
            }
            persistFileNumberUpdates(fileUpdates, masterUpdates);
            updateProgressAfterCandidates(task, progress, outcomes);
        }
    }

    private List<FileNumberProjectCodeCandidate> buildFileNumberProjectCodeCandidates() {
        Map<String, String> projectCodeByProjectName = new HashMap<>();
        List<FileNumberProjectCodeCandidate> candidates = new ArrayList<>();
        for (DccProjectCodeDO projectCode : projectCodeMapper.selectEnabledList()) {
            String projectName = StrUtil.trimToNull(projectCode.getProjectName());
            if (projectName == null) {
                throw new IllegalStateException("enabled DCC project code has blank project name: "
                        + projectCode.getId());
            }
            String targetProjectCode = StrUtil.trimToEmpty(projectCode.getProjectCode());
            String existingProjectCode = projectCodeByProjectName.putIfAbsent(projectName, targetProjectCode);
            if (existingProjectCode != null && !Objects.equals(existingProjectCode, targetProjectCode)) {
                throw new IllegalStateException("enabled DCC project name maps to multiple project codes: "
                        + projectName);
            }
            candidates.add(new FileNumberProjectCodeCandidate(projectName, targetProjectCode));
        }
        return candidates;
    }

    private String resolveFileNumber(String fileNameBase, List<FileNumberProjectCodeCandidate> candidates) {
        String matchedProjectCode = resolveProjectCodeContainsMatch(fileNameBase, candidates);
        if (matchedProjectCode != null) {
            return matchedProjectCode;
        }
        matchedProjectCode = resolveProjectNameContainsMatch(fileNameBase, candidates);
        return matchedProjectCode == null ? "" : matchedProjectCode;
    }

    private String resolveProjectCodeContainsMatch(String fileNameBase, List<FileNumberProjectCodeCandidate> candidates) {
        List<FileNumberProjectCodeMatch> matches = new ArrayList<>();
        String normalizedFileName = StrUtil.trimToEmpty(fileNameBase).toUpperCase(Locale.ROOT);
        for (FileNumberProjectCodeCandidate candidate : candidates) {
            String projectCode = StrUtil.trimToNull(candidate.projectCode());
            if (projectCode == null) {
                continue;
            }
            int index = normalizedFileName.indexOf(projectCode.toUpperCase(Locale.ROOT));
            if (index >= 0) {
                matches.add(new FileNumberProjectCodeMatch(candidate, index, projectCode.length()));
            }
        }
        if (matches.isEmpty()) {
            return null;
        }
        matches.sort((left, right) -> {
            int lengthCompare = Integer.compare(right.matchLength(), left.matchLength());
            if (lengthCompare != 0) {
                return lengthCompare;
            }
            int positionCompare = Integer.compare(left.position(), right.position());
            if (positionCompare != 0) {
                return positionCompare;
            }
            return left.candidate().projectCode().compareTo(right.candidate().projectCode());
        });
        return matches.get(0).candidate().projectCode();
    }

    private String resolveProjectNameContainsMatch(String fileNameBase, List<FileNumberProjectCodeCandidate> candidates) {
        List<FileNumberProjectCodeMatch> matches = new ArrayList<>();
        for (FileNumberProjectCodeCandidate candidate : candidates) {
            int index = fileNameBase.indexOf(candidate.projectName());
            if (index >= 0) {
                matches.add(new FileNumberProjectCodeMatch(candidate, index, candidate.projectName().length()));
            }
        }
        if (matches.isEmpty()) {
            return null;
        }
        matches.sort((left, right) -> {
            int lengthCompare = Integer.compare(right.matchLength(), left.matchLength());
            if (lengthCompare != 0) {
                return lengthCompare;
            }
            int positionCompare = Integer.compare(left.position(), right.position());
            if (positionCompare != 0) {
                return positionCompare;
            }
            return left.candidate().projectCode().compareTo(right.candidate().projectCode());
        });
        return matches.get(0).candidate().projectCode();
    }

    private record FileNumberProjectCodeCandidate(String projectName, String projectCode) {
    }

    private record FileNumberProjectCodeMatch(FileNumberProjectCodeCandidate candidate, int position, int matchLength) {
    }

    private List<List<Long>> partition(List<Long> source, int chunkSize) {
        if (source.isEmpty()) {
            return List.of();
        }
        List<List<Long>> partitions = new ArrayList<>();
        for (int fromIndex = 0; fromIndex < source.size(); fromIndex += chunkSize) {
            partitions.add(source.subList(fromIndex, Math.min(fromIndex + chunkSize, source.size())));
        }
        return partitions;
    }

    private boolean shouldSkipExistingFileNumber(DccControlledFileBatchRecognitionTaskDO task,
                                                 DccControlledFileDO file) {
        if (EXISTING_RECORD_POLICY_OVERWRITE_ALL.equals(resolveExistingRecordPolicy(task))) {
            return false;
        }
        return StrUtil.isNotBlank(file.getFileNumber());
    }

    private String normalizeFileNameBase(String fileName) {
        String normalized = StrUtil.trimToEmpty(fileName);
        int extensionIndex = normalized.lastIndexOf('.');
        if (extensionIndex > 0) {
            normalized = normalized.substring(0, extensionIndex);
        }
        return StrUtil.trimToEmpty(normalized);
    }

    private void persistFileNumberUpdates(List<DccControlledFileDO> fileUpdates,
                                          List<DccControlledFileMasterDO> masterUpdates) {
        if (fileUpdates.isEmpty() && masterUpdates.isEmpty()) {
            return;
        }
        tx().executeWithoutResult(status -> {
            if (!fileUpdates.isEmpty()
                    && !Boolean.TRUE.equals(controlledFileMapper.updateBatch(fileUpdates, 500))) {
                throw new IllegalStateException("DCC file-number batch update failed");
            }
            if (!masterUpdates.isEmpty()
                    && !Boolean.TRUE.equals(controlledFileMasterMapper.updateBatch(masterUpdates, 500))) {
                throw new IllegalStateException("DCC file master file-number batch update failed");
            }
        });
    }

    private CandidateOutcome processOneCandidate(DccControlledFileBatchRecognitionTaskDO task, Long fileId) {
        String lastFailureMessage = null;
        try {
            if (isStopRequested(task.getId())) {
                return CandidateOutcome.skipped();
            }
            DccControlledFileDO file = controlledFileMapper.selectById(fileId);
            if (file == null) {
                String failureMessage = "controlled file not found: " + fileId;
                recordBatchFailureIfMissing(task, fileId, failureMessage,
                        DccRecognitionFailureClassifier.STAGE_PRECONDITION,
                        DccRecognitionFailureClassifier.CODE_CONTROLLED_FILE_NOT_FOUND);
                return new CandidateOutcome(ProgressOutcome.FAILED, failureMessage);
            }
            DccControlledFileRecognitionRecordDO existingRecord = findExistingRecognition(file, task);
            if (shouldUseExistingRecognition(task, existingRecord)) {
                ProgressOutcome outcome = resolveExistingRecognitionOutcome(task, existingRecord);
                return new CandidateOutcome(outcome, resolveExistingFailureMessage(outcome, existingRecord));
            } else {
                if (!acquireBatchFileClaim(task, file)) {
                    return CandidateOutcome.skipped();
                }
                if (isFileCategoryTask(task)) {
                    return processFileCategoryCandidate(task, file);
                }
                DccControlledFileProjectCodeRecognitionRespVO recognitionResp =
                        projectCodeRecognitionService.recognizeProjectCode(
                                task.getOperatorUserId(), fileId, task.getId());
                return resolveRecognitionOutcome(task, fileId, recognitionResp);
            }
        } catch (RuntimeException exception) {
            lastFailureMessage = resolveThrowableMessage(exception);
            DccRecognitionFailureClassifier.FailureMetadata failureMetadata =
                    DccRecognitionFailureClassifier.classify(
                            exception,
                            DccRecognitionFailureClassifier.STAGE_BATCH_ORCHESTRATION,
                            DccRecognitionFailureClassifier.CODE_BATCH_CANDIDATE_FAILED);
            recordBatchFailureIfMissing(task, fileId, lastFailureMessage,
                    failureMetadata.stage(), failureMetadata.code());
            return new CandidateOutcome(ProgressOutcome.FAILED, lastFailureMessage);
        }
    }

    private CandidateOutcome processFileCategoryCandidate(DccControlledFileBatchRecognitionTaskDO task,
                                                          DccControlledFileDO file) {
        if (file.getDccProjectCodeId() == null) {
            String message = "controlled file has no DCC project code: " + file.getId();
            recordFileCategoryOutcome(task, file, null, ProgressOutcome.FAILED, message,
                    DccRecognitionFailureClassifier.STAGE_PRECONDITION,
                    DccRecognitionFailureClassifier.CODE_FILE_CATEGORY_PROJECT_CODE_MISSING);
            return new CandidateOutcome(ProgressOutcome.FAILED, message);
        }
        try {
            DccProjectCodeAssociatedFileAiCategoryRespVO response =
                    projectCodeService.classifyAssociatedFileByName(
                            task.getOperatorUserId(), file.getDccProjectCodeId(), file.getId());
            ProgressOutcome outcome = resolveFileCategoryOutcome(response);
            String message = response == null ? "DCC file category returned no response"
                    : response.getClassificationMessage();
            DccControlledFileDO classifiedFile = controlledFileMapper.selectById(file.getId());
            String failureStage = outcome == ProgressOutcome.FAILED
                    ? DccRecognitionFailureClassifier.STAGE_RESULT_VALIDATION : null;
            String failureCode = outcome == ProgressOutcome.FAILED
                    ? DccRecognitionFailureClassifier.CODE_FILE_CATEGORY_RESPONSE_INVALID : null;
            recordFileCategoryOutcome(task, classifiedFile == null ? file : classifiedFile,
                    response, outcome, message, failureStage, failureCode);
            return new CandidateOutcome(outcome,
                    outcome == ProgressOutcome.FAILED ? normalizeLastFailureMessage(message) : null);
        } catch (ServiceException exception) {
            if (!isFileCategoryConflict(exception)) {
                throw exception;
            }
            String message = resolveThrowableMessage(exception);
            recordFileCategoryOutcome(task, file, null, ProgressOutcome.CONFLICT, message, null, null);
            return new CandidateOutcome(ProgressOutcome.CONFLICT, message);
        } catch (RuntimeException exception) {
            String message = resolveThrowableMessage(exception);
            DccRecognitionFailureClassifier.FailureMetadata failureMetadata =
                    DccRecognitionFailureClassifier.classify(
                            exception,
                            DccRecognitionFailureClassifier.STAGE_AI_CLASSIFICATION,
                            DccRecognitionFailureClassifier.CODE_AI_REQUEST_FAILED);
            recordFileCategoryOutcome(task, file, null, ProgressOutcome.FAILED, message,
                    failureMetadata.stage(), failureMetadata.code());
            return new CandidateOutcome(ProgressOutcome.FAILED, message);
        }
    }

    private ProgressOutcome resolveFileCategoryOutcome(DccProjectCodeAssociatedFileAiCategoryRespVO response) {
        if (response == null) {
            return ProgressOutcome.FAILED;
        }
        String status = StrUtil.trimToEmpty(response.getClassificationStatus()).toUpperCase(Locale.ROOT);
        if (Boolean.TRUE.equals(response.getMatched()) || "MATCHED".equals(status)) {
            return ProgressOutcome.SUCCESS;
        }
        if (FILE_CATEGORY_STATUS_UNCLASSIFIED.equals(status)) {
            return ProgressOutcome.UNCLASSIFIED;
        }
        if (FILE_CATEGORY_STATUS_AMBIGUOUS.equals(status)) {
            return ProgressOutcome.AMBIGUOUS;
        }
        return ProgressOutcome.FAILED;
    }

    private void recordFileCategoryOutcome(DccControlledFileBatchRecognitionTaskDO task,
                                           DccControlledFileDO file,
                                           DccProjectCodeAssociatedFileAiCategoryRespVO response,
                                           ProgressOutcome outcome,
                                           String message,
                                           String failureStage,
                                           String failureCode) {
        recognitionRecordMapper.upsert(DccControlledFileRecognitionRecordDO.builder()
                .tenantId(file.getTenantId())
                .controlledFileId(file.getId())
                .recognitionScope(RECOGNITION_TYPE_FILE_CATEGORY)
                .recognitionMethod(RECOGNITION_METHOD_BATCH_FILE_CATEGORY)
                .recognitionVersion(recognitionVersionSnapshotOrCurrent(task))
                .status(recognitionStatus(outcome))
                .batchTaskId(task.getId())
                .failureStage(outcome == ProgressOutcome.FAILED ? failureStage : null)
                .failureCode(outcome == ProgressOutcome.FAILED ? failureCode : null)
                .failureMessage(outcome == ProgressOutcome.SUCCESS ? null : normalizeLastFailureMessage(message))
                .recognizedBy(task.getOperatorUserId())
                .recognizedTime(LocalDateTime.now())
                .sourceFileId(file.getSourceFileId())
                .fileTypeLevel1(file.getFileTypeLevel1())
                .fileTypeLevel2(response == null ? file.getFileTypeLevel2() : response.getTargetStage())
                .fileTypeLevel3(response == null ? file.getFileTypeLevel3() : response.getTargetFileType())
                .build());
    }

    private String recognitionStatus(ProgressOutcome outcome) {
        return switch (outcome) {
            case SUCCESS -> DccControlledFileProjectCodeRecognitionServiceImpl.RECOGNITION_STATUS_SUCCESS;
            case UNCLASSIFIED -> FILE_CATEGORY_STATUS_UNCLASSIFIED;
            case AMBIGUOUS -> FILE_CATEGORY_STATUS_AMBIGUOUS;
            case CONFLICT -> FILE_CATEGORY_STATUS_CONFLICT;
            case FAILED -> DccControlledFileProjectCodeRecognitionServiceImpl.RECOGNITION_STATUS_FAILED;
            case SKIPPED -> throw new IllegalStateException("skipped outcome must not be persisted");
        };
    }

    private boolean isFileCategoryConflict(ServiceException exception) {
        int code = exception.getCode();
        return code == PROJECT_CODE_ASSOCIATED_FILE_NOT_EXISTS.getCode()
                || code == PROJECT_CODE_ASSOCIATED_FILE_ALREADY_CATEGORIZED.getCode()
                || code == PROJECT_CODE_ASSOCIATED_FILE_CONCURRENT_MODIFICATION.getCode();
    }

    private CandidateOutcome resolveRecognitionOutcome(DccControlledFileBatchRecognitionTaskDO task, Long fileId,
                                                       DccControlledFileProjectCodeRecognitionRespVO recognitionResp) {
        String recognitionStatus = recognitionResp == null ? null : StrUtil.trimToNull(recognitionResp.getRecognitionStatus());
        if (isNonFailedRecognitionStatus(recognitionStatus)) {
            return CandidateOutcome.success();
        }
        String failureMessage = StrUtil.blankToDefault(recognitionStatus, "DCC project-code recognition returned no status");
        recordBatchFailureIfMissing(task, fileId, failureMessage,
                DccRecognitionFailureClassifier.STAGE_RESULT_VALIDATION,
                DccRecognitionFailureClassifier.CODE_INVALID_RESULT);
        return new CandidateOutcome(ProgressOutcome.FAILED, failureMessage);
    }

    private void recordBatchFailureIfMissing(DccControlledFileBatchRecognitionTaskDO task, Long fileId,
                                             String failureMessage, String failureStage, String failureCode) {
        String recognitionVersion = recognitionVersionSnapshotOrCurrent(task);
        DccControlledFileRecognitionRecordDO existingRecord =
                recognitionRecordMapper.selectLatestByFileAndVersion(
                        fileId,
                        recognitionScope(task),
                        recognitionVersion);
        if (existingRecord != null) {
            return;
        }
        DccControlledFileDO file = controlledFileMapper.selectById(fileId);
        Long tenantId = file == null ? TenantContextHolder.getTenantId() : file.getTenantId();
        recognitionRecordMapper.upsert(DccControlledFileRecognitionRecordDO.builder()
                .tenantId(tenantId)
                .controlledFileId(fileId)
                .recognitionScope(recognitionScope(task))
                .recognitionMethod(recognitionMethod(task))
                .recognitionVersion(recognitionVersion)
                .status(DccControlledFileProjectCodeRecognitionServiceImpl.RECOGNITION_STATUS_FAILED)
                .batchTaskId(task.getId())
                .failureStage(failureStage)
                .failureCode(failureCode)
                .failureMessage(normalizeLastFailureMessage(failureMessage))
                .recognizedBy(task.getOperatorUserId())
                .recognizedTime(LocalDateTime.now())
                .sourceFileId(file == null ? null : file.getSourceFileId())
                .build());
    }

    private void updateProgressAfterCandidate(DccControlledFileBatchRecognitionTaskDO task, TaskProgress progress,
                                              ProgressOutcome outcome, String lastFailureMessage) {
        TaskProgress.ProgressSnapshot snapshot = progress.recordOutcome(outcome);
        updateTaskProgress(task.getId(), task.getTotalCount(), snapshot.processedCount(),
                snapshot.successCount(), snapshot.failedCount(), snapshot.skippedExistingCount(),
                snapshot.unclassifiedCount(), snapshot.ambiguousCount(), snapshot.conflictCount(),
                lastFailureMessage);
    }

    private void updateProgressAfterCandidates(DccControlledFileBatchRecognitionTaskDO task, TaskProgress progress,
                                               List<CandidateOutcome> outcomes) {
        if (outcomes.isEmpty()) {
            return;
        }
        String lastFailureMessage = null;
        TaskProgress.ProgressSnapshot snapshot = null;
        for (CandidateOutcome outcome : outcomes) {
            snapshot = progress.recordOutcome(outcome.outcome());
            if (outcome.lastFailureMessage() != null) {
                lastFailureMessage = outcome.lastFailureMessage();
            }
        }
        updateTaskProgress(task.getId(), task.getTotalCount(), snapshot.processedCount(),
                snapshot.successCount(), snapshot.failedCount(), snapshot.skippedExistingCount(),
                snapshot.unclassifiedCount(), snapshot.ambiguousCount(), snapshot.conflictCount(),
                lastFailureMessage);
    }

    private DccControlledFileRecognitionRecordDO findExistingRecognition(DccControlledFileDO file,
                                                                         DccControlledFileBatchRecognitionTaskDO task) {
        String recognitionVersion = recognitionVersionSnapshotOrCurrent(task);
        return recognitionRecordMapper.selectLatestByFileAndVersion(
                file.getId(),
                recognitionScope(task),
                recognitionVersion);
    }

    private ProgressOutcome resolveExistingRecognitionOutcome(DccControlledFileBatchRecognitionTaskDO task,
                                                               DccControlledFileRecognitionRecordDO record) {
        if (isFileCategoryTask(task)) {
            return switch (StrUtil.trimToEmpty(record.getStatus()).toUpperCase(Locale.ROOT)) {
                case DccControlledFileProjectCodeRecognitionServiceImpl.RECOGNITION_STATUS_SUCCESS ->
                        ProgressOutcome.SUCCESS;
                case FILE_CATEGORY_STATUS_UNCLASSIFIED -> ProgressOutcome.UNCLASSIFIED;
                case FILE_CATEGORY_STATUS_AMBIGUOUS -> ProgressOutcome.AMBIGUOUS;
                case FILE_CATEGORY_STATUS_CONFLICT -> ProgressOutcome.CONFLICT;
                default -> ProgressOutcome.FAILED;
            };
        }
        if (isNonFailedRecognitionStatus(record.getStatus())) {
            return ProgressOutcome.SUCCESS;
        }
        return ProgressOutcome.FAILED;
    }

    private String resolveExistingFailureMessage(ProgressOutcome outcome,
                                                 DccControlledFileRecognitionRecordDO record) {
        if (outcome != ProgressOutcome.FAILED && outcome != ProgressOutcome.CONFLICT) {
            return null;
        }
        return StrUtil.blankToDefault(record.getFailureMessage(),
                "Existing recognition record status: " + record.getStatus());
    }

    private boolean isNonFailedRecognitionStatus(String status) {
        return DccControlledFileProjectCodeRecognitionServiceImpl.RECOGNITION_STATUS_SUCCESS.equals(status)
                || DccControlledFileProjectCodeRecognitionServiceImpl.RECOGNITION_STATUS_NO_MATCH.equals(status)
                || DccControlledFileProjectCodeRecognitionServiceImpl.RECOGNITION_STATUS_UNKNOWN_DCC_BASIC_DATA.equals(status)
                || DccControlledFileProjectCodeRecognitionServiceImpl.RECOGNITION_STATUS_UNRECOGNIZED_PROJECT_NAME.equals(status);
    }

    private boolean isReusableSuccessfulRecognitionStatus(String status) {
        return DccControlledFileProjectCodeRecognitionServiceImpl.RECOGNITION_STATUS_SUCCESS.equals(status);
    }

    private boolean acquireBatchFileClaim(DccControlledFileBatchRecognitionTaskDO task, DccControlledFileDO file) {
        String recognitionScope = recognitionScope(task);
        if (recognitionClaimMapper.tryClaimBasicInfo(
                file.getTenantId(),
                file.getId(),
                recognitionScope,
                task.getOperatorUserId(),
                task.getId(),
                LocalDateTime.now()) > 0) {
            return true;
        }
        DccControlledFileRecognitionClaimDO currentClaim = recognitionClaimMapper.selectByFileAndScope(
                file.getId(), recognitionScope);
        if (currentClaim != null
                && Objects.equals(currentClaim.getClaimedBy(), task.getOperatorUserId())
                && Objects.equals(currentClaim.getClaimTaskId(), task.getId())) {
            return true;
        }
        return false;
    }

    private void updateTaskProgress(Long taskId, Long totalCount, Long processedCount, Long successCount,
                                    Long failedCount, Long skippedExistingCount, Long unclassifiedCount,
                                    Long ambiguousCount, Long conflictCount, String lastFailureMessage) {
        long total = totalCount == null ? 0L : totalCount;
        long processed = processedCount == null ? 0L : processedCount;
        DccControlledFileBatchRecognitionTaskDO update = DccControlledFileBatchRecognitionTaskDO.builder()
                .id(taskId)
                .processedCount(processed)
                .successCount(successCount)
                .failedCount(failedCount)
                .skippedExistingCount(skippedExistingCount)
                .unclassifiedCount(unclassifiedCount)
                .ambiguousCount(ambiguousCount)
                .conflictCount(conflictCount)
                .remainingCount(Math.max(total - processed, 0L))
                .lastFailureMessage(normalizeLastFailureMessage(lastFailureMessage))
                .build();
        taskMapper.updateById(update);
    }

    private void markTaskCompleted(Long taskId, Long processedCount, Long successCount,
                                   Long failedCount, Long skippedExistingCount, Long unclassifiedCount,
                                   Long ambiguousCount, Long conflictCount) {
        DccControlledFileBatchRecognitionTaskDO existing = taskMapper.selectById(taskId);
        long totalCount = existing == null || existing.getTotalCount() == null ? 0L : existing.getTotalCount();
        DccControlledFileBatchRecognitionTaskDO update = DccControlledFileBatchRecognitionTaskDO.builder()
                .id(taskId)
                .status(TASK_STATUS_COMPLETED)
                .processedCount(processedCount)
                .successCount(successCount)
                .failedCount(failedCount)
                .skippedExistingCount(skippedExistingCount)
                .unclassifiedCount(unclassifiedCount)
                .ambiguousCount(ambiguousCount)
                .conflictCount(conflictCount)
                .remainingCount(Math.max(totalCount - (processedCount == null ? 0L : processedCount), 0L))
                .completedAt(LocalDateTime.now())
                .build();
        taskMapper.updateById(update);
        recognitionClaimMapper.releaseClaimsByTaskId(taskId);
    }

    private void markTaskFailed(Long taskId, String reason) {
        DccControlledFileBatchRecognitionTaskDO update = DccControlledFileBatchRecognitionTaskDO.builder()
                .id(taskId)
                .status(TASK_STATUS_FAILED)
                .lastFailureMessage(normalizeLastFailureMessage(reason))
                .completedAt(LocalDateTime.now())
                .build();
        taskMapper.updateById(update);
        recognitionClaimMapper.releaseClaimsByTaskId(taskId);
    }

    private List<Long> listCandidateIds(Long userId,
                                        DccControlledFileBatchRecognitionCreateReqVO reqVO,
                                        String recognitionType,
                                        String scope,
                                        Long directoryId) {
        if (RECOGNITION_TYPE_FILE_CATEGORY.equals(recognitionType)) {
            LinkedHashSet<Long> candidateIds = new LinkedHashSet<>();
            for (DccProjectCodeDO projectCode : projectCodeMapper.selectList()) {
                if (projectCode.getId() == null) {
                    continue;
                }
                for (DccProjectCodeAssociatedFileAiCategoryRespVO candidate :
                        projectCodeService.getAssociatedFileAiCategoryCandidates(userId, projectCode.getId())) {
                    if (candidate != null && candidate.getFileId() != null) {
                        candidateIds.add(candidate.getFileId());
                    }
                }
            }
            return new ArrayList<>(candidateIds);
        }
        DccControlledFilePageReqVO pageReqVO = buildBrowserCandidateReq(reqVO, scope, directoryId);
        return queryService.listControlledFileBrowserCandidates(userId, pageReqVO).stream()
                .filter(this::isBusinessRecognitionCandidate)
                .map(DccControlledFileDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new), ArrayList::new));
    }

    private DccControlledFilePageReqVO buildBrowserCandidateReq(DccControlledFileBatchRecognitionCreateReqVO reqVO,
                                                                String scope, Long directoryId) {
        DccControlledFilePageReqVO pageReqVO = new DccControlledFilePageReqVO();
        pageReqVO.setCategoryId(reqVO.getCategoryId());
        pageReqVO.setStatus(normalizeKeyword(reqVO.getStatus()));
        pageReqVO.setKeyword(normalizeKeyword(reqVO.getKeyword()));
        pageReqVO.setLatestVersionOnly(true);
        if (SCOPE_CURRENT.equals(scope)) {
            pageReqVO.setDirectoryId(directoryId);
            pageReqVO.setIncludeDescendantDirectories(!Boolean.FALSE.equals(reqVO.getIncludeDescendantDirectories()));
        }
        return pageReqVO;
    }

    private String normalizeScope(String rawScope) {
        String scope = StrUtil.trimToEmpty(rawScope).toUpperCase();
        if (!SCOPE_CURRENT.equals(scope) && !SCOPE_GLOBAL.equals(scope)) {
            throw new IllegalStateException("unsupported batch recognition scope: " + rawScope);
        }
        return scope;
    }

    private String normalizeRecognitionType(String rawRecognitionType) {
        String recognitionType = StrUtil.trimToEmpty(rawRecognitionType).toUpperCase(Locale.ROOT);
        if (RECOGNITION_TYPE_BASIC_INFO.equals(recognitionType)
                || RECOGNITION_TYPE_FILE_CATEGORY.equals(recognitionType)
                || RECOGNITION_TYPE_FILE_NUMBER.equals(recognitionType)) {
            return recognitionType;
        }
        throw new IllegalStateException("unsupported batch recognition type: " + rawRecognitionType);
    }

    private String normalizeExistingRecordPolicy(DccControlledFileBatchRecognitionCreateReqVO reqVO) {
        String policy = StrUtil.trimToNull(reqVO.getExistingRecordPolicy());
        if (policy == null) {
            throw new IllegalStateException("batch recognition existing record policy is required");
        }
        policy = policy.toUpperCase(Locale.ROOT);
        if (EXISTING_RECORD_POLICY_SKIP_ALL_EXISTING.equals(policy)
                || EXISTING_RECORD_POLICY_RETRY_FAILED.equals(policy)
                || EXISTING_RECORD_POLICY_OVERWRITE_ALL.equals(policy)) {
            return policy;
        }
        throw new IllegalStateException("unsupported batch recognition existing record policy: "
                + reqVO.getExistingRecordPolicy());
    }

    private Long resolveDirectoryId(String scope, Long directoryId) {
        if (SCOPE_GLOBAL.equals(scope)) {
            return null;
        }
        if (directoryId == null || directoryId <= 0) {
            throw new IllegalStateException("directoryId is required for CURRENT scope");
        }
        return directoryId;
    }

    private String buildDirectoryPathSnapshot(Long directoryId) {
        List<DccFileDirectoryDO> directories = directoryMapper.selectList();
        Map<Long, DccFileDirectoryDO> directoryById = new HashMap<>();
        for (DccFileDirectoryDO directory : directories) {
            if (directory.getId() != null) {
                directoryById.put(directory.getId(), directory);
            }
        }
        List<String> segments = new ArrayList<>();
        Long currentId = directoryId;
        while (currentId != null) {
            DccFileDirectoryDO current = directoryById.get(currentId);
            if (current == null) {
                break;
            }
            segments.add(0, current.getName());
            currentId = current.getParentId();
        }
        return String.join("/", segments);
    }

    private List<Long> parseCandidateIds(String candidateIdsJson) {
        List<Long> candidateIds = JsonUtils.parseArray(candidateIdsJson, Long.class);
        return candidateIds == null ? List.of() : candidateIds.stream()
                .filter(Objects::nonNull)
                .toList();
    }

    private String normalizeKeyword(String value) {
        String normalized = StrUtil.trimToNull(value);
        return normalized;
    }

    private DccControlledFileBatchRecognitionTaskRespVO toRespVO(DccControlledFileBatchRecognitionTaskDO task) {
        DccControlledFileBatchRecognitionTaskRespVO respVO = new DccControlledFileBatchRecognitionTaskRespVO();
        respVO.setTaskId(task.getId());
        respVO.setStatus(task.getStatus());
        respVO.setRecognitionType(task.getRecognitionType());
        respVO.setScope(task.getScopeType());
        respVO.setRecognitionVersionSnapshot(task.getRecognitionVersionSnapshot());
        respVO.setDirectoryId(task.getDirectoryId());
        respVO.setDirectoryPath(task.getDirectoryPathSnapshot());
        respVO.setKeyword(task.getKeyword());
        respVO.setStatusFilter(task.getStatusFilter());
        respVO.setCategoryId(task.getCategoryId());
        respVO.setOverwriteExisting(task.getOverwriteExisting());
        respVO.setExistingRecordPolicy(resolveExistingRecordPolicy(task));
        respVO.setSyncFileNameTitle(task.getSyncFileNameTitle());
        respVO.setWorkerCount(task.getWorkerCount());
        respVO.setActiveWorkerCount(resolveActiveWorkerCount(task));
        respVO.setRecordedFileCount(resolveRecordedFileCount(task));
        respVO.setTotalCount(defaultLong(task.getTotalCount()));
        respVO.setProcessedCount(defaultLong(task.getProcessedCount()));
        respVO.setSuccessCount(defaultLong(task.getSuccessCount()));
        respVO.setFailedCount(defaultLong(task.getFailedCount()));
        respVO.setSkippedExistingCount(defaultLong(task.getSkippedExistingCount()));
        respVO.setUnclassifiedCount(defaultLong(task.getUnclassifiedCount()));
        respVO.setAmbiguousCount(defaultLong(task.getAmbiguousCount()));
        respVO.setConflictCount(defaultLong(task.getConflictCount()));
        respVO.setRemainingCount(defaultLong(task.getRemainingCount()));
        respVO.setLastFailureMessage(task.getLastFailureMessage());
        respVO.setFailureSummaries(resolveFailureSummaries(task));
        respVO.setStartedAt(task.getStartedAt());
        respVO.setCompletedAt(task.getCompletedAt());
        return respVO;
    }

    private List<DccControlledFileBatchRecognitionTaskRespVO.FailureSummary> resolveFailureSummaries(
            DccControlledFileBatchRecognitionTaskDO task) {
        if (!isTerminalStatus(task.getStatus()) || defaultLong(task.getFailedCount()) <= 0) {
            return List.of();
        }
        return recognitionRecordMapper.selectFailureSummariesByBatchTaskId(task.getId(), FAILURE_SUMMARY_LIMIT)
                .stream()
                .map(this::toFailureSummary)
                .toList();
    }

    private DccControlledFileBatchRecognitionTaskRespVO.FailureSummary toFailureSummary(
            DccControlledFileRecognitionFailureSummaryDO source) {
        DccControlledFileBatchRecognitionTaskRespVO.FailureSummary summary =
                new DccControlledFileBatchRecognitionTaskRespVO.FailureSummary();
        summary.setStage(source.getFailureStage());
        summary.setCode(source.getFailureCode());
        summary.setReason(source.getFailureMessage());
        summary.setCount(defaultLong(source.getFailureCount()));
        return summary;
    }

    private boolean shouldUseExistingRecognition(DccControlledFileBatchRecognitionTaskDO task,
                                                 DccControlledFileRecognitionRecordDO existingRecord) {
        if (existingRecord == null) {
            return false;
        }
        if (Objects.equals(existingRecord.getBatchTaskId(), task.getId())) {
            return true;
        }
        String policy = resolveExistingRecordPolicy(task);
        if (EXISTING_RECORD_POLICY_OVERWRITE_ALL.equals(policy)) {
            return false;
        }
        if (EXISTING_RECORD_POLICY_RETRY_FAILED.equals(policy)) {
            return isReusableSuccessfulRecognitionStatus(existingRecord.getStatus());
        }
        return true;
    }

    private String resolveExistingRecordPolicy(DccControlledFileBatchRecognitionTaskDO task) {
        String policy = StrUtil.trimToNull(task.getExistingRecordPolicy());
        if (policy == null) {
            throw new IllegalStateException("batch recognition existing record policy is required: " + task.getId());
        }
        policy = policy.toUpperCase(Locale.ROOT);
        if (EXISTING_RECORD_POLICY_SKIP_ALL_EXISTING.equals(policy)
                || EXISTING_RECORD_POLICY_RETRY_FAILED.equals(policy)
                || EXISTING_RECORD_POLICY_OVERWRITE_ALL.equals(policy)) {
            return policy;
        }
        throw new IllegalStateException("unsupported batch recognition existing record policy: "
                + task.getExistingRecordPolicy());
    }

    private boolean isTerminalStatus(String status) {
        return TASK_STATUS_COMPLETED.equals(status)
                || TASK_STATUS_FAILED.equals(status)
                || TASK_STATUS_STOPPED.equals(status);
    }

    private boolean isStopRequested(Long taskId) {
        DccControlledFileBatchRecognitionTaskDO current = taskMapper.selectById(taskId);
        return current == null || TASK_STATUS_STOPPED.equals(current.getStatus());
    }

    private int resolveActiveWorkerCount(DccControlledFileBatchRecognitionTaskDO task) {
        if (!TASK_STATUS_RUNNING.equals(task.getStatus())) {
            return 0;
        }
        return resolveWorkerCount(task.getWorkerCount(), parseCandidateIds(task.getCandidateIdsJson()).size());
    }

    private long resolveRecordedFileCount(DccControlledFileBatchRecognitionTaskDO task) {
        List<Long> candidateIds = parseCandidateIds(task.getCandidateIdsJson());
        if (candidateIds.isEmpty()) {
            return 0L;
        }
        Long count = recognitionRecordMapper.countRecordedFilesByFileIdsAndVersion(
                candidateIds,
                recognitionScope(task),
                recognitionVersionSnapshotOrCurrent(task));
        return count == null ? 0L : count;
    }

    private String recognitionVersionSnapshotOrCurrent(DccControlledFileBatchRecognitionTaskDO task) {
        String snapshot = StrUtil.trimToNull(task.getRecognitionVersionSnapshot());
        return snapshot == null ? resolveRecognitionVersion(task.getRecognitionType()) : snapshot;
    }

    private String resolveRecognitionVersion(String recognitionType) {
        if (RECOGNITION_TYPE_FILE_CATEGORY.equals(recognitionType)) {
            return FILE_CATEGORY_RECOGNITION_VERSION;
        }
        if (RECOGNITION_TYPE_FILE_NUMBER.equals(recognitionType)) {
            return FILE_NUMBER_RECOGNITION_VERSION;
        }
        return requireRecognitionVersion();
    }

    private boolean isFileCategoryTask(DccControlledFileBatchRecognitionTaskDO task) {
        return RECOGNITION_TYPE_FILE_CATEGORY.equals(task.getRecognitionType());
    }

    private boolean isFileNumberTask(DccControlledFileBatchRecognitionTaskDO task) {
        return RECOGNITION_TYPE_FILE_NUMBER.equals(task.getRecognitionType());
    }

    private String recognitionScope(DccControlledFileBatchRecognitionTaskDO task) {
        if (isFileCategoryTask(task)) {
            return RECOGNITION_TYPE_FILE_CATEGORY;
        }
        if (isFileNumberTask(task)) {
            return RECOGNITION_TYPE_FILE_NUMBER;
        }
        return DccControlledFileProjectCodeRecognitionServiceImpl.RECOGNITION_SCOPE_BASIC_INFO;
    }

    private String recognitionMethod(DccControlledFileBatchRecognitionTaskDO task) {
        if (isFileCategoryTask(task)) {
            return RECOGNITION_METHOD_BATCH_FILE_CATEGORY;
        }
        if (isFileNumberTask(task)) {
            return RECOGNITION_METHOD_BATCH_FILE_NUMBER;
        }
        return RECOGNITION_METHOD_BATCH_PROJECT_CODE;
    }

    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private String resolveThrowableMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return StrUtil.blankToDefault(current.getMessage(), current.getClass().getSimpleName());
    }

    private String normalizeLastFailureMessage(String message) {
        String normalized = StrUtil.trimToNull(message);
        if (normalized == null) {
            return null;
        }
        if (normalized.length() <= MAX_LAST_FAILURE_MESSAGE_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_LAST_FAILURE_MESSAGE_LENGTH);
    }

    private boolean isBusinessRecognitionCandidate(DccControlledFileDO file) {
        if (file == null || file.getId() == null) {
            return false;
        }
        return !isNonBusinessSystemFile(file.getFileName())
                && !isNonBusinessSystemFile(file.getTitle())
                && !isNonBusinessSystemFile(file.getFileNumber());
    }

    private boolean isNonBusinessSystemFile(String value) {
        String normalized = StrUtil.trimToEmpty(value).toLowerCase(Locale.ROOT);
        return "thumbs.db".equals(normalized)
                || "desktop.ini".equals(normalized)
                || normalized.startsWith("~$");
    }

    private int resolveWorkerCountSnapshot(Integer requestedWorkerCount) {
        Integer configured = requestedWorkerCount == null ? recognitionProperties.getWorkerCount() : requestedWorkerCount;
        if (configured == null || configured <= 0) {
            throw new IllegalStateException("dcc batch recognition worker count must be positive");
        }
        return configured;
    }

    private int resolveWorkerCount(Integer configured, int candidateCount) {
        if (candidateCount <= 1) {
            return 1;
        }
        if (configured == null || configured <= 1) {
            return 1;
        }
        return Math.min(configured, candidateCount);
    }

    private String requireRecognitionVersion() {
        String version = StrUtil.trimToNull(recognitionProperties.getVersion());
        if (version == null || isUnresolvedConfigPlaceholder(version)) {
            throw exception(CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_CONFIG_MISSING, "version is required");
        }
        return version;
    }

    private boolean isUnresolvedConfigPlaceholder(String value) {
        return value.contains("${");
    }

    private TransactionTemplate tx() {
        return new TransactionTemplate(transactionManager);
    }

    private enum ProgressOutcome {
        SUCCESS,
        UNCLASSIFIED,
        AMBIGUOUS,
        CONFLICT,
        FAILED,
        SKIPPED
    }

    private record CandidateOutcome(ProgressOutcome outcome, String lastFailureMessage) {

        private static CandidateOutcome success() {
            return new CandidateOutcome(ProgressOutcome.SUCCESS, null);
        }

        private static CandidateOutcome skipped() {
            return new CandidateOutcome(ProgressOutcome.SKIPPED, null);
        }
    }

    private static final class TaskProgress {

        private long processedCount;
        private long successCount;
        private long failedCount;
        private long skippedExistingCount;
        private long unclassifiedCount;
        private long ambiguousCount;
        private long conflictCount;

        private synchronized ProgressSnapshot recordOutcome(ProgressOutcome outcome) {
            switch (outcome) {
                case SUCCESS -> successCount++;
                case UNCLASSIFIED -> unclassifiedCount++;
                case AMBIGUOUS -> ambiguousCount++;
                case CONFLICT -> conflictCount++;
                case FAILED -> failedCount++;
                case SKIPPED -> skippedExistingCount++;
            }
            processedCount++;
            return new ProgressSnapshot(processedCount, successCount, failedCount, skippedExistingCount,
                    unclassifiedCount, ambiguousCount, conflictCount);
        }

        private synchronized ProgressSnapshot snapshot() {
            return new ProgressSnapshot(processedCount, successCount, failedCount, skippedExistingCount,
                    unclassifiedCount, ambiguousCount, conflictCount);
        }

        private static TaskProgress from(DccControlledFileBatchRecognitionTaskDO task) {
            TaskProgress progress = new TaskProgress();
            progress.processedCount = task.getProcessedCount() == null ? 0L : task.getProcessedCount();
            progress.successCount = task.getSuccessCount() == null ? 0L : task.getSuccessCount();
            progress.failedCount = task.getFailedCount() == null ? 0L : task.getFailedCount();
            progress.skippedExistingCount = task.getSkippedExistingCount() == null ? 0L : task.getSkippedExistingCount();
            progress.unclassifiedCount = task.getUnclassifiedCount() == null ? 0L : task.getUnclassifiedCount();
            progress.ambiguousCount = task.getAmbiguousCount() == null ? 0L : task.getAmbiguousCount();
            progress.conflictCount = task.getConflictCount() == null ? 0L : task.getConflictCount();
            return progress;
        }

        private record ProgressSnapshot(long processedCount, long successCount, long failedCount,
                                        long skippedExistingCount, long unclassifiedCount,
                                        long ambiguousCount, long conflictCount) {
        }
    }
}
