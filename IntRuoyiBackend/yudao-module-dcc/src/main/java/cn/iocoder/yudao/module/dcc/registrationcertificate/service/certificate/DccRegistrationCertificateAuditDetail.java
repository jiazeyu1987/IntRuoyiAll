package cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DccRegistrationCertificateAuditDetail {

    private String commandKind;
    private Long actorId;
    private String payloadHash;
    private Long outcomeCertificateId;
    private Long outcomeVersionId;
    private Long outcomeSnapshotId;
    private Long outcomeBusinessFileId;
    private Integer failureCode;
    private String failureMessage;
}
