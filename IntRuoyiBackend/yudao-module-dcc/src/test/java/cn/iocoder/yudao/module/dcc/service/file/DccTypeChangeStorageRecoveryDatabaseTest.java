package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccWorkingApplicationStorageRecoveryReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccWorkingApplicationStorageRecoveryPreviewVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccFileDirectoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMetadataChangeDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectFolderStorageMappingDO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Real original legacy proof plus a separate declared recovery record; does not pretend to test the future command. */
class DccTypeChangeStorageRecoveryDatabaseTest extends DccWorkingApplicationTypeChangePlacementDatabaseTest {
    static final String RECOVERY_SOURCE = "APPLICANT_TYPE_STORAGE_RECOVERY";
    static final String RECOVERY_FORMAT = "DCC_NEW_REJECTED_TYPE_CHANGE_STORAGE_RECOVERY_V1";

    DccControlledFileMetadataChangeDO legacyDamagedProof() throws Exception {
        mappedActionFixture();
        var before = state(22L); // The original format really lacks projectFolderId.
        jdbc.update("UPDATE dcc_controlled_file SET category_id=3,file_type_taxonomy_id=8,draft_approval_matrix_json=? WHERE id=22", targetMatrix.canonical());
        jdbc.update("UPDATE dcc_controlled_file_master SET category_id=3,file_type_taxonomy_leaf_id=8 WHERE id=10");
        jdbc.update("UPDATE dcc_controlled_file_name_claim SET file_type_taxonomy_leaf_id=8 WHERE id=600");
        var proof = DccControlledFileMetadataChangeDO.builder().projectCodeId(5L).controlledFileId(22L).masterId(10L)
                .operatorUserId(99L).source(SOURCE).requestId(REQUEST).changeReason("Original confirmed type change before storage fix")
                .changedFieldCount(5).beforeSnapshotJson(JsonUtils.toJsonString(before))
                .afterSnapshotJson(JsonUtils.toJsonString(state(22L))).changedTime(LocalDateTime.now()).build();
        proof.setTenantId(1L);
        assertEquals(1, typeChanges.insert(proof));
        return typeChanges.selectById(proof.getId());
    }

