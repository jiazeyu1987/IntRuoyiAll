package cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("gxp_audit_policy_activation")
@KeySequence("gxp_audit_policy_activation_seq")
@Data
public class GxpAuditPolicyActivationDO {

    @TableId
    private Long id;
    private Long tenantId;
    private String policyVersion;
    private String policyHash;
    private String previousActivationHash;
    private String requestId;
    private Long actorId;
    private String approvalReference;
    private LocalDateTime activatedAtUtc;
    private Long effectiveAfterSequence;
    private String canonicalActivationJson;
    private String activationHash;

}
