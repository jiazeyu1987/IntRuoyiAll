package cn.iocoder.yudao.module.dcc.registrationcertificate.service.accesspolicy;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateFileDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateGrantDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateVersionDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateFileMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateDownloadConsumptionMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateGrantMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateVersionMapper;
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
    private final DccRegistrationCertificateDownloadConsumptionMapper consumptionMapper;
    private final PermissionApi permissionApi;
    private final JdbcTemplate jdbcTemplate;

    public DccRegistrationCertificateAccessPolicyService(
            DccRegistrationCertificateMapper certificateMapper,
            DccRegistrationCertificateVersionMapper versionMapper,
            DccRegistrationCertificateFileMapper fileMapper,
            DccRegistrationCertificateGrantMapper grantMapper,
            PermissionApi permissionApi,
            DccRegistrationCertificateDownloadConsumptionMapper consumptionMapper,
            JdbcTemplate jdbcTemplate) {
        this.certificateMapper = require(certificateMapper, "certificateMapper");
        this.versionMapper = require(versionMapper, "versionMapper");
        this.fileMapper = require(fileMapper, "fileMapper");
        this.grantMapper = require(grantMapper, "grantMapper");
        this.permissionApi = require(permissionApi, "permissionApi");
        this.consumptionMapper = require(consumptionMapper, "consumptionMapper");
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
        try {
            requireDownloadGrant(tenantId, actorId, businessFileId, at);
        } catch (ServiceException ex) {
            if (isMissingDownloadGrant(ex)) throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID);
            throw ex;
        }
    }

    public boolean canDownloadFile(Long tenantId, Long actorId, Long businessFileId, LocalDateTime at) {
        if (businessFileId == null || at == null) {
            return false;
        }
        DccRegistrationCertificateFileDO file = fileMapper.selectById(businessFileId);
        if (file == null || !Objects.equals(file.getTenantId(), tenantId)
                || !FILE_STATUS_BOUND.equals(file.getStatus())
                || !isDownloadableFileKind(file)) {
            return false;
        }
        DccRegistrationCertificateVersionDO version = versionMapper.selectById(resolveVersionId(tenantId, file));
        if (version == null || !Objects.equals(version.getTenantId(), tenantId)) {
            return false;
        }
        if (isExpired(tenantId, version, at) && !isRegistrationDepartment(tenantId, actorId)) return false;
        try {
            DccRegistrationCertificateGrantDO grant = requireDownloadGrant(tenantId, actorId, businessFileId, at);
            return consumptionMapper.countSuccess(tenantId, grant.getId(), businessFileId) == 0;
        } catch (ServiceException ex) {
            if (isMissingDownloadGrant(ex)) {
                return false;
            }
            throw ex;
        }
    }

    public Long findPendingDownloadRequestId(Long tenantId, Long actorId, Long businessFileId) {
        if (tenantId == null || actorId == null || businessFileId == null || businessFileId <= 0) {
            return null;
        }
        return jdbcTemplate.query("""
                SELECT r.id
                  FROM dcc_registration_certificate_access_request r
                  JOIN dcc_registration_certificate_access_request_file rf
                    ON rf.tenant_id = r.tenant_id
                   AND rf.request_id = r.id
                   AND rf.deleted = 0
                 WHERE r.tenant_id = ?
                   AND r.requester_user_id = ?
                   AND r.request_type = 'DOWNLOAD_FILE'
                   AND r.status IN ('SUBMITTED', 'BPM_BOUND')
                   AND r.deleted = 0
                   AND rf.business_file_id = ?
                   AND rf.download_requested = 1
                 ORDER BY r.requested_at DESC, r.id DESC
                 LIMIT 1
                """, rs -> rs.next() ? rs.getLong(1) : null, tenantId, actorId, businessFileId);
    }

    public DccRegistrationCertificateGrantDO requireDownloadGrant(Long tenantId, Long actorId, Long businessFileId,
                                                                  LocalDateTime at) {
        List<DccRegistrationCertificateGrantDO> grants = grantMapper.selectByBusinessFile(
                tenantId, actorId, businessFileId, GRANT_TYPE_DOWNLOAD);
        DccRegistrationCertificateGrantDO grant = selectValidGrant(grants, at);
        // Prefer an unused valid grant. Delivery checks consumption under its lock;
        // retain the valid grant identity when exhausted for the rejection audit.
        for (DccRegistrationCertificateGrantDO candidate : grants) {
            if (GRANT_STATUS_ACTIVE.equals(candidate.getStatus()) && candidate.getExpiresAt() != null
                    && candidate.getExpiresAt().isAfter(at)
                    && consumptionMapper.countSuccess(tenantId, candidate.getId(), businessFileId) == 0) {
                grant = candidate;
                break;
            }
        }
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
        if (isExpired(tenantId, version, at) && !isRegistrationDepartment(tenantId, actorId))
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID);
        return grant;
    }

    private boolean isDownloadableFileKind(DccRegistrationCertificateFileDO file) {
        return (FILE_OWNER_TYPE_VERSION.equals(file.getOwnerType())
                && java.util.Set.of(FILE_KIND_REGISTRATION_CERTIFICATE, "CERTIFICATE_ATTACHMENT", "FDA_PRODUCT_DESCRIPTION").contains(file.getFileKind()))
                || ("CHANGE".equals(file.getOwnerType())
                && java.util.Set.of("CHANGE_APPROVAL","CERTIFICATE_ATTACHMENT","FDA_PRODUCT_DESCRIPTION").contains(file.getFileKind()))
                || ("SUPPORTING_DOCUMENT".equals(file.getOwnerType()) && java.util.Set.of("RENEWAL_ACCEPTANCE_RECEIPT", "RENEWAL_SUPPLEMENT_NOTICE", "CE_RENEWAL_CONTRACT", "CE_RENEWAL_QUOTATION").contains(file.getFileKind()));
    }

    private boolean isRegistrationDepartment(Long tenantId, Long actorId) {
        List<Long> departments = jdbcTemplate.queryForList("""
                SELECT dept_id FROM system_users WHERE tenant_id=? AND id=? AND deleted=0
                """, Long.class, tenantId, actorId);
        if (departments.size() != 1) return false;
        Long departmentId = departments.get(0);
        java.util.Set<Long> visited = new java.util.HashSet<>();
        // 国内/国际注册等子部门继承注册部身份，仍须通过逐文件授权校验。
        while (departmentId != null && departmentId > 0) {
            if (!visited.add(departmentId)) {
                throw new IllegalStateException("注册证下载权限检查发现部门层级循环");
            }
            List<DepartmentMembership> rows = jdbcTemplate.query("""
                    SELECT name,parent_id FROM system_dept WHERE tenant_id=? AND id=? AND deleted=0
                    """, (rs, rowNum) -> new DepartmentMembership(rs.getString("name"),
                            rs.getObject("parent_id", Long.class)), tenantId, departmentId);
            if (rows.size() != 1) return false;
            DepartmentMembership department = rows.get(0);
            if ("注册部".equals(department.name())) return true;
            departmentId = department.parentId();
        }
        return false;
    }

    private record DepartmentMembership(String name, Long parentId) { }

    private boolean isExpired(Long tenantId, DccRegistrationCertificateVersionDO version, LocalDateTime at) {
        List<String> jsons = jdbcTemplate.queryForList("""
                SELECT facts_json FROM dcc_registration_certificate_snapshot
                 WHERE tenant_id=? AND version_id=? AND deleted=0 ORDER BY revision_no DESC,id DESC LIMIT 1
                """, String.class, tenantId, version.getId());
        if (jsons.size()!=1) throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID);
        return cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateFacts.parse(jsons.get(0)).isExpired(at.toLocalDate());
    }

    private boolean hasRegistrationManagerRole(Long actorId) {
        return actorId != null && permissionApi.hasAnyRolesOrSuperAdmin(actorId, APPROVER_ROLE_CODE);
    }

    private Long resolveVersionId(Long tenantId, DccRegistrationCertificateFileDO file) {
        if (FILE_OWNER_TYPE_VERSION.equals(file.getOwnerType())) {
            return file.getOwnerId();
        }
        if ("SUPPORTING_DOCUMENT".equals(file.getOwnerType())) {
            Long versionId = jdbcTemplate.query("""
                    SELECT sd.version_id FROM dcc_registration_certificate_supporting_document sd
                    JOIN dcc_registration_certificate_version v ON v.id=sd.version_id AND v.tenant_id=sd.tenant_id AND v.certificate_id=sd.certificate_id AND v.deleted=0
                    WHERE sd.tenant_id=? AND sd.id=? AND sd.business_file_id=? AND sd.document_type=? AND sd.status='EFFECTIVE' AND sd.deleted=0
                    """, rs -> rs.next() ? rs.getLong(1) : null, tenantId, file.getOwnerId(), file.getId(), file.getFileKind());
            if (versionId == null) throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID);
            return versionId;
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

    private boolean isMissingDownloadGrant(ServiceException ex) {
        Integer code = ex.getCode();
        return Objects.equals(code, REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID.getCode())
                || Objects.equals(code, REGISTRATION_CERTIFICATE_ACCESS_GRANT_EXPIRED.getCode())
                || Objects.equals(code, REGISTRATION_CERTIFICATE_ACCESS_GRANT_REVOKED.getCode());
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
    }

    private static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + "不能为空");
        }
        return value;
    }
}
