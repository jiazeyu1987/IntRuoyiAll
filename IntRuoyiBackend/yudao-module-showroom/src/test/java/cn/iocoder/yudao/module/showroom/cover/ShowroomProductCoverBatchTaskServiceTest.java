package cn.iocoder.yudao.module.showroom.cover;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetDraftCommand;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetFiles;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetKey;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetOperations;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetStatus;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetTargetType;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetVersion;
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
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationGenerationStatus;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationKey;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationLanguage;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationOperations;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationStatus;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationTargetType;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationVersion;
import cn.iocoder.yudao.module.showroom.prompt.ShowroomImagePromptVersionService;
import cn.iocoder.yudao.module.showroom.release.ShowroomVersionBundleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import(ShowroomProductCoverBatchTaskService.class)
class ShowroomProductCoverBatchTaskServiceTest extends BaseDbUnitTest {

    @Resource
    private ShowroomProductCoverBatchTaskService service;

    @Resource
    private ShowroomProductCoverBatchTaskMapper taskMapper;

    @Resource
    private ShowroomProductCoverBatchTaskItemMapper itemMapper;

    @MockBean
    private ShowroomPersistentContentService contentService;

    @MockBean
    private ShowroomProductCoverImageService productCoverImageService;

    @MockBean
    private ShowroomImagePromptVersionService imagePromptVersionService;

    @MockBean
    private ShowroomPreviewAssetOperations previewAssetService;

    @MockBean
    private ShowroomNarrationOperations narrationService;

    @MockBean
    private ShowroomVersionBundleService versionBundleService;

    @BeforeEach
    void setUp() {
        when(productCoverImageService.resolveBatchParallelism()).thenReturn(1);
        when(imagePromptVersionService.renderProductCoverPrompt(eq(7001L), anyString(), anyString()))
                .thenAnswer(invocation -> "rendered:" + invocation.getArgument(1, String.class));
    }

    @Test
    void startTaskShouldReturnWaitingTaskMetadataWhenFailuresRemain() {
        mockContentPersistence(Map.of(
                1L, createSnapshot(1L, "BATCH-COVER-OK"),
                2L, createSnapshot(2L, "BATCH-COVER-FAIL")
        ), Map.of(
                1L, createRevision(1L, 1001L, 5, "批量封面成功产品", "Batch Cover Success Product"),
                2L, createRevision(2L, 2001L, 6, "批量封面失败产品", "Batch Cover Failed Product")
        ));
        when(productCoverImageService.generateCoverImage(eq("BATCH-COVER-OK"), eq("rendered:批量封面成功产品")))
                .thenReturn("/admin-api/infra/file/29/get/showroom/product/cover/batch-cover-ok.png");
        when(productCoverImageService.generateCoverImage(eq("BATCH-COVER-FAIL"), eq("rendered:批量封面失败产品")))
                .thenThrow(new IllegalStateException("SHOWROOM_COVER_GENERATION_FAILED: mock cover generation failure"));

        ShowroomAdminController.ProductBatchGenerateRespVO summary = service.startTask(
                new ShowroomProductCoverBatchTaskService.StartTaskCommand(
                        300L,
                        "BATCH-COVER",
                        null,
                        null,
                        null,
                        "ALL",
                        7001L,
                        2,
                        2,
                        0,
                        0,
                        List.of(
                                new ShowroomProductCoverBatchTaskService.TaskItemSnapshot(
                                        1L, 1001L, "BATCH-COVER-OK", "批量封面成功产品", "Batch Cover Success Product",
                                        Map.of("target_market", "中国", "core_selling_points", "成功卖点")
                                ),
                                new ShowroomProductCoverBatchTaskService.TaskItemSnapshot(
                                        2L, 2001L, "BATCH-COVER-FAIL", "批量封面失败产品", "Batch Cover Failed Product",
                                        Map.of("target_market", "中国", "core_selling_points", "失败卖点")
                                )
                        )
                )
        );

        assertNotNull(summary.taskId());
        assertEquals("WAITING", summary.taskStatus());
        assertEquals(1, summary.remainingPendingCount());
        assertNotNull(summary.nextCheckAt());
        assertEquals(1, summary.succeededCount());
        assertEquals(1, summary.failedCount());

        ShowroomProductCoverBatchTaskDO task = taskMapper.selectById(summary.taskId());
        assertEquals("WAITING", task.getStatus());
        assertEquals("ALL", task.getCoverGenerationMode());
        assertEquals(7001L, task.getPromptVersionId());

        List<ShowroomProductCoverBatchTaskItemDO> items = itemMapper.selectListByTaskId(summary.taskId());
        assertEquals(2, items.size());
        assertEquals(1, items.stream().filter(item -> "COMPLETED".equals(item.getStatus())).count());
        ShowroomProductCoverBatchTaskItemDO failedItem = items.stream()
                .filter(item -> "WAITING".equals(item.getStatus()))
                .findFirst()
                .orElseThrow();
        assertTrue(failedItem.getLastError().contains("mock cover generation failure"));
    }

