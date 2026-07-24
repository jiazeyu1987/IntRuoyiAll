package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionFieldAuditSignatureResult {

    private Long signatureId;

    private Long actorId;

    private String actorName;

    private LocalDateTime signedAt;

    private LocalDateTime selectedSignedAt;

    private LocalDateTime signatureDisplayAt;

    private String signatureTimeMode;

    private String selectedTimeZone;

    private String selectedTimeReason;

    private String selectedTimePolicyVersion;

    private String selectedTimeAuditHash;
}
