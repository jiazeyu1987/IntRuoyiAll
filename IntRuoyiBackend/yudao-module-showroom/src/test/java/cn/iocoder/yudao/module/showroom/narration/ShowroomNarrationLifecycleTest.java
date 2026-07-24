package cn.iocoder.yudao.module.showroom.narration;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShowroomNarrationLifecycleTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-05-19T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void scriptDraftsShouldVersionByTargetAudienceAndLanguage() {
        ShowroomNarrationService service = ShowroomNarrationService.withoutAudioAdapter(FIXED_CLOCK);

        ShowroomNarrationVersion zhV1 = service.draftScript(new ShowroomNarrationDraftCommand(
                ShowroomNarrationTargetType.PRODUCT, 100L, 900L,
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.ZH,
                "ZH script v1", true));
        ShowroomNarrationVersion enV1 = service.draftScript(new ShowroomNarrationDraftCommand(
                ShowroomNarrationTargetType.PRODUCT, 100L, 900L,
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.EN,
                "EN script v1", true));
        ShowroomNarrationVersion zhV2 = service.draftScript(new ShowroomNarrationDraftCommand(
                ShowroomNarrationTargetType.PRODUCT, 100L, 901L,
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.ZH,
                "ZH script v2", false));

        assertEquals(1, zhV1.versionNo());
        assertEquals(1, enV1.versionNo());
        assertEquals(2, zhV2.versionNo());
        assertEquals(new ShowroomNarrationKey(ShowroomNarrationTargetType.PRODUCT, 100L,
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.ZH), zhV2.key());
        assertEquals(ShowroomNarrationGenerationStatus.SCRIPT_GENERATED, zhV1.generationStatus());
        assertEquals(ShowroomNarrationStatus.DRAFT, zhV1.status());
        assertFalse(zhV1.live());
    }

    @Test
    void publishShouldRequireApprovalAndKeepOneLiveNarrationPerTargetLanguageAudience() {
        ShowroomNarrationService service = ShowroomNarrationService.withoutAudioAdapter(FIXED_CLOCK);
        ShowroomNarrationVersion oldDraft = service.draftScript(new ShowroomNarrationDraftCommand(
                ShowroomNarrationTargetType.COMPANY, 200L, 500L,
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.ZH,
                "Old ZH script", false));
        ShowroomNarrationVersion oldDraftWithAudio = service.attachAudio(
                new ShowroomNarrationAudioDraftCommand(oldDraft.id(), 7001L, 65));

        ShowroomNarrationException unapproved = assertThrows(ShowroomNarrationException.class,
                () -> service.publish(oldDraftWithAudio.id()));
        assertEquals("SHOWROOM_NARRATION_NOT_APPROVED", unapproved.code());
        assertTrue(service.live(oldDraftWithAudio.key()).isEmpty());

        ShowroomNarrationVersion oldLive = service.publish(service.gaoxinApprove(
                service.supervisorApprove(service.submit(oldDraftWithAudio.id()).id(), 300L).id(), 400L).id());
        assertTrue(oldLive.live());
        assertEquals(ShowroomNarrationStatus.PUBLISHED, oldLive.status());

        ShowroomNarrationVersion newDraft = service.draftScript(new ShowroomNarrationDraftCommand(
                ShowroomNarrationTargetType.COMPANY, 200L, 501L,
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.ZH,
                "New ZH script", false));
        newDraft = service.attachAudio(new ShowroomNarrationAudioDraftCommand(newDraft.id(), 7002L, 70));
        ShowroomNarrationVersion newLive = service.publish(service.gaoxinApprove(
                service.supervisorApprove(service.submit(newDraft.id()).id(), 301L).id(), 401L).id());

        assertFalse(service.version(oldLive.id()).live());
        assertTrue(newLive.live());
        assertEquals(newLive.id(), service.live(newLive.key()).orElseThrow().id());
        assertEquals(7002L, service.live(newLive.key()).orElseThrow().audioFileId());
    }

    @Test
    void languageSwitchingShouldChangeOnlyNarrationTextAndAudioContract() {
        ShowroomNarrationService service = ShowroomNarrationService.withoutAudioAdapter(FIXED_CLOCK);
        ShowroomNarrationVersion zhLive = approvedLive(service, ShowroomNarrationLanguage.ZH,
                "ZH public script", 8101L);
        ShowroomNarrationVersion enLive = approvedLive(service, ShowroomNarrationLanguage.EN,
                "EN public script", 8102L);

        assertEquals(zhLive.key().targetType(), enLive.key().targetType());
        assertEquals(zhLive.key().targetId(), enLive.key().targetId());
        assertEquals(zhLive.sourceRevisionId(), enLive.sourceRevisionId());
        assertNotEquals(zhLive.scriptText(), enLive.scriptText());
        assertNotEquals(zhLive.audioFileId(), enLive.audioFileId());
        assertEquals(zhLive.id(), service.live(zhLive.key()).orElseThrow().id());
        assertEquals(enLive.id(), service.live(enLive.key()).orElseThrow().id());
    }

    @Test
    void latestNarrationShouldTrackLatestVersionPerLanguageOnly() {
        ShowroomNarrationService service = ShowroomNarrationService.withoutAudioAdapter(FIXED_CLOCK);
        ShowroomNarrationKey zhKey = new ShowroomNarrationKey(ShowroomNarrationTargetType.PRODUCT, 301L,
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.ZH);
        ShowroomNarrationKey enKey = new ShowroomNarrationKey(ShowroomNarrationTargetType.PRODUCT, 301L,
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.EN);

        ShowroomNarrationVersion zhV1 = service.draftScript(new ShowroomNarrationDraftCommand(
                zhKey.targetType(), zhKey.targetId(), 610L, zhKey.audienceType(), zhKey.language(), "ZH v1", false));
        service.attachAudio(new ShowroomNarrationAudioDraftCommand(zhV1.id(), 8301L, 91, "ruoxi"));

        ShowroomNarrationVersion enV1 = service.draftScript(new ShowroomNarrationDraftCommand(
                enKey.targetType(), enKey.targetId(), 610L, enKey.audienceType(), enKey.language(), "EN v1", false));
        service.attachAudio(new ShowroomNarrationAudioDraftCommand(enV1.id(), 8302L, 92, "xiaoyun"));

        ShowroomNarrationVersion zhV2 = service.draftScript(new ShowroomNarrationDraftCommand(
                zhKey.targetType(), zhKey.targetId(), 611L, zhKey.audienceType(), zhKey.language(), "ZH v2", false));

        assertEquals(zhV2.id(), service.latest(zhKey).orElseThrow().id());
        assertEquals(enV1.id(), service.latest(enKey).orElseThrow().id());
    }

    private static ShowroomNarrationVersion approvedLive(ShowroomNarrationService service,
                                                         ShowroomNarrationLanguage language,
                                                         String scriptText,
                                                         Long audioFileId) {
        ShowroomNarrationVersion draft = service.draftScript(new ShowroomNarrationDraftCommand(
                ShowroomNarrationTargetType.PRODUCT, 300L, 600L,
                ShowroomNarrationAudienceType.PUBLIC, language, scriptText, false));
        draft = service.attachAudio(new ShowroomNarrationAudioDraftCommand(draft.id(), audioFileId, 90, "ruoxi"));
        return service.publish(service.gaoxinApprove(
                service.supervisorApprove(service.submit(draft.id()).id(), 310L).id(), 410L).id());
    }
}
