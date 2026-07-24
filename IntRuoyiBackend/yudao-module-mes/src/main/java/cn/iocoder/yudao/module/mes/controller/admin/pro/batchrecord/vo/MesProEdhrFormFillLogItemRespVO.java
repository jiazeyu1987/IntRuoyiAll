package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesProEdhrFormFillLogItemRespVO {

    private Long auditItemId;
    private String fieldPath;
    private String fieldKey;
    private String fieldLabel;
    private Integer rowIndex;
    private Integer columnIndex;
    private String oldValueDisplay;
    private String newValueDisplay;
    private String recordbookValueDisplay;
    private String batchRecordValueDisplay;
    private LocalDateTime changedAt;
}
