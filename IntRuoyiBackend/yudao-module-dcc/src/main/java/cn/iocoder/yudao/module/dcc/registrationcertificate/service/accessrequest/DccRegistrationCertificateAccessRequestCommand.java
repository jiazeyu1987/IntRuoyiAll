package cn.iocoder.yudao.module.dcc.registrationcertificate.service.accessrequest;

import java.util.List;

public record DccRegistrationCertificateAccessRequestCommand(
        Long certificateId,
        String requestType,
        String purpose,
        Long projectCodeId,
        List<Long> businessFileIds) {
}
