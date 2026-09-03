package cn.iocoder.yudao.module.dcc.registrationcertificate.service.query;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DccRegistrationCertificateQueryRecord {
    private Long certificateId;
    private Integer rowVersion;
    private Long versionId;
    private Long snapshotId;
    private Integer snapshotRevision;
    private Long ownerCompanyId;
    private Long productMasterId;
    private Long projectCodeId;
    private String projectCode;
    private Boolean hasPendingRenewal;
    private String reminderColor;
    private String visualState;
    private LocalDate firstObtainedDate;
    private String productName;
    private String certificateNo;
    private Integer versionNo;
    private String status;
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
}
