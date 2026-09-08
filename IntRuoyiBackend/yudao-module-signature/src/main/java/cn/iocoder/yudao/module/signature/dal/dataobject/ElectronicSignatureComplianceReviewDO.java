package cn.iocoder.yudao.module.signature.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("system_electronic_signature_review")
@KeySequence("system_electronic_signature_review_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElectronicSignatureComplianceReviewDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String reviewType;
    private String reviewScope;
    private String triggerReason;
    private String status;
    private Long ownerUserId;
    private Long backupOwnerUserId;
    private String sopVersion;
    private String trainingEvidenceId;
    private LocalDateTime plannedAt;
    private LocalDateTime dueAt;
    private LocalDateTime escalatedAt;
    private Long escalatedToUserId;
    private LocalDateTime completedAt;
    private Long completedBy;
    private String sampleRuleJson;
    private String resultSummaryJson;

}
