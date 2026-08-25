package cn.iocoder.yudao.module.mes.productionrelease.core;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrBatchTraceSourcePrecheckRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionOriginDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionOriginMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionBackfillMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesIndependentBatchPrerequisiteReceipt;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesIndependentBatchPrerequisiteReceiptService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesReleaseAuthoritativeContextPortImplTest {

    @Mock MesProEdhrReleaseTransactionMapper releaseTransactionMapper;
    @Mock MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Mock MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper;
    @Mock MesProEdhrBatchExecutionOriginMapper originMapper;
    @Mock MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Mock MesProcessPoolActiveOrderCompletionBackfillMapper backfillMapper;
    @Mock MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort completionReceiptPort;
    @Mock ObjectProvider<MesReleaseMaterialGateReceiptPort> materialGateReceiptPort;
    @Mock MesReleaseMaterialGateReceiptPort materialGatePort;
    @Mock MesProEdhrBatchTraceabilityService traceabilityService;
    @Mock MesIndependentBatchPrerequisiteReceiptService independentReceiptService;

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void independentEntryReadsPersistedReceiptAndDoesNotRequireActiveOrder() {
        TenantContextHolder.setTenantId(1L);
        MesProEdhrReleaseTransactionDO transaction = new MesProEdhrReleaseTransactionDO()
                .setId(10L).setBatchExecutionId(20L).setReleaseStatus("PENDING_APPROVAL").setVersion(3);
        MesProcessPoolActiveOrderReleaseApplicationDO application = new MesProcessPoolActiveOrderReleaseApplicationDO()
                .setId(30L).setBatchExecutionId(20L).setReleaseTransactionId(10L)
                .setWorkOrderId(40L).setReleaseApprovalWorkTaskId(50L)
                .setApplicationStatus(MesReleaseFlowStatus.MANAGER_RELEASE_PENDING);
        MesProEdhrBatchExecutionDO batch = new MesProEdhrBatchExecutionDO()
                .setId(20L).setStatus(20);
        MesProEdhrBatchExecutionOriginDO origin = new MesProEdhrBatchExecutionOriginDO()
                .setId(60L).setBatchExecutionId(20L).setEntryType("MANUAL")
                .setWorkOrderId(40L).setSourceSnapshotHash("source-snapshot")
                .setSourceCredentialHash("independent-hash");
        MesProEdhrBatchTraceSourcePrecheckRespVO source = new MesProEdhrBatchTraceSourcePrecheckRespVO()
                .setBatchExecutionId(20L).setOriginLinkId(61L).setTraceLinkHash("trace-hash")
                .setSourceSnapshotHash("source-snapshot").setRelationStatus("CAPTURED");
        MesReleaseMaterialGateReceipt gate = completeGate("gate-1", 20L, "source-snapshot");
        MesIndependentBatchPrerequisiteReceipt receipt = independentReceipt();

        when(releaseTransactionMapper.selectById(10L)).thenReturn(transaction);
        when(applicationMapper.selectListByReleaseTransactionId(10L)).thenReturn(List.of(application));
        when(batchExecutionMapper.selectById(20L)).thenReturn(batch);
        when(originMapper.selectListByBatchExecutionId(20L)).thenReturn(List.of(origin));
        when(traceabilityService.resolveSourcePrecheck(org.mockito.ArgumentMatchers.any()))
                .thenReturn(source);
        when(materialGateReceiptPort.orderedStream()).thenReturn(Stream.of(materialGatePort));
        when(materialGatePort.getVerifiedByReceiptId(1L, 20L, "independent-gate", "source-snapshot"))
                .thenReturn(gate);
        when(independentReceiptService.getVerifiedByReceiptId(1L, "independent-1", "MANUAL", "source-snapshot"))
                .thenReturn(receipt);

        MesReleaseFinalizationCommand command = new MesReleaseFinalizationCommand()
                .setReleaseTransactionId(10L).setReleaseApplicationId(30L).setBatchExecutionId(20L)
                .setWorkTaskId(50L).setOrigin(MesReleaseOrigin.MANUAL).setEntryType("MANUAL")
                .setIndependentPrerequisiteReceiptId("independent-1")
                .setMaterialGateReceiptId("independent-gate").setActorUserId(100L)
                .setExpectedVersion(3).setIdempotencyKey("release-idem").setSignoffEvidenceHash("signoff");

        MesReleaseFinalizationEvidence evidence = new MesReleaseAuthoritativeContextPortImpl(
                releaseTransactionMapper, batchExecutionMapper, applicationMapper, originMapper,
                activeOrderMapper, backfillMapper, completionReceiptPort, materialGateReceiptPort,
                traceabilityService, independentReceiptService).require(command);

        assertSame(gate, evidence.getMaterialGateReceipt());
        assertEquals("independent-1", evidence.getIndependentPrerequisiteReceipt().getReceiptId());
        assertEquals(20L, evidence.getIndependentPrerequisiteReceipt().getBatchExecutionId());
        assertNull(command.getActiveOrderId());
        assertNull(command.getPickListId());
    }

    @Test
    void missingPersistedMaterialReceiptBlocksInsteadOfUsingRequestPayload() {
        TenantContextHolder.setTenantId(1L);
        when(releaseTransactionMapper.selectById(10L)).thenReturn(new MesProEdhrReleaseTransactionDO()
                .setId(10L).setBatchExecutionId(20L).setReleaseStatus("PENDING_APPROVAL").setVersion(1));
        when(applicationMapper.selectListByReleaseTransactionId(10L)).thenReturn(List.of(
                new MesProcessPoolActiveOrderReleaseApplicationDO().setId(30L).setBatchExecutionId(20L)
                        .setReleaseTransactionId(10L).setReleaseApprovalWorkTaskId(50L)
                        .setApplicationStatus(MesReleaseFlowStatus.MANAGER_RELEASE_PENDING)));
        when(batchExecutionMapper.selectById(20L)).thenReturn(new MesProEdhrBatchExecutionDO().setId(20L).setStatus(20));
        when(originMapper.selectListByBatchExecutionId(20L)).thenReturn(List.of(
                new MesProEdhrBatchExecutionOriginDO().setId(60L).setBatchExecutionId(20L)
                        .setEntryType("MANUAL").setWorkOrderId(40L).setSourceSnapshotHash("source-snapshot")
                        .setSourceCredentialHash("independent-hash")));
        when(traceabilityService.resolveSourcePrecheck(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new MesProEdhrBatchTraceSourcePrecheckRespVO().setBatchExecutionId(20L)
                        .setOriginLinkId(61L).setTraceLinkHash("trace-hash")
                        .setSourceSnapshotHash("source-snapshot").setRelationStatus("CAPTURED"));
        when(materialGateReceiptPort.orderedStream()).thenReturn(Stream.empty());

        MesReleaseFinalizationCommand command = new MesReleaseFinalizationCommand()
                .setReleaseTransactionId(10L).setReleaseApplicationId(30L).setBatchExecutionId(20L)
                .setWorkTaskId(50L).setOrigin(MesReleaseOrigin.MANUAL).setEntryType("MANUAL")
                .setIndependentPrerequisiteReceiptId("independent-1").setMaterialGateReceiptId("client-fake")
                .setMaterialGateReceipt(completeGate("client-fake", 20L, "source-snapshot"));

        MesReleaseFlowBlockerException failure = assertThrows(MesReleaseFlowBlockerException.class, () ->
                new MesReleaseAuthoritativeContextPortImpl(releaseTransactionMapper, batchExecutionMapper,
                        applicationMapper, originMapper, activeOrderMapper, backfillMapper, completionReceiptPort,
                        materialGateReceiptPort, traceabilityService, independentReceiptService).require(command));

        assertEquals(MesReleaseFlowBlockerType.AUTHORITATIVE_RECEIPT_CONTEXT_REQUIRED,
                failure.getFailure().getBlockers().get(0).getBlockerType());
    }

    private MesReleaseMaterialGateReceipt completeGate(String id, Long batchId, String sourceHash) {
        return new MesReleaseMaterialGateReceipt().setReceiptId(id).setBatchExecutionId(batchId)
                .setGateStatus(MesReleaseMaterialGateReceipt.STATUS_MATERIALS_READY)
                .setMaterialTypeKeys(MesReleaseMaterialGateReceipt.REQUIRED_MATERIAL_TYPES)
                .setManifestHash("manifest").setSourceSnapshotHash(sourceHash)
                .setMaterialVersionSetHash("version-set").setReceiptHash("gate-hash")
                .setIssuedBy(100L).setAuditEventId("gate-audit").setVersion(1);
    }

    private MesIndependentBatchPrerequisiteReceipt independentReceipt() {
        return new MesIndependentBatchPrerequisiteReceipt()
                .setReceiptId("independent-1").setTenantId(1L).setEntryType("MANUAL")
                .setWorkOrderId(40L).setWorkOrderCode("WO-40").setRouteId(41L).setRouteVersionId(42L)
                .setRouteVersion("1").setBatchCode("B-20").setSourceRelationId("MANUAL-SOURCE")
                .setSourceRelationVersion("1").setSourceRelationSnapshotHash("source-snapshot")
                .setSourceObjectType("MANUAL_BATCH_ENTRY").setSourceObjectId("source-1")
                .setMaterialSourceType("WORK_ORDER").setMaterialSourceId("40")
                .setSourceContextHash("context").setSourceSnapshotHash("source-snapshot")
                .setBusinessReason("controlled").setIssuerSystem("mes").setIssuerUserId(100L)
                .setIssuerUserRole("BACKEND_CONTROLLED").setIssuedAt(LocalDateTime.now().minusMinutes(1))
                .setExpiresAt(LocalDateTime.now().plusHours(1)).setCredentialVersion(1L)
                .setStatus("ISSUED").setReceiptHash("independent-hash").setPayloadHash("payload")
                .setSignature("signature").setAuditEventId("audit").setIdempotencyKey("independent-idem")
                .setSourceEvidence(List.of(new cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchExecutionSourceEvidence()
                        .setSourceType("MANUAL").setSourceId("source-1").setSourceVersion("1")
                        .setSourceSnapshotHash("source-snapshot").setPayloadHash("payload").setSignature("signature")));
    }
}
