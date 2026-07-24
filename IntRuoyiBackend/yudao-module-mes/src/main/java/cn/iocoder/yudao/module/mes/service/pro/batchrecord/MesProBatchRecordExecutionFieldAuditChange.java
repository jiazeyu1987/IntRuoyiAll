package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionFieldAuditChange {

    private String fieldPath;

    private String fieldKey;

    private Integer rowIndex;

    private Integer columnIndex;

    private MesProBatchRecordExecutionFieldAuditValueType valueType;

    private Object newValueJson;

    private String newValueDisplay;

    private Object expectedOldValueJson;

    private String expectedOldValueHash;
}
