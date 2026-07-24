package cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * MES 产线 DO
 */
@TableName("mes_md_production_line")
@KeySequence("mes_md_production_line_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesMdProductionLineDO extends BaseDO {

    @TableId
    private Long id;

    private String code;

    private String name;

    private Long workshopId;

    private Long calendarPlanId;

    private Integer status;

    private String remark;

}
