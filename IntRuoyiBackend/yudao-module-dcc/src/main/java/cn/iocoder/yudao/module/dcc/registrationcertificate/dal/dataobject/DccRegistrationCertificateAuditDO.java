package cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("dcc_registration_certificate_audit")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccRegistrationCertificateAuditDO implements Serializable {

    @TableId
    private Long id;
    private Long tenantId;
    private Long ownerCompanyId;
    private Long certificateId;
    private Long versionId;
    private Long snapshotId;
    private Long businessFileId;
    private String eventKey;
    private String eventType;
    private Long actorId;
    private String result;
    private String resultCode;
    private String requestTraceId;
    private String detailJson;
    private LocalDateTime occurredAt;
    private String creator;
    private LocalDateTime createTime;
}
