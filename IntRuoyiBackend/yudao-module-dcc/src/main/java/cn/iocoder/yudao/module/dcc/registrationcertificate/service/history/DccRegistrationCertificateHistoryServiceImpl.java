package cn.iocoder.yudao.module.dcc.registrationcertificate.service.history;

import cn.iocoder.yudao.module.dcc.registrationcertificate.service.audit.DccRegistrationCertificateOperationAudit;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.audit.DccRegistrationCertificateOperationAuditService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DccRegistrationCertificateHistoryServiceImpl implements DccRegistrationCertificateHistoryService {

    private final JdbcTemplate jdbcTemplate;
    private final DccRegistrationCertificateOperationAuditService operationAuditService;
    private final AdminUserApi adminUserApi;

    public DccRegistrationCertificateHistoryServiceImpl(
            JdbcTemplate jdbcTemplate,
            DccRegistrationCertificateOperationAuditService operationAuditService,
            AdminUserApi adminUserApi) {
        if (jdbcTemplate == null || operationAuditService == null || adminUserApi == null) {
            throw new IllegalArgumentException("注册证历史查询依赖不能为空");
        }
        this.jdbcTemplate = jdbcTemplate;
        this.operationAuditService = operationAuditService;
        this.adminUserApi = adminUserApi;
    }

    @Override
    public List<DccRegistrationCertificateHistoryItem> listHistory(Long tenantId, Long certificateId) {
        if (tenantId == null || tenantId <= 0 || certificateId == null || certificateId <= 0) {
            throw new IllegalArgumentException("租户 ID 和注册证 ID 必须为正数");
        }
        List<DccRegistrationCertificateHistoryItem> items = jdbcTemplate.query("""
                SELECT e.id AS event_id,
                       e.event_type,
                       COALESCE(i.item_type, e.event_type) AS item_type,
                       COALESCE(i.before_value_json,
                                CASE WHEN e.event_type = 'CERTIFICATE_VOIDED'
                                     THEN '{\"value\":\"ACTIVE\"}' ELSE NULL END) AS before_value_json,
                       COALESCE(i.after_value_json, e.detail_json) AS after_value_json,
                       e.actor_id,
                       c.id AS change_id,
                       c.approval_request_id,
                       c.status AS change_status,
                       c.actor_id AS submitted_by,
                       c.reviewer_user_id AS reviewed_by,
                       c.reviewed_at,
                       CASE WHEN e.event_type = 'RENEWAL_UPLOADED'
                            THEN renewal_file.id
                            ELSE (SELECT MIN(f.id)
                                    FROM dcc_registration_certificate_file f
                                   WHERE f.tenant_id = e.tenant_id
                                     AND f.owner_type = 'CHANGE'
                                     AND f.owner_id = c.id
                                     AND f.file_kind = 'CHANGE_APPROVAL'
                                     AND f.status = 'BOUND'
                                     AND f.deleted = 0)
                       END AS business_file_id,
                       CASE WHEN e.event_type = 'RENEWAL_UPLOADED' AND renewal_file.id IS NOT NULL
                            THEN renewal_file.file_kind
                            WHEN EXISTS (
                              SELECT 1
                                FROM dcc_registration_certificate_file f
                               WHERE f.tenant_id = e.tenant_id
                                 AND f.owner_type = 'CHANGE'
                                 AND f.owner_id = c.id
                                 AND f.file_kind = 'CHANGE_APPROVAL'
                                 AND f.status = 'BOUND'
                                 AND f.deleted = 0)
                            THEN 'CHANGE_APPROVAL' ELSE NULL END AS file_kind,
                       e.target_version_id,
                       CASE WHEN e.event_type = 'RENEWAL_UPLOADED' THEN v.version_no ELSE NULL END AS version_no,
                       CASE WHEN e.event_type = 'RENEWAL_UPLOADED' THEN v.approval_date
                            ELSE c.approval_date END AS approval_date,
                       CASE WHEN e.event_type = 'RENEWAL_UPLOADED' THEN v.effective_date ELSE NULL END AS effective_date,
                       CASE WHEN e.event_type = 'RENEWAL_UPLOADED' THEN v.expiry_date ELSE NULL END AS expiry_date,
                       CASE WHEN e.event_type = 'RENEWAL_UPLOADED' THEN v.category_changed ELSE NULL END AS category_changed,
                       CASE WHEN e.event_type = 'RENEWAL_UPLOADED' THEN v.certificate_no ELSE NULL END AS certificate_no,
                       CASE WHEN e.event_type = 'RENEWAL_UPLOADED' THEN v.classification ELSE NULL END AS classification,
                       CASE WHEN e.event_type = 'RENEWAL_UPLOADED'
                            THEN renewal_file.original_name
                            ELSE (SELECT MIN(f.original_name)
                                    FROM dcc_registration_certificate_file f
                                   WHERE f.tenant_id = e.tenant_id
                                     AND f.owner_type = 'CHANGE'
                                     AND f.owner_id = c.id
                                     AND f.file_kind = 'CHANGE_APPROVAL'
                                     AND f.status = 'BOUND'
                                     AND f.deleted = 0)
                       END AS original_file_name,
                       CASE WHEN e.event_type = 'RENEWAL_UPLOADED'
                            THEN renewal_file.status
                            ELSE (SELECT MIN(f.status)
                                    FROM dcc_registration_certificate_file f
                                   WHERE f.tenant_id = e.tenant_id
                                     AND f.owner_type = 'CHANGE'
                                     AND f.owner_id = c.id
                                     AND f.file_kind = 'CHANGE_APPROVAL'
                                     AND f.status = 'BOUND'
                                     AND f.deleted = 0)
                       END AS file_status,
                       e.occurred_at
                  FROM dcc_registration_certificate_lifecycle_event e
                  LEFT JOIN dcc_registration_certificate_change c
                    ON c.tenant_id = e.tenant_id AND c.event_id = e.id AND c.deleted = 0
                  LEFT JOIN dcc_registration_certificate_change_item i
                    ON i.tenant_id = c.tenant_id AND i.change_id = c.id
                  LEFT JOIN dcc_registration_certificate_version v
                    ON v.tenant_id = e.tenant_id
                   AND v.id = e.target_version_id
                   AND v.deleted = 0
                  LEFT JOIN dcc_registration_certificate_file renewal_file
                    ON e.event_type = 'RENEWAL_UPLOADED'
                   AND renewal_file.tenant_id = e.tenant_id
                   AND renewal_file.owner_type = 'VERSION'
                   AND renewal_file.owner_id = e.target_version_id
                   AND renewal_file.file_kind = 'REGISTRATION_CERTIFICATE'
                   AND renewal_file.status IN ('BOUND', 'VOIDED')
                   AND renewal_file.deleted = 0
                   AND renewal_file.id = (
                         SELECT MIN(candidate.id)
                           FROM dcc_registration_certificate_file candidate
                          WHERE candidate.tenant_id = e.tenant_id
                            AND candidate.owner_type = 'VERSION'
                            AND candidate.owner_id = e.target_version_id
                            AND candidate.file_kind = 'REGISTRATION_CERTIFICATE'
                            AND candidate.status IN ('BOUND', 'VOIDED')
                            AND candidate.deleted = 0)
                 WHERE e.tenant_id = ? AND e.certificate_id = ?
                 ORDER BY e.event_sequence ASC, COALESCE(i.sort_order, 0) ASC, i.id ASC
                """, (rs, rowNum) -> new DccRegistrationCertificateHistoryItem(
                rs.getString("event_type"),
                rs.getString("item_type"),
                rs.getString("before_value_json"),
                rs.getString("after_value_json"),
                rs.getObject("actor_id", Long.class),
                rs.getObject("business_file_id", Long.class),
                rs.getString("file_kind"),
                rs.getObject("target_version_id", Long.class),
                rs.getObject("version_no", Integer.class),
                rs.getObject("approval_date", java.time.LocalDate.class),
                rs.getObject("effective_date", java.time.LocalDate.class),
                rs.getObject("expiry_date", java.time.LocalDate.class),
                rs.getObject("category_changed", Boolean.class),
                rs.getString("certificate_no"),
                rs.getString("classification"),
                rs.getString("original_file_name"),
                rs.getString("file_status"),
                rs.getObject("occurred_at", java.time.LocalDateTime.class),
                null, null, null, null,
                rs.getObject("event_id", Long.class),
                rs.getObject("change_id", Long.class),
                rs.getObject("approval_request_id", Long.class),
                rs.getString("change_status"),
                rs.getObject("submitted_by", Long.class),
                rs.getObject("occurred_at", java.time.LocalDateTime.class),
                rs.getObject("reviewed_by", Long.class),
                rs.getObject("reviewed_at", java.time.LocalDateTime.class),
                null, null), tenantId, certificateId);
        Map<Long, String> changeUserNames = resolveChangeUserNames(items);
        Map<Long, DccRegistrationCertificateOperationAudit> renewalAudits =
                operationAuditService.getRenewalAudits(tenantId, certificateId);
        return items.stream()
                .map(item -> withChangeAudit(item, changeUserNames))
                .map(item -> withRenewalAudit(item, renewalAudits))
                .toList();
    }

    private Map<Long, String> resolveChangeUserNames(List<DccRegistrationCertificateHistoryItem> items) {
        Set<Long> userIds = new LinkedHashSet<>();
        items.stream().filter(item -> item.changeId() != null).forEach(item -> {
            if (item.submittedBy() != null) {
                userIds.add(item.submittedBy());
            }
            if (item.reviewedBy() != null) {
                userIds.add(item.reviewedBy());
            }
        });
        if (userIds.isEmpty()) {
            return Map.of();
        }
        List<AdminUserRespDTO> users = adminUserApi.getUserList(userIds);
        if (users == null) {
            throw new IllegalStateException("注册证变更操作人员查询失败");
        }
        Map<Long, AdminUserRespDTO> usersById = users.stream()
                .filter(user -> user != null && user.getId() != null)
                .collect(Collectors.toMap(AdminUserRespDTO::getId, Function.identity(),
                        (left, right) -> left, LinkedHashMap::new));
        Map<Long, String> names = new LinkedHashMap<>();
        for (Long userId : userIds) {
            AdminUserRespDTO user = usersById.get(userId);
            if (user == null || user.getNickname() == null || user.getNickname().isBlank()) {
                throw new IllegalStateException("注册证变更操作人员正式姓名缺失");
            }
            names.put(userId, user.getNickname().trim());
        }
        return Map.copyOf(names);
    }

    private static DccRegistrationCertificateHistoryItem withChangeAudit(
            DccRegistrationCertificateHistoryItem item, Map<Long, String> userNames) {
        if (item.changeId() == null) {
            return item;
        }
        return new DccRegistrationCertificateHistoryItem(
                item.eventType(), item.itemType(), item.beforeValueJson(), item.afterValueJson(), item.actorId(),
                item.businessFileId(), item.fileKind(), item.targetVersionId(), item.versionNo(), item.approvalDate(),
                item.effectiveDate(), item.expiryDate(), item.categoryChanged(), item.certificateNo(),
                item.classification(), item.originalFileName(), item.fileStatus(), item.occurredAt(),
                item.renewalOperatorName(), item.renewalOperatedAt(), item.renewalApproverName(),
                item.renewalApprovedAt(), item.eventId(), item.changeId(), item.approvalRequestId(),
                item.changeStatus(), item.submittedBy(), item.submittedAt(), item.reviewedBy(), item.reviewedAt(),
                userName(userNames, item.submittedBy()), userName(userNames, item.reviewedBy()));
    }

    private static String userName(Map<Long, String> userNames, Long userId) {
        return userId == null ? null : userNames.get(userId);
    }

    private static DccRegistrationCertificateHistoryItem withRenewalAudit(
            DccRegistrationCertificateHistoryItem item,
            Map<Long, DccRegistrationCertificateOperationAudit> renewalAudits) {
        if (!"RENEWAL_UPLOADED".equals(item.eventType())) {
            return item;
        }
        DccRegistrationCertificateOperationAudit renewalAudit = renewalAudits.get(item.targetVersionId());
        if (renewalAudit == null) {
            throw new IllegalStateException("延续版本操作审计记录缺失");
        }
        return new DccRegistrationCertificateHistoryItem(
                item.eventType(), item.itemType(), item.beforeValueJson(), item.afterValueJson(), item.actorId(),
                item.businessFileId(), item.fileKind(), item.targetVersionId(), item.versionNo(),
                item.approvalDate(), item.effectiveDate(), item.expiryDate(), item.categoryChanged(),
                item.certificateNo(), item.classification(), item.originalFileName(), item.fileStatus(),
                item.occurredAt(), renewalAudit.operatorName(), renewalAudit.operatedAt(),
                renewalAudit.approverName(), renewalAudit.approvedAt(), item.eventId(), item.changeId(),
                item.approvalRequestId(), item.changeStatus(), item.submittedBy(), item.submittedAt(),
                item.reviewedBy(), item.reviewedAt(), item.submittedByName(), item.reviewedByName());
    }
}
