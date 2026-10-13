package cn.iocoder.yudao.module.system.dal.dataobject.signature;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@TableName("system_signature_authentication_event")
@KeySequence("system_signature_authentication_event_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class SignatureAuthenticationEventDO extends TenantBaseDO {
    @TableId private Long id;
    private Long actorId;
    private String identityDomain;
    private Long operatorId;
    private String requestId;
    private String operationType;
    private String outcome;
    private LocalDateTime occurredAt;
    private Long credentialVersion;
    private String credentialGeneration;
}
