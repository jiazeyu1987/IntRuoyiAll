package cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.task.MesProTaskDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@TableName("mes_pro_task_schedule_ext")
@KeySequence("mes_pro_task_schedule_ext_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProTaskScheduleExtDO extends BaseDO {

    @TableId
    private Long id;

    private Long taskId;

    private Long scheduleOrderId;

    private Long scheduleOrderProcessId;

    private String scheduleSource;

    private Boolean locked;

    private String lockedReason;

    private String generatedRequestId;

    private String riskStatus;

    private String remark;

}
