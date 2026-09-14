package cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@TableName("mes_pro_batch_record_cell_link_rule")
@KeySequence("mes_pro_batch_record_cell_link_rule_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MesProBatchRecordCellLinkRuleDO extends BaseDO {

    @TableId
    private Long id;

    private String scopeType;

    private Long scopeId;

    private Long routeId;

    private Long routeProcessId;

    private Long batchRecordDefinitionId;

    private Long batchRecordVersionId;

    private String sourceType;

    private String sourceReportId;

    private String sourceReportName;

    private Integer sourceRowIndex;

    private Integer sourceColumnIndex;

    private String sourceCellKey;

    private String sourceFieldCode;

    private String sourceFieldName;

    private String sourceLabel;

    private String sourceValueType;

    private String targetReportId;

    private String targetReportName;

    private Integer targetRowIndex;

    private Integer targetColumnIndex;

    private String targetCellKey;

    private String targetLabel;

    private String targetValueType;

    private String aggregationStrategy;

    private String overwritePolicy;

    private String templateSnapshotHash;

    private Long ruleVersion;

    private Boolean enabled;

    private String remark;
}
