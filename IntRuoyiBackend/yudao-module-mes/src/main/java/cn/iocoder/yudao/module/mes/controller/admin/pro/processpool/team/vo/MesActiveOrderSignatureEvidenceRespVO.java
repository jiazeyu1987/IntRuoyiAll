package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesActiveOrderSignatureEvidenceService;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureEvidenceDTO;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureVerificationDTO;

/** Stored evidence and a fresh verification result are intentionally separate. */
public record MesActiveOrderSignatureEvidenceRespVO(Long activeOrderId, String signerName,
                                                   ElectronicSignatureEvidenceDTO evidence,
                                                   ElectronicSignatureVerificationDTO verification) {
    public static MesActiveOrderSignatureEvidenceRespVO from(MesActiveOrderSignatureEvidenceService.Result result) {
        return new MesActiveOrderSignatureEvidenceRespVO(result.activeOrderId(), result.signerName(),
                result.evidence(), result.verification());
    }
}
