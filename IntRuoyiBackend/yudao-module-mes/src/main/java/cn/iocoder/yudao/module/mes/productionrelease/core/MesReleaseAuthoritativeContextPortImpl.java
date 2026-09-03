package cn.iocoder.yudao.module.mes.productionrelease.core;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionOriginDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionBackfillDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionOriginMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionBackfillMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrBatchTraceSourcePrecheckRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrBatchTraceabilityRespVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchExecutionPickListSource;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceSourcePrecheckCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceLinkType;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseServiceImpl;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionServiceImpl;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesIndependentBatchPrerequisiteReceipt;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesIndependentBatchPrerequisiteReceiptService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesFlow6CompletionBackfillReceipt;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Objects;

/**
 * Loads the final-release evidence from the persisted Flow-4/6/8 owners.
 * The HTTP request only identifies the manager task and signoff; it cannot provide release evidence.
 */
@Service
public class MesReleaseAuthoritativeContextPortImpl implements MesReleaseAuthoritativeContextPort {

    private final MesProEdhrReleaseTransactionMapper releaseTransactionMapper;
    private final MesProEdhrBatchExecutionMapper batchExecutionMapper;
    private final MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper;
    private final MesProEdhrBatchExecutionOriginMapper originMapper;
    private final MesProcessPoolActiveOrderMapper activeOrderMapper;
    private final MesProcessPoolActiveOrderCompletionBackfillMapper backfillMapper;
    private final MesProcessPoolActiveOrderPickListBindingMapper pickListBindingMapper;
    private final MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort completionReceiptPort;
    private final ObjectProvider<MesReleaseMaterialGateReceiptPort> materialGateReceiptPort;
    private final MesProEdhrBatchTraceabilityService traceabilityService;
    private final MesIndependentBatchPrerequisiteReceiptService independentReceiptService;

    public MesReleaseAuthoritativeContextPortImpl(
            MesProEdhrReleaseTransactionMapper releaseTransactionMapper,
            MesProEdhrBatchExecutionMapper batchExecutionMapper,
            MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper,
            MesProEdhrBatchExecutionOriginMapper originMapper,
            MesProcessPoolActiveOrderMapper activeOrderMapper,
            MesProcessPoolActiveOrderCompletionBackfillMapper backfillMapper,
            MesProcessPoolActiveOrderPickListBindingMapper pickListBindingMapper,
            MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort completionReceiptPort,
            ObjectProvider<MesReleaseMaterialGateReceiptPort> materialGateReceiptPort,
            MesProEdhrBatchTraceabilityService traceabilityService,
            MesIndependentBatchPrerequisiteReceiptService independentReceiptService) {
        this.releaseTransactionMapper = releaseTransactionMapper;
        this.batchExecutionMapper = batchExecutionMapper;
        this.applicationMapper = applicationMapper;
        this.originMapper = originMapper;
        this.activeOrderMapper = activeOrderMapper;
        this.backfillMapper = backfillMapper;
        this.pickListBindingMapper = pickListBindingMapper;
        this.completionReceiptPort = completionReceiptPort;
        this.materialGateReceiptPort = materialGateReceiptPort;
        this.traceabilityService = traceabilityService;
        this.independentReceiptService = independentReceiptService;
    }

