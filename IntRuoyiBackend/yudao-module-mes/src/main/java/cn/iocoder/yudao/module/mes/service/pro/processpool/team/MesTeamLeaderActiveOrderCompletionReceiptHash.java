package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptDO;
import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Canonical hash for the immutable Flow-4 receipt body, excluding database metadata and the hash itself. */
public final class MesTeamLeaderActiveOrderCompletionReceiptHash {

    private MesTeamLeaderActiveOrderCompletionReceiptHash() {
    }

    public static String compute(MesProcessPoolActiveOrderCompletionReceiptDO receipt) {
        return sha256(snapshotJson(receipt));
    }

    public static String snapshotJson(MesProcessPoolActiveOrderCompletionReceiptDO receipt) {
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
        body.put("formalSourceSnapshotJson", canonicalJson(receipt.getFormalSourceSnapshotJson()));
        body.put("signatureSnapshotJson", canonicalJson(receipt.getSignatureSnapshotJson()));
        body.put("expectedVersion", receipt.getExpectedVersion());
        body.put("completedVersion", receipt.getCompletedVersion());
        body.put("receiptStatus", receipt.getReceiptStatus());
        body.put("completionStatus", receipt.getCompletionStatus());
        body.put("batchRecordStatus", receipt.getBatchRecordStatus());
        body.put("processInspectionStatus", receipt.getProcessInspectionStatus());
        body.put("batchRecordId", receipt.getBatchRecordId());
        body.put("processInspectionId", receipt.getProcessInspectionId());
        body.put("lossReportStatus", receipt.getLossReportStatus());
        body.put("hasActualLoss", receipt.getHasActualLoss());
        body.put("lossQuantity", canonicalDecimal(receipt.getLossQuantity()));
        body.put("lossRecordId", receipt.getLossRecordId());
        body.put("zeroLossConfirmationSnapshot", canonicalJson(receipt.getZeroLossConfirmationSnapshot()));
        body.put("lossConditionFactsJson", canonicalJson(receipt.getLossConditionFactsJson()));
        body.put("batchRecordSourceIdsJson", canonicalJson(receipt.getBatchRecordSourceIdsJson()));
        body.put("processInspectionSourceIdsJson", canonicalJson(receipt.getProcessInspectionSourceIdsJson()));
        body.put("lossSourceHash", receipt.getLossSourceHash());
        body.put("provisionHandoff", receipt.getProvisionHandoff());
        body.put("completedAt", canonicalDateTime(receipt.getCompletedAt()));
        body.put("completedBy", receipt.getCompletedBy());
        return JsonUtils.toJsonString(body);
    }

    private static LocalDateTime canonicalDateTime(LocalDateTime value) {
        return value == null ? null : value.truncatedTo(ChronoUnit.SECONDS);
    }

    private static String canonicalDecimal(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    private static Object canonicalJson(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        try {
            return canonicalNode(JsonUtils.getObjectMapper().readTree(value));
        } catch (Exception ex) {
            throw new IllegalArgumentException("receipt json field is invalid", ex);
        }
    }

    private static Object canonicalNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isObject()) {
            List<String> fieldNames = new ArrayList<>();
            Iterator<String> iterator = node.fieldNames();
            while (iterator.hasNext()) {
                fieldNames.add(iterator.next());
            }
            fieldNames.sort(String::compareTo);
            Map<String, Object> result = new LinkedHashMap<>();
            for (String fieldName : fieldNames) {
                result.put(fieldName, canonicalNode(node.get(fieldName)));
            }
            return result;
        }
        if (node.isArray()) {
            List<Object> result = new ArrayList<>();
            for (JsonNode child : node) {
                result.add(canonicalNode(child));
            }
            return result;
        }
        if (node.isNumber()) {
            return canonicalDecimal(node.decimalValue());
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        return node.asText();
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
