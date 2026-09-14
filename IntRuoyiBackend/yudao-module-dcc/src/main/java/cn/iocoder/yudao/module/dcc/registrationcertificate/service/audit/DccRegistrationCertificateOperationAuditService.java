package cn.iocoder.yudao.module.dcc.registrationcertificate.service.audit;

import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DccRegistrationCertificateOperationAuditService {

    private static final String INITIAL_CERTIFICATE = "INITIAL_CERTIFICATE";
    private static final String RENEWAL_CERTIFICATE = "RENEWAL_CERTIFICATE";

    private final JdbcTemplate jdbcTemplate;
    private final AdminUserApi adminUserApi;

    public DccRegistrationCertificateOperationAuditService(
            JdbcTemplate jdbcTemplate, AdminUserApi adminUserApi) {
        if (jdbcTemplate == null || adminUserApi == null) {
            throw new IllegalArgumentException("注册证操作审计依赖不能为空");
        }
        this.jdbcTemplate = jdbcTemplate;
        this.adminUserApi = adminUserApi;
    }

    public DccRegistrationCertificateOperationAudit getInitialAudit(Long tenantId, Long certificateId) {
        List<OperationAuditRow> rows = selectRows(tenantId, certificateId, INITIAL_CERTIFICATE);
        if (rows.size() != 1) {
            throw new IllegalStateException("注册证首次上传版本记录必须唯一");
        }
        return resolve(rows).get(rows.get(0).versionId());
    }

    public Map<Long, DccRegistrationCertificateOperationAudit> getRenewalAudits(
            Long tenantId, Long certificateId) {
        List<OperationAuditRow> rows = selectRows(tenantId, certificateId, RENEWAL_CERTIFICATE);
        return resolve(rows);
    }

    private List<OperationAuditRow> selectRows(Long tenantId, Long certificateId, String versionType) {
        if (tenantId == null || tenantId <= 0 || certificateId == null || certificateId <= 0) {
            throw new IllegalArgumentException("租户 ID 和注册证 ID 必须为正数");
        }
        return jdbcTemplate.query("""
                SELECT v.id AS version_id,
                       r.requester_user_id AS operator_id,
                       r.requested_at AS operated_at,
                       v.formalized_by AS approver_id,
                       v.formalized_at AS approved_at
                  FROM dcc_registration_certificate_version v
                  LEFT JOIN dcc_registration_certificate_file f
                    ON f.tenant_id = v.tenant_id
                   AND f.owner_type = 'VERSION'
                   AND f.owner_id = v.id
                   AND f.file_kind = 'REGISTRATION_CERTIFICATE'
                   AND f.status IN ('BOUND', 'VOIDED')
                   AND f.deleted = 0
                   AND f.id = (
                         SELECT MIN(candidate.id)
                           FROM dcc_registration_certificate_file candidate
                          WHERE candidate.tenant_id = v.tenant_id
                            AND candidate.owner_type = 'VERSION'
                            AND candidate.owner_id = v.id
                            AND candidate.file_kind = 'REGISTRATION_CERTIFICATE'
                            AND candidate.status IN ('BOUND', 'VOIDED')
                            AND candidate.deleted = 0)
                  LEFT JOIN dcc_registration_certificate_access_request r
                    ON r.tenant_id = v.tenant_id
                   AND r.deleted = 0
                   AND r.id = (
                         SELECT MIN(candidate_request.id)
                           FROM dcc_registration_certificate_access_request candidate_request
                           JOIN dcc_registration_certificate_access_request_file candidate_request_file
                             ON candidate_request_file.tenant_id = candidate_request.tenant_id
                            AND candidate_request_file.request_id = candidate_request.id
                            AND candidate_request_file.business_file_id = f.id
                            AND candidate_request_file.file_kind = 'REGISTRATION_CERTIFICATE'
                            AND candidate_request_file.status = 'APPROVED'
                            AND candidate_request_file.deleted = 0
                          WHERE candidate_request.tenant_id = v.tenant_id
                            AND candidate_request.certificate_id = v.certificate_id
                            AND candidate_request.request_type = 'UPLOAD_CERTIFICATE'
                            AND candidate_request.status = 'APPROVED'
                            AND candidate_request.deleted = 0)
                 WHERE v.tenant_id = ?
                   AND v.certificate_id = ?
                   AND v.version_type = ?
                   AND v.status != 'DRAFT'
                   AND v.deleted = 0
                 ORDER BY v.version_no ASC, v.id ASC
                """, (rs, rowNum) -> new OperationAuditRow(
                rs.getLong("version_id"),
                rs.getObject("operator_id", Long.class),
                rs.getObject("operated_at", LocalDateTime.class),
                rs.getObject("approver_id", Long.class),
                rs.getObject("approved_at", LocalDateTime.class)), tenantId, certificateId, versionType);
    }

    private Map<Long, DccRegistrationCertificateOperationAudit> resolve(List<OperationAuditRow> rows) {
        if (rows.isEmpty()) {
            return Map.of();
        }
        Set<Long> userIds = new LinkedHashSet<>();
        for (OperationAuditRow row : rows) {
            addUserId(userIds, row.operatorId());
            addUserId(userIds, row.approverId());
        }
        Map<Long, String> userNames = resolveUserNames(userIds);
        Map<Long, DccRegistrationCertificateOperationAudit> result = new LinkedHashMap<>();
        for (OperationAuditRow row : rows) {
            DccRegistrationCertificateOperationAudit previous = result.put(row.versionId(),
                    new DccRegistrationCertificateOperationAudit(
                            row.operatorId(), userName(userNames, row.operatorId()), row.operatedAt(),
                            row.approverId(), userName(userNames, row.approverId()), row.approvedAt()));
            if (previous != null) {
                throw new IllegalStateException("注册证版本操作审计记录不唯一");
            }
        }
        return Map.copyOf(result);
    }

    private Map<Long, String> resolveUserNames(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        List<AdminUserRespDTO> users = adminUserApi.getUserList(userIds);
        if (users == null) {
            throw new IllegalStateException("注册证操作人员查询失败");
        }
        Map<Long, AdminUserRespDTO> usersById = users.stream()
                .filter(user -> user != null && user.getId() != null)
                .collect(Collectors.toMap(AdminUserRespDTO::getId, Function.identity(),
                        (left, right) -> left, LinkedHashMap::new));
        Map<Long, String> names = new LinkedHashMap<>();
        for (Long userId : userIds) {
            AdminUserRespDTO user = usersById.get(userId);
            if (user == null || user.getNickname() == null || user.getNickname().isBlank()) {
                throw new IllegalStateException("注册证操作人员正式姓名缺失");
            }
            names.put(userId, user.getNickname().trim());
        }
        return Map.copyOf(names);
    }

    private static void addUserId(Set<Long> userIds, Long userId) {
        if (userId == null) {
            return;
        }
        if (userId <= 0) {
            throw new IllegalStateException("注册证操作人员 ID 不合法");
        }
        userIds.add(userId);
    }

    private static String userName(Map<Long, String> userNames, Long userId) {
        return userId == null ? null : userNames.get(userId);
    }

    private record OperationAuditRow(
            Long versionId,
            Long operatorId,
            LocalDateTime operatedAt,
            Long approverId,
            LocalDateTime approvedAt) {
    }
}