    @Override
    public MesReleaseFinalizationEvidence require(MesReleaseFinalizationCommand command) {
        if (command == null || command.getReleaseTransactionId() == null) {
            throw blocker(null, "releaseTransactionId is required");
        }
        MesProEdhrReleaseTransactionDO transaction = releaseTransactionMapper
                .selectById(command.getReleaseTransactionId());
        List<MesProcessPoolActiveOrderReleaseApplicationDO> applications = applicationMapper
                .selectListByReleaseTransactionId(command.getReleaseTransactionId());
        if (transaction == null || applications.size() != 1) {
            throw blocker(null, "release transaction must have exactly one authoritative release application");
        }
        MesProcessPoolActiveOrderReleaseApplicationDO application = applications.get(0);
        if (!MesReleaseFlowStatus.MANAGER_RELEASE_PENDING.equals(application.getApplicationStatus())
                || !MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL.equals(transaction.getReleaseStatus())
                || !Objects.equals(transaction.getBatchExecutionId(), application.getBatchExecutionId())
                || application.getBatchExecutionId() == null) {
            throw blocker(application, "release application and transaction are not pending manager approval");
        }
        requireMatches(command.getReleaseApplicationId(), application.getId(), application,
                "releaseApplicationId does not match the authoritative application");
        requireMatches(command.getBatchExecutionId(), application.getBatchExecutionId(), application,
                "batchExecutionId does not match the authoritative application");
        requireMatches(command.getWorkTaskId(), application.getReleaseApprovalWorkTaskId(), application,
                "workTaskId does not match the frozen manager task");

        Long batchExecutionId = application.getBatchExecutionId();
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        MesProEdhrBatchExecutionDO batch = batchExecutionMapper.selectById(batchExecutionId);
        if (batch == null || batch.getStatus() == null
                || (batch.getTenantId() != null && !Objects.equals(batch.getTenantId(), tenantId))
                || !"BATCH_READY".equals(batch.getProvisioningStatus())
                || !(Objects.equals(batch.getStatus(), MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_READY_TO_CLOSE)
                || Objects.equals(batch.getStatus(), MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CLOSED))) {
            throw blocker(application, "flow 6 batch execution is not BATCH_READY");
        }
        String entryType = command.getEntryType() != null ? command.getEntryType()
                : command.getOrigin() == null ? null : command.getOrigin().name();
        List<MesProEdhrBatchExecutionOriginDO> allOrigins =
                originMapper.selectListByBatchExecutionId(batchExecutionId);
        String persistedEntryType;
        List<MesProEdhrBatchExecutionOriginDO> origins;
        if (isBlank(entryType)) {
            origins = allOrigins.stream()
                    .filter(origin -> application.getActiveOrderId() == null
                            ? origin.getActiveOrderId() == null
                            : Objects.equals(origin.getEntryType(), "ACTIVE_ORDER_COMPLETION")
                            && Objects.equals(origin.getActiveOrderId(), application.getActiveOrderId()))
                    .toList();
            if (origins.size() != 1) {
                throw blocker(application, "formal Flow 7 origin must uniquely identify the release entry");
            }
            persistedEntryType = origins.get(0).getEntryType();
            entryType = persistedEntryType;
        } else {
            persistedEntryType = MesReleaseOrigin.ACTIVE_ORDER.name().equals(entryType)
                    ? "ACTIVE_ORDER_COMPLETION" : entryType;
            origins = allOrigins.stream()
                .filter(origin -> Objects.equals(origin.getEntryType(), persistedEntryType))
                .filter(origin -> "ACTIVE_ORDER_COMPLETION".equals(persistedEntryType)
                        ? Objects.equals(origin.getActiveOrderId(), application.getActiveOrderId())
                        : origin.getActiveOrderId() == null)
                .toList();
        }
        if (origins.size() != 1) {
            throw blocker(application, "formal Flow 7 origin must be unique for the release entry");
        }
        MesProEdhrBatchExecutionOriginDO origin = origins.get(0);
        MesProEdhrBatchTraceSourcePrecheckRespVO source = traceabilityService.resolveSourcePrecheck(
                new MesProEdhrBatchTraceSourcePrecheckCommand().setBatchExecutionId(batchExecutionId));
        requireTracePrecheck(source, origin, batchExecutionId, application);

        MesReleaseMaterialGateReceipt gateReceipt = loadMaterialGateReceipt(
                tenantId, batchExecutionId, command.getMaterialGateReceiptId(), source.getSourceSnapshotHash(), application);
        MesReleaseFinalizationEvidence evidence = new MesReleaseFinalizationEvidence()
                .setMaterialGateReceipt(gateReceipt);
        if ("ACTIVE_ORDER_COMPLETION".equals(persistedEntryType)) {
            MesProcessPoolActiveOrderDO activeOrder = activeOrderMapper.selectById(origin.getActiveOrderId());
            if (activeOrder == null || origin.getCompletionBackfillReceiptId() == null
                    || origin.getCompletionVersion() == null || isBlank(origin.getSourceSnapshotHash())) {
                throw blocker(application, "formal active-order completion source is incomplete");
            }
            MesFlow6CompletionBackfillReceipt flow6Receipt = completionReceiptPort.getByReceiptId(
                    origin.getCompletionBackfillReceiptId(), tenantId);
            requireFlow6Receipt(origin, flow6Receipt, application);
            requireBackfills(origin, application);
            List<MesBatchExecutionPickListSource> pickListSources = resolvePickListSources(
                    origin, flow6Receipt, traceabilityService.getTraceability(batchExecutionId), application, tenantId);
            CompletionBackfillReceipt completion = toCompletionReceipt(origin, flow6Receipt, pickListSources);
            evidence.setCompletionBackfillReceipt(completion);
            hydrate(command, application, transaction, origin, activeOrder, flow6Receipt, completion, gateReceipt);
        } else {
            if (command.getIndependentPrerequisiteReceiptId() == null
                    || isBlank(origin.getSourceSnapshotHash())
                    || isBlank(origin.getSourceCredentialHash())) {
                throw blocker(application, "IndependentBatchPrerequisiteReceipt witness is incomplete");
            }
            MesIndependentBatchPrerequisiteReceipt independent = independentReceiptService
                    .getVerifiedByReceiptId(tenantId, command.getIndependentPrerequisiteReceiptId(),
                            entryType, origin.getSourceSnapshotHash());
            if (independent == null || !Objects.equals(independent.getReceiptHash(), origin.getSourceCredentialHash())) {
                throw blocker(application, "IndependentBatchPrerequisiteReceipt does not match Flow 7 source snapshot");
            }
            IndependentBatchPrerequisiteReceipt releaseReceipt = toReleaseReceipt(independent, batchExecutionId);
            evidence.setIndependentPrerequisiteReceipt(releaseReceipt);
            hydrateIndependent(command, application, transaction, origin, releaseReceipt, gateReceipt);
        }
        return evidence;
    }

