package cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@TableName("dcc_registration_certificate_snapshot")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccRegistrationCertificateSnapshotDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long versionId;
    private Integer revisionNo;
    private Long sourceChangeId;
    private String productName;
    private String registrantName;
    private String modelSpecification;
    private String structureComposition;
    private String intendedUse;
    private String technicalRequirements;
    private String residenceAddress;
    private String productionAddress;
    private Boolean entrustedProduction;
    private Boolean selfProduction;
    private String entrustedEnterprisesJson;
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Integer entrustedEnterpriseCount;
    private LocalDateTime effectiveAt;
}
