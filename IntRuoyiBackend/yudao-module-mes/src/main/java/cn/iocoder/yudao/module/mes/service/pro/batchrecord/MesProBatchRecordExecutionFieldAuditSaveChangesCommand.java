package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionFieldAuditSaveChangesCommand {

    private Long executionId;

    private Long workTaskId;

    private String idempotencyKey;

    private String baseCellValuesHash;

    private Long baseFieldAuditRevision;

    private String baseFieldAuditHeadHash;

    private List<MesProBatchRecordExecutionFieldAuditChange> changes;

    private List<MesProBatchRecordExecutionFieldAuditAttachmentChange> attachmentChanges;

    private Signature signature;

    private String reasonCategory;

    private String reasonText;

    @Data
    @Accessors(chain = true)
    public static class Signature {

        private String password;

        private MesProBatchRecordExecutionSignatureTimeCommand signatureTimeCommand;
    }
}
