package cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("gxp_audit_policy_version")
@KeySequence("gxp_audit_policy_version_seq")
@Data
public class GxpAuditPolicyVersionDO {

    @TableId
    private Long id;
    private Long tenantId;
    private String policyVersion;
    private String policyHash;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private String approvalReference;
    private String coverageReportHash;
    private String schemaVersion;
    private String canonicalPolicyJson;
    private String artifactHash;

}
