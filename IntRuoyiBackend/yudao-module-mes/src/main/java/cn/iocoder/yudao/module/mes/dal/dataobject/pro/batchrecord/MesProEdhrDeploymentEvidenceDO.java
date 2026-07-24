package cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("mes_pro_edhr_deployment_evidence")
@KeySequence("mes_pro_edhr_deployment_evidence_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrDeploymentEvidenceDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;

    private Long projectId;

    private String deploymentCode;

    private String deploymentName;

    private String customerProjectName;

    private String targetEnvironment;

    private Boolean environmentAuthorized;

    private String environmentCheckSummary;

    private String serverSummary;

    private String networkSummary;

    private String objectStorageSummary;

    private String capacitySummary;

    private String permissionSummary;

    private String releaseTag;

    private String artifactVersion;

    private String artifactChecksum;

    private String schemaVersion;

    private String migrationManifest;

    private String requiredSqlManifest;

    private String appImportResult;

    private String licenseScope;

    private LocalDate licenseValidUntil;

    private String licenseFileEvidence;

    private String licenseCheckResult;

    private String customerLicenseConfirmation;

    private String interfaceScope;

    private String interfaceVersion;

    private String integrationEnvironment;

    private String requestEvidence;

    private String responseEvidence;

    private Integer interfaceFailureCount;

    private String remediationAction;

    private String retestEvidence;

    private String interfaceConfirmedBy;

    private String deploymentStatus;

    private String blockedReason;

    private String nextAction;

    private Boolean gatePassed;

    private LocalDateTime gateCheckedAt;

    private String evidenceSnapshotChecksum;

    private String remark;
}

