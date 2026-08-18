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

@TableName("dcc_registration_certificate_access_request")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccRegistrationCertificateAccessRequestDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long ownerCompanyId;
    private Long certificateId;
    private Long requesterUserId;
    private String requestType;
    private String requestKey;
    private String bpmProcessInstanceId;
    private String purpose;
    private Long projectCodeId;
    private String status;
    private LocalDateTime requestedAt;
    private LocalDateTime completedAt;
    private LocalDateTime withdrawnAt;
    private String withdrawReason;
    private String rejectReason;
    private String detailJson;
}
