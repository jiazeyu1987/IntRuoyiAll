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
    @NotNull
    private LocalDate approvalDate;
    @NotNull
    private LocalDate effectiveDate;
    @NotNull
    private LocalDate expiryDate;
    @NotBlank
    @Size(max = 64)
    private String classification;
    @NotBlank
    @Size(max = 255)
    private String registrantName;
    @NotBlank
    private String modelSpecification;
    @NotBlank
    private String structureComposition;
    @NotBlank
    private String intendedUse;
    @NotBlank
    private String technicalRequirements;
    @NotBlank
    private String residenceAddress;
    @NotBlank
    private String productionAddress;
    @NotNull
    private Boolean entrustedProduction;
    @NotNull
    private Boolean selfProduction;
    @NotNull
    private List<@Positive Long> entrustedEnterpriseIds;

    public DccRegistrationCertificateDraftData toDraftData() {
        return new DccRegistrationCertificateDraftData(
                ownerCompanyId, productMasterId, projectCodeId, firstObtainedDate,
                certificateNo, approvalDate, effectiveDate, expiryDate, classification,
                registrantName, modelSpecification, structureComposition, intendedUse,
                technicalRequirements, residenceAddress, productionAddress,
                entrustedProduction, selfProduction, entrustedEnterpriseIds);
    }
}