    private MesReleaseMaterialGateReceipt loadMaterialGateReceipt(Long tenantId, Long batchExecutionId,
                                                                    String receiptId, String sourceSnapshotHash,
                                                                    MesProcessPoolActiveOrderReleaseApplicationDO application) {
        if (materialGateReceiptPort == null) {
            throw blocker(application, "Flow 8 persisted MATERIALS_READY receipt adapter is not wired");
        }
        List<MesReleaseMaterialGateReceiptPort> adapters = materialGateReceiptPort.orderedStream().toList();
        if (adapters.size() != 1) {
            throw blocker(application, "Flow 8 persisted MATERIALS_READY receipt adapter must have exactly one bean");
        }
        MesReleaseMaterialGateReceipt receipt = isBlank(receiptId)
                ? adapters.get(0).getLatestVerified(tenantId, batchExecutionId, sourceSnapshotHash)
                : adapters.get(0).getVerifiedByReceiptId(tenantId, batchExecutionId, receiptId, sourceSnapshotHash);
        if (receipt == null || !receipt.isCompleteFor(batchExecutionId)) {
            throw blocker(application, "Flow 8 persisted receipt is not MATERIALS_READY or is stale");
        }
        return receipt;
    }

    private void requireTracePrecheck(MesProEdhrBatchTraceSourcePrecheckRespVO source,
                                      MesProEdhrBatchExecutionOriginDO origin, Long batchExecutionId,
                                      MesProcessPoolActiveOrderReleaseApplicationDO application) {
        if (source == null || !Objects.equals(batchExecutionId, source.getBatchExecutionId())
                || source.getOriginLinkId() == null || isBlank(source.getTraceLinkHash())
                || isBlank(source.getSourceSnapshotHash()) || isBlank(source.getRelationStatus())
                || !("CAPTURED".equalsIgnoreCase(source.getRelationStatus())
                || "READY".equalsIgnoreCase(source.getRelationStatus()))
                || "NOT_APPLICABLE".equalsIgnoreCase(source.getRelationStatus())
                || !Objects.equals(source.getSourceSnapshotHash(), origin.getSourceSnapshotHash())) {
            throw blocker(application, "Flow 7 Origin/TraceLink/Manifest mapping is not ready");
        }
    }

