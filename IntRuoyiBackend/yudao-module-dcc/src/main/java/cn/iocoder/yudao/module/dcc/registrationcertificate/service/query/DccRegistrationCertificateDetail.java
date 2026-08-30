package cn.iocoder.yudao.module.dcc.registrationcertificate.service.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccRegistrationCertificateDetail {

    private Long certificateId;
    private Integer rowVersion;
    private Long versionId;
    private Long snapshotId;
    private Integer snapshotRevision;
    private Long ownerCompanyId;
    private String ownerCompanyName;
    private Long productMasterId;
    private String productName;
    private Long projectCodeId;
    private String projectCode;
    private String certificateNo;
    private Integer versionNo;
    private String status;
    private LocalDate firstObtainedDate;
    private LocalDate approvalDate;
    private LocalDate effectiveDate;
    private LocalDate expiryDate;
    private String classification;
    private String remark;
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
    private Long registrationFileId;
    private String registrationFileName;
    private Boolean hasRegistrationFile;
    private String reminderColor;
    private String visualState;
}
