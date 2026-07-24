package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeOpsBusinessHealthCheckResult;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeOpsBusinessHealthCollector;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeOpsInspectionStatus;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionArchiveDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionArchiveEventDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionArchiveEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionArchiveMapper;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Import(MesProBatchRecordExecutionArchiveBusinessHealthCollector.class)
class MesProBatchRecordExecutionArchiveBusinessHealthCollectorTest extends BaseDbUnitTest {

    private static final String HEALTH_CODE = "edhr-archive-integrity";
    private static final String HEALTH_NAME = "eDHR 归档完整性";
    private static final Long ACTOR_ID = 101L;
    private static final String VALID_SHA256 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    @Resource
    private MesProBatchRecordExecutionArchiveBusinessHealthCollector collector;
    @Resource
    private MesProBatchRecordExecutionArchiveMapper archiveMapper;
    @Resource
    private MesProBatchRecordExecutionArchiveEventMapper archiveEventMapper;

    @Test
    @DisplayName("BDD: collector exposes edhr archive integrity code and runtime business health contract")
    void collect_exposesArchiveIntegrityCodeAndName() {
        assertTrue(collector instanceof RuntimeOpsBusinessHealthCollector);

        RuntimeOpsBusinessHealthCheckResult result = collectWithoutMutatingArchiveTables();

        assertEquals(HEALTH_CODE, result.getCode());
        assertEquals(HEALTH_NAME, result.getName());
        assertNotNull(result.getSampledAt());
    }

    @Test
    @DisplayName("BDD: SEALED archive without ARCHIVE_SEAL blocks archive integrity")
    void collect_sealedArchiveMissingSealEvent_blocks() {
        MesProBatchRecordExecutionArchiveDO archive = insertArchive("SEALED", 5001L, VALID_SHA256);
        insertStorageRetentionSourceEvent(archive, 5001L, VALID_SHA256);

        RuntimeOpsBusinessHealthCheckResult result = collectWithoutMutatingArchiveTables();

        assertBlocked(result);
        assertSignalContains(result, "ARCHIVE_SEAL");
        assertSignalContains(result, "missingCount=1");
        assertSignalContains(result, "missingSummary=ARCHIVE_SEAL");
        assertSignalContains(result, String.valueOf(archive.getId()));
    }

    @Test
    @DisplayName("BDD: ARCHIVE_SEAL null metadata and GENERATE_SUCCESS storageRetention remain separated")
    void collect_sealMetadataNullButGenerateSuccessStorageRetentionComplete_passes() {
        MesProBatchRecordExecutionArchiveDO archive = insertArchive("SEALED", 5002L, VALID_SHA256);
        insertEvent(archive, "ARCHIVE_SEAL", null);
        insertStorageRetentionSourceEvent(archive, 5002L, VALID_SHA256);

        RuntimeOpsBusinessHealthCheckResult result = collectWithoutMutatingArchiveTables();

        assertEquals(RuntimeOpsInspectionStatus.PASS, result.getStatus());
        assertSignalContains(result, "sealed=1");
        assertSignalContains(result, "failed=0");
        assertSignalContains(result, "GENERATE_SUCCESS");
        assertStorageRetentionSignal(result, 5002L);
    }

    @Test
    @DisplayName("BDD: SEALED archive without append-only storageRetention source event blocks")
    void collect_missingStorageRetentionSourceEvent_blocks() {
        MesProBatchRecordExecutionArchiveDO archive = insertArchive("SEALED", 5003L, VALID_SHA256);
        insertEvent(archive, "ARCHIVE_SEAL", null);

        RuntimeOpsBusinessHealthCheckResult result = collectWithoutMutatingArchiveTables();

        assertBlocked(result);
        assertSignalContains(result, "storageRetention");
        assertSignalContains(result, "source event");
        assertSignalContains(result, String.valueOf(archive.getId()));
    }

    @Test
    @DisplayName("BDD: storageRetention fileId mismatch blocks archive integrity")
    void collect_storageRetentionFileIdMismatch_blocks() {
        MesProBatchRecordExecutionArchiveDO archive = insertArchive("SEALED", 5004L, VALID_SHA256);
        insertEvent(archive, "ARCHIVE_SEAL", null);
        insertStorageRetentionSourceEvent(archive, 9999L, VALID_SHA256);

        RuntimeOpsBusinessHealthCheckResult result = collectWithoutMutatingArchiveTables();

        assertBlocked(result);
        assertSignalContains(result, "fileId");
        assertSignalContains(result, String.valueOf(archive.getId()));
        assertStorageRetentionSignal(result, 9999L);
    }

