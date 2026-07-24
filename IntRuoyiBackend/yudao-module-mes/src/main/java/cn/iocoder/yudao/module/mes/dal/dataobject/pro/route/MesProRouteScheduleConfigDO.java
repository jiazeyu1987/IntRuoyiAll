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

/**
 * MES 路线排产侧配置 DO
 */
@TableName("mes_pro_route_schedule_config")
@KeySequence("mes_pro_route_schedule_config_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProRouteScheduleConfigDO extends BaseDO {

    @TableId
    private Long id;

    private Long routeVersionId;

    private Long itemId;

    private Long routeProcessId;

    private String capacityMode;

    private BigDecimal hourlyCapacity;

    private BigDecimal infiniteDurationQuantityFactor;

    private BigDecimal infiniteDurationBaseMinutes;

    private Boolean nightShiftEnabled;

    private Long calendarRuleId;

    private String configVersion;

    private Long copiedFromConfigId;

    private String remark;

}