    @Test
    void processWaitingTasksShouldRetryOnlyWaitingItemsAndCompleteTaskWhenAllSucceeded() {
        Long taskId = insertWaitingTask();
        itemMapper.insert(ShowroomProductCoverBatchTaskItemDO.builder()
                .taskId(taskId)
                .productId(1L)
                .sourceRevisionId(1001L)
                .productCode("BATCH-COVER-DONE")
                .nameCn("已完成产品")
                .nameEn("Completed Product")
                .promptFieldsJson("{\"target_market\":\"中国\"}")
                .status("COMPLETED")
                .attemptCount(1)
                .generatedCoverImage("/admin-api/infra/file/29/get/showroom/product/cover/already-done.png")
                .build());
        itemMapper.insert(ShowroomProductCoverBatchTaskItemDO.builder()
                .taskId(taskId)
                .productId(2L)
                .sourceRevisionId(2001L)
                .productCode("BATCH-COVER-RETRY")
                .nameCn("待重试产品")
                .nameEn("Retry Product")
                .promptFieldsJson("{\"target_market\":\"中国\",\"core_selling_points\":\"待重试卖点\"}")
                .status("WAITING")
                .attemptCount(1)
                .lastError("SHOWROOM_COVER_GENERATION_FAILED: previous failure")
                .build());

        mockContentPersistence(Map.of(2L, createSnapshot(2L, "BATCH-COVER-RETRY")),
                Map.of(2L, createRevision(2L, 2001L, 6, "待重试产品", "Retry Product")));
        when(productCoverImageService.generateCoverImage(eq("BATCH-COVER-RETRY"), eq("rendered:待重试产品")))
                .thenReturn("/admin-api/infra/file/29/get/showroom/product/cover/retry-success.png");

        service.processWaitingTasks();

        ShowroomProductCoverBatchTaskDO task = taskMapper.selectById(taskId);
        assertEquals("COMPLETED", task.getStatus());
        assertNull(task.getNextCheckAt());

        List<ShowroomProductCoverBatchTaskItemDO> items = itemMapper.selectListByTaskId(taskId);
        assertEquals(2, items.stream().filter(item -> "COMPLETED".equals(item.getStatus())).count());
        ShowroomProductCoverBatchTaskItemDO retriedItem = items.stream()
                .filter(item -> "BATCH-COVER-RETRY".equals(item.getProductCode()))
                .findFirst()
                .orElseThrow();
        assertEquals(2, retriedItem.getAttemptCount());
        assertEquals("/admin-api/infra/file/29/get/showroom/product/cover/retry-success.png",
                retriedItem.getGeneratedCoverImage());
        verify(productCoverImageService, never()).generateCoverImage(eq("BATCH-COVER-DONE"), any());
    }

