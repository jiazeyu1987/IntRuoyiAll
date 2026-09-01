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
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_ACCESS_GRANT_EXPIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_ACCESS_GRANT_REVOKED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID;
import static cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalContract.APPROVER_ROLE_CODE;

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
    private final PermissionApi permissionApi;
    private final JdbcTemplate jdbcTemplate;

    public DccRegistrationCertificateAccessPolicyService(
            DccRegistrationCertificateMapper certificateMapper,
            DccRegistrationCertificateVersionMapper versionMapper,
            DccRegistrationCertificateFileMapper fileMapper,
            DccRegistrationCertificateGrantMapper grantMapper,
            MdmCompanyScopeApi companyScopeApi,
            PermissionApi permissionApi,
            JdbcTemplate jdbcTemplate) {
        this.certificateMapper = require(certificateMapper, "certificateMapper");
        this.versionMapper = require(versionMapper, "versionMapper");
        this.fileMapper = require(fileMapper, "fileMapper");
        this.grantMapper = require(grantMapper, "grantMapper");
        this.companyScopeApi = require(companyScopeApi, "companyScopeApi");
        this.permissionApi = require(permissionApi, "permissionApi");
        this.jdbcTemplate = require(jdbcTemplate, "jdbcTemplate");
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

    public void assertFilePreviewAllowed(Long tenantId, Long actorId, Long certificateId, Long versionId,
                                         LocalDateTime at) {
        if (versionId == null || at == null) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID);
        }
        DccRegistrationCertificateDO certificate = requireLiveCertificate(tenantId, certificateId);
        DccRegistrationCertificateVersionDO version = versionMapper.selectById(versionId);
        if (version == null || !Objects.equals(version.getTenantId(), tenantId)
                || !Objects.equals(version.getCertificateId(), certificate.getId())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID);
        }
        if ("CURRENT".equals(version.getStatus())) {
            if (!"ACTIVE".equals(certificate.getStatus())
                    || !Objects.equals(certificate.getCurrentVersionId(), version.getId())) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID);
            }
            assertCompanyScope(actorId, certificate.getOwnerCompanyId());
            return;
        }
        if ("PENDING_EFFECTIVE".equals(version.getStatus())) {
            if (!("ACTIVE".equals(certificate.getStatus())
                    || "PENDING_FIRST_EFFECTIVE".equals(certificate.getStatus()))
                    || !Objects.equals(certificate.getPendingVersionId(), version.getId())) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID);
            }
            assertCompanyScope(actorId, certificate.getOwnerCompanyId());
            return;
        }
        if ("OLD".equals(version.getStatus())) {
            assertOldViewAllowed(tenantId, actorId, certificateId, at);
            return;
        }
        throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID);
    }

    public void assertOldViewAllowed(Long tenantId, Long actorId, Long certificateId, LocalDateTime at) {
        if (hasRegistrationManagerRole(actorId)) {
            DccRegistrationCertificateDO certificate = requireLiveCertificate(tenantId, certificateId);
            assertCompanyScope(actorId, certificate.getOwnerCompanyId());
            return;
        }
        List<DccRegistrationCertificateGrantDO> grants = grantMapper.selectByCertificate(
                tenantId, actorId, certificateId, GRANT_TYPE_VIEW_OLD_CERTIFICATE);
        DccRegistrationCertificateGrantDO grant = selectValidGrant(grants, at);
        DccRegistrationCertificateDO certificate = requireLiveCertificate(tenantId, grant.getCertificateId());
        assertCompanyScope(actorId, certificate.getOwnerCompanyId());
    }

    public void assertDownloadAllowed(Long tenantId, Long actorId, Long businessFileId, LocalDateTime at) {
        requireDownloadGrant(tenantId, actorId, businessFileId, at);
    }

    public boolean authorizeRegistrationManagerDownloadIfRole(Long tenantId, Long actorId, Long certificateId) {
        if (!hasRegistrationManagerRole(actorId)) {
            return false;
        }
        DccRegistrationCertificateDO certificate = requireLiveCertificate(tenantId, certificateId);
        assertCompanyScope(actorId, certificate.getOwnerCompanyId());
        return true;
    }

    public DccRegistrationCertificateGrantDO requireDownloadGrant(Long tenantId, Long actorId, Long businessFileId,
                                                                  LocalDateTime at) {
        List<DccRegistrationCertificateGrantDO> grants = grantMapper.selectByBusinessFile(
                tenantId, actorId, businessFileId, GRANT_TYPE_DOWNLOAD);
        DccRegistrationCertificateGrantDO grant = selectValidGrant(grants, at);
        DccRegistrationCertificateFileDO file = fileMapper.selectById(businessFileId);
        if (file == null || !Objects.equals(file.getTenantId(), tenantId)
                || !FILE_STATUS_BOUND.equals(file.getStatus())
                || !isDownloadableFileKind(file)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID);
        }
        DccRegistrationCertificateVersionDO version = versionMapper.selectById(resolveVersionId(tenantId, file));
        if (version == null || !Objects.equals(version.getTenantId(), tenantId)
                || !Objects.equals(version.getCertificateId(), grant.getCertificateId())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID);
        }
        DccRegistrationCertificateDO certificate = requireLiveCertificate(tenantId, grant.getCertificateId());
        assertCompanyScope(actorId, certificate.getOwnerCompanyId());
        return grant;
    }

    private boolean isDownloadableFileKind(DccRegistrationCertificateFileDO file) {
        return (FILE_OWNER_TYPE_VERSION.equals(file.getOwnerType())
                && FILE_KIND_REGISTRATION_CERTIFICATE.equals(file.getFileKind()))
                || ("CHANGE".equals(file.getOwnerType())
                && "CHANGE_APPROVAL".equals(file.getFileKind()));
    }

    private boolean hasRegistrationManagerRole(Long actorId) {
        return actorId != null && permissionApi.hasAnyRolesOrSuperAdmin(actorId, APPROVER_ROLE_CODE);
    }

    private Long resolveVersionId(Long tenantId, DccRegistrationCertificateFileDO file) {
        if (FILE_OWNER_TYPE_VERSION.equals(file.getOwnerType())) {
            return file.getOwnerId();
        }
        return jdbcTemplate.query("""
                SELECT source_version_id FROM dcc_registration_certificate_change
                 WHERE tenant_id = ? AND id = ? AND status = 'APPLIED' AND deleted = 0
                """, rs -> rs.next() ? rs.getLong(1) : null, tenantId, file.getOwnerId());
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
            throw new IllegalArgumentException(name + "不能为空");
        }
        return value;
    }
}
