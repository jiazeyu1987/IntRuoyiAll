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

import java.time.LocalDate;

@TableName("dcc_registration_certificate")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccRegistrationCertificateDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long ownerCompanyId;
    private Long productMasterId;
    private Long projectCodeId;
    private LocalDate firstObtainedDate;
    private Long currentVersionId;
    private Long pendingVersionId;
    private Long currentSnapshotId;
    private String status;
    private Integer rowVersion;
}
