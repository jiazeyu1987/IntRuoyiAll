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
        MultipartFile file) {
    public DccRegistrationCertificateUploadCommand {
        entrustedEnterpriseIds = entrustedEnterpriseIds == null ? null : List.copyOf(entrustedEnterpriseIds);
    }
}