    private void hydrate(MesReleaseFinalizationCommand command,
                          MesProcessPoolActiveOrderReleaseApplicationDO application,
                          MesProEdhrReleaseTransactionDO transaction,
                          MesProEdhrBatchExecutionOriginDO origin,
                          MesProcessPoolActiveOrderDO activeOrder,
                          MesFlow6CompletionBackfillReceipt flow6Receipt,
                          CompletionBackfillReceipt completion,
                          MesReleaseMaterialGateReceipt gate) {
        setOrRequire(command.getReleaseApplicationId(), application.getId(), application,
                value -> command.setReleaseApplicationId(value), "releaseApplicationId");
        setOrRequire(command.getBatchExecutionId(), application.getBatchExecutionId(), application,
                value -> command.setBatchExecutionId(value), "batchExecutionId");
        setOrRequire(command.getWorkOrderId(), origin.getWorkOrderId(), application,
                value -> command.setWorkOrderId(value), "workOrderId");
        setOrRequire(command.getOrigin(), MesReleaseOrigin.ACTIVE_ORDER, application,
                value -> command.setOrigin(value), "origin");
        setOrRequire(command.getEntryType(), "ACTIVE_ORDER_COMPLETION", application,
                value -> command.setEntryType(value), "entryType");
        setOrRequire(command.getActiveOrderId(), origin.getActiveOrderId(), application,
                value -> command.setActiveOrderId(value), "activeOrderId");
        setOrRequire(command.getActiveOrderExpectedVersion(), activeOrder.getVersion(), application,
                value -> command.setActiveOrderExpectedVersion(value), "activeOrderExpectedVersion");
        setOrRequire(command.getPickListBindingId(), completion.getPickListBindingId(), application,
                value -> command.setPickListBindingId(value), "pickListBindingId");
        setOrRequire(command.getPickListId(), completion.getPickListId(), application,
                value -> command.setPickListId(value), "pickListId");
        setOrRequire(command.getPickListSources(), completion.getPickListSources(), application,
                value -> command.setPickListSources(value), "pickListSources");
        setOrRequire(command.getCompletionEventId(), flow6Receipt.getRequestIdempotencyKey(), application,
                value -> command.setCompletionEventId(value), "completionEventId");
        setOrRequire(command.getCompletionBackfillReceiptId(), completion.getReceiptId(), application,
                value -> command.setCompletionBackfillReceiptId(value), "completionBackfillReceiptId");
        setOrRequire(command.getSourceRelation(), "FLOW4_COMPLETION_BACKFILL_RECEIPT", application,
                value -> command.setSourceRelation(value), "sourceRelation");
        setOrRequire(command.getSourceSnapshotHash(), completion.getSourceSnapshotHash(), application,
                value -> command.setSourceSnapshotHash(value), "sourceSnapshotHash");
        setOrRequire(command.getDualProgressCompleted(), true, application,
                value -> command.setDualProgressCompleted(value), "dualProgressCompleted");
        setOrRequire(command.getThreeBackfillsSucceeded(), true, application,
                value -> command.setThreeBackfillsSucceeded(value), "threeBackfillsSucceeded");
        setOrRequire(command.getMaterialGateReceiptId(), gate.getReceiptId(), application,
                value -> command.setMaterialGateReceiptId(value), "materialGateReceiptId");
        setOrRequire(command.getMaterialGateManifestHash(), gate.getManifestHash(), application,
                value -> command.setMaterialGateManifestHash(value), "materialGateManifestHash");
        setOrRequire(command.getMaterialGateSourceSnapshotHash(), gate.getSourceSnapshotHash(), application,
                value -> command.setMaterialGateSourceSnapshotHash(value), "materialGateSourceSnapshotHash");
        setOrRequire(command.getExpectedVersion(), transaction.getVersion(), application,
                value -> command.setExpectedVersion(value), "expectedVersion");
    }

