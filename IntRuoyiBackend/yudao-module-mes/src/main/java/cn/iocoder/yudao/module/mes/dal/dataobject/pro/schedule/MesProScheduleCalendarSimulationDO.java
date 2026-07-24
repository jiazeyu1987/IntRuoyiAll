package cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

@TableName("mes_pro_schedule_calendar_simulation")
@KeySequence("mes_pro_schedule_calendar_simulation_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProScheduleCalendarSimulationDO extends TenantBaseDO {

    @TableId
    private Long id;

    @TableField("simulation_date")
    private LocalDateTime currentDate;

    private String remark;

}
