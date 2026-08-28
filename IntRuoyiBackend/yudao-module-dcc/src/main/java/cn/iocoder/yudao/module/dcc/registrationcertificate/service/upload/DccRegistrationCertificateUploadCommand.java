package cn.iocoder.yudao.module.dcc.registrationcertificate.service.upload;

import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

public record DccRegistrationCertificateUploadCommand(
        Long projectCodeId,
        String companyName,
        String certificateNo,
        LocalDate firstObtainedDate,
        LocalDate effectiveDate,
        LocalDate expiryDate,
        String classification,
        String remark,
        MultipartFile file) {
}
