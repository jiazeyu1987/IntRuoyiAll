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

@TableName("mes_pro_edhr_unified_change_impact")
@KeySequence("mes_pro_edhr_unified_change_impact_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrUnifiedChangeImpactDO extends BaseDO {

    @TableId
    private Long id;

    private Long changeRequestId;

    private String impactType;

    private String impactObjectType;

    private String impactObjectId;

    private String impactObjectCode;

    private String riskLevel;

    private String responsibilityModule;

    private Boolean requiresTraining;

    private Boolean requiresRevalidation;

    private Boolean requiresReleaseRecheck;

    private String impactDetail;

    private String nextAction;

    private String evidenceHash;
}
