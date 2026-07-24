package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesProEdhrUnifiedChangeEventRespVO {

    private Long id;

    private Long changeRequestId;

    private String eventType;

    private String fromStatus;

    private String toStatus;

    private Long actorUserId;

    private String reason;

    private String signoffEvidenceHash;

    private String eventSnapshotJson;

    private String evidenceHash;

    private LocalDateTime occurredAt;

    private String idempotencyKey;
}
