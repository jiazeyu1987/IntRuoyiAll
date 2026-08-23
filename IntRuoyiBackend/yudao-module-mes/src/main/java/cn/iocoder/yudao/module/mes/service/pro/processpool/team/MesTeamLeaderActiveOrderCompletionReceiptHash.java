package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptDO;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Canonical hash for the immutable Flow-4 receipt body, excluding database metadata and the hash itself. */
public final class MesTeamLeaderActiveOrderCompletionReceiptHash {

    private MesTeamLeaderActiveOrderCompletionReceiptHash() {
    }

    public static String compute(MesProcessPoolActiveOrderCompletionReceiptDO receipt) {
        if (receipt == null) {
            throw new IllegalArgumentException("receipt is required");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("activeOrderId", receipt.getActiveOrderId());
        body.put("workOrderId", receipt.getWorkOrderId());
        body.put("batchCode", receipt.getBatchCode());
        body.put("routeId", receipt.getRouteId());
        body.put("routeVersionId", receipt.getRouteVersionId());
        body.put("leaderUserId", receipt.getLeaderUserId());
        body.put("requestIdempotencyKey", receipt.getRequestIdempotencyKey());
        body.put("requestPayloadHash", receipt.getRequestPayloadHash());
        body.put("sourceSnapshotHash", receipt.getSourceSnapshotHash());
        body.put("formalSourceSnapshotJson", receipt.getFormalSourceSnapshotJson());
        body.put("signatureSnapshotJson", receipt.getSignatureSnapshotJson());
        body.put("expectedVersion", receipt.getExpectedVersion());
        body.put("completedVersion", receipt.getCompletedVersion());
        body.put("receiptStatus", receipt.getReceiptStatus());
        body.put("completionStatus", receipt.getCompletionStatus());
        body.put("batchRecordStatus", receipt.getBatchRecordStatus());
        body.put("processInspectionStatus", receipt.getProcessInspectionStatus());
        body.put("lossReportStatus", receipt.getLossReportStatus());
        body.put("hasActualLoss", receipt.getHasActualLoss());
        body.put("lossQuantity", receipt.getLossQuantity());
        body.put("lossRecordId", receipt.getLossRecordId());
        body.put("zeroLossConfirmationSnapshot", receipt.getZeroLossConfirmationSnapshot());
        body.put("lossConditionFactsJson", receipt.getLossConditionFactsJson());
        body.put("batchRecordSourceIdsJson", receipt.getBatchRecordSourceIdsJson());
        body.put("processInspectionSourceIdsJson", receipt.getProcessInspectionSourceIdsJson());
        body.put("lossSourceHash", receipt.getLossSourceHash());
        body.put("provisionHandoff", receipt.getProvisionHandoff());
        body.put("completedAt", receipt.getCompletedAt());
        body.put("completedBy", receipt.getCompletedBy());
        return sha256(JsonUtils.toJsonString(body));
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                result.append(String.format("%02x", item));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is unavailable", exception);
        }
    }
}
