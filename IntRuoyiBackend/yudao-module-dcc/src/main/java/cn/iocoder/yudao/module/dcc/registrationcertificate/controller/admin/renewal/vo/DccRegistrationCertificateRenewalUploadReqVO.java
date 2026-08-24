package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.renewal.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DccRegistrationCertificateRenewalUploadReqVO {

    @NotNull
    @Positive
    private Integer expectedRowVersion;
    @NotNull
    @Positive
    private Long currentVersionId;
    @Positive
    private Long businessFileId;
    @NotNull
    private Boolean categoryChanged;
    @Size(max = 128)
    private String certificateNo;
    @Size(max = 64)
    private String classification;
    @NotNull
    private LocalDate approvalDate;
    @NotNull
    private LocalDate effectiveDate;
    @NotNull
    private LocalDate expiryDate;
}
