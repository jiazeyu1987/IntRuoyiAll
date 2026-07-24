package cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * MES 排产工单按天计划实际差异 DO
 */
@TableName("mes_pro_schedule_order_daily_compare")
@KeySequence("mes_pro_schedule_order_daily_compare_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProScheduleOrderDailyCompareDO extends BaseDO {

    @TableId
    private Long id;

    private Long scheduleOrderId;

    private Long scheduleOrderProcessId;

    /**
     * 工序编号
     *
     * 关联 {@link MesProProcessDO#getId()}
     */
    private Long processId;

    private LocalDate planDate;

    private BigDecimal plannedQuantity;

    private BigDecimal actualQuantity;

    private BigDecimal diffQuantity;

    private Integer status;

    private String remark;

}
