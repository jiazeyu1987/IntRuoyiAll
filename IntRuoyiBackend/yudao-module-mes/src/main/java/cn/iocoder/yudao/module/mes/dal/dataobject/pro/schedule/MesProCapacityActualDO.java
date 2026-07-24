package cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

@TableName("mes_pro_capacity_actual")
@KeySequence("mes_pro_capacity_actual_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProCapacityActualDO extends BaseDO {

    @TableId
    private Long id;

    private Long lineId;

    private LocalDateTime calendarDate;

    private Long shiftId;

    private Integer capacityMinutes;

    private Boolean enabled;

    private String remark;

}
