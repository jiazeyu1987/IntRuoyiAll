package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesProEdhrReleaseEventRespVO {

    private Long id;

    private Long releaseTransactionId;

    private String eventType;

    private String fromStatus;

    private String toStatus;

    private Long actorUserId;

    private String reason;

    private String opinion;

    private String idempotencyKey;

    private String signoffEvidenceHash;

    private String eventSnapshotJson;

    private String evidenceHash;

    private LocalDateTime occurredAt;
}
