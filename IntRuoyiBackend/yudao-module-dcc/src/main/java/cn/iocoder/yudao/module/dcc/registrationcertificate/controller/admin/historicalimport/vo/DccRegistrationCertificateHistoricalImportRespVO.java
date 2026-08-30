package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.historicalimport.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DccRegistrationCertificateHistoricalImportRespVO {

    private Long id;
    private String sourceHash;
    private Integer sourceRow;
    private String payloadHash;
    private Long outcomeCertificateId;
    private Long outcomeVersionId;
    private Long outcomeSnapshotId;
    private List<String> restrictedReasons;
    private Long ownerCompanyId;
    private String ownerCompanyCode;
    private String ownerCompanyName;
    private Long certificateId;
    private String certificateNo;
    private Integer versionNo;
    private String productName;
    private Long actorId;
    private String result;
    private String resultCode;
    private String requestTraceId;
    private LocalDateTime occurredAt;
}
