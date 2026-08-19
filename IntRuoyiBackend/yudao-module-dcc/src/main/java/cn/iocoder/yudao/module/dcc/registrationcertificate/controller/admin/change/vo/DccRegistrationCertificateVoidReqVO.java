package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.change.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DccRegistrationCertificateVoidReqVO {

    @NotNull
    @Positive
    private Integer expectedRowVersion;
    @NotNull
    private LocalDate approvalDate;
    @NotBlank
    @Size(max = 512)
    private String voidReason;
}
