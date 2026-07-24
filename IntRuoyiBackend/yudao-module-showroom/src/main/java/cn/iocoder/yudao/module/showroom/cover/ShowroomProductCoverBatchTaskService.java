package cn.iocoder.yudao.module.showroom.cover;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductDraft;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductRevision;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductSnapshot;
import cn.iocoder.yudao.module.showroom.content.service.ShowroomPersistentContentService;
import cn.iocoder.yudao.module.showroom.controller.admin.ShowroomAdminController;
import cn.iocoder.yudao.module.showroom.dal.dataobject.cover.ShowroomProductCoverBatchTaskDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.cover.ShowroomProductCoverBatchTaskItemDO;
import cn.iocoder.yudao.module.showroom.dal.mysql.cover.ShowroomProductCoverBatchTaskItemMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.cover.ShowroomProductCoverBatchTaskMapper;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationAudioDraftCommand;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationAudienceType;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationDraftCommand;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationKey;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationLanguage;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationOperations;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationTargetType;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationVersion;
import cn.iocoder.yudao.module.showroom.prompt.ShowroomImagePromptVersionService;
import cn.iocoder.yudao.module.showroom.release.ShowroomVersionBundleService;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
public class ShowroomProductCoverBatchTaskService {

    public static final String TASK_STATUS_WAITING = "WAITING";
    public static final String TASK_STATUS_RUNNING = "RUNNING";
    public static final String TASK_STATUS_COMPLETED = "COMPLETED";
    public static final String ITEM_STATUS_WAITING = "WAITING";
    public static final String ITEM_STATUS_RUNNING = "RUNNING";
    public static final String ITEM_STATUS_COMPLETED = "COMPLETED";
    public static final String ITEM_STATUS_FAILED = "FAILED";

    public record TaskItemSnapshot(Long productId, Long sourceRevisionId, String productCode, String nameCn,
                                   String nameEn, Map<String, String> promptFields) {
    }

    public record StartTaskCommand(Long operatorUserId, String keyword, String lifecycleStage,
                                   String incompleteStatus, String approvalStatus, String coverGenerationMode,
                                   Long promptVersionId,
                                   int matchedCount, int publishedCount, int skippedUnpublishedCount,
                                   int skippedExistingCount, List<TaskItemSnapshot> items) {
    }

    private record ItemExecutionResult(boolean succeeded,
                                       ShowroomAdminController.ProductBatchGenerateFailureRespVO failure) {
    }

    private record TaskCounters(int succeededCount, int failedCount, int remainingPendingCount) {
    }

    private final ShowroomProductCoverBatchTaskMapper taskMapper;
    private final ShowroomProductCoverBatchTaskItemMapper itemMapper;
    private final ShowroomPersistentContentService contentService;
    private final ShowroomProductCoverImageService productCoverImageService;
    private final ShowroomImagePromptVersionService imagePromptVersionService;
    private final ShowroomNarrationOperations narrationService;
    private final ShowroomVersionBundleService versionBundleService;
    private final TransactionTemplate transactionTemplate;
    private final ReentrantLock schedulerLock = new ReentrantLock();

