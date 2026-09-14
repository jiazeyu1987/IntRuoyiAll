package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceOwnershipDO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DccControlledFileSourceGovernanceClassifierTest {

    private static final String HASH = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    private final DccControlledFileSourceGovernanceClassifier classifier =
            new DccControlledFileSourceGovernanceClassifier();

    @Test
    void classify_missingSourceReturnsStableBlocker() {
        DccControlledFileSourceGovernanceDecision decision = classifier.classify(
                31L, 901L, null, true, false, true, true, null, "sha", null, Set.of(31L));

        assertEquals("BLOCKED", decision.status());
        assertEquals("SOURCE_REFERENCE_MISSING", decision.reasonCode());
    }

    @Test
    void classify_globalReferenceOutsideScopeBlocksWholeGroup() {
        DccControlledFileSourceGovernanceDecision decision = classifier.classify(
                31L, 901L, 700L, true, false, true, true, null, HASH,
                List.of(new DccControlledFileSourceGovernanceClassifier.GlobalReference(32L, 700L, 902L)),
                Set.of(31L));

        assertEquals("BLOCKED", decision.status());
        assertEquals("SOURCE_GLOBAL_REFERENCE_OUT_OF_SCOPE", decision.reasonCode());
    }

    @Test
    void classify_validSharedSourceIsReadyForCopy() {
        DccControlledFileSourceGovernanceDecision decision = classifier.classify(
                31L, 901L, 700L, true, false, true, true, null, HASH,
                List.of(new DccControlledFileSourceGovernanceClassifier.GlobalReference(31L, 700L, 901L),
                        new DccControlledFileSourceGovernanceClassifier.GlobalReference(31L, 700L, 902L)),
                Set.of(31L));

        assertEquals("READY", decision.status(), decision.reasonCode() + ":" + decision.detail());
        assertEquals("COPY_SHARED_SOURCE", decision.action());
    }

    @Test
    void classify_invalidOwnershipBlocksWithoutGuessing() {
        DccControlledFileSourceGovernanceDecision decision = classifier.classify(
                31L, 901L, 700L, true, false, true, true,
                DccControlledFileSourceOwnershipDO.builder().tenantId(31L).controlledFileId(999L)
                        .sourceFileId(700L).sourceSha256(HASH).build(),
                HASH, List.of(new DccControlledFileSourceGovernanceClassifier.GlobalReference(31L, 700L, 901L)),
                Set.of(31L));

        assertEquals("BLOCKED", decision.status());
        assertEquals("OWNERSHIP_POINTER_MISMATCH", decision.reasonCode());
    }

    @Test
    void classify_currentControlledFileMissingFromGlobalReferencesBlocks() {
        DccControlledFileSourceGovernanceDecision decision = classifier.classify(
                31L, 901L, 700L, true, false, true, true, null, HASH,
                List.of(new DccControlledFileSourceGovernanceClassifier.GlobalReference(31L, 700L, 902L)),
                Set.of(31L));

        assertEquals("BLOCKED", decision.status());
        assertEquals("SOURCE_REFERENCE_NOT_IN_GLOBAL_INDEX", decision.reasonCode());
    }

    @Test
    void classify_missingOrMalformedHashBlocks() {
        DccControlledFileSourceGovernanceDecision decision = classifier.classify(
                31L, 901L, 700L, true, false, true, true, null, "not-a-sha",
                List.of(new DccControlledFileSourceGovernanceClassifier.GlobalReference(31L, 700L, 901L)),
                Set.of(31L));

        assertEquals("BLOCKED", decision.status());
        assertEquals("SOURCE_HASH_MISMATCH", decision.reasonCode());
    }
}