    @Test
    @DisplayName("BDD: storageRetention sha256 mismatch blocks archive integrity")
    void collect_storageRetentionSha256Mismatch_blocks() {
        MesProBatchRecordExecutionArchiveDO archive = insertArchive("SEALED", 5005L, VALID_SHA256);
        insertEvent(archive, "ARCHIVE_SEAL", null);
        insertStorageRetentionSourceEvent(archive, 5005L,
                "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");

        RuntimeOpsBusinessHealthCheckResult result = collectWithoutMutatingArchiveTables();

        assertBlocked(result);
        assertSignalContains(result, "sha256");
        assertSignalContains(result, String.valueOf(archive.getId()));
        assertStorageRetentionSignal(result, 5005L);
    }

    @Test
    @DisplayName("BDD: invalid source metadata JSON blocks archive integrity without collector failure")
    void collect_invalidSourceMetadataJson_blocks() {
        MesProBatchRecordExecutionArchiveDO archive = insertArchive("SEALED", 5008L, VALID_SHA256);
        insertEvent(archive, "ARCHIVE_SEAL", null);
        insertEvent(archive, "GENERATE_SUCCESS", "{not-valid-json");

        RuntimeOpsBusinessHealthCheckResult result = collectWithoutMutatingArchiveTables();

        assertBlocked(result);
        assertSignalContains(result, String.valueOf(archive.getId()));
        assertSignalContains(result, "sourceEvent=GENERATE_SUCCESS");
        assertSignalContains(result, "invalid metadata");
    }

    @Test
    @DisplayName("BDD: storageRetention field type error blocks archive integrity without collector failure")
    void collect_storageRetentionFileIdTypeError_blocks() {
        MesProBatchRecordExecutionArchiveDO archive = insertArchive("SEALED", 5009L, VALID_SHA256);
        insertEvent(archive, "ARCHIVE_SEAL", null);
        insertStorageRetentionSourceEventWithFileIdValue(archive, "not-a-number", VALID_SHA256);

        RuntimeOpsBusinessHealthCheckResult result = collectWithoutMutatingArchiveTables();

        assertBlocked(result);
        assertSignalContains(result, String.valueOf(archive.getId()));
        assertSignalContains(result, "sourceEvent=GENERATE_SUCCESS");
        assertSignalContains(result, "invalid metadata");
        assertSignalContains(result, "fileId");
        assertSignalContains(result, "object-version-not-a-number");
        assertSignalContains(result, "2036-05-28T00:00:00Z");
        assertSignalContains(result, "2026-05-28T02:30:00Z");
    }

    @Test
    @DisplayName("BDD: complete SEALED archives with FAILED archive return WARN and expose failed evidence")
    void collect_completeSealedWithFailedArchive_warns() {
        MesProBatchRecordExecutionArchiveDO sealed = insertArchive("SEALED", 5006L, VALID_SHA256);
        insertEvent(sealed, "ARCHIVE_SEAL", null);
        insertStorageRetentionSourceEvent(sealed, 5006L, VALID_SHA256);
        MesProBatchRecordExecutionArchiveDO failed = insertArchive("FAILED", 5007L, VALID_SHA256);

        RuntimeOpsBusinessHealthCheckResult result = collectWithoutMutatingArchiveTables();

        assertEquals(RuntimeOpsInspectionStatus.WARN, result.getStatus());
        assertSignalContains(result, "sealed=1");
        assertSignalContains(result, "failed=1");
        assertSignalContains(result, String.valueOf(failed.getId()));
        assertStorageRetentionSignal(result, 5006L);
    }

    @Test
    @DisplayName("BDD: controlled voided sealed archive is counted but not revalidated as active sealed archive")
    void collect_voidedSealedArchive_countsControlledInvalidArchive() {
        MesProBatchRecordExecutionArchiveDO voided = insertArchive("SEALED", 5011L, VALID_SHA256);
        archiveMapper.updateById(new MesProBatchRecordExecutionArchiveDO()
                .setId(voided.getId())
                .setArchiveValidFlag(Boolean.FALSE)
                .setArchiveValidStatus("VOIDED")
                .setInvalidatedByChangeEventId(91001L));

        RuntimeOpsBusinessHealthCheckResult result = collectWithoutMutatingArchiveTables();

        assertEquals(RuntimeOpsInspectionStatus.PASS, result.getStatus());
        assertSignalContains(result, "sealed=0");
        assertSignalContains(result, "controlledInvalid=1");
        assertSignalContains(result, "VOIDED=1");
        assertSignalContains(result, "changeEventIds=91001");
    }