    private void hydrateIndependent(MesReleaseFinalizationCommand command,
                                    MesProcessPoolActiveOrderReleaseApplicationDO application,
                                    MesProEdhrReleaseTransactionDO transaction,
                                    MesProEdhrBatchExecutionOriginDO origin,
                                    IndependentBatchPrerequisiteReceipt receipt,
                                    MesReleaseMaterialGateReceipt gate) {
        setOrRequire(command.getReleaseApplicationId(), application.getId(), application,
                value -> command.setReleaseApplicationId(value), "releaseApplicationId");
        setOrRequire(command.getBatchExecutionId(), application.getBatchExecutionId(), application,
                value -> command.setBatchExecutionId(value), "batchExecutionId");
        setOrRequire(command.getWorkOrderId(), origin.getWorkOrderId(), application,
                value -> command.setWorkOrderId(value), "workOrderId");
        setOrRequire(command.getOrigin(), MesReleaseOrigin.valueOf(origin.getEntryType()), application,
                value -> command.setOrigin(value), "origin");
        setOrRequire(command.getEntryType(), origin.getEntryType(), application,
                value -> command.setEntryType(value), "entryType");
        setOrRequire(command.getSourceRelation(), receipt.getSourceRelation(), application,
                value -> command.setSourceRelation(value), "sourceRelation");
        setOrRequire(command.getSourceSnapshotHash(), receipt.getSourceSnapshotHash(), application,
                value -> command.setSourceSnapshotHash(value), "sourceSnapshotHash");
        setOrRequire(command.getIndependentPrerequisiteReceiptId(), receipt.getReceiptId(), application,
                value -> command.setIndependentPrerequisiteReceiptId(value), "independentPrerequisiteReceiptId");
        setOrRequire(command.getMaterialGateReceiptId(), gate.getReceiptId(), application,
                value -> command.setMaterialGateReceiptId(value), "materialGateReceiptId");
        setOrRequire(command.getMaterialGateManifestHash(), gate.getManifestHash(), application,
                value -> command.setMaterialGateManifestHash(value), "materialGateManifestHash");
        setOrRequire(command.getMaterialGateSourceSnapshotHash(), gate.getSourceSnapshotHash(), application,
                value -> command.setMaterialGateSourceSnapshotHash(value), "materialGateSourceSnapshotHash");
        setOrRequire(command.getExpectedVersion(), transaction.getVersion(), application,
                value -> command.setExpectedVersion(value), "expectedVersion");
    }

