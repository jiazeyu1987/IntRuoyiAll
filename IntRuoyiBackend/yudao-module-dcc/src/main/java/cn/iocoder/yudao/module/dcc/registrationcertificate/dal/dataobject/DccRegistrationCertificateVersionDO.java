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

import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("dcc_registration_certificate_version")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccRegistrationCertificateVersionDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long certificateId;
    private Integer versionNo;
    private String versionType;
    private String certificateNo;
    private LocalDate approvalDate;
    private LocalDate effectiveDate;
    private LocalDate expiryDate;
    private String classification;
    private Boolean categoryChanged;
    private Long baseSnapshotId;
    private String remark;
    private String status;
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Integer currentUniqueFlag;
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Integer pendingUniqueFlag;
    private LocalDateTime formalizedAt;
    private Long formalizedBy;
    private LocalDateTime voidedAt;
    private Long voidedBy;
    private String voidReason;
}
