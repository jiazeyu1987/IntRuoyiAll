package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import java.time.LocalDateTime;
import java.util.*;

/** Projects persisted submission/review evidence; never creates or backdates audit events. */
final class MesActiveOrderExecutionOperationFacts {
    private MesActiveOrderExecutionOperationFacts() { }

    static List<MesTeamLeaderActiveOrderDetail.OperationFact> project(MesTeamLeaderActiveOrderDetail detail) {
        Map<String, MesTeamLeaderActiveOrderDetail.OperationFact> facts = new LinkedHashMap<>();
        for (var process : detail.getProcesses()) {
            for (var submission : process.getSubmissions()) {
                if (simulation(submission.getSubmitterSignature())) continue;
                var identity = identity(submission.getOriginalBusinessIdentityJson());
                if (!Objects.equals(submission.getEventId(), identity.getLong("eventId")))
                    throw new IllegalStateException("生产操作日志与正式提交不一致");
                add(facts, identity, submission.getSubmitterSignature(), false, "PRODUCTION_SUBMIT",
                        "提交生产记录：" + process.getProcessName(), submission.getSubmittedAt(), submission.getSubmitterName());
                add(facts, identity, submission.getReviewerSignature(), true, "PRODUCTION_REVIEW",
                        "复核生产记录：" + process.getProcessName(), null, submission.getReviewerName());
                addEarlierReviews(facts, identity, "PRODUCTION_REVIEW", "复核生产记录：" + process.getProcessName());
            }
        }
        for (var submission : detail.getPqcSubmissions()) {
            if (submission.getOriginalBusinessIdentities().isEmpty()) continue;
            if (!submission.getSubmitterSignatures().isEmpty()
                    && submission.getSubmitterSignatures().stream().allMatch(MesActiveOrderExecutionOperationFacts::simulation)) continue;
            String inspection = switch (submission.getInspectionType()) {
                case "FIRST" -> "首检";
                case "PATROL" -> "第" + submission.getRoundNo() + "轮巡检";
                case "FINAL" -> "末检";
                default -> throw new IllegalStateException("检验日志的检验类型无效");
            };
            for (var json : submission.getOriginalBusinessIdentities()) {
                var identity = identity(json);
                add(facts, identity, signature(submission.getSubmitterSignatures(), identity.getLong("signatureId")),
                        false, "PQC_SUBMIT", "提交过程检验记录：" + submission.getQaProcessName() + "（" + inspection + "）", null, null);
                add(facts, identity, signature(submission.getReviewerSignatures(), identity.getLong("reviewSignatureId")),
                        true, "PQC_REVIEW", "复核过程检验记录：" + submission.getQaProcessName() + "（" + inspection + "）", null, null);
                addEarlierReviews(facts, identity, "PQC_REVIEW", "复核过程检验记录：" + submission.getQaProcessName() + "（" + inspection + "）");
            }
        }
        return List.copyOf(facts.values());
    }

    private static JSONObject identity(String json) {
        var value = JSON.parseObject(json);
        if (value == null || value.getLong("eventId") == null || value.getLong("actorId") == null)
            throw new IllegalStateException("操作日志缺少正式提交身份");
        return value;
    }

    private static MesTeamLeaderActiveOrderDetail.SignatureDetail signature(
            List<MesTeamLeaderActiveOrderDetail.SignatureDetail> signatures, Long id) {
        if (id == null) return null;
        var matches = signatures.stream().filter(s -> Objects.equals(id, s.getSignatureId())).toList();
        if (matches.size() > 1) throw new IllegalStateException("操作日志的正式签名身份不唯一");
        return matches.isEmpty() ? null : matches.get(0);
    }

    private static void add(Map<String, MesTeamLeaderActiveOrderDetail.OperationFact> facts, JSONObject identity,
                            MesTeamLeaderActiveOrderDetail.SignatureDetail signature, boolean review,
                            String operation, String action, LocalDateTime submittedAt, String actorName) {
        if (review && identity.getLong("reviewId") == null) return;
        if (simulation(signature)) return;
        Long id = identity.getLong(review ? "reviewId" : "eventId");
        Long actor = identity.getLong(review ? "reviewActorId" : "actorId");
        Long signatureId = identity.getLong(review ? "reviewSignatureId" : "signatureId");
        String time = identity.getString(review ? "reviewedAt" : "submittedAt");
        LocalDateTime occurredAt = submittedAt == null && time != null ? LocalDateTime.parse(time) : submittedAt;
        if (id == null || actor == null || occurredAt == null)
            throw new IllegalStateException("操作日志缺少原始操作人或发生时间");
        if (signature != null && !Objects.equals(signatureId, signature.getSignatureId()))
            throw new IllegalStateException("操作日志与正式签名身份不一致");
        // A legacy business row can identify its actor even when no signed display name was retained.
        // Keep that absence explicit; do not infer a historical name from today's user directory.
        String name = signature == null ? actorName : signature.getSignerName();
        if (name == null || name.isBlank()) name = "操作人编号：" + actor;
        String status = review ? identity.getString("reviewStatus") : "SUCCESS";
        if (review && !Set.of("APPROVED", "REJECTED").contains(status))
            throw new IllegalStateException("操作日志的复核结果无效");
        String source = review ? "SUBMISSION_REVIEW" : "PROCESS_POOL_EVENT";
        var fact = new MesTeamLeaderActiveOrderDetail.OperationFact().setId(id).setSourceType(source)
                .setSourceId(id.toString()).setOperationType(operation).setOperationName(action)
                .setActorUserId(actor).setActorName(name).setOccurredAt(occurredAt)
                .setResultStatus("REJECTED".equals(status) ? "REJECTED" : "SUCCESS");
        facts.putIfAbsent(source + ":" + id, fact);
    }

    private static boolean simulation(MesTeamLeaderActiveOrderDetail.SignatureDetail signature) {
        return signature != null && "SIMULATION_SESSION".equals(signature.getRole());
    }

    private static void addEarlierReviews(Map<String, MesTeamLeaderActiveOrderDetail.OperationFact> facts,
                                          JSONObject identity, String operation, String action) {
        var history = identity.getJSONArray("reviewHistory");
        if (history == null) return;
        for (int i = 0; i < history.size(); i++) {
            var review = history.getJSONObject(i);
            if (!Objects.equals(identity.getLong("eventId"), review.getLong("eventId")))
                throw new IllegalStateException("复核日志与原始提交不一致");
            add(facts, review, null, true, operation, action, null, null);
        }
    }
}
