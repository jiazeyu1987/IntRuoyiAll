package cn.iocoder.yudao.module.mes.dal.dataobject.pro.route;

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

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * MES 排产日资源调整 DO
 */
@TableName("mes_pro_schedule_resource_adjustment")
@KeySequence("mes_pro_schedule_resource_adjustment_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProScheduleResourceAdjustmentDO extends BaseDO {

    @TableId
    private Long id;

    private Long routeId;

    private Long routeProcessId;

    private LocalDate calendarDate;

    private String resourceType;

    private Long workstationId;

    private Long workstationMachineId;

    private Long machineryId;

    private Integer availableQuantityOverride;

    private Integer workerQuantityOverride;

    private BigDecimal singleHourlyCapacityOverride;

    private BigDecimal shiftHoursOverride;

    private String reason;

}
