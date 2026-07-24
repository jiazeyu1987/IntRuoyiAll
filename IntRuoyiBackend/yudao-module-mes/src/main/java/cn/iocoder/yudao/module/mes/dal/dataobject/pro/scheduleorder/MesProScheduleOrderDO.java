package cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleOrderRiskStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleOrderStatusEnum;
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
 * MES 排产工单 DO
 */
@TableName("mes_pro_schedule_order")
@KeySequence("mes_pro_schedule_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProScheduleOrderDO extends BaseDO {

    @TableId
    private Long id;

    /**
     * 排产工单编码
     */
    private String code;

    /**
     * 来源生产工单编号
     *
     * 关联 {@link MesProWorkOrderDO#getId()}
     */
    private Long workOrderId;

    /**
     * ERP 工单编码
     */
    private String erpWorkOrderCode;

    /**
     * 产品编号
     *
     * 关联 {@link MesMdItemDO#getId()}
     */
    private Long productId;

    /**
     * 排产数量，必须等于来源生产工单数量
     */
    private BigDecimal quantity;

    /**
     * 承诺交期
     */
    private LocalDate promiseDate;

    /**
     * 优先级排序
     */
    private Integer priorityNo;

    /**
     * 状态
     *
     * 枚举 {@link MesProScheduleOrderStatusEnum}
     */
    private Integer status;

    /**
     * ERP 差异状态
     */
    private Integer diffStatus;

    /**
     * 风险状态
     *
     * 枚举 {@link MesProScheduleOrderRiskStatusEnum}
     */
    private Integer riskStatus;

    private Integer routeStatus;

    private Boolean autoSchedulable;

    /**
     * 工艺路线编号
     *
     * 关联 {@link MesProRouteDO#getId()}
     */
    private Long routeId;

    private Long routeVersionId;

    private String routeVersion;

    private String scheduleConfigVersion;

    private LocalDateTime latestStartTime;

    private LocalDateTime plannedStartTime;

    private LocalDateTime plannedEndTime;

    private Boolean startRiskFlag;

    private Boolean delayRiskFlag;

    private BigDecimal totalQuantity;

    private BigDecimal completedQuantity;

    private BigDecimal uncompletedQuantity;

    private BigDecimal progressPercent;

    private Boolean frozen;

    private LocalDateTime frozenTime;

    private Long frozenBy;

    private String freezeReason;

    private Boolean manualFinished;

    private LocalDateTime manualFinishedTime;

    private Long manualFinishedBy;

    private String manualFinishedReason;

    private String sourceSnapshotJson;

    private String routeSnapshotJson;

    private String capacitySnapshotJson;

    private String remark;

}
