package cn.iocoder.yudao.module.signature.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

@TableName("system_electronic_signature")
@KeySequence("system_electronic_signature_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElectronicSignatureRecordDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String moduleCode;
    private String actionCode;
    private String subjectType;
    private String subjectId;
    private String subjectVersion;
    private Long actorId;
    private String meaningCode;
    private String meaningLabel;
    private String reason;
    private LocalDateTime signedAt;
    private String timeEvidenceId;
    private String authenticationMethod;
    private String contentHash;
    private String beforeContentHash;
    private String afterContentHash;
    private String canonicalContentJson;
    private String beforeContentJson;
    private String afterContentJson;
    private String fieldDiffJson;
    private String evidenceHash;
    private String algorithm;
    private String keyVersion;
    private String policyVersion;
    private String verificationStatus;
    private String idempotencyKey;
    private String commandHash;
    private String processInstanceId;
    private String taskId;
    private String nodeCode;
    private Integer nodeOrder;

}
