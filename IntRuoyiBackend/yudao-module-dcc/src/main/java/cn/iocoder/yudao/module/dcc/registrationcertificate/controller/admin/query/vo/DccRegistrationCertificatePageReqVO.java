package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.query.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.query.DccRegistrationCertificatePageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
public class DccRegistrationCertificatePageReqVO extends PageParam {

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

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate firstObtainedStart;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate firstObtainedEnd;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate approvalStart;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate approvalEnd;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate effectiveStart;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate effectiveEnd;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expiryStart;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expiryEnd;
    private String sortField;
    private String sortOrder;

    public DccRegistrationCertificatePageQuery toQuery() {
        return DccRegistrationCertificatePageQuery.builder()
                .pageNo(getPageNo())
                .pageSize(getPageSize())
                .ownerCompanyId(ownerCompanyId)
                .productMasterId(productMasterId)
                .projectCodeId(projectCodeId)
                .status(status)
                .certificateNo(certificateNo)
                .ownerCompanyName(ownerCompanyName)
                .productName(productName)
                .classification(classification)
                .registrantName(registrantName)
                .modelSpecification(modelSpecification)
                .productionAddress(productionAddress)
                .entrustedEnterpriseName(entrustedEnterpriseName)
                .projectCode(projectCode)
                .missingProjectCode(missingProjectCode)
                .missingFile(missingFile)
                .reminderState(reminderState)
                .firstObtainedStart(firstObtainedStart)
                .firstObtainedEnd(firstObtainedEnd)
                .approvalStart(approvalStart)
                .approvalEnd(approvalEnd)
                .effectiveStart(effectiveStart)
                .effectiveEnd(effectiveEnd)
                .expiryStart(expiryStart)
                .expiryEnd(expiryEnd)
                .sortField(sortField)
                .sortOrder(sortOrder)
                .build();
    }
}
