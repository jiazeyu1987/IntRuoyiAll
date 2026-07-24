package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesProEdhrPrintTaskRespVO {

    private Long id;

    private String taskCode;

    private String sourceType;

    private Long sourceObjectId;

    private String sourceObjectCode;

    private String templateType;

    private Long templateId;

    private String templateCode;

    private Long labelInstanceId;

    private Long travelerId;

    private String status;

    private String printConfirmStatus;

    private Boolean isReprint;

    private Long originalPrintTaskId;

    private String reprintReason;

    private String watermarkText;

    private String failureReason;

    private String idempotencyKey;

    private Boolean printCountDeducted;

    private Long requestedBy;

    private LocalDateTime requestedAt;

    private Long confirmedBy;

    private LocalDateTime confirmedAt;

    private String confirmationEvidenceHash;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