    @Test
    @DisplayName("BDD: empty archive data passes with explicit sealed and failed evidence")
    void collect_emptyArchiveData_passesWithExplicitEvidence() {
        RuntimeOpsBusinessHealthCheckResult result = collectWithoutMutatingArchiveTables();

        assertEquals(RuntimeOpsInspectionStatus.PASS, result.getStatus());
        assertSignalContains(result, "sealed=0");
        assertSignalContains(result, "failed=0");
    }

    @Test
    @DisplayName("BDD: archive mapper query failure blocks archive integrity with collector context")
    void collect_archiveMapperQueryFailure_blocksWithCollectorContext() {
        MesProBatchRecordExecutionArchiveMapper failingArchiveMapper =
                mock(MesProBatchRecordExecutionArchiveMapper.class);
        when(failingArchiveMapper.selectList(any())).thenThrow(new IllegalStateException("archive query failed"));

        RuntimeOpsBusinessHealthCheckResult result = collectWithArchiveMapper(failingArchiveMapper);

        assertCollectorBlockedWithContext(result, "archiveMapper", "archive query failed");
    }

    @Test
    @DisplayName("BDD: archive event mapper query failure blocks archive integrity with collector context")
    void collect_archiveEventMapperQueryFailure_blocksWithCollectorContext() {
        MesProBatchRecordExecutionArchiveDO archive = insertArchive("SEALED", 5010L, VALID_SHA256);
        MesProBatchRecordExecutionArchiveEventMapper failingEventMapper =
                mock(MesProBatchRecordExecutionArchiveEventMapper.class);
        when(failingEventMapper.selectListByArchiveId(archive.getId()))
                .thenThrow(new IllegalStateException("archive event query failed"));

        RuntimeOpsBusinessHealthCheckResult result = collectWithArchiveEventMapper(failingEventMapper);

        assertCollectorBlockedWithContext(result, "archiveEventMapper", "archive event query failed");
        assertSignalContains(result, String.valueOf(archive.getId()));
    }

    @Test
    @DisplayName("BDD: missing archive mapper blocks archive integrity with dependency context")
    void collect_missingArchiveMapper_blocksWithCollectorContext() {
        RuntimeOpsBusinessHealthCheckResult result = collectWithArchiveMapper(null);

        assertCollectorBlockedWithContext(result, "archiveMapper", "missing");
    }

    @Test
    @DisplayName("BDD: missing archive event mapper blocks archive integrity with dependency context")
    void collect_missingArchiveEventMapper_blocksWithCollectorContext() {
        RuntimeOpsBusinessHealthCheckResult result = collectWithArchiveEventMapper(null);

        assertCollectorBlockedWithContext(result, "archiveEventMapper", "missing");
    }

    private RuntimeOpsBusinessHealthCheckResult collectWithoutMutatingArchiveTables() {
        long archiveCount = archiveMapper.selectCount();
        long eventCount = archiveEventMapper.selectCount();

        RuntimeOpsBusinessHealthCheckResult result = collector.collect();

        assertEquals(archiveCount, archiveMapper.selectCount(), "collector must not create/delete archive rows");
        assertEquals(eventCount, archiveEventMapper.selectCount(), "collector must not create/delete archive event rows");
        return result;
    }

    private void assertBlocked(RuntimeOpsBusinessHealthCheckResult result) {
        assertEquals(RuntimeOpsInspectionStatus.BLOCKED, result.getStatus());
    }

    private void assertSignalContains(RuntimeOpsBusinessHealthCheckResult result, String expected) {
        assertTrue(signal(result).contains(expected),
                () -> "Expected business health signal to contain `" + expected + "`, actual=" + signal(result));
    }

    private void assertStorageRetentionSignal(RuntimeOpsBusinessHealthCheckResult result, Long fileId) {
        assertSignalContains(result, "object-version-" + fileId);
        assertSignalContains(result, "2036-05-28T00:00:00Z");
        assertSignalContains(result, "2026-05-28T02:30:00Z");
    }

    private void assertCollectorBlockedWithContext(RuntimeOpsBusinessHealthCheckResult result, String context,
                                                   String detail) {
        assertEquals(HEALTH_CODE, result.getCode());
        assertBlocked(result);
        assertSignalContains(result, context);
        assertSignalContains(result, detail);
        assertFalse(signal(result).contains("business-health-collector-failed"),
                () -> "Collector must expose its own archive integrity BLOCKED reason, actual=" + signal(result));
    }

