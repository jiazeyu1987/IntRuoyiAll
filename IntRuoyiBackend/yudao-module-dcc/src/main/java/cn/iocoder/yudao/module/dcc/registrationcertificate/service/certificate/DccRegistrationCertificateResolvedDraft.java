package cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate;

import cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateProductionRelation;

public record DccRegistrationCertificateResolvedDraft(
        String productName,
        DccRegistrationCertificateProductionRelation productionRelation) {
}
