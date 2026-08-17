package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.certificate.vo.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DccRegistrationCertificateUpdateDraftReqVO extends DccRegistrationCertificateDraftReqVO {

    @NotNull
    @Positive
    private Integer expectedRowVersion;
    @NotNull
    @Positive
    private Integer expectedSnapshotRevision;
}
