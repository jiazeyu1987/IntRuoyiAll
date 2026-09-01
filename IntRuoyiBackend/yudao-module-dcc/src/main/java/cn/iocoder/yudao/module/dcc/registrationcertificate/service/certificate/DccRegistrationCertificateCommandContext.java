package cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate;

public final class DccRegistrationCertificateCommandContext {

    private final Long requestedOwnerCompanyId;
    private final Long requestedCertificateId;
    private Long ownerCompanyId;
    private Long certificateId;

    public DccRegistrationCertificateCommandContext(Long requestedOwnerCompanyId, Long requestedCertificateId) {
        this.requestedOwnerCompanyId = requestedOwnerCompanyId;
        this.requestedCertificateId = requestedCertificateId;
    }

    public void resolveTrustedIdentity(Long ownerCompanyId, Long certificateId) {
        if (ownerCompanyId == null || ownerCompanyId <= 0 || certificateId == null || certificateId <= 0) {
            throw new IllegalArgumentException("可信注册证业务身份必须为正数");
        }
        this.ownerCompanyId = ownerCompanyId;
        this.certificateId = certificateId;
    }

    public Long requestedOwnerCompanyId() {
        return requestedOwnerCompanyId;
    }

    public Long requestedCertificateId() {
        return requestedCertificateId;
    }

    public Long ownerCompanyId() {
        return ownerCompanyId;
    }

    public Long certificateId() {
        return certificateId;
    }
}
