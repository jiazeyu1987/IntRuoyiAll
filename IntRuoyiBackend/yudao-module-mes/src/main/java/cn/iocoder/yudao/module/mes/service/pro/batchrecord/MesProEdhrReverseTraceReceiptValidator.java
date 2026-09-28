package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionOriginDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptDO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderCompletionReceiptHash;
import java.util.Objects;

/** Shared immutable completion receipt contract; category-specific facts remain with their adapters. */
final class MesProEdhrReverseTraceReceiptValidator {
    private MesProEdhrReverseTraceReceiptValidator() { }

    static void validate(MesProEdhrBatchExecutionDO batch, MesProEdhrBatchExecutionOriginDO origin,
                         MesProcessPoolActiveOrderCompletionReceiptDO receipt) {
        if (batch == null || batch.getId() == null || batch.getTenantId() == null || batch.getWorkOrderId() == null
                || batch.getRouteId() == null || batch.getRouteVersionId() == null
                || origin == null || origin.getBatchExecutionId() == null || origin.getTenantId() == null
                || origin.getActiveOrderId() == null || origin.getWorkOrderId() == null
                || origin.getCompletionBackfillReceiptId() == null || origin.getCompletionVersion() == null
                || StrUtil.isBlank(origin.getCompletionBackfillReceiptHash()) || StrUtil.isBlank(origin.getSourceSnapshotHash())
                || receipt == null || receipt.getId() == null || receipt.getTenantId() == null
                || receipt.getActiveOrderId() == null || receipt.getWorkOrderId() == null
                || receipt.getRouteId() == null || receipt.getRouteVersionId() == null) {
            throw missing("批次、来源或完工回执身份及冻结绑定缺失");
        }
        if (!Objects.equals(batch.getId(), origin.getBatchExecutionId())
                || !Objects.equals(batch.getTenantId(), origin.getTenantId())
                || !Objects.equals(batch.getTenantId(), receipt.getTenantId())
                || !Objects.equals(batch.getWorkOrderId(), origin.getWorkOrderId())
                || !Objects.equals(batch.getWorkOrderId(), receipt.getWorkOrderId())
                || !Objects.equals(batch.getRouteId(), receipt.getRouteId())
                || !Objects.equals(batch.getRouteVersionId(), receipt.getRouteVersionId())
                || !Objects.equals(origin.getActiveOrderId(), receipt.getActiveOrderId())
                || !Objects.equals(origin.getCompletionBackfillReceiptId(), receipt.getId())) {
            throw conflict("完工回执与批次正式身份不一致");
        }
        if (receipt.getReceiptStatus() == null || receipt.getCompletionStatus() == null
                || receipt.getBatchRecordStatus() == null || receipt.getProcessInspectionStatus() == null
                || receipt.getCompletedVersion() == null || receipt.getBatchRecordId() == null
                || receipt.getProcessInspectionId() == null || StrUtil.isBlank(receipt.getFormalSourceSnapshotJson())
                || StrUtil.isBlank(receipt.getLossConditionFactsJson()) || StrUtil.isBlank(receipt.getSourceSnapshotHash())
                || StrUtil.isBlank(receipt.getReceiptHash())) {
            throw missing("完工回执状态、物化身份、冻结版本或哈希缺失");
        }
        if (!MesProcessPoolActiveOrderCompletionReceiptDO.RECEIPT_STATUS_BACKFILL_SUCCEEDED.equals(receipt.getReceiptStatus())
                || !MesProcessPoolActiveOrderCompletionReceiptDO.STATUS_SUCCESS.equals(receipt.getCompletionStatus())
                || !MesProcessPoolActiveOrderCompletionReceiptDO.BACKFILL_STATUS_SUCCESS.equals(receipt.getBatchRecordStatus())
                || !MesProcessPoolActiveOrderCompletionReceiptDO.BACKFILL_STATUS_SUCCESS.equals(receipt.getProcessInspectionStatus())
                || receipt.getCompletedVersion() <= 0
                || !Objects.equals(origin.getCompletionVersion(), receipt.getCompletedVersion())
                || !Objects.equals(origin.getCompletionBackfillReceiptHash(), receipt.getReceiptHash())
                || !Objects.equals(origin.getSourceSnapshotHash(), receipt.getSourceSnapshotHash())) {
            throw conflict("完工回执状态、冻结版本或绑定哈希冲突");
        }
        if (!Objects.equals(receipt.getSourceSnapshotHash(), DigestUtil.sha256Hex(
                DigestUtil.sha256Hex(receipt.getFormalSourceSnapshotJson()) + "|" + receipt.getLossConditionFactsJson()))) {
            throw conflict("完工正式来源快照哈希冲突");
        }
        final String actualHash;
        try {
            actualHash = MesTeamLeaderActiveOrderCompletionReceiptHash.compute(receipt);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("SOURCE_CONFLICT:完工回执JSON无法校验", exception);
        }
        if (!Objects.equals(receipt.getReceiptHash(), actualHash)) {
            throw conflict("完工正式回执正文哈希冲突");
        }
        validateSnapshotBinding(receipt);
    }

    private static void validateSnapshotBinding(MesProcessPoolActiveOrderCompletionReceiptDO receipt) {
        final JsonNode snapshot;
        try {
            snapshot = JsonUtils.getObjectMapper().readTree(receipt.getFormalSourceSnapshotJson());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("SOURCE_CONFLICT:完工正式快照JSON无法解析", exception);
        }
        if (snapshot == null || !snapshot.isObject()) {
            throw conflict("完工正式快照根节点不是对象");
        }
        JsonNode activeOrder = requiredBinding(snapshot, "activeOrderBinding");
        JsonNode workOrder = requiredBinding(snapshot, "workOrderBinding");
        requireIdentity(activeOrder, "id", receipt.getActiveOrderId());
        requireIdentity(activeOrder, "tenantId", receipt.getTenantId());
        requireIdentity(activeOrder, "workOrderId", receipt.getWorkOrderId());
        requireIdentity(activeOrder, "routeId", receipt.getRouteId());
        requireIdentity(activeOrder, "routeVersionId", receipt.getRouteVersionId());
        requireIdentity(workOrder, "id", receipt.getWorkOrderId());
    }

    private static JsonNode requiredBinding(JsonNode snapshot, String name) {
        JsonNode binding = snapshot.get(name);
        if (binding == null || binding.isNull()) throw missing("完工正式快照缺少公共绑定 " + name);
        if (!binding.isObject()) throw conflict("完工正式快照公共绑定不是对象 " + name);
        return binding;
    }

    private static void requireIdentity(JsonNode binding, String name, Long expected) {
        JsonNode value = binding.get(name);
        if (value == null || value.isNull()) throw missing("完工正式快照公共身份缺失 " + name);
        // NumberSerializer writes large Longs as their exact decimal text, without coercion.
        boolean matches = value.isTextual() ? expected.toString().equals(value.textValue())
                : value.isIntegralNumber() && value.canConvertToLong() && value.longValue() == expected.longValue();
        if (!matches) {
            throw conflict("完工正式快照公共身份冲突 " + name);
        }
    }

    private static IllegalStateException missing(String reason) {
        return new IllegalStateException("SOURCE_MISSING:" + reason);
    }

    private static IllegalStateException conflict(String reason) {
        return new IllegalStateException("SOURCE_CONFLICT:" + reason);
    }
}
