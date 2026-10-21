package cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager;

import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesEdhrSourceCreationEvidence;

/** Verified completion and PQC facts. This is not a four-material readiness receipt. */
public record MesEdhrVerifiedActiveOrderReleaseFact(Long applicationId, Long batchExecutionId,
        String formalFactsHash, String sourceSnapshotHash, MesEdhrSourceCreationEvidence completionCreation,
        Long pqcSignatureId, String pqcEvidenceHash, Long pqcCreationEventId) {}
