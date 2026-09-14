package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class BatchRecordRepeatRowGroupVO {

    private Long id;
    private String scopeType;
    private Long scopeId;
    private Long routeId;
    private Long batchRecordDefinitionId;
    private Long batchRecordVersionId;
    private Long routeProcessId;
    private String targetReportId;
    private String targetReportName;
    private Integer templateStartRowIndex;
    private Integer templateEndRowIndex;
    private Integer repeatAreaStartRowIndex;
    private Integer repeatAreaEndRowIndex;
    private String sourceType;
    private List<BatchRecordRepeatRowGroupRecordVO> records;
    private List<BatchRecordRepeatRowGroupMappingVO> mappings;
    private Long configVersion;
    private String templateSnapshotHash;
    private Boolean enabled;
    private String remark;
}