package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.renewal.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DccRegistrationCertificateRenewalVoidReqVO {

    @NotNull
    @Positive
    private Integer expectedRowVersion;
    @NotBlank
    @Size(max = 512)
    private String voidReason;
}
