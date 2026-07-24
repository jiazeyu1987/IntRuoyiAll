package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class BatchRecordReportSignatureCellMarkerVO {

    private Integer rowIndex;

    private Integer columnIndex;

    private Boolean enabled;

    private String signatureCellKey;

    private String actionType;

    private String label;

    private String displayFormat;

    private String reviewSourceType;

    private Long reviewSourceId;

    private List<Long> reviewSourceIds;

    private String reviewSourceName;
}
