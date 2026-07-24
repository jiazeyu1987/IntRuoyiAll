package cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@TableName("mes_pro_edhr_dhr_template_impact")
@KeySequence("mes_pro_edhr_dhr_template_impact_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class MesProEdhrDhrTemplateImpactDO extends BaseDO {

    @TableId
    private Long id;

    private Long templateId;

    private String actionType;

    private String impactScopeJson;

    private Boolean impactConfirmed;

    private Long confirmedBy;

    private LocalDateTime confirmedAt;
}
