package cn.iocoder.yudao.module.showroom.asset;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.showroom.dal.mysql.asset.ShowroomPreviewAssetVersionMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(ShowroomPersistentPreviewAssetService.class)
class ShowroomPreviewAssetLifecycleTest extends BaseDbUnitTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-05-19T00:00:00Z"), ZoneOffset.UTC);

    @Resource
    private ShowroomPersistentPreviewAssetService persistentPreviewAssetService;

    @Resource
    private ShowroomPreviewAssetVersionMapper previewAssetVersionMapper;

    @Test
    void staticPreviewAssetBindingShouldNotInvokeRuntimeGeneration() {
        ShowroomPreviewAssetService service = new ShowroomPreviewAssetService(FIXED_CLOCK);
        ShowroomPreviewAssetFiles files = new ShowroomPreviewAssetFiles(1001L, 1002L, 1003L);

        ShowroomPreviewAssetVersion draft = service.bindStaticPreviewAssets(new ShowroomPreviewAssetDraftCommand(
                ShowroomPreviewAssetTargetType.PRODUCT, 77L, 880L, files));

        assertEquals(1, draft.versionNo());
        assertEquals(ShowroomPreviewAssetStatus.DRAFT, draft.status());
        assertEquals(files, draft.files());
        assertFalse(draft.runtimeGenerationRequested());
        assertFalse(draft.live());
    }

    @Test
    void previewAssetsShouldRequireApprovalAndKeepOneLiveAssetPerTarget() {
        ShowroomPreviewAssetService service = new ShowroomPreviewAssetService(FIXED_CLOCK);
        ShowroomPreviewAssetVersion oldDraft = service.bindStaticPreviewAssets(new ShowroomPreviewAssetDraftCommand(
                ShowroomPreviewAssetTargetType.HALL, 9L, 991L,
                new ShowroomPreviewAssetFiles(2101L, 2102L, 2103L)));

        ShowroomPreviewAssetException unapproved = assertThrows(ShowroomPreviewAssetException.class,
                () -> service.publish(oldDraft.id()));
        assertEquals("SHOWROOM_PREVIEW_ASSET_NOT_APPROVED", unapproved.code());
        assertTrue(service.live(oldDraft.key()).isEmpty());

        ShowroomPreviewAssetVersion oldLive = service.publish(service.gaoxinApprove(
                service.supervisorApprove(service.submit(oldDraft.id()).id(), 800L).id(), 900L).id());
        assertTrue(oldLive.live());

        ShowroomPreviewAssetVersion newDraft = service.bindStaticPreviewAssets(new ShowroomPreviewAssetDraftCommand(
                ShowroomPreviewAssetTargetType.HALL, 9L, 992L,
                new ShowroomPreviewAssetFiles(2201L, 2202L, 2203L)));
        ShowroomPreviewAssetVersion newLive = service.publish(service.gaoxinApprove(
                service.supervisorApprove(service.submit(newDraft.id()).id(), 801L).id(), 901L).id());

        assertFalse(service.version(oldLive.id()).live());
        assertTrue(newLive.live());
        assertEquals(newLive.id(), service.live(newLive.key()).orElseThrow().id());
        assertEquals(new ShowroomPreviewAssetFiles(2201L, 2202L, 2203L),
                service.live(newLive.key()).orElseThrow().files());
    }

    @Test
    void staticPreviewAssetBindingShouldRequireAllDeviceFileReferences() {
        ShowroomPreviewAssetService service = new ShowroomPreviewAssetService(FIXED_CLOCK);

        ShowroomPreviewAssetException exception = assertThrows(ShowroomPreviewAssetException.class,
                () -> service.bindStaticPreviewAssets(new ShowroomPreviewAssetDraftCommand(
                        ShowroomPreviewAssetTargetType.COMPANY, 3L, 701L,
                        new ShowroomPreviewAssetFiles(3101L, null, 3103L))));

        assertEquals("SHOWROOM_PREVIEW_STATIC_ASSET_MISSING", exception.code());
    }

    @Test
    void publishedPreviewAssetShouldSurviveFreshServiceInstanceAndExposeLiveImageFileId() {
        ShowroomPreviewAssetKey key = new ShowroomPreviewAssetKey(ShowroomPreviewAssetTargetType.PRODUCT, 188L);
        ShowroomPreviewAssetVersion draft = persistentPreviewAssetService.bindStaticPreviewAssets(
                new ShowroomPreviewAssetDraftCommand(key.targetType(), key.targetId(), 991L,
                        new ShowroomPreviewAssetFiles(6101L, 6102L, 6103L)));
        ShowroomPreviewAssetVersion published = persistentPreviewAssetService.publish(
                persistentPreviewAssetService.gaoxinApprove(
                        persistentPreviewAssetService.supervisorApprove(
                                persistentPreviewAssetService.submit(draft.id()).id(), 800L).id(), 900L).id());

        ShowroomPersistentPreviewAssetService restartedService = new ShowroomPersistentPreviewAssetService(
                previewAssetVersionMapper);
        ShowroomPreviewAssetVersion live = restartedService.live(key).orElseThrow();

        assertEquals(published.id(), live.id());
        assertEquals(6101L, restartedService.liveImageFileId(key).orElseThrow());
        assertEquals(6101L, live.files().desktopFileId());
        assertNull(live.files().mobileFileId());
        assertNull(live.files().padFileId());
        assertTrue(live.live());
    }
}
