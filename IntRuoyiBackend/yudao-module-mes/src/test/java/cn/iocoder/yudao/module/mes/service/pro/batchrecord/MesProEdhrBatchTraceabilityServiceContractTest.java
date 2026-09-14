package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTraceLinkDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionOriginDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTraceManifestDO;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrBatchTraceSourcePrecheckRespVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import cn.hutool.crypto.digest.DigestUtil;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityErrorCodeConstants.FLOW8_SOURCE_PRECHECK_STALE;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityErrorCodeConstants.FLOW8_SOURCE_PRECHECK_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityErrorCodeConstants.FLOW8_TRACE_LINK_ORIGIN_MISMATCH;

class MesProEdhrBatchTraceabilityServiceContractTest {

    @Test
    void sameSnapshotFromSameOriginIsIdempotent() {
        MesProEdhrBatchExecutionTraceLinkDO existing = MesProEdhrBatchExecutionTraceLinkDO.builder()
                .originId(1L).snapshotHash("hash").build();

        assertTrue(MesProEdhrBatchTraceabilityServiceImpl.isIdempotentTraceLink(existing, 1L, "hash"));
    }

    @Test
    void sameSnapshotFromDifferentOriginIsNotSilentlyReused() {
        MesProEdhrBatchExecutionTraceLinkDO existing = MesProEdhrBatchExecutionTraceLinkDO.builder()
                .originId(1L).snapshotHash("hash").build();

        assertFalse(MesProEdhrBatchTraceabilityServiceImpl.isIdempotentTraceLink(existing, 2L, "hash"));
    }

    @Test
    void explicitOriginIdResolvesMultiOriginBatchWithoutGuessing() {
        MesProEdhrBatchExecutionOriginDO first = MesProEdhrBatchExecutionOriginDO.builder().id(11L).build();
        MesProEdhrBatchExecutionOriginDO second = MesProEdhrBatchExecutionOriginDO.builder().id(22L).build();

        assertEquals(22L, MesProEdhrBatchTraceabilityServiceImpl.resolveReleaseOriginId(List.of(first, second), 22L));
        assertNull(MesProEdhrBatchTraceabilityServiceImpl.resolveReleaseOriginId(List.of(first, second), 33L));
    }

    @Test
    void releaseDecisionLinksExposeBatchIdsForListFiltering() {
        MesProEdhrBatchExecutionTraceLinkDO release = MesProEdhrBatchExecutionTraceLinkDO.builder()
                .batchExecutionId(101L).linkType(MesProEdhrBatchTraceLinkType.RELEASE_DECISION)
                .sourceObjectType("RELEASE_APPLICATION").sourceObjectId(9001L).build();
        MesProEdhrBatchExecutionTraceLinkDO unrelated = MesProEdhrBatchExecutionTraceLinkDO.builder()
                .batchExecutionId(202L).linkType(MesProEdhrBatchTraceLinkType.RELEASE_DECISION)
                .sourceObjectType("RELEASE_APPLICATION").sourceObjectId(9002L).build();

        assertEquals(List.of(101L), MesProEdhrBatchTraceabilityServiceImpl
                .batchIdsForReleaseApplication(List.of(release, unrelated), 9001L));
    }

    @Test
    void txCProducerRequiresTenantBoundBatchBeforeReadingProvisionAudit() {
        assertTrue(MesProEdhrBatchTraceTxCProducer.isTenantVisible(7L, 7L));
        assertFalse(MesProEdhrBatchTraceTxCProducer.isTenantVisible(7L, 8L));
        assertFalse(MesProEdhrBatchTraceTxCProducer.isTenantVisible(null, 7L));
    }

    @Test
    void missingTraceGraphIsNotReportedAsCaptured() {
        MesProEdhrBatchExecutionOriginDO origin = MesProEdhrBatchExecutionOriginDO.builder()
                .id(1L).batchExecutionId(101L).entryType(MesProEdhrBatchTraceEntryType.MANUAL).build();
        MesProEdhrBatchExecutionTraceLinkDO workOrder = traceLink(2L, 1L,
                MesProEdhrBatchTraceLinkType.WORK_ORDER, "WORK_ORDER", 8L);
        MesProEdhrBatchExecutionTraceLinkDO provision = traceLink(3L, 1L,
                MesProEdhrBatchTraceLinkType.BATCH_PROVISION_RECEIPT,
                "BATCH_PROVISION_RECEIPT", 13L);
        String manifestJson = "{\"batchExecutionId\":101}";
        MesProEdhrBatchExecutionTraceManifestDO manifest = MesProEdhrBatchExecutionTraceManifestDO.builder()
                .id(3L).batchExecutionId(101L).manifestVersion(1).manifestJson(manifestJson)
                .manifestHash(DigestUtil.sha256Hex(
                        MesProBatchRecordExecutionFieldAuditHasher.canonicalizeJsonString(manifestJson))).build();

        assertTrue(MesProEdhrBatchTraceabilityServiceImpl.isTraceCaptured(
                List.of(origin), List.of(workOrder, provision), List.of(manifest)));
        assertFalse(MesProEdhrBatchTraceabilityServiceImpl.isTraceCaptured(
                List.of(origin), List.of(workOrder), List.of(manifest)));
        assertFalse(MesProEdhrBatchTraceabilityServiceImpl.isTraceCaptured(List.of(origin), List.of(), List.of(manifest)));
    }

