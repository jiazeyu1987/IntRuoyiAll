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
public class DccRegistrationCertificateOldIndexItem {

    private Long certificateId;
    private Long versionId;
    private Long ownerCompanyId;
    private String ownerCompanyName;
    private Long productMasterId;
    private String productName;
    private Long projectCodeId;
    private String projectCode;
    private String certificateNo;
    private Integer versionNo;
    private String classification;
    private LocalDate expiryDate;
    private String status;
}
