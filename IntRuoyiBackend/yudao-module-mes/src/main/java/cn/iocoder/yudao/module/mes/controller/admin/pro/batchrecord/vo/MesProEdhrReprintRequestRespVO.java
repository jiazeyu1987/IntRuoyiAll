package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesProEdhrReprintRequestRespVO {

    private Long id;

    private String requestCode;

    private Long printTaskId;

    private Long originalPrintTaskId;

    private String reprintReasonCode;

    private String reprintReason;

    private Integer usedReprintCount;

    private Integer reprintLimit;

    private String watermarkText;

    private String status;

    private String idempotencyKey;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
