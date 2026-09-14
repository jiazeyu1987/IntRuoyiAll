package cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate;

import java.time.LocalDate;
import java.util.List;

public record DccRegistrationCertificateDraftData(
        Long ownerCompanyId,
        Long productMasterId,
        String productName,
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
        List<Long> entrustedEnterpriseIds,
        String remark) {

    public DccRegistrationCertificateDraftData {
        productName = trim(productName);
        entrustedEnterpriseIds = entrustedEnterpriseIds == null ? null : List.copyOf(entrustedEnterpriseIds);
    }

    public DccRegistrationCertificateDraftData(
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
            List<Long> entrustedEnterpriseIds,
            String remark) {
        this(ownerCompanyId, productMasterId, null, projectCodeId, firstObtainedDate, certificateNo,
                approvalDate, effectiveDate, expiryDate, classification, registrantName, modelSpecification,
                structureComposition, intendedUse, technicalRequirements, residenceAddress, productionAddress,
                entrustedProduction, selfProduction, entrustedEnterpriseIds, remark);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
