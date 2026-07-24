package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class BatchRecordCellLinkFormCellsRespVO {

    private String reportId;
    private String reportName;
    private Long batchRecordDefinitionId;
    private Long batchRecordVersionId;
    private String layoutSnapshotHash;
    private String sheetLayoutJson;
    private List<BatchRecordCellLinkCellVO> cells;
}
