package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesProEdhrTravelerEventRespVO {

    private Long id;

    private Long travelerId;

    private String travelerCode;

    private String eventType;

    private String resultStatus;

    private String failureReason;

    private Long operatorUserId;

    private String operatorUsername;

    private LocalDateTime occurredAt;

    private String metadataJson;
}
