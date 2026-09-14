package cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@TableName("mes_pro_batch_record_repeat_row_group")
@KeySequence("mes_pro_batch_record_repeat_row_group_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MesProBatchRecordRepeatRowGroupDO extends BaseDO {

    @TableId
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

    private String recordsJson;

    private String mappingsJson;

    private Long configVersion;

    private String templateSnapshotHash;

    private Boolean enabled;

    private String remark;
}