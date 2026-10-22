package cn.iocoder.yudao.module.bpm.dal.mysql.approval;
import cn.iocoder.yudao.module.bpm.dal.dataobject.approval.BpmApprovalReviewReceiptDO;
import org.apache.ibatis.annotations.*;
@Mapper
public interface BpmApprovalReviewReceiptMapper {
    @Select("SELECT * FROM bpm_approval_review_receipt WHERE tenant_id=#{tenantId} AND idempotency_key=#{key}")
    BpmApprovalReviewReceiptDO selectExact(@Param("tenantId") Long tenantId,@Param("key") String key);
    @Select("SELECT * FROM bpm_approval_review_receipt WHERE tenant_id=#{tenantId} AND idempotency_key=#{key} FOR UPDATE")
    BpmApprovalReviewReceiptDO selectExactCurrent(@Param("tenantId") Long tenantId,@Param("key") String key);
    @Insert("INSERT INTO bpm_approval_review_receipt (id,tenant_id,actor_id,operator_id,identity_domain,idempotency_key,request_hash,request_hash_version,prepare_id,identity_json,content_policy_version,subject_version,preview_content_hash,canonical_business_content_json,action_code,reason,center_signature_id,center_evidence_hash,signature_creation_audit_id,result_code,resulting_status,domain_links_json,response_json,receipt_audit_id,receipt_record_hash,committed_at) VALUES (#{id},#{tenantId},#{actorId},#{operatorId},#{identityDomain},#{idempotencyKey},#{requestHash},#{requestHashVersion},#{prepareId},#{identityJson},#{contentPolicyVersion},#{subjectVersion},#{previewContentHash},#{canonicalBusinessContentJson},#{actionCode},#{reason},#{centerSignatureId},#{centerEvidenceHash},#{signatureCreationAuditId},#{resultCode},#{resultingStatus},#{domainLinksJson},#{responseJson},#{receiptAuditId},#{receiptRecordHash},#{committedAt})")
    int insert(BpmApprovalReviewReceiptDO row);
}
