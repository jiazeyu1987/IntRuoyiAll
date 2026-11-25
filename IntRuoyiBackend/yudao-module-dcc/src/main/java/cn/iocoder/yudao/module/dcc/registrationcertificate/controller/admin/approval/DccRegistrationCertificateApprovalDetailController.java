package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.approval;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.LinkedHashMap;
import java.util.Map;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@RestController
@RequestMapping("/dcc/registration-certificates/approval-detail")
public class DccRegistrationCertificateApprovalDetailController {
    private final DccRegistrationCertificateApprovalService approvals;
    private final JdbcTemplate jdbc;
    public DccRegistrationCertificateApprovalDetailController(DccRegistrationCertificateApprovalService approvals,
                                                             JdbcTemplate jdbc) {
        this.approvals = approvals;
        this.jdbc = jdbc;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public CommonResult<Map<String, Object>> get(@RequestParam Long requestId,
                                                 @RequestParam String processInstanceId) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        var request = approvals.requireApprovalDetailRequest(tenantId, getLoginUserId(), requestId, processInstanceId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("requestId", requestId);
        result.put("requestType", request.getRequestType());
        result.put("status", request.getStatus());
        result.put("purpose", request.getPurpose());
        result.put("requestedAt", request.getRequestedAt());
        result.put("completedAt", request.getCompletedAt());
        result.put("rejectReason", request.getRejectReason());
        result.put("withdrawReason", request.getWithdrawReason());
        Map<String, Object> submission = new LinkedHashMap<>(JsonUtils.parseObject(request.getDetailJson(), Map.class));
        // Older download applications did not persist the company display name.
        // Resolve only those records; preserve the submitted name snapshot when present.
        Object submittedCompanyName = submission.get("ownerCompanyName");
        if ("DOWNLOAD_FILE".equals(request.getRequestType())
                && (!(submittedCompanyName instanceof String name) || name.isBlank())) {
            var names = jdbc.queryForList("""
                SELECT name FROM mdm_enterprise
                 WHERE tenant_id=? AND id=? AND type='OWNED_COMPANY' AND deleted=0
                """, String.class, tenantId, request.getOwnerCompanyId());
            if (names.size() != 1 || names.get(0) == null || names.get(0).isBlank()) {
                throw new cn.iocoder.yudao.framework.common.exception.ServiceException(400, "下载申请所属公司不存在或名称为空");
            }
            submission.put("ownerCompanyName", names.get(0));
        }
        if ("DOWNLOAD_FILE".equals(request.getRequestType()) && (!(submission.get("certificateName") instanceof String name) || name.isBlank())) {
            var names = jdbc.queryForList("""
                SELECT s.product_name FROM dcc_registration_certificate_access_request_file rf
                LEFT JOIN dcc_registration_certificate_file f ON f.id=rf.business_file_id AND f.tenant_id=rf.tenant_id AND f.deleted=0
                LEFT JOIN dcc_registration_certificate_version v ON v.tenant_id=f.tenant_id AND v.deleted=0
                 AND v.id=CASE f.owner_type
                   WHEN 'VERSION' THEN f.owner_id
                   WHEN 'CHANGE' THEN (SELECT c.source_version_id FROM dcc_registration_certificate_change c WHERE c.id=f.owner_id AND c.tenant_id=f.tenant_id AND c.certificate_id=? AND c.deleted=0)
                   WHEN 'SUPPORTING_DOCUMENT' THEN (SELECT sd.version_id FROM dcc_registration_certificate_supporting_document sd WHERE sd.id=f.owner_id AND sd.tenant_id=f.tenant_id AND sd.business_file_id=f.id AND sd.certificate_id=? AND sd.deleted=0)
                 END
                LEFT JOIN dcc_registration_certificate_snapshot s ON s.version_id=v.id AND s.tenant_id=v.tenant_id AND s.deleted=0 AND v.certificate_id=?
                 AND s.id=(SELECT s2.id FROM dcc_registration_certificate_snapshot s2 WHERE s2.version_id=v.id AND s2.tenant_id=v.tenant_id AND s2.deleted=0 ORDER BY s2.revision_no DESC,s2.id DESC LIMIT 1)
                WHERE rf.tenant_id=? AND rf.request_id=? AND rf.deleted=0 ORDER BY rf.id
                """, String.class, request.getCertificateId(), request.getCertificateId(), request.getCertificateId(), tenantId, requestId);
            if (names.isEmpty() || names.stream().anyMatch(n -> n==null || n.isBlank())) {
                throw new cn.iocoder.yudao.framework.common.exception.ServiceException(400,"申请文件关联的注册证名称缺失");
            }
            submission.put("certificateName",String.join("；", names.stream().map(String::trim).distinct().toList()));
        }
        if (submission.get("draft") instanceof Map<?, ?> rawDraft
                && !rawDraft.containsKey("entrustedEnterprisesJson")
                && rawDraft.get("entrustedEnterpriseIds") instanceof java.util.List<?> ids) {
            Map<String, Object> draft = new LinkedHashMap<>((Map<String, Object>) rawDraft);
            java.util.List<Map<String, Object>> enterprises = new java.util.ArrayList<>();
            for (Object id : ids) {
                var names = jdbc.queryForList("SELECT name FROM mdm_enterprise WHERE tenant_id=? AND id=? AND deleted=0",
                        String.class, tenantId, id);
                if (names.size() != 1 || names.get(0) == null || names.get(0).isBlank()) {
                    draft.put("entrustedEnterpriseDisplayError", "申请受托企业不存在或名称缺失，无法完整核对");
                    continue;
                }
                enterprises.add(Map.of("enterpriseName", names.get(0)));
            }
            draft.put("entrustedEnterprisesJson", JsonUtils.toJsonString(enterprises));
            submission.put("draft", draft);
        }
        if ("CHANGE_CERTIFICATE".equals(submission.get("operation"))) {
            var dates = jdbc.queryForList("""
                SELECT s.facts_json FROM dcc_registration_certificate_change c
                JOIN dcc_registration_certificate_version v ON v.id=c.source_version_id
                 AND v.tenant_id=c.tenant_id AND v.deleted=0
                JOIN dcc_registration_certificate_snapshot s ON s.id=c.source_snapshot_id
                 AND s.version_id=v.id AND s.tenant_id=v.tenant_id AND s.deleted=0
                WHERE c.tenant_id=? AND c.approval_request_id=? AND c.deleted=0
                """, String.class, tenantId, requestId);
            result.put("originalApprovalDate", dates.size() == 1 && dates.get(0) != null
                    ? JsonUtils.parseObject(dates.get(0), Map.class).get("approvalDate") : null);
        }
        result.put("submission", submission);
        result.put("files", jdbc.queryForList("""
            SELECT f.id,f.file_kind AS fileKind,f.original_name AS originalName
              FROM dcc_registration_certificate_access_request_file rf
              JOIN dcc_registration_certificate_file f ON f.id=rf.business_file_id
               AND f.tenant_id=rf.tenant_id AND f.deleted=0
             WHERE rf.tenant_id=? AND rf.request_id=? AND rf.deleted=0 ORDER BY rf.id
            """, tenantId, requestId));
        result.put("changes", jdbc.queryForList("""
            SELECT i.item_type AS itemType,i.before_value_json AS beforeValueJson,i.after_value_json AS afterValueJson
              FROM dcc_registration_certificate_change_item i
              JOIN dcc_registration_certificate_change c ON c.id=i.change_id AND c.tenant_id=i.tenant_id AND c.deleted=0
             WHERE i.tenant_id=? AND c.approval_request_id=? ORDER BY i.id
            """, tenantId, requestId));
        return success(result);
    }
}
