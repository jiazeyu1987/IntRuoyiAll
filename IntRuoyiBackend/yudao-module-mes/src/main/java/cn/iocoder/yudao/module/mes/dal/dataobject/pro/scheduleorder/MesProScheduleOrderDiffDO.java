package cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleOrderDiffStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * MES 排产工单 ERP 差异 DO
 */
@TableName("mes_pro_schedule_order_diff")
@KeySequence("mes_pro_schedule_order_diff_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProScheduleOrderDiffDO extends BaseDO {

    @TableId
    private Long id;

    private Long scheduleOrderId;

    /**
     * 来源生产工单编号
     *
     * 关联 {@link MesProWorkOrderDO#getId()}
     */
    private Long workOrderId;

    private String diffType;

    private String oldValueJson;

    private String newValueJson;

    /**
     * 状态
     *
     * 枚举 {@link MesProScheduleOrderDiffStatusEnum}
     */
    private Integer status;

    private Long resolvedBy;

    private LocalDateTime resolvedTime;

    private String remark;

}