    @Test
    void processWaitingTasksShouldRecoverStaleRunningItemsBeforeDeclaringTaskComplete() {
        Long taskId = insertWaitingTask();
        itemMapper.insert(ShowroomProductCoverBatchTaskItemDO.builder()
                .taskId(taskId)
                .productId(2L)
                .sourceRevisionId(2001L)
                .productCode("BATCH-COVER-STALE-RUNNING")
                .nameCn("遗留运行中产品")
                .nameEn("Stale Running Product")
                .promptFieldsJson("{\"target_market\":\"中国\",\"core_selling_points\":\"待恢复卖点\"}")
                .status("RUNNING")
                .attemptCount(1)
                .lastError("SHOWROOM_COVER_GENERATION_FAILED: previous interrupted round")
                .build());

        mockContentPersistence(Map.of(2L, createSnapshot(2L, "BATCH-COVER-STALE-RUNNING")),
                Map.of(2L, createRevision(2L, 2001L, 6, "遗留运行中产品", "Stale Running Product")));
        when(productCoverImageService.generateCoverImage(eq("BATCH-COVER-STALE-RUNNING"),
                eq("rendered:遗留运行中产品")))
                .thenReturn("/admin-api/infra/file/29/get/showroom/product/cover/stale-running-recovered.png");

        service.processWaitingTasks();

        ShowroomProductCoverBatchTaskDO task = taskMapper.selectById(taskId);
        assertEquals("COMPLETED", task.getStatus());
        assertEquals(1, task.getSucceededCount());
        assertEquals(0, task.getFailedCount());
        assertEquals(0, task.getRemainingPendingCount());

        ShowroomProductCoverBatchTaskItemDO item = itemMapper.selectListByTaskId(taskId).stream()
                .filter(current -> "BATCH-COVER-STALE-RUNNING".equals(current.getProductCode()))
                .findFirst()
                .orElseThrow();
        assertEquals("COMPLETED", item.getStatus());
        assertEquals(2, item.getAttemptCount());
        assertEquals("/admin-api/infra/file/29/get/showroom/product/cover/stale-running-recovered.png",
                item.getGeneratedCoverImage());
    }

    @Test
    void resolveExecutionParallelismShouldUseAtMostConfiguredMaximumAndNoMoreThanWaitingItemCount() {
        assertEquals(8, ShowroomProductCoverBatchTaskService.resolveExecutionParallelism(8, 12));
        assertEquals(3, ShowroomProductCoverBatchTaskService.resolveExecutionParallelism(8, 3));
        assertEquals(5, ShowroomProductCoverBatchTaskService.resolveExecutionParallelism(5, 9));
    }

    @Test
    void recoverInterruptedTasksOnStartupShouldMoveRunningTasksBackToWaiting() {
        ShowroomProductCoverBatchTaskDO runningTask = ShowroomProductCoverBatchTaskDO.builder()
                .operatorUserId(300L)
                .status("RUNNING")
                .coverGenerationMode("ALL")
                .promptVersionId(7001L)
                .matchedCount(1)
                .publishedCount(1)
                .skippedUnpublishedCount(0)
                .skippedExistingCount(0)
                .build();
        taskMapper.insert(runningTask);
        itemMapper.insert(ShowroomProductCoverBatchTaskItemDO.builder()
                .taskId(runningTask.getId())
                .productId(1L)
                .sourceRevisionId(1001L)
                .productCode("BATCH-COVER-RECOVER")
                .nameCn("待恢复产品")
                .nameEn("Recover Product")
                .promptFieldsJson("{\"target_market\":\"中国\"}")
                .status("RUNNING")
                .attemptCount(1)
                .lastError("SHOWROOM_COVER_GENERATION_FAILED: interrupted during previous round")
                .build());

        service.recoverInterruptedTasksOnStartup();

        ShowroomProductCoverBatchTaskDO recoveredTask = taskMapper.selectById(runningTask.getId());
        assertEquals("WAITING", recoveredTask.getStatus());
        assertNotNull(recoveredTask.getNextCheckAt());

        ShowroomProductCoverBatchTaskItemDO recoveredItem = itemMapper.selectListByTaskId(runningTask.getId()).stream()
                .findFirst()
                .orElseThrow();
        assertEquals("WAITING", recoveredItem.getStatus());
    }

