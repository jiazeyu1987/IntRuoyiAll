package cn.iocoder.yudao.module.showroom.narration;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShowroomAudioGenerationContractTest {

    @Test
    void missingAudioAdapterShouldFailExplicitlyWithoutGeneratedAudio() {
        ShowroomNarrationService service = ShowroomNarrationService.withoutAudioAdapter(
                Clock.fixed(Instant.parse("2026-05-19T00:00:00Z"), ZoneOffset.UTC));
        ShowroomNarrationVersion scriptDraft = service.draftScript(new ShowroomNarrationDraftCommand(
                ShowroomNarrationTargetType.HALL, 12L, 620L,
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.ZH,
                "Hall narration script", true));

        ShowroomNarrationException exception = assertThrows(ShowroomNarrationException.class,
                () -> service.generateAudio(scriptDraft.id()));

        assertEquals("SHOWROOM_AUDIO_GENERATION_FAILED", exception.code());
        assertTrue(exception.getMessage().contains("audio generation adapter contract is missing"));
        ShowroomNarrationVersion unchanged = service.version(scriptDraft.id());
        assertEquals(ShowroomNarrationGenerationStatus.SCRIPT_GENERATED, unchanged.generationStatus());
        assertNull(unchanged.audioFileId());
        assertTrue(service.live(scriptDraft.key()).isEmpty());
    }
}