    // Digest includes exact immutable identity and payload strings; a later mutation cannot borrow the new recovery.
    static String originalProofDigest(DccControlledFileMetadataChangeDO proof) throws Exception {
        String input = JsonUtils.toJsonString(Arrays.asList(proof.getId(), proof.getTenantId(), proof.getControlledFileId(),
                proof.getMasterId(), proof.getProjectCodeId(), proof.getOperatorUserId(), proof.getSource(), proof.getRequestId(),
                proof.getChangeReason(), proof.getBeforeSnapshotJson(), proof.getAfterSnapshotJson()));
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8)));
    }

    Map<String, Object> recoveryState(DccControlledFileMetadataChangeDO proof) throws Exception {
        var observed = state(22L);
        observed.put("projectFolderId", 36L); // Observed from the exact current AND source placement, never patched into the original proof.
        var envelope = new LinkedHashMap<String, Object>();
        envelope.put("formatVersion", RECOVERY_FORMAT);
        envelope.put("originalProofId", proof.getId());
        envelope.put("originalProofSha256", originalProofDigest(proof));
        envelope.put("observed", observed);
        return envelope;
    }

    void moveCurrentToTargetStorage() {
        directoryRows.insert(DccFileDirectoryDO.builder().id(32L).tenantId(1L).parentId(3L).code("PFS-RECOVERY-TARGET")
                .name("Verified target derived storage").active(true).sort(0).accessRuleManuallyBound(false).build());
        var mapping = new DccProjectFolderStorageMappingDO();
        mapping.setId(19L); mapping.setTenantId(1L); mapping.setProjectCodeId(5L); mapping.setProjectFolderId(36L);
        mapping.setCategoryId(3L); mapping.setBaseDirectoryId(3L); mapping.setStorageDirectoryId(32L);
        storageMappings.insert(mapping);
        jdbc.update("UPDATE dcc_controlled_file SET directory_id=32 WHERE id=22");
        jdbc.update("UPDATE dcc_controlled_file_master SET directory_id=32 WHERE id=10");
        jdbc.update("UPDATE dcc_project_file_placement SET storage_directory_id=32 WHERE controlled_file_id=22");
    }

    void declaredRecovery(DccControlledFileMetadataChangeDO proof, boolean validDigest) throws Exception {
        var before = recoveryState(proof);
        moveCurrentToTargetStorage();
        var after = recoveryState(proof);
        if (!validDigest) after.put("originalProofSha256", "0".repeat(64));
        var recovery = DccControlledFileMetadataChangeDO.builder().projectCodeId(5L).controlledFileId(22L).masterId(10L)
                .operatorUserId(99L).source(RECOVERY_SOURCE).requestId("c10-storage-recovery-22")
                .changeReason("Explicitly restore the current working location from registered source facts")
                .changedFieldCount(3).beforeSnapshotJson(JsonUtils.toJsonString(before))
                .afterSnapshotJson(JsonUtils.toJsonString(after)).changedTime(LocalDateTime.now()).build();
        recovery.setTenantId(1L);
        assertEquals(1, typeChanges.insert(recovery));
    }

    @Test void separateRecoveryEvidenceRestoresTargetContractWithoutRewritingOriginalProof() throws Exception {
        var proof = legacyDamagedProof();
        var rejected = original(20L); var predecessor = original(21L);
        var oldMapping = storageMappings.selectById(18L);
        var sourcePlacement = placementRows.findFile(21L);
        declaredRecovery(proof, true);
        assertDoesNotThrow(() -> tx().execute(s -> baselines.preview(99L, targetRequest(22L))),
                "An explicitly verified separate storage recovery must reconcile the old proof with the current location");
        assertEquals(targetMatrix.canonical(), tx().execute(s -> baselines.confirm(99L, 5L, 3L, 22L, "NEW", targetExpectation(22L))).source().canonical());
        assertEquals(proof, typeChanges.selectById(proof.getId()), "Original proof must remain byte-for-byte unchanged");
        assertEquals(rejected, original(20L)); assertEquals(predecessor, original(21L));
        assertEquals(oldMapping, storageMappings.selectById(18L)); assertEquals(sourcePlacement, placementRows.findFile(21L));
        assertEquals(1, typeChanges.selectListBySource(SOURCE).size());
        assertEquals(1, typeChanges.selectListBySource(RECOVERY_SOURCE).size());
    }

    @Test void formalRecoveryPreviewConfirmAndSameKeyReplayMoveOnlyCurrentPlacement() throws Exception {
        var proof=legacyDamagedProof();var oldFile=original(22L);var oldMaster=masters.selectById(10L);
        var oldSource=original(20L);var oldPredecessor=original(21L);var oldMapping=storageMappings.selectById(18L);
        var oldSourcePlacement=placementRows.findFile(21L);var preview=action.previewStorageRecovery(99L,22L);
        assertFalse(preview.recovered());assertEquals(proof.getId().toString(),preview.originalProofId());
        assertEquals(36L,Long.valueOf(preview.projectFolderId()));assertNull(preview.targetDirectoryId());
        var request=new DccWorkingApplicationStorageRecoveryReqVO(true,proof.getId(),preview.originalProofSha256(),
                preview.observedSha256(),"c10-recover-22","Restore target type storage for the current working file");
        DccWorkingApplicationStorageRecoveryPreviewVO receipt=tx().execute(s->action.confirmStorageRecovery(99L,22L,request));
        assertTrue(receipt.recovered());assertNotNull(receipt.recoveryEvidenceId());
        assertEquals(proof.getId().toString(),receipt.originalProofId());assertNotEquals(31L,Long.valueOf(receipt.targetDirectoryId()));
        assertEquals(Long.valueOf(receipt.targetDirectoryId()),files.selectById(22L).getDirectoryId());
        assertEquals(Long.valueOf(receipt.targetDirectoryId()),masters.selectById(10L).getDirectoryId());
        assertEquals(Long.valueOf(receipt.targetDirectoryId()),placementRows.findFile(22L).getStorageDirectoryId());
        assertEquals(oldMapping,storageMappings.selectById(18L));assertEquals(oldSourcePlacement,placementRows.findFile(21L));
        assertEquals(oldSource,original(20L));assertEquals(oldPredecessor,original(21L));assertEquals(proof,typeChanges.selectById(proof.getId()));
        assertEquals(1,typeChanges.selectListBySource(RECOVERY_SOURCE).size());
        var recoveredFile=original(22L);
        var replay=tx().execute(s->action.confirmStorageRecovery(99L,22L,request));
        assertEquals(receipt,replay);assertEquals(recoveredFile,original(22L));
        var changedPayload=new DccWorkingApplicationStorageRecoveryReqVO(true,proof.getId(),preview.originalProofSha256(),
                preview.observedSha256(),"c10-recover-22","Changed payload under the same idempotency key");
        assertThrows(IllegalArgumentException.class,()->tx().execute(s->action.confirmStorageRecovery(99L,22L,changedPayload)));
        assertEquals(recoveredFile,original(22L));assertEquals(1,typeChanges.selectListBySource(RECOVERY_SOURCE).size());
    }

    @Test void formalRecoveryLateFailureRollsBackReceiptMappingAndDirectory() throws Exception {
        legacyDamagedProof();var before=original(22L);var mapping=storageMappings.selectById(18L);
        var preview=action.previewStorageRecovery(99L,22L);
        var request=new DccWorkingApplicationStorageRecoveryReqVO(true,Long.valueOf(preview.originalProofId()),preview.originalProofSha256(),
                preview.observedSha256(),"c10-recover-rollback-22","Rollback on a late transactional failure");
        int directoryCount=directoryRows.selectList().size();var placement=placementRows.findFile(22L);
        var failure=assertThrows(IllegalStateException.class,()->tx().executeWithoutResult(s->{
            action.confirmStorageRecovery(99L,22L,request);throw new IllegalStateException("TEST_RECOVERY_LATE_FAILURE");
        }));
        assertEquals("TEST_RECOVERY_LATE_FAILURE",failure.getMessage());assertEquals(before,original(22L));
        assertEquals(mapping,storageMappings.selectById(18L));assertEquals(placement,placementRows.findFile(22L));
        assertEquals(31L,masters.selectById(10L).getDirectoryId());assertEquals(directoryCount,directoryRows.selectList().size());
        assertEquals(0,typeChanges.selectListBySource(RECOVERY_SOURCE).size());
    }

    @Test void movingDirectoryWithoutRecoveryEvidenceStillFails() throws Exception {
        var proof = legacyDamagedProof();
        moveCurrentToTargetStorage();
        assertThrows(IllegalArgumentException.class, () -> tx().execute(s -> baselines.preview(99L, targetRequest(22L))));
        assertEquals(proof, typeChanges.selectById(proof.getId()));
        assertEquals(0, typeChanges.selectListBySource(RECOVERY_SOURCE).size());
    }

    @Test void mismatchedOriginalProofDigestCannotAuthorizeRecovery() throws Exception {
        var proof = legacyDamagedProof(); declaredRecovery(proof, false);
        assertThrows(IllegalArgumentException.class, () -> tx().execute(s -> baselines.preview(99L, targetRequest(22L))));
        assertEquals(proof, typeChanges.selectById(proof.getId()));
    }

    @Test void originalDamagedProofCannotAuthorizeOrdinarySave() throws Exception {
        var proof = legacyDamagedProof();
        var current = original(22L);
        assertThrows(IllegalArgumentException.class,
                () -> tx().execute(s -> baselines.savedTypeChangeSource(99L, files.selectById(22L))));
        assertEquals(current, original(22L));
        assertEquals(proof, typeChanges.selectById(proof.getId()));
    }

    @ParameterizedTest
    @ValueSource(strings={"ACTOR", "PROOF_REFERENCE", "OBSERVED_HASH", "SOURCE_FOLDER", "DUPLICATE"})
    void recoveryCannotAuthorizeChangedIdentityOrAmbiguousEvidence(String damage) throws Exception {
        var proof = legacyDamagedProof();
        declaredRecovery(proof, true);
        var recovery = typeChanges.selectListBySource(RECOVERY_SOURCE).get(0);
        switch (damage) {
            case "ACTOR" -> recovery.setOperatorUserId(98L);
            case "PROOF_REFERENCE", "OBSERVED_HASH" -> {
                var after = JsonUtils.parseObject(recovery.getAfterSnapshotJson(), Map.class);
                if (damage.equals("PROOF_REFERENCE")) after.put("originalProofId", proof.getId() + 1);
                else ((Map<String, Object>) after.get("observed")).put("sourceSha256", "0".repeat(64));
                recovery.setAfterSnapshotJson(JsonUtils.toJsonString(after));
            }
            case "SOURCE_FOLDER" -> jdbc.update("UPDATE dcc_project_file_placement SET project_folder_id=37 WHERE controlled_file_id=21");
            case "DUPLICATE" -> {
                recovery.setId(null);
                recovery.setRequestId("another-recovery-key");
                assertEquals(1, typeChanges.insert(recovery));
            }
            default -> throw new AssertionError(damage);
        }
        if (!damage.equals("DUPLICATE") && !damage.equals("SOURCE_FOLDER"))
            assertEquals(1, typeChanges.updateById(recovery));
        var current = original(22L);
        assertThrows(IllegalArgumentException.class, () -> tx().execute(s -> baselines.preview(99L, targetRequest(22L))));
        assertEquals(current, original(22L));
        assertEquals(proof, typeChanges.selectById(proof.getId()));
    }
}