    @Test
    void processWaitingTasksShouldSucceedWithoutLiveProductPreviewAsset() {
        Long taskId = insertWaitingTask();
        itemMapper.insert(ShowroomProductCoverBatchTaskItemDO.builder()
                .taskId(taskId)
                .productId(2L)
                .sourceRevisionId(2001L)
                .productCode("BATCH-COVER-MISSING-PREVIEW")
                .nameCn("缺预览图产品")
                .nameEn("Missing Preview Product")
                .promptFieldsJson("{\"target_market\":\"中国\",\"core_selling_points\":\"缺预览图卖点\"}")
                .status("WAITING")
                .attemptCount(0)
                .build());

        mockContentPersistence(Map.of(2L, createSnapshot(2L, "BATCH-COVER-MISSING-PREVIEW")),
                Map.of(2L, createRevision(2L, 2001L, 6, "缺预览图产品", "Missing Preview Product")));
        when(productCoverImageService.generateCoverImage(eq("BATCH-COVER-MISSING-PREVIEW"),
                eq("rendered:缺预览图产品")))
                .thenReturn("/admin-api/infra/file/29/get/showroom/product/cover/missing-preview.png");
        doReturn(Optional.empty()).when(previewAssetService).live(any());

        service.processWaitingTasks();

        ShowroomProductCoverBatchTaskDO task = taskMapper.selectById(taskId);
        assertEquals("COMPLETED", task.getStatus());
        assertEquals(1, task.getSucceededCount());
        assertEquals(0, task.getFailedCount());
        assertEquals(0, task.getRemainingPendingCount());
        assertNotNull(task.getCompletedAt());

        ShowroomProductCoverBatchTaskItemDO item = itemMapper.selectListByTaskId(taskId).stream()
                .filter(current -> "BATCH-COVER-MISSING-PREVIEW".equals(current.getProductCode()))
                .findFirst()
                .orElseThrow();
        assertEquals("COMPLETED", item.getStatus());
        assertEquals(1, item.getAttemptCount());
        assertNotNull(item.getCompletedAt());
        assertEquals("/admin-api/infra/file/29/get/showroom/product/cover/missing-preview.png",
                item.getGeneratedCoverImage());
        assertNull(item.getLastError());
    }

    @Test
    void processWaitingTasksShouldTreat503GenerationFailuresAsNonRetryableSkip() {
        Long taskId = insertWaitingTask();
        itemMapper.insert(ShowroomProductCoverBatchTaskItemDO.builder()
                .taskId(taskId)
                .productId(2L)
                .sourceRevisionId(2001L)
                .productCode("BATCH-COVER-503")
                .nameCn("503封面失败产品")
                .nameEn("503 Cover Failure Product")
                .promptFieldsJson("{\"target_market\":\"中国\",\"core_selling_points\":\"503卖点\"}")
                .status("WAITING")
                .attemptCount(0)
                .build());

        mockContentPersistence(Map.of(2L, createSnapshot(2L, "BATCH-COVER-503")),
                Map.of(2L, createRevision(2L, 2001L, 6, "503封面失败产品", "503 Cover Failure Product")));
        when(productCoverImageService.generateCoverImage(eq("BATCH-COVER-503"), eq("rendered:503封面失败产品")))
                .thenThrow(new IllegalStateException(
                        "SHOWROOM_COVER_GENERATION_FAILED: Generation failed: the single native `image_generation` request returned `503 Service temporarily unavailable`"));

        service.processWaitingTasks();

        ShowroomProductCoverBatchTaskDO task = taskMapper.selectById(taskId);
        assertEquals("COMPLETED", task.getStatus());
        assertEquals(0, task.getSucceededCount());
        assertEquals(1, task.getFailedCount());
        assertEquals(0, task.getRemainingPendingCount());

        ShowroomProductCoverBatchTaskItemDO item = itemMapper.selectListByTaskId(taskId).stream()
                .filter(current -> "BATCH-COVER-503".equals(current.getProductCode()))
                .findFirst()
                .orElseThrow();
        assertEquals("FAILED", item.getStatus());
        assertEquals(1, item.getAttemptCount());
        assertNotNull(item.getCompletedAt());
        assertTrue(item.getLastError().contains("503 Service temporarily unavailable"));
    }

    @Test
    void getTaskStateShouldAllowStartWhenNoTaskExists() {
        ShowroomAdminController.ProductCoverBatchTaskStateRespVO state = service.getTaskState();

        assertTrue(state.startAllowed());
        assertEquals(false, state.active());
        assertEquals(false, state.running());
        assertEquals("", state.taskStatus());
        assertEquals(null, state.currentProduct());
    }

