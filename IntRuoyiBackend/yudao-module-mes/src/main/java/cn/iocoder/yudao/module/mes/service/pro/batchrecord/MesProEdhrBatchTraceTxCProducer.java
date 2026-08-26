package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchTraceOutboxEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrOperationAuditEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchProvisioningRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingItemDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchTraceOutboxEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrOperationAuditEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchProvisioningRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingMapper;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrBatchTraceabilityRespVO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesFlow6CompletionBackfillReceipt;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityBlocker.TRACE_MAPPING_BLOCKED;

/**
 * The only Flow 7 producer for Tx-C. It reads the Flow 6 provision audit and
 * the formal Flow 1 binding snapshot, then delegates immutable graph writes
 * to the traceability service and appends an outbox event in the same Tx.
 */
@Service
@RequiredArgsConstructor
public class MesProEdhrBatchTraceTxCProducer implements MesProEdhrBatchTraceTxCInvoker {

    public static final String SUCCESS_EVENT = "FLOW7_TRACE_MAPPING_SUCCEEDED";
    public static final String RETRYABLE_FAILURE_EVENT = "FLOW7_TRACE_MAPPING_FAILED_RETRYABLE";
    public static final String FINAL_FAILURE_EVENT = "FLOW7_TRACE_MAPPING_FAILED_FINAL";
    public static final String MAPPING_STATUS_CAPTURED = "TRACE_CAPTURED";
    public static final String MAPPING_STATUS_BLOCKED = "TRACE_MAPPING_BLOCKED";

    private static final String PROVISION_OPERATION = "OPEN";
    private static final String BOUND_STATUS = "BOUND";
    private static final Set<String> RETRYABLE_REASONS = Set.of(
            "SOURCE_CHANGED_AFTER_PRECHECK",
            "FORMAL_SOURCE_EVIDENCE_REQUIRED",
            "SOURCE_SNAPSHOT_HASH_MISMATCH",
            "FLOW6_PROVISION_AUDIT_REQUIRED",
            "FLOW1_BINDING_REQUIRED");

    private final MesProEdhrBatchExecutionMapper batchExecutionMapper;
    private final MesProEdhrOperationAuditEventMapper operationAuditEventMapper;
    private final MesProcessPoolActiveOrderPickListBindingMapper bindingMapper;
    private final MesProcessPoolActiveOrderPickListBindingItemMapper bindingItemMapper;
    private final MesProEdhrBatchTraceOutboxEventMapper outboxEventMapper;
    private final MesProEdhrBatchTraceabilityService traceabilityService;
    private final ApplicationEventPublisher eventPublisher;
    private final PlatformTransactionManager transactionManager;
    private final MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort completionReceiptPort;
    private final MesIndependentBatchPrerequisiteReceiptPort independentReceiptPort;
    private final MesProEdhrBatchProvisioningRecordMapper provisioningRecordMapper;

    @Override
    public MesProEdhrBatchTraceTxCResult produce(MesProEdhrBatchTraceTxCCommand command) {
        if (command == null || command.getBatchExecutionId() == null
                || command.getProvisioningReceiptId() == null || isBlank(command.getEventId())
                || isBlank(command.getIdempotencyKey())) {
            throw new IllegalArgumentException("Tx-C requires batchExecutionId, eventId and idempotencyKey");
        }
        try {
            MesProEdhrBatchTraceTxCResult result = new TransactionTemplate(transactionManager).execute(status -> {
                MesProEdhrBatchTraceOutboxEventDO byEvent = outboxEventMapper.selectByEventId(command.getEventId());
                if (byEvent != null) {
                    return toResult(byEvent).setIdempotent(true);
                }
                MesProEdhrBatchTraceOutboxEventDO byKey = outboxEventMapper.selectByIdempotencyKey(command.getIdempotencyKey());
                if (byKey != null) {
                    if (Objects.equals(byKey.getSourceSnapshotHash(), command.getExpectedSourceSnapshotHash())
                            && Objects.equals(byKey.getSourceBundleHash(), command.getExpectedSourceBundleHash())) {
                        return toResult(byKey).setIdempotent(true);
                    }
                    throw blocked("IDEMPOTENCY_WITNESS_CONFLICT",
                            "idempotencyKey already belongs to a different source witness");
                }

                FormalInput first = readFormalInput(command);
                validateWitness(command, first.metadata);
                FormalInput second = readFormalInput(command);
                if (!Objects.equals(first.fingerprint, second.fingerprint)) {
                    throw blocked("SOURCE_CHANGED_AFTER_PRECHECK",
                            "formal source fingerprint changed after precheck");
                }
                MesProEdhrBatchTraceCaptureCommand capture = toCaptureCommand(command, second);
                MesProEdhrBatchTraceabilityRespVOWithLink trace = captureFormalMapping(capture);
                markProvisioningReady(command, second.metadata);
                MesProEdhrBatchTraceOutboxEventDO event = persistSuccess(command, second, trace);
                publishAfterCommit(event);
                return toResult(event);
            });
            return Objects.requireNonNull(result, "Tx-C transaction returned no result");
        } catch (TxCBlockedException ex) {
            return persistFailureInNewTransaction(command, ex.reasonCode, ex.getMessage());
        } catch (RuntimeException ex) {
            return persistFailureInNewTransaction(command, "TRACE_SERVICE_FAILURE", nonBlankMessage(ex));
        }
    }

