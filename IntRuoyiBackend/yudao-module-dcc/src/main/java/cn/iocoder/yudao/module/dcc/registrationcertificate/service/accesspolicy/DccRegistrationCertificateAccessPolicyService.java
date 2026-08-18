package cn.iocoder.yudao.module.dcc.registrationcertificate.service.accesspolicy;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateFileDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateGrantDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateVersionDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateFileMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateGrantMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateVersionMapper;
import cn.iocoder.yudao.module.mdm.api.companyscope.MdmCompanyScopeApi;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_ACCESS_GRANT_EXPIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_ACCESS_GRANT_REVOKED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID;

@Service
public class DccRegistrationCertificateAccessPolicyService {

    private static final String GRANT_TYPE_VIEW_OLD_CERTIFICATE = "VIEW_OLD_CERTIFICATE";
    private static final String GRANT_TYPE_DOWNLOAD = "DOWNLOAD";
    private static final String GRANT_STATUS_ACTIVE = "ACTIVE";
    private static final String GRANT_STATUS_REVOKED = "REVOKED";
    private static final String FILE_OWNER_TYPE_VERSION = "VERSION";
    private static final String FILE_KIND_REGISTRATION_CERTIFICATE = "REGISTRATION_CERTIFICATE";
    private static final String FILE_STATUS_BOUND = "BOUND";

    private final DccRegistrationCertificateMapper certificateMapper;
    private final DccRegistrationCertificateVersionMapper versionMapper;
    private final DccRegistrationCertificateFileMapper fileMapper;
    private final DccRegistrationCertificateGrantMapper grantMapper;
    private final MdmCompanyScopeApi companyScopeApi;

    public DccRegistrationCertificateAccessPolicyService(
            DccRegistrationCertificateMapper certificateMapper,
            DccRegistrationCertificateVersionMapper versionMapper,
            DccRegistrationCertificateFileMapper fileMapper,
            DccRegistrationCertificateGrantMapper grantMapper,
            MdmCompanyScopeApi companyScopeApi) {
        this.certificateMapper = require(certificateMapper, "certificateMapper");
        this.versionMapper = require(versionMapper, "versionMapper");
        this.fileMapper = require(fileMapper, "fileMapper");
        this.grantMapper = require(grantMapper, "grantMapper");
        this.companyScopeApi = require(companyScopeApi, "companyScopeApi");
    }

    public void assertCurrentPreviewAllowed(Long tenantId, Long actorId, Long certificateId) {
        DccRegistrationCertificateDO certificate = requireLiveCertificate(tenantId, certificateId);
        if (!"ACTIVE".equals(certificate.getStatus()) || certificate.getCurrentVersionId() == null) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID);
        }
        DccRegistrationCertificateVersionDO currentVersion = versionMapper.selectById(certificate.getCurrentVersionId());
        if (currentVersion == null || !Objects.equals(currentVersion.getTenantId(), tenantId)
                || !Objects.equals(currentVersion.getCertificateId(), certificate.getId())
                || !"CURRENT".equals(currentVersion.getStatus())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID);
        }
        assertCompanyScope(actorId, certificate.getOwnerCompanyId());
    }

    public void assertOldViewAllowed(Long tenantId, Long actorId, Long certificateId, LocalDateTime at) {
        List<DccRegistrationCertificateGrantDO> grants = grantMapper.selectByCertificate(
                tenantId, actorId, certificateId, GRANT_TYPE_VIEW_OLD_CERTIFICATE);
        DccRegistrationCertificateGrantDO grant = selectValidGrant(grants, at);
        DccRegistrationCertificateDO certificate = requireLiveCertificate(tenantId, grant.getCertificateId());
        assertCompanyScope(actorId, certificate.getOwnerCompanyId());
    }

    public void assertDownloadAllowed(Long tenantId, Long actorId, Long businessFileId, LocalDateTime at) {
        requireDownloadGrant(tenantId, actorId, businessFileId, at);
    }

    public DccRegistrationCertificateGrantDO requireDownloadGrant(Long tenantId, Long actorId, Long businessFileId,
                                                                  LocalDateTime at) {
        List<DccRegistrationCertificateGrantDO> grants = grantMapper.selectByBusinessFile(
                tenantId, actorId, businessFileId, GRANT_TYPE_DOWNLOAD);
        DccRegistrationCertificateGrantDO grant = selectValidGrant(grants, at);
        DccRegistrationCertificateFileDO file = fileMapper.selectById(businessFileId);
        if (file == null || !Objects.equals(file.getTenantId(), tenantId)
                || !FILE_OWNER_TYPE_VERSION.equals(file.getOwnerType())
                || !FILE_KIND_REGISTRATION_CERTIFICATE.equals(file.getFileKind())
                || !FILE_STATUS_BOUND.equals(file.getStatus())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID);
        }
        DccRegistrationCertificateVersionDO version = versionMapper.selectById(file.getOwnerId());
        if (version == null || !Objects.equals(version.getTenantId(), tenantId)
                || !Objects.equals(version.getCertificateId(), grant.getCertificateId())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID);
        }
        DccRegistrationCertificateDO certificate = requireLiveCertificate(tenantId, grant.getCertificateId());
        assertCompanyScope(actorId, certificate.getOwnerCompanyId());
        return grant;
    }

    private DccRegistrationCertificateGrantDO selectValidGrant(
            List<DccRegistrationCertificateGrantDO> grants, LocalDateTime at) {
        ServiceException lastFailure = new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID);
        for (DccRegistrationCertificateGrantDO grant : grants) {
            if (GRANT_STATUS_REVOKED.equals(grant.getStatus())) {
                lastFailure = new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_GRANT_REVOKED);
                continue;
            }
            if (!GRANT_STATUS_ACTIVE.equals(grant.getStatus())) {
                continue;
            }
            if (grant.getExpiresAt() == null || !grant.getExpiresAt().isAfter(at)) {
                lastFailure = new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_GRANT_EXPIRED);
                continue;
            }
            return grant;
        }
        throw lastFailure;
    }

    private DccRegistrationCertificateDO requireLiveCertificate(Long tenantId, Long certificateId) {
        DccRegistrationCertificateDO certificate = certificateMapper.selectById(certificateId);
        if (certificate == null || !Objects.equals(certificate.getTenantId(), tenantId)
                || "VOIDED".equals(certificate.getStatus()) || "DRAFT".equals(certificate.getStatus())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID);
        }
        return certificate;
    }

    private void assertCompanyScope(Long actorId, Long ownerCompanyId) {
        if (ownerCompanyId == null) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID);
        }
        try {
            companyScopeApi.validateUserCompanyAccess(actorId, ownerCompanyId);
        } catch (ServiceException ex) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID);
        }
    }

    private static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
