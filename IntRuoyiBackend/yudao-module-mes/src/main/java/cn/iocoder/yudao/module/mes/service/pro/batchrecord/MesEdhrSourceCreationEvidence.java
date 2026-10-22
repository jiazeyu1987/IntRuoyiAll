package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

/** Immutable historical provenance only; never a current eligibility or submission credential. */
public record MesEdhrSourceCreationEvidence(String ownerProtocol, Long sourceRowId, String sourceReceiptId,
        String sourceVersion, String sourceHash, Long creationEventId, String creationEventHash) {}
