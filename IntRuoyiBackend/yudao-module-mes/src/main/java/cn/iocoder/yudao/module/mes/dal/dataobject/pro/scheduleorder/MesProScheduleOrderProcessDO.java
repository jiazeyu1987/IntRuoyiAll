package cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
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
import java.time.LocalDateTime;

/**
 * MES 排产工单工序快照 DO
 */
@TableName("mes_pro_schedule_order_process")
@KeySequence("mes_pro_schedule_order_process_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProScheduleOrderProcessDO extends BaseDO {

    @TableId
    private Long id;

    private Long scheduleOrderId;

    /**
     * 路线工序编号
     *
     * 关联 {@link MesProRouteProcessDO#getId()}
     */
    private Long routeProcessId;

    private Long predecessorRouteProcessId;

    /**
     * 完整直接前置路线工序ID集合快照（JSON数组）。
     * predecessorRouteProcessId 仅保留给旧数据和单前置兼容读取。
     */
    private String predecessorRouteProcessIdsJson;

    private Boolean rootProcessFlag;

    private Long routeVersionId;

    private Long routeScheduleConfigId;

    /**
     * 工序编号
     *
     * 关联 {@link MesProProcessDO#getId()}
     */
    private Long processId;

    private String processCode;

    private String processName;

    private Integer sort;

    private Boolean enabled;

    private String capacitySource;

    private String capacityMode;

    private BigDecimal hourlyCapacityTotal;

    private BigDecimal infiniteDurationQuantityFactor;

    private BigDecimal infiniteDurationBaseMinutes;

    private BigDecimal shiftHours;

    private BigDecimal shiftCapacityTotal;

    private BigDecimal productionQuantityFactor;

    private String resourceSnapshotJson;

    private BigDecimal plannedQuantity;

    private BigDecimal reportedQuantity;

    private BigDecimal remainingQuantity;

    private BigDecimal progressPercent;

    private Boolean nightShiftEnabled;

    private Long calendarRuleId;

    private Boolean keyProcessFlag;

    private LocalDate planDate;

    private LocalDateTime plannedStartTime;

    private LocalDateTime plannedEndTime;

    private LocalDateTime actualStartTime;

    private LocalDateTime actualEndTime;

    private Boolean bottleneckFlag;

    private String remark;

}
