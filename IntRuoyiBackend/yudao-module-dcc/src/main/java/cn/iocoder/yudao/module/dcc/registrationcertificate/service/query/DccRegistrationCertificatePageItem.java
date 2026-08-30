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
public class DccRegistrationCertificatePageItem {

    private Long certificateId;
    private Integer rowVersion;
    private Long versionId;
    private Long snapshotId;
    private Long ownerCompanyId;
    private String ownerCompanyName;
    private Long productMasterId;
    private String productName;
    private Long projectCodeId;
    private String projectCode;
    private String certificateNo;
    private Integer versionNo;
    private String status;
    private String classification;
    private String remark;
    private Boolean hasProjectCode;
    private Boolean hasRegistrationFile;
    private String reminderColor;
    private String visualState;
    private LocalDate firstObtainedDate;
    private LocalDate approvalDate;
    private LocalDate effectiveDate;
    private LocalDate expiryDate;
}
