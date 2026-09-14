package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.supportingdocument.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DccRegistrationCertificateSupportingDocumentReviewReqVO {

    @NotNull
    @Positive
    private Long versionId;
    @NotNull
    @Positive
    private Long businessFileId;
    @NotNull
    @Positive
    private Integer expectedRowVersion;
    @NotBlank
    @Size(max = 64)
    private String documentType;
    @Size(max = 512)
    private String rejectReason;
}
