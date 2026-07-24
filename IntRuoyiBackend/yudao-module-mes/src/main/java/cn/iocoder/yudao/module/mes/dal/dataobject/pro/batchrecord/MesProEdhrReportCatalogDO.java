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

@TableName("mes_pro_edhr_report_catalog")
@KeySequence("mes_pro_edhr_report_catalog_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrReportCatalogDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;

    private String reportCode;

    private String reportName;

    private String reportCategory;

    private String businessPurpose;

    private String primaryDimensions;

    private String relatedDimensions;

    private String dataSourceSummary;

    private String permissionPolicy;

    private String exportPolicy;

    private String status;

    private String acceptanceStatus;

    private Integer sort;

    private String remark;
}
