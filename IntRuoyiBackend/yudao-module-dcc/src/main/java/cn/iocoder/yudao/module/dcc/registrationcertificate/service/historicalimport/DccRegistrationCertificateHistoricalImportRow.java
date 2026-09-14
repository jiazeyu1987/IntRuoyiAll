package cn.iocoder.yudao.module.dcc.registrationcertificate.service.historicalimport;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DccRegistrationCertificateHistoricalImportRow {

    private Long id;
    private Long ownerCompanyId;
    private Long certificateId;
    private Long certificateRecordId;
    private Long certificateOwnerCompanyId;
    private Long versionId;
    private Long versionRecordId;
    private Long snapshotId;
    private Long snapshotRecordId;
    private Long actorId;
    private String result;
    private String resultCode;
    private String requestTraceId;
    private String detailJson;
    private LocalDateTime occurredAt;
    private String certificateNo;
    private Integer versionNo;
    private String productName;
}
