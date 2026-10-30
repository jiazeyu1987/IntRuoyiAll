package cn.iocoder.yudao.module.mes.productionrelease.core;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrBatchTraceSourcePrecheckRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrBatchTraceabilityRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionOriginDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionBackfillDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionOriginMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionBackfillMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesIndependentBatchPrerequisiteReceipt;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceLinkType;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesFlow6CompletionBackfillReceipt;
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
    @Mock MesProcessPoolActiveOrderPickListBindingMapper pickListBindingMapper;
    @Mock MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort completionReceiptPort;
    @Mock ObjectProvider<MesReleaseMaterialGateReceiptPort> materialGateReceiptPort;
    @Mock MesReleaseMaterialGateReceiptPort materialGatePort;
    @Mock MesProEdhrBatchTraceabilityService traceabilityService;


    @Mock cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper approvalTasks;
    @Mock cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesEdhrReleaseMaterialSelectionRegistry selections;
    private MesReleaseAuthoritativeContextPortImpl port() {
        var port=new MesReleaseAuthoritativeContextPortImpl(releaseTransactionMapper,batchExecutionMapper,
            applicationMapper,originMapper,activeOrderMapper,backfillMapper,pickListBindingMapper,
            completionReceiptPort,materialGateReceiptPort,traceabilityService);
        org.springframework.test.util.ReflectionTestUtils.setField(port,"approvalTasks",approvalTasks);
        org.springframework.test.util.ReflectionTestUtils.setField(port,"materialSelections",selections);
        org.mockito.Mockito.lenient().when(approvalTasks.selectById(50L)).thenReturn(
            new cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO().setId(50L));
        org.mockito.Mockito.lenient().when(selections.requireCurrentVerified(org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.anyBoolean()))
            .thenAnswer(call -> {
                var origin=originMapper.selectListByBatchExecutionId(20L).get(0);
                var gate=completeGate(origin.getActiveOrderId()==null?"independent-gate":"active-gate",20L,"source-snapshot");
                var proof=new cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesEdhrSourceCreationEvidence(
                    "MATERIAL_GATE_V1",12L,gate.getReceiptId(),"1","b".repeat(64),13L,"c".repeat(64));
                return new cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesEdhrVerifiedMaterialSelection(
                    "MANAGED",10L,50L,"precheck",new cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesEdhrVerifiedMaterialGateFact(gate,proof),null);
            });
        return port;
    }
    private cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesEdhrVerifiedCompletionReceiptFact verifiedCompletion(MesFlow6CompletionBackfillReceipt receipt) {
        return new cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesEdhrVerifiedCompletionReceiptFact(receipt,
            new cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesEdhrSourceCreationEvidence(
                "COMPLETION_MAIN_V1",900L,"900","1","b".repeat(64),13L,"c".repeat(64)),"completion-tx");
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void independentEntryIsRejectedBeforeReceiptOrMaterialLoading() {
        TenantContextHolder.setTenantId(1L);
        MesProEdhrReleaseTransactionDO transaction = new MesProEdhrReleaseTransactionDO()
                .setId(10L).setBatchExecutionId(20L).setReleaseStatus("PENDING_APPROVAL").setVersion(3);
        MesProcessPoolActiveOrderReleaseApplicationDO application = new MesProcessPoolActiveOrderReleaseApplicationDO()
                .setId(30L).setBatchExecutionId(20L).setReleaseTransactionId(10L)
                .setWorkOrderId(40L).setReleaseApprovalWorkTaskId(50L)
                .setApplicationStatus(MesReleaseFlowStatus.MANAGER_RELEASE_PENDING);
        MesProEdhrBatchExecutionDO batch = new MesProEdhrBatchExecutionDO()
                .setId(20L).setStatus(20).setProvisioningStatus("BATCH_READY");
        MesProEdhrBatchExecutionOriginDO origin = new MesProEdhrBatchExecutionOriginDO()
                .setId(60L).setBatchExecutionId(20L).setEntryType("MANUAL")
                .setWorkOrderId(40L).setSourceSnapshotHash("source-snapshot")
                .setSourceCredentialHash("independent-hash");
        MesProEdhrBatchTraceSourcePrecheckRespVO source = new MesProEdhrBatchTraceSourcePrecheckRespVO()
                .setBatchExecutionId(20L).setOriginLinkId(61L).setTraceLinkHash("trace-hash")
                .setSourceSnapshotHash("source-snapshot").setRelationStatus("CAPTURED");
        when(releaseTransactionMapper.selectById(10L)).thenReturn(transaction);
        when(applicationMapper.selectListByReleaseTransactionId(10L)).thenReturn(List.of(application));
        when(batchExecutionMapper.selectById(20L)).thenReturn(batch);
        when(originMapper.selectListByBatchExecutionId(20L)).thenReturn(List.of(origin));
        MesReleaseFinalizationCommand command = new MesReleaseFinalizationCommand()
                .setReleaseTransactionId(10L).setReleaseApplicationId(30L).setBatchExecutionId(20L)
                .setWorkTaskId(50L).setOrigin(MesReleaseOrigin.MANUAL).setEntryType("MANUAL")
                .setIndependentPrerequisiteReceiptId("independent-1")
                .setMaterialGateReceiptId("independent-gate").setActorUserId(100L)
                .setExpectedVersion(3).setIdempotencyKey("release-idem").setSignoffEvidenceHash("signoff");

        assertThrows(MesReleaseFlowBlockerException.class, () -> port().require(command));
        org.mockito.Mockito.verifyNoInteractions(completionReceiptPort, materialGateReceiptPort, traceabilityService);

    }

    @Test
    void activeOrderEntryHydratesTheFormalCompletionEntryType() {
        TenantContextHolder.setTenantId(1L);
        MesProEdhrReleaseTransactionDO transaction = new MesProEdhrReleaseTransactionDO()
                .setId(10L).setBatchExecutionId(20L).setReleaseStatus("PENDING_APPROVAL").setVersion(3);
        MesProcessPoolActiveOrderReleaseApplicationDO application = new MesProcessPoolActiveOrderReleaseApplicationDO()
                .setId(30L).setBatchExecutionId(20L).setReleaseTransactionId(10L)
                .setActiveOrderId(70L).setWorkOrderId(40L).setReleaseApprovalWorkTaskId(50L)
                .setApplicationStatus(MesReleaseFlowStatus.MANAGER_RELEASE_PENDING);
        MesProEdhrBatchExecutionDO batch = new MesProEdhrBatchExecutionDO()
                .setId(20L).setStatus(20).setProvisioningStatus("BATCH_READY");
        MesProEdhrBatchExecutionOriginDO origin = new MesProEdhrBatchExecutionOriginDO()
                .setId(60L).setBatchExecutionId(20L).setEntryType("ACTIVE_ORDER_COMPLETION")
                .setActiveOrderId(70L).setWorkOrderId(40L).setSourceSnapshotHash("source-snapshot")
                .setCompletionBackfillReceiptId(900L).setCompletionVersion(1)
                .setPickListBindingId(55L).setPickListId(66L).setPickListBindingVersion(1)
                .setHasActualLoss(false);
        MesProEdhrBatchTraceSourcePrecheckRespVO source = new MesProEdhrBatchTraceSourcePrecheckRespVO()
                .setBatchExecutionId(20L).setOriginLinkId(61L).setTraceLinkHash("trace-hash")
                .setSourceSnapshotHash("source-snapshot").setRelationStatus("CAPTURED");
        MesReleaseMaterialGateReceipt gate = completeGate("active-gate", 20L, "source-snapshot");
        MesFlow6CompletionBackfillReceipt completionReceipt = activeCompletionReceipt();

        when(releaseTransactionMapper.selectById(10L)).thenReturn(transaction);
        when(applicationMapper.selectListByReleaseTransactionId(10L)).thenReturn(List.of(application));
        when(batchExecutionMapper.selectById(20L)).thenReturn(batch);
        when(originMapper.selectListByBatchExecutionId(20L)).thenReturn(List.of(origin));
        when(traceabilityService.resolveSourcePrecheck(org.mockito.ArgumentMatchers.any())).thenReturn(source);
        when(materialGateReceiptPort.orderedStream()).thenReturn(Stream.of(materialGatePort));
        when(materialGatePort.getVerifiedByReceiptId(1L,20L,"active-gate","source-snapshot")).thenReturn(gate);
        when(activeOrderMapper.selectById(70L)).thenReturn(new MesProcessPoolActiveOrderDO().setId(70L).setVersion(3));
        when(completionReceiptPort.requireVerifiedByReceiptId(900L, 1L)).thenReturn(verifiedCompletion(completionReceipt));
        when(backfillMapper.selectListByActiveOrderIdForUpdate(70L)).thenReturn(List.of(
                backfill(MesProcessPoolActiveOrderCompletionBackfillDO.TYPE_BATCH_RECORD),
                backfill(MesProcessPoolActiveOrderCompletionBackfillDO.TYPE_PROCESS_INSPECTION)));
        when(pickListBindingMapper.selectListByActiveOrderId(70L))
                .thenReturn(List.of(binding(70L, 40L, 55L, 66L, "pick-source")));
        when(traceabilityService.getTraceability(20L)).thenReturn(traceability(20L, 60L,
                binding(70L, 40L, 55L, 66L, "pick-source")));

        MesReleaseFinalizationCommand command = new MesReleaseFinalizationCommand()
                .setReleaseTransactionId(10L).setReleaseApplicationId(30L).setBatchExecutionId(20L)
                .setWorkTaskId(50L);

        MesReleaseFinalizationEvidence evidence = port().require(command);

        assertSame(gate, evidence.getMaterialGateReceipt());
        assertEquals(MesReleaseOrigin.ACTIVE_ORDER, command.getOrigin());
        assertEquals("ACTIVE_ORDER_COMPLETION", command.getEntryType());
        assertEquals("900", evidence.getCompletionBackfillReceipt().getReceiptId());
        assertEquals(70L, command.getActiveOrderId());
        assertEquals(1, command.getPickListSources().size());
    }

    @Test
    void activeOrderFormalFactsUseProvisioningReadinessWhenLegacyBatchStatusIsCreated() {
        TenantContextHolder.setTenantId(1L);
        MesProEdhrReleaseTransactionDO transaction = new MesProEdhrReleaseTransactionDO()
                .setId(10L).setBatchExecutionId(20L).setReleaseStatus("PENDING_APPROVAL").setVersion(3);
        MesProcessPoolActiveOrderReleaseApplicationDO application = new MesProcessPoolActiveOrderReleaseApplicationDO()
                .setId(30L).setBatchExecutionId(20L).setReleaseTransactionId(10L)
                .setActiveOrderId(70L).setWorkOrderId(40L).setReleaseApprovalWorkTaskId(50L)
                .setApplicationStatus(MesReleaseFlowStatus.MANAGER_RELEASE_PENDING);
        MesProEdhrBatchExecutionDO batch = new MesProEdhrBatchExecutionDO()
                .setId(20L).setStatus(0).setProvisioningStatus("BATCH_READY");
        MesProEdhrBatchExecutionOriginDO origin = new MesProEdhrBatchExecutionOriginDO()
                .setId(60L).setBatchExecutionId(20L).setEntryType("ACTIVE_ORDER_COMPLETION")
                .setActiveOrderId(70L).setWorkOrderId(40L).setSourceSnapshotHash("source-snapshot")
                .setCompletionBackfillReceiptId(900L).setCompletionVersion(1)
                .setPickListBindingId(55L).setPickListId(66L).setPickListBindingVersion(1)
                .setHasActualLoss(false);
        MesProEdhrBatchTraceSourcePrecheckRespVO source = new MesProEdhrBatchTraceSourcePrecheckRespVO()
                .setBatchExecutionId(20L).setOriginLinkId(61L).setTraceLinkHash("trace-hash")
                .setSourceSnapshotHash("source-snapshot").setRelationStatus("BOUND");
        MesFlow6CompletionBackfillReceipt completionReceipt = activeCompletionReceipt();

        when(releaseTransactionMapper.selectById(10L)).thenReturn(transaction);
        when(applicationMapper.selectListByReleaseTransactionId(10L)).thenReturn(List.of(application));
        when(batchExecutionMapper.selectById(20L)).thenReturn(batch);
        when(originMapper.selectListByBatchExecutionId(20L)).thenReturn(List.of(origin));
        when(traceabilityService.resolveSourcePrecheck(org.mockito.ArgumentMatchers.any())).thenReturn(source);
        when(activeOrderMapper.selectById(70L)).thenReturn(new MesProcessPoolActiveOrderDO().setId(70L).setVersion(3));
        when(completionReceiptPort.requireVerifiedByReceiptId(900L, 1L)).thenReturn(verifiedCompletion(completionReceipt));
        when(backfillMapper.selectListByActiveOrderIdForUpdate(70L)).thenReturn(List.of(
                backfill(MesProcessPoolActiveOrderCompletionBackfillDO.TYPE_BATCH_RECORD),
                backfill(MesProcessPoolActiveOrderCompletionBackfillDO.TYPE_PROCESS_INSPECTION)));
        when(pickListBindingMapper.selectListByActiveOrderId(70L))
                .thenReturn(List.of(binding(70L, 40L, 55L, 66L, "pick-source")));
        when(traceabilityService.getTraceability(20L)).thenReturn(traceability(20L, 60L,
                binding(70L, 40L, 55L, 66L, "pick-source")));

        MesReleaseFinalizationCommand command = new MesReleaseFinalizationCommand()
                .setReleaseTransactionId(10L).setReleaseApplicationId(30L).setBatchExecutionId(20L)
                .setWorkTaskId(50L);

        var actualPort=port();
        var fact=new cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesEdhrVerifiedActiveOrderReleaseFact(
            30L,20L,"ACTIVE_ORDER_FACTS:"+"a".repeat(64),"source-snapshot",verifiedCompletion(completionReceipt).creation(),14L,"b".repeat(64),15L);
        when(selections.requireCurrentVerified(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.anyBoolean())).thenReturn(
            new cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesEdhrVerifiedMaterialSelection("MANAGED_ACTIVE_ORDER",10L,50L,"p",null,fact));
        assertThrows(MesReleaseFlowBlockerException.class,()->actualPort.require(command));
        command.setMaterialGateRequired(false);
        MesReleaseFinalizationEvidence evidence = actualPort.require(command);

        assertNull(evidence.getMaterialGateReceipt());
        org.mockito.Mockito.verifyNoInteractions(materialGateReceiptPort,materialGatePort);
        assertEquals(70L, command.getActiveOrderId());
    }

    @Test
    void activeOrderEntryUsesMaterializedBackfillIdsWhenReceiptCarriesMultipleSourceIds() {
        TenantContextHolder.setTenantId(1L);
        MesProEdhrReleaseTransactionDO transaction = new MesProEdhrReleaseTransactionDO()
                .setId(10L).setBatchExecutionId(20L).setReleaseStatus("PENDING_APPROVAL").setVersion(3);
        MesProcessPoolActiveOrderReleaseApplicationDO application = new MesProcessPoolActiveOrderReleaseApplicationDO()
                .setId(30L).setBatchExecutionId(20L).setReleaseTransactionId(10L)
                .setActiveOrderId(70L).setWorkOrderId(40L).setReleaseApprovalWorkTaskId(50L)
                .setApplicationStatus(MesReleaseFlowStatus.MANAGER_RELEASE_PENDING);
        MesProEdhrBatchExecutionDO batch = new MesProEdhrBatchExecutionDO()
                .setId(20L).setStatus(20).setProvisioningStatus("BATCH_READY");
        MesProEdhrBatchExecutionOriginDO origin = new MesProEdhrBatchExecutionOriginDO()
                .setId(60L).setBatchExecutionId(20L).setEntryType("ACTIVE_ORDER_COMPLETION")
                .setActiveOrderId(70L).setWorkOrderId(40L).setSourceSnapshotHash("source-snapshot")
                .setCompletionBackfillReceiptId(900L).setCompletionVersion(1)
                .setPickListBindingId(55L).setPickListId(66L).setPickListBindingVersion(1)
                .setHasActualLoss(false);
        MesProEdhrBatchTraceSourcePrecheckRespVO source = new MesProEdhrBatchTraceSourcePrecheckRespVO()
                .setBatchExecutionId(20L).setOriginLinkId(61L).setTraceLinkHash("trace-hash")
                .setSourceSnapshotHash("source-snapshot").setRelationStatus("CAPTURED");
        MesReleaseMaterialGateReceipt gate = completeGate("active-gate", 20L, "source-snapshot");
        MesFlow6CompletionBackfillReceipt completionReceipt = activeCompletionReceipt()
                .setBatchRecordId(140L)
                .setProcessInspectionId(150L)
                .setBatchRecordSourceIdsJson("[40,41,55,56,57,58]")
                .setProcessInspectionSourceIdsJson("[50,51]");

        when(releaseTransactionMapper.selectById(10L)).thenReturn(transaction);
        when(applicationMapper.selectListByReleaseTransactionId(10L)).thenReturn(List.of(application));
        when(batchExecutionMapper.selectById(20L)).thenReturn(batch);
        when(originMapper.selectListByBatchExecutionId(20L)).thenReturn(List.of(origin));
        when(traceabilityService.resolveSourcePrecheck(org.mockito.ArgumentMatchers.any())).thenReturn(source);
        when(materialGateReceiptPort.orderedStream()).thenReturn(Stream.of(materialGatePort));
        when(materialGatePort.getVerifiedByReceiptId(1L,20L,"active-gate","source-snapshot")).thenReturn(gate);
        when(activeOrderMapper.selectById(70L)).thenReturn(new MesProcessPoolActiveOrderDO().setId(70L).setVersion(3));
        when(completionReceiptPort.requireVerifiedByReceiptId(900L, 1L)).thenReturn(verifiedCompletion(completionReceipt));
        when(backfillMapper.selectListByActiveOrderIdForUpdate(70L)).thenReturn(List.of(
                backfill(MesProcessPoolActiveOrderCompletionBackfillDO.TYPE_BATCH_RECORD),
                backfill(MesProcessPoolActiveOrderCompletionBackfillDO.TYPE_PROCESS_INSPECTION)));
        when(pickListBindingMapper.selectListByActiveOrderId(70L))
                .thenReturn(List.of(binding(70L, 40L, 55L, 66L, "pick-source")));
        when(traceabilityService.getTraceability(20L)).thenReturn(traceability(20L, 60L,
                binding(70L, 40L, 55L, 66L, "pick-source")));

        MesReleaseFinalizationCommand command = new MesReleaseFinalizationCommand()
                .setReleaseTransactionId(10L).setReleaseApplicationId(30L).setBatchExecutionId(20L)
                .setWorkTaskId(50L).setOrigin(MesReleaseOrigin.ACTIVE_ORDER);

        MesReleaseFinalizationEvidence evidence = port().require(command);

        assertEquals(140L, evidence.getCompletionBackfillReceipt().getBatchRecordId());
        assertEquals(150L, evidence.getCompletionBackfillReceipt().getProcessInspectionId());
        assertEquals(List.of(40L, 41L, 55L, 56L, 57L, 58L),
                evidence.getCompletionBackfillReceipt().getBatchRecordSourceIds());
        assertEquals(List.of(50L, 51L),
                evidence.getCompletionBackfillReceipt().getProcessInspectionSourceIds());
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
        when(batchExecutionMapper.selectById(20L)).thenReturn(new MesProEdhrBatchExecutionDO().setId(20L).setStatus(20)
                .setProvisioningStatus("BATCH_READY"));
        when(originMapper.selectListByBatchExecutionId(20L)).thenReturn(List.of(
                new MesProEdhrBatchExecutionOriginDO().setId(60L).setBatchExecutionId(20L)
                        .setEntryType("MANUAL").setWorkOrderId(40L).setSourceSnapshotHash("source-snapshot")
                        .setSourceCredentialHash("independent-hash")));
        when(traceabilityService.resolveSourcePrecheck(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new MesProEdhrBatchTraceSourcePrecheckRespVO().setBatchExecutionId(20L)
                        .setOriginLinkId(61L).setTraceLinkHash("trace-hash")
                        .setSourceSnapshotHash("source-snapshot").setRelationStatus("CAPTURED"));

        MesReleaseFinalizationCommand command = new MesReleaseFinalizationCommand()
                .setReleaseTransactionId(10L).setReleaseApplicationId(30L).setBatchExecutionId(20L)
                .setWorkTaskId(50L).setOrigin(MesReleaseOrigin.MANUAL).setEntryType("MANUAL")
                .setIndependentPrerequisiteReceiptId("independent-1").setMaterialGateReceiptId("client-fake")
                .setMaterialGateReceipt(completeGate("client-fake", 20L, "source-snapshot"));

        MesReleaseFlowBlockerException failure = assertThrows(MesReleaseFlowBlockerException.class, () ->
                port().require(command));

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

    private MesFlow6CompletionBackfillReceipt activeCompletionReceipt() {
        return new MesFlow6CompletionBackfillReceipt()
                .setReceiptId(900L)
                .setTenantId(1L)
                .setActiveOrderId(70L)
                .setWorkOrderId(40L)
                .setBatchCode("B-20")
                .setRouteId(41L)
                .setRouteVersionId(42L)
                .setRequestIdempotencyKey("completion-event")
                .setCreatedAt(LocalDateTime.now().minusMinutes(1))
                .setSourceSnapshotHash("source-snapshot")
                .setCompletionVersion(1)
                .setStatus(MesFlow6CompletionBackfillReceipt.STATUS_BACKFILL_SUCCEEDED)
                .setBatchRecordStatus("SUCCESS")
                .setProcessInspectionStatus("SUCCESS")
                .setBatchRecordId(40L)
                .setProcessInspectionId(50L)
                .setBatchRecordSourceIdsJson("[40]")
                .setProcessInspectionSourceIdsJson("[50]")
                .setHasActualLoss(false)
                .setLossReportStatus("NOT_REQUIRED")
                .setReceiptHash("completion-receipt")
                .setPayloadHash("completion-payload");
    }

    private MesProcessPoolActiveOrderPickListBindingDO binding(Long activeOrderId, Long workOrderId,
                                                               Long bindingId, Long pickListId,
                                                               String sourceSnapshotHash) {
        MesProcessPoolActiveOrderPickListBindingDO binding = new MesProcessPoolActiveOrderPickListBindingDO()
                .setId(bindingId).setActiveOrderId(activeOrderId).setWorkOrderId(workOrderId)
                .setPickListId(pickListId).setBindingVersion(1)
                .setBindingStatus("BOUND").setSourceSnapshotHash(sourceSnapshotHash);
        binding.setTenantId(1L);
        return binding;
    }

    private MesProEdhrBatchTraceabilityRespVO traceability(
            Long batchExecutionId, Long originId, MesProcessPoolActiveOrderPickListBindingDO... bindings) {
        List<MesProEdhrBatchTraceabilityRespVO.TraceLink> links = java.util.Arrays.stream(bindings)
                .flatMap(binding -> Stream.of(
                        traceLink(batchExecutionId, originId, MesProEdhrBatchTraceLinkType.MATERIAL_ISSUE,
                                binding.getPickListId(), binding.getSourceSnapshotHash()),
                        traceLink(batchExecutionId, originId, MesProEdhrBatchTraceLinkType.MATERIAL_ISSUE_LINE,
                                binding.getId(), binding.getSourceSnapshotHash())))
                .toList();
        return new MesProEdhrBatchTraceabilityRespVO()
                .setBatchExecutionId(batchExecutionId)
                .setTraceLinks(links);
    }

    private MesProEdhrBatchTraceabilityRespVO.TraceLink traceLink(
            Long batchExecutionId, Long originId, String linkType, Long sourceObjectId, String snapshotHash) {
        return new MesProEdhrBatchTraceabilityRespVO.TraceLink()
                .setBatchExecutionId(batchExecutionId)
                .setOriginId(originId)
                .setLinkType(linkType)
                .setSourceObjectId(sourceObjectId)
                .setSnapshotHash(snapshotHash)
                .setRelationStatus("CAPTURED");
    }

    private MesProcessPoolActiveOrderCompletionBackfillDO backfill(String type) {
        return new MesProcessPoolActiveOrderCompletionBackfillDO()
                .setActiveOrderId(70L)
                .setWorkOrderId(40L)
                .setBackfillType(type)
                .setStatus("SUCCESS");
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
