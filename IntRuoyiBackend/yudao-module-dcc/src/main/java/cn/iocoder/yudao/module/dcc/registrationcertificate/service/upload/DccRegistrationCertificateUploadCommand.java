package cn.iocoder.yudao.module.dcc.registrationcertificate.service.upload;

import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public record DccRegistrationCertificateUploadCommand(
        Long projectCodeId,
        Long companyId,
        String productName,
        String certificateNo,
        LocalDate firstObtainedDate,
        LocalDate effectiveDate,
        LocalDate expiryDate,
        String classification,
        Boolean entrustedProduction,
        Boolean selfProduction,
        List<Long> entrustedEnterpriseIds,
        String remark,
        MultipartFile file,
        String factsJson,
        List<MultipartFile> attachments,
        List<MultipartFile> productDescriptionFiles,
        Long sourceRequestId, List<Long> retainedFileIds) {
    public DccRegistrationCertificateUploadCommand(Long projectCodeId, Long companyId, String productName,
            String certificateNo, LocalDate firstObtainedDate, LocalDate effectiveDate, LocalDate expiryDate,
            String classification, Boolean entrustedProduction, Boolean selfProduction,
            List<Long> entrustedEnterpriseIds, String remark, MultipartFile file) {
        this(projectCodeId, companyId, productName, certificateNo, firstObtainedDate, effectiveDate, expiryDate,
                classification, entrustedProduction, selfProduction, entrustedEnterpriseIds, remark, file,
                new cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateDraftData(
                    companyId, null, productName, projectCodeId, firstObtainedDate, certificateNo, null,
                    effectiveDate, expiryDate, classification, null, null, null, null, null, null, null,
                    entrustedProduction, selfProduction, entrustedEnterpriseIds, remark).factsJson(), List.of(), List.of(), null, List.of());
    }
    public DccRegistrationCertificateUploadCommand {
        entrustedEnterpriseIds = entrustedEnterpriseIds == null ? null : List.copyOf(entrustedEnterpriseIds);
    }
}