    private IndependentBatchPrerequisiteReceipt toReleaseReceipt(
            MesIndependentBatchPrerequisiteReceipt receipt, Long batchExecutionId) {
        List<String> sourceIds = receipt.getSourceEvidence() == null ? List.of()
                : receipt.getSourceEvidence().stream().map(item -> item == null ? null : item.getSourceId())
                .filter(Objects::nonNull).toList();
        return new IndependentBatchPrerequisiteReceipt()
                .setReceiptId(receipt.getReceiptId())
                .setTenantId(receipt.getTenantId())
                .setEntryType(receipt.getEntryType())
                .setBatchExecutionId(batchExecutionId)
                .setWorkOrderId(receipt.getWorkOrderId())
                .setWorkOrderCode(receipt.getWorkOrderCode())
                .setRouteId(receipt.getRouteId())
                .setRouteVersion(receipt.getRouteVersion())
                .setBatchCode(receipt.getBatchCode())
                .setSourceRelationId(receipt.getSourceRelationId())
                .setSourceRelation(receipt.getSourceRelationId())
                .setSourceIds(sourceIds)
                .setSourceSnapshotHash(receipt.getSourceSnapshotHash())
                .setBusinessReason(receipt.getBusinessReason())
                .setIssuerSystem(receipt.getIssuerSystem())
                .setIssuerUserId(receipt.getIssuerUserId())
                .setIssuerUserRole(receipt.getIssuerUserRole())
                .setIssuedBy(receipt.getIssuerUserId())
                .setIssuedAt(receipt.getIssuedAt())
                .setExpiresAt(receipt.getExpiresAt())
                .setRevokedAt(receipt.getRevokedAt())
                .setRevocationReason(receipt.getRevocationReason())
                .setCredentialVersion(receipt.getCredentialVersion() == null ? null
                        : receipt.getCredentialVersion().intValue())
                .setPayloadHash(receipt.getPayloadHash())
                .setSignature(receipt.getSignature())
                .setReceiptHash(receipt.getReceiptHash())
                .setAuditEventId(receipt.getAuditEventId())
                .setIdempotencyKey(receipt.getIdempotencyKey())
                .setVersion(receipt.getCredentialVersion() == null ? null
                        : receipt.getCredentialVersion().intValue());
    }

    private CompletionBackfillReceipt toCompletionReceipt(
            MesProEdhrBatchExecutionOriginDO origin,
            MesFlow6CompletionBackfillReceipt receipt,
            List<MesBatchExecutionPickListSource> pickListSources) {
        List<Long> batchRecordIds = parseIds(receipt.getBatchRecordSourceIdsJson());
        List<Long> inspectionIds = parseIds(receipt.getProcessInspectionSourceIdsJson());
        if (batchRecordIds.isEmpty() || inspectionIds.isEmpty()
                || receipt.getBatchRecordId() == null || receipt.getProcessInspectionId() == null) {
            throw blocker(null, "formal completion receipt lacks materialized backfill ids or source evidence ids");
        }
        boolean actualLoss = Boolean.TRUE.equals(receipt.getHasActualLoss());
        return new CompletionBackfillReceipt()
                .setReceiptId(String.valueOf(receipt.getReceiptId()))
                .setTenantId(receipt.getTenantId())
                .setActiveOrderId(receipt.getActiveOrderId())
                .setWorkOrderId(receipt.getWorkOrderId())
                .setBatchCode(receipt.getBatchCode())
                .setRouteId(receipt.getRouteId())
                .setRouteVersionId(receipt.getRouteVersionId())
                .setPickListBindingId(pickListSources.size() == 1
                        ? String.valueOf(pickListSources.get(0).getPickListBindingId()) : null)
                .setPickListId(pickListSources.size() == 1 ? pickListSources.get(0).getPickListId() : null)
                .setPickListSources(pickListSources)
                .setSourceSnapshotHash(receipt.getSourceSnapshotHash())
                .setBindingVersion(pickListSources.size() == 1
                        ? pickListSources.get(0).getBindingVersion().intValue() : null)
                .setCompletionVersion(receipt.getCompletionVersion())
                .setCompletionTransactionId(String.valueOf(origin.getCompletionTransactionId()))
                .setCompletionEventId(receipt.getRequestIdempotencyKey())
                .setBatchRecordId(receipt.getBatchRecordId())
                .setProcessInspectionId(receipt.getProcessInspectionId())
                .setBatchRecordSourceIds(batchRecordIds)
                .setProcessInspectionSourceIds(inspectionIds)
                .setHasActualLoss(receipt.getHasActualLoss())
                .setLossDecision(actualLoss ? "HAS_LOSS" : "NO_LOSS")
                .setLossReportStatus(receipt.getLossReportStatus())
                .setLossRecordId(receipt.getLossRecordId())
                .setLossQuantity(receipt.getLossQuantity() == null ? null : receipt.getLossQuantity().toPlainString())
                .setSourceEventIds(List.of(receipt.getRequestIdempotencyKey()))
                .setReceiptHash(receipt.getReceiptHash())
                .setIdempotencyKey(receipt.getRequestIdempotencyKey())
                .setAuditEventId("FLOW4-COMPLETION-RECEIPT-AUDIT:" + receipt.getReceiptId())
                .setStatus(CompletionBackfillReceipt.STATUS_BACKFILL_SUCCEEDED)
                .setIssuedAt(receipt.getCreatedAt());
    }

