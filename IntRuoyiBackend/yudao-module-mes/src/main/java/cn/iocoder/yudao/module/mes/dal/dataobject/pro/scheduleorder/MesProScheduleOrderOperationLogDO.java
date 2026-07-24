package cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder;

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

/**
 * MES 排产工单操作追溯 DO
 */
@TableName("mes_pro_schedule_order_operation_log")
@KeySequence("mes_pro_schedule_order_operation_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProScheduleOrderOperationLogDO extends BaseDO {

    @TableId
    private Long id;

    private Long scheduleOrderId;

    private String scheduleOrderCode;

    private String operationType;

    private String beforeSnapshotJson;

    private String afterSnapshotJson;

    private String reason;

    private Long operatorId;

    private String operatorName;

}
