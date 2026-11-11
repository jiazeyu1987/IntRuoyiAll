package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MesActiveOrderExecutionOperationFactsTest {
    private static final LocalDateTime SUBMITTED = LocalDateTime.of(2026, 11, 10, 9, 0);
    private static final LocalDateTime REVIEWED = SUBMITTED.plusMinutes(3);

    @Test void productionAndPqcKeepActualPeopleTimeAndChineseProcessRound() {
        var production = new MesTeamLeaderActiveOrderDetail.SubmissionDetail().setEventId(11L)
                .setOriginalBusinessIdentityJson(identity(11L, 21L, 31L, 41L))
                .setSubmittedAt(SUBMITTED).setSubmitterSignature(signature(31L, "生产员工", SUBMITTED))
                .setReviewerSignature(signature(41L, "包装组长", REVIEWED));
        var pqc = new MesTeamLeaderActiveOrderDetail.PqcSubmissionDetail().setQaProcessName("中大包装")
                .setInspectionType("PATROL").setRoundNo(3).setSubmittedEventIds(List.of(12L))
                .setOriginalBusinessIdentities(List.of(identity(12L, 22L, 32L, 42L)))
                .setSubmitterSignatures(List.of(signature(32L, "包装检验员", SUBMITTED)))
                .setReviewerSignatures(List.of(signature(42L, "包装检验组长", REVIEWED)));
        var detail = new MesTeamLeaderActiveOrderDetail().setProcesses(List.of(
                new MesTeamLeaderActiveOrderDetail.ProcessDetail().setProcessName("大包装").setSubmissions(List.of(production))))
                .setPqcSubmissions(List.of(pqc));
        var facts = MesActiveOrderExecutionOperationFacts.project(detail);
        assertEquals(4, facts.size());
        assertEquals("提交生产记录：大包装", facts.get(0).getOperationName());
        assertEquals("生产员工", facts.get(0).getActorName());
        assertEquals(101L, facts.get(0).getActorUserId());
        assertEquals(SUBMITTED, facts.get(0).getOccurredAt());
        assertEquals("包装组长", facts.get(1).getActorName());
        assertEquals(REVIEWED, facts.get(1).getOccurredAt());
        assertEquals("复核过程检验记录：中大包装（第3轮巡检）", facts.get(3).getOperationName());
        assertTrue(facts.stream().allMatch(f -> f.getActorUserId() != null && f.getOccurredAt() != null));
        // Same persisted inspection shown in two process panels is one operation.
        detail.setPqcSubmissions(List.of(pqc, pqc));
        assertEquals(4, MesActiveOrderExecutionOperationFacts.project(detail).size());
    }

    @Test void pendingReviewDoesNotInventAnApproval() {
        var json = JSON.parseObject(identity(11L, 21L, 31L, 41L));
        json.remove("reviewSignatureId"); json.remove("reviewId"); json.remove("reviewActorId"); json.remove("reviewStatus");
        var submission = new MesTeamLeaderActiveOrderDetail.SubmissionDetail().setEventId(11L)
                .setOriginalBusinessIdentityJson(json.toJSONString()).setSubmittedAt(SUBMITTED)
                .setSubmitterSignature(signature(31L, "生产员工", SUBMITTED));
        var detail = new MesTeamLeaderActiveOrderDetail().setProcesses(List.of(
                new MesTeamLeaderActiveOrderDetail.ProcessDetail().setProcessName("小包装").setSubmissions(List.of(submission))));
        assertEquals(1, MesActiveOrderExecutionOperationFacts.project(detail).size());
        submission.setSubmitterSignature(signature(99L, "他人", SUBMITTED));
        assertThrows(IllegalStateException.class, () -> MesActiveOrderExecutionOperationFacts.project(detail));
    }

    @Test void earlierRejectedReviewRemainsVisibleAfterApprovalWithoutInventingHistoricalName() {
        var json = JSON.parseObject(identity(11L, 21L, 31L, 41L));
        var previous = JSON.parseObject(identity(11L, 20L, 31L, 40L));
        previous.put("reviewStatus", "REJECTED");
        previous.put("reviewActorId", 103L);
        json.put("reviewHistory", List.of(previous, JSON.parseObject(identity(11L, 21L, 31L, 41L))));
        var submission = new MesTeamLeaderActiveOrderDetail.SubmissionDetail().setEventId(11L)
                .setOriginalBusinessIdentityJson(json.toJSONString()).setSubmittedAt(SUBMITTED)
                .setSubmitterSignature(signature(31L, "生产员工", SUBMITTED))
                .setReviewerSignature(signature(41L, "包装组长", REVIEWED));
        var detail = new MesTeamLeaderActiveOrderDetail().setProcesses(List.of(
                new MesTeamLeaderActiveOrderDetail.ProcessDetail().setProcessName("大包装").setSubmissions(List.of(submission))));
        var facts = MesActiveOrderExecutionOperationFacts.project(detail);
        assertEquals(3, facts.size());
        var rejected = facts.stream().filter(f -> "REJECTED".equals(f.getResultStatus())).findFirst().orElseThrow();
        assertEquals(103L, rejected.getActorUserId());
        assertEquals("操作人编号：103", rejected.getActorName());
    }

    private static String identity(long event, long review, long submitSignature, long reviewSignature) {
        return JSON.toJSONString(Map.of("eventId", event, "actorId", 101L, "signatureId", submitSignature,
                "reviewId", review, "reviewActorId", 102L, "reviewSignatureId", reviewSignature, "reviewStatus", "APPROVED",
                "submittedAt", SUBMITTED.toString(), "reviewedAt", REVIEWED.toString()));
    }
    private static MesTeamLeaderActiveOrderDetail.SignatureDetail signature(long id, String name, LocalDateTime time) {
        return new MesTeamLeaderActiveOrderDetail.SignatureDetail().setSignatureId(id).setSignerName(name).setSignedAt(time).setRole("FORMAL");
    }
}
