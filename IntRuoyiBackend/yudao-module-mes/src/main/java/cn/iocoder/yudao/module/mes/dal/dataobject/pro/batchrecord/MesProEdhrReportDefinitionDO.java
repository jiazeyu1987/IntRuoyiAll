package cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@TableName("mes_pro_edhr_report_definition")
@KeySequence("mes_pro_edhr_report_definition_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrReportDefinitionDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;

    private String reportCode;

    private String reportName;

    private String reportType;

    private Long datasetId;

    private String datasetCode;

    private String datasetVersion;

    private String status;

    private String caliberVersion;

    private String fieldCaliberJson;

    private String filterSchemaJson;

    private String drilldownTargetJson;

    private String permissionSummaryJson;

    private String dataSourceStatus;

    private String sampleQueryJson;

    private LocalDateTime publishedAt;

    private String remark;
}
