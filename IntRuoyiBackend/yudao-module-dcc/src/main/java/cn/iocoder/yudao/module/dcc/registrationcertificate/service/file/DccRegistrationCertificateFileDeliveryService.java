package cn.iocoder.yudao.module.dcc.registrationcertificate.service.file;

import cn.iocoder.yudao.module.dcc.service.file.DccRequestAuditContext;

public interface DccRegistrationCertificateFileDeliveryService {

    DccRegistrationCertificateFileDownloadResult download(Long tenantId, Long userId, Long businessFileId,
                                                          String attemptKey,
                                                          DccRequestAuditContext auditContext);
}
