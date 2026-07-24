package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesProEdhrPrintHistoryCopyRespVO {

    private Long id;

    private String copyCode;

    private Long sourcePrintTaskId;

    private String sourceObjectType;

    private String sourceObjectCode;

    private String copyReason;

    private String watermarkText;

    private String evidenceHash;

    private String idempotencyKey;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