    @Test
    void manifestHistoryMustBeHashLinkedAndCanonical() {
        String firstJson = "{\"version\":1}";
        String firstHash = DigestUtil.sha256Hex(
                MesProBatchRecordExecutionFieldAuditHasher.canonicalizeJsonString(firstJson));
        String secondJson = "{\"version\":2}";
        String secondHash = DigestUtil.sha256Hex(
                MesProBatchRecordExecutionFieldAuditHasher.canonicalizeJsonString(secondJson));
        MesProEdhrBatchExecutionTraceManifestDO first = MesProEdhrBatchExecutionTraceManifestDO.builder()
                .id(1L).batchExecutionId(101L).manifestVersion(1).manifestJson(firstJson)
                .manifestHash(firstHash).build();
        MesProEdhrBatchExecutionTraceManifestDO second = MesProEdhrBatchExecutionTraceManifestDO.builder()
                .id(2L).batchExecutionId(101L).manifestVersion(2).previousManifestHash(firstHash)
                .manifestJson(secondJson).manifestHash(secondHash).build();

        assertTrue(MesProEdhrBatchTraceabilityServiceImpl.isManifestHistoryValid(List.of(first, second)));

        second.setManifestHash("tampered");
        assertFalse(MesProEdhrBatchTraceabilityServiceImpl.isManifestHistoryValid(List.of(first, second)));
    }

    @Test
    void capturedGraphMustUseTheFormalLossDecisionRelation() {
        MesProEdhrBatchExecutionOriginDO origin = MesProEdhrBatchExecutionOriginDO.builder()
                .id(1L).entryType(MesProEdhrBatchTraceEntryType.ACTIVE_ORDER_COMPLETION)
                .hasActualLoss(false).build();
        MesProEdhrBatchExecutionTraceLinkDO lossFact = MesProEdhrBatchExecutionTraceLinkDO.builder()
                .originId(1L).linkType(MesProEdhrBatchTraceLinkType.LOSS_FACT)
                .relationStatus("HAS_LOSS").build();
        MesProEdhrBatchExecutionTraceLinkDO noLoss = MesProEdhrBatchExecutionTraceLinkDO.builder()
                .originId(1L).linkType(MesProEdhrBatchTraceLinkType.NO_LOSS_CONFIRMED)
                .relationStatus("NO_LOSS").build();

        assertFalse(MesProEdhrBatchTraceabilityServiceImpl.isLossRelationConsistent(origin,
                List.of(lossFact)));
        assertTrue(MesProEdhrBatchTraceabilityServiceImpl.isLossRelationConsistent(origin,
                List.of(noLoss)));
    }

    @Test
    void activeOrderAliasesMustStillEnforceLossDecisionRelation() {
        MesProEdhrBatchExecutionOriginDO origin = MesProEdhrBatchExecutionOriginDO.builder()
                .id(1L).entryType("ACTIVE_ORDER_SCHEDULED")
                .hasActualLoss(false).build();
        MesProEdhrBatchExecutionTraceLinkDO lossFact = MesProEdhrBatchExecutionTraceLinkDO.builder()
                .originId(1L).linkType(MesProEdhrBatchTraceLinkType.LOSS_FACT)
                .relationStatus("HAS_LOSS").build();

        assertFalse(MesProEdhrBatchTraceabilityServiceImpl.isLossRelationConsistent(origin,
                List.of(lossFact)));
    }

