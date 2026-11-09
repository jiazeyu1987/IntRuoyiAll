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
        String remark,
        String factsJson) {

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
                entrustedProduction, selfProduction, entrustedEnterpriseIds, remark, domesticFacts(firstObtainedDate, certificateNo, approvalDate, effectiveDate, expiryDate, classification, remark, productMasterId));
    }


    public DccRegistrationCertificateDraftData(Long ownerCompanyId, Long productMasterId, String productName,
            Long projectCodeId, LocalDate firstObtainedDate, String certificateNo, LocalDate approvalDate,
            LocalDate effectiveDate, LocalDate expiryDate, String classification, String registrantName,
            String modelSpecification, String structureComposition, String intendedUse, String technicalRequirements,
            String residenceAddress, String productionAddress, Boolean entrustedProduction, Boolean selfProduction,
            List<Long> entrustedEnterpriseIds, String remark) {
        this(ownerCompanyId, productMasterId, productName, projectCodeId, firstObtainedDate, certificateNo,
                approvalDate, effectiveDate, expiryDate, classification, registrantName, modelSpecification,
                structureComposition, intendedUse, technicalRequirements, residenceAddress, productionAddress,
                entrustedProduction, selfProduction, entrustedEnterpriseIds, remark,
                domesticFacts(firstObtainedDate, certificateNo, approvalDate, effectiveDate, expiryDate, classification, remark, productMasterId));
    }

    private static String domesticFacts(LocalDate first, String number, LocalDate approval, LocalDate effective,
            LocalDate expiry, String classification, String remark, Long productMasterId) {
        var facts = new cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateFacts();
        facts.setCertificateType("DOMESTIC"); facts.setCertificateStatus("VALID"); facts.setOwnProduct(productMasterId != null);
        facts.setCertificateNo(number); facts.setFirstObtainedDate(first); facts.setApprovalDate(approval);
        facts.setEffectiveDate(effective); facts.setExpiryDate(expiry); facts.setClassification(classification); facts.setRemark(remark);
        return facts.toJson();
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
