package cn.iocoder.yudao.module.signature.api;

import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureEvidenceDTO;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureVerificationDTO;

import java.util.List;

public interface ElectronicSignatureQueryService {

    ElectronicSignatureEvidenceDTO getById(Long signatureId);

    List<ElectronicSignatureEvidenceDTO> listBySubject(String moduleCode, String subjectType, String subjectId);

    ElectronicSignatureVerificationDTO verifyEvidence(Long signatureId);

}