    @Test
    void getTaskStateShouldExposeRunningCurrentProduct() {
        ShowroomProductCoverBatchTaskDO runningTask = ShowroomProductCoverBatchTaskDO.builder()
                .operatorUserId(300L)
                .status("RUNNING")
                .keyword("BATCH-COVER")
                .coverGenerationMode("ALL")
                .promptVersionId(7001L)
                .matchedCount(3)
                .publishedCount(3)
                .skippedUnpublishedCount(0)
                .skippedExistingCount(1)
                .succeededCount(1)
                .failedCount(0)
                .remainingPendingCount(2)
                .build();
        taskMapper.insert(runningTask);
        itemMapper.insert(ShowroomProductCoverBatchTaskItemDO.builder()
                .taskId(runningTask.getId())
                .productId(11L)
                .sourceRevisionId(1011L)
                .productCode("BATCH-COVER-RUNNING")
                .nameCn("正在生成封面的产品")
                .nameEn("Running Cover Product")
                .promptFieldsJson("{\"target_market\":\"中国\"}")
                .status("RUNNING")
                .attemptCount(1)
                .build());

        ShowroomAdminController.ProductCoverBatchTaskStateRespVO state = service.getTaskState();

        assertEquals(false, state.startAllowed());
        assertTrue(state.active());
        assertTrue(state.running());
        assertEquals("RUNNING", state.taskStatus());
        assertNotNull(state.currentProduct());
        assertEquals(11L, state.currentProduct().productId());
        assertEquals("BATCH-COVER-RUNNING", state.currentProduct().productCode());
        assertEquals("正在生成封面的产品", state.currentProduct().nameCn());
    }

