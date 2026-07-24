package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionFieldResponsibilityHistoryRespVO {

    private Long executionId;
    private String fieldPath;
    private String fieldKey;
    private Integer rowIndex;
    private Integer columnIndex;
    private List<MesProBatchRecordExecutionFieldResponsibilityHistoryItemRespVO> list;
    private Boolean hasMore;
    private Long nextCursorFieldAuditRevision;
    private Long nextCursorAuditItemId;
}
