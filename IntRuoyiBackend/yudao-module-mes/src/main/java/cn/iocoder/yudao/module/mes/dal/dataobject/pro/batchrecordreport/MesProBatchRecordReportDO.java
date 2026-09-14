package cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@TableName("mes_pro_batch_record_report")
@KeySequence("mes_pro_batch_record_report_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MesProBatchRecordReportDO extends BaseDO {

    @TableId
    private Long id;

    private String sampleKey;

    private String batchRecordName;

    private String productName;

    private String projectCode;

    private String formSlotType;

    private String routeKey;

    private Long batchRecordDefinitionId;

    private Long batchRecordVersionId;

    private String sourceFileName;

    private String sourceFileSha256;

    private Integer sourceTableIndex;

    private String tableTitle;

    private String reportId;

    private String reportCode;

    private String reportName;

    private String reportCategoryId;

    private LocalDateTime lastImportTime;
}
