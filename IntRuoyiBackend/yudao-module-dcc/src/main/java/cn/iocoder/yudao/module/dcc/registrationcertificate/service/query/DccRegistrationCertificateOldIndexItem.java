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
    private String productName;
    private String certificateNo;
    private Integer versionNo;
    private LocalDate expiryDate;
    private String status;
}
