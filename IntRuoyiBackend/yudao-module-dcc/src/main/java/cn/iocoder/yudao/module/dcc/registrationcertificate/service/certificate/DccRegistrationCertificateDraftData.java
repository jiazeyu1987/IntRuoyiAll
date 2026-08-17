package cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate;

import java.time.LocalDate;
import java.util.List;

public record DccRegistrationCertificateDraftData(
        Long ownerCompanyId,
        Long productMasterId,
        Long projectCodeId,
        LocalDate firstObtainedDate,
        String certificateNo,
        LocalDate approvalDate,
        LocalDate effectiveDate,
        LocalDate expiryDate,
        String classification,
        String registrantName,
        String modelSpecification,
        String structureComposition,
        String intendedUse,
        String technicalRequirements,
        String residenceAddress,
        String productionAddress,
        Boolean entrustedProduction,
        Boolean selfProduction,
        List<Long> entrustedEnterpriseIds) {

    public DccRegistrationCertificateDraftData {
        entrustedEnterpriseIds = entrustedEnterpriseIds == null ? null : List.copyOf(entrustedEnterpriseIds);
    }
}