    private void mockContentPersistence(Map<Long, ShowroomProductSnapshot> snapshots,
                                        Map<Long, ShowroomProductRevision> currentRevisions) {
        AtomicLong generatedRevisionId = new AtomicLong(9000L);
        AtomicLong generatedPreviewVersionId = new AtomicLong(12000L);
        AtomicLong generatedNarrationVersionId = new AtomicLong(24000L);
        Map<Long, ShowroomPreviewAssetVersion> previewDrafts = new HashMap<>();
        Map<Long, ShowroomNarrationVersion> narrationDrafts = new HashMap<>();
        when(contentService.getProduct(any())).thenAnswer(invocation -> snapshots.get(invocation.getArgument(0)));
        when(contentService.requireCurrentProductRevision(any())).thenAnswer(invocation ->
                currentRevisions.get(invocation.getArgument(0)));
        when(previewAssetService.live(any())).thenAnswer(invocation -> {
            ShowroomPreviewAssetKey key = invocation.getArgument(0);
            ShowroomProductRevision currentRevision = currentRevisions.get(key.targetId());
            if (currentRevision == null) {
                return Optional.empty();
            }
            return Optional.of(new ShowroomPreviewAssetVersion(
                    key.targetId() * 10 + 1,
                    key,
                    currentRevision.revisionId(),
                    1,
                    new ShowroomPreviewAssetFiles(88001L, 88001L, 88001L),
                    ShowroomPreviewAssetStatus.PUBLISHED,
                    false,
                    false,
                    null,
                    Instant.now(),
                    true
            ));
        });
        when(previewAssetService.bindStaticPreviewAssets(any(ShowroomPreviewAssetDraftCommand.class))).thenAnswer(invocation -> {
            ShowroomPreviewAssetDraftCommand command = invocation.getArgument(0);
            ShowroomPreviewAssetVersion draft = new ShowroomPreviewAssetVersion(
                    generatedPreviewVersionId.incrementAndGet(),
                    command.key(),
                    command.sourceRevisionId(),
                    1,
                    command.files(),
                    ShowroomPreviewAssetStatus.DRAFT,
                    false,
                    false,
                    null,
                    null,
                    false
            );
            previewDrafts.put(draft.id(), draft);
            return draft;
        });
        when(previewAssetService.publishDirectly(any())).thenAnswer(invocation -> {
            Long versionId = invocation.getArgument(0);
            ShowroomPreviewAssetVersion draft = previewDrafts.get(versionId);
            return draft.withPublication(Instant.now(), true);
        });
        when(narrationService.live(any())).thenAnswer(invocation -> {
            ShowroomNarrationKey key = invocation.getArgument(0);
            ShowroomProductRevision currentRevision = currentRevisions.get(key.targetId());
            if (currentRevision == null) {
                return Optional.empty();
            }
            String scriptText = key.language() == ShowroomNarrationLanguage.ZH ? "中文讲解" : "English narration";
            return Optional.of(new ShowroomNarrationVersion(
                    key.targetId() * 100 + (key.language() == ShowroomNarrationLanguage.ZH ? 1 : 2),
                    key,
                    currentRevision.revisionId(),
                    1,
                    scriptText,
                    key.targetId() * 1000 + (key.language() == ShowroomNarrationLanguage.ZH ? 1 : 2),
                    60,
                    "ruoxi",
                    ShowroomNarrationGenerationStatus.AUDIO_GENERATED,
                    ShowroomNarrationStatus.PUBLISHED,
                    false,
                    null,
                    Instant.now(),
                    true
            ));
        });
        when(narrationService.draftScript(any(ShowroomNarrationDraftCommand.class))).thenAnswer(invocation -> {
            ShowroomNarrationDraftCommand command = invocation.getArgument(0);
            ShowroomNarrationVersion draft = new ShowroomNarrationVersion(
                    generatedNarrationVersionId.incrementAndGet(),
                    new ShowroomNarrationKey(command.targetType(), command.targetId(),
                            command.audienceType(), command.language()),
                    command.sourceRevisionId(),
                    1,
                    command.scriptText(),
                    null,
                    null,
                    null,
                    ShowroomNarrationGenerationStatus.SCRIPT_GENERATED,
                    ShowroomNarrationStatus.DRAFT,
                    command.generatedByAi(),
                    null,
                    null,
                    false
            );
            narrationDrafts.put(draft.id(), draft);
            return draft;
        });
        when(narrationService.attachAudio(any(ShowroomNarrationAudioDraftCommand.class))).thenAnswer(invocation -> {
            ShowroomNarrationAudioDraftCommand command = invocation.getArgument(0);
            ShowroomNarrationVersion draft = narrationDrafts.get(command.narrationVersionId());
            ShowroomNarrationVersion updated = draft.withAudio(command.audioFileId(),
                    command.audioDurationSeconds(), command.voice(),
                    ShowroomNarrationGenerationStatus.AUDIO_GENERATED);
            narrationDrafts.put(updated.id(), updated);
            return updated;
        });
        when(narrationService.publishDirectly(any())).thenAnswer(invocation -> {
            Long narrationVersionId = invocation.getArgument(0);
            ShowroomNarrationVersion draft = narrationDrafts.get(narrationVersionId);
            ShowroomNarrationVersion published = draft.withPublication(Instant.now(), true);
            narrationDrafts.put(published.id(), published);
            return published;
        });
        when(contentService.saveProductDraft(any(ShowroomProductDraft.class))).thenAnswer(invocation -> {
            ShowroomProductDraft draft = invocation.getArgument(0);
            ShowroomProductRevision currentRevision = currentRevisions.get(draft.productId());
            return new ShowroomProductRevision(
                    draft.productId(),
                    generatedRevisionId.incrementAndGet(),
                    currentRevision.revisionNo() + 1,
                    "DRAFT",
                    currentRevision.nameCn(),
                    currentRevision.nameEn(),
                    false,
                    new LinkedHashMap<>(draft.fields())
            );
        });
        when(contentService.publishProductRevision(any(), any())).thenAnswer(invocation -> {
            Long revisionId = invocation.getArgument(0);
            Long operatorUserId = invocation.getArgument(1);
            assertNotNull(operatorUserId);
            return new ShowroomProductRevision(0L, revisionId, 0, "PUBLISHED", "", "", false, Map.of());
        });
    }

    private static ShowroomProductSnapshot createSnapshot(Long productId, String productCode) {
        return new ShowroomProductSnapshot(productId, productCode, Optional.of(productId * 1000 + 1), false, true);
    }

    private static ShowroomProductRevision createRevision(Long productId, Long revisionId, int revisionNo,
                                                          String nameCn, String nameEn) {
        return new ShowroomProductRevision(
                productId,
                revisionId,
                revisionNo,
                "PUBLISHED",
                nameCn,
                nameEn,
                false,
                new LinkedHashMap<>(Map.of("target_market", "中国", "core_selling_points", "卖点"))
        );
    }

    private Long insertWaitingTask() {
        ShowroomProductCoverBatchTaskDO task = ShowroomProductCoverBatchTaskDO.builder()
                .operatorUserId(300L)
                .status("WAITING")
                .coverGenerationMode("ALL")
                .promptVersionId(7001L)
                .matchedCount(2)
                .publishedCount(2)
                .skippedUnpublishedCount(0)
                .skippedExistingCount(0)
                .build();
        taskMapper.insert(task);
        return task.getId();
    }
}
