package cn.iocoder.yudao.module.bpm.approval.service.ownership;

import java.util.Set;

/** Server-read frozen domain facts for one explicit, still writable signature task. */
public record EdhrDomainAssignmentProof(Long requestId, Long executionId, Integer executionRevision,
        Long snapshotId, Long workTaskId, String phase, Long actorId, Set<Long> eligibleActorIds,
        EngineApprovalIdentity engineIdentity) {
    public EdhrDomainAssignmentProof {
        eligibleActorIds = Set.copyOf(eligibleActorIds);
    }

    /** Stable decimal ids, ascending, UTF-8, explicit contract version. */
    public String candidateDigest() {
        String canonical = "EDHR_FROZEN_ACTORS_V1:" + eligibleActorIds.stream().sorted()
                .map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("EDHR_CANDIDATE_DIGEST_ALGORITHM_REQUIRED", ex);
        }
    }
}