    private List<MesBatchExecutionPickListSource> resolvePickListSources(
            MesProEdhrBatchExecutionOriginDO origin,
            MesFlow6CompletionBackfillReceipt receipt,
            MesProEdhrBatchTraceabilityRespVO traceability,
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            Long tenantId) {
        List<MesProcessPoolActiveOrderPickListBindingDO> bindings =
                pickListBindingMapper.selectListByActiveOrderId(origin.getActiveOrderId());
        if (bindings == null || bindings.isEmpty()) {
            throw blocker(application, "formal active-order completion has no Flow-1 pick-list bindings");
        }
        List<MesBatchExecutionPickListSource> sources = bindings.stream().map(binding -> {
            if (binding == null || binding.getId() == null || binding.getPickListId() == null
                    || binding.getBindingVersion() == null || binding.getBindingVersion() <= 0
                    || isBlank(binding.getSourceSnapshotHash())
                    || !Objects.equals(binding.getTenantId(), tenantId)
                    || !Objects.equals(binding.getActiveOrderId(), origin.getActiveOrderId())
                    || !Objects.equals(binding.getWorkOrderId(), origin.getWorkOrderId())
                    || !"BOUND".equalsIgnoreCase(binding.getBindingStatus())) {
                throw blocker(application, "formal Flow-1 pick-list binding is incomplete or stale");
            }
            return new MesBatchExecutionPickListSource()
                    .setPickListBindingId(binding.getId())
                    .setPickListId(binding.getPickListId())
                    .setBindingVersion(binding.getBindingVersion().longValue())
                    .setSourceSnapshotHash(binding.getSourceSnapshotHash());
        }).toList();
        if (sources.stream().anyMatch(source -> !traceHasPickListSource(traceability, source))) {
            throw blocker(application, "Flow 7 trace links do not cover every formal pick-list binding");
        }
        return List.copyOf(sources);
    }

    private boolean traceHasPickListSource(MesProEdhrBatchTraceabilityRespVO traceability,
                                           MesBatchExecutionPickListSource source) {
        if (traceability == null || traceability.getTraceLinks() == null) {
            return false;
        }
        boolean hasPickList = traceability.getTraceLinks().stream().anyMatch(link ->
                MesProEdhrBatchTraceLinkType.MATERIAL_ISSUE.equals(link.getLinkType())
                        && Objects.equals(link.getSourceObjectId(), source.getPickListId())
                        && Objects.equals(link.getSnapshotHash(), source.getSourceSnapshotHash())
                        && !"NOT_APPLICABLE".equalsIgnoreCase(link.getRelationStatus()));
        boolean hasBinding = traceability.getTraceLinks().stream().anyMatch(link ->
                MesProEdhrBatchTraceLinkType.MATERIAL_ISSUE_LINE.equals(link.getLinkType())
                        && Objects.equals(link.getSourceObjectId(), source.getPickListBindingId())
                        && Objects.equals(link.getSnapshotHash(), source.getSourceSnapshotHash())
                        && !"NOT_APPLICABLE".equalsIgnoreCase(link.getRelationStatus()));
        return hasPickList && hasBinding;
    }

