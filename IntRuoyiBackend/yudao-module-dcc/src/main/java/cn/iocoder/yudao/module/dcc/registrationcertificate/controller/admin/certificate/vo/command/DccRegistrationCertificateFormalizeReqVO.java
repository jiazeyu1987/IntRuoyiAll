package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.certificate.vo.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class DccRegistrationCertificateFormalizeReqVO {

    @NotNull
    @Positive
    private Integer expectedRowVersion;
    @NotNull
    @Positive
    private Integer expectedSnapshotRevision;
    @NotNull
    @Positive
    private Long businessFileId;
}