    private String signal(RuntimeOpsBusinessHealthCheckResult result) {
        return String.valueOf(result.getEvidence()) + "\n" + String.valueOf(result.getReason());
    }

    private RuntimeOpsBusinessHealthCheckResult collectWithArchiveMapper(
            MesProBatchRecordExecutionArchiveMapper replacement) {
        ReflectionTestUtils.setField(collector, "archiveMapper", replacement);
        try {
            return collector.collect();
        } finally {
            ReflectionTestUtils.setField(collector, "archiveMapper", archiveMapper);
        }
    }

    private RuntimeOpsBusinessHealthCheckResult collectWithArchiveEventMapper(
            MesProBatchRecordExecutionArchiveEventMapper replacement) {
        ReflectionTestUtils.setField(collector, "archiveEventMapper", replacement);
        try {
            return collector.collect();
        } finally {
            ReflectionTestUtils.setField(collector, "archiveEventMapper", archiveEventMapper);
        }
    }

    private MesProBatchRecordExecutionArchiveDO insertArchive(String status, Long fileId, String sha256) {
        long nextId = System.nanoTime();
        MesProBatchRecordExecutionArchiveDO archive = MesProBatchRecordExecutionArchiveDO.builder()
                .executionId(nextId)
                .archiveCode("EDHRA-" + nextId)
                .archiveVersion(1)
                .artifactType("PDF")
                .archiveStatus(status)
                .fileId(fileId)
                .fileName("edhr-execution-" + nextId + ".pdf")
                .contentType("application/pdf")
                .fileSize(128L)
                .sha256(sha256)
                .renderSourceVersion("EDHR_ARCHIVE_V1")
                .executionSnapshotHash("snapshot-" + nextId)
                .cellValuesHash("cell-values-" + nextId)
                .fieldAuditRevision(1L)
                .fieldAuditHeadHash("field-audit-" + nextId)
                .signatureHash("signature-" + nextId)
                .approvalSnapshotId(7000L + fileId)
                .approvalSnapshotHash("approval-" + nextId)
                .sealSignatureId(8000L + fileId)
                .generatedBy(ACTOR_ID)
                .generatedAt(LocalDateTime.now().minusMinutes(5))
                .sealedBy("SEALED".equals(status) ? ACTOR_ID : null)
                .sealedAt("SEALED".equals(status) ? LocalDateTime.now().minusMinutes(4) : null)
                .failureReason("FAILED".equals(status) ? "render failure" : null)
                .build();
        archiveMapper.insert(archive);
        return archive;
    }

    private void insertStorageRetentionSourceEvent(MesProBatchRecordExecutionArchiveDO archive, Long fileId,
                                                   String sha256) {
        insertStorageRetentionSourceEventWithFileIdValue(archive, fileId, sha256);
    }

    private void insertStorageRetentionSourceEventWithFileIdValue(MesProBatchRecordExecutionArchiveDO archive,
                                                                  Object fileId, String sha256) {
        JSONObject storageRetention = new JSONObject();
        storageRetention.put("fileId", fileId);
        storageRetention.put("sha256", sha256);
        storageRetention.put("objectVersionId", "object-version-" + fileId);
        storageRetention.put("retentionMode", "COMPLIANCE");
        storageRetention.put("retainUntil", "2036-05-28T00:00:00Z");
        storageRetention.put("legalHoldStatus", "ON");
        storageRetention.put("verifiedAt", "2026-05-28T02:30:00Z");
        storageRetention.put("bucket", "edhr-lock-bucket");
        storageRetention.put("path", "mes/edhr/archive/" + archive.getFileName());
        storageRetention.put("key", "mes/edhr/archive/" + archive.getFileName());
        JSONObject metadata = new JSONObject();
        metadata.put("storageRetention", storageRetention);
        insertEvent(archive, "GENERATE_SUCCESS", metadata.toJSONString());
    }

    private void insertEvent(MesProBatchRecordExecutionArchiveDO archive, String eventType, String metadataJson) {
        archiveEventMapper.insert(MesProBatchRecordExecutionArchiveEventDO.builder()
                .archiveId(archive.getId())
                .executionId(archive.getExecutionId())
                .eventType(eventType)
                .actorId(ACTOR_ID)
                .eventTime(LocalDateTime.now())
                .message(eventType + " event")
                .metadataJson(metadataJson)
                .build());
    }
}
