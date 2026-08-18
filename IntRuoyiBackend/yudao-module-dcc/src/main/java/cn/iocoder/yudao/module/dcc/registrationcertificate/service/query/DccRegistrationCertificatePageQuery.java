package cn.iocoder.yudao.module.dcc.registrationcertificate.service.query;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class DccRegistrationCertificatePageQuery {

    @Builder.Default
    private Integer pageNo = 1;
    @Builder.Default
    private Integer pageSize = 10;
    private Long ownerCompanyId;
    private Long productMasterId;
    private String status;
    private String certificateNo;
    private Boolean missingProjectCode;
    private Boolean missingFile;
    private LocalDate firstObtainedStart;
    private LocalDate firstObtainedEnd;
    private LocalDate approvalStart;
    private LocalDate approvalEnd;
    private LocalDate effectiveStart;
    private LocalDate effectiveEnd;
    private LocalDate expiryStart;
    private LocalDate expiryEnd;
}
