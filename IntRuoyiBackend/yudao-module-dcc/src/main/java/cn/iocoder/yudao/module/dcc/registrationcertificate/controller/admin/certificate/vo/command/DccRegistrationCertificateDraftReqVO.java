package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.certificate.vo.command;

import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateDraftData;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class DccRegistrationCertificateDraftReqVO {

    @NotNull
    @Positive
    private Long ownerCompanyId;
    @NotNull
    @Positive
    private Long productMasterId;
    @Positive
    private Long projectCodeId;
    @NotNull
    private LocalDate firstObtainedDate;
    @NotBlank
    @Size(max = 128)
    private String certificateNo;
    private LocalDate approvalDate;
    @NotNull
    private LocalDate effectiveDate;
    @NotNull
    private LocalDate expiryDate;
    @NotBlank
    @Size(max = 64)
    private String classification;
    @Size(max = 255)
    private String registrantName;
    private String modelSpecification;
    private String structureComposition;
    private String intendedUse;
    private String technicalRequirements;
    private String residenceAddress;
    private String productionAddress;
    private Boolean entrustedProduction;
    private Boolean selfProduction;
    private List<@Positive Long> entrustedEnterpriseIds;
    @Size(max = 1024)
    private String remark;

    public DccRegistrationCertificateDraftData toDraftData() {
        return new DccRegistrationCertificateDraftData(
                ownerCompanyId, productMasterId, projectCodeId, firstObtainedDate,
                certificateNo, approvalDate, effectiveDate, expiryDate, classification,
                registrantName, modelSpecification, structureComposition, intendedUse,
                technicalRequirements, residenceAddress, productionAddress,
                entrustedProduction, selfProduction, entrustedEnterpriseIds, remark);
    }
}
