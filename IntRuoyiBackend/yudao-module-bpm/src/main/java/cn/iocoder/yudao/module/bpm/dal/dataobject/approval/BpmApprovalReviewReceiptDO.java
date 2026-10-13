package cn.iocoder.yudao.module.bpm.dal.dataobject.approval;
import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
@Data
@TableName("bpm_approval_review_receipt")
public class BpmApprovalReviewReceiptDO {
    @TableId private Long id;
    private Long tenantId;
    private Long actorId;
    private Long operatorId;
    private String identityDomain;
    private String idempotencyKey;
    private String requestHash;
    private String requestHashVersion;
    private String prepareId;
    private String identityJson;
    private String contentPolicyVersion;
    private String subjectVersion;
    private String previewContentHash;
    private String canonicalBusinessContentJson;
    private String actionCode;
    private String reason;
    private Long centerSignatureId;
    private String centerEvidenceHash;
    private Long signatureCreationAuditId;
    private String resultCode;
    private String resultingStatus;
    private String domainLinksJson;
    private String responseJson;
    private Long receiptAuditId;
    private String receiptRecordHash;
    private LocalDateTime committedAt;
}
