package cn.iocoder.yudao.module.dcc.registrationcertificate.service.query;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.Map;

/** Read only after the caller has passed the selected version's detail authorization. */
@Service
public class DccRegistrationCertificateEvidenceService {
    private final JdbcTemplate jdbc;
    public DccRegistrationCertificateEvidenceService(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public Map<String,Object> getEvidence(Long tenantId,Long certificateId,Long versionId) {
        var snapshots=jdbc.queryForList("""
            SELECT id,revision_no AS revisionNo,facts_json AS factsJson,product_name AS productName,
                   registrant_name AS registrantName,model_specification AS modelSpecification,
                   structure_composition AS structureComposition,intended_use AS intendedUse,
                   technical_requirements AS technicalRequirements,residence_address AS residenceAddress,
                   production_address AS productionAddress,entrusted_production AS entrustedProduction,
                   self_production AS selfProduction,entrusted_enterprises_json AS entrustedEnterprisesJson,
                   effective_at AS effectiveAt,create_time AS recordedAt
              FROM dcc_registration_certificate_snapshot WHERE tenant_id=? AND version_id=? AND deleted=0
             ORDER BY revision_no DESC,id DESC
            """,tenantId,versionId);
        var files=jdbc.queryForList("""
            SELECT f.id,f.file_kind AS fileKind,f.original_name AS originalName,f.bound_at AS boundAt
              FROM dcc_registration_certificate_file f
             WHERE f.tenant_id=? AND f.deleted=0 AND f.status='BOUND'
               AND ((f.owner_type='VERSION' AND f.owner_id=?) OR
                    (f.owner_type='CHANGE' AND EXISTS(SELECT 1 FROM dcc_registration_certificate_change ch
                     WHERE ch.tenant_id=f.tenant_id AND ch.id=f.owner_id AND ch.source_version_id=?
                       AND ch.certificate_id=? AND ch.status='APPLIED' AND ch.deleted=0)))
             ORDER BY f.bound_at,f.id
            """,tenantId,versionId,versionId,certificateId);
        var applications=jdbc.queryForList("""
            SELECT r.id,r.request_type AS requestType,r.status,r.requester_user_id AS requesterUserId,
                   u.nickname AS requesterName,r.requested_at AS requestedAt,r.completed_at AS completedAt,
                   r.reject_reason AS rejectReason,r.detail_json AS detailJson,b.detail_json AS approvalDetailJson,
                   COALESCE(b.bpm_process_instance_id,r.bpm_process_instance_id) AS bpmProcessInstanceId
              FROM dcc_registration_certificate_access_request r
              LEFT JOIN system_users u ON u.id=r.requester_user_id AND u.tenant_id=r.tenant_id AND u.deleted=0
              LEFT JOIN dcc_registration_certificate_bpm_binding b ON b.request_id=r.id AND b.tenant_id=r.tenant_id AND b.deleted=0
             WHERE r.tenant_id=? AND r.certificate_id=? AND r.request_type='UPLOAD_CERTIFICATE' AND r.deleted=0
               AND EXISTS (SELECT 1 FROM dcc_registration_certificate_access_request_file rf
                JOIN dcc_registration_certificate_file bf ON bf.id=rf.business_file_id AND bf.tenant_id=rf.tenant_id AND bf.deleted=0
                WHERE rf.tenant_id=r.tenant_id AND rf.request_id=r.id AND rf.deleted=0
                 AND ((bf.owner_type='VERSION' AND bf.owner_id=?) OR
                  (bf.owner_type='CHANGE' AND EXISTS(SELECT 1 FROM dcc_registration_certificate_change ch
                   WHERE ch.tenant_id=bf.tenant_id AND ch.id=bf.owner_id AND ch.source_version_id=? AND ch.deleted=0))))
             ORDER BY r.requested_at DESC,r.id DESC
            """,tenantId,certificateId,versionId,versionId);
        for (var application : applications) {
            Object bindingJson=application.get("approvalDetailJson");
            if(bindingJson!=null) {
                var binding=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(String.valueOf(bindingJson),Map.class);
                Object reviewer=binding.get("reviewerUserId");
                if(reviewer!=null) {
                    Long userId=Long.valueOf(String.valueOf(reviewer));
                    application.put("reviewerUserId",userId);
                    var names=jdbc.queryForList("SELECT nickname FROM system_users WHERE tenant_id=? AND id=? AND deleted=0",String.class,tenantId,userId);
                    if(names.size()==1)application.put("reviewerName",names.get(0));
                }
            }
            application.put("files",jdbc.queryForList("""
                SELECT f.id,f.file_kind AS fileKind,f.original_name AS originalName,rf.status
                 FROM dcc_registration_certificate_access_request_file rf
                 JOIN dcc_registration_certificate_file f ON f.id=rf.business_file_id AND f.tenant_id=rf.tenant_id AND f.deleted=0
                 WHERE rf.tenant_id=? AND rf.request_id=? AND rf.deleted=0 ORDER BY rf.id
                """,tenantId,application.get("id")));
            application.put("changes",jdbc.queryForList("""
                SELECT i.item_type AS itemType,i.before_value_json AS beforeValueJson,i.after_value_json AS afterValueJson
                 FROM dcc_registration_certificate_change_item i
                 JOIN dcc_registration_certificate_change ch ON ch.id=i.change_id AND ch.tenant_id=i.tenant_id AND ch.deleted=0
                 WHERE i.tenant_id=? AND ch.approval_request_id=? ORDER BY i.id
                """,tenantId,application.get("id")));
        }
        return Map.of("snapshots",snapshots,"files",files,"applications",applications);
    }
}