    private MesProEdhrBatchTraceTxCResult persistFailureInNewTransaction(
            MesProEdhrBatchTraceTxCCommand command, String reasonCode, String reason) {
        MesProEdhrBatchTraceTxCResult result = new TransactionTemplate(transactionManager)
                .execute(status -> persistFailure(command, reasonCode, reason));
        return Objects.requireNonNull(result, "Tx-C failure transaction returned no result");
    }

    private void markProvisioningReady(MesProEdhrBatchTraceTxCCommand command, JSONObject metadata) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long recordId = command.getProvisioningReceiptId();
        MesProEdhrBatchProvisioningRecordDO record = provisioningRecordMapper
                .selectByIdAndTenantId(tenantId, recordId);
        if (record == null || !Objects.equals(record.getBatchExecutionId(), command.getBatchExecutionId())
                || !Objects.equals(record.getSourceSnapshotHash(), metadata.getString("sourceSnapshotHash"))
                || !MesBatchProvisioningStatus.BATCH_PROVISIONING.name().equals(record.getStatus())) {
            throw blocked("BATCH_PROVISIONING_STATE_INVALID", "Flow 6 provisioning state is not ready for Tx-C");
        }
        if (batchExecutionMapper.updateProvisioningStatus(tenantId, command.getBatchExecutionId(),
                MesBatchProvisioningStatus.BATCH_READY.name()) != 1) {
            throw blocked("BATCH_PROVISIONING_STATE_PERSIST_FAILED", "BATCH_READY was not persisted");
        }
        record.setStatus(MesBatchProvisioningStatus.BATCH_READY.name())
                .setMappingEventId(command.getEventId()).setMappingIdempotencyKey(command.getIdempotencyKey())
                .setErrorCode(null);
        if (provisioningRecordMapper.updateById(record) != 1) {
            throw blocked("BATCH_PROVISIONING_STATE_PERSIST_FAILED", "provisioning receipt was not updated");
        }
    }

    private FormalInput readFormalInput(MesProEdhrBatchTraceTxCCommand command) {
        Long batchExecutionId = command.getBatchExecutionId();
        MesProEdhrBatchExecutionDO batch = batchExecutionMapper.selectById(batchExecutionId);
        if (batch == null) {
            throw blocked("FLOW6_PROVISION_AUDIT_REQUIRED", "batchExecutionId does not exist");
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        if (!isTenantVisible(batch.getTenantId(), tenantId)) {
            throw blocked("FLOW6_PROVISION_AUDIT_REQUIRED",
                    "batchExecutionId crosses the current tenant boundary");
        }
        List<MesProEdhrOperationAuditEventDO> audits = operationAuditEventMapper
                .selectSuccessfulListByBatchExecutionIdAndOperation(batchExecutionId, PROVISION_OPERATION);
        if (audits == null || audits.isEmpty()) {
            throw blocked("FLOW6_PROVISION_AUDIT_REQUIRED", "Flow 6 successful provision audit is missing");
        }
        MesProEdhrOperationAuditEventDO audit = audits.get(0);
        JSONObject metadata = parseMetadata(audit.getMetadataJson());
        Long provisioningReceiptId = requireLong(metadata, "batchProvisionReceiptId");
        if (!Objects.equals(provisioningReceiptId, command == null ? null : command.getProvisioningReceiptId())) {
            throw blocked("BATCH_PROVISIONING_WITNESS_MISMATCH",
                    "Tx-C event does not identify the persisted Flow 6 provisioning record");
        }
        MesProEdhrBatchProvisioningRecordDO provisioningRecord = provisioningRecordMapper
                .selectByIdAndTenantId(tenantId, provisioningReceiptId);
        if (provisioningRecord == null
                || !Objects.equals(provisioningRecord.getBatchExecutionId(), batchExecutionId)
                || !MesBatchProvisioningStatus.BATCH_PROVISIONING.name().equals(provisioningRecord.getStatus())) {
            throw blocked("BATCH_PROVISIONING_STATE_INVALID", "Flow 6 provisioning record is not pending Tx-C");
        }
        String entryType = requireText(metadata, "entryType").toUpperCase(Locale.ROOT);
        requireText(metadata, "originKey");
        requireText(metadata, "sourceSnapshotHash");
        requireText(metadata, "sourceVersion");
        JSONArray evidence = metadata.getJSONArray("sourceEvidence");
        MesProcessPoolActiveOrderPickListBindingDO binding = null;
        List<MesProcessPoolActiveOrderPickListBindingItemDO> items = List.of();
        if (MesProEdhrBatchTraceFormalSourceResolver.ACTIVE_ORDER_COMPLETION.equals(entryType)) {
            Long credentialId = requireLong(metadata, "sourceCredentialId");
            MesFlow6CompletionBackfillReceipt receipt = completionReceiptPort.getByReceiptId(credentialId, tenantId);
            evidence = MesProEdhrBatchTraceFormalSourceResolver.resolveActive(tenantId, receipt, metadata, evidence);
            requireLong(metadata, "activeOrderId");
            requireLong(metadata, "workOrderId");
            requireText(metadata, "completionTransactionId");
            requireLong(metadata, "completionVersion");
            requireLong(metadata, "completionBackfillReceiptId");
            Long bindingId = requireLong(metadata, "pickListBindingId");
            requireLong(metadata, "pickListId");
            requireLong(metadata, "bindingVersion");
            binding = bindingMapper.selectById(bindingId);
            if (binding == null || !Objects.equals(binding.getTenantId(), tenantId)) {
                throw blocked("FLOW1_BINDING_REQUIRED", "formal pick-list binding is missing or crosses tenant boundary");
            }
            if (!BOUND_STATUS.equalsIgnoreCase(binding.getBindingStatus())
                    || !Objects.equals(binding.getActiveOrderId(), metadata.getLong("activeOrderId"))
                    || !Objects.equals(binding.getWorkOrderId(), metadata.getLong("workOrderId"))
                    || !Objects.equals(binding.getPickListId(), metadata.getLong("pickListId"))
                    || !Objects.equals(binding.getBindingVersion(), metadata.getInteger("bindingVersion"))) {
                throw blocked("SOURCE_SNAPSHOT_HASH_MISMATCH", "binding snapshot/version does not match provision witness");
            }
            items = bindingItemMapper.selectListByBindingId(bindingId);
            if (items == null || items.isEmpty()) {
                throw blocked("FLOW1_BINDING_REQUIRED", "formal pick-list binding has no immutable line snapshots");
            }
        } else {
            String credentialId = requireText(metadata, "sourceCredentialId");
            MesIndependentBatchPrerequisiteReceipt receipt = independentReceiptPort.getVerifiedByReceiptId(
                    tenantId, credentialId, entryType, metadata.getString("sourceSnapshotHash"));
            evidence = MesProEdhrBatchTraceFormalSourceResolver.resolveIndependent(tenantId, receipt, metadata, evidence);
            requireLong(metadata, "workOrderId");
        }
        Map<String, Object> fingerprintPayload = new LinkedHashMap<>();
        fingerprintPayload.put("auditHash", audit.getAuditHash());
        fingerprintPayload.put("metadata", metadata);
        fingerprintPayload.put("bindingId", binding == null ? null : binding.getId());
        fingerprintPayload.put("bindingVersion", binding == null ? null : binding.getBindingVersion());
        fingerprintPayload.put("bindingHash", binding == null ? null : binding.getSourceSnapshotHash());
        fingerprintPayload.put("itemHashes", items.stream().map(MesProcessPoolActiveOrderPickListBindingItemDO::getItemSnapshotHash).toList());
        fingerprintPayload.put("evidence", evidence);
        String fingerprint = DigestUtil.sha256Hex(
                MesProBatchRecordExecutionFieldAuditHasher.canonicalizeJsonString(
                        JSON.toJSONString(fingerprintPayload)));
        return new FormalInput(metadata, evidence, binding, items, fingerprint);
    }

    private JSONObject parseMetadata(String metadataJson) {
        if (isBlank(metadataJson)) {
            throw blocked("FLOW6_PROVISION_AUDIT_REQUIRED", "Flow 6 provision audit metadata is empty");
        }
        try {
            JSONObject metadata = JSON.parseObject(metadataJson);
            if (metadata == null) {
                throw blocked("FLOW6_PROVISION_AUDIT_REQUIRED", "Flow 6 provision audit metadata is invalid JSON");
            }
            return metadata;
        } catch (TxCBlockedException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw blocked("FLOW6_PROVISION_AUDIT_REQUIRED", "Flow 6 provision audit metadata is invalid JSON");
        }
    }

    private void validateWitness(MesProEdhrBatchTraceTxCCommand command, JSONObject metadata) {
        witness(command.getExpectedSourceSnapshotHash(), metadata.getString("sourceSnapshotHash"), "sourceSnapshotHash");
        witness(command.getExpectedSourceBundleHash(), metadata.getString("sourceBundleHash"), "sourceBundleHash");
        witness(command.getExpectedCompletionBackfillReceiptHash(),
                metadata.getString("completionBackfillReceiptHash"), "completionBackfillReceiptHash");
        witness(command.getExpectedSourceVersion(), metadata.getString("sourceVersion"), "sourceVersion");
    }

    private void witness(String expected, String actual, String field) {
        if (!isBlank(expected) && !Objects.equals(expected, actual)) {
            throw blocked("SOURCE_SNAPSHOT_HASH_MISMATCH", field + " witness does not match formal source");
        }
    }

    private MesProEdhrBatchTraceCaptureCommand toCaptureCommand(MesProEdhrBatchTraceTxCCommand command,
                                                                  FormalInput input) {
        JSONObject metadata = input.metadata;
        List<MesProEdhrBatchTraceSource> sources = new ArrayList<>();
        for (Object raw : input.evidence) {
            if (!(raw instanceof JSONObject evidence)) {
                throw blocked("FORMAL_SOURCE_EVIDENCE_REQUIRED", "sourceEvidence contains a non-object item");
            }
            String sourceType = requiredText(evidence, "sourceType");
            String linkType = toLinkType(sourceType);
            String objectType = nonBlank(evidence.getString("sourceObjectType"), sourceType);
            String objectIdText = nonBlank(evidence.getString("sourceObjectId"), evidence.getString("sourceId"));
            Long objectId = isBlank(objectIdText) ? null : parseLong(objectIdText, "sourceObjectId");
            String snapshotJson = nonBlank(evidence.getString("snapshotJson"),
                    JSON.toJSONString(new java.util.LinkedHashMap<>(evidence)));
            String snapshotHash = requiredText(evidence, "sourceSnapshotHash");
            String calculatedHash = MesProEdhrBatchTraceSourceHash.calculate(linkType, snapshotJson);
            boolean externallyWitnessed = MesProEdhrBatchTraceLinkType.MATERIAL_ISSUE.equals(linkType)
                    || MesProEdhrBatchTraceLinkType.MATERIAL_ISSUE_LINE.equals(linkType)
                    || MesProEdhrBatchTraceLinkType.COMPLETION_BACKFILL_RECEIPT.equals(linkType)
                    || MesProEdhrBatchTraceLinkType.BATCH_PROVISION_RECEIPT.equals(linkType)
                    || MesProEdhrBatchTraceLinkType.LOSS_REPORT_RECEIPT.equals(linkType);
            if (!externallyWitnessed && !calculatedHash.equalsIgnoreCase(snapshotHash)) {
                throw blocked("SOURCE_SNAPSHOT_HASH_MISMATCH", "sourceEvidence snapshot hash mismatch");
            }
            String identity = nonBlank(evidence.getString("sourceIdentityKey"), sourceType + ":" + objectIdText);
            String relationStatus = nonBlank(evidence.getString("relationStatus"), "BOUND");
            sources.add(new MesProEdhrBatchTraceSource()
                    .setLinkType(linkType).setSourceObjectType(objectType).setSourceObjectId(objectId)
                    .setSourceLineId(optionalLong(evidence, "sourceLineId"))
                    .setSourceEventId(optionalLong(evidence, "sourceEventId"))
                    .setSourceVersion(optionalInteger(evidence, "sourceVersion"))
                    .setSourceIdentityKey(identity).setSnapshotJson(snapshotJson).setSnapshotHash(snapshotHash)
                    .setRelationStatus(relationStatus).setRelationReason(evidence.getString("relationReason")));
        }
        String entryType = requiredText(metadata, "entryType").toUpperCase(Locale.ROOT);
        MesProEdhrBatchTraceCaptureCommand capture = new MesProEdhrBatchTraceCaptureCommand()
                .setBatchExecutionId(command.getBatchExecutionId()).setEntryType(entryType)
                .setOriginKey(requiredText(metadata, "originKey"))
                .setWorkOrderId(requiredLong(metadata, "workOrderId"))
                .setSourceSnapshotHash(requiredText(metadata, "sourceSnapshotHash"))
                .setSourceBundleHash(requiredText(metadata, "sourceBundleHash"))
                .setIdempotencyKey(command.getIdempotencyKey())
                .setSourceCredentialId(optionalLong(metadata, "sourceCredentialId"))
                .setSourceCredentialHash(metadata.getString("sourceCredentialHash"))
                .setCapturedBy(command.getCapturedBy()).setSources(sources);
        if (MesProEdhrBatchTraceFormalSourceResolver.ACTIVE_ORDER_COMPLETION.equals(entryType)) {
            capture.setActiveOrderId(requiredLong(metadata, "activeOrderId"))
                    .setCompletionTransactionId(parseLong(requiredText(metadata, "completionTransactionId"), "completionTransactionId"))
                    .setCompletionVersion(requiredInteger(metadata, "completionVersion"))
                    .setCompletionBackfillReceiptId(requiredLong(metadata, "completionBackfillReceiptId"))
                    .setCompletionBackfillReceiptHash(requiredText(metadata, "completionBackfillReceiptHash"))
                    .setPickListBindingId(requiredLong(metadata, "pickListBindingId"))
                    .setPickListId(requiredLong(metadata, "pickListId"))
                    .setPickListBindingVersion(requiredInteger(metadata, "bindingVersion"))
                    .setBatchProvisionReceiptId(requiredLong(metadata, "batchProvisionReceiptId"))
                    .setBatchProvisionStatus(requiredText(metadata, "batchProvisionStatus"))
                    .setHasActualLoss(metadata.getBoolean("hasActualLoss"));
        } else {
            capture.setBatchProvisionReceiptId(optionalLong(metadata, "batchProvisionReceiptId"))
                    .setBatchProvisionStatus(metadata.getString("batchProvisionStatus"));
        }
        return capture;
    }

    private MesProEdhrBatchTraceabilityRespVOWithLink captureFormalMapping(MesProEdhrBatchTraceCaptureCommand capture) {
        var response = traceabilityService.capture(capture);
        List<MesProEdhrBatchTraceabilityRespVO.TraceLink> provisionLinks = response.getTraceLinks().stream()
                .filter(link -> MesProEdhrBatchTraceLinkType.BATCH_PROVISION_RECEIPT.equals(link.getLinkType()))
                .filter(link -> !"NOT_APPLICABLE".equalsIgnoreCase(link.getRelationStatus())).toList();
        if (provisionLinks.size() != 1) {
            throw blocked("TRACE_MAPPING_BLOCKED", "batch provision receipt relation is not unambiguous");
        }
        MesProEdhrBatchTraceabilityRespVO.Manifest manifest = response.getLatestManifest();
        if (manifest == null || manifest.getManifestVersion() == null) {
            throw blocked("TRACE_MAPPING_BLOCKED", "trace manifest was not persisted");
        }
        List<MesProEdhrBatchTraceabilityRespVO.Origin> origins = response.getOrigins().stream()
                .filter(origin -> Objects.equals(origin.getBatchExecutionId(), capture.getBatchExecutionId()))
                .filter(origin -> Objects.equals(origin.getOriginKey(), capture.getOriginKey())).toList();
        if (origins.size() != 1 || origins.get(0).getId() == null) {
            throw blocked("TRACE_MAPPING_BLOCKED", "captured origin is not unambiguous");
        }
        return new MesProEdhrBatchTraceabilityRespVOWithLink(
                response, origins.get(0), provisionLinks.get(0), manifest);
    }

    private MesProEdhrBatchTraceOutboxEventDO persistSuccess(MesProEdhrBatchTraceTxCCommand command,
                                                               FormalInput input,
                                                               MesProEdhrBatchTraceabilityRespVOWithLink trace) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("batchExecutionId", command.getBatchExecutionId());
        payload.put("originId", trace.origin.getId());
        payload.put("originLinkId", trace.provisionLink.getId());
        payload.put("traceLinkHash", trace.provisionLink.getSnapshotHash());
        payload.put("sourceSnapshotHash", input.metadata.getString("sourceSnapshotHash"));
        payload.put("sourceBundleHash", input.metadata.getString("sourceBundleHash"));
        payload.put("sourceVersion", input.metadata.getString("sourceVersion"));
        payload.put("manifestVersion", trace.manifest.getManifestVersion());
        payload.put("sourceEvidence", input.evidence);
        return insertOutbox(command, SUCCESS_EVENT, MAPPING_STATUS_CAPTURED, null,
                "formal source mapping persisted", false, trace.origin.getId(),
                trace.provisionLink.getId(), trace.provisionLink.getSnapshotHash(),
                input.metadata.getString("sourceSnapshotHash"), input.metadata.getString("sourceBundleHash"),
                trace.manifest.getManifestVersion(), payload);
    }

    private MesProEdhrBatchTraceTxCResult persistFailure(MesProEdhrBatchTraceTxCCommand command,
                                                          String reasonCode, String reason) {
        Long batchId = command == null ? null : command.getBatchExecutionId();
        String eventId = command == null ? "tx-c-invalid" : nonBlank(command.getEventId(), "tx-c-invalid");
        String idempotencyKey = command == null ? eventId : nonBlank(command.getIdempotencyKey(), eventId);
        String actualEventId = eventId;
        String actualKey = idempotencyKey;
        if (outboxEventMapper.selectByEventId(actualEventId) != null) {
            return toResult(outboxEventMapper.selectByEventId(actualEventId)).setIdempotent(true);
        }
        if (outboxEventMapper.selectByIdempotencyKey(actualKey) != null) {
            String suffix = DigestUtil.sha256Hex(reasonCode + ":" + reason).substring(0, 16);
            actualEventId = eventId + ":FAIL:" + suffix;
            actualKey = idempotencyKey + ":FAIL:" + suffix;
        }
        boolean retryable = RETRYABLE_REASONS.contains(reasonCode);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("batchExecutionId", batchId);
        payload.put("requestedEventId", eventId);
        payload.put("reasonCode", reasonCode);
        payload.put("reason", reason);
        markProvisioningFailure(command, reasonCode, retryable);
        MesProEdhrBatchTraceOutboxEventDO event = insertOutbox(
                new MesProEdhrBatchTraceTxCCommand().setBatchExecutionId(batchId).setEventId(actualEventId)
                        .setIdempotencyKey(actualKey)
                        .setExpectedSourceSnapshotHash(command == null ? null : command.getExpectedSourceSnapshotHash())
                        .setExpectedSourceBundleHash(command == null ? null : command.getExpectedSourceBundleHash()),
                retryable ? RETRYABLE_FAILURE_EVENT : FINAL_FAILURE_EVENT, MAPPING_STATUS_BLOCKED,
                TRACE_MAPPING_BLOCKED, reason, retryable, null, null, null,
                command == null ? null : command.getExpectedSourceSnapshotHash(),
                command == null ? null : command.getExpectedSourceBundleHash(), null, payload);
        publishAfterCommit(event);
        return toResult(event);
    }

    private void markProvisioningFailure(MesProEdhrBatchTraceTxCCommand command,
                                          String reasonCode, boolean retryable) {
        if (command == null || command.getProvisioningReceiptId() == null) {
            return;
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        MesProEdhrBatchProvisioningRecordDO record = provisioningRecordMapper
                .selectByIdAndTenantId(tenantId, command.getProvisioningReceiptId());
        if (record == null || !Objects.equals(record.getBatchExecutionId(), command.getBatchExecutionId())) {
            throw blocked("BATCH_PROVISIONING_RECORD_REQUIRED", "Flow 6 provisioning receipt is missing");
        }
        String status = retryable ? MesBatchProvisioningStatus.BATCH_PROVISIONING_RETRYABLE.name()
                : MesBatchProvisioningStatus.BATCH_PROVISIONING_BLOCKED.name();
        record.setStatus(status).setErrorCode(reasonCode)
                .setAttemptCount(record.getAttemptCount() == null ? 1 : record.getAttemptCount() + 1);
        if (provisioningRecordMapper.updateById(record) != 1
                || batchExecutionMapper.updateProvisioningStatus(tenantId, command.getBatchExecutionId(), status) != 1) {
            throw blocked("BATCH_PROVISIONING_STATE_PERSIST_FAILED", "provisioning failure state was not persisted");
        }
    }

    private MesProEdhrBatchTraceOutboxEventDO insertOutbox(MesProEdhrBatchTraceTxCCommand command,
                                                             String eventType, String mappingStatus,
                                                             String errorCode, String reason, boolean retryable,
                                                             Long originId, Long originLinkId, String traceLinkHash,
                                                             String sourceSnapshotHash, String sourceBundleHash,
                                                             Integer manifestVersion, Map<String, Object> payload) {
        String payloadJson = JSON.toJSONString(payload);
        MesProEdhrBatchTraceOutboxEventDO event = MesProEdhrBatchTraceOutboxEventDO.builder()
                .tenantId(TenantContextHolder.getRequiredTenantId()).eventId(command.getEventId())
                .idempotencyKey(command.getIdempotencyKey()).batchExecutionId(command.getBatchExecutionId())
                .originId(originId).originLinkId(originLinkId).eventType(eventType).mappingStatus(mappingStatus)
                .errorCode(errorCode).reason(reason).traceLinkHash(traceLinkHash)
                .sourceSnapshotHash(sourceSnapshotHash).sourceBundleHash(sourceBundleHash)
                .manifestVersion(manifestVersion).payloadJson(payloadJson)
                .payloadHash(DigestUtil.sha256Hex(payloadJson)).retryable(retryable)
                .occurredAt(LocalDateTime.now()).build();
        if (outboxEventMapper.insert(event) != 1 || event.getId() == null) {
            throw new IllegalStateException("Tx-C outbox persistence failed");
        }
        return event;
    }

    private void publishAfterCommit(MesProEdhrBatchTraceOutboxEventDO event) {
        MesProEdhrBatchTraceMappingEvent message = new MesProEdhrBatchTraceMappingEvent()
                .setEventId(event.getEventId()).setBatchExecutionId(event.getBatchExecutionId())
                .setOriginId(event.getOriginId()).setOriginLinkId(event.getOriginLinkId())
                .setTraceLinkHash(event.getTraceLinkHash()).setSourceSnapshotHash(event.getSourceSnapshotHash())
                .setManifestVersion(event.getManifestVersion()).setEventType(event.getEventType())
                .setStatus(event.getMappingStatus()).setErrorCode(event.getErrorCode())
                .setReason(event.getReason()).setRetryable(event.getRetryable());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventPublisher.publishEvent(message);
            }
        });
    }

    private MesProEdhrBatchTraceTxCResult toResult(MesProEdhrBatchTraceOutboxEventDO event) {
        return new MesProEdhrBatchTraceTxCResult().setEventId(event.getEventId())
                .setBatchExecutionId(event.getBatchExecutionId()).setOriginId(event.getOriginId())
                .setOriginLinkId(event.getOriginLinkId()).setTraceLinkHash(event.getTraceLinkHash())
                .setSourceSnapshotHash(event.getSourceSnapshotHash()).setManifestVersion(event.getManifestVersion())
                .setStatus(event.getMappingStatus()).setEventType(event.getEventType()).setErrorCode(event.getErrorCode())
                .setReason(event.getReason()).setRetryable(event.getRetryable()).setIdempotent(false);
    }

    public static boolean sourceChanged(String firstFingerprint, String secondFingerprint) {
        return !Objects.equals(firstFingerprint, secondFingerprint);
    }

    public static boolean isRetryableReason(String reasonCode) {
        return RETRYABLE_REASONS.contains(reasonCode);
    }

    private String toLinkType(String sourceType) {
        String normalized = sourceType.trim().toUpperCase(Locale.ROOT);
        List<String> supported = Arrays.asList(
                MesProEdhrBatchTraceLinkType.ACTIVE_ORDER, MesProEdhrBatchTraceLinkType.WORK_ORDER,
                MesProEdhrBatchTraceLinkType.MATERIAL_ISSUE, MesProEdhrBatchTraceLinkType.MATERIAL_ISSUE_LINE,
                MesProEdhrBatchTraceLinkType.PRODUCTION_SUBMIT, MesProEdhrBatchTraceLinkType.PRODUCTION_SIGNATURE,
                MesProEdhrBatchTraceLinkType.PRODUCTION_LEADER_REVIEW, MesProEdhrBatchTraceLinkType.PQC_TASK,
                MesProEdhrBatchTraceLinkType.PQC_SUBMISSION, MesProEdhrBatchTraceLinkType.PQC_SIGNATURE,
                MesProEdhrBatchTraceLinkType.PQC_LEADER_CONFIRMATION, MesProEdhrBatchTraceLinkType.PQC_AGGREGATE_DETAIL,
                MesProEdhrBatchTraceLinkType.LOSS_FACT, MesProEdhrBatchTraceLinkType.NO_LOSS_CONFIRMED,
                MesProEdhrBatchTraceLinkType.LOSS_REPORT_RECEIPT, MesProEdhrBatchTraceLinkType.BATCH_RECORD_RECEIPT,
                MesProEdhrBatchTraceLinkType.COMPLETION_BACKFILL_RECEIPT,
                MesProEdhrBatchTraceLinkType.BATCH_PROVISION_RECEIPT,
                MesProEdhrBatchTraceLinkType.PROCESS_INSPECTION_RECEIPT);
        if (!supported.contains(normalized)) {
            throw blocked("FORMAL_SOURCE_EVIDENCE_REQUIRED", "unsupported formal sourceType: " + sourceType);
        }
        return normalized;
    }

    private String requireText(JSONObject object, String key) {
        return requiredText(object, key);
    }

    private String requiredText(JSONObject object, String key) {
        String value = object.getString(key);
        if (isBlank(value)) {
            throw blocked("FORMAL_SOURCE_EVIDENCE_REQUIRED", "missing formal field: " + key);
        }
        return value;
    }

    private Long requireLong(JSONObject object, String key) {
        return parseLong(requiredText(object, key), key);
    }

    private Long requiredLong(JSONObject object, String key) {
        return requireLong(object, key);
    }

    private Integer requiredInteger(JSONObject object, String key) {
        return Integer.valueOf(requiredText(object, key));
    }

    private Long optionalLong(JSONObject object, String key) {
        return object.containsKey(key) && object.get(key) != null ? parseLong(object.getString(key), key) : null;
    }

    private Integer optionalInteger(JSONObject object, String key) {
        return object.containsKey(key) && object.get(key) != null ? Integer.valueOf(object.getString(key)) : null;
    }

    private Long parseLong(String value, String key) {
        try {
            return Long.valueOf(value);
        } catch (RuntimeException ex) {
            throw blocked("FORMAL_SOURCE_EVIDENCE_REQUIRED", "formal field is not a valid long: " + key);
        }
    }

    private static TxCBlockedException blocked(String reasonCode, String reason) {
        return new TxCBlockedException(reasonCode, reason);
    }

    private static String nonBlank(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private static String nonBlankMessage(RuntimeException ex) {
        return isBlank(ex.getMessage()) ? "trace mapping service failed" : ex.getMessage();
    }

    static boolean isTenantVisible(Long batchTenantId, Long currentTenantId) {
        return batchTenantId != null && currentTenantId != null
                && Objects.equals(batchTenantId, currentTenantId);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static final class FormalInput {
        private final JSONObject metadata;
        private final JSONArray evidence;
        private final MesProcessPoolActiveOrderPickListBindingDO binding;
        private final List<MesProcessPoolActiveOrderPickListBindingItemDO> items;
        private final String fingerprint;

        private FormalInput(JSONObject metadata, JSONArray evidence,
                            MesProcessPoolActiveOrderPickListBindingDO binding,
                            List<MesProcessPoolActiveOrderPickListBindingItemDO> items,
                            String fingerprint) {
            this.metadata = metadata;
            this.evidence = evidence;
            this.binding = binding;
            this.items = items;
            this.fingerprint = fingerprint;
        }
    }

    private static final class MesProEdhrBatchTraceabilityRespVOWithLink {
        private final cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrBatchTraceabilityRespVO response;
        private final cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrBatchTraceabilityRespVO.Origin origin;
        private final cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrBatchTraceabilityRespVO.TraceLink provisionLink;
        private final cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrBatchTraceabilityRespVO.Manifest manifest;

        private MesProEdhrBatchTraceabilityRespVOWithLink(
                cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrBatchTraceabilityRespVO response,
                cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrBatchTraceabilityRespVO.Origin origin,
                cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrBatchTraceabilityRespVO.TraceLink provisionLink,
                cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrBatchTraceabilityRespVO.Manifest manifest) {
            this.response = response;
            this.origin = origin;
            this.provisionLink = provisionLink;
            this.manifest = manifest;
        }
    }

    private static final class TxCBlockedException extends RuntimeException {
        private final String reasonCode;

        private TxCBlockedException(String reasonCode, String message) {
            super(message);
            this.reasonCode = reasonCode;
        }
    }
}
