package cn.iocoder.yudao.module.dcc.signature.service.records;

import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureEvidenceDTO;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureVerificationDTO;

/** Original evidence and diagnostic verification; does not assert complete business traceability. */
public record SignatureGovernanceRecordEvidence(String globalId,
        ElectronicSignatureEvidenceDTO evidence, ElectronicSignatureVerificationDTO verification) {}