    private void requireFlow6Receipt(MesProEdhrBatchExecutionOriginDO origin,
                                     MesFlow6CompletionBackfillReceipt receipt,
                                     MesProcessPoolActiveOrderReleaseApplicationDO application) {
        if (receipt == null || !Objects.equals(receipt.getActiveOrderId(), origin.getActiveOrderId())
                || !Objects.equals(receipt.getWorkOrderId(), origin.getWorkOrderId())
                || !Objects.equals(receipt.getSourceSnapshotHash(), origin.getSourceSnapshotHash())
                || !MesFlow6CompletionBackfillReceipt.STATUS_BACKFILL_SUCCEEDED.equals(receipt.getStatus())) {
            throw blocker(application, "formal Flow-6 completion receipt is missing or bound to different source data");
        }
    }

    private void requireBackfills(MesProEdhrBatchExecutionOriginDO origin,
                                  MesProcessPoolActiveOrderReleaseApplicationDO application) {
        List<MesProcessPoolActiveOrderCompletionBackfillDO> rows = backfillMapper
                .selectListByActiveOrderIdForUpdate(origin.getActiveOrderId());
        boolean batchRecord = rows.stream().anyMatch(row ->
                MesProcessPoolActiveOrderCompletionBackfillDO.TYPE_BATCH_RECORD.equals(row.getBackfillType())
                        && "SUCCESS".equals(row.getStatus()));
        boolean processInspection = rows.stream().anyMatch(row ->
                MesProcessPoolActiveOrderCompletionBackfillDO.TYPE_PROCESS_INSPECTION.equals(row.getBackfillType())
                        && "SUCCESS".equals(row.getStatus()));
        boolean loss = !Boolean.TRUE.equals(origin.getHasActualLoss()) || rows.stream().anyMatch(row ->
                MesProcessPoolActiveOrderCompletionBackfillDO.TYPE_LOSS_REPORT.equals(row.getBackfillType())
                        && "SUCCESS".equals(row.getStatus()));
        if (!batchRecord || !processInspection || !loss) {
            throw blocker(application, "formal batch record, process inspection and required loss backfills are incomplete");
        }
    }

    private List<Long> parseIds(String json) {
        if (isBlank(json)) {
            return List.of();
        }
        try {
            List<Long> ids = JsonUtils.parseArray(json, Long.class);
            if (ids == null || ids.stream().anyMatch(Objects::isNull)) {
                throw blocker(null, "formal source id list is invalid");
            }
            return ids;
        } catch (MesReleaseFlowBlockerException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw blocker(null, "formal source id list cannot be parsed");
        }
    }

    private <T> void setOrRequire(T actual, T expected,
                                  MesProcessPoolActiveOrderReleaseApplicationDO application,
                                  java.util.function.Consumer<T> setter,
                                  String field) {
        if (actual != null && !Objects.equals(actual, expected)) {
            throw blocker(application, field + " does not match authoritative source");
        }
        if (actual == null) {
            setter.accept(expected);
        }
    }

    private void requireMatches(Object actual, Object expected,
                                MesProcessPoolActiveOrderReleaseApplicationDO application,
                                String reason) {
        if (actual != null && !Objects.equals(actual, expected)) {
            throw blocker(application, reason);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private MesReleaseFlowBlockerException blocker(
            MesProcessPoolActiveOrderReleaseApplicationDO application, String reason) {
        return new MesReleaseFlowBlockerException(reason,
                new MesReleaseFlowFailureRespVO()
                        .setStage(MesReleaseFlowStage.SP_4)
                        .setCurrentStatus(application == null ? null : application.getApplicationStatus())
                        .setBlockers(List.of(new MesReleaseFlowBlocker()
                                .setBlockerType(MesReleaseFlowBlockerType.AUTHORITATIVE_RECEIPT_CONTEXT_REQUIRED)
                                .setObjectType("RELEASE_APPLICATION")
                                .setObjectId(application == null || application.getId() == null
                                        ? null : String.valueOf(application.getId()))
                                .setReason(reason)
                                .setSuggestion("complete the formal upstream receipt and retry final release"))));
    }

}
