package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class BatchRecordReportSignatureCellMarkersRespVO {

    private String reportId;

    private String sheetLayoutJson;

    private List<BatchRecordReportSignatureCellMarkerVO> markers;
}
