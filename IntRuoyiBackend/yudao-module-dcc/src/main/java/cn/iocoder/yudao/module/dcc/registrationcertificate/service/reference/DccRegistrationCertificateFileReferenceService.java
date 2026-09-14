package cn.iocoder.yudao.module.dcc.registrationcertificate.service.reference;

import java.util.Optional;

public interface DccRegistrationCertificateFileReferenceService {

    Optional<DccRegistrationCertificateFileReference> resolveByInfraFileId(Long infraFileId);

    Optional<DccRegistrationCertificateFileReference> resolveByBusinessFileId(Long tenantId, Long businessFileId);

    DccRegistrationCertificateFileReference requireBoundByBusinessFileId(Long tenantId, Long businessFileId);

    DccRegistrationCertificateFileReference requireCurrentByBusinessFileId(Long tenantId, Long businessFileId);

    DccRegistrationCertificateFileReference requireCurrentByReference(Long tenantId, Long businessFileId,
                                                                      Long expectedInfraFileId);

    DccRegistrationCertificateFileReference requireBoundByReference(Long tenantId, Long businessFileId,
                                                                    Long expectedInfraFileId);
}
