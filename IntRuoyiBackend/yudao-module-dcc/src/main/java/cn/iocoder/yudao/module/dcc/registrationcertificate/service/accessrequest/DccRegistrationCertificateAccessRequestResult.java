package cn.iocoder.yudao.module.dcc.registrationcertificate.service.accessrequest;

import java.util.List;

public record DccRegistrationCertificateAccessRequestResult(
        Long requestId,
        Long certificateId,
        Long ownerCompanyId,
        String status,
        List<Long> businessFileIds) {
}
