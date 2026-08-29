package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.renewal.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

@Data
public class DccRegistrationCertificateRenewalUploadReqVO {

    @NotNull
    @Positive
    private Integer expectedRowVersion;
    @NotNull
    @Positive
    private Long currentVersionId;
    @NotNull
    @DateTimeFormat(iso = DATE)
    private LocalDate approvalDate;
    @NotNull
    @DateTimeFormat(iso = DATE)
    private LocalDate effectiveDate;
    @NotNull
    @DateTimeFormat(iso = DATE)
    private LocalDate expiryDate;
    @NotNull
    private Boolean categoryChanged;
    @Size(max = 128)
    private String certificateNo;
    @Size(max = 64)
    private String classification;
    @NotNull
    private MultipartFile file;
}