    @Test
    void duplicateLossFactsMustBeRejectedInsteadOfLettingOneGoodRowHideOneBadRow() {
        MesProEdhrBatchExecutionOriginDO origin = MesProEdhrBatchExecutionOriginDO.builder()
                .id(1L).entryType(MesProEdhrBatchTraceEntryType.ACTIVE_ORDER_COMPLETION)
                .hasActualLoss(true).build();
        MesProEdhrBatchExecutionTraceLinkDO lossFact = MesProEdhrBatchExecutionTraceLinkDO.builder()
                .originId(1L).linkType(MesProEdhrBatchTraceLinkType.LOSS_FACT)
                .relationStatus("HAS_LOSS").build();
        MesProEdhrBatchExecutionTraceLinkDO duplicateLossFact = MesProEdhrBatchExecutionTraceLinkDO.builder()
                .originId(1L).linkType(MesProEdhrBatchTraceLinkType.LOSS_FACT)
                .relationStatus("NO_LOSS").build();
        MesProEdhrBatchExecutionTraceLinkDO lossReport = MesProEdhrBatchExecutionTraceLinkDO.builder()
                .originId(1L).linkType(MesProEdhrBatchTraceLinkType.LOSS_REPORT_RECEIPT)
                .build();

        assertFalse(MesProEdhrBatchTraceabilityServiceImpl.isLossRelationConsistent(origin,
                List.of(lossFact, duplicateLossFact, lossReport)));
    }

    @Test
    void tamperedTraceLinkIdentityOrSnapshotBlocksCapturedGraph() {
        MesProEdhrBatchExecutionOriginDO origin = MesProEdhrBatchExecutionOriginDO.builder()
                .id(1L).batchExecutionId(101L).entryType(MesProEdhrBatchTraceEntryType.MANUAL).build();
        String workOrderJson = "{\"workOrderId\":8}";
        String workOrderHash = DigestUtil.sha256Hex(
                MesProBatchRecordExecutionFieldAuditHasher.canonicalizeJsonString(workOrderJson));
        MesProEdhrBatchExecutionTraceLinkDO workOrder = MesProEdhrBatchExecutionTraceLinkDO.builder()
                .id(2L).batchExecutionId(101L).originId(1L).linkType(MesProEdhrBatchTraceLinkType.WORK_ORDER)
                .sourceObjectType("WORK_ORDER").sourceObjectId(8L).sourceIdentityKey("caller-selected-identity")
                .snapshotJson(workOrderJson).snapshotHash(workOrderHash).build();
        String provisionJson = "{\"receiptId\":13}";
        String provisionHash = DigestUtil.sha256Hex(
                MesProBatchRecordExecutionFieldAuditHasher.canonicalizeJsonString(provisionJson));
        MesProEdhrBatchExecutionTraceLinkDO provision = MesProEdhrBatchExecutionTraceLinkDO.builder()
                .id(3L).batchExecutionId(101L).originId(1L)
                .linkType(MesProEdhrBatchTraceLinkType.BATCH_PROVISION_RECEIPT)
                .sourceObjectType("BATCH_PROVISION_RECEIPT").sourceObjectId(13L)
                .sourceIdentityKey("BATCH_PROVISION_RECEIPT:BATCH_PROVISION_RECEIPT:13::")
                .snapshotJson(provisionJson).snapshotHash(provisionHash).build();
        String manifestJson = "{\"batchExecutionId\":101}";
        MesProEdhrBatchExecutionTraceManifestDO manifest = MesProEdhrBatchExecutionTraceManifestDO.builder()
                .id(4L).batchExecutionId(101L).manifestVersion(1).manifestJson(manifestJson)
                .manifestHash(DigestUtil.sha256Hex(
                        MesProBatchRecordExecutionFieldAuditHasher.canonicalizeJsonString(manifestJson))).build();

        assertFalse(MesProEdhrBatchTraceabilityServiceImpl.isTraceCaptured(
                List.of(origin), List.of(workOrder, provision), List.of(manifest)));
    }

    @Test
    void duplicateTraceLinkTypesOnTheSameOriginMustBlockCapturedGraph() {
        MesProEdhrBatchExecutionOriginDO origin = MesProEdhrBatchExecutionOriginDO.builder()
                .id(1L).batchExecutionId(101L).entryType(MesProEdhrBatchTraceEntryType.MANUAL).build();
        MesProEdhrBatchExecutionTraceLinkDO first = traceLink(2L, 1L,
                MesProEdhrBatchTraceLinkType.WORK_ORDER, "WORK_ORDER", 8L);
        MesProEdhrBatchExecutionTraceLinkDO second = traceLink(3L, 1L,
                MesProEdhrBatchTraceLinkType.WORK_ORDER, "WORK_ORDER", 9L);
        String manifestJson = "{\"batchExecutionId\":101}";
        MesProEdhrBatchExecutionTraceManifestDO manifest = MesProEdhrBatchExecutionTraceManifestDO.builder()
                .id(4L).batchExecutionId(101L).manifestVersion(1).manifestJson(manifestJson)
                .manifestHash(DigestUtil.sha256Hex(
                        MesProBatchRecordExecutionFieldAuditHasher.canonicalizeJsonString(manifestJson))).build();

        assertFalse(MesProEdhrBatchTraceabilityServiceImpl.isTraceCaptured(
                List.of(origin), List.of(first, second), List.of(manifest)));
    }

