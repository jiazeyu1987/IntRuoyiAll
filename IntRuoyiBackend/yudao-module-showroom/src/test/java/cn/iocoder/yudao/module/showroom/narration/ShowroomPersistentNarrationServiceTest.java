package cn.iocoder.yudao.module.showroom.narration;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.showroom.dal.mysql.narration.ShowroomNarrationVersionMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import({ShowroomPersistentNarrationService.class, ShowroomPersistentNarrationServiceTest.AudioAdapterConfig.class})
class ShowroomPersistentNarrationServiceTest extends BaseDbUnitTest {

    private static final Long TEST_TENANT_ID = 1L;

    @Resource
    private ShowroomPersistentNarrationService narrationService;

    @Resource
    private ShowroomNarrationVersionMapper narrationVersionMapper;

    @BeforeEach
    void setUpTenantContext() {
        TenantContextHolder.setTenantId(TEST_TENANT_ID);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @TestConfiguration
    static class AudioAdapterConfig {

        @Bean
        ShowroomAudioGenerationAdapter showroomAudioGenerationAdapter() {
            return request -> new ShowroomGeneratedAudio(9101L, 77);
        }

    }

    @Test
    void publishedNarrationShouldSurviveFreshServiceInstance() {
        ShowroomNarrationKey key = new ShowroomNarrationKey(ShowroomNarrationTargetType.COMPANY, 1L,
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.ZH);

        ShowroomNarrationVersion draft = narrationService.draftScript(new ShowroomNarrationDraftCommand(
                key.targetType(), key.targetId(), 101L, key.audienceType(), key.language(), "公司讲解版本一", false));
        draft = narrationService.attachAudio(new ShowroomNarrationAudioDraftCommand(draft.id(), 9001L, 60));
        ShowroomNarrationVersion published = narrationService.publish(narrationService.gaoxinApprove(
                narrationService.supervisorApprove(narrationService.submit(draft.id()).id(), 30L).id(), 40L).id());

        ShowroomPersistentNarrationService restartedService = new ShowroomPersistentNarrationService(
                narrationVersionMapper);
        ShowroomNarrationVersion live = restartedService.live(key).orElseThrow();

        assertEquals(published.id(), live.id());
        assertEquals("公司讲解版本一", live.scriptText());
        assertEquals(9001L, live.audioFileId());
        assertTrue(live.live());
    }

    @Test
    void latestPublishedNarrationShouldWinAfterFreshServiceInstance() {
        ShowroomNarrationKey key = new ShowroomNarrationKey(ShowroomNarrationTargetType.COMPANY, 1L,
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.ZH);

        ShowroomNarrationVersion v1 = narrationService.draftScript(new ShowroomNarrationDraftCommand(
                key.targetType(), key.targetId(), 101L, key.audienceType(), key.language(), "公司讲解版本一", false));
        v1 = narrationService.attachAudio(new ShowroomNarrationAudioDraftCommand(v1.id(), 9001L, 60));
        narrationService.publish(narrationService.gaoxinApprove(
                narrationService.supervisorApprove(narrationService.submit(v1.id()).id(), 30L).id(), 40L).id());

        ShowroomNarrationVersion v2 = narrationService.draftScript(new ShowroomNarrationDraftCommand(
                key.targetType(), key.targetId(), 102L, key.audienceType(), key.language(), "公司讲解版本二", false));
        v2 = narrationService.attachAudio(new ShowroomNarrationAudioDraftCommand(v2.id(), 9002L, 65));
        ShowroomNarrationVersion latestPublished = narrationService.publish(narrationService.gaoxinApprove(
                narrationService.supervisorApprove(narrationService.submit(v2.id()).id(), 31L).id(), 41L).id());

        ShowroomPersistentNarrationService restartedService = new ShowroomPersistentNarrationService(
                narrationVersionMapper);
        ShowroomNarrationVersion live = restartedService.live(key).orElseThrow();

        assertEquals(latestPublished.id(), live.id());
        assertEquals("公司讲解版本二", live.scriptText());
        assertEquals(9002L, live.audioFileId());
        assertTrue(restartedService.version(latestPublished.id()).live());
        assertFalse(restartedService.version(v1.id()).live());
    }

    @Test
    void generateAudioShouldUseInjectedAdapterAndPersistAudioMetadata() {
        ShowroomNarrationVersion draft = narrationService.draftScript(new ShowroomNarrationDraftCommand(
                ShowroomNarrationTargetType.PRODUCT, 88L, 103L,
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.ZH,
                "产品讲解脚本", true));

        ShowroomNarrationVersion generated = narrationService.generateAudio(draft.id());

        assertEquals(9101L, generated.audioFileId());
        assertEquals(77, generated.audioDurationSeconds());
        assertEquals(ShowroomNarrationGenerationStatus.AUDIO_GENERATED, generated.generationStatus());
        assertEquals(generated.audioFileId(), narrationVersionMapper.selectById(generated.id()).getAudioFileId());
    }

    @Test
    void latestNarrationShouldKeepVoiceAndIgnoreOtherLanguage() {
        ShowroomNarrationKey zhKey = new ShowroomNarrationKey(ShowroomNarrationTargetType.PRODUCT, 8L,
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.ZH);
        ShowroomNarrationKey enKey = new ShowroomNarrationKey(ShowroomNarrationTargetType.PRODUCT, 8L,
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.EN);

        ShowroomNarrationVersion zhV1 = narrationService.draftScript(new ShowroomNarrationDraftCommand(
                zhKey.targetType(), zhKey.targetId(), 501L, zhKey.audienceType(), zhKey.language(), "中文讲解 V1", true));
        narrationService.attachAudio(new ShowroomNarrationAudioDraftCommand(zhV1.id(), 9201L, 60, "ruoxi"));

        ShowroomNarrationVersion enV1 = narrationService.draftScript(new ShowroomNarrationDraftCommand(
                enKey.targetType(), enKey.targetId(), 501L, enKey.audienceType(), enKey.language(), "English narration", true));
        narrationService.attachAudio(new ShowroomNarrationAudioDraftCommand(enV1.id(), 9202L, 61, "xiaoyun"));

        ShowroomNarrationVersion zhLatest = narrationService.draftScript(new ShowroomNarrationDraftCommand(
                zhKey.targetType(), zhKey.targetId(), 502L, zhKey.audienceType(), zhKey.language(), "中文讲解 V2", false));
        narrationService.attachAudio(new ShowroomNarrationAudioDraftCommand(zhLatest.id(), 9203L, 62, "ruoxi"));

        ShowroomPersistentNarrationService restartedService = new ShowroomPersistentNarrationService(
                narrationVersionMapper);

        ShowroomNarrationVersion latestZh = restartedService.latest(zhKey).orElseThrow();
        ShowroomNarrationVersion latestEn = restartedService.latest(enKey).orElseThrow();

        assertEquals(zhLatest.id(), latestZh.id());
        assertEquals("ruoxi", latestZh.voice());
        assertEquals(enV1.id(), latestEn.id());
        assertEquals("xiaoyun", latestEn.voice());
    }

    @Test
    void latestPublishedNarrationShouldFilterBySourceRevisionAndIgnoreNewerDraft() {
        ShowroomNarrationKey key = new ShowroomNarrationKey(ShowroomNarrationTargetType.COMPANY, 18L,
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.ZH);

        ShowroomNarrationVersion publishedV1 = narrationService.draftScript(new ShowroomNarrationDraftCommand(
                key.targetType(), key.targetId(), 701L, key.audienceType(), key.language(), "历史版本已发布讲解", false));
        publishedV1 = narrationService.attachAudio(new ShowroomNarrationAudioDraftCommand(publishedV1.id(), 9301L, 60));
        publishedV1 = narrationService.publish(narrationService.gaoxinApprove(
                narrationService.supervisorApprove(narrationService.submit(publishedV1.id()).id(), 30L).id(), 40L).id());

        ShowroomNarrationVersion newerDraftSameSource = narrationService.draftScript(new ShowroomNarrationDraftCommand(
                key.targetType(), key.targetId(), 701L, key.audienceType(), key.language(), "历史版本草稿讲解", false));
        narrationService.attachAudio(new ShowroomNarrationAudioDraftCommand(newerDraftSameSource.id(), 9302L, 62));

        ShowroomNarrationVersion publishedV2 = narrationService.draftScript(new ShowroomNarrationDraftCommand(
                key.targetType(), key.targetId(), 702L, key.audienceType(), key.language(), "当前版本已发布讲解", false));
        publishedV2 = narrationService.attachAudio(new ShowroomNarrationAudioDraftCommand(publishedV2.id(), 9303L, 64));
        publishedV2 = narrationService.publish(narrationService.gaoxinApprove(
                narrationService.supervisorApprove(narrationService.submit(publishedV2.id()).id(), 31L).id(), 41L).id());

        ShowroomPersistentNarrationService restartedService = new ShowroomPersistentNarrationService(
                narrationVersionMapper);

        ShowroomNarrationVersion latestPublishedSource701 = restartedService.latestPublished(key, 701L).orElseThrow();
        ShowroomNarrationVersion latestPublishedSource702 = restartedService.latestPublished(key, 702L).orElseThrow();

        assertEquals(publishedV1.id(), latestPublishedSource701.id());
        assertEquals("历史版本已发布讲解", latestPublishedSource701.scriptText());
        assertEquals(publishedV2.id(), latestPublishedSource702.id());
        assertEquals("当前版本已发布讲解", latestPublishedSource702.scriptText());
    }

}
