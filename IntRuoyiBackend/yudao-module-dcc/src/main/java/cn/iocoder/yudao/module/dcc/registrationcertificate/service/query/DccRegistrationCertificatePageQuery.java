package cn.iocoder.yudao.module.dcc.registrationcertificate.service.query;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
    private String listFilters;
    private QueryWrapper<Object> listFilterWrapper;
    private String certificateType;
    private java.util.Map<String,String> fieldFilters;
    private String ceCertificateNo;
    private String ceRevision;
    @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
    private LocalDate ceIssueStart;
    @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
    private LocalDate ceIssueEnd;
    @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
    private LocalDate listingStart;
    @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
    private LocalDate listingEnd;
    private Long ownerCompanyId;
    private Long productMasterId;
    private Long projectCodeId;
    private String status;
    private String certificateNo;
    private String ownerCompanyName;
    private String productName;
    private String classification;
    private String registrantName;
    private String modelSpecification;
    private String productionAddress;
    private String entrustedEnterpriseName;
    private String projectCode;
    private Boolean missingProjectCode;
    private Boolean missingFile;
    private String reminderState;
    private LocalDate businessDate;
    private LocalDate firstObtainedStart;
    private LocalDate firstObtainedEnd;
    private LocalDate approvalStart;
    private LocalDate approvalEnd;
    private LocalDate effectiveStart;
    private LocalDate effectiveEnd;
    private LocalDate expiryStart;
    private LocalDate expiryEnd;
    private String sortField;
    private String sortOrder;
}
