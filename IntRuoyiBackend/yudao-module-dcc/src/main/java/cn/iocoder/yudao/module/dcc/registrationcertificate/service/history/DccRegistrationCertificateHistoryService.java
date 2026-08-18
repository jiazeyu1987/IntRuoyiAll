package cn.iocoder.yudao.module.dcc.registrationcertificate.service.history;

import java.util.List;

public interface DccRegistrationCertificateHistoryService {

    List<DccRegistrationCertificateHistoryItem> listHistory(Long tenantId, Long certificateId);
}
