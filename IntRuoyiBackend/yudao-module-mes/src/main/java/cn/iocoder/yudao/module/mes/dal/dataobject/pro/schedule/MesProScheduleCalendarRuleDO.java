package cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@TableName("mes_pro_schedule_calendar_rule")
@KeySequence("mes_pro_schedule_calendar_rule_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProScheduleCalendarRuleDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Boolean skipStatutoryHolidays;

    private String weekendRestMode;

    private String dateShiftModeByDateJson;

    private Boolean temporaryFreezeEnabled;

    private String remark;

}