    public ShowroomProductCoverBatchTaskService(ShowroomProductCoverBatchTaskMapper taskMapper,
                                                ShowroomProductCoverBatchTaskItemMapper itemMapper,
                                                 ShowroomPersistentContentService contentService,
                                                 ShowroomProductCoverImageService productCoverImageService,
                                                 ShowroomImagePromptVersionService imagePromptVersionService,
                                                 ShowroomNarrationOperations narrationService,
                                                 ShowroomVersionBundleService versionBundleService,
                                                 PlatformTransactionManager transactionManager) {
        this.taskMapper = taskMapper;
        this.itemMapper = itemMapper;
        this.contentService = contentService;
        this.productCoverImageService = productCoverImageService;
        this.imagePromptVersionService = imagePromptVersionService;
        this.narrationService = narrationService;
        this.versionBundleService = versionBundleService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public ShowroomAdminController.ProductBatchGenerateRespVO startTask(StartTaskCommand command) {
        ShowroomProductCoverBatchTaskDO activeTask = taskMapper.selectActiveTask();
        if (activeTask != null) {
            throw new IllegalStateException("SHOWROOM_COVER_GENERATION_FAILED: 已存在未完成的一键封面后台任务，任务 "
                    + activeTask.getId() + " 仍有 " + nullSafeInt(activeTask.getRemainingPendingCount())
                    + " 个产品待生成，请等待自动续跑完成后再重试");
        }
        if (command.items() == null || command.items().isEmpty()) {
            return new ShowroomAdminController.ProductBatchGenerateRespVO(
                    command.matchedCount(),
                    command.publishedCount(),
                    command.skippedUnpublishedCount(),
                    command.skippedExistingCount(),
                    0,
                    0,
                    false,
                    0,
                    null,
                    TASK_STATUS_COMPLETED,
                    0,
                    null,
                    List.of()
            );
        }
        Long taskId = Objects.requireNonNull(transactionTemplate.execute(status -> createTaskSnapshot(command)));
        return executeTaskRound(taskId);
    }

    public void recoverInterruptedTasksOnStartup() {
        LocalDateTime nextCheckAt = nextScheduledCheckTime();
        int recoveredCount = taskMapper.recoverRunningTasksToWaiting(nextCheckAt);
        int recoveredItemCount = 0;
        for (ShowroomProductCoverBatchTaskDO task : taskMapper.selectUnfinishedTasks()) {
            recoveredItemCount += recoverStaleRunningItems(task.getId());
        }
        if (recoveredCount > 0) {
            log.info("[recoverInterruptedTasksOnStartup][recoveredCount({})][nextCheckAt({})]",
                    recoveredCount, nextCheckAt);
        }
        if (recoveredItemCount > 0) {
            log.info("[recoverInterruptedTasksOnStartup][recoveredItemCount({})]", recoveredItemCount);
        }
    }

    public void processWaitingTasks() {
        if (!schedulerLock.tryLock()) {
            return;
        }
        try {
            for (ShowroomProductCoverBatchTaskDO task : taskMapper.selectWaitingTasks()) {
                try {
                    executeTaskRound(task.getId());
                } catch (RuntimeException exception) {
                    log.error("[processWaitingTasks][taskId({}) 后台续跑执行失败]", task.getId(), exception);
                }
            }
        } finally {
            schedulerLock.unlock();
        }
    }

    public ShowroomAdminController.ProductCoverBatchTaskStateRespVO getTaskState() {
        ShowroomProductCoverBatchTaskDO activeTask = taskMapper.selectActiveTask();
        if (activeTask != null) {
            return toStateResponse(activeTask, false);
        }
        ShowroomProductCoverBatchTaskDO latestTask = taskMapper.selectLatestTask();
        if (latestTask != null) {
            return toStateResponse(latestTask, true);
        }
        return new ShowroomAdminController.ProductCoverBatchTaskStateRespVO(
                true,
                false,
                false,
                "",
                "",
                "",
                "",
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                null,
                "",
                null,
                null,
                null,
                "",
                null
        );
    }

    public ShowroomAdminController.ProductBatchGenerateRespVO executeTaskRound(Long taskId) {
        ShowroomProductCoverBatchTaskDO task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalStateException("SHOWROOM_COVER_GENERATION_FAILED: cover batch task not found: " + taskId);
        }
        if (TASK_STATUS_COMPLETED.equals(task.getStatus())) {
            return toResponse(task, List.of());
        }
        if (taskMapper.claimWaitingTask(taskId) == 0) {
            ShowroomProductCoverBatchTaskDO current = taskMapper.selectById(taskId);
            return toResponse(current == null ? task : current, List.of());
        }

        try {
            recoverStaleRunningItems(taskId);
            return executeClaimedTask(taskId);
        } catch (Throwable throwable) {
            ShowroomProductCoverBatchTaskDO current = taskMapper.selectById(taskId);
            if (current != null && !TASK_STATUS_COMPLETED.equals(current.getStatus())) {
                current.setStatus(TASK_STATUS_WAITING);
                current.setNextCheckAt(nextScheduledCheckTime());
                current.setLastFailureMessage(batchFailureReason(throwable));
                taskMapper.updateById(current);
            }
            if (throwable instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (throwable instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("SHOWROOM_COVER_GENERATION_FAILED: unexpected task execution error", throwable);
        }
    }

    private ShowroomAdminController.ProductBatchGenerateRespVO executeClaimedTask(Long taskId) {
        ShowroomProductCoverBatchTaskDO task = taskMapper.selectById(taskId);
        List<ShowroomProductCoverBatchTaskItemDO> waitingItems = itemMapper.selectWaitingItemsByTaskId(taskId);
        if (waitingItems.isEmpty()) {
            markTaskCompleted(task, 0, null);
            return toResponse(taskMapper.selectById(taskId), List.of());
        }

        int parallelism = resolveExecutionParallelism(
                productCoverImageService.resolveBatchParallelism(),
                waitingItems.size());
        ExecutorService executorService = Executors.newFixedThreadPool(parallelism);
        List<ShowroomAdminController.ProductBatchGenerateFailureRespVO> failures = new ArrayList<>();
        try {
            List<CompletableFuture<ItemExecutionResult>> futures = waitingItems.stream()
                    .map(item -> CompletableFuture.supplyAsync(
                            () -> processSingleItem(item, task.getOperatorUserId()),
                            executorService
                    ))
                    .toList();
            for (CompletableFuture<ItemExecutionResult> future : futures) {
                ItemExecutionResult result = future.join();
                if (!result.succeeded() && result.failure() != null) {
                    failures.add(result.failure());
                }
            }
        } finally {
            executorService.shutdown();
        }

        ShowroomProductCoverBatchTaskDO refreshedTask = taskMapper.selectById(taskId);
        int remainingPendingCount = itemMapper.selectWaitingItemsByTaskId(taskId).size();
        if (remainingPendingCount == 0) {
            markTaskCompleted(refreshedTask, failures.size(), firstFailureMessage(failures));
        } else {
            markTaskWaiting(refreshedTask, remainingPendingCount, failures.size(), firstFailureMessage(failures));
        }
        return toResponse(taskMapper.selectById(taskId), failures);
    }

    static int resolveExecutionParallelism(int configuredMaximumParallelism, int waitingItemCount) {
        return Math.min(configuredMaximumParallelism, waitingItemCount);
    }

    private ItemExecutionResult processSingleItem(ShowroomProductCoverBatchTaskItemDO item, Long operatorUserId) {
        LocalDateTime now = LocalDateTime.now();
        try {
            item.setStatus(ITEM_STATUS_RUNNING);
            item.setLastAttemptAt(now);
            itemMapper.updateById(item);
            String coverImage = generateAndPublishSingleProductCoverImage(item, operatorUserId);
            item.setStatus(ITEM_STATUS_COMPLETED);
            item.setAttemptCount(nullSafeInt(item.getAttemptCount()) + 1);
            item.setGeneratedCoverImage(coverImage);
            item.setLastError(null);
            item.setLastAttemptAt(now);
            item.setCompletedAt(now);
            itemMapper.updateById(item);
            return new ItemExecutionResult(true, null);
        } catch (RuntimeException exception) {
            String failureReason = batchFailureReason(exception);
            boolean retryable = isRetryableFailure(failureReason);
            item.setStatus(retryable ? ITEM_STATUS_WAITING : ITEM_STATUS_FAILED);
            item.setAttemptCount(nullSafeInt(item.getAttemptCount()) + 1);
            item.setLastError(failureReason);
            item.setLastAttemptAt(now);
            item.setCompletedAt(retryable ? null : now);
            itemMapper.updateById(item);
            return new ItemExecutionResult(false, new ShowroomAdminController.ProductBatchGenerateFailureRespVO(
                    item.getProductId(), item.getProductCode(), nullToEmpty(item.getNameCn()),
                    failureReason
            ));
        }
    }

    private String generateAndPublishSingleProductCoverImage(ShowroomProductCoverBatchTaskItemDO item,
                                                             Long operatorUserId) {
        ShowroomProductSnapshot snapshot = contentService.getProduct(item.getProductId());
        ShowroomProductRevision currentRevision = contentService.requireCurrentProductRevision(item.getProductId());
        Map<String, String> nextFields = new LinkedHashMap<>(currentRevision.fields());
        parsePromptFields(item.getPromptFieldsJson());
        Long promptVersionId = requirePromptVersionId(item.getTaskId());
        String renderedPrompt = imagePromptVersionService.renderProductCoverPrompt(promptVersionId,
                item.getNameCn(), item.getNameEn());
        String coverImage = productCoverImageService.generateCoverImage(item.getProductCode(), renderedPrompt);
        nextFields.put("cover_image", coverImage);
        transactionTemplate.executeWithoutResult(status -> {
            ShowroomProductRevision savedDraft = contentService.saveProductDraft(new ShowroomProductDraft(
                    item.getProductId(), snapshot.productCode(), currentRevision.nameCn(), currentRevision.nameEn(),
                    nextFields
            ));
            carryForwardLivePublicAssets(item.getProductId(), currentRevision.revisionId(), savedDraft.revisionId());
            ShowroomProductRevision published = contentService.publishProductRevision(savedDraft.revisionId(),
                    operatorUserId);
            versionBundleService.ensureBundleForPublishedRevision("PRODUCT", item.getProductId(),
                    published.revisionId(), operatorUserId, null);
        });
        imagePromptVersionService.recordUsage(promptVersionId);
        return coverImage;
    }

    private Long requirePromptVersionId(Long taskId) {
        ShowroomProductCoverBatchTaskDO task = taskMapper.selectById(taskId);
        if (task == null || task.getPromptVersionId() == null) {
            throw new IllegalStateException("SHOWROOM_IMAGE_PROMPT_NOT_FOUND: prompt version id missing for cover batch task "
                    + taskId);
        }
        return task.getPromptVersionId();
    }

    private void carryForwardLivePublicAssets(Long productId, Long currentRevisionId, Long nextRevisionId) {
        carryForwardNarration(productId, currentRevisionId, nextRevisionId, ShowroomNarrationLanguage.ZH);
        carryForwardNarration(productId, currentRevisionId, nextRevisionId, ShowroomNarrationLanguage.EN);
    }

    private void carryForwardNarration(Long productId, Long currentRevisionId, Long nextRevisionId,
                                       ShowroomNarrationLanguage language) {
        ShowroomNarrationVersion liveNarration = narrationService.live(new ShowroomNarrationKey(
                        ShowroomNarrationTargetType.PRODUCT, productId,
                        ShowroomNarrationAudienceType.PUBLIC, language))
                .orElseThrow(() -> new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: live product "
                        + language.name() + " narration not found"));
        if (!currentRevisionId.equals(liveNarration.sourceRevisionId())) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: live product "
                    + language.name() + " narration source revision mismatch");
        }
        if (liveNarration.scriptText() == null || liveNarration.scriptText().isBlank()) {
            throw new IllegalStateException("SHOWROOM_SCRIPT_MISSING: live product "
                    + language.name() + " narration text is required");
        }
        if (liveNarration.audioFileId() == null || liveNarration.audioDurationSeconds() == null
                || liveNarration.audioDurationSeconds() <= 0) {
            throw new IllegalStateException("SHOWROOM_AUDIO_GENERATION_FAILED: live product "
                    + language.name() + " narration audio is required");
        }
        ShowroomNarrationVersion draft = narrationService.draftScript(new ShowroomNarrationDraftCommand(
                ShowroomNarrationTargetType.PRODUCT,
                productId,
                nextRevisionId,
                ShowroomNarrationAudienceType.PUBLIC,
                language,
                liveNarration.scriptText(),
                liveNarration.generatedByAi()
        ));
        draft = narrationService.attachAudio(new ShowroomNarrationAudioDraftCommand(
                draft.id(),
                liveNarration.audioFileId(),
                liveNarration.audioDurationSeconds(),
                liveNarration.voice()
        ));
        narrationService.publishDirectly(draft.id());
    }

    private static Map<String, String> parsePromptFields(String promptFieldsJson) {
        Map<String, String> promptFields = JsonUtils.parseObject(promptFieldsJson, new TypeReference<Map<String, String>>() {
        });
        if (promptFields == null) {
            throw new IllegalStateException("SHOWROOM_COVER_GENERATION_FAILED: cover batch task prompt fields are required");
        }
        return promptFields;
    }

    private void markTaskCompleted(ShowroomProductCoverBatchTaskDO task, int roundFailureCount, String lastFailureMessage) {
        TaskCounters counters = summarizeTask(task.getId());
        task.setStatus(TASK_STATUS_COMPLETED);
        task.setSucceededCount(counters.succeededCount());
        task.setFailedCount(counters.failedCount());
        task.setRemainingPendingCount(0);
        task.setNextCheckAt(null);
        task.setLastRunAt(LocalDateTime.now());
        task.setCompletedAt(LocalDateTime.now());
        task.setLastFailureMessage(resolveFailureMessage(task.getLastFailureMessage(),
                roundFailureCount > 0 || counters.failedCount() > 0 ? lastFailureMessage : null));
        taskMapper.updateById(task);
    }

    private void markTaskWaiting(ShowroomProductCoverBatchTaskDO task, int remainingPendingCount,
                                 int roundFailureCount, String lastFailureMessage) {
        TaskCounters counters = summarizeTask(task.getId());
        task.setStatus(TASK_STATUS_WAITING);
        task.setSucceededCount(counters.succeededCount());
        task.setFailedCount(counters.failedCount());
        task.setRemainingPendingCount(remainingPendingCount);
        task.setNextCheckAt(nextScheduledCheckTime());
        task.setLastRunAt(LocalDateTime.now());
        task.setLastFailureMessage(resolveFailureMessage(task.getLastFailureMessage(),
                roundFailureCount > 0 || counters.failedCount() > 0 ? lastFailureMessage : null));
        taskMapper.updateById(task);
    }

    private ShowroomAdminController.ProductBatchGenerateRespVO toResponse(
            ShowroomProductCoverBatchTaskDO task,
            List<ShowroomAdminController.ProductBatchGenerateFailureRespVO> failures) {
        return new ShowroomAdminController.ProductBatchGenerateRespVO(
                nullSafeInt(task.getMatchedCount()),
                nullSafeInt(task.getPublishedCount()),
                nullSafeInt(task.getSkippedUnpublishedCount()),
                nullSafeInt(task.getSkippedExistingCount()),
                0,
                nullSafeInt(task.getSucceededCount()),
                failures.isEmpty() ? nullSafeInt(task.getFailedCount()) : failures.size(),
                TASK_STATUS_WAITING.equals(task.getStatus()),
                nullSafeInt(task.getRemainingPendingCount()),
                task.getId(),
                nullToEmpty(task.getStatus()),
                nullSafeInt(task.getRemainingPendingCount()),
                formatTime(task.getNextCheckAt()),
                List.copyOf(failures)
        );
    }

    private ShowroomAdminController.ProductCoverBatchTaskStateRespVO toStateResponse(
            ShowroomProductCoverBatchTaskDO task,
            boolean startAllowed) {
        ShowroomProductCoverBatchTaskItemDO currentRunningItem = itemMapper.selectRunningItemsByTaskId(task.getId()).stream()
                .findFirst()
                .orElse(null);
        boolean active = TASK_STATUS_WAITING.equals(task.getStatus()) || TASK_STATUS_RUNNING.equals(task.getStatus());
        boolean running = TASK_STATUS_RUNNING.equals(task.getStatus()) || currentRunningItem != null;
        ShowroomAdminController.ProductBatchTaskCurrentProductRespVO currentProduct = currentRunningItem == null
                ? null
                : new ShowroomAdminController.ProductBatchTaskCurrentProductRespVO(
                currentRunningItem.getProductId(),
                nullToEmpty(currentRunningItem.getProductCode()),
                nullToEmpty(currentRunningItem.getNameCn())
        );
        return new ShowroomAdminController.ProductCoverBatchTaskStateRespVO(
                startAllowed,
                active,
                running,
                nullToEmpty(task.getKeyword()),
                nullToEmpty(task.getLifecycleStage()),
                nullToEmpty(task.getIncompleteStatus()),
                nullToEmpty(task.getApprovalStatus()),
                nullSafeInt(task.getMatchedCount()),
                nullSafeInt(task.getPublishedCount()),
                nullSafeInt(task.getSkippedUnpublishedCount()),
                nullSafeInt(task.getSkippedExistingCount()),
                nullSafeInt(task.getSucceededCount()),
                nullSafeInt(task.getFailedCount()),
                nullSafeInt(task.getRemainingPendingCount()),
                task.getId(),
                nullToEmpty(task.getStatus()),
                formatTime(task.getNextCheckAt()),
                toEpochMilli(task.getLastRunAt()),
                toEpochMilli(task.getCompletedAt()),
                nullToEmpty(task.getLastFailureMessage()),
                currentProduct
        );
    }

    private static String firstFailureMessage(List<ShowroomAdminController.ProductBatchGenerateFailureRespVO> failures) {
        return failures.isEmpty() ? null : failures.get(0).reason();
    }

    private int recoverStaleRunningItems(Long taskId) {
        int recoveredItemCount = itemMapper.recoverRunningItemsToWaiting(taskId);
        if (recoveredItemCount > 0) {
            log.info("[recoverStaleRunningItems][taskId({})][recoveredItemCount({})]", taskId, recoveredItemCount);
        }
        return recoveredItemCount;
    }

    private TaskCounters summarizeTask(Long taskId) {
        int succeededCount = 0;
        int failedCount = 0;
        int remainingPendingCount = 0;
        for (ShowroomProductCoverBatchTaskItemDO item : itemMapper.selectListByTaskId(taskId)) {
            if (ITEM_STATUS_COMPLETED.equals(item.getStatus())) {
                succeededCount++;
                continue;
            }
            if (ITEM_STATUS_FAILED.equals(item.getStatus())) {
                failedCount++;
                continue;
            }
            if (ITEM_STATUS_WAITING.equals(item.getStatus())) {
                remainingPendingCount++;
                if (hasText(item.getLastError())) {
                    failedCount++;
                }
            }
        }
        return new TaskCounters(succeededCount, failedCount, remainingPendingCount);
    }

    private static int nullSafeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String formatTime(LocalDateTime value) {
        return value == null ? null : value.truncatedTo(ChronoUnit.SECONDS).toString().replace('T', ' ');
    }

    private static Long toEpochMilli(LocalDateTime value) {
        return value == null ? null : value.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private static String batchFailureReason(RuntimeException exception) {
        if (exception.getMessage() != null && !exception.getMessage().trim().isEmpty()) {
            return exception.getMessage().trim();
        }
        Throwable cause = exception.getCause();
        if (cause != null && cause.getMessage() != null && !cause.getMessage().trim().isEmpty()) {
            return cause.getMessage().trim();
        }
        return exception.getClass().getSimpleName();
    }

    private static String batchFailureReason(Throwable throwable) {
        if (throwable instanceof RuntimeException runtimeException) {
            return batchFailureReason(runtimeException);
        }
        if (throwable.getMessage() != null && !throwable.getMessage().trim().isEmpty()) {
            return throwable.getMessage().trim();
        }
        Throwable cause = throwable.getCause();
        if (cause != null && cause.getMessage() != null && !cause.getMessage().trim().isEmpty()) {
            return cause.getMessage().trim();
        }
        return throwable.getClass().getSimpleName();
    }

    private static boolean isRetryableFailure(String failureReason) {
        if (!hasText(failureReason)) {
            return true;
        }
        return !(failureReason.startsWith("SHOWROOM_TARGET_NOT_FOUND:")
                || failureReason.contains("503 Service temporarily unavailable")
                || failureReason.startsWith("SHOWROOM_SCRIPT_MISSING:")
                || failureReason.startsWith("SHOWROOM_AUDIO_GENERATION_FAILED:"));
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String resolveFailureMessage(String existingFailureMessage, String latestFailureMessage) {
        if (hasText(latestFailureMessage)) {
            return latestFailureMessage.trim();
        }
        if (hasText(existingFailureMessage)) {
            return existingFailureMessage.trim();
        }
        return null;
    }

    private static LocalDateTime nextScheduledCheckTime() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        int minute = now.getMinute();
        int nextMinute = ((minute / 10) + 1) * 10;
        if (nextMinute < 60) {
            return now.withMinute(nextMinute).withSecond(0).withNano(0);
        }
        return now.plusHours(1).withMinute(0).withSecond(0).withNano(0);
    }

    private Long createTaskSnapshot(StartTaskCommand command) {
        ShowroomProductCoverBatchTaskDO task = ShowroomProductCoverBatchTaskDO.builder()
                .operatorUserId(command.operatorUserId())
                .status(TASK_STATUS_WAITING)
                .keyword(normalizeText(command.keyword()))
                .lifecycleStage(normalizeText(command.lifecycleStage()))
                .incompleteStatus(normalizeText(command.incompleteStatus()))
                .approvalStatus(normalizeText(command.approvalStatus()))
                .coverGenerationMode(Objects.requireNonNullElse(normalizeText(command.coverGenerationMode()), "ALL"))
                .promptVersionId(command.promptVersionId())
                .matchedCount(command.matchedCount())
                .publishedCount(command.publishedCount())
                .skippedUnpublishedCount(command.skippedUnpublishedCount())
                .skippedExistingCount(command.skippedExistingCount())
                .succeededCount(0)
                .failedCount(command.items().size())
                .remainingPendingCount(command.items().size())
                .nextCheckAt(nextScheduledCheckTime())
                .build();
        taskMapper.insert(task);
        for (TaskItemSnapshot item : command.items()) {
            itemMapper.insert(ShowroomProductCoverBatchTaskItemDO.builder()
                    .taskId(task.getId())
                    .productId(item.productId())
                    .sourceRevisionId(item.sourceRevisionId())
                    .productCode(item.productCode())
                    .nameCn(item.nameCn())
                    .nameEn(item.nameEn())
                    .promptFieldsJson(JsonUtils.toJsonString(item.promptFields()))
                    .status(ITEM_STATUS_WAITING)
                    .attemptCount(0)
                    .build());
        }
        return task.getId();
    }
}