    @Test
    void flow8SourcePrecheckReadsFormalFourFieldContract() {
        MesProEdhrBatchExecutionOriginDO origin = MesProEdhrBatchExecutionOriginDO.builder()
                .id(1L).batchExecutionId(101L).sourceSnapshotHash("origin-snapshot-v1").build();
        MesProEdhrBatchExecutionTraceLinkDO link = traceLink(2L, 1L,
                MesProEdhrBatchTraceLinkType.WORK_ORDER, "WORK_ORDER", 8L)
                .setRelationStatus("CAPTURED").setSourceVersion(3);

        MesProEdhrBatchTraceSourcePrecheckRespVO snapshot =
                MesProEdhrBatchTraceabilityServiceImpl.resolveSourcePrecheck(
                        new MesProEdhrBatchTraceSourcePrecheckCommand()
                                .setBatchExecutionId(101L).setOriginLinkId(2L),
                        origin, link, java.time.LocalDateTime.of(2026, 8, 23, 12, 0));

        assertNotNull(snapshot);
        assertEquals(101L, snapshot.getBatchExecutionId());
        assertEquals(2L, snapshot.getOriginLinkId());
        assertEquals(link.getSnapshotHash(), snapshot.getTraceLinkHash());
        assertEquals(origin.getSourceSnapshotHash(), snapshot.getSourceSnapshotHash());
        assertEquals(3, snapshot.getSourceVersion());
    }

    @Test
    void flow8SourcePrecheckFailsFastWhenSourceChangesAfterPrecheck() {
        MesProEdhrBatchExecutionOriginDO initialOrigin = MesProEdhrBatchExecutionOriginDO.builder()
                .id(1L).batchExecutionId(101L).sourceSnapshotHash("origin-snapshot-v1").build();
        MesProEdhrBatchExecutionTraceLinkDO initialLink = traceLink(2L, 1L,
                MesProEdhrBatchTraceLinkType.WORK_ORDER, "WORK_ORDER", 8L)
                .setRelationStatus("CAPTURED").setSourceVersion(3);
        MesProEdhrBatchTraceSourcePrecheckRespVO precheck =
                MesProEdhrBatchTraceabilityServiceImpl.resolveSourcePrecheck(
                        new MesProEdhrBatchTraceSourcePrecheckCommand()
                                .setBatchExecutionId(101L).setOriginLinkId(2L),
                        initialOrigin, initialLink, java.time.LocalDateTime.now());

        MesProEdhrBatchExecutionOriginDO changedOrigin = MesProEdhrBatchExecutionOriginDO.builder()
                .id(1L).batchExecutionId(101L).sourceSnapshotHash("origin-snapshot-v2").build();
        String changedJson = "{\"sourceId\":9}";
        MesProEdhrBatchExecutionTraceLinkDO changedLink = traceLink(2L, 1L,
                MesProEdhrBatchTraceLinkType.WORK_ORDER, "WORK_ORDER", 9L)
                .setRelationStatus("CAPTURED").setSourceVersion(4)
                .setSnapshotJson(changedJson)
                .setSnapshotHash(DigestUtil.sha256Hex(
                        MesProBatchRecordExecutionFieldAuditHasher.canonicalizeJsonString(changedJson)))
                .setSourceIdentityKey("WORK_ORDER:WORK_ORDER:9::");

        ServiceException exception = assertThrows(ServiceException.class, () ->
                MesProEdhrBatchTraceabilityServiceImpl.resolveSourcePrecheck(
                        new MesProEdhrBatchTraceSourcePrecheckCommand()
                                .setBatchExecutionId(101L).setOriginLinkId(2L)
                                .setExpectedTraceLinkHash(precheck.getTraceLinkHash())
                                .setExpectedSourceSnapshotHash(precheck.getSourceSnapshotHash())
                                .setExpectedSourceVersion(precheck.getSourceVersion()),
                        changedOrigin, changedLink, java.time.LocalDateTime.now()));

        assertEquals(FLOW8_SOURCE_PRECHECK_STALE.getCode(), exception.getCode());
    }

