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
        result.put("submission", JsonUtils.parseObject(request.getDetailJson(), Map.class));
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
