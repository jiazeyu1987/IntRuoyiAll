package cn.iocoder.yudao.module.mes.approval.content;

import lombok.Data;
import lombok.experimental.Accessors;

/** Server constructed command. It is never accepted as an HTTP request. */
@Data
@Accessors(chain = true)
public class EdhrCenterReviewCommand {
    private Long actorId;
    private Long workTaskId;
    private Integer expectedVersion;
    private String idempotencyKey;
    private String reason;
    private Long signatureId;
    private Long signatureCreationEventId;
    private String signatureCreationEventHash;
    private Long centerReceiptId;
    private String prepareId;
    private String canonicalHash;
    private String renderedHash;
    private String subjectId;
    private String subjectVersion;
    private String evidenceHash;
}
