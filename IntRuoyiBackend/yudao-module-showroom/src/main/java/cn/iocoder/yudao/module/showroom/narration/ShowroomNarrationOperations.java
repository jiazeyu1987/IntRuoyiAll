package cn.iocoder.yudao.module.showroom.narration;

import java.util.Optional;

public interface ShowroomNarrationOperations {

    ShowroomNarrationVersion draftScript(ShowroomNarrationDraftCommand command);

    ShowroomNarrationVersion attachAudio(ShowroomNarrationAudioDraftCommand command);

    ShowroomNarrationVersion generateAudio(Long narrationVersionId);

    ShowroomNarrationVersion submit(Long narrationVersionId);

    ShowroomNarrationVersion supervisorApprove(Long narrationVersionId, Long supervisorUserId);

    ShowroomNarrationVersion gaoxinApprove(Long narrationVersionId, Long gaoxinApproverUserId);

    ShowroomNarrationVersion publish(Long narrationVersionId);

    ShowroomNarrationVersion publishDirectly(Long narrationVersionId);

    Optional<ShowroomNarrationVersion> latest(ShowroomNarrationKey key);

    Optional<ShowroomNarrationVersion> live(ShowroomNarrationKey key);

    ShowroomNarrationVersion version(Long narrationVersionId);

    Optional<ShowroomNarrationVersion> latest(ShowroomNarrationKey key, Long sourceRevisionId);

    Optional<ShowroomNarrationVersion> latestPublished(ShowroomNarrationKey key, Long sourceRevisionId);

}
