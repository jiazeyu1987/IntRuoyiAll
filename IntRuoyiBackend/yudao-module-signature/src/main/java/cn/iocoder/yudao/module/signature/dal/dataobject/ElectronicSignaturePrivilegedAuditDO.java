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

@TableName("system_electronic_signature_privileged_audit")
@KeySequence("system_electronic_signature_privileged_audit_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElectronicSignaturePrivilegedAuditDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long reviewerUserId;
    private String operationCode;
    private String reason;
    private String resultStatus;
    private String evidenceHash;
    private LocalDateTime auditedAt;

}