    @Test
    void flow8SourcePrecheckPreservesBatchOriginLinkMismatchEvidence() {
        MesProEdhrBatchExecutionOriginDO origin = MesProEdhrBatchExecutionOriginDO.builder()
                .id(1L).batchExecutionId(101L).sourceSnapshotHash("origin-snapshot-v1").build();
        MesProEdhrBatchExecutionTraceLinkDO link = traceLink(2L, 1L,
                MesProEdhrBatchTraceLinkType.WORK_ORDER, "WORK_ORDER", 8L)
                .setBatchExecutionId(202L).setRelationStatus("CAPTURED");

        ServiceException exception = assertThrows(ServiceException.class, () ->
                MesProEdhrBatchTraceabilityServiceImpl.resolveSourcePrecheck(
                        new MesProEdhrBatchTraceSourcePrecheckCommand()
                                .setBatchExecutionId(101L).setOriginLinkId(2L),
                        origin, link, java.time.LocalDateTime.now()));

        assertEquals(FLOW8_TRACE_LINK_ORIGIN_MISMATCH.getCode(), exception.getCode());
    }

    @Test
    void flow8SourcePrecheckSelectsTheSingleFormalBatchProvisionLinkWhenCallerHasNoLinkId() {
        MesProEdhrBatchExecutionOriginDO origin = MesProEdhrBatchExecutionOriginDO.builder()
                .id(1L).batchExecutionId(101L).entryType(MesProEdhrBatchTraceEntryType.ACTIVE_ORDER_COMPLETION)
                .sourceSnapshotHash("origin-snapshot-v1").build();
        MesProEdhrBatchExecutionTraceLinkDO provision = traceLink(3L, 1L,
                MesProEdhrBatchTraceLinkType.BATCH_PROVISION_RECEIPT, "BATCH_PROVISION_RECEIPT", 13L)
                .setRelationStatus("CAPTURED");

        MesProEdhrBatchTraceSourcePrecheckRespVO snapshot =
                MesProEdhrBatchTraceabilityServiceImpl.resolveSourcePrecheckWithoutLinkId(
                        new MesProEdhrBatchTraceSourcePrecheckCommand().setBatchExecutionId(101L),
                        List.of(origin), List.of(provision), java.time.LocalDateTime.of(2026, 8, 24, 12, 0));

        assertEquals(3L, snapshot.getOriginLinkId());
        assertEquals("origin-snapshot-v1", snapshot.getSourceSnapshotHash());
    }

    @Test
    void flow8SourcePrecheckBlocksWhenFormalBatchProvisionLinkIsAmbiguous() {
        MesProEdhrBatchExecutionOriginDO origin = MesProEdhrBatchExecutionOriginDO.builder()
                .id(1L).batchExecutionId(101L).entryType(MesProEdhrBatchTraceEntryType.ACTIVE_ORDER_COMPLETION)
                .sourceSnapshotHash("origin-snapshot-v1").build();
        MesProEdhrBatchExecutionTraceLinkDO first = traceLink(3L, 1L,
                MesProEdhrBatchTraceLinkType.BATCH_PROVISION_RECEIPT, "BATCH_PROVISION_RECEIPT", 13L)
                .setRelationStatus("CAPTURED");
        MesProEdhrBatchExecutionTraceLinkDO second = traceLink(4L, 1L,
                MesProEdhrBatchTraceLinkType.BATCH_PROVISION_RECEIPT, "BATCH_PROVISION_RECEIPT", 14L)
                .setRelationStatus("CAPTURED");

        ServiceException exception = assertThrows(ServiceException.class, () ->
                MesProEdhrBatchTraceabilityServiceImpl.resolveSourcePrecheckWithoutLinkId(
                        new MesProEdhrBatchTraceSourcePrecheckCommand().setBatchExecutionId(101L),
                        List.of(origin), List.of(first, second), java.time.LocalDateTime.now()));

        assertEquals(FLOW8_SOURCE_PRECHECK_REQUIRED.getCode(), exception.getCode());
    }

    private MesProEdhrBatchExecutionTraceLinkDO traceLink(Long id, Long originId, String linkType,
                                                          String sourceObjectType, Long sourceObjectId) {
        String snapshotJson = "{\"sourceId\":" + sourceObjectId + "}";
        String snapshotHash = DigestUtil.sha256Hex(
                MesProBatchRecordExecutionFieldAuditHasher.canonicalizeJsonString(snapshotJson));
        String identity = String.join(":", linkType, sourceObjectType, String.valueOf(sourceObjectId), "", "");
        return MesProEdhrBatchExecutionTraceLinkDO.builder().id(id).batchExecutionId(101L).originId(originId)
                .linkType(linkType).sourceObjectType(sourceObjectType).sourceObjectId(sourceObjectId)
                .sourceIdentityKey(identity).snapshotJson(snapshotJson).snapshotHash(snapshotHash).build();
    }
}
