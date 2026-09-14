package cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@TableName("dcc_registration_certificate_access_audit")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccRegistrationCertificateAccessAuditDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long requestId;
    private Long grantId;
    private Long businessFileId;
    private Long actorUserId;
    private String eventType;
    private String eventKey;
    private String result;
    private LocalDateTime occurredAt;
    private String detailJson;
}
