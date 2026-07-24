package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionFieldAuditSignatureCommand {

    private Long executionId;

    private String password;

    private String reasonCategory;

    private String reasonText;

    private String signatureChallengeHash;

    private MesProBatchRecordExecutionSignatureTimeCommand signatureTimeCommand;
}
